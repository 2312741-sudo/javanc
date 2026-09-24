package vn.edu.dlu.dhopm.bridge;

/**
 * Factory tạo {@link BridgeEngine} theo {@link EngineMode} được chọn.
 *
 * <p>Đây là điểm duy nhất trong codebase biết sự tồn tại của cả
 * {@link TamSimulationBridge} và {@link TsonV1Bridge}. UI chỉ cần gọi:
 * <pre>
 *   BridgeEngine engine = EngineFactory.create(mode, minSupRatio, decayFactor);
 * </pre>
 *
 * <h3>Khi Tson phát hành engine mới (V2, V3):</h3>
 * <ol>
 *   <li>Tson tạo class {@code TsonV2Bridge} tương tự {@link TsonV1Bridge}.</li>
 *   <li>Thêm entry {@code TSON_V2_OPTIMIZED} vào {@link EngineMode}.</li>
 *   <li>Thêm {@code case TSON_V2_OPTIMIZED} ở đây.</li>
 *   <li><b>Tâm không sửa bất kỳ dòng UI nào.</b></li>
 * </ol>
 *
 * <p><b>Design Pattern:</b> Factory Method – tách rời việc tạo đối tượng ra khỏi
 * nơi sử dụng, tuân thủ Open/Closed Principle (mở cho mở rộng, đóng cho sửa đổi).
 *
 * @author Nguyễn Thanh Tâm (2312741) - Bridge Layer
 */
public final class EngineFactory {

    private EngineFactory() {
        // utility class – không khởi tạo
    }

    /**
     * Tạo một {@link BridgeEngine} mới theo chế độ được chọn.
     *
     * @param mode         chế độ engine (xem {@link EngineMode})
     * @param minSupRatio  ngưỡng support tương đối ∂ ∈ [0,1]
     * @param decayFactor  hệ số suy giảm f ∈ (0,1]
     * @return instance {@link BridgeEngine} sẵn sàng sử dụng
     * @throws IllegalArgumentException nếu minSupRatio hoặc decayFactor nằm ngoài miền hợp lệ
     */
    public static BridgeEngine create(EngineMode mode, double minSupRatio, double decayFactor) {
        validateConfig(minSupRatio, decayFactor);
        return switch (mode) {
            case TAM_SIMULATION   -> new TamSimulationBridge(minSupRatio, decayFactor);
            case TSON_V1_STANDARD -> new TsonV1Bridge(minSupRatio, decayFactor);
        };
    }

    /**
     * Tạo engine mặc định (chế độ Tâm Simulation) với tham số mặc định từ bài báo.
     * Dùng nhanh trong unit test hoặc khởi tạo ban đầu của Controller.
     */
    public static BridgeEngine createDefault() {
        return create(EngineMode.TAM_SIMULATION, 0.15, 0.9);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────────────────

    private static void validateConfig(double minSupRatio, double decayFactor) {
        if (Double.isNaN(minSupRatio) || minSupRatio < 0.0 || minSupRatio > 1.0) {
            throw new IllegalArgumentException(
                    "minSupRatio (∂) phải trong [0,1]: " + minSupRatio);
        }
        if (Double.isNaN(decayFactor) || decayFactor <= 0.0 || decayFactor > 1.0) {
            throw new IllegalArgumentException(
                    "decayFactor (f) phải trong (0,1]: " + decayFactor);
        }
    }
}
