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
        assertEquals("{A,E}", pat.pattern());
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
}
