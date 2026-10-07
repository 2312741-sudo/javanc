package vn.edu.dlu.dhopm.history;

import vn.edu.dlu.dhopm.model.PatternResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * <b>Memento Pattern:</b> Lưu trữ trạng thái snapshot của một lần thực thi khai phá (Mining Run).
 * <p>
 * Lưu giữ đầy đủ thông số đầu vào (f, ∂, minSup, dataset) và kết quả đo đạc (DHOP count, pruned count, runtime ms)
 * để phục vụ biểu đồ lịch sử và đối chiếu so sánh xu hướng qua các lần mining.
 * </p>
 *
 * @author Nguyễn Thanh Tâm (2312741)
 */
public class MiningRunMemento {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final int runId;
    private final String timestamp;
    private final String engineName;
    private final String datasetName;
    private final double f;
    private final double partial;
    private final double minSup;
    private final int totalTransactions;
    private final int dhopCount;
    private final int prunedCount;
    private final int totalCandidates;
    private final long runtimeMs;
    private final double peakHeapMb;
    private final List<PatternResult> topPatterns;

    public MiningRunMemento(
            int runId,
            String engineName,
            String datasetName,
            double f,
            double partial,
            double minSup,
            int totalTransactions,
            int dhopCount,
            int prunedCount,
            int totalCandidates,
            long runtimeMs,
            double peakHeapMb,
            List<PatternResult> topPatterns
    ) {
        this.runId = runId;
        this.timestamp = LocalDateTime.now().format(FORMATTER);
        this.engineName = engineName;
        this.datasetName = datasetName;
        this.f = f;
        this.partial = partial;
        this.minSup = minSup;
        this.totalTransactions = totalTransactions;
        this.dhopCount = dhopCount;
        this.prunedCount = prunedCount;
        this.totalCandidates = totalCandidates;
        this.runtimeMs = runtimeMs;
        this.peakHeapMb = peakHeapMb;
        this.topPatterns = topPatterns != null ? List.copyOf(topPatterns) : Collections.emptyList();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getRunId() { return runId; }
    public String getTimestamp() { return timestamp; }
    public String getEngineName() { return engineName; }
    public String getDatasetName() { return datasetName; }
    public double getF() { return f; }
    public double getPartial() { return partial; }
    public double getMinSup() { return minSup; }
    public int getTotalTransactions() { return totalTransactions; }
    public int getDhopCount() { return dhopCount; }
    public int getPrunedCount() { return prunedCount; }
    public int getTotalCandidates() { return totalCandidates; }
    public long getRuntimeMs() { return runtimeMs; }
    public double getPeakHeapMb() { return peakHeapMb; }
    public List<PatternResult> getTopPatterns() { return topPatterns; }

    public double getPrunedRatio() {
        return totalCandidates > 0 ? (double) prunedCount / totalCandidates : 0.0;
    }

    public String getFormattedSummary() {
        return String.format("#%d [%s] %s | f=%.2f, ∂=%.1f%% -> %d DHOPs (%d ms)",
                runId, timestamp, datasetName, f, partial * 100.0, dhopCount, runtimeMs);
    }

    @Override
    public String toString() {
        return getFormattedSummary();
    }
}
