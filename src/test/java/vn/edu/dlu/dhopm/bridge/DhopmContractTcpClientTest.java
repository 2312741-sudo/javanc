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
}
