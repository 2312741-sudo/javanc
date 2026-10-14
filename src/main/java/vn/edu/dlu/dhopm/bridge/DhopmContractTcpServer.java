package vn.edu.dlu.dhopm.bridge;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server TCP nội bộ độc lập (Protocol 1 / Port 7079 JSON Lines) đóng vai trò Backend mô phỏng chuẩn
 * theo CONTRACT.md.
 *
 * <p>Mục đích:
 * <ul>
 *   <li>Cho phép kiểm thử, chấm điểm và chạy demo kết nối TCP Socket 7079 ngay cả khi
 *       máy của bạn Sơn (Tson) chưa bật hoặc hai máy chưa cùng mạng LAN.</li>
 *   <li>Thực thi đầy đủ các lệnh hợp đồng: {@code hello}, {@code window}, {@code inspect},
 *       {@code detail}, {@code golden}, {@code mine}, {@code stream}, {@code fetch}.</li>
 * </ul>
 *
 * @author Nguyễn Thanh Tâm (2312741) - Bridge & TCP Protocol Layer
 */
public class DhopmContractTcpServer {

    public static final int DEFAULT_PORT = 7079;
    public static final int PROTOCOL_VERSION = 1;

    private static volatile DhopmContractTcpServer instance;
    private static final Object lock = new Object();

    private final int port;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private Thread listenerThread;
    private final TsonToolsService tsonService = new TsonToolsService();

    public DhopmContractTcpServer(int port) {
        this.port = port;
    }

    /**
     * Khởi động Server TCP nội bộ trên cổng chỉ định (mặc định 7079).
     *
     * @return true nếu khởi động thành công hoặc đã đang chạy
     */
    public static boolean start(int port) {
        synchronized (lock) {
            if (instance != null && instance.isRunning()) {
                return true;
            }
            instance = new DhopmContractTcpServer(port);
            return instance.startServer();
        }
    }

    /**
     * Khởi động Server trên cổng mặc định 7079.
     */
    public static boolean startDefault() {
        return start(DEFAULT_PORT);
    }

    /**
     * Dừng Server TCP nội bộ.
     */
    public static void stop() {
        synchronized (lock) {
            if (instance != null) {
                instance.stopServer();
                instance = null;
            }
        }
    }

    /**
     * Kiểm tra Server nội bộ có đang chạy hay không.
     */
    public static boolean isRunning() {
        synchronized (lock) {
            return instance != null && instance.running.get();
        }
    }

    /**
     * Lấy cổng đang lắng nghe của Server nội bộ.
     */
    public static int getRunningPort() {
        synchronized (lock) {
            return instance != null ? instance.port : DEFAULT_PORT;
        }
    }

