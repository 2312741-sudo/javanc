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
 * theo đúng hợp đồng giao thức {@code Protocol 1} (cổng 7079, JSON Lines).
 *
 * <p>Nếu Backend chưa chạy trên cổng 7079, tự động fallback sang {@link TsonV1Bridge}
 * để đảm bảo ứng dụng hoạt động liền mạch và không bao giờ bị gián đoạn.
 *
 * @author Nguyễn Thanh Tâm (2312741) - Bridge & Protocol Layer
 */
public class TsonTcpContractBridge implements BridgeEngine {

    private static final String ENGINE_NAME = "Tson-TCP-Backend (Protocol 1 / Port 7079)";

    private final double minSupRatio;
    private final double decayFactor;
    private final DhopmContractTcpClient tcpClient;
    private final TsonV1Bridge fallbackBridge;

    private BiConsumer<String, Double> phaseCallback;
    private Consumer<Double> progressCallback;
    private final List<Transaction> buffer = new ArrayList<>();
    private long txCount = 0;
    private int tl = 0;

    public TsonTcpContractBridge(double minSupRatio, double decayFactor) {
        this.minSupRatio = minSupRatio;
        this.decayFactor = decayFactor;
        this.tcpClient = new DhopmContractTcpClient();
        this.fallbackBridge = new TsonV1Bridge(minSupRatio, decayFactor);
    }

    @Override
    public String engineName() {
        return tcpClient.isConnected() ? ENGINE_NAME : fallbackBridge.engineName() + " (TCP Offline Fallback)";
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
        if (progressCallback != null) progressCallback.accept(0.1);

        // 1. Thử kết nối TCP tới Backend (localhost:7079)
        boolean hasTcp = tcpClient.handshake();
        if (hasTcp) {
            try {
                if (phaseCallback != null) phaseCallback.accept("Pha TCP: Bắt tay thành công với dhopm-cli", 5.0);
                if (progressCallback != null) progressCallback.accept(0.5);

                // Gửi lệnh mine theo chuẩn CONTRACT.md
                DhopmContractTcpClient.ContractMineResponse resp = tcpClient.mine(
                        "default.dat", minSupRatio, decayFactor, 1e-6, 0, List.of("v1"), true
                );

                if (resp.ok() && resp.patterns() != null && !resp.patterns().isEmpty()) {
                    if (progressCallback != null) progressCallback.accept(1.0);
                    if (phaseCallback != null) phaseCallback.accept("Pha TCP: Hoàn thành khai phá qua Socket", 15.0);
                    return resp.patterns();
                }
            } catch (Exception ignored) {
                // Thất bại thì chuyển tiếp sang fallback
            }
        }

        // 2. Fallback sang In-Process Tson Engine nếu BE TCP chưa mở
        if (phaseCallback != null) fallbackBridge.onPhaseUpdate(phaseCallback);
        if (progressCallback != null) fallbackBridge.onMiningProgress(progressCallback);
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
    public void cancel() {
        fallbackBridge.cancel();
    }

    @Override
    public void reset() {
        buffer.clear();
        txCount = 0;
        tl = 0;
        fallbackBridge.reset();
    }

    @Override
    public long transactionCount() {
        return txCount;
    }

    @Override
    public int lastTid() {
        return tl;
    }
}
