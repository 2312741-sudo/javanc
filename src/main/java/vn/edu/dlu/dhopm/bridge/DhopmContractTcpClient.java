package vn.edu.dlu.dhopm.bridge;

import vn.edu.dlu.dhopm.model.PatternResult;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client kết nối Backend độc lập qua TCP Socket (mặc định localhost:7079)
 * theo chuẩn Hợp đồng Frontend ↔ Backend (FE Contract Protocol 1).
 *
 * <p>Đặc tả chuẩn:</p>
 * <ul>
 *   <li>Protocol version: 1 ("v": 1)</li>
 *   <li>Wire format: JSON Lines (JSONL - 1 request / 1 dòng, multi-line stream events, 1 response envelope)</li>
 *   <li>Cổng mặc định: TCP 7079</li>
 *   <li>Handshake: {"id":0,"v":1,"cmd":"hello"}</li>
 *   <li>Streaming: {"id":...,"v":1,"cmd":"stream",...} tiếp nhận các sự kiện progress/window/pattern và kết thúc bằng response envelope</li>
 *   <li>Ràng buộc R14: Tự động lưu bản sao kết quả (mirror local) qua lệnh fetch khi có saved fileID</li>
 * </ul>
 *
 * @author Nguyễn Thanh Tâm (2312741) - Bridge & Protocol Layer
 */
public class DhopmContractTcpClient {

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 7079;
    public static final int PROTOCOL_VERSION = 1;

    private final String host;
    private final int port;
    private final AtomicInteger requestIdSeq = new AtomicInteger(1);
    private boolean connected = false;
    private String serverManager = "";
    private List<String> serverCommands = new ArrayList<>();

    // Persistent Socket connection
    private Socket socket;
    private BufferedReader socketReader;
    private BufferedWriter socketWriter;

    private static volatile String activeHost = resolveInitialHost();
    private static volatile int activePort = resolveInitialPort();

