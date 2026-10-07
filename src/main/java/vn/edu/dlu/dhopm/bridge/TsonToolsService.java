package vn.edu.dlu.dhopm.bridge;

import dhopm.common.config.MiningConfig;
import dhopm.common.contract.MineResult;
import dhopm.common.contract.Pattern;
import dhopm.common.contract.Phase;
import dhopm.common.io.FimiTransactionReader;
import dhopm.common.io.TextTransactionReader;
import dhopm.common.io.TransactionSource;
import dhopm.common.testkit.GoldenAssert;
import dhopm.common.testkit.GoldenCase;
import dhopm.common.testkit.GoldenCases;
import dhopm.common.testkit.GoldenRunner;
import dhopm.common.transaction.Transaction;
import dhopm.common.util.TimingRecorder;
import dhopm.v1.engine.MiningEngine;
import vn.edu.dlu.dhopm.model.PatternResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

/**
 * Service cung cấp các tính năng tương ứng bộ lệnh CLI trong README của Tson:
 * <ul>
 *   <li><b>mine</b>: Khai phá dataset FIMI với cấu hình tham số, đo đạc thời gian 3 pha & Peak Heap</li>
 *   <li><b>inspect</b>: Thống kê đặc trưng dataset (số giao dịch, distinct items, avg len, top support)</li>
 *   <li><b>golden</b>: Chạy toàn bộ TestKit TC1–TC8 qua engine V1, kiểm chứng độ chính xác</li>
 *   <li><b>detail</b>: Debug chi tiết từng phần, top-N mẫu theo DO với 6 số lẻ</li>
 * </ul>
 *
 * <p>Toàn bộ các tác vụ có thể chạy không chặn (non-blocking) trên background thread.
 *
 * @author Nguyễn Thanh Tâm (2312741) - Bridge & Tooling Layer
 */
public class TsonToolsService {

    /**
     * Thông tin dataset có sẵn.
     */
    public record DatasetItem(String filename, String displayName, double defaultPartial, String description) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Kết quả thực thi lệnh mine.
     */
    public record MineExecutionResult(
            List<PatternResult> uiPatterns,
            MineResult rawResult,
            long constrMs,
            long reconstMs,
            long miningMs,
            long totalMs,
            double peakHeapMb,
            long totalTransactions,
            int patternCount,
            String logOutput
    ) {}

    /**
     * Kết quả thực thi lệnh inspect.
     */
    public record InspectReport(
            int totalTransactions,
            int lastTid,
            int distinctItems,
            long totalEntries,
            double avgLength,
            int maxLength,
            double minSupAbsolute,
            List<Map.Entry<String, Integer>> topItems,
            String textOutput
    ) {
        public List<TopItemStat> toTopItemStats() {
            List<TopItemStat> stats = new java.util.ArrayList<>();
            if (topItems != null) {
                for (int i = 0; i < topItems.size(); i++) {
                    var e = topItems.get(i);
                    double pct = totalTransactions > 0 ? (100.0 * e.getValue() / totalTransactions) : 0;
                    stats.add(new TopItemStat(i + 1, e.getKey(), e.getValue(), pct));
                }
            }
            return stats;
        }
    }

    /**
     * Danh sách các dataset chuẩn của Tson kèm tham số ∂ khuyến nghị.
     */
    public static List<DatasetItem> getPresetDatasets() {
        return List.of(
            new DatasetItem("default.dat", "default.dat (8 tx - Mẫu bài báo)", 0.15, "8 giao dịch chuẩn từ Bảng 1 của bài báo"),
            new DatasetItem("chess.dat", "chess.dat (3,196 tx - Cờ vua)", 0.35, "3.196 giao dịch, 75 items, avg len 37.0 (∂=35%)"),
            new DatasetItem("mushroom.dat", "mushroom.dat (8,124 tx - Nấm)", 0.06, "8.124 giao dịch, 119 items, avg len 23.0 (∂=6%)"),
            new DatasetItem("retail.dat", "retail.dat (88,162 tx - Bán lẻ)", 0.001, "88.162 giao dịch, 16.470 items, avg len 10.3 (∂=0.1%)"),
            new DatasetItem("connect.dat", "connect.dat (67,557 tx - Connect4)", 0.30, "67.557 giao dịch, 129 items, avg len 43.0 (∂=30%)"),
            new DatasetItem("pumsb.dat", "pumsb.dat (49,046 tx - Census)", 0.30, "49.046 giao dịch, 2.113 items, avg len 74.0 (∂=30%)"),
            new DatasetItem("pumsb_star.dat", "pumsb_star.dat (49,046 tx - Census*)", 0.30, "49.046 giao dịch, 2.088 items, avg len 50.5 (∂=30%)"),
            new DatasetItem("kosarak.dat", "kosarak.dat (990,002 tx - Clickstream)", 0.0005, "990.002 giao dịch, 41.270 items (khuyên dùng limit)")
        );
    }

