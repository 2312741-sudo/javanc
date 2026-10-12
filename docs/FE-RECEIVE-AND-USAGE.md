# ĐẶC TẢ GIAO DIỆN FE: DỮ LIỆU NHẬN, SỬ DỤNG VÀ RÀNG BUỘC (FE-RECEIVE-AND-USAGE)

> **Tài liệu chuẩn hóa trao đổi Frontend ↔ Backend**  
> **Dự án:** DHOPM Visualizer (`javanc`, JavaFX) ↔ DHOPM Backend CLI (`dhopm-cli`, C++/Java)  
> **Giao thức:** TCP Socket (Cổng mặc định `7079`), Wire format: JSON Lines (JSONL), Protocol Version: `1` (`"v": 1`)  
> **Tác giả:** Nguyễn Thanh Tâm (2312741) — Frontend & Bridge Protocol Engineer  
> **Căn cứ tham chiếu:** `BRIDGE_TCP_FE_PROPOSAL.md`, `CONTRACT.md`, `08-FE-CONTRACT.md`

---

## 1. Giới thiệu & Mục tiêu

Tài liệu này định nghĩa chi tiết:
1. **Các lệnh (Commands) và Luồng dữ liệu (Streams)** mà Frontend (`javanc`) gửi tới Backend qua TCP Socket 7079.
2. **Cấu trúc sự kiện (Events) và Phản hồi (Responses)** mà FE tiếp nhận từ BE, bao gồm kiểu dữ liệu, tần suất phát sinh, và kích thước dự kiến.
3. **Mục đích sử dụng cụ thể của từng trường dữ liệu trên giao diện FE** (hiển thị KPI, vẽ đồ thị, cập nhật thanh tiến trình, hiển thị bảng mẫu, ETA).
4. **Các giới hạn kỹ thuật của Frontend** (ngưỡng dòng log, độ trễ render, bộ đệm bộ nhớ) để BE tối ưu hóa tần suất phát stream.
5. **Bảng đối chiếu 3 cột minh bạch hóa nguồn dữ liệu:**
   - Dữ liệu BE cung cấp theo hợp đồng chuẩn `08-FE-CONTRACT.md`.
   - Dữ liệu FE thực tế sử dụng.
   - Các trường số liệu ước lượng/nội bộ FE cần loại bỏ triệt để (`estPruned`, `estCandidates`, đo JVM Heap của FE).

---

## 2. Danh mục Lệnh, Tần suất, Payload và Mục đích Sử dụng của FE

