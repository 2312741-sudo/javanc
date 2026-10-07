package vn.edu.dlu.dhopm.model;

import java.util.Locale;

/**
 * Điểm dữ liệu thực nghiệm trong quá trình chạy quét theo từng ngưỡng minSup (minSup Sweep Benchmark).
 * <p>
 * Đại diện cho một điểm trên trục hoành (X-Axis: minSup threshold ∂) của các biểu đồ thực nghiệm
 * trong bài báo EAAI 2026:
 * <ul>
 *   <li><b>Figure 11:</b> Thời gian thực thi (Runtime ms) vs minSup (∂)</li>
 *   <li><b>Figure 6:</b> Số lượng mẫu DHOPs &amp; Tỷ lệ cắt tỉa DUBO vs minSup (∂)</li>
 *   <li><b>Figure 13:</b> Dung lượng bộ nhớ JVM Peak Heap (MB) vs minSup (∂)</li>
 * </ul>
 * </p>
 *
 * @param partial         Tỷ lệ ngưỡng ∂ (0.05, 0.10, 0.15, ...)
 * @param minSupAbsolute  Ngưỡng hỗ trợ tuyệt đối = ∂ × |DB|
 * @param dhopCount       Số lượng mẫu DHOP tìm được
 * @param prunedCount     Số lượng mẫu/nhánh bị cắt tỉa bởi cận trên DUBO
 * @param totalCandidates Tổng số ứng viên DFS duyệt qua
 * @param runtimeMs       Thời gian thực thi (milliseconds)
 * @param peakHeapMb      Bộ nhớ RAM đỉnh tiêu thụ (Megabytes)
 *
 * @author Nguyễn Thanh Tâm (2312741)
 */
public record MinSupSweepPoint(
        double partial,
        double minSupAbsolute,
        int dhopCount,
        int prunedCount,
        int totalCandidates,
        long runtimeMs,
        double peakHeapMb
) {
    public double getPrunedRatio() {
        return totalCandidates > 0 ? (double) prunedCount / totalCandidates : 0.0;
    }

    public String getPartialPercentFormatted() {
        return String.format(Locale.US, "%.1f%%", partial * 100.0);
    }

    public String getMinSupFormatted() {
        return String.format(Locale.US, "%.2f", minSupAbsolute);
    }

    public String getDhopCountFormatted() {
        return String.format(Locale.US, "%,d mẫu", dhopCount);
    }

    public String getPrunedCountFormatted() {
        return String.format(Locale.US, "%,d mẫu", prunedCount);
    }

    public String getPrunedRatioFormatted() {
        return String.format(Locale.US, "%.1f%%", getPrunedRatio() * 100.0);
    }

    public String getTotalCandidatesFormatted() {
        return String.format(Locale.US, "%,d ứng viên", totalCandidates);
    }

    public String getRuntimeFormatted() {
        return String.format(Locale.US, "%,d ms", runtimeMs);
    }

    public String getHeapFormatted() {
        return String.format(Locale.US, "%.1f MB", peakHeapMb);
    }
}
