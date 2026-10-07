package vn.edu.dlu.dhopm.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.edu.dlu.dhopm.bridge.EngineMode;
import vn.edu.dlu.dhopm.model.MinSupSweepPoint;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MinSupSweepServiceTest {

    private MinSupSweepService sweepService;
    private DHOPMEngine engine;

    @BeforeEach
    void setUp() {
        sweepService = new MinSupSweepService();
        engine = new DHOPMEngine();
        engine.phase1_constructOrUpdate(DatasetLoader.getPaperDataset());
    }

    @Test
    void testGeneratePartialSteps() {
        List<Double> steps = MinSupSweepService.generatePartialSteps(0.05, 0.30, 0.05);
        assertEquals(6, steps.size());
        assertEquals(0.05, steps.get(0), 1e-4);
        assertEquals(0.10, steps.get(1), 1e-4);
        assertEquals(0.15, steps.get(2), 1e-4);
        assertEquals(0.20, steps.get(3), 1e-4);
        assertEquals(0.25, steps.get(4), 1e-4);
        assertEquals(0.30, steps.get(5), 1e-4);
    }

    @Test
    void testExecuteSweepOnPaperDataset() throws Exception {
        List<Double> steps = List.of(0.05, 0.10, 0.15, 0.20, 0.25, 0.30);
        List<MinSupSweepPoint> results = sweepService.executeSweep(
                steps, 0.9, EngineMode.TAM_SIMULATION,
                engine, null, null, 8, null, null
        );

        assertEquals(6, results.size());

        // Mốc 1: ∂=0.05 (minSup = 0.40) -> Nhiều mẫu hơn
        MinSupSweepPoint p1 = results.get(0);
        assertEquals(0.05, p1.partial(), 1e-4);
        assertEquals(0.40, p1.minSupAbsolute(), 1e-4);
        assertTrue(p1.dhopCount() >= 2);

        // Mốc 3: ∂=0.15 (minSup = 1.20) -> Đúng 2 mẫu DHOP ({AE}, {F}) như bài báo!
        MinSupSweepPoint p3 = results.get(2);
        assertEquals(0.15, p3.partial(), 1e-4);
        assertEquals(1.20, p3.minSupAbsolute(), 1e-4);
        assertEquals(2, p3.dhopCount(), "Ở mốc ∂=15%, default.dat phải có đúng 2 mẫu DHOP ({AE}, {F})");
        assertEquals(19, p3.prunedCount(), "Ở mốc ∂=15%, DUBO phải cắt tỉa đúng 19 mẫu");

        // Khi minSup tăng, số mẫu DHOP phải giảm hoặc bằng (monotonically decreasing)
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).dhopCount() >= results.get(i + 1).dhopCount(),
                    "Số mẫu DHOP phải giảm đơn điệu khi minSup tăng: " + results.get(i).dhopCount() + " >= " + results.get(i + 1).dhopCount());
        }
    }
}
