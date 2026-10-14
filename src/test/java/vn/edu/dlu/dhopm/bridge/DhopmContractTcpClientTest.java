package vn.edu.dlu.dhopm.bridge;

import org.junit.jupiter.api.Test;
import vn.edu.dlu.dhopm.model.PatternResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DhopmContractTcpClientTest {

    @Test
    void testBuildJsonString() {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("id", 0);
        req.put("v", 1);
        req.put("cmd", "hello");
        req.put("export", true);

        String json = DhopmContractTcpClient.buildJsonString(req);
        assertEquals("{\"id\":0,\"v\":1,\"cmd\":\"hello\",\"export\":true}", json);
    }

    @Test
    void testParseMineResponseSuccess() {
        String json = "{\"id\":2,\"v\":1,\"ok\":true,\"totalTransactions\":88162,\"minSup\":9.18," +
                "\"patterns\":[{\"items\":[\"A\",\"E\"],\"key\":\"A,E\",\"do\":1.2601,\"support\":3,\"tids\":[2,4,7]}]," +
                "\"saved\":{\"fileID\":\"26-10-11_001\",\"path\":\"/mine/mine_26-10-11_001.txt\"}}";

        DhopmContractTcpClient.ContractMineResponse resp = DhopmContractTcpClient.parseMineResponse(json);

        assertTrue(resp.ok());
        assertEquals(88162, resp.totalTransactions());
        assertEquals(9.18, resp.minSup(), 0.001);
        assertNotNull(resp.patterns());
        assertEquals(1, resp.patterns().size());

        PatternResult pat = resp.patterns().get(0);
        assertEquals("{A, E}", pat.pattern());
        assertEquals(1.2601, pat.doValue(), 0.0001);
        assertEquals(3, pat.support());
    }

    @Test
    void testParseMineResponseError() {
        String json = "{\"id\":1,\"v\":1,\"ok\":false,\"code\":\"MISSING_PARAMETER\",\"message\":\"Thieu tham so bat buoc\"}";

        DhopmContractTcpClient.ContractMineResponse resp = DhopmContractTcpClient.parseMineResponse(json);

        assertFalse(resp.ok());
        assertEquals("MISSING_PARAMETER", resp.errorCode());
        assertEquals("Thieu tham so bat buoc", resp.message());
        assertEquals(0, resp.patterns().size());
    }

    @Test
    void testParseMineResponseWithMetrics() {
        String json = "{\"id\":3,\"v\":1,\"ok\":true,\"totalTransactions\":10000,\"minSup\":15.0," +
                "\"elapsedMs\":1250,\"peakMemoryMb\":45.8,\"candidates\":1820,\"pruned\":1750," +
                "\"patterns\":[{\"key\":\"1 2\",\"do\":18.5,\"support\":20}]," +
                "\"fileID\":\"test_file_001\"}";

        DhopmContractTcpClient.ContractMineResponse resp = DhopmContractTcpClient.parseMineResponse(json);

        assertTrue(resp.ok());
        assertEquals(10000, resp.totalTransactions());
        assertEquals(15.0, resp.minSup(), 0.001);
        assertEquals(1250L, resp.elapsedMs());
        assertEquals(45.8, resp.peakMemoryMb(), 0.01);
        assertEquals(1820, resp.candidates());
        assertEquals(1750, resp.pruned());
        assertEquals("test_file_001", resp.savedFileId());
        assertEquals(1, resp.patterns().size());
    }

    @Test
    void testParseStreamEvents() {
        String progressJson = "{\"event\":\"progress\",\"fraction\":0.65,\"elapsedMs\":800,\"etaMs\":400}";
        assertEquals("progress", DhopmContractTcpClient.parseString(progressJson, "event"));
        assertEquals(0.65, DhopmContractTcpClient.parseDouble(progressJson, "fraction"), 0.001);
        assertEquals(800L, DhopmContractTcpClient.parseLong(progressJson, "elapsedMs"));
        assertEquals(400L, DhopmContractTcpClient.parseLong(progressJson, "etaMs"));

        String windowJson = "{\"event\":\"window\",\"TL\":5000,\"minSup\":120.5}";
        assertEquals("window", DhopmContractTcpClient.parseString(windowJson, "event"));
        assertEquals(5000, DhopmContractTcpClient.parseInteger(windowJson, "TL"));
        assertEquals(120.5, DhopmContractTcpClient.parseDouble(windowJson, "minSup"), 0.01);
    }

    @Test
    void testParseStringListAndVersions() {
        String helloJson = "{\"id\":0,\"v\":1,\"ok\":true,\"protocol\":1,\"manager\":\"dhopm-cli\",\"versions\":[\"v1\",\"v2\",\"v3\"]}";
        List<String> versions = DhopmContractTcpClient.parseStringList(helloJson, "versions");
        assertEquals(List.of("v1", "v2", "v3"), versions);

        String emptyListJson = "{\"id\":0,\"v\":1,\"versions\":[]}";
        List<String> emptyList = DhopmContractTcpClient.parseStringList(emptyListJson, "versions");
        assertTrue(emptyList.contains("v1") && emptyList.contains("v2") && emptyList.contains("v3"));

        // Phản hồi thực tế từ Tson Backend dạng mảng Object kèm status
        String tsonVersionsObjJson = "{\"id\":0,\"v\":1,\"ok\":true,\"protocol\":1,\"manager\":\"dhopm-cli\"," +
                "\"versions\":[{\"id\":\"v1\",\"engine\":\"v1-standard\",\"status\":\"ready\"}," +
                "{\"id\":\"v2\",\"engine\":\"v2-minocc\",\"status\":\"ready\"}," +
                "{\"id\":\"v3\",\"engine\":\"v3-optimized\",\"status\":\"ready\"}," +
                "{\"id\":\"v4\",\"engine\":\"v4-extreme\",\"status\":\"pending\"}]}";
        List<String> tsonVers = DhopmContractTcpClient.parseStringList(tsonVersionsObjJson, "versions");
        assertEquals(List.of("v1", "v2", "v3"), tsonVers);
    }

    @Test
    void testNormalizePatternRemovesNestedBraces() {
        assertEquals("{B, F}", DhopmContractTcpClient.normalizePattern("{{B, F}}"));
        assertEquals("{B, F}", DhopmContractTcpClient.normalizePattern("{B, F}"));
        assertEquals("{B, F}", DhopmContractTcpClient.normalizePattern("B, F"));
        assertEquals("{B, F}", DhopmContractTcpClient.normalizePattern("B F"));
        assertEquals("{}", DhopmContractTcpClient.normalizePattern(null));
        assertEquals("{}", DhopmContractTcpClient.normalizePattern(""));
    }

    @Test
    void testDeduplicatePatterns() {
        PatternResult p1 = new PatternResult("{B, F}", 1.31, 1.31, 2, true, false, List.of(5, 7));
        PatternResult p2 = new PatternResult("{C, D}", 1.079, 1.079, 3, true, false, List.of(1, 3, 4));
        PatternResult p3 = new PatternResult("{{B, F}}", 1.31, 1.31, 2, true, false, List.of(5, 7)); // duplicate of p1
        PatternResult p4 = new PatternResult("F, B", 1.30, 1.30, 2, true, false, List.of(5, 7)); // duplicate of p1 with lower DO

        List<PatternResult> deduped = DhopmContractTcpClient.deduplicatePatterns(List.of(p1, p2, p3, p4));
        assertEquals(2, deduped.size());
        assertEquals(1.31, deduped.get(0).doValue(), 0.001);
    }

    @Test
    void testParseMineResponseIgnoresPerVersionDuplicates() {
        // Mô phỏng cấu trúc phản hồi thực tế của server Tson có cả "patterns" và "perVersion.v1.patterns"
        String tsonRealJson = "{\"id\":2,\"v\":1,\"ok\":true," +
                "\"patterns\":[" +
                "{\"items\":[\"B\",\"F\"],\"key\":\"B,F\",\"do\":1.31,\"support\":2,\"tids\":[5,7]}," +
                "{\"items\":[\"F\"],\"key\":\"F\",\"do\":1.39483,\"support\":5,\"tids\":[2,4,5,6,7]}" +
                "]," +
                "\"perVersion\":{\"v1\":{\"patterns\":[" +
                "{\"items\":[\"B\",\"F\"],\"key\":\"B,F\",\"do\":1.31,\"support\":2,\"tids\":[5,7]}," +
                "{\"items\":[\"F\"],\"key\":\"F\",\"do\":1.39483,\"support\":5,\"tids\":[2,4,5,6,7]}" +
                "]}}}";

        var resp = DhopmContractTcpClient.parseMineResponse(tsonRealJson);
        assertTrue(resp.ok());
        assertEquals(2, resp.patterns().size(), "Phải có đúng 2 mẫu sau khi khử trùng lặp và không bị nuốt mảng perVersion");
        assertEquals("{F}", resp.patterns().get(0).pattern());
        assertEquals("{B, F}", resp.patterns().get(1).pattern());
        assertEquals(List.of(5, 7), resp.patterns().get(1).transactionIds(), "Phải trích xuất đúng danh sách TIDs");
    }
}