    private static String resolveInitialHost() {
        String prop = System.getProperty("dhopm.backend.host");
        if (prop != null && !prop.isBlank()) return prop.trim();
        String env = System.getenv("DHOPM_BACKEND_HOST");
        if (env != null && !env.isBlank()) return env.trim();
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DhopmContractTcpClient.class);
            return prefs.get("backend_host", DEFAULT_HOST);
        } catch (Exception ignored) {
            return DEFAULT_HOST;
        }
    }

    private static int resolveInitialPort() {
        String prop = System.getProperty("dhopm.backend.port");
        if (prop != null && !prop.isBlank()) {
            try { return Integer.parseInt(prop.trim()); } catch (Exception ignored) {}
        }
        String env = System.getenv("DHOPM_BACKEND_PORT");
        if (env != null && !env.isBlank()) {
            try { return Integer.parseInt(env.trim()); } catch (Exception ignored) {}
        }
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DhopmContractTcpClient.class);
            return prefs.getInt("backend_port", DEFAULT_PORT);
        } catch (Exception ignored) {
            return DEFAULT_PORT;
        }
    }

    public static synchronized void setBackendAddress(String host, int port) {
        if (host != null && !host.isBlank()) {
            activeHost = host.trim();
        }
        if (port > 0 && port <= 65535) {
            activePort = port;
        }
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DhopmContractTcpClient.class);
            prefs.put("backend_host", activeHost);
            prefs.putInt("backend_port", activePort);
            prefs.flush();
        } catch (Exception ignored) {}
    }

    public static String getActiveHost() {
        return activeHost;
    }

    public static int getActivePort() {
        return activePort;
    }

    public record HandshakeResult(
            boolean success,
            String host,
            int port,
            String manager,
            List<String> commands,
            List<String> versions,
            long latencyMs,
            String errorMessage
    ) {
        public HandshakeResult(boolean success, String host, int port, String manager, List<String> commands, long latencyMs, String errorMessage) {
            this(success, host, port, manager, commands, List.of("v1"), latencyMs, errorMessage);
        }
    }

    public static HandshakeResult testConnection(String host, int port) {
        String targetHost = (host != null && !host.isBlank()) ? host.trim() : DEFAULT_HOST;
        int targetPort = (port > 0 && port <= 65535) ? port : DEFAULT_PORT;
        long start = System.currentTimeMillis();

        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(targetHost, targetPort), 2500);
            s.setSoTimeout(3000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));

            writer.write("{\"id\":0,\"v\":1,\"cmd\":\"hello\"}\n");
            writer.flush();

            String line = reader.readLine();
            long latency = System.currentTimeMillis() - start;
            if (line == null) {
                return new HandshakeResult(false, targetHost, targetPort, null, List.of(), List.of("v1"), latency,
                        "Backend ngắt kết nối ngay khi bắt tay.");
            }
            Boolean ok = parseBoolean(line, "ok");
            Integer proto = parseInteger(line, "protocol");
            String manager = parseString(line, "manager");
            List<String> versions = parseStringList(line, "versions");
            if (versions == null || versions.isEmpty()) versions = List.of("v1");

            if (Boolean.TRUE.equals(ok) && proto != null && proto == PROTOCOL_VERSION) {
                return new HandshakeResult(true, targetHost, targetPort,
                        manager != null ? manager : "Dhopm Backend", List.of(), versions, latency, null);
            } else {
                return new HandshakeResult(false, targetHost, targetPort, manager, List.of(), versions, latency,
                        "Phản hồi không hợp lệ từ Backend: " + line);
            }
        } catch (java.net.ConnectException ce) {
            long latency = System.currentTimeMillis() - start;
            return new HandshakeResult(false, targetHost, targetPort, null, List.of(), List.of("v1"), latency,
                    "Từ chối kết nối (Connection Refused). Máy Tson (" + targetHost + ") chưa mở cổng " + targetPort + ".");
        } catch (java.net.SocketTimeoutException ste) {
            long latency = System.currentTimeMillis() - start;
            return new HandshakeResult(false, targetHost, targetPort, null, List.of(), List.of("v1"), latency,
                    "Hết thời gian chờ (Timeout). Hãy kiểm tra lại IP hoặc tường lửa (Firewall) trên máy Tson.");
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;
            return new HandshakeResult(false, targetHost, targetPort, null, List.of(), List.of("v1"), latency,
                    "Lỗi kết nối: " + (ex.getMessage() != null ? ex.getMessage() : ex.toString()));
        }
    }

    public static String scanLan(int port, int timeoutMs, Consumer<String> statusConsumer) {
        if (statusConsumer != null) statusConsumer.accept("Đang kiểm tra kết nối localhost (127.0.0.1)...");
        HandshakeResult localTest = testConnection("127.0.0.1", port);
        if (localTest.success()) {
            return "127.0.0.1";
        }

        Set<String> candidateIps = new LinkedHashSet<>();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"arp", "-a"});
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String l;
                Pattern ipPattern = Pattern.compile("\\b(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\b");
                while ((l = r.readLine()) != null) {
                    Matcher m = ipPattern.matcher(l);
                    if (m.find()) {
                        String ip = m.group(1);
                        if (!ip.startsWith("127.") && !ip.endsWith(".255") && !ip.equals("224.0.0.251")) {
                            candidateIps.add(ip);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        try {
            Enumeration<java.net.NetworkInterface> nics = java.net.NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                var nic = nics.nextElement();
                if (nic.isLoopback() || !nic.isUp()) continue;
                for (var addr : nic.getInterfaceAddresses()) {
                    java.net.InetAddress inet = addr.getAddress();
                    if (inet instanceof java.net.Inet4Address && !inet.isLoopbackAddress()) {
                        String myIp = inet.getHostAddress();
                        int lastDot = myIp.lastIndexOf('.');
                        if (lastDot > 0) {
                            String prefix = myIp.substring(0, lastDot + 1);
                            int myNum = Integer.parseInt(myIp.substring(lastDot + 1));
                            for (int offset = 1; offset <= 30; offset++) {
                                int down = myNum - offset;
                                int up = myNum + offset;
                                if (down >= 1) candidateIps.add(prefix + down);
                                if (up <= 254) candidateIps.add(prefix + up);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        if (statusConsumer != null) {
            statusConsumer.accept("Đang dò quét song song " + candidateIps.size() + " IP trong mạng LAN trên cổng " + port + "...");
        }

        var pool = java.util.concurrent.Executors.newFixedThreadPool(32);
        java.util.concurrent.atomic.AtomicReference<String> foundIp = new java.util.concurrent.atomic.AtomicReference<>(null);

        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
        for (String ip : candidateIps) {
            futures.add(pool.submit(() -> {
                if (foundIp.get() != null) return;
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(ip, port), Math.max(200, timeoutMs));
                    HandshakeResult res = testConnection(ip, port);
                    if (res.success()) {
                        foundIp.compareAndSet(null, ip);
                    }
                } catch (Exception ignored) {
                }
            }));
        }

        for (var f : futures) {
            try {
                f.get(timeoutMs + 2500L, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {}
            if (foundIp.get() != null) break;
        }

        pool.shutdownNow();
        return foundIp.get();
    }

    public DhopmContractTcpClient() {
        this(getActiveHost(), getActivePort());
    }

    public DhopmContractTcpClient(String host, int port) {
        this.host = (host != null && !host.isBlank()) ? host.trim() : DEFAULT_HOST;
        this.port = (port > 0 && port <= 65535) ? port : DEFAULT_PORT;
    }

    /**
     * Đảm bảo Socket được kết nối và duy trì (Persistent TCP Socket).
     */
    private synchronized void ensureConnected() throws IOException {
        if (socket != null && socket.isConnected() && !socket.isClosed() && !socket.isInputShutdown() && !socket.isOutputShutdown()) {
            return;
        }
        close();
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 2500); // 2.5s connect timeout
        socket.setSoTimeout(60000); // 60s read timeout
        socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        socketWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        connected = true;
    }

    /**
     * Đóng socket hiện tại một cách an toàn.
     */
    public synchronized void close() {
        connected = false;
        if (socketReader != null) {
            try { socketReader.close(); } catch (Exception ignored) {}
            socketReader = null;
        }
        if (socketWriter != null) {
            try { socketWriter.close(); } catch (Exception ignored) {}
            socketWriter = null;
        }
        if (socket != null) {
            try { socket.close(); } catch (Exception ignored) {}
            socket = null;
        }
    }

    /**
     * Kiểm tra và thực hiện bắt tay (Handshake) với Backend.
     */
    public synchronized boolean handshake() {
        try {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("id", 0);
            req.put("v", PROTOCOL_VERSION);
            req.put("cmd", "hello");

            String respJson = sendRawRequest(buildJsonString(req));
            if (respJson == null || respJson.isEmpty()) {
                connected = false;
                return false;
            }

            Boolean ok = parseBoolean(respJson, "ok");
            Integer proto = parseInteger(respJson, "protocol");
            if (Boolean.TRUE.equals(ok) && proto != null && proto == PROTOCOL_VERSION) {
                this.connected = true;
                this.serverManager = parseString(respJson, "manager");
                return true;
            }
        } catch (Exception e) {
            close();
        }
        return false;
    }

    /**
     * Kiểm tra trạng thái đã kết nối hay chưa.
     */
    public boolean isConnected() {
        return connected;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getServerManager() {
        return serverManager;
    }

    /**
     * Gửi yêu cầu khai phá dạng luồng sự kiện (cmd = "stream") qua Backend.
     * Tiếp nhận các dòng sự kiện trung gian (progress, window, pattern...)
     * và chỉ hoàn tất khi nhận được dòng phản hồi kết thúc (final response envelope).
     */
    public ContractMineResponse stream(String dataset, double partial, double f, double minOcc, int limit,
                                       List<String> versions, boolean export,
                                       Consumer<String> eventConsumer) throws IOException {
        int reqId = requestIdSeq.getAndIncrement();
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("id", reqId);
        req.put("v", PROTOCOL_VERSION);
        req.put("cmd", "stream");
        req.put("dataset", dataset);
        req.put("format", "fimi");
        req.put("partial", partial);
        req.put("f", f);
        req.put("minOcc", minOcc);
        req.put("limit", limit);
        if (versions != null && !versions.isEmpty()) {
            req.put("versions", versions);
        } else {
            req.put("versions", List.of("v1"));
        }
        req.put("export", export);

        String jsonReq = buildJsonString(req);
        ContractMineResponse response = sendStreamingRequest(jsonReq, eventConsumer);

        // R14: Mirror kết quả cục bộ nếu BE trả về saved fileID
        if (export && response.savedFileId != null && !response.savedFileId.isEmpty()) {
            try {
                String fetchedContent = fetchFile(response.savedFileId);
                mirrorLocalResult(response.savedFileId, (fetchedContent != null && !fetchedContent.isEmpty()) ? fetchedContent : jsonReq);
            } catch (Exception ex) {
                mirrorLocalResult(response.savedFileId, "{\"response\":" + jsonReq + "}");
            }
        }

        return response;
    }

    /**
     * Gửi yêu cầu khai phá đồng bộ (cmd = "mine") qua Backend.
     */
    public ContractMineResponse mine(String dataset, double partial, double f, double minOcc, int limit,
                                     List<String> versions, boolean export) throws IOException {
        int reqId = requestIdSeq.getAndIncrement();
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("id", reqId);
        req.put("v", PROTOCOL_VERSION);
        req.put("cmd", "mine");
        req.put("dataset", dataset);
        req.put("format", "fimi");
        req.put("partial", partial);
        req.put("f", f);
        req.put("minOcc", minOcc);
        req.put("limit", limit);
        if (versions != null && !versions.isEmpty()) {
            req.put("versions", versions);
        } else {
            req.put("versions", List.of("v1"));
        }
        req.put("export", export);

        String jsonReq = buildJsonString(req);
        ContractMineResponse response = sendStreamingRequest(jsonReq, null);

        // R14: Mirror kết quả cục bộ nếu BE trả về saved fileID
        if (export && response.savedFileId != null && !response.savedFileId.isEmpty()) {
            try {
                String fetchedContent = fetchFile(response.savedFileId);
                mirrorLocalResult(response.savedFileId, (fetchedContent != null && !fetchedContent.isEmpty()) ? fetchedContent : jsonReq);
            } catch (Exception ex) {
                mirrorLocalResult(response.savedFileId, jsonReq);
            }
        }

        return response;
    }

    /**
     * Gửi yêu cầu tra cứu thông số cửa sổ (cmd = "window").
     */
    public String requestWindow(double f, double minOcc) throws IOException {
        int reqId = requestIdSeq.getAndIncrement();
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("id", reqId);
        req.put("v", PROTOCOL_VERSION);
        req.put("cmd", "window");
        req.put("f", f);
        req.put("minOcc", minOcc);

        return sendRawRequest(buildJsonString(req));
    }

    /**
     * Tải file kết quả từ BE (cmd = "fetch").
     */
    public String fetchFile(String fileID) throws IOException {
        int reqId = requestIdSeq.getAndIncrement();
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("id", reqId);
        req.put("v", PROTOCOL_VERSION);
        req.put("cmd", "fetch");
        req.put("file", fileID);

        return sendRawRequest(buildJsonString(req));
    }

    /**
     * Gửi 1 dòng JSONL và nhận lại 1 dòng phản hồi từ persistent TCP Socket.
     */
    public synchronized String sendRawRequest(String jsonLine) throws IOException {
        try {
            ensureConnected();
            socketWriter.write(jsonLine.trim());
            socketWriter.newLine();
            socketWriter.flush();

            String line = socketReader.readLine();
            if (line == null) {
                close();
                throw new IOException("Backend đã đóng kết nối TCP");
            }
            return line;
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    /**
     * Gửi 1 dòng JSONL và đọc nhiều dòng event cho tới khi nhận final response envelope.
     */
    public synchronized ContractMineResponse sendStreamingRequest(String jsonReq, Consumer<String> eventConsumer) throws IOException {
        try {
            ensureConnected();
            socketWriter.write(jsonReq.trim());
            socketWriter.newLine();
            socketWriter.flush();

            String line;
            while ((line = socketReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // 1. Dòng sự kiện (event: progress, window, pattern, step...)
                if (line.contains("\"event\"")) {
                    if (eventConsumer != null) {
                        eventConsumer.accept(line);
                    }
                    continue;
                }

                // 2. Final response envelope có chứa "ok": true/false
                if (line.contains("\"ok\"") || (line.contains("\"id\"") && !line.contains("\"event\""))) {
                    return parseMineResponse(line);
                }

                // Dự phòng: đẩy cho eventConsumer nếu không nhận dạng được
                if (eventConsumer != null) {
                    eventConsumer.accept(line);
                }
            }
            close();
            throw new IOException("Backend đã đóng luồng trước khi gửi response envelope kết thúc");
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    /**
     * Lưu bản sao kết quả cục bộ (Mirroring) để tuân thủ ràng buộc R14 (D48).
     */
    public void mirrorLocalResult(String fileId, String content) {
        try {
            Path dir = Paths.get("mine");
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path localFile = dir.resolve("mine_" + fileId + ".txt");
            Files.writeString(localFile, content != null ? content : "", StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    // =========================================================================
    // Lightweight JSON Builder & Parser (Zero-dependency)
    // =========================================================================

    public static String buildJsonString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(valueToJson(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    private static String valueToJson(Object val) {
        if (val == null) return "null";
        if (val instanceof String s) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        if (val instanceof Number || val instanceof Boolean) {
            return val.toString();
        }
        if (val instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(valueToJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + val.toString() + "\"";
    }

    public static Boolean parseBoolean(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(json);
        return m.find() ? Boolean.parseBoolean(m.group(1)) : null;
    }

    public static Integer parseInteger(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    public static Long parseLong(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }

    public static Double parseDouble(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*([-+]?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?)");
        Matcher m = p.matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : null;
    }

    public static String parseString(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    public static List<String> parseVersions(String json) {
        if (json == null) return List.of("v1", "v2", "v3");
        int idx = json.indexOf("\"versions\"");
        if (idx == -1) return List.of("v1", "v2", "v3");

        int start = json.indexOf("[", idx);
        int end = -1;
        if (start != -1) {
            int depth = 0;
            boolean inQuote = false;
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                    inQuote = !inQuote;
                } else if (!inQuote) {
                    if (c == '[') depth++;
                    else if (c == ']') {
                        depth--;
                        if (depth == 0) {
                            end = i;
                            break;
                        }
                    }
                }
            }
        }
        if (start == -1 || end == -1 || end <= start) return List.of("v1", "v2", "v3");

        String content = json.substring(start + 1, end).trim();
        if (content.isEmpty()) return List.of("v1", "v2", "v3");

        List<String> result = new ArrayList<>();

        // Trường hợp 1: Mảng objects [{"id":"v1", ...}, {"id":"v2", ...}]
        Pattern objPattern = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
        Matcher objMatcher = objPattern.matcher(content);
        while (objMatcher.find()) {
            String vId = objMatcher.group(1).trim().toLowerCase();
            if (vId.matches("v[1-3]") && !result.contains(vId)) {
                result.add(vId);
            }
        }

        // Trường hợp 2: Mảng strings ["v1", "v2", "v3"]
        if (result.isEmpty()) {
            Pattern strPattern = Pattern.compile("\"([^\"]+)\"");
            Matcher strMatcher = strPattern.matcher(content);
            while (strMatcher.find()) {
                String val = strMatcher.group(1).trim().toLowerCase();
                if (val.matches("v[1-3]") && !result.contains(val)) {
                    result.add(val);
                }
            }
        }

        return result.isEmpty() ? List.of("v1", "v2", "v3") : result;
    }

    public static List<String> parseStringList(String json, String key) {
        if (json == null || key == null) return List.of();
        if ("versions".equalsIgnoreCase(key)) {
            return parseVersions(json);
        }
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[([^\\]]*)\\]");
        Matcher m = p.matcher(json);
        if (m.find()) {
            String inner = m.group(1).trim();
            if (inner.isEmpty()) return List.of();
            String[] tokens = inner.split(",");
            List<String> list = new ArrayList<>();
            for (String t : tokens) {
                String clean = t.trim().replaceAll("^\"|\"$", "");
                if (!clean.isEmpty()) list.add(clean);
            }
            return list;
        }
        return List.of();
    }

    /**
     * Chuẩn hóa chuỗi pattern thành dạng tập hợp đẹp {A, B, C} thay vì dính chùm hoặc bị lồng {{...}}.
     */
    public static String normalizePattern(String raw) {
        if (raw == null || raw.isBlank()) return "{}";
        String clean = raw.trim();
        while (clean.startsWith("{") && clean.endsWith("}")) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }
        String[] parts = clean.split("[,\\s]+");
        List<String> items = new ArrayList<>();
        for (String p : parts) {
            String item = p.replace("{", "").replace("}", "").trim();
            if (!item.isEmpty()) items.add(item);
        }
        return "{" + String.join(", ", items) + "}";
    }

    /**
     * Sinh khóa đại diện chuẩn cho tập mục (được sắp xếp từ điển) để phát hiện và loại bỏ mẫu trùng lặp (dedup).
     */
    public static String canonicalItemsetKey(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String clean = raw.replace("{", "").replace("}", "").trim();
        String[] parts = clean.split("[,\\s]+");
        List<String> items = new ArrayList<>();
        for (String p : parts) {
            String item = p.trim();
            if (!item.isEmpty()) items.add(item);
        }
        Collections.sort(items);
        return String.join(" ", items);
    }

    /**
     * Khử trùng lặp (Deduplicate) danh sách PatternResult theo khóa tập mục chuẩn.
     * Nếu có mẫu trùng khóa, giữ lại mẫu có DO cao nhất.
     */
    public static List<PatternResult> deduplicatePatterns(List<PatternResult> list) {
        if (list == null || list.isEmpty()) return list != null ? list : List.of();
        Map<String, PatternResult> dedupMap = new LinkedHashMap<>();
        for (PatternResult p : list) {
            if (p == null) continue;
            String key = canonicalItemsetKey(p.pattern());
            if (!dedupMap.containsKey(key) || p.doValue() > dedupMap.get(key).doValue()) {
                dedupMap.put(key, p);
            }
        }
        List<PatternResult> result = new ArrayList<>(dedupMap.values());
        result.sort((a, b) -> Double.compare(b.doValue(), a.doValue()));
        return result;
    }

    public static ContractMineResponse parseMineResponse(String json) {
        boolean ok = Boolean.TRUE.equals(parseBoolean(json, "ok"));
        String errCode = parseString(json, "code");
        String message = parseString(json, "message");

        int totalTx = parseInteger(json, "totalTransactions") != null ? parseInteger(json, "totalTransactions") : 0;
        double minSup = parseDouble(json, "minSup") != null ? parseDouble(json, "minSup") : 0.0;
        String savedFileId = parseString(json, "fileID");

        Long elapsed = parseLong(json, "elapsedMs");
        Double mem = parseDouble(json, "peakMemoryMb");
        if (mem == null) mem = parseDouble(json, "memoryMb");
        Integer cand = parseInteger(json, "candidates");
        Integer prun = parseInteger(json, "pruned");

        long elapsedMs = elapsed != null ? elapsed : 0L;
        double peakMemoryMb = mem != null ? mem : 0.0;

        List<PatternResult> patterns = new ArrayList<>();
        // Trích xuất chính xác các pattern trong mảng "patterns": [{...}]
        int patIdx = json.indexOf("\"patterns\"");
        if (patIdx != -1) {
            int arrStart = json.indexOf("[", patIdx);
            int arrEnd = -1;
            if (arrStart != -1) {
                int depth = 0;
                boolean inQuote = false;
                for (int i = arrStart; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                        inQuote = !inQuote;
                    } else if (!inQuote) {
                        if (c == '[') depth++;
                        else if (c == ']') {
                            depth--;
                            if (depth == 0) {
                                arrEnd = i;
                                break;
                            }
                        }
                    }
                }
            }

            if (arrStart != -1 && arrEnd > arrStart) {
                String sub = json.substring(arrStart, arrEnd + 1);
                Map<String, PatternResult> dedupMap = new LinkedHashMap<>();
                Pattern objPattern = Pattern.compile("\\{([^\\}]+)\\}");
                Matcher m = objPattern.matcher(sub);
                while (m.find()) {
                    String objStr = m.group(1);
                    String key = parseString("{" + objStr + "}", "key");
                    if (key == null) key = parseString("{" + objStr + "}", "pattern");
                    Double doVal = parseDouble("{" + objStr + "}", "do");
                    Integer sup = parseInteger("{" + objStr + "}", "support");
                    if (key != null) {
                        double doD = doVal != null ? doVal : 0.0;
                        int s = sup != null ? sup : 0;
                        String cleanPat = normalizePattern(key);
                        String cKey = canonicalItemsetKey(cleanPat);

                        List<Integer> tidList = new ArrayList<>();
                        int tidsIdx = objStr.indexOf("\"tids\"");
                        if (tidsIdx != -1) {
                            int tStart = objStr.indexOf("[", tidsIdx);
                            int tEnd = objStr.indexOf("]", tStart);
                            if (tStart != -1 && tEnd != -1) {
                                String[] tParts = objStr.substring(tStart + 1, tEnd).split(",");
                                for (String tp : tParts) {
                                    String cleanTp = tp.trim();
                                    if (!cleanTp.isEmpty()) {
                                        try {
                                            tidList.add(Integer.parseInt(cleanTp));
                                        } catch (NumberFormatException ignored) {}
                                    }
                                }
                            }
                        }

                        if (!dedupMap.containsKey(cKey) || doD > dedupMap.get(cKey).doValue()) {
                            dedupMap.put(cKey, new PatternResult(cleanPat, doD, doD, s, true, false, tidList));
                        }
                    }
                }
                patterns.addAll(dedupMap.values());
                patterns.sort((a, b) -> Double.compare(b.doValue(), a.doValue()));
            }
        }

        int candidates = cand != null ? cand : patterns.size();
        int pruned = prun != null ? prun : 0;

        return new ContractMineResponse(ok, errCode, message, totalTx, minSup, patterns, savedFileId,
                elapsedMs, peakMemoryMb, candidates, pruned);
    }

    /**
     * Đối tượng phản hồi khai phá theo Contract Protocol 1.
     */
    public record ContractMineResponse(
            boolean ok,
            String errorCode,
            String message,
            int totalTransactions,
            double minSup,
            List<PatternResult> patterns,
            String savedFileId,
            long elapsedMs,
            double peakMemoryMb,
            int candidates,
            int pruned
    ) {
        public ContractMineResponse(boolean ok, String errorCode, String message, int totalTransactions,
                                    double minSup, List<PatternResult> patterns, String savedFileId) {
            this(ok, errorCode, message, totalTransactions, minSup, patterns, savedFileId, 0L, 0.0,
                    patterns != null ? patterns.size() : 0, 0);
        }
    }
}
