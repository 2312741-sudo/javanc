package vn.edu.dlu.dhopm.bridge;

import javafx.application.Platform;
import vn.edu.dlu.dhopm.core.DHOPMEngine;
import vn.edu.dlu.dhopm.model.PatternResult;
import vn.edu.dlu.dhopm.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Adapter bọc {@link DHOPMEngine} của Tâm để tuân thủ {@link BridgeEngine}.
 *
 * <p>Engine này ưu tiên tính trực quan: mỗi pha pipeline phát sự kiện lên UI
 * để hiển thị step-by-step animation. Phù hợp cho demo thuyết trình thầy cô.
 *
 * <p><b>Design Patterns áp dụng:</b>
 * <ul>
 *   <li>Adapter – bọc {@code DHOPMEngine} thành {@code BridgeEngine}.</li>
 *   <li>Observer – {@code onPhaseUpdate} và {@code onMiningProgress} callbacks.</li>
 *   <li>Template Method – luồng feed → execute → result cố định, chi tiết thay đổi theo engine.</li>
 * </ul>
 *
 * @author Nguyễn Thanh Tâm (2312741) - Bridge Layer
 */
public class TamSimulationBridge implements BridgeEngine {

    private static final String ENGINE_NAME = "Tâm-Simulation";

    private DHOPMEngine engine;
    private double minSupRatio;
    private double decayFactor;

    private BiConsumer<String, Double> phaseCallback;
    private Consumer<Double> progressCallback;

    public TamSimulationBridge(double minSupRatio, double decayFactor) {
        this.minSupRatio = minSupRatio;
        this.decayFactor = decayFactor;
        this.engine = new DHOPMEngine();
    }

    @Override
    public String engineName() {
        return ENGINE_NAME;
    }

    @Override
    public void onPhaseUpdate(BiConsumer<String, Double> callback) {
        this.phaseCallback = callback;
    }

    @Override
    public void onMiningProgress(Consumer<Double> callback) {
        this.progressCallback = callback;
    }

    @Override
    public void feedTransactions(List<Transaction> batch) {
        long t0 = System.nanoTime();
        engine.phase1_constructOrUpdate(batch);
        double ms = (System.nanoTime() - t0) / 1_000_000.0;
        notifyPhase("CONSTRUCTION", ms);
    }

    @Override
    public List<PatternResult> executeAndGetResults() {
        long t0 = System.nanoTime();
        engine.phase2_reconstruct(decayFactor, engine.getCurrentTL());
        double msRecon = (System.nanoTime() - t0) / 1_000_000.0;
        notifyPhase("RECONSTRUCTION", msRecon);

        long t1 = System.nanoTime();
        double absoluteMinSup = minSupRatio * engine.getAllTransactions().size();
        List<PatternResult> results = engine.phase3_mine(absoluteMinSup, decayFactor, engine.getCurrentTL());
        double msMine = (System.nanoTime() - t1) / 1_000_000.0;
        notifyPhase("MINING", msMine);

        notifyProgress(1.0);
        return results;
    }

    @Override
    public void reset() {
        this.engine = new DHOPMEngine();
    }

    @Override
    public long transactionCount() {
        return engine.getAllTransactions().size();
    }

    @Override
    public int lastTid() {
        return engine.getCurrentTL();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void notifyPhase(String name, double ms) {
        if (phaseCallback != null) {
            Platform.runLater(() -> phaseCallback.accept(name, ms));
        }
    }

    private void notifyProgress(double fraction) {
        if (progressCallback != null) {
            Platform.runLater(() -> progressCallback.accept(fraction));
        }
    }
}
