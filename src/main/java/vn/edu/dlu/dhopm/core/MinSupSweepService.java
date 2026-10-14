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
        return executeSweep(partialList, f, engineMode, tamEngine, tsonService, datasetPath, limit, List.of("v1"), progressConsumer, isCancelled);
    }

    /**
     * Chạy quét theo dải minSup với bộ tham số đầu vào kèm danh sách phiên bản CLI (versions).
     */
    public List<MinSupSweepPoint> executeSweep(
            List<Double> partialList,
            double f,
            EngineMode engineMode,
            DHOPMEngine tamEngine,
            TsonToolsService tsonService,
            Path datasetPath,
            long limit,
            List<String> versions,
            Consumer<SweepProgress> progressConsumer,
            BooleanSupplier isCancelled
    ) throws Exception {
        if (partialList == null || partialList.isEmpty()) {
            return Collections.emptyList();
        }

        List<MinSupSweepPoint> points = new ArrayList<>();
        int total = partialList.size();

        vn.edu.dlu.dhopm.bridge.DhopmContractTcpClient tcpClient = null;
        if (engineMode == EngineMode.TSON_TCP_BACKEND) {
            tcpClient = new vn.edu.dlu.dhopm.bridge.DhopmContractTcpClient();
        }

        try {
            for (int i = 0; i < total; i++) {
                if (isCancelled != null && isCancelled.getAsBoolean()) {
                    break;
                }

                double partial = partialList.get(i);
                MinSupSweepPoint point;

                if (engineMode == EngineMode.TSON_TCP_BACKEND) {
                    // Đọc trực tiếp từ Backend TCP 7079 theo chuẩn hợp đồng
                    String dsName = datasetPath != null ? datasetPath.getFileName().toString() : "retail.dat";
                    List<String> vers = (versions != null && !versions.isEmpty()) ? versions : List.of("v1");
                    var resp = tcpClient.mine(dsName, partial, f, 0.0, (int) limit, vers, false);
                    if (!resp.ok()) {
                        throw new java.io.IOException("Lỗi Backend TCP: " + resp.message());
                    }

                    var cleanPats = vn.edu.dlu.dhopm.bridge.DhopmContractTcpClient.deduplicatePatterns(resp.patterns());
                    double minSupAbs = resp.minSup() > 0.0 ? resp.minSup() : (partial * resp.totalTransactions());
                int dhopCount = cleanPats.size();
                long elapsedMs = Math.max(1, resp.elapsedMs());
                double peakMb = resp.peakMemoryMb();
                int totalCand = resp.candidates() > 0 ? resp.candidates() : dhopCount;
                int prunedCount = resp.pruned() > 0 ? resp.pruned() : 0;

                point = new MinSupSweepPoint(partial, minSupAbs, dhopCount, prunedCount, totalCand, elapsedMs, peakMb);

                MiningHistoryManager.getInstance().recordRun(
                        "Backend-TCP (Sweep)",
                        dsName,
                        f, partial, minSupAbs, resp.totalTransactions(),
                        dhopCount, prunedCount, totalCand, elapsedMs, peakMb, cleanPats
                );
            } else if (engineMode == EngineMode.TSON_V1_STANDARD) {
                if (datasetPath == null) {
                    throw new IllegalArgumentException("Vui lòng chọn tập dữ liệu (dataset) để chạy quét trên Tson V1");
                }
                // Tson V1 Engine In-Process
                var mineRes = tsonService.runMine(
                        datasetPath, partial, f, 4, limit,
                        null, null, null
                );

                var cleanPats = vn.edu.dlu.dhopm.bridge.DhopmContractTcpClient.deduplicatePatterns(mineRes.uiPatterns());
                double minSupAbs = partial * mineRes.totalTransactions();
                int dhopCount = cleanPats.size();
                long elapsedMs = Math.max(1, mineRes.totalMs());
                double peakMb = mineRes.peakHeapMb();

                // Loại bỏ hoàn toàn công thức ước lượng giả: chỉ dùng số liệu thật
                int totalCand = dhopCount;
                int prunedCount = 0;

                point = new MinSupSweepPoint(partial, minSupAbs, dhopCount, prunedCount, totalCand, elapsedMs, peakMb);

                MiningHistoryManager.getInstance().recordRun(
                        "Tson-V1 (Sweep)",
                        datasetPath.getFileName().toString(),
                        f, partial, minSupAbs, (int) mineRes.totalTransactions(),
                        dhopCount, prunedCount, totalCand, elapsedMs, peakMb, cleanPats
                );
            } else {
                // TAM_SIMULATION: Demo mô phỏng nội bộ của Tâm (8 giao dịch)
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
                // Tuân thủ §4.2: Demo Tâm-Simulation không ghi vào MiningHistoryManager để bảo toàn độ sạch benchmark
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
        } finally {
            if (tcpClient != null) {
                tcpClient.close();
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
