package vn.edu.dlu.dhopm.bridge;

/**
 * Record lưu trữ thông tin tiến độ khai phá kèm ước tính thời gian thực hiện (ETA).
 *
 * @param fraction              tiến độ hoàn thành từ 0.0 đến 1.0
 * @param elapsedMs             thời gian thực thi đã trôi qua (ms)
 * @param estimatedRemainingMs  thời gian ước tính còn lại (ms)
 * @param patternsFound         số lượng mẫu DHOP đã tìm thấy
 * @param completedRootTasks    số tác vụ root đã hoàn tất
 * @param totalRootTasks        tổng số tác vụ root
 *
 * @author Nguyễn Thanh Tâm (2312741) &amp; Nguyễn Thanh Sơn
 */
public record MiningProgressInfo(
        double fraction,
        long elapsedMs,
        long estimatedRemainingMs,
        int patternsFound,
        long completedRootTasks,
        long totalRootTasks
) {
    /**
     * Định dạng khoảng thời gian milliseconds thành chuỗi trực quan (VD: "12s", "1m 30s").
     */
    public static String formatDuration(long ms) {
        if (ms <= 0) return "< 1s";
        if (ms < 1000) return ms + "ms";
        long totalSecs = ms / 1000;
        if (totalSecs < 60) return totalSecs + "s";
        long mins = totalSecs / 60;
        long secs = totalSecs % 60;
        if (mins < 60) return String.format("%dm %02ds", mins, secs);
        long hours = mins / 60;
        mins = mins % 60;
        return String.format("%dh %02dm %02ds", hours, mins, secs);
    }

    /**
     * Chuỗi thời gian còn lại (ETA).
     */
    public String formatRemainingTime() {
        return formatDuration(estimatedRemainingMs);
    }

    /**
     * Chuỗi thời gian đã chạy.
     */
    public String formatElapsedTime() {
        return formatDuration(elapsedMs);
    }

    /**
     * Trạng thái tiến độ và ước lượng thời gian hiển thị lên UI.
     */
    public String formatEtaStatus() {
        if (fraction >= 1.0) {
            return "Hoàn tất (" + formatElapsedTime() + ")";
        }
        if (fraction <= 0.005) {
            return "Đang ước tính... (" + formatElapsedTime() + ")";
        }
        return String.format(java.util.Locale.US, "Còn ~%s (%.1f%%) • %d mẫu",
                formatRemainingTime(), fraction * 100.0, patternsFound);
    }
}
