package vn.edu.dlu.dhopm.bridge;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test kiểm tra tính toán và định dạng thời gian ước tính (ETA) và hủy mining.
 *
 * @author Nguyễn Thanh Tâm (2312741)
 */
class MiningProgressInfoTest {

    @Test
    void formatDuration_variousTimes() {
        assertEquals("< 1s", MiningProgressInfo.formatDuration(0));
        assertEquals("< 1s", MiningProgressInfo.formatDuration(-50));
        assertEquals("650ms", MiningProgressInfo.formatDuration(650));
        assertEquals("15s", MiningProgressInfo.formatDuration(15_000));
        assertEquals("1m 30s", MiningProgressInfo.formatDuration(90_000));
        assertEquals("1h 01m 40s", MiningProgressInfo.formatDuration(3_700_000));
    }

    @Test
    void formatEtaStatus_completeness() {
        MiningProgressInfo done = new MiningProgressInfo(1.0, 5000, 0, 12, 10, 10);
        assertTrue(done.formatEtaStatus().contains("Hoàn tất"));
        assertTrue(done.formatEtaStatus().contains("5s"));

        MiningProgressInfo starting = new MiningProgressInfo(0.001, 200, 20000, 0, 0, 10);
        assertTrue(starting.formatEtaStatus().contains("Đang ước tính"));

        MiningProgressInfo running = new MiningProgressInfo(0.5, 5000, 5000, 7, 5, 10);
        String status = running.formatEtaStatus();
        assertTrue(status.contains("Còn ~5s"));
        assertTrue(status.contains("50.0%"));
        assertTrue(status.contains("7 mẫu"));
    }

    @Test
    void bridgeEngine_supportsCancelAndProgressInfo() {
        BridgeEngine engine = EngineFactory.create(EngineMode.TSON_V1_STANDARD, 0.15, 0.9);
        assertNotNull(engine);

        AtomicBoolean received = new AtomicBoolean(false);
        engine.onMiningProgressInfo(info -> received.set(true));

        assertDoesNotThrow(engine::cancel);
    }
}
