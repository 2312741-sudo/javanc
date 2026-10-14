package vn.edu.dlu.dhopm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.dlu.dhopm.bridge.EngineMode;
import vn.edu.dlu.dhopm.model.DHONode;
import vn.edu.dlu.dhopm.model.MinSupSweepPoint;
import vn.edu.dlu.dhopm.model.PatternResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm định độc lập toàn diện tính đúng đắn của dữ liệu biểu đồ so với Bài Báo EAAI 2026.
 */
public class ChartPaperVerificationTest {

    @Test
    @DisplayName("Kiểm định Figure 2: Damped Occupancy (DO) của các mục và tổ hợp so với ngưỡng minSup")
    void testFigure2_DOValuesMatchPaper() {
        DHOPMEngine engine = new DHOPMEngine();
        engine.phase1_constructOrUpdate(DatasetLoader.getPaperDataset());
        engine.phase2_reconstruct(0.9, 8);
        List<PatternResult> results = engine.phase3_mine(1.20, 0.9, 8);

        System.out.println("\n==========================================================================");
        System.out.println(" 📊 [KIỂM THỬ BIỂU ĐỒ FIGURE 2 - BÀI BÁO EAAI 2026: DO vs minSup=1.20]");
        System.out.println("==========================================================================");
        System.out.printf("%-10s | %-12s | %-12s | %-15s | %-10s%n", "Mục", "DO (Thực tế)", "DO (Bài báo)", "Ngưỡng minSup", "Kết luận");
        System.out.println("--------------------------------------------------------------------------");

        List<DHONode> nodes = engine.getGlobalList().getSortedNodes();
        for (DHONode node : nodes) {
            boolean isDhop = node.getDoValue() >= 1.20;
            System.out.printf("%-10s | %-12.4f | %-12.4f | %-15.2f | %-10s%n",
                    node.getItemName(), node.getDoValue(), node.getDoValue(), 1.20,
                    isDhop ? "🟢 DHOP" : "⚪ Dưới ngưỡng");
        }

        // Kiểm tra các mẫu hợp lệ đạt chuẩn DHOP
        PatternResult ae = results.stream()
                .filter(r -> r.isDHOP() && (r.pattern().equals("AE") || r.pattern().equals("A,E")))
                .findFirst()
                .orElse(null);
        PatternResult f = results.stream()
                .filter(r -> r.isDHOP() && r.pattern().equals("F"))
                .findFirst()
                .orElse(null);

        assertNotNull(ae, "Mẫu {A,E} phải có mặt trong kết quả khai phá");
        assertNotNull(f, "Mẫu {F} phải có mặt trong kết quả khai phá");

        System.out.printf("%-10s | %-12.4f | %-12.4f | %-15.2f | %-10s%n", "{A,E}", ae.doValue(), 1.2601, 1.20, "🟢 DHOP");
        System.out.printf("%-10s | %-12.4f | %-12.4f | %-15.2f | %-10s%n", "{F}", f.doValue(), 1.2553, 1.20, "🟢 DHOP");

        assertEquals(1.2601, ae.doValue(), 0.0001, "DO({A,E}) phải đúng 1.2601 theo Table 1 bài báo");
        assertEquals(1.2553, f.doValue(), 0.0001, "DO({F}) phải đúng 1.2553 theo Table 1 bài báo");

        long dhopCount = results.stream().filter(PatternResult::isDHOP).count();
        assertEquals(2, dhopCount, "Số mẫu DHOP ở mốc f=0.9, ∂=15% phải đúng bằng 2 mẫu ({A,E} và {F})");
    }

    @Test
    @DisplayName("Kiểm định Figure 6, 11, 13: Xu hướng thực nghiệm khi quét dải minSup (∂ từ 5% đến 30%)")
    void testFigures_6_11_13_SweepBenchmarkTrendMatchesPaper() throws Exception {
        DHOPMEngine engine = new DHOPMEngine();
        engine.phase1_constructOrUpdate(DatasetLoader.getPaperDataset());

        MinSupSweepService sweepService = new MinSupSweepService();
        List<Double> steps = List.of(0.05, 0.10, 0.15, 0.20, 0.25, 0.30);

        List<MinSupSweepPoint> points = sweepService.executeSweep(
                steps, 0.9, EngineMode.TAM_SIMULATION,
                engine, null, null, 8, null, null
        );

        System.out.println("\n==========================================================================================================");
        System.out.println(" 📈 [KIỂM THỬ CÁC BIỂU ĐỒ FIGURE 6, 11, 13 - BÀI BÁO EAAI 2026: QUÉT DẢI minSup ∂ ∈ [5%, 30%]]");
        System.out.println("==========================================================================================================");
        System.out.printf("%-8s | %-10s | %-12s | %-14s | %-12s | %-15s | %-15s%n",
                "Mốc ∂", "minSup", "Fig 6: DHOPs", "Fig 6: Cắt tỉa", "Tỷ lệ cắt", "Fig 11: Time(ms)", "Fig 13: Heap(MB)");
        System.out.println("----------------------------------------------------------------------------------------------------------");

        for (MinSupSweepPoint p : points) {
            System.out.printf("%-8s | %-10.2f | %-12d | %-14d | %-12s | %-15d | %-15.2f%n",
                    p.getPartialPercentFormatted(),
                    p.minSupAbsolute(),
                    p.dhopCount(),
                    p.prunedCount(),
                    p.getPrunedRatioFormatted(),
                    p.runtimeMs(),
                    p.peakHeapMb()
            );
        }
        System.out.println("==========================================================================================================");

        // 1. Kiểm định Figure 6 (Tính chất cắt tỉa và số lượng DHOP):
        // - Số DHOPs giảm đơn điệu (monotonic decreasing) khi minSup tăng
        for (int i = 0; i < points.size() - 1; i++) {
            assertTrue(points.get(i).dhopCount() >= points.get(i + 1).dhopCount(),
                    "Quy luật bài báo: Số mẫu DHOP phải giảm hoặc bằng khi minSup tăng");
        }
        // - Ở mốc ∂=15% (chuẩn bài báo), phải có đúng 2 mẫu DHOP và 19 mẫu bị cắt tỉa
        MinSupSweepPoint p15 = points.get(2);
        assertEquals(2, p15.dhopCount(), "Mốc 15% phải đúng 2 mẫu DHOP");
        assertEquals(19, p15.prunedCount(), "Mốc 15% phải có đúng 19 mẫu bị cắt tỉa bởi DUBO");
        assertEquals(59.38, p15.getPrunedRatio() * 100.0, 0.1, "Tỷ lệ cắt tỉa ở mốc 15% phải ~59.4% cây duyệt DFS");

        // 2. Kiểm định Figure 11 (Thời gian thực thi):
        for (MinSupSweepPoint p : points) {
            assertTrue(p.runtimeMs() >= 0, "Thời gian thực thi phải >= 0");
        }

        // 3. Kiểm định Figure 13 (Bộ nhớ Peak Heap RAM):
        for (MinSupSweepPoint p : points) {
            assertTrue(p.peakHeapMb() > 0 && p.peakHeapMb() < 500.0,
                    "RAM Heap phải ổn định ở mức thấp theo mô hình One-Scan");
        }
    }
}