| Lệnh (`cmd`) | Thời điểm gọi từ FE | Event / Phản hồi BE phát | Kiểu dữ liệu | Tần suất & Độ lớn dự kiến | FE dùng để làm gì |
|---|---|---|---|---|---|
| **`hello`** | Khi khởi động app, kết nối lại hoặc kiểm tra liveness | Response: `{"id":0,"v":1,"ok":true,"protocol":1,"manager":"...","commands":[...]}` | JSON Object envelope | 1 lần khi bắt tay, payload < 500 B | Xác nhận BE sẵn sàng, hiển thị badge trạng thái kết nối `🟢 Backend (TCP 7079)`, đọc danh sách lệnh BE hỗ trợ. |
| **`stream`** | Người dùng bấm **"Khai Phá Dataset" (Start Mining)** trên giao diện chính | 1. `{"event":"progress","fraction":0.45,"elapsedMs":1200,"etaMs":1400,"candidates":3420,"dhop":18}`<br>2. `{"event":"window","f":0.9,"TL":10000,"minSup":1500.0}`<br>3. `{"event":"pattern","key":"1 2 3","do":1520.4,"support":1600,"len":3}`<br>4. Final Envelope: `{"id":1,"v":1,"ok":true,"fileID":"abc123","totalTransactions":10000,"patterns":[...]}` | JSON Lines đa dòng: nhiều sự kiện + 1 envelope kết thúc | - `progress`: 5–10 Hz (cứ mỗi 100-200ms)<br>- `pattern`: chunking hoặc gửi top patterns<br>- `final`: 1 lần kết thúc (vài KB - vài MB tùy limit) | - Cập nhật thanh tiến trình `ProgressBar` và nhãn `ETA` thời gian thực.<br>- Cập nhật KPI số mẫu DHOP và số ứng viên.<br>- Cập nhật thông số cửa sổ suy giảm (TL, minSup tuyệt đối).<br>- Điền vào bảng mẫu (`TableView`), vẽ biểu đồ phân phối chiều dài/support.<br>- Lưu `fileID` để tự động tải bản sao đối chứng (Mirror). |
| **`mine`** | Chạy khai phá đồng bộ (batch mode) khi người dùng chạy quét tham số (Sweep/Benchmark) | Final Envelope: `{"id":2,"v":1,"ok":true,"totalTransactions":N,"minSup":S,"elapsedMs":T,"peakMemoryMb":M,"fileID":"...","patterns":[...]}` | 1 dòng JSON response | 1 lần mỗi điểm quét minSup (1-10 lần/lượt sweep), payload 5 KB – 5 MB | - Điền điểm dữ liệu vào bảng Sweep Benchmark (Tab 3).<br>- Vẽ 5 biểu đồ hiệu năng: Runtime, Memory, Pattern Count, Pruned Count, Speedup.<br>- Ghi lịch sử khai phá (`MiningHistoryManager`) với thông số chuẩn từ BE. |
| **`inspect`** | Người dùng bấm **"Thống kê (Inspect)"** | Final Envelope: `{"id":3,"v":1,"ok":true,"totalTransactions":N,"distinctItems":D,"avgTxLength":L,"maxItem":M,"topItems":[{"item":1,"count":500},...]}` | 1 dòng JSON response | 1 lần theo yêu cầu, payload < 50 KB | - Điền thông số dataset vào thẻ thông tin: Số giao dịch, Số mục phân biệt, Chiều dài trung bình.<br>- Vẽ biểu đồ tần suất Top Item trong dataset (Tab 4 View 2). |
| **`detail`** | Người dùng bấm **"Top DO (Detail)"** | Final Envelope: `{"id":4,"v":1,"ok":true,"patterns":[{"key":"...","do":...,"support":...,"tids":[...]}], "memoryMb":..., "elapsedMs":...}` | 1 dòng JSON response | 1 lần theo yêu cầu, payload 10 KB – 500 KB (giới hạn theo `limit`) | - Hiển thị danh sách Top các mẫu có Dynamic Occurrence (DO) cao nhất.<br>- Hiển thị chi tiết TIDs xuất hiện và biểu đồ so sánh DO vs Support. |
| **`window`** | Khi người dùng nhập thay đổi tham số $f$ hoặc $\partial$ | Final Envelope: `{"id":5,"v":1,"ok":true,"f":0.9,"minOcc":0.0,"w":1000,"effectiveN":...,"minSupAbs":...}` | 1 dòng JSON response | 1 lần sau khi người dùng xác nhận tham số, payload < 500 B | - Cập nhật nhãn `minSup tuyệt đối` ($minSup = \partial \times N_{eff}$ hoặc $\partial \times TL$).<br>- Hiển thị độ rộng cửa sổ hiệu dụng $W$ và kích thước mẫu tương đương. |
| **`fetch`** | Tự động gọi ngay sau khi `mine` hoặc `stream` hoàn tất có trả về `fileID` | Final Envelope: `{"id":6,"v":1,"ok":true,"fileID":"...","content":"...","lineCount":L,"byteSize":B}` | 1 dòng JSON response | 1 lần mỗi lần khai phá thành công có export, payload 10 KB – 10 MB | - Tải nguyên văn file kết quả khai phá mà BE đã sinh.<br>- Lưu xuống thư mục cục bộ `mine/mine_<fileID>.txt` làm bản đối chứng chứng thực hợp đồng (R14 Mirroring). |
| **`session.*`** | (Dự phòng cho phiên làm việc tương tác nhiều bước) | Final Envelope: `{"id":7,"v":1,"ok":true,"sessionId":"...","state":"..."}` | 1 dòng JSON response | 1 lần khi mở/đóng phiên tương tác | Duy trì ngữ cảnh dataset đã tải sẵn trong RAM của BE để tránh đọc lại đĩa. |

