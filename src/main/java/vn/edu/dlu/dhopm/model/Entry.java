package vn.edu.dlu.dhopm.model;

/**
 * Bieu dien mot entry <TID, TLen> trong DHO-List theo bai bao DHOPM.
 *
 * @param tid  Ma dinh danh giao dich (Transaction ID)
 * @param tlen Do dai (so luong items) cua giao dich tuong ung
 */
public record Entry(int tid, int tlen) {
    public Entry {
        if (tlen <= 0) {
            throw new IllegalArgumentException("Transaction length must be positive");
        }
    }

    @Override
    public String toString() {
        return "<" + tid + ", " + tlen + ">";
    }
}
