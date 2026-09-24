package vn.edu.dlu.dhopm.bridge;

import vn.edu.dlu.dhopm.model.PatternResult;
import vn.edu.dlu.dhopm.model.Transaction;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Hợp đồng chung (Contract) giữa tầng UI của Tâm và tầng Engine phía sau.
 *
 * <p>Tâm chỉ phụ thuộc vào interface này – không bao giờ import trực tiếp
 * {@code DHOPMEngine} hay {@code MiningEngine} của Tson. Nguyên tắc DIP
 * (Dependency Inversion Principle) đảm bảo không có merge conflict.
 *
 * <p><b>Luồng sử dụng chuẩn:</b>
 * <pre>
 * BridgeEngine engine = EngineFactory.create(EngineMode.TSON_V1_STANDARD, minSup, f);
 * engine.onPhaseUpdate((phase, ms) -> lblStatus.setText(phase + " – " + ms + " ms"));
 * engine.onMiningProgress(pct -> progressBar.setProgress(pct));
 * engine.feedTransactions(batch);
 * List&lt;PatternResult&gt; results = engine.executeAndGetResults();
 * </pre>
 *
 * @author Nguyễn Thanh Tâm (2312741) - Bridge Layer
 * @see EngineFactory
 * @see TamSimulationBridge
 * @see TsonV1Bridge
 */
public interface BridgeEngine {

    /**
     * Tên định danh của engine đang chạy (ví dụ: {@code "Tâm-Simulation"}, {@code "v1-standard"}).
     * Dùng để hiển thị lên thanh trạng thái UI.
     */
    String engineName();

    /**
     * Nạp một batch giao dịch mới vào engine (One-Scan incremental load).
     * Gọi nhiều lần liên tiếp để mô phỏng Data Stream.
     *
     * @param batch danh sách giao dịch trong batch hiện tại (TID phải tăng dần)
     */
    void feedTransactions(List<Transaction> batch);

    /**
     * Thực thi khai phá mẫu và trả về kết quả dưới dạng {@link PatternResult}
     * (định dạng UI – độc lập với bất kỳ internal class nào của Tson hay Tâm).
     *
     * @return danh sách mẫu đã khai phá, đã sắp xếp theo DO giảm dần
     */
    List<PatternResult> executeAndGetResults();

    /**
     * Gắn Observer nhận thông báo khi một pha pipeline hoàn thành.
     * Hàm callback nhận: tên pha (String) và thời gian thực thi (milliseconds).
     *
     * <p>Phải gọi trước {@link #feedTransactions} hoặc {@link #executeAndGetResults}.
     *
     * @param callback {@code (phaseName, durationMs) -> ...}
     */
    void onPhaseUpdate(BiConsumer<String, Double> callback);

    /**
     * Gắn Observer nhận tiến độ khai phá (0.0 → 1.0).
     * Dùng để cập nhật {@code ProgressBar} hoặc phần trăm trên UI.
     *
     * @param callback {@code (fraction) -> ...}
     */
    void onMiningProgress(Consumer<Double> callback);

    /**
     * Đặt lại trạng thái engine về trạng thái ban đầu (xóa toàn bộ DHO-List,
     * reset TID counter). Tương đương với tạo engine mới.
     */
    void reset();

    /**
     * Số giao dịch đã nạp vào engine tính từ lần reset gần nhất.
     */
    long transactionCount();

    /**
     * TID lớn nhất đã được nạp (TL trong ký hiệu bài báo).
     */
    int lastTid();
}
