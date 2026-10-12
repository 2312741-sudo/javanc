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

    public DhopmContractTcpClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public DhopmContractTcpClient(String host, int port) {
        this.host = host;
        this.port = port;
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
        String jsonResp = sendRawRequest(jsonReq);
        if (jsonResp == null || jsonResp.isEmpty()) {
            throw new IOException("Không nhận được phản hồi từ Backend TCP 7079");
        }

        ContractMineResponse response = parseMineResponse(jsonResp);

        // R14: Mirror kết quả cục bộ nếu BE trả về saved fileID
        if (export && response.savedFileId != null && !response.savedFileId.isEmpty()) {
            try {
                String fetchedContent = fetchFile(response.savedFileId);
                mirrorLocalResult(response.savedFileId, (fetchedContent != null && !fetchedContent.isEmpty()) ? fetchedContent : jsonResp);
            } catch (Exception ex) {
                mirrorLocalResult(response.savedFileId, jsonResp);
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
        req.put("fileID", fileID);

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
        // Trích xuất các pattern trong mảng "patterns": [{...}]
        int patIdx = json.indexOf("\"patterns\"");
        if (patIdx != -1) {
            int arrStart = json.indexOf("[", patIdx);
            int arrEnd = json.lastIndexOf("]");
            if (arrStart != -1 && arrEnd > arrStart) {
                String sub = json.substring(arrStart, arrEnd + 1);
                // Phân tích từng object {...}
                Pattern objPattern = Pattern.compile("\\{([^\\}]+)\\}");
                Matcher m = objPattern.matcher(sub);
                while (m.find()) {
                    String objStr = m.group(1);
                    String key = parseString("{" + objStr + "}", "key");
                    Double doVal = parseDouble("{" + objStr + "}", "do");
                    Integer sup = parseInteger("{" + objStr + "}", "support");
                    if (key != null) {
                        double doD = doVal != null ? doVal : 0.0;
                        int s = sup != null ? sup : 0;
                        patterns.add(new PatternResult("{" + key + "}", doD, doD, s, true, false, List.of()));
                    }
                }
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