---

## 3. Ràng buộc & Giới hạn Kỹ thuật của Frontend (`javanc`)

Frontend được phát triển bằng JavaFX đa luồng. Để đảm bảo giao diện đạt tốc độ khung hình 60 FPS mượt mà và không tràn bộ nhớ, FE thiết lập các giới hạn kỹ thuật sau:

1. **Giới hạn số dòng Log sự kiện (`CalculationLogger` / Tab 5):**
   - Bộ nhớ đệm hiển thị: Tối đa **5,000 dòng log gần nhất**.
   - Khi vượt quá 5,000 dòng, cơ chế FIFO tự động loại bỏ các dòng cũ nhất để tránh quá tải heap JavaFX UI thread.
2. **Tần suất cập nhật UI thanh tiến trình (`progress` events):**
   - Backend nên phát sự kiện `progress` ở tần suất tối đa **10 Hz (khoảng 100ms/lần)**. Việc phát quá dày (hàng nghìn event/giây) sẽ làm nghẽn hàng đợi `Platform.runLater()`.
3. **Giới hạn kích thước danh sách mẫu hiển thị trực tiếp (`patterns`):**
   - Frontend hiển thị tốt nhất với danh sách mẫu $\le 10,000$ mẫu (`TableView` có ảo hóa).
   - Với các bộ dữ liệu dense ($minSup \le 0.01$), số mẫu có thể bùng nổ hàng triệu mẫu. FE yêu cầu tham số `limit` (mặc định 1,000 – 5,000 mẫu) khi truyền qua `stream`/`mine`/`detail` để bảo vệ bộ nhớ cả 2 phía. Toàn bộ mẫu đầy đủ được BE lưu vào đĩa và FE tải qua `fetch`.
4. **Thời gian chờ phản hồi TCP (Socket Timeout):**
   - Thời gian kết nối bắt tay (`connect timeout`): **2,500 ms**.
   - Thời gian đọc luồng dữ liệu (`read timeout` / `soTimeout`): **60,000 ms (60 giây)** cho các tác vụ khai phá lớn. Trong quá trình stream, mỗi khi có event dòng mới đến, bộ đếm thời gian được duy trì.

---

## 4. Bảng Đối Chiếu Nguồn Số Liệu 3 Cột

Bảng dưới đây phân định rõ ràng giữa dữ liệu BE cung cấp theo hợp đồng, dữ liệu FE thực nhận và sử dụng, cùng các trường số liệu **ước lượng giả định (fake estimations) của FE đã bị loại bỏ hoàn toàn**:

