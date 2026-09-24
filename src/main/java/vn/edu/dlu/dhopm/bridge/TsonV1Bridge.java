package vn.edu.dlu.dhopm.bridge;

import dhopm.common.config.MiningConfig;
import dhopm.common.contract.MineResult;
import dhopm.common.contract.Pattern;
import dhopm.common.contract.Phase;
import dhopm.v1.engine.MiningEngine;
import javafx.application.Platform;
import vn.edu.dlu.dhopm.model.PatternResult;
import vn.edu.dlu.dhopm.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Adapter bọc {@code MiningEngine} của Tson để tuân thủ {@link BridgeEngine}.
 *
 * <p>Class này là điểm tích hợp duy nhất giữa UI của Tâm và Engine của Tson.
 * UI tuyệt đối KHÔNG import trực tiếp bất kỳ class nào từ {@code dhopm.v1.*}
 * hoặc {@code dhopm.common.*} – chỉ dùng qua {@code BridgeEngine}.
 *
 * <h3>Cách sử dụng trong UI:</h3>
 * <pre>{@code
 * BridgeEngine engine = EngineFactory.create(EngineMode.TSON_V1_STANDARD, 0.15, 0.9);
 * engine.onPhaseUpdate((phase, ms) -> System.out.printf("Pha %s: %.2f ms%n", phase, ms));
 * engine.onMiningProgress(pct -> progressBar.setProgress(pct));
 * engine.feedTransactions(batch);
 * List<PatternResult> results = engine.executeAndGetResults();
 * }</pre>
 *
 * <p><b>Design Patterns áp dụng:</b>
 * <ul>
 *   <li>Adapter – bọc {@code MiningEngine} thành {@code BridgeEngine}.</li>
 *   <li>Observer – relay {@code PhaseListener} và {@code MiningProgressListener} về JavaFX UI thread.</li>
 *   <li>Facade – ẩn hoàn toàn API phức tạp của Tson ({@code MiningConfig}, {@code WorkerPool}, v.v.).</li>
 * </ul>
 *
 * <p><b>Khi Tson phát hành V2:</b> Tâm chỉ thêm {@code TSON_V2_OPTIMIZED} vào {@link EngineMode},
 * tạo {@code TsonV2Bridge.java} tương tự, không sửa bất kỳ dòng UI nào.
 *
 * @author Nguyễn Thanh Tâm (2312741) - Bridge Layer
 */
public class TsonV1Bridge implements BridgeEngine {

    private static final String ENGINE_NAME = "Tson-V1-Standard";

    private final double minSupRatio;
    private final double decayFactor;

    private BiConsumer<String, Double> phaseCallback;
    private Consumer<Double> progressCallback;

    private MiningEngine tsonEngine;
    private final List<dhopm.common.transaction.Transaction> tsonBuffer = new ArrayList<>();
    private long txCount = 0;
    private int tl = 0;

    /**
     * Tạo bridge đến engine V1 của Tson với cấu hình mining.
     *
     * @param minSupRatio  ngưỡng support tương đối ∂ ∈ [0,1]  (ví dụ: 0.15 = 15%)
     * @param decayFactor  hệ số suy giảm f ∈ (0,1] (ví dụ: 0.9)
     */
    public TsonV1Bridge(double minSupRatio, double decayFactor) {
        this.minSupRatio = minSupRatio;
        this.decayFactor = decayFactor;
        initTsonEngine();
    }

    private void initTsonEngine() {
        MiningConfig config = MiningConfig.of(minSupRatio, decayFactor);
        this.tsonEngine = new MiningEngine(config);
        this.tsonBuffer.clear();
        this.txCount = 0;
        this.tl = 0;
    }

    @Override
    public String engineName() {
        return ENGINE_NAME;
    }

    @Override
    public void onPhaseUpdate(BiConsumer<String, Double> callback) {
        this.phaseCallback = callback;
        // Relay sang PhaseListener của Tson – sự kiện được bắn sau mỗi pha
        tsonEngine.setPhaseListener((phase, startNs, endNs) -> {
            double ms = (endNs - startNs) / 1_000_000.0;
            String phaseName = phaseDisplayName(phase);
            Platform.runLater(() -> callback.accept(phaseName, ms));
        });
    }

