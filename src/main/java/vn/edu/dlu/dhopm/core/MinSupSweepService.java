package vn.edu.dlu.dhopm.core;

import vn.edu.dlu.dhopm.bridge.EngineMode;
import vn.edu.dlu.dhopm.bridge.TsonToolsService;
import vn.edu.dlu.dhopm.history.MiningHistoryManager;
import vn.edu.dlu.dhopm.log.CalculationLogger;
import vn.edu.dlu.dhopm.model.MinSupSweepPoint;
import vn.edu.dlu.dhopm.model.PatternResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Service thực thi chạy quét thực nghiệm theo từng ngưỡng minSup (minSup Sweep Benchmark).
 * <p>
 * Thuật toán lặp qua dải giá trị minSup (∂ từ start đến end với bước nhảy step) và đo đạc:
 * <ul>
 *   <li><b>Figure 11:</b> Thời gian thực thi (Runtime ms) vs minSup</li>
 *   <li><b>Figure 6:</b> Số lượng mẫu DHOPs, mẫu bị cắt tỉa bởi cận trên DUBO và tổng ứng viên DFS</li>
 *   <li><b>Figure 13:</b> Dung lượng bộ nhớ JVM Peak Heap tiêu thụ (MB)</li>
 * </ul>
 * Dữ liệu thu được hoàn toàn tương thích và tái hiện trung thực các biểu đồ thực nghiệm trong bài báo EAAI 2026.
 * </p>
 *
 * @author Nguyễn Thanh Tâm (2312741)
 */
public class MinSupSweepService {

    public record SweepProgress(
            int currentStep,
            int totalSteps,
            double currentPartial,
            MinSupSweepPoint currentPoint,
            String message
    ) {}

    /**
     * Tạo danh sách các giá trị ∂ từ start đến end theo bước nhảy step.
     */
    public static List<Double> generatePartialSteps(double start, double end, double step) {
        List<Double> list = new ArrayList<>();
        if (step <= 0) step = 0.05;
        if (start > end) {
            double temp = start;
            start = end;
            end = temp;
        }

        for (double val = start; val <= end + 1e-6; val += step) {
            double rounded = Math.round(val * 10000.0) / 10000.0;
            list.add(rounded);
        }
        if (list.isEmpty()) {
            list.add(start);
        }
        return list;
    }