    /**
     * Nạp giao dịch từ file FIMI hoặc Text, áp dụng giới hạn limit nếu > 0.
     */
    public static List<Transaction> loadTransactions(Path path, long limit) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Không tìm thấy file dataset: " + path);
        }
        String fileName = path.getFileName().toString().toLowerCase();
        TransactionSource source = fileName.endsWith(".txt") ?
                new TextTransactionReader(path) : new FimiTransactionReader(path);

        List<Transaction> list = new ArrayList<>();
        long remaining = limit;
        while (source.hasNext() && (limit == 0 || remaining-- > 0)) {
            if (Thread.currentThread().isInterrupted()) {
                throw new java.util.concurrent.CancellationException("Đã dừng đọc dữ liệu.");
            }
            list.add(source.next());
        }
        return list;
    }

    /**
     * Thực thi lệnh "inspect": Thống kê dataset mà không cần mining.
     */
    public InspectReport runInspect(Path path, double partial, long limit, int topN) throws IOException {
        List<Transaction> transactions = loadTransactions(path, limit);

        long entries = 0;
        int maxLen = 0;
        Map<String, Integer> freq = new HashMap<>();
        int lastTid = 0;

        for (Transaction t : transactions) {
            entries += t.length();
            maxLen = Math.max(maxLen, t.length());
            lastTid = Math.max(lastTid, t.tid());
            for (String item : t.items()) {
                freq.merge(item, 1, Integer::sum);
            }
        }

        double avgLen = transactions.isEmpty() ? 0 : (double) entries / transactions.size();
        double minSupAbs = partial * transactions.size();

        List<Map.Entry<String, Integer>> topItems = freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(Math.max(1, topN))
                .toList();

        String formattedOutput = TableFormatter.formatInspectReport(
                path.getFileName().toString(),
                transactions.size(),
                lastTid,
                freq.size(),
                entries,
                avgLen,
                maxLen,
                partial,
                minSupAbs,
                topItems,
                limit
        );

        return new InspectReport(transactions.size(), lastTid, freq.size(), entries, avgLen, maxLen, minSupAbs, topItems, formattedOutput);
    }

    /**
     * Thực thi lệnh "golden": Chạy toàn bộ TestKit TC1–TC8 với bảng chia cột đẹp mắt.
     */
    public String runGoldenTestKit() {
        List<GoldenCase> cases = GoldenCases.all();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("╔══════════════════════════════════════════════════════════════════════════════════════════════════════╗%n"));
        sb.append(String.format("║                         BẢNG KIỂM ĐỊNH BỘ BÀI TOÁN VÀNG TESTKIT (TC1 - TC8)                          ║%n"));
        sb.append(String.format("║  Dung sai so sánh: %.1E (Khớp tuyệt đối số liệu tính tay Lab 1 & Bài báo Cho et al. 2026)             ║%n", GoldenAssert.GOLDEN_TOLERANCE));
        sb.append(String.format("╚══════════════════════════════════════════════════════════════════════════════════════════════════════╝%n"));

        sb.append("┌──────┬───────┬────────┬────────┬─────────────────────────┬─────────────────────────┬──────────────┐\n");
        sb.append(String.format("│ %-4s │ %-5s │ %-6s │ %-6s │ %-23s │ %-23s │ %-12s │%n",
                "Case", "f", "∂", "minSup", "Kỳ vọng (Bài báo)", "Thực nghiệm (Tson Engine)", "Trạng thái"));
        sb.append("├──────┼───────┼────────┼────────┼─────────────────────────┼─────────────────────────┼──────────────┤\n");

        boolean allOk = true;
        String[] expectedSummaries = {
            "2 mẫu: AE, F",
            "0 mẫu (trống)",
            "15 mẫu DHOPs",
            "0 mẫu (f=0.8 suy giảm)",
            "9 mẫu (f=1.0 không suy)",
            "3 mẫu (DB0, 4 TID)",
            "9 mẫu (10 TID tùy chỉnh)",
            "1 mẫu: A (1 item/TID)"
        };

        for (int i = 0; i < cases.size(); i++) {
            GoldenCase c = cases.get(i);
            StringBuilder caseReport = new StringBuilder();
            boolean ok;
            int foundPatterns = 0;
            try (MiningEngine engine = new MiningEngine(MiningConfig.of(c.partial(), c.decayFactor()))) {
                ok = GoldenRunner.run(engine, c, GoldenAssert.GOLDEN_TOLERANCE, caseReport);
                foundPatterns = engine.mineNow().patterns().size();
            } catch (Exception e) {
                ok = false;
                caseReport.append("Lỗi: ").append(e.getMessage());
            }
            allOk &= ok;
            String statusBadge = ok ? "✅ PASS 100%" : "❌ FAIL";
            String exp = i < expectedSummaries.length ? expectedSummaries[i] : "Đạt chuẩn";
            String actual = foundPatterns + " mẫu tìm thấy";

            double effectiveN = (i == 5 ? 4 : (i == 6 ? 10 : (i == 7 ? 5 : 8)));
            sb.append(String.format("│ TC%-2d │ %5.2f │ %5.1f%% │ %6.2f │ %-23s │ %-23s │ %-12s │%n",
                    i + 1, c.decayFactor(), c.partial() * 100, c.partial() * effectiveN,
                    exp, actual, statusBadge));
        }

        sb.append("└──────┴───────┴────────┴────────┴─────────────────────────┴─────────────────────────┴──────────────┘\n");
        if (allOk) {
            sb.append(String.format("  >>> KẾT LUẬN: TOÀN BỘ %d BỘ TEST VÀNG ĐẠT CHUẨN XÁC SUẤT ĐÚNG 100%% SO VỚI BÀI BÁO! <<<%n", cases.size()));
        } else {
            sb.append(String.format("  >>> CẢNH BÁO: CÓ TEST CASE CHƯA ĐẠT CHUẨN! <<<%n"));
        }
        return sb.toString();
    }

    /**
     * Chạy kiểm thử TC1-TC8 và trả về danh sách đối tượng GoldenRow cho UI TableView.
     */
    public List<GoldenRow> runGoldenTestKitRows() {
        List<GoldenCase> cases = GoldenCases.all();
        List<GoldenRow> rows = new java.util.ArrayList<>(cases.size());

        String[] expectedSummaries = {
            "2 mẫu: AE, F",
            "0 mẫu (trống)",
            "15 mẫu DHOPs",
            "0 mẫu (f=0.8 suy giảm)",
            "9 mẫu (f=1.0 không suy)",
            "3 mẫu (DB0, 4 TID)",
            "9 mẫu (10 TID tùy chỉnh)",
            "1 mẫu: A (1 item/TID)"
        };

        for (int i = 0; i < cases.size(); i++) {
            GoldenCase c = cases.get(i);
            StringBuilder caseReport = new StringBuilder();
            boolean ok;
            int foundPatterns = 0;
            try (MiningEngine engine = new MiningEngine(MiningConfig.of(c.partial(), c.decayFactor()))) {
                ok = GoldenRunner.run(engine, c, GoldenAssert.GOLDEN_TOLERANCE, caseReport);
                foundPatterns = engine.mineNow().patterns().size();
            } catch (Exception e) {
                ok = false;
            }
            String exp = i < expectedSummaries.length ? expectedSummaries[i] : "Đạt chuẩn";
            String actual = foundPatterns + " mẫu";
            double effectiveN = (i == 5 ? 4 : (i == 6 ? 10 : (i == 7 ? 5 : 8)));
            double minSup = c.partial() * effectiveN;

            rows.add(new GoldenRow(
                "TC" + (i + 1),
                c.decayFactor(),
                c.partial(),
                minSup,
                exp,
                actual,
                ok ? "✅ PASS 100%" : "❌ FAIL",
                ok
            ));
        }
        return rows;
    }

    /**
     * Thực thi lệnh "mine": Khai phá dataset FIMI với cấu hình tham số (tương thích ngược).
     */
    public MineExecutionResult runMine(
            Path path,
            double partial,
            double f,
            int workers,
            long limit,
            Consumer<String> phaseLogConsumer,
            Consumer<Double> progressConsumer
    ) throws IOException {
        return runMine(
                path, partial, f, workers, limit,
                phaseLogConsumer,
                progressConsumer != null ? info -> progressConsumer.accept(info.fraction()) : null,
                null
        );
    }

    /**
     * Thực thi lệnh "mine" kèm tính toán thời gian ước tính (ETA) và hỗ trợ dừng (cancel).
     */
    public MineExecutionResult runMine(
            Path path,
            double partial,
            double f,
            int workers,
            long limit,
            Consumer<String> phaseLogConsumer,
            Consumer<MiningProgressInfo> progressInfoConsumer,
            Consumer<MiningEngine> engineConsumer
    ) throws IOException {
        StringBuilder log = new StringBuilder();
        List<Transaction> transactions = loadTransactions(path, limit);

        MiningConfig config = new MiningConfig(partial, f, MiningConfig.DEFAULT_EPSILON, workers);
        MineResult result;
        TimingRecorder recorder = new TimingRecorder();

        try (MiningEngine engine = new MiningEngine(config)) {
            if (engineConsumer != null) {
                engineConsumer.accept(engine);
            }

            engine.setPhaseListener((phase, startNs, endNs) -> {
                recorder.onPhase(phase, startNs, endNs);
                double ms = (endNs - startNs) / 1_000_000.0;
                String msg = String.format("Pha %s hoàn thành: %.2f ms", phase, ms);
                if (phaseLogConsumer != null) phaseLogConsumer.accept(msg);
            });

            if (progressInfoConsumer != null) {
                engine.setMiningProgressListener(p -> {
                    double frac = p.fraction();
                    long elapsed = p.elapsedMs();
                    long remainingMs = 0;
                    if (frac > 0.005) {
                        long totalEstimateMs = (long) (elapsed / frac);
                        remainingMs = Math.max(0, totalEstimateMs - elapsed);
                    }
                    MiningProgressInfo info = new MiningProgressInfo(
                            frac, elapsed, remainingMs, p.patternsFound(),
                            p.completedRootTasks(), p.totalRootTasks()
                    );
                    progressInfoConsumer.accept(info);
                });
            }

            if (Thread.currentThread().isInterrupted()) {
                throw new java.util.concurrent.CancellationException("Đã dừng trước khi nạp batch.");
            }

            engine.loadBatch(transactions);

            if (Thread.currentThread().isInterrupted()) {
                throw new java.util.concurrent.CancellationException("Đã dừng trước khi khai phá.");
            }

            result = engine.mineNow();
        } catch (Exception ex) {
            if (Thread.currentThread().isInterrupted() || ex instanceof InterruptedException ||
                    (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("interrupted"))) {
                throw new java.util.concurrent.CancellationException("Đã dừng khai phá theo yêu cầu.");
            }
            throw ex;
        }

        long cMs = recorder.phaseMs(Phase.CONSTRUCTION);
        long rMs = recorder.phaseMs(Phase.RECONSTRUCTION);
        long mMs = recorder.phaseMs(Phase.MINING);
        long totalMs = recorder.totalMs();
        double peakHeapMb = recorder.peakHeapBytes() / (1024.0 * 1024.0);

        String summaryTable = TableFormatter.formatMineReport(
                path.getFileName().toString(),
                partial,
                f,
                workers,
                limit,
                result.totalTransactions(),
                result.lastTid(),
                cMs,
                rMs,
                mMs,
                totalMs,
                peakHeapMb,
                result.patterns().size(),
                result.minSup()
        );
        log.append(summaryTable);

        // Chuyển đổi sang List<PatternResult> của Tâm UI
        List<PatternResult> uiPatterns = new ArrayList<>(result.patterns().size());
        double absMinSup = partial * result.totalTransactions();

        for (Pattern p : result.patterns()) {
            String patternStr = p.canonicalKey();
            double doVal = p.dampedOccupancy();
            int sup = p.tids().length;
            List<Integer> tids = new ArrayList<>(sup);
            for (int t : p.tids()) tids.add(t);

            uiPatterns.add(new PatternResult(
                patternStr,
                doVal,
                doVal,
                sup,
                doVal >= absMinSup,
                false,
                tids
            ));
        }

        // Sắp xếp DO giảm dần
        uiPatterns.sort((a, b) -> Double.compare(b.doValue(), a.doValue()));

        return new MineExecutionResult(
            uiPatterns,
            result,
            cMs,
            rMs,
            mMs,
            totalMs,
            peakHeapMb,
            result.totalTransactions(),
            result.patterns().size(),
            log.toString()
        );
    }
}
