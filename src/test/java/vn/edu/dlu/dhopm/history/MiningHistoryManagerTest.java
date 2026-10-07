package vn.edu.dlu.dhopm.history;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class MiningHistoryManagerTest {

    private MiningHistoryManager manager;

    @BeforeEach
    void setUp() {
        manager = MiningHistoryManager.getInstance();
        manager.clearHistory();
    }

    @Test
    void testRecordRun_IncrementsRunIdAndStoresMemento() {
        assertEquals(0, manager.size());

        MiningRunMemento run1 = manager.recordRun(
                "Tam-Simulation", "default.dat", 0.90, 0.15, 1.20,
                8, 2, 19, 32, 15L, 12.0, Collections.emptyList()
        );

        assertNotNull(run1);
        assertEquals(1, run1.getRunId());
        assertEquals("Tam-Simulation", run1.getEngineName());
        assertEquals(2, run1.getDhopCount());
        assertEquals(19, run1.getPrunedCount());
        assertEquals(32, run1.getTotalCandidates());
        assertEquals(1, manager.size());

        MiningRunMemento run2 = manager.recordRun(
                "Tson-V1", "retail.dat", 1.0, 0.01, 10.0,
                1000, 15, 5, 20, 103L, 25.0, Collections.emptyList()
        );

        assertEquals(2, run2.getRunId());
        assertEquals(2, manager.size());
        assertEquals(run2, manager.getLatestRun());
    }

    @Test
    void testObserverNotificationOnRecordRun() {
        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addListener(m -> notified.set(true));

        manager.recordRun(
                "Tam-Simulation", "default.dat", 0.90, 0.15, 1.20,
                8, 2, 19, 32, 15L, 12.0, Collections.emptyList()
        );

        assertTrue(notified.get());
    }

    @Test
    void testClearHistory() {
        manager.recordRun("Test", "test.dat", 0.9, 0.1, 1.0, 10, 1, 1, 2, 5L, 10.0, Collections.emptyList());
        assertEquals(1, manager.size());

        manager.clearHistory();
        assertEquals(0, manager.size());
        assertNull(manager.getLatestRun());
    }
}
