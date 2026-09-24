package vn.edu.dlu.dhopm.bridge;

/**
 * Chế độ Engine được chọn tại runtime.
 *
 * <ul>
 *   <li>{@link #TAM_SIMULATION} – Engine mô phỏng nội bộ của Tâm (step-by-step, JavaFX animation).
 *       Phù hợp để demo trực quan từng bước biến đổi DHO-List cho thầy cô xem.</li>
 *   <li>{@link #TSON_V1_STANDARD} – Engine V1 hiệu năng cao của Tson (đa luồng, FIMI datasets).
 *       Phù hợp để chạy thử nghiệm trên bộ dữ liệu lớn: chess.dat, retail.dat, kosarak.dat.</li>
 * </ul>
 *
 * <p>Thêm phiên bản mới (V2, V3) mà không sửa bất kỳ dòng UI nào –
 * chỉ thêm enum entry và cập nhật {@link EngineFactory}.
 *
 * @author Nguyễn Thanh Tâm (2312741) - Bridge Layer
 * @see EngineFactory
 */
public enum EngineMode {

    /** Engine mô phỏng của Tâm – hiển thị từng bước DHO-List. */
    TAM_SIMULATION("Tâm Simulation Engine (Step-by-Step / Animation)"),

    /** Engine V1 Standard của Tson – hiệu năng cao, đa luồng, dữ liệu FIMI. */
    TSON_V1_STANDARD("Tson V1 Standard Engine (High Performance / FIMI Datasets)");

    private final String displayName;

    EngineMode(String displayName) {
        this.displayName = displayName;
    }

    /** Tên hiển thị trong ComboBox UI. */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
