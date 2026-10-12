package vn.edu.dlu.dhopm.bridge;

import vn.edu.dlu.dhopm.model.PatternResult;
import vn.edu.dlu.dhopm.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Adapter bọc {@link DhopmContractTcpClient} tuân thủ {@link BridgeEngine}.
 *
 * <p>Thực thi khai phá thông qua kết nối mạng TCP Socket tới Backend (dhopm-cli)
 * theo đúng hợp đồng giao thức {@code Protocol 1} (cổng 7079, JSON Lines đa dòng).
 *
 * <p>Đặc điểm nổi bật:</p>
 * <ul>
 *   <li>Sử dụng lệnh chuẩn {@code cmd: "stream"} tiếp nhận tiến độ thời gian thực.</li>
 *   <li>Không hardcode dataset, minOcc hay engine version.</li>
 *   <li>Nếu Backend chưa chạy trên cổng 7079, chuyển sang chế độ {@code [DEMO OFFLINE FALLBACK]}
 *       và thông báo minh bạch cho người dùng, tuyệt đối không âm thầm giả mạo kết quả.</li>
 * </ul>
 *
 * @author Nguyễn Thanh Tâm (2312741) - Bridge & Protocol Layer
 */
public class TsonTcpContractBridge implements BridgeEngine {

    private static final String ENGINE_NAME = "Tson-TCP-Backend (Protocol 1 / Port 7079)";

    private final double minSupRatio;
    private final double decayFactor;
    private final String dataset;
    private final double minOcc;
    private final int limit;
    private final List<String> versions;

    private final DhopmContractTcpClient tcpClient;
    private final TsonV1Bridge fallbackBridge;

    private BiConsumer<String, Double> phaseCallback;
    private Consumer<Double> progressCallback;
    private Consumer<MiningProgressInfo> progressInfoCallback;
    private final List<Transaction> buffer = new ArrayList<>();
    private long txCount = 0;
    private int tl = 0;
    private boolean isFallback = false;

    public TsonTcpContractBridge(double minSupRatio, double decayFactor) {
        this(minSupRatio, decayFactor, "retail.dat", 0.0, 1000, List.of("v1"));
    }

    public TsonTcpContractBridge(double minSupRatio, double decayFactor, String dataset,
                                double minOcc, int limit, List<String> versions) {
        this.minSupRatio = minSupRatio;
        this.decayFactor = decayFactor;
        this.dataset = (dataset != null && !dataset.isBlank()) ? dataset : "retail.dat";
        this.versions = (versions != null && !versions.isEmpty()) ? versions : List.of("v1");
        // V1 BE chặn minOcc != 0: tự động chuẩn hóa minOcc = 0 nếu chạy v1
        if (this.versions.contains("v1")) {
            this.minOcc = 0.0;
        } else {
            this.minOcc = minOcc;
        }
        this.limit = limit;

        this.tcpClient = new DhopmContractTcpClient();
        this.fallbackBridge = new TsonV1Bridge(minSupRatio, decayFactor);
    }

    @Override
    public String engineName() {
        if (isFallback) {
            return fallbackBridge.engineName() + " [DEMO OFFLINE FALLBACK]";
        }
        return tcpClient.isConnected() ? ENGINE_NAME : ENGINE_NAME + " [Chờ kết nối]";
    }

    public boolean isUsingDemoFallback() {
        return isFallback;
    }

    @Override
    public void feedTransactions(List<Transaction> batch) {
        if (batch == null || batch.isEmpty()) return;
        buffer.addAll(batch);
        fallbackBridge.feedTransactions(batch);
        txCount += batch.size();
        for (Transaction t : batch) {
            if (t.getTid() > tl) tl = t.getTid();
        }
    }

