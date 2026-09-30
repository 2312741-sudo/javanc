package vn.edu.dlu.dhopm.bridge;

import vn.edu.dlu.dhopm.model.PatternResult;

import java.util.List;
import java.util.Map;

/**
 * Tiện ích định dạng bảng dữ liệu chia cột (Box-Drawing Tables)
 * giúp toàn bộ dữ liệu xuất ra console đều ngay hàng thẳng lối, dễ nhìn,
 * không bị xô lệch khi tên mẫu quá dài.
 *
 * @author Nguyễn Thanh Tâm (2312741)
 */
public final class TableFormatter {

    private TableFormatter() {}

    /**
     * Định dạng bảng chi tiết Top-N mẫu theo DO (lệnh Detail).
     */
    public static String formatDetailTable(
            String datasetName,
            double partial,
            double f,
            int totalPatterns,
            List<PatternResult> topPatterns
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("╔══════════════════════════════════════════════════════════════════════════════════════════════════════╗%n"));
        sb.append(String.format("║                         BẢNG CHI TIẾT TOP MẪU CÓ ĐỘ CHIẾM DỤNG CAO NHẤT (DETAIL)                     ║%n"));
        sb.append(String.format("║  Tập tin: %-18s | Ngưỡng ∂: %-6.2f%% | Hệ số f: %-4.2f | Tổng số mẫu tìm thấy: %,6d mẫu     ║%n",
                datasetName, partial * 100, f, totalPatterns));
        sb.append(String.format("╚══════════════════════════════════════════════════════════════════════════════════════════════════════╝%n"));

        if (topPatterns == null || topPatterns.isEmpty()) {
            sb.append("  (Không có mẫu nào đạt ngưỡng minSup với cấu hình tham số hiện tại)\n");
            return sb.toString();
        }

        // 1. Tính toán độ rộng động cho cột Itemset để không bao giờ bị xô lệch
        int patternColWidth = "Mẫu mục chiếm dụng (Itemset X)".length();
        for (PatternResult p : topPatterns) {
            String formattedPattern = formatItemset(p.pattern());
            if (formattedPattern.length() > patternColWidth) {
                patternColWidth = formattedPattern.length();
            }
        }
        patternColWidth = Math.max(patternColWidth + 2, 34);

        // 2. Vẽ đường viền trên
        String borderTop = "┌─────┬─" + "─".repeat(patternColWidth) + "─┬─────────────┬─────────────┬─────────────┐";
        String borderMid = "├─────┼─" + "─".repeat(patternColWidth) + "─┼─────────────┼─────────────┼─────────────┤";
        String borderBot = "└─────┴─" + "─".repeat(patternColWidth) + "─┴─────────────┴─────────────┴─────────────┘";

        sb.append(borderTop).append("\n");
        sb.append(String.format("│ %-3s │ %-" + patternColWidth + "s │ %-11s │ %-11s │ %-11s │%n",
                "STT", "Mẫu mục chiếm dụng (Itemset X)", "Độ đo DO(X)", "Support", "Độ dài |X|"));
        sb.append(borderMid).append("\n");

        for (int i = 0; i < topPatterns.size(); i++) {
            PatternResult p = topPatterns.get(i);
            String itemset = formatItemset(p.pattern());
            int length = countItems(p.pattern());
            sb.append(String.format("│ %3d │ %-" + patternColWidth + "s │ %11.6f │ %,8d tx │ %6d mục │%n",
                    i + 1, itemset, p.doValue(), p.support(), length));
        }

