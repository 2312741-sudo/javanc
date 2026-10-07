package vn.edu.dlu.dhopm.history;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * <b>Memento Pattern (Caretaker) &amp; Singleton Pattern:</b>
 * Quản lý lịch sử toàn bộ các lần khai phá (Mining Runs) trong phiên làm việc.
 * <p>
 * Lưu giữ danh sách {@link MiningRunMemento}, cung cấp dữ liệu cho biểu đồ so sánh xu hướng
 * và phát sự kiện (Observer Pattern) cho giao diện khi có lượt chạy mới.
 * </p>
 *
 * @author Nguyễn Thanh Tâm (2312741)
 */
public class MiningHistoryManager {

    private static final MiningHistoryManager INSTANCE = new MiningHistoryManager();

    private final AtomicInteger sequenceCounter = new AtomicInteger(1);
    private final ObservableList<MiningRunMemento> history = FXCollections.observableArrayList();
    private final List<Consumer<MiningRunMemento>> listeners = new ArrayList<>();

    private MiningHistoryManager() {
        // Singleton private constructor
    }

    public static MiningHistoryManager getInstance() {
        return INSTANCE;
    }

    /**
     * Lưu lại một snapshot trạng thái của lần mining vừa hoàn thành.
     */
    public synchronized MiningRunMemento recordRun(
            String engineName,
            String datasetName,
            double f,
            double partial,
            double minSup,
            int totalTransactions,
            int dhopCount,
            int prunedCount,
            int totalCandidates,
            long runtimeMs,
            double peakHeapMb,
            List<vn.edu.dlu.dhopm.model.PatternResult> patterns
    ) {
        int id = sequenceCounter.getAndIncrement();
        MiningRunMemento memento = new MiningRunMemento(
                id, engineName, datasetName, f, partial, minSup,
                totalTransactions, dhopCount, prunedCount, totalCandidates,
                runtimeMs, peakHeapMb, patterns
        );

        history.add(memento);
        notifyListeners(memento);
        return memento;
    }

    public ObservableList<MiningRunMemento> getHistory() {
        return history;
    }

    public synchronized void clearHistory() {
        history.clear();
        sequenceCounter.set(1);
    }

    public int size() {
        return history.size();
    }

    public MiningRunMemento getLatestRun() {
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    // ── Observer Pattern ─────────────────────────────────────────────────────

    public void addListener(Consumer<MiningRunMemento> listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Consumer<MiningRunMemento> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(MiningRunMemento memento) {
        for (Consumer<MiningRunMemento> listener : listeners) {
            try {
                listener.accept(memento);
            } catch (Exception ignored) {}
        }
    }
}