    @Override
    public List<PatternResult> executeAndGetResults() {
        if (progressCallback != null) progressCallback.accept(0.05);

        // 1. Thử bắt tay TCP với Backend (localhost:7079)
        boolean hasTcp = tcpClient.handshake();
        if (hasTcp) {
            try {
                if (phaseCallback != null) {
                    phaseCallback.accept("Pha TCP: Bắt tay thành công với Backend " + tcpClient.getServerManager(), 5.0);
                }

                // Gửi lệnh stream đa dòng (cmd: "stream")
                DhopmContractTcpClient.ContractMineResponse resp = tcpClient.stream(
                        dataset, minSupRatio, decayFactor, minOcc, limit, versions, true,
                        eventJson -> {
                            // Xử lý các event dòng từ BE
                            String evType = DhopmContractTcpClient.parseString(eventJson, "event");
                            if ("progress".equalsIgnoreCase(evType)) {
                                Double frac = DhopmContractTcpClient.parseDouble(eventJson, "fraction");
                                Long elapsed = DhopmContractTcpClient.parseLong(eventJson, "elapsedMs");
                                Long eta = DhopmContractTcpClient.parseLong(eventJson, "etaMs");
                                if (frac != null && progressCallback != null) {
                                    progressCallback.accept(frac);
                                }
                                if (frac != null && progressInfoCallback != null) {
                                    long el = elapsed != null ? elapsed : 0L;
                                    long et = eta != null ? eta : 0L;
                                    progressInfoCallback.accept(new MiningProgressInfo(frac, el, et));
                                }
                            } else if ("window".equalsIgnoreCase(evType)) {
                                Integer curTl = DhopmContractTcpClient.parseInteger(eventJson, "TL");
                                if (curTl != null) this.tl = curTl;
                                if (phaseCallback != null) {
                                    phaseCallback.accept("Pha TCP: Đang quét cửa sổ trượt TL=" + this.tl, 1.0);
                                }
                            }
                        }
                );

                if (resp.ok() && resp.patterns() != null && !resp.patterns().isEmpty()) {
                    isFallback = false;
                    if (resp.totalTransactions() > 0) {
                        this.txCount = resp.totalTransactions();
                    }
                    if (progressCallback != null) progressCallback.accept(1.0);
                    if (phaseCallback != null) {
                        phaseCallback.accept("Pha TCP: Hoàn tất khai phá (" + resp.patterns().size() + " mẫu, " + resp.elapsedMs() + " ms)", (double) resp.elapsedMs());
                    }
                    return resp.patterns();
                }
            } catch (Exception ex) {
                // Thất bại TCP → kích hoạt fallback minh bạch
                isFallback = true;
            }
        } else {
            isFallback = true;
        }

        // 2. Chuyển sang fallback có gắn cờ thông báo minh bạch
        isFallback = true;
        if (phaseCallback != null) {
            phaseCallback.accept("⚠️ [DEMO OFFLINE FALLBACK] Backend TCP 7079 không phản hồi, chạy In-Process Demo", 0.0);
            fallbackBridge.onPhaseUpdate(phaseCallback);
        }
        if (progressCallback != null) fallbackBridge.onMiningProgress(progressCallback);
        if (progressInfoCallback != null) fallbackBridge.onMiningProgressInfo(progressInfoCallback);

        return fallbackBridge.executeAndGetResults();
    }

    @Override
    public void onPhaseUpdate(BiConsumer<String, Double> callback) {
        this.phaseCallback = callback;
        fallbackBridge.onPhaseUpdate(callback);
    }

    @Override
    public void onMiningProgress(Consumer<Double> callback) {
        this.progressCallback = callback;
        fallbackBridge.onMiningProgress(callback);
    }

    @Override
    public void onMiningProgressInfo(Consumer<MiningProgressInfo> callback) {
        this.progressInfoCallback = callback;
        fallbackBridge.onMiningProgressInfo(callback);
    }

    @Override
    public void cancel() {
        tcpClient.close();
        fallbackBridge.cancel();
    }

    @Override
    public void reset() {
        buffer.clear();
        txCount = 0;
        tl = 0;
        isFallback = false;
        tcpClient.close();
        fallbackBridge.reset();
    }

    @Override
    public long transactionCount() {
        return txCount > 0 ? txCount : fallbackBridge.transactionCount();
    }

    @Override
    public int lastTid() {
        return tl > 0 ? tl : fallbackBridge.lastTid();
    }
}
