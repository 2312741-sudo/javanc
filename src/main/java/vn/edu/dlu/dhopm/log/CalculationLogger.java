package vn.edu.dlu.dhopm.log;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * <b>Singleton Pattern &amp; Observer Pattern:</b>
 * Quản lý tập trung toàn bộ nhật ký chi tiết từng bước tính toán số học của thuật toán DHOPM.
 * <p>
 * Lưu vết đầy đủ:
 * <ul>
 *   <li>Pha 1: One-Scan nạp từng giao dịch và ghi nhận Entry &lt;TID, TLen&gt; vào DHO-List</li>
 *   <li>Pha 2: Khai triển công thức tính DO(i) = &Sigma; f^(TL-Td)/|Td| và DUBO(i) chi tiết cho từng Item</li>
 *   <li>Pha 3: Khai phá cây DFS, kiểm định cận trên DUBO(X) và cắt tỉa nhánh (Pruning Property)</li>
 * </ul>
 * </p>
 *
 * @author Nguyễn Thanh Tâm (2312741)
 */
public class CalculationLogger {

    private static final CalculationLogger INSTANCE = new CalculationLogger();

    private final AtomicInteger counter = new AtomicInteger(1);
    private final ObservableList<CalculationLogEntry> logs = FXCollections.observableArrayList();
    private final List<Consumer<CalculationLogEntry>> listeners = new ArrayList<>();
    private static final int MAX_ENTRIES = 5000; // Giới hạn bộ nhớ giao diện

    private CalculationLogger() {}

    public static CalculationLogger getInstance() {
        return INSTANCE;
    }

    public ObservableList<CalculationLogEntry> getLogs() {
        return logs;
    }

    public synchronized void log(String phase, String target, String formula, String comparison, String decision) {
        int id = counter.getAndIncrement();
        CalculationLogEntry entry = CalculationLogEntry.of(id, phase, target, formula, comparison, decision);

        try {
            if (Platform.isFxApplicationThread()) {
                addEntryInternal(entry);
            } else {
                Platform.runLater(() -> addEntryInternal(entry));
            }
        } catch (IllegalStateException e) {
            // Khi chạy Unit Test hoặc môi trường headless, JavaFX Toolkit chưa được start
            addEntryInternal(entry);
        }
    }

    private void addEntryInternal(CalculationLogEntry entry) {
        if (logs.size() >= MAX_ENTRIES) {
            logs.remove(0, 500); // Tỉa bớt log cũ nếu quá đầy
        }
        logs.add(entry);
        for (Consumer<CalculationLogEntry> listener : listeners) {
            try {
                listener.accept(entry);
            } catch (Exception ignored) {}
        }
    }

    public synchronized void clear() {
        try {
            if (Platform.isFxApplicationThread()) {
                logs.clear();
                counter.set(1);
            } else {
                Platform.runLater(() -> {
                    logs.clear();
                    counter.set(1);
                });
            }
        } catch (IllegalStateException e) {
            logs.clear();
            counter.set(1);
        }
    }

    public void addListener(Consumer<CalculationLogEntry> listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Consumer<CalculationLogEntry> listener) {
        listeners.remove(listener);
    }

    /**
     * Xuất toàn bộ nhật ký thành chuỗi văn bản thuần túy có thể copy hoặc lưu file.
     */
    public String exportToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-5s | %-12s | %-18s | %-8s | %-65s | %-25s | %-15s%n",
                "STT", "Thời gian", "Pha", "Đối tượng", "Công thức & Diễn giải chi tiết", "So sánh minSup", "Quyết định"));
        sb.append("=".repeat(160)).append("\n");

        for (CalculationLogEntry e : logs) {
            sb.append(String.format("%-5d | %-12s | %-18s | %-8s | %-65s | %-25s | %-15s%n",
                    e.id(), e.timestamp(), e.phase(), e.target(), e.formula(), e.comparison(), e.decision()));
        }
        return sb.toString();
    }
}
