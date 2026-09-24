package vn.edu.dlu.dhopm.bridge;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử TsonToolsService – xác thực các thao tác CLI đã được GUI hóa:
 * mine, inspect, golden TC1-TC8.
 */
class TsonToolsServiceTest {

    private final TsonToolsService service = new TsonToolsService();

    @Test
    void testPresetDatasetsListNotEmpty() {
        var presets = TsonToolsService.getPresetDatasets();
        assertFalse(presets.isEmpty(), "Danh sách preset datasets không được rỗng");
        assertTrue(presets.stream().anyMatch(d -> d.filename().equals("chess.dat")), "Phải có chess.dat");
        assertTrue(presets.stream().anyMatch(d -> d.filename().equals("default.dat")), "Phải có default.dat");
    }

    @Test
    void testGoldenTestKitRunsSuccessfully() {
        String report = service.runGoldenTestKit();
        assertNotNull(report);
        assertTrue(report.contains("TC1:"), "Báo cáo phải chứa TC1");
        assertTrue(report.contains("TC8:"), "Báo cáo phải chứa TC8");
        assertTrue(report.contains("PASS"), "Báo cáo phải có PASS");
        assertTrue(report.contains("100% TUYỆT ĐỐI"), "Tất cả test case vàng phải PASS 100%");
    }

    @Test
    void testInspectDefaultDataset() throws Exception {
        Path path = Paths.get("dataset/default.dat");
        if (!java.nio.file.Files.exists(path)) {
            path = Paths.get("/tmp/tson_jvnc/dataset/default.dat");
        }
        if (java.nio.file.Files.exists(path)) {
            var report = service.runInspect(path, 0.15, 0, 5);
            assertEquals(8, report.totalTransactions(), "default.dat có đúng 8 giao dịch");
            assertEquals(8, report.lastTid());
            assertFalse(report.topItems().isEmpty());
            assertTrue(report.textOutput().contains("INSPECT"));
        }
    }

    @Test
    void testMineDefaultDataset() throws Exception {
        Path path = Paths.get("dataset/default.dat");
        if (!java.nio.file.Files.exists(path)) {
            path = Paths.get("/tmp/tson_jvnc/dataset/default.dat");
        }
        if (java.nio.file.Files.exists(path)) {
            var res = service.runMine(path, 0.15, 0.9, 2, 0, null, null);
            assertNotNull(res);
            assertEquals(2, res.patternCount(), "TC1 trên default.dat phải tìm ra 2 DHOPs (AE, F)");
            assertEquals(8, res.totalTransactions());
            assertTrue(res.totalMs() >= 0);
        }
    }
}
