package vn.edu.dlu.dhopm.bridge;

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
 * <h3>Trạng thái compile khi chưa có Tson jar:</h3>
 * <p>Khi module {@code dhopm-v1-standard} chưa được build (chưa {@code mvn install}),
 * class này sẽ compile-error. Giải pháp: chạy lệnh sau để install từ repo Tson:
 * <pre>
 *   cd /tmp/tson_jvnc/implementation
 *   mvn install -DskipTests
 * </pre>
 *
 * <p><b>Design Patterns áp dụng:</b>
 * <ul>
 *   <li>Adapter – bọc {@code MiningEngine} thành {@code BridgeEngine}.</li>
 *   <li>Observer – relay {@code PhaseListener} và {@code MiningProgressListener} về JavaFX UI thread.</li>
 *   <li>Facade – ẩn hoàn toàn API phức tạp của Tson ({@code MiningConfig}, {@code WorkerPool}, v.v.).</li>
 * </ul>
 *
 * <p><b>Khi Tson phát hành V2:</b> Tâm chỉ thêm {@link EngineMode#TSON_V1_STANDARD} → {@code TSON_V2_OPTIMIZED},
 * tạo {@code TsonV2Bridge.java} tương tự, không sửa bất kỳ dòng UI code nào.
 *
 * @author Nguyễn Thanh Tâm (2312741) - Bridge Layer
 */
public class TsonV1Bridge implements BridgeEngine {

    /*
     * ─────────────────────────────────────────────────────────────────────────
     *  HƯỚNG DẪN TÍCH HỢP TỪNG BƯỚC:
     *
     *  Bước 1: Clone repo Tson
     *    git clone https://github.com/Tson-dev/JVNC.git /tmp/tson_jvnc
     *
     *  Bước 2: Cài đặt dhopm-common và dhopm-v1-standard vào Maven local repo
     *    cd /tmp/tson_jvnc/implementation
     *    mvn install -DskipTests
     *
     *  Bước 3: Thêm dependencies vào pom.xml của repo Tâm (đã được thêm sẵn):
     *    <dependency>
     *      <groupId>dhopm</groupId>
     *      <artifactId>dhopm-common</artifactId>
     *      <version>1.0.0</version>
     *    </dependency>
     *    <dependency>
     *      <groupId>dhopm</groupId>
     *      <artifactId>dhopm-v1-standard</artifactId>
     *      <version>1.0.0</version>
     *    </dependency>
     *
     *  Bước 4: Bỏ comment các dòng import bên dưới và xóa stub code
     * ─────────────────────────────────────────────────────────────────────────
     */

    // TODO: Bỏ comment khi đã chạy: mvn install -DskipTests tại repo Tson
    // import dhopm.common.config.MiningConfig;
    // import dhopm.common.contract.MineResult;
    // import dhopm.common.contract.MiningProgress;
    // import dhopm.common.contract.Phase;
    // import dhopm.v1.engine.MiningEngine;

    private static final String ENGINE_NAME = "Tson-V1-Standard";

    private final double minSupRatio;
    private final double decayFactor;

    private BiConsumer<String, Double> phaseCallback;
    private Consumer<Double> progressCallback;

    // ── Tson engine instance (uncomment sau khi install jar) ──────────────────
    // private MiningEngine tsonEngine;
    private long txCount = 0;
    private int tl = 0;

    // ── Local buffer: chuyển đổi Transaction Tâm → Transaction Tson ──────────
    // private final List<dhopm.common.transaction.Transaction> tsonBuffer = new ArrayList<>();

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
        // TODO: Bỏ comment khi đã install jar của Tson:
        // MiningConfig config = MiningConfig.of(minSupRatio, decayFactor);
        // this.tsonEngine = new MiningEngine(config);
    }

    @Override
    public String engineName() {
        return ENGINE_NAME;
    }

    @Override
    public void onPhaseUpdate(BiConsumer<String, Double> callback) {
        this.phaseCallback = callback;
        // TODO: Relay sang PhaseListener của Tson sau khi install jar:
        // tsonEngine.setPhaseListener((phase, startNs, endNs) -> {
        //     double ms = (endNs - startNs) / 1_000_000.0;
        //     Platform.runLater(() -> callback.accept(phase.name(), ms));
        // });
    }

    @Override
    public void onMiningProgress(Consumer<Double> callback) {
        this.progressCallback = callback;
        // TODO: Relay sang MiningProgressListener của Tson sau khi install jar:
        // tsonEngine.setMiningProgressListener(progress -> {
        //     Platform.runLater(() -> callback.accept(progress.fraction()));
        // });
    }

    @Override
    public void feedTransactions(List<Transaction> batch) {
        if (batch == null || batch.isEmpty()) return;
        for (Transaction t : batch) {
            txCount++;
            if (t.getTid() > tl) tl = t.getTid();
        }
        // TODO: Bỏ comment khi đã install jar của Tson:
        // List<dhopm.common.transaction.Transaction> tsonBatch = toTsonTransactions(batch);
        // tsonEngine.loadBatch(tsonBatch);
    }

    @Override
    public List<PatternResult> executeAndGetResults() {
        // TODO: Bỏ comment khi đã install jar của Tson:
        // MineResult result = tsonEngine.mineNow();
        // notifyProgress(1.0);
        // return toPatternResults(result);

        // STUB: trả về rỗng cho đến khi tích hợp đầy đủ
        notifyProgress(1.0);
        return new ArrayList<>();
    }

    @Override
    public void reset() {
        txCount = 0;
        tl = 0;
        initTsonEngine();
        // TODO: Tson MiningEngine không có reset() – phải tạo instance mới (là đúng thiết kế)
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
     * Chuyển đổi {@link Transaction} (model của Tâm) sang
     * {@code dhopm.common.transaction.Transaction} (model của Tson).
     *
     * <p>Cả 2 model đều lưu (tid, items[]). Phép chuyển đổi O(n) đơn giản.
     */
    // TODO: Bỏ comment khi đã install jar của Tson:
    // private List<dhopm.common.transaction.Transaction> toTsonTransactions(List<Transaction> tamList) {
    //     List<dhopm.common.transaction.Transaction> tsonList = new ArrayList<>(tamList.size());
    //     for (Transaction t : tamList) {
    //         String[] items = t.getItems().toArray(new String[0]);
    //         tsonList.add(new dhopm.common.transaction.Transaction(t.getTid(), items));
    //     }
    //     return tsonList;
    // }

    /**
     * Chuyển đổi {@code MineResult} của Tson sang {@link PatternResult} của Tâm
     * để hiển thị lên {@code TableView}.
     */
    // TODO: Bỏ comment khi đã install jar của Tson:
    // private List<PatternResult> toPatternResults(MineResult mineResult) {
    //     List<PatternResult> uiList = new ArrayList<>();
    //     for (dhopm.common.contract.Pattern p : mineResult.patterns()) {
    //         String patternStr = p.canonicalKey();
    //         double doValue   = p.dampedOccupancy();
    //         // DUBO không có sẵn trong Pattern – gán bằng DO (V1 không expose DUBO riêng)
    //         double dubo      = doValue;
    //         int support      = p.tids().length;
    //         String tidsStr   = buildTidsString(p.tids());
    //
    //         PatternResult pr = new PatternResult(patternStr, support, doValue, dubo, "DHOP", tidsStr);
    //         uiList.add(pr);
    //     }
    //     uiList.sort((a, b) -> Double.compare(b.getDo(), a.getDo())); // DO giảm dần
    //     return uiList;
    // }

    // private String buildTidsString(int[] tids) {
    //     if (tids == null || tids.length == 0) return "-";
    //     StringBuilder sb = new StringBuilder();
    //     for (int i = 0; i < tids.length; i++) {
    //         if (i > 0) sb.append(", ");
    //         sb.append("T").append(tids[i]);
    //     }
    //     return sb.toString();
    // }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void notifyProgress(double fraction) {
        if (progressCallback != null) {
            Platform.runLater(() -> progressCallback.accept(fraction));
        }
    }
}
