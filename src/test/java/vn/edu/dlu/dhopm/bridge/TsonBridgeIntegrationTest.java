package vn.edu.dlu.dhopm.bridge;

import org.junit.jupiter.api.Test;
import vn.edu.dlu.dhopm.model.PatternResult;
import vn.edu.dlu.dhopm.model.Transaction;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test: Bridge thật sự gọi MiningEngine của Tson.
 *
 * <p>Dataset dùng đúng bộ 8 giao dịch chuẩn của Tson (GoldenCases.baseStream()):
 *   T1={A,C,D,E} T2={A,E,F} T3={B,C,D,E} T4={C,D,F}
 *   T5={B,F}      T6={D,E,F} T7={A,B,C,F} T8={A,E,G}
 *
 * @author Nguyễn Thanh Tâm (2312741) - Integration Test
 */
class TsonBridgeIntegrationTest {

    /** Dataset chuẩn của Tson – khớp với GoldenCases.baseStream() */
    private List<Transaction> buildTsonDataset() {
        return List.of(
            new Transaction(1, List.of("A", "C", "D", "E")),
            new Transaction(2, List.of("A", "E", "F")),
            new Transaction(3, List.of("B", "C", "D", "E")),
            new Transaction(4, List.of("C", "D", "F")),
            new Transaction(5, List.of("B", "F")),
            new Transaction(6, List.of("D", "E", "F")),
            new Transaction(7, List.of("A", "B", "C", "F")),
            new Transaction(8, List.of("A", "E", "G"))
        );
    }

    /** TC1: f=0.9, ∂=0.15 → minSup=1.2 → kỳ vọng 2 mẫu: AE, F */
    @Test
    void tsonV1_TC1_shouldReturn2Patterns() {
        BridgeEngine engine = EngineFactory.create(EngineMode.TSON_V1_STANDARD, 0.15, 0.9);
        engine.feedTransactions(buildTsonDataset());
        List<PatternResult> results = engine.executeAndGetResults();

        System.out.println("=== TsonBridge TC1 (f=0.9, ∂=0.15) ===");
        System.out.printf("Engine : %s | Tx=%d TL=%d%n",
                engine.engineName(), engine.transactionCount(), engine.lastTid());
        System.out.printf("Patterns tìm được: %d%n", results.size());
        results.forEach(p -> System.out.printf("  %-20s DO=%.4f  isDHOP=%s%n",
                p.pattern(), p.doValue(), p.isDHOP()));

        assertEquals(2, results.size(), "TC1 phải trả về đúng 2 mẫu DHOP (AE và F)");
        assertTrue(results.stream().anyMatch(p -> p.pattern().equals("A,E")), "AE phải là DHOP");
        assertTrue(results.stream().anyMatch(p -> p.pattern().equals("F")),   "F phải là DHOP");
    }

    /** TC2: f=0.9, ∂=0.20 → minSup=1.6 → kỳ vọng 0 mẫu */
    @Test
    void tsonV1_TC2_shouldReturn0Patterns() {
        BridgeEngine engine = EngineFactory.create(EngineMode.TSON_V1_STANDARD, 0.20, 0.9);
        engine.feedTransactions(buildTsonDataset());
        List<PatternResult> results = engine.executeAndGetResults();

        System.out.printf("=== TC2 (f=0.9, ∂=0.20) → %d patterns ===%n", results.size());
        assertEquals(0, results.size(), "TC2 phải trả về 0 mẫu");
    }

    /** TC5: f=1.0, ∂=0.15 → minSup=1.2 → kỳ vọng 9 mẫu (không suy giảm) */
    @Test
    void tsonV1_TC5_f1_shouldReturn9Patterns() {
        BridgeEngine engine = EngineFactory.create(EngineMode.TSON_V1_STANDARD, 0.15, 1.0);
        engine.feedTransactions(buildTsonDataset());
        List<PatternResult> results = engine.executeAndGetResults();

        System.out.printf("=== TC5 (f=1.0, ∂=0.15) → %d patterns ===%n", results.size());
        results.forEach(p -> System.out.printf("  %-20s DO=%.4f%n", p.pattern(), p.doValue()));

        assertEquals(9, results.size(), "TC5 (f=1.0) phải trả về đúng 9 mẫu DHOP");
    }

    /** transactionCount và lastTid cập nhật đúng */
    @Test
    void tsonV1_countsCorrectAfterFeed() {
        BridgeEngine engine = EngineFactory.create(EngineMode.TSON_V1_STANDARD, 0.15, 0.9);
        engine.feedTransactions(buildTsonDataset());

        assertEquals(8, engine.transactionCount());
        assertEquals(8, engine.lastTid());
    }

    /** engineName đúng */
    @Test
    void tsonV1_engineName_correct() {
        assertEquals("Tson-V1-Standard",
                EngineFactory.create(EngineMode.TSON_V1_STANDARD, 0.15, 0.9).engineName());
    }

    /**
     * SO SÁNH Tâm vs Tson trên TC1 – cùng số pattern.
     * (PhaseListener dùng Platform.runLater, không chạy trong headless test nên bỏ qua)
     */
    @Test
    void tamVsTson_TC1_samePatternCount() {
        BridgeEngine tamEngine  = EngineFactory.create(EngineMode.TAM_SIMULATION,   0.15, 0.9);
        BridgeEngine tsonEngine = EngineFactory.create(EngineMode.TSON_V1_STANDARD, 0.15, 0.9);

        List<Transaction> data = buildTsonDataset();
        tamEngine.feedTransactions(data);
        tsonEngine.feedTransactions(data);

        List<PatternResult> tamResults  = tamEngine.executeAndGetResults();
        List<PatternResult> tsonResults = tsonEngine.executeAndGetResults();

        System.out.printf("=== SO SÁNH TÂM vs TSON (TC1) ===%n");
        System.out.printf("Tâm   → %d total (duyệt DFS), %d DHOP%n",
                tamResults.size(),
                tamResults.stream().filter(PatternResult::isDHOP).count());
        System.out.printf("Tson  → %d patterns (đã lọc sẵn, tất cả là DHOP)%n",
                tsonResults.size());

        // Tâm duyệt toàn bộ cây DFS (kể cả candidates bị pruned).
        // Tson chỉ trả về patterns đạt chuẩn DHOP.
        // → So sánh đúng: số DHOP của Tâm phải bằng số pattern của Tson.
        long tamDHOPCount = tamResults.stream().filter(PatternResult::isDHOP).count();
        assertEquals(tamDHOPCount, (long) tsonResults.size(),
                "Số DHOP của Tâm phải bằng số pattern của Tson trên TC1");
    }
}
