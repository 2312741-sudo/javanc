package vn.edu.dlu.dhopm.bridge;

/**
 * Model biểu diễn một dòng đối chiếu trong bảng kiểm định TestKit Vàng (TC1 - TC8).
 */
public record GoldenRow(
        String caseName,
        double f,
        double partial,
        double minSup,
        String expected,
        String actual,
        String status,
        boolean passed
) {
    public String getFormattedF() {
        return String.format("%.2f", f);
    }

    public String getFormattedPartial() {
        return String.format("%.1f%%", partial * 100);
    }

    public String getFormattedMinSup() {
        return String.format("%.2f", minSup);
    }
}