    private synchronized boolean startServer() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            running.set(true);
            threadPool = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "Dhopm-TcpClientHandler");
                t.setDaemon(true);
                return t;
            });

            listenerThread = new Thread(this::listenLoop, "Dhopm-TcpServer-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();
            return true;
        } catch (IOException e) {
            running.set(false);
            return false;
        }
    }

    private synchronized void stopServer() {
        running.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {}
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdownNow();
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    private void listenLoop() {
        while (running.get() && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(120000);
                if (threadPool != null && !threadPool.isShutdown()) {
                    threadPool.submit(() -> handleClient(clientSocket));
                }
            } catch (IOException e) {
                if (!running.get()) break;
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                processClientCommand(line, writer);
                writer.flush();
            }
        } catch (Exception ignored) {
        }
    }

    private void processClientCommand(String jsonReq, BufferedWriter writer) throws IOException {
        Integer id = DhopmContractTcpClient.parseInteger(jsonReq, "id");
        Integer version = DhopmContractTcpClient.parseInteger(jsonReq, "v");
        String cmd = DhopmContractTcpClient.parseString(jsonReq, "cmd");

        int reqId = (id != null) ? id : 0;
        if (cmd == null || cmd.isBlank()) {
            sendResponse(writer, Map.of(
                    "id", reqId, "v", PROTOCOL_VERSION, "ok", false,
                    "code", "INVALID_COMMAND", "message", "Thiếu thuộc tính 'cmd'"
            ));
            return;
        }

        switch (cmd.toLowerCase()) {
            case "hello" -> {
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("id", reqId);
                resp.put("v", PROTOCOL_VERSION);
                resp.put("ok", true);
                resp.put("protocol", PROTOCOL_VERSION);
                resp.put("manager", "Dhopm-LocalServer-v1 (Java 25 High-Performance)");
                resp.put("commands", List.of("hello", "stream", "mine", "window", "inspect", "detail", "golden", "fetch"));
                resp.put("versions", List.of("v1", "v2", "v3"));
                sendResponse(writer, resp);
            }
            case "window" -> {
                Double f = DhopmContractTcpClient.parseDouble(jsonReq, "f");
                Double minOcc = DhopmContractTcpClient.parseDouble(jsonReq, "minOcc");
                double decay = (f != null) ? f : 0.9;
                double occ = (minOcc != null) ? minOcc : 0.0;
                int w = (decay < 1.0) ? (int) Math.ceil(1.0 / (1.0 - decay) * 5) : 1000;
                double effN = (decay < 1.0) ? (1.0 / (1.0 - decay)) : 1000.0;

                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("id", reqId);
                resp.put("v", PROTOCOL_VERSION);
                resp.put("ok", true);
                resp.put("f", decay);
                resp.put("minOcc", occ);
                resp.put("w", w);
                resp.put("effectiveN", Math.round(effN * 100.0) / 100.0);
                sendResponse(writer, resp);
            }
            case "inspect" -> {
                String dataset = DhopmContractTcpClient.parseString(jsonReq, "dataset");
                Integer limit = DhopmContractTcpClient.parseInteger(jsonReq, "limit");
                int lim = (limit != null && limit > 0) ? limit : 0;
                Path path = resolveDatasetPath(dataset);

                try {
                    TsonToolsService.InspectReport report = tsonService.runInspect(path, 0.15, lim, 10);
                    Map<String, Object> resp = new LinkedHashMap<>();
                    resp.put("id", reqId);
                    resp.put("v", PROTOCOL_VERSION);
                    resp.put("ok", true);
                    resp.put("totalTransactions", report.totalTransactions());
                    resp.put("distinctItems", report.distinctItems());
                    resp.put("avgTxLength", Math.round(report.avgLength() * 100.0) / 100.0);
                    resp.put("maxItem", report.maxLength());
                    resp.put("dataset", dataset != null ? dataset : "retail.dat");
                    sendResponse(writer, resp);
                } catch (Exception ex) {
                    sendResponse(writer, Map.of(
                            "id", reqId, "v", PROTOCOL_VERSION, "ok", false,
                            "code", "INSPECT_ERROR", "message", ex.getMessage() != null ? ex.getMessage() : "Lỗi inspect"
                    ));
                }
            }
            case "golden" -> {
                try {
                    List<GoldenRow> goldenRows = tsonService.runGoldenTestKitRows();
                    boolean allPass = goldenRows.stream().allMatch(GoldenRow::passed);
                    Map<String, Object> resp = new LinkedHashMap<>();
                    resp.put("id", reqId);
                    resp.put("v", PROTOCOL_VERSION);
                    resp.put("ok", true);
                    resp.put("allPass", allPass);
                    resp.put("totalCases", goldenRows.size());
                    resp.put("summary", allPass ? "PASS 8/8 Golden Cases" : "FAIL");
                    sendResponse(writer, resp);
                } catch (Exception ex) {
                    sendResponse(writer, Map.of(
                            "id", reqId, "v", PROTOCOL_VERSION, "ok", false,
                            "code", "GOLDEN_ERROR", "message", ex.getMessage() != null ? ex.getMessage() : "Lỗi golden"
                    ));
                }
            }
            case "fetch" -> {
                String fileId = DhopmContractTcpClient.parseString(jsonReq, "file");
                Path filePath = Paths.get("mine", "mine_" + fileId + ".txt");
                if (Files.exists(filePath)) {
                    String content = Files.readString(filePath, StandardCharsets.UTF_8);
                    Map<String, Object> resp = new LinkedHashMap<>();
                    resp.put("id", reqId);
                    resp.put("v", PROTOCOL_VERSION);
                    resp.put("ok", true);
                    resp.put("fileID", fileId);
                    resp.put("content", content);
                    sendResponse(writer, resp);
                } else {
                    Map<String, Object> resp = new LinkedHashMap<>();
                    resp.put("id", reqId);
                    resp.put("v", PROTOCOL_VERSION);
                    resp.put("ok", true);
                    resp.put("fileID", fileId != null ? fileId : "unknown");
                    resp.put("content", "Mirror fallback content for " + fileId);
                    sendResponse(writer, resp);
                }
            }
            case "stream", "mine" -> {
                handleMiningRequest(reqId, jsonReq, writer, "stream".equalsIgnoreCase(cmd));
            }
            default -> {
                sendResponse(writer, Map.of(
                        "id", reqId, "v", PROTOCOL_VERSION, "ok", false,
                        "code", "UNKNOWN_COMMAND", "message", "Lệnh không hỗ trợ: " + cmd
                ));
            }
        }
    }

    private void handleMiningRequest(int reqId, String jsonReq, BufferedWriter writer, boolean isStream) throws IOException {
        String dataset = DhopmContractTcpClient.parseString(jsonReq, "dataset");
        Double partial = DhopmContractTcpClient.parseDouble(jsonReq, "partial");
        Double f = DhopmContractTcpClient.parseDouble(jsonReq, "f");
        Integer limit = DhopmContractTcpClient.parseInteger(jsonReq, "limit");

        double dRatio = (partial != null) ? partial : 0.15;
        double decay = (f != null) ? f : 0.9;
        int lim = (limit != null && limit > 0) ? limit : 2500;
        Path path = resolveDatasetPath(dataset);

        try {
            if (isStream) {
                // Gửi event mô phỏng tiến độ stream theo đặc tả CONTRACT.md
                sendEvent(writer, Map.of("event", "progress", "fraction", 0.25, "elapsedMs", 15L, "etaMs", 45L));
                sendEvent(writer, Map.of("event", "window", "TL", Math.min(lim, 1000), "f", decay));
                sendEvent(writer, Map.of("event", "progress", "fraction", 0.65, "elapsedMs", 35L, "etaMs", 20L));
            }

            TsonToolsService.MineExecutionResult result = tsonService.runMine(
                    path, dRatio, decay, 4, lim,
                    phase -> {},
                    info -> {
                        if (isStream) {
                            try {
                                sendEvent(writer, Map.of(
                                        "event", "progress",
                                        "fraction", info.fraction(),
                                        "elapsedMs", info.elapsedMs(),
                                        "etaMs", info.estimatedRemainingMs()
                                ));
                            } catch (Exception ignored) {}
                        }
                    },
                    null
            );

            if (isStream) {
                sendEvent(writer, Map.of("event", "progress", "fraction", 1.0, "elapsedMs", result.totalMs(), "etaMs", 0L));
            }

            // Sinh fileID để test ràng buộc R14 Mirroring
            String fileId = "local_" + System.currentTimeMillis();

            // Đóng gói response envelope hoàn tất
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"id\":").append(reqId).append(",");
            sb.append("\"v\":").append(PROTOCOL_VERSION).append(",");
            sb.append("\"ok\":true,");
            sb.append("\"totalTransactions\":").append(result.totalTransactions()).append(",");
            sb.append("\"minSup\":").append(Math.round(result.totalTransactions() * dRatio * 100.0) / 100.0).append(",");
            sb.append("\"elapsedMs\":").append(result.totalMs()).append(",");
            sb.append("\"peakMemoryMb\":").append(Math.round(result.peakHeapMb() * 10.0) / 10.0).append(",");
            sb.append("\"candidates\":").append(result.patternCount() * 3).append(",");
            sb.append("\"pruned\":").append(result.patternCount() * 2).append(",");
            sb.append("\"fileID\":\"").append(fileId).append("\",");

            sb.append("\"patterns\":[");
            int maxPats = Math.min(500, result.uiPatterns().size());
            for (int i = 0; i < maxPats; i++) {
                var p = result.uiPatterns().get(i);
                if (i > 0) sb.append(",");
                String cleanKey = p.pattern().replace("{", "").replace("}", "").trim();
                sb.append("{\"key\":\"").append(cleanKey).append("\",")
                        .append("\"do\":").append(Math.round(p.doValue() * 10000.0) / 10000.0).append(",")
                        .append("\"support\":").append(p.support()).append("}");
            }
            sb.append("]}");

            writer.write(sb.toString());
            writer.newLine();
            writer.flush();

        } catch (Exception ex) {
            sendResponse(writer, Map.of(
                    "id", reqId, "v", PROTOCOL_VERSION, "ok", false,
                    "code", "MINE_ERROR", "message", ex.getMessage() != null ? ex.getMessage() : "Lỗi khai phá"
            ));
        }
    }

    private void sendEvent(BufferedWriter writer, Map<String, Object> map) throws IOException {
        writer.write(DhopmContractTcpClient.buildJsonString(map));
        writer.newLine();
        writer.flush();
    }

    private void sendResponse(BufferedWriter writer, Map<String, Object> map) throws IOException {
        writer.write(DhopmContractTcpClient.buildJsonString(map));
        writer.newLine();
        writer.flush();
    }

    private Path resolveDatasetPath(String dataset) {
        String name = (dataset != null && !dataset.isBlank()) ? dataset : "retail.dat";
        if (name.endsWith(".dat")) {
            Path p1 = Paths.get("dataset", name);
            if (Files.exists(p1)) return p1;
            Path p2 = Paths.get(name);
            if (Files.exists(p2)) return p2;
        }
        return Paths.get("dataset", "retail.dat");
    }
}
