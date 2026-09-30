package vn.edu.dlu.dhopm.bridge;

/**
 * Model biểu diễn một dòng trong bảng thống kê Top Items của Inspect.
 */
public record TopItemStat(int rank, String item, int support, double percentage) {
    public String getFormattedPercentage() {
        return String.format("%.2f%%", percentage);
    }

    public String getFormattedSupport() {
        return String.format("%,d tx", support);
    }
}
