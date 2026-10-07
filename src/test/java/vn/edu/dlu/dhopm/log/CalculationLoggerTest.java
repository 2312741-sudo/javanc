package vn.edu.dlu.dhopm.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculationLoggerTest {

    private CalculationLogger logger;

    @BeforeEach
    void setUp() {
        logger = CalculationLogger.getInstance();
        logger.clear();
    }

    @Test
    void testLogAndExport() {
        assertEquals(0, logger.getLogs().size());

        logger.log("Pha 1: Construct", "T1", "Entry <1, 4>", "TL=1", "📦 CẬP NHẬT");
        logger.log("Pha 2: Reconstruct", "Item A", "DO=0.8551", "minSup=1.20", "⚪ ỨNG VIÊN");
        logger.log("Pha 3: Mining DFS", "Mẫu AE", "DO=1.2601", "minSup=1.20", "🟢 DHOP");

        assertEquals(3, logger.getLogs().size());

        CalculationLogEntry entry = logger.getLogs().get(2);
        assertEquals("Mẫu AE", entry.target());
        assertEquals("🟢 DHOP", entry.decision());

        String exported = logger.exportToString();
        assertNotNull(exported);
        assertTrue(exported.contains("Pha 1: Construct"));
        assertTrue(exported.contains("Mẫu AE"));
        assertTrue(exported.contains("🟢 DHOP"));
    }

    @Test
    void testClear() {
        logger.log("Pha 1", "T1", "Formula", "Comp", "Dec");
        assertEquals(1, logger.getLogs().size());

        logger.clear();
        assertEquals(0, logger.getLogs().size());
    }
}