    @Override
    public void onMiningProgress(Consumer<Double> callback) {
        this.progressCallback = callback;
        // Relay sang MiningProgressListener của Tson
        tsonEngine.setMiningProgressListener(progress -> {
            Platform.runLater(() -> callback.accept(progress.fraction()));
        });
    }

    @Override
    public void feedTransactions(List<Transaction> batch) {
        if (batch == null || batch.isEmpty()) return;

        // Chuyển đổi Transaction của Tâm → Transaction của Tson
        List<dhopm.common.transaction.Transaction> tsonBatch = toTsonTransactions(batch);
        tsonBuffer.addAll(tsonBatch);
        tsonEngine.loadBatch(tsonBatch);

        for (Transaction t : batch) {
            txCount++;
            if (t.getTid() > tl) tl = t.getTid();
        }
    }

    @Override
    public List<PatternResult> executeAndGetResults() {
        MineResult result = tsonEngine.mineNow();
        notifyProgress(1.0);
        return toPatternResults(result);
    }

    @Override
    public void reset() {
        initTsonEngine();
        // Gắn lại listener nếu đã có (vì engine mới được tạo)
        if (phaseCallback != null) onPhaseUpdate(phaseCallback);
        if (progressCallback != null) onMiningProgress(progressCallback);
    }

    @Override
    public long transactionCount() {
        return txCount;
    }

    @Override
    public int lastTid() {
        return tl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Conversion helpers: Tâm model ↔ Tson model
    // Cách ly hoàn toàn – UI không biết sự tồn tại của 2 lớp Transaction khác nhau
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Chuyển đổi {@link Transaction} (model của Tâm: class mutable, dùng getTid()/getItems())
     * sang {@code dhopm.common.transaction.Transaction} (model của Tson: record bất biến).
     */
    private List<dhopm.common.transaction.Transaction> toTsonTransactions(List<Transaction> tamList) {
        List<dhopm.common.transaction.Transaction> tsonList = new ArrayList<>(tamList.size());
        for (Transaction t : tamList) {
            String[] items = t.getItems().toArray(new String[0]);
            tsonList.add(new dhopm.common.transaction.Transaction(t.getTid(), items));
        }
        return tsonList;
    }

    /**
     * Chuyển đổi {@code MineResult} của Tson sang {@link PatternResult} của Tâm
     * để hiển thị lên {@code TableView}.
     *
     * <p>Sắp xếp theo DO giảm dần để các mẫu nóng nhất hiển thị đầu bảng.
     */
    private List<PatternResult> toPatternResults(MineResult mineResult) {
        List<PatternResult> uiList = new ArrayList<>();
        double absoluteMinSup = minSupRatio * mineResult.totalTransactions();

        for (Pattern p : mineResult.patterns()) {
            String patternStr = p.canonicalKey();
            double doValue    = p.dampedOccupancy();
            double dubo       = doValue; // V1 không expose DUBO riêng
            int support       = p.tids().length;
            boolean isDHOP    = doValue >= absoluteMinSup;
            boolean isPruned  = false; // Tson đã lọc sẵn – chỉ trả pattern đạt chuẩn

            // Chuyển int[] tids → List<Integer>
            List<Integer> tidList = new ArrayList<>(p.tids().length);
            for (int tid : p.tids()) tidList.add(tid);

            uiList.add(new PatternResult(patternStr, doValue, dubo, support, isDHOP, isPruned, tidList));
        }

        // Sắp xếp DO giảm dần
        uiList.sort((a, b) -> Double.compare(b.doValue(), a.doValue()));
        return uiList;
    }

    private String phaseDisplayName(Phase phase) {
        return switch (phase) {
            case CONSTRUCTION   -> "CONSTRUCTION (Pha 1 - Xây DHO-List)";
            case RECONSTRUCTION -> "RECONSTRUCTION (Pha 2 - Tái cấu trúc DO)";
            case MINING         -> "MINING (Pha 3 - Khai phá DFS)";
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void notifyProgress(double fraction) {
        if (progressCallback != null) {
            Platform.runLater(() -> progressCallback.accept(fraction));
        }
    }
}