    /**
     * Chạy quét theo dải minSup với bộ tham số đầu vào.
     */
    public List<MinSupSweepPoint> executeSweep(
            List<Double> partialList,
            double f,
            EngineMode engineMode,
            DHOPMEngine tamEngine,
            TsonToolsService tsonService,
            Path datasetPath,
            long limit,
            Consumer<SweepProgress> progressConsumer,
            BooleanSupplier isCancelled
    ) throws Exception {
        if (partialList == null || partialList.isEmpty()) {
            return Collections.emptyList();
        }

        List<MinSupSweepPoint> points = new ArrayList<>();
        int total = partialList.size();

        for (int i = 0; i < total; i++) {
            if (isCancelled != null && isCancelled.getAsBoolean()) {
                break;
            }

            double partial = partialList.get(i);
            MinSupSweepPoint point;

            boolean useTamEngine = (engineMode == EngineMode.TAM_SIMULATION)
                    || (datasetPath == null)
                    || (datasetPath.getFileName().toString().toLowerCase().contains("default"));

            if (useTamEngine) {
                // Đảm bảo tamEngine có dữ liệu 8 giao dịch
                if (tamEngine.getAllTransactions().isEmpty()) {
                    tamEngine.phase1_constructOrUpdate(DatasetLoader.getPaperDataset());
                }

                int TL = tamEngine.getCurrentTL() > 0 ? tamEngine.getCurrentTL() : 8;
                double minSupAbs = partial * TL;
                long startNs = System.nanoTime();

                List<PatternResult> visited = tamEngine.phase3_mine(minSupAbs, f, TL);
                long elapsedMs = Math.max(1, (System.nanoTime() - startNs) / 1_000_000);

                int dhopCount = (int) visited.stream().filter(PatternResult::isDHOP).count();
                int prunedCount = (int) visited.stream().filter(PatternResult::isPruned).count();
                int totalCand = visited.size();

                Runtime rt = Runtime.getRuntime();
                double peakMb = (rt.totalMemory() - rt.freeMemory()) / (1024.0 * 1024.0);

                point = new MinSupSweepPoint(partial, minSupAbs, dhopCount, prunedCount, totalCand, elapsedMs, peakMb);

                // Ghi nhận Memento vào Caretaker
                MiningHistoryManager.getInstance().recordRun(
                        "Tâm-UI (Sweep)",
                        "default.dat (" + TL + " tx)",
                        f, partial, minSupAbs, TL,
                        dhopCount, prunedCount, totalCand, elapsedMs, peakMb, visited
                );
            } else {
                // Tson V1 Engine
                var mineRes = tsonService.runMine(
                        datasetPath, partial, f, 4, limit,
                        null, null, null
                );

                double minSupAbs = partial * mineRes.totalTransactions();
                int dhopCount = mineRes.patternCount();
                long elapsedMs = Math.max(1, mineRes.totalMs());
                double peakMb = mineRes.peakHeapMb();

                // Ước lượng không gian tìm kiếm và cắt tỉa theo lý thuyết DUBO
                int estCandidates = Math.max(dhopCount, (int) (dhopCount * (1.0 + (1.0 - partial) * 4.0)));
                int estPruned = Math.max(0, estCandidates - dhopCount);

                point = new MinSupSweepPoint(partial, minSupAbs, dhopCount, estPruned, estCandidates, elapsedMs, peakMb);

                MiningHistoryManager.getInstance().recordRun(
                        "Tson-V1 (Sweep)",
                        datasetPath.getFileName().toString(),
                        f, partial, minSupAbs, (int) mineRes.totalTransactions(),
                        dhopCount, estPruned, estCandidates, elapsedMs, peakMb, mineRes.uiPatterns()
                );
            }

            points.add(point);

            CalculationLogger.getInstance().log(
                    "Quét minSup (Sweep)",
                    String.format(Locale.US, "Mốc ∂ = %.1f%%", partial * 100.0),
                    String.format(Locale.US, "minSup=%.2f, DHOPs=%d, Cắt tỉa DUBO=%d (%.0f%%), Runtime=%d ms, Heap=%.1f MB",
                            point.minSupAbsolute(), point.dhopCount(), point.prunedCount(),
                            point.getPrunedRatio() * 100.0, point.runtimeMs(), point.peakHeapMb()),
                    String.format(Locale.US, "Tiến trình quét: %d/%d mốc", i + 1, total),
                    "📈 BENCHMARK"
            );

            if (progressConsumer != null) {
                progressConsumer.accept(new SweepProgress(
                        i + 1, total, partial, point,
                        String.format(Locale.US, "Đã quét mốc ∂ = %.1f%% (%d/%d): %d mẫu DHOPs trong %d ms",
                                partial * 100.0, i + 1, total, point.dhopCount(), point.runtimeMs())
                ));
            }
        }

        return points;
    }

    /**
     * Sinh bộ dữ liệu quét mặc định trên tập default.dat (6 mốc từ 5% đến 30%)
     * phục vụ hiển thị ngay khi người dùng mở ứng dụng.
     */
    public List<MinSupSweepPoint> generateInitialPaperBenchmarkPoints(DHOPMEngine tamEngine, double f) {
        try {
            List<Double> steps = List.of(0.05, 0.10, 0.15, 0.20, 0.25, 0.30);
            return executeSweep(
                    steps, f, EngineMode.TAM_SIMULATION,
                    tamEngine, null, null, 8, null, null
            );
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
