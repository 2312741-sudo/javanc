package vn.edu.dlu.dhopm.bridge;

import org.junit.jupiter.api.Test;
import vn.edu.dlu.dhopm.model.PatternResult;
import vn.edu.dlu.dhopm.model.Transaction;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test kiểm tra tầng Bridge hoạt động đúng và không phụ thuộc vào engine cụ thể.
 *
 * <p>Các test này chỉ kiểm tra lớp {@link BridgeEngine} (contract) –
 * không cần jar của Tson được install.
 *
 * @author Nguyễn Thanh Tâm (2312741) - Bridge Layer
 */
class BridgeEngineTest {

    /** Kiểm tra EngineFactory.createDefault() trả về non-null. */
    @Test
    void createDefault_returnsNonNull() {
        BridgeEngine engine = EngineFactory.createDefault();
        assertNotNull(engine);
        assertFalse(engine.engineName().isBlank());
    }

    /** Kiểm tra EngineFactory.create() với TamSimulation. */
    @Test
    void createTamSimulation_valid() {
        BridgeEngine engine = EngineFactory.create(EngineMode.TAM_SIMULATION, 0.15, 0.9);
        assertNotNull(engine);
        assertEquals(0, engine.transactionCount());
        assertEquals(0, engine.lastTid());
    }

    /** Kiểm tra EngineFactory.create() với TsonV1 (stub mode – không cần jar Tson). */
    @Test
    void createTsonV1_stub_valid() {
        BridgeEngine engine = EngineFactory.create(EngineMode.TSON_V1_STANDARD, 0.15, 0.9);
        assertNotNull(engine);
        assertEquals("Tson-V1-Standard", engine.engineName());
    }

    /** Kiểm tra EngineFactory ném lỗi khi minSupRatio âm. */
    @Test
    void create_invalidMinSup_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> EngineFactory.create(EngineMode.TAM_SIMULATION, -0.1, 0.9));
    }

    /** Kiểm tra EngineFactory ném lỗi khi decayFactor = 0 (ngoài miền (0,1]). */
    @Test
    void create_invalidDecayFactor_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> EngineFactory.create(EngineMode.TAM_SIMULATION, 0.15, 0.0));
    }

    /** Kiểm tra feedTransactions cập nhật transactionCount và lastTid đúng. */
    @Test
    void feedTransactions_updatesCounts() {
        BridgeEngine engine = EngineFactory.create(EngineMode.TSON_V1_STANDARD, 0.15, 0.9);

        List<Transaction> batch = List.of(
                new Transaction(1, List.of("A", "B", "C")),
                new Transaction(2, List.of("A", "D")),
                new Transaction(3, List.of("B", "C", "E"))
        );
        engine.feedTransactions(batch);

        assertEquals(3, engine.transactionCount());
        assertEquals(3, engine.lastTid());
    }

    /** Kiểm tra reset() về trạng thái ban đầu. */
    @Test
    void reset_clearsState() {
        BridgeEngine engine = EngineFactory.create(EngineMode.TSON_V1_STANDARD, 0.15, 0.9);
        engine.feedTransactions(List.of(new Transaction(5, List.of("X", "Y"))));
        engine.reset();

        assertEquals(0, engine.transactionCount());
        assertEquals(0, engine.lastTid());
    }

    /** Kiểm tra onPhaseUpdate callback được gắn mà không ném lỗi. */
    @Test
    void onPhaseUpdate_doesNotThrow() {
        BridgeEngine engine = EngineFactory.create(EngineMode.TSON_V1_STANDARD, 0.15, 0.9);
        assertDoesNotThrow(() -> engine.onPhaseUpdate((phase, ms) -> {}));
    }

    /** Kiểm tra onMiningProgress callback được gắn mà không ném lỗi. */
    @Test
    void onMiningProgress_doesNotThrow() {
        BridgeEngine engine = EngineFactory.create(EngineMode.TSON_V1_STANDARD, 0.15, 0.9);
        assertDoesNotThrow(() -> engine.onMiningProgress(pct -> {}));
    }

    /** Kiểm tra executeAndGetResults() (stub) trả về non-null. */
    @Test
    void executeAndGetResults_stubReturnsNonNull() {
        BridgeEngine engine = EngineFactory.create(EngineMode.TSON_V1_STANDARD, 0.15, 0.9);
        engine.feedTransactions(List.of(new Transaction(1, List.of("A", "B"))));
        List<PatternResult> results = engine.executeAndGetResults();
        assertNotNull(results);
    }
}
