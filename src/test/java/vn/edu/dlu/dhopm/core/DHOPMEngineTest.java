package vn.edu.dlu.dhopm.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.dlu.dhopm.model.DHONode;
import vn.edu.dlu.dhopm.model.PatternResult;
import vn.edu.dlu.dhopm.model.Transaction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test xac thuc 100% tinh dung dan cua thuat toan DHOPM so voi so lieu trong bai bao khoa hoc.
 */
class DHOPMEngineTest {

    private DHOPMEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DHOPMEngine();
    }

    @Test
    @DisplayName("Vi du 1: Kiem tra DO tren DB0 (T1..T4) voi f=0.9, TL=4, minSup=1.0")
    void testPaperExample1_DB0() {
        // Nap DB0: T1, T2, T3, T4
        List<Transaction> db0 = Arrays.asList(
                new Transaction(1, Arrays.asList("A", "C", "D", "E")),
                new Transaction(2, Arrays.asList("A", "E", "F")),
                new Transaction(3, Arrays.asList("B", "C", "D", "E")),
                new Transaction(4, Arrays.asList("C", "D", "F"))
        );
        engine.phase1_constructOrUpdate(db0);
        engine.phase2_reconstruct(0.9, 4);

        // DO(AE) tren DB0 phai = 0.9045
        List<PatternResult> results = engine.phase3_mine(1.0, 0.9, 4);
        PatternResult ae = results.stream().filter(r -> r.pattern().equals("AE")).findFirst().orElse(null);
        assertNotNull(ae, "Mau AE phai duoc duyet qua");
        assertEquals(0.9045, ae.doValue(), 0.001, "DO(AE) phai xap xi 0.9045");
        assertFalse(ae.isDHOP(), "AE co DO < 1.0 nen KHONG phai la DHOP tren DB0");

        // DO(CD) tren DB0 phai = 1.4812
        PatternResult cd = results.stream().filter(r -> r.pattern().equals("CD")).findFirst().orElse(null);
        assertNotNull(cd, "Mau CD phai duoc duyet qua");
        assertEquals(1.4812, cd.doValue(), 0.001, "DO(CD) phai xap xi 1.4812");
        assertTrue(cd.isDHOP(), "CD co DO >= 1.0 nen LA DHOP tren DB0");
    }

    @Test
    @DisplayName("Vi du 3: Thu tu sap xep Support tang dan tren DB0: B < A < F < C < D < E")
    void testPaperExample3_SortOrder_DB0() {
        List<Transaction> db0 = Arrays.asList(
                new Transaction(1, Arrays.asList("A", "C", "D", "E")),
                new Transaction(2, Arrays.asList("A", "E", "F")),
                new Transaction(3, Arrays.asList("B", "C", "D", "E")),
                new Transaction(4, Arrays.asList("C", "D", "F"))
        );
        engine.phase1_constructOrUpdate(db0);
        engine.phase2_reconstruct(0.9, 4);

        List<DHONode> sorted = engine.getGlobalList().getSortedNodes();
        assertEquals(6, sorted.size());

        assertEquals("B", sorted.get(0).getItemName());
        assertEquals(1, sorted.get(0).getSupport());
        assertEquals(0.225, sorted.get(0).getDoValue(), 0.001);

        assertEquals("A", sorted.get(1).getItemName());
        assertEquals(2, sorted.get(1).getSupport());
        assertEquals(0.4523, sorted.get(1).getDoValue(), 0.001);
    }

    @Test
    @DisplayName("Vi du 4: Tinh DO va sap xep tren toan bo CSDL (T1..T8), f=0.9, TL=8")
    void testPaperExample4_FullDB() {
        engine.phase1_constructOrUpdate(DatasetLoader.getPaperDataset());
        engine.phase2_reconstruct(0.9, 8);

        List<DHONode> sorted = engine.getGlobalList().getSortedNodes();
        assertEquals(7, sorted.size());

        // Thu tu: G(1) < B(3) < A(4) < C(4) < D(4) < E(5) < F(5)
        assertEquals("G", sorted.get(0).getItemName());
        assertEquals(1, sorted.get(0).getSupport());
        assertEquals(0.3333, sorted.get(0).getDoValue(), 0.001);

        assertEquals("B", sorted.get(1).getItemName());
        assertEquals(3, sorted.get(1).getSupport());
        assertEquals(0.7371, sorted.get(1).getDoValue(), 0.002);

        DHONode nodeF = engine.getGlobalList().getNode("F");
        assertEquals(5, nodeF.getSupport());
        assertEquals(1.2553, nodeF.getDoValue(), 0.001);
    }

    @Test
    @DisplayName("Vi du 5: Khai pha toan bo CSDL tim duoc dung 2 DHOPs: AE (1.2601) va F (1.2553)")
    void testPaperExample5_MiningResults() {
        engine.phase1_constructOrUpdate(DatasetLoader.getPaperDataset());
        List<PatternResult> results = engine.phase3_mine(1.2, 0.9, 8);

        List<PatternResult> dhops = results.stream().filter(PatternResult::isDHOP).toList();
        assertEquals(2, dhops.size(), "Phai co dung 2 DHOPs");

        PatternResult ae = dhops.stream().filter(p -> p.pattern().equals("AE")).findFirst().orElse(null);
        assertNotNull(ae);
        assertEquals(1.2601, ae.doValue(), 0.001);

        PatternResult f = dhops.stream().filter(p -> p.pattern().equals("F")).findFirst().orElse(null);
        assertNotNull(f);
        assertEquals(1.2553, f.doValue(), 0.001);
    }

    @Test
    @DisplayName("Kiem tra can tren DUBO: DUBO(G) = 1.0 < 1.2 cat tia ngay tu dau")
    void testDUBO_Pruning_ItemG() {
        engine.phase1_constructOrUpdate(DatasetLoader.getPaperDataset());
        List<PatternResult> results = engine.phase3_mine(1.2, 0.9, 8);

        PatternResult g = results.stream().filter(p -> p.pattern().equals("G")).findFirst().orElse(null);
        assertNotNull(g);
        assertEquals(1.0, g.duboValue(), 0.001);
        assertTrue(g.isPruned(), "G phai bi cat tia vi DUBO(G) < 1.2");
    }

    @Test
    @DisplayName("Kiem tra One-Scan Streaming: Bom T9 khong can doc lai T1..T8")
    void testOneScanStreaming() {
        engine.phase1_constructOrUpdate(DatasetLoader.getPaperDataset());
        assertEquals(8, engine.getCurrentTL());

        // Bom them T9 = {A, E}
        Transaction t9 = new Transaction(9, Arrays.asList("A", "E"));
        engine.phase1_constructOrUpdate(Collections.singletonList(t9));

        assertEquals(9, engine.getCurrentTL());
        assertEquals(5, engine.getGlobalList().getNode("A").getSupport());
        assertEquals(6, engine.getGlobalList().getNode("E").getSupport());
    }
}