        sb.append(borderBot).append("\n");
        sb.append(String.format("  * Ghi chú: DO(X) được sắp xếp giảm dần | Hiển thị %d / %,d mẫu tốt nhất.%n",
                topPatterns.size(), totalPatterns));
        return sb.toString();
    }

    /**
     * Định dạng bảng thống kê đặc trưng tập dữ liệu (lệnh Inspect).
     */
    public static String formatInspectReport(
            String datasetName,
            long totalTx,
            int lastTid,
            int distinctItems,
            long totalEntries,
            double avgLen,
            int maxLen,
            double partial,
            double minSupAbs,
            List<Map.Entry<String, Integer>> topItems,
            long limit
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("╔══════════════════════════════════════════════════════════════════════════════════════════════════════╗%n"));
        sb.append(String.format("║                         BẢNG THỐNG KÊ ĐẶC TRƯNG TẬP DỮ LIỆU FIMI (INSPECT)                           ║%n"));
        sb.append(String.format("╚══════════════════════════════════════════════════════════════════════════════════════════════════════╝%n"));

        sb.append("┌───────────────────────────────────┬──────────────────────────────────────────────────────────────────┐\n");
        sb.append(String.format("│ %-33s │ %-64s │%n", "Thuộc tính tập dữ liệu", "Giá trị thống kê"));
        sb.append("├───────────────────────────────────┼──────────────────────────────────────────────────────────────────┤\n");
        sb.append(String.format("│ %-33s │ %-64s │%n", "Tên tập tin (Dataset)", datasetName));
        sb.append(String.format("│ %-33s │ %,12d giao dịch %-35s │%n", "Tổng số giao dịch đã đọc", totalTx, limit > 0 ? "(giới hạn " + limit + " tx)" : "(đọc toàn bộ file)"));
        sb.append(String.format("│ %-33s │ TID = %-56d │%n", "Transaction ID mới nhất (TL)", lastTid));
        sb.append(String.format("│ %-33s │ %,12d mục khác biệt %-32s │%n", "Số lượng mặt hàng (Distinct Items)", distinctItems, ""));
        sb.append(String.format("│ %-33s │ %,12d phần tử %-37s │%n", "Tổng số mục trong toàn CSDL", totalEntries, ""));
        sb.append(String.format("│ %-33s │ %12.2f mục / giao dịch %-31s │%n", "Độ dài giao dịch trung bình", avgLen, ""));
        sb.append(String.format("│ %-33s │ %12d mục %-45s │%n", "Giao dịch dài nhất (Max Length)", maxLen, ""));
        sb.append(String.format("│ %-33s │ ∂ = %-6.2f%% -> minSup = %,.2f %-30s │%n", "Ngưỡng hỗ trợ tối thiểu", partial * 100, minSupAbs, ""));
        sb.append("└───────────────────────────────────┴──────────────────────────────────────────────────────────────────┘\n");

        if (topItems != null && !topItems.isEmpty()) {
            sb.append("\n┌──────┬──────────────────────────────────────────┬─────────────────────────┬──────────────────────────┐\n");
            sb.append(String.format("│ %-4s │ %-40s │ %-23s │ %-24s │%n", "Hạng", "Mã mặt hàng (Item ID / Name)", "Tần số xuất hiện (Sup)", "Tỷ lệ xuất hiện (%)"));
            sb.append("├──────┼──────────────────────────────────────────┼─────────────────────────┼──────────────────────────┤\n");
            for (int i = 0; i < topItems.size(); i++) {
                Map.Entry<String, Integer> e = topItems.get(i);
                double pct = totalTx > 0 ? (100.0 * e.getValue() / totalTx) : 0;
                sb.append(String.format("│  %2d  │ Mục %-36s │ %,15d tx     │ %18.2f %%     │%n",
                        i + 1, "'" + e.getKey() + "'", e.getValue(), pct));
            }
            sb.append("└──────┴──────────────────────────────────────────┴─────────────────────────┴──────────────────────────┘\n");
        }

        return sb.toString();
    }

    /**
     * Định dạng bảng tổng kết khai phá (lệnh Mine).
     */
    public static String formatMineReport(
            String datasetName,
            double partial,
            double f,
            int workers,
            long limit,
            long totalTx,
            int lastTid,
            long cMs,
            long rMs,
            long mMs,
            long totalMs,
            double peakHeapMb,
            int patternCount,
            double minSup
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("╔══════════════════════════════════════════════════════════════════════════════════════════════════════╗%n"));
        sb.append(String.format("║                       BÁO CÁO KẾT QUẢ KHAI PHÁ HIỆU NĂNG CAO TSON V1 STANDARD                        ║%n"));
        sb.append(String.format("╚══════════════════════════════════════════════════════════════════════════════════════════════════════╝%n"));

        sb.append("┌──────────────────────────────────────────────┬───────────────────────────────────────────────────────┐\n");
        sb.append(String.format("│ %-44s │ %-53s │%n", "Thông số thực nghiệm", "Số liệu đo đạc thực tế"));
        sb.append("├──────────────────────────────────────────────┼───────────────────────────────────────────────────────┤\n");
        sb.append(String.format("│ %-44s │ %-53s │%n", "Tập dữ liệu FIMI", datasetName));
        sb.append(String.format("│ %-44s │ %,12d giao dịch %-30s │%n", "Quy mô dữ liệu nạp vào", totalTx, limit > 0 ? "(limit=" + limit + ")" : "(toàn bộ)"));
        sb.append(String.format("│ %-44s │ TID = %-45d │%n", "Transaction ID cuối cùng (TL)", lastTid));
        sb.append(String.format("│ %-44s │ f = %-49.2f │%n", "Hệ số suy giảm thời gian (Decay Factor)", f));
        sb.append(String.format("│ %-44s │ ∂ = %-6.2f%%  (minSup tuyệt đối = %,.2f) %-15s │%n", "Ngưỡng chiếm dụng tối thiểu", partial * 100, minSup, ""));
        sb.append(String.format("│ %-44s │ %-53s │%n", "Số nhân luồng xử lý song song", workers + " CPU Worker Threads"));
        sb.append("├──────────────────────────────────────────────┼───────────────────────────────────────────────────────┤\n");
        sb.append(String.format("│ %-44s │ %,12d ms %-35s │%n", "Pha 1 - Xây dựng DHO-List (Construction)", cMs, ""));
        sb.append(String.format("│ %-44s │ %,12d ms %-35s │%n", "Pha 2 - Tái cấu trúc DO (Reconstruction)", rMs, ""));
        sb.append(String.format("│ %-44s │ %,12d ms %-35s │%n", "Pha 3 - Khai phá cây DFS (Mining)", mMs, ""));
        sb.append(String.format("│ %-44s │ %,12d ms (%.3f giây) %-25s │%n", "TỔNG THỜI GIAN THỰC THI (TOTAL RUNTIME)", totalMs, totalMs / 1000.0, ""));
        sb.append(String.format("│ %-44s │ %12.2f MB %-35s │%n", "Bộ nhớ tiêu thụ đỉnh điểm (Peak Heap RAM)", peakHeapMb, ""));
        double throughput = totalMs > 0 ? (totalTx * 1000.0) / totalMs : 0;
        sb.append(String.format("│ %-44s │ %,12.0f giao dịch / giây %-23s │%n", "Tốc độ xử lý thông lượng (Throughput)", throughput, ""));
        sb.append("├──────────────────────────────────────────────┼───────────────────────────────────────────────────────┤\n");
        sb.append(String.format("│ %-44s │ %,12d mẫu DHOP hợp lệ %-23s │%n", "KẾT QUẢ: TỔNG SỐ MẪU DHOP TÌM ĐƯỢC", patternCount, ""));
        sb.append("└──────────────────────────────────────────────┴───────────────────────────────────────────────────────┘\n");

        return sb.toString();
    }

    /**
     * Chuẩn hóa chuỗi pattern thành dạng tập hợp đẹp {A, B, C} thay vì A,B,C dính chùm.
     */
    private static String formatItemset(String raw) {
        if (raw == null || raw.isBlank()) return "{}";
        String clean = raw.trim();
        if (clean.startsWith("{") && clean.endsWith("}")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        String[] parts = clean.split("[,\\s]+");
        return "{" + String.join(", ", parts) + "}";
    }

    private static int countItems(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        String clean = raw.replace("{", "").replace("}", "").trim();
        return clean.split("[,\\s]+").length;
    }
}
