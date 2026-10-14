package vn.edu.dlu.dhopm.bridge;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DhopmContractTcpServerTest {

    private static final int TEST_PORT = 7188;

    @BeforeAll
    static void setUp() {
        DhopmContractTcpServer.start(TEST_PORT);
    }

    @AfterAll
    static void tearDown() {
        DhopmContractTcpServer.stop();
    }

    @Test
    void testServerHandshake() {
        assertTrue(DhopmContractTcpServer.isRunning());
        DhopmContractTcpClient.HandshakeResult res = DhopmContractTcpClient.testConnection("127.0.0.1", TEST_PORT);
        assertTrue(res.success());
        assertNotNull(res.manager());
        assertTrue(res.latencyMs() >= 0);
    }

    @Test
    void testClientMethodsOverServer() throws Exception {
        DhopmContractTcpClient client = new DhopmContractTcpClient("127.0.0.1", TEST_PORT);
        assertTrue(client.handshake());
        assertTrue(client.isConnected());

        // Test window command
        String winResp = client.requestWindow(0.9, 0.0);
        assertNotNull(winResp);
        assertTrue(winResp.contains("\"ok\":true"));

        // Test stream command
        var streamResp = client.stream("retail.dat", 0.15, 0.9, 0.0, 50, null, false, event -> {
            assertNotNull(event);
        });
        assertTrue(streamResp.ok());
        assertTrue(streamResp.totalTransactions() > 0);

        // Test mine command
        var mineResp = client.mine("default.dat", 0.15, 0.9, 0.0, 8, java.util.List.of("v1"), false);
        assertTrue(mineResp.ok());
        assertTrue(mineResp.totalTransactions() > 0);
        assertFalse(mineResp.patterns().isEmpty());

        client.close();
    }

    @Test
    void testSweepOverTcpServer() throws Exception {
        DhopmContractTcpClient.setBackendAddress("127.0.0.1", TEST_PORT);
        vn.edu.dlu.dhopm.core.MinSupSweepService sweepService = new vn.edu.dlu.dhopm.core.MinSupSweepService();
        java.util.List<Double> steps = java.util.List.of(0.15, 0.20);
        java.util.List<vn.edu.dlu.dhopm.model.MinSupSweepPoint> sweepPoints = sweepService.executeSweep(
                steps, 0.9, EngineMode.TSON_TCP_BACKEND,
                null, null, java.nio.file.Paths.get("default.dat"), 8, java.util.List.of("v1"),
                null, null
        );
        assertEquals(2, sweepPoints.size());
        assertTrue(sweepPoints.get(0).minSupAbsolute() > 0);
        assertTrue(sweepPoints.get(0).dhopCount() >= sweepPoints.get(1).dhopCount());
    }
}
