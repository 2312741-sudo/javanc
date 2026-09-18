package vn.edu.dlu.dhopm.model;

import java.util.List;

/**
 * Bieu dien ket qua khai pha cua mot mau ung vien trong DHOPM.
 */
public record PatternResult(
    String pattern,
    double doValue,
    double duboValue,
    int support,
    boolean isDHOP,
    boolean isPruned,
    List<Integer> transactionIds
) {
    public String getFormattedDO() {
        return String.format("%.4f", doValue);
    }

    public String getFormattedDUBO() {
        return String.format("%.4f", duboValue);
    }

    public String getStatus() {
        if (isDHOP) {
            return "DHOP";
        } else if (isPruned) {
            return "Cắt tỉa";
        } else {
            return "Mở rộng";
        }
    }
}