| STT | (a) Đặc tả BE theo `08-FE-CONTRACT.md` | (b) FE Thực Tế Sử Dụng | (c) Phần FE Tự Ước Lượng Trước Đây — ĐÃ LOẠI BỎ TRIỆT ĐỂ |
|---|---|---|---|
| **1** | **Thời gian thực thi (`elapsedMs`)**: Đo từ lõi C++/Java BE từ lúc bắt đầu thuật toán đến khi xuất xong mẫu. | Đọc trực tiếp từ trường `elapsedMs` trong event `progress` và response `mine`/`stream`. | ❌ **LOẠI BỎ:** Đo bằng `System.nanoTime()` của JVM FE (bị méo mó bởi chi phí đồ họa JavaFX và GC nội bộ). |
| **2** | **Bộ nhớ tiêu thụ (`peakMemoryMb`)**: Đo bộ nhớ RAM thực tế mà tiến trình Backend sử dụng khi xử lý cây DHO. | Đọc trực tiếp từ trường `peakMemoryMb` / `memoryMb` trong response của BE. | ❌ **LOẠI BỎ:** Lấy `Runtime.getRuntime().totalMemory() - freeMemory()` của JVM FE (đây là RAM của JavaFX UI, hoàn toàn vô nghĩa với thuật toán BE). |
| **3** | **Số ứng viên đã duyệt (`candidates` / `totalVisited`)**: Số node/prefix mà BE thực sự duyệt qua trong không gian tìm kiếm. | Đọc từ trường `candidates` trong stream `progress` và thống kê tổng kết của BE. | ❌ **LOẠI BỎ:** Công thức tự bịa `estCandidates = dhopCount * (1.0 + (1.0 - partial) * 4.0)` tại `MinSupSweepService.java:139-140`. |
| **4** | **Số mẫu bị cắt tỉa (`pruned` / `prunedCount`)**: Số nhánh bị nhánh cận DUBO/minSup loại bỏ thực tế bởi BE. | Đọc từ trường `pruned` của BE (hoặc `candidates - dhopCount` dựa trên số ứng viên thực từ BE). | ❌ **LOẠI BỎ:** Công thức tự bịa `estPruned = estCandidates - dhopCount` tại `MinSupSweepService.java:140`. |
| **5** | **Tiến độ và ETA (`fraction`, `etaMs`)**: Tính toán dựa trên số giao dịch đã xử lý / cây tiền tố đã duyệt của BE. | Nhận trực tiếp từ sự kiện `{"event":"progress","fraction":..., "etaMs":...}` qua TCP. | ❌ **LOẠI BỎ:** Bộ đếm thời gian và tỷ lệ giả lập nội bộ trong controller. |
| **6** | **Tỷ lệ cắt tỉa (% Cắt tỉa)**: Đo lường hiệu quả của kỹ thuật chặn trên DUBO so với không gian tìm kiếm. | Tính từ số liệu thực: `(pruned / candidates) * 100%` với số liệu do BE gửi. | ❌ **LOẠI BỎ:** Tỷ lệ % ảo tính từ các biến ước lượng `estPruned`. |
| **7** | **Bản sao kiểm chứng kết quả (Mirroring R14)**: BE lưu file và trả về `fileID`, FE tải file thật qua `fetch`. | FE lưu nguyên văn nội dung nhận từ `fetch` vào `mine/mine_<fileID>.txt`. | ❌ **LOẠI BỎ:** Lưu chuỗi JSON response thô hoặc bỏ qua khi TCP không chạy. |
| **8** | **Trạng thái nguồn dữ liệu (Data Source)**: Phân định rạch ròi giữa BE độc lập và Demo mô phỏng. | Hiển thị rõ trên UI: `🟢 Nguồn: Backend (TCP 7079)` hoặc `🟡 Nguồn: DEMO (In-Process)`. | ❌ **LOẠI BỎ:** Fallback im lặng khiến người dùng hiểu nhầm dữ liệu demo là kết quả BE. |

---

## 5. Kết luận & Cam kết Kiến trúc

1. **Frontend không tự bịa số liệu:** Mọi chỉ số benchmark, số lượng mẫu, thời gian tính toán và bộ nhớ được hiển thị trên Tab 3 (Benchmark Sweep) và Tab 4 (Lịch sử) **100% bắt nguồn từ Backend TCP**.
2. **Quarantine chế độ DEMO:** Chế độ mô phỏng nội bộ của Tâm (`TAM_SIMULATION` với 8 giao dịch mẫu) được gắn nhãn `[DEMO]` rõ ràng trên UI, chỉ phục vụ mục đích minh họa từng bước cho hội đồng chấm đồ án, **tuyệt đối không ghi vào lịch sử benchmark hay báo cáo đối chứng khoa học**.
3. **Tuân thủ toàn diện Protocol 1:** Kết nối TCP duy trì chế độ đọc nhiều dòng (multi-line streaming JSONL), hỗ trợ đầy đủ `stream`, `mine`, `window`, `inspect`, `detail`, và `fetch`.
