package vn.edu.dlu.dhopm.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Record lưu trữ một dòng nhật ký chi tiết của một phép tính toán thuật toán DHOPM.
 *
 * @param id           Số thứ tự phép tính
 * @param timestamp    Thời điểm ghi nhận (HH:mm:ss.SSS)
 * @param phase        Pha thuật toán (Pha 1: Construct, Pha 2: Reconstruct, Pha 3: Mining DFS)
 * @param target       Đối tượng (Item, Mẫu X, hoặc Giao dịch T)
 * @param formula      Công thức và diễn giải từng bước tính toán số học
 * @param comparison   So sánh đối chiếu với ngưỡng minSup
 * @param decision     Quyết định (🟢 DHOP, 🔴 CẮT TỈA, ⚪ MỞ RỘNG, 📦 CẬP NHẬT)
 *
 * @author Nguyễn Thanh Tâm (2312741)
 */
public record CalculationLogEntry(
        int id,
        String timestamp,
        String phase,
        String target,
        String formula,
        String comparison,
        String decision
) {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static CalculationLogEntry of(int id, String phase, String target, String formula, String comparison, String decision) {
        return new CalculationLogEntry(
                id,
                LocalDateTime.now().format(TIME_FORMATTER),
                phase,
                target,
                formula,
                comparison,
                decision
        );
    }
}
