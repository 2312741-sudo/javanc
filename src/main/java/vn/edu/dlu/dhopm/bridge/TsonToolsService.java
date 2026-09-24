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
    ) {}

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

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("================================================================%n"));
        sb.append(String.format("            THỐNG KÊ ĐẶC TRƯNG TẬP DỮ LIỆU (INSPECT)            %n"));
        sb.append(String.format("================================================================%n"));
        sb.append(String.format("  Tập tin         : %s%n", path.getFileName()));
        sb.append(String.format("  Tổng giao dịch  : %,d %s%n", transactions.size(), limit > 0 ? "(giới hạn " + limit + ")" : "(toàn bộ)"));
        sb.append(String.format("  Last TID        : %d%n", lastTid));
        sb.append(String.format("  Distinct Items  : %,d mục%n", freq.size()));
        sb.append(String.format("  Tổng entries    : %,d phần tử%n", entries));
        sb.append(String.format("  Độ dài trung bình: %.2f items/giao dịch%n", avgLen));
        sb.append(String.format("  Độ dài tối đa   : %d items%n", maxLen));
        sb.append(String.format("  Ngưỡng minSup   : ∂=%.4f * N = %.2f%n", partial, minSupAbs));
        sb.append(String.format("----------------------------------------------------------------%n"));
        sb.append(String.format("  TOP %d ITEMS THEO TẦN SỐ XUẤT HIỆN (SUPPORT):%n", topItems.size()));
        for (int i = 0; i < topItems.size(); i++) {
            Map.Entry<String, Integer> e = topItems.get(i);
            double pct = 100.0 * e.getValue() / transactions.size();
            sb.append(String.format("    [%2d] Mục '%-15s': support = %,6d (%6.2f%%)%n", i + 1, e.getKey(), e.getValue(), pct));
        }
        sb.append(String.format("================================================================%n"));

        return new InspectReport(transactions.size(), lastTid, freq.size(), entries, avgLen, maxLen, minSupAbs, topItems, sb.toString());
    }

    /**
     * Thực thi lệnh "golden": Chạy toàn bộ TestKit TC1–TC8.
     */
    public String runGoldenTestKit() {
        List<GoldenCase> cases = GoldenCases.all();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("================================================================%n"));
        sb.append(String.format("      CHẠY KIỂM ĐỊNH BỘ BÀI TOÁN VÀNG TESTKIT (TC1 - TC8)       %n"));
        sb.append(String.format("      Dung sai so sánh: %.1E (Độ lệch chuẩn bài báo)          %n", GoldenAssert.GOLDEN_TOLERANCE));
        sb.append(String.format("================================================================%n"));

        boolean allOk = true;
        for (int i = 0; i < cases.size(); i++) {
            GoldenCase c = cases.get(i);
            StringBuilder caseReport = new StringBuilder();
            boolean ok;
            try (MiningEngine engine = new MiningEngine(MiningConfig.of(c.partial(), c.decayFactor()))) {
                ok = GoldenRunner.run(engine, c, GoldenAssert.GOLDEN_TOLERANCE, caseReport);
            } catch (Exception e) {
                ok = false;
                caseReport.append("Lỗi ngoại lệ: ").append(e.getMessage()).append("\n");
            }
            allOk &= ok;
            String statusBadge = ok ? "✅ [PASS]" : "❌ [FAIL]";
            sb.append(String.format("  TC%d: f=%.2f, ∂=%.2f -> %s%n", i + 1, c.decayFactor(), c.partial(), statusBadge));
            if (!caseReport.isEmpty()) {
                String[] lines = caseReport.toString().split("\n");
                for (String line : lines) {
                    if (!line.isBlank()) sb.append("     ").append(line).append("\n");
                }
            }
        }
        sb.append(String.format("----------------------------------------------------------------%n"));
        if (allOk) {
            sb.append(String.format("  KẾT LUẬN: TOÀN BỘ %d BỘ TEST VÀNG ĐẠT CHUẨN 100%% TUYỆT ĐỐI!%n", cases.size()));
        } else {
            sb.append(String.format("  CẢNH BÁO: CÓ TEST CASE CHƯA ĐẠT!%n"));
        }
        sb.append(String.format("================================================================%n"));
        return sb.toString();
    }

    /**
     * Thực thi lệnh "mine": Khai phá dataset FIMI với cấu hình tham số.
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
        StringBuilder log = new StringBuilder();
        log.append(String.format("=== BẮT ĐẦU KHAI PHÁ TSON V1 STANDARD ===%n"));
        log.append(String.format("  Tập tin   : %s%n", path.getFileName()));
        log.append(String.format("  Tham số   : ∂=%.4f (%.2f%%), f=%.2f, workers=%d%n", partial, partial * 100, f, workers));
        if (limit > 0) {
            log.append(String.format("  Giới hạn  : %,d giao dịch đầu%n", limit));
        }

        List<Transaction> transactions = loadTransactions(path, limit);
        log.append(String.format("  Đã nạp    : %,d giao dịch vào bộ nhớ%n", transactions.size()));

        MiningConfig config = new MiningConfig(partial, f, MiningConfig.DEFAULT_EPSILON, workers);
        MineResult result;
        TimingRecorder recorder = new TimingRecorder();

        try (MiningEngine engine = new MiningEngine(config)) {
            engine.setPhaseListener((phase, startNs, endNs) -> {
                recorder.onPhase(phase, startNs, endNs);
                double ms = (endNs - startNs) / 1_000_000.0;
                String msg = String.format("Pha %s hoàn thành: %.2f ms", phase, ms);
                if (phaseLogConsumer != null) phaseLogConsumer.accept(msg);
            });

            if (progressConsumer != null) {
                engine.setMiningProgressListener(p -> progressConsumer.accept(p.fraction()));
            }

            engine.loadBatch(transactions);
            result = engine.mineNow();
        }

        long cMs = recorder.phaseMs(Phase.CONSTRUCTION);
        long rMs = recorder.phaseMs(Phase.RECONSTRUCTION);
        long mMs = recorder.phaseMs(Phase.MINING);
        long totalMs = recorder.totalMs();
        double peakHeapMb = recorder.peakHeapBytes() / (1024.0 * 1024.0);

        log.append(String.format("------------------------------------------------%n"));
        log.append(String.format("  Pha 1 (Construction)  : %,d ms%n", cMs));
        log.append(String.format("  Pha 2 (Reconstruction): %,d ms%n", rMs));
        log.append(String.format("  Pha 3 (Mining DFS)     : %,d ms%n", mMs));
        log.append(String.format("  Tổng thời gian         : %,d ms (%.2f giây)%n", totalMs, totalMs / 1000.0));
        log.append(String.format("  Peak Heap RAM          : %.2f MB%n", peakHeapMb));
        log.append(String.format("  Số mẫu DHOP tìm được   : %,d mẫu%n", result.patterns().size()));
        log.append(String.format("================================================%n"));

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
