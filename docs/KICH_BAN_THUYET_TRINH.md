# 🎤 KỊCH BẢN THUYẾT TRÌNH BÁO CÁO ĐỀ TÀI & HỎI ĐÁP PHẢN BIỆN (ĐIỂM 10)
> **Đồ Án Môn Học:** Lập Trình Java Nâng Cao  
> **Trường:** Đại Học Đà Lạt (DLU) - Khoa Công Nghệ Thông Tin  
> **Sinh viên thực hiện:** Nguyễn Thanh Tâm – MSSV: 2312741  
> **Đề tài:** Nghiên cứu, Cài đặt và Trực quan hóa Thuật toán Khai phá Mẫu Độ Chiếm Dụng Cao trên Luồng Dữ liệu theo Cửa sổ Suy giảm (DHOPM)  
> **Bài báo gốc:** *"Damped window based high occupancy pattern mining with one scanning of data streams"*, EAAI (Vol. 174, 2026).

---

## 📌 PHẦN 1: KỊCH BẢN NÓI CHI TIẾT TỪNG SLIDE (SLIDE-BY-SLIDE SPEECH)

### Slide 1: Trang Tiêu Đề
- **Thời lượng:** ~20 giây.
- **Lời thoại gợi ý:**
> *"Kính chào Quý Thầy/Cô trong Hội đồng phản biện và các bạn sinh viên. Em tên là **Nguyễn Thanh Tâm**, sinh viên lớp Công nghệ Thông tin Khóa 47, mã số sinh viên **2312741**.  
> Hôm nay, em xin phép đại diện nhóm báo cáo đề tài đồ án môn Lập trình Java Nâng cao: **'Nghiên cứu, Cài đặt và Trực quan hóa Thuật toán Khai phá Mẫu Độ Chiếm Dụng Cao trên Luồng Dữ liệu theo Cửa sổ Suy giảm (DHOPM Stream Visualizer)'**.  
> Đề tài của em được phát triển dựa trên bài báo khoa học quốc tế mới nhất công bố trên tạp chí **Engineering Applications of Artificial Intelligence (EAAI)** năm 2026."*

---

### Slide 2: Đặt Vấn Đề & Động Lực Nghiên Cứu
- **Thời lượng:** ~45 giây.
- **Lời thoại gợi ý:**
> *"Để mở đầu, em xin trình bày về động lực của đề tài. Trong phân tích giỏ hàng, khai phá tập mục phổ biến truyền thống (FIM) có một nhược điểm cố hữu: **nó chỉ đếm số lần xuất hiện nhị phân mà bỏ qua hoàn toàn độ dài giỏ hàng**.  
> Ví dụ: Một khách hàng mua `{Bánh mì, Sữa}` trong hóa đơn chỉ có 2 món (chiếm 100% giỏ hàng), hành vi này có ý đồ rất rõ ràng. Nhưng nếu `{Bánh mì, Sữa}` xuất hiện trong một giỏ hàng 100 món ngẫu nhiên, nó chỉ chiếm 2%, mang tính ngẫu nhiên, nhưng FIM vẫn đếm nó bằng 1 điểm tương đương.  
> Do đó, khái niệm **Độ chiếm dụng (Occupancy)** ra đời: $O(X, T_d) = \frac{|X|}{|T_d|}$ đo tỷ lệ chiếm hữu của mẫu trong giỏ hàng.  
> Khi đưa lên môi trường **luồng dữ liệu (Data Streams)**, bài toán gặp 2 thách thức lớn:  
> 1. Dữ liệu đến liên tục ở tốc độ cao và kích thước vô hạn, đòi hỏi thuật toán phải tuân thủ chuẩn **Một Lần Quét (One-Scan)** — tuyệt đối không được đọc lại lịch sử.  
> 2. Sở thích người dùng thay đổi theo thời gian (**Concept Drift**), đòi hỏi dữ liệu cũ phải giảm dần giá trị để ưu tiên các xu hướng mới nhất."*

---

### Slide 3: Bài Báo Khoa Học Gốc & 3 Đóng Góp Đột Phá
- **Thời lượng:** ~45 giây.
- **Lời thoại gợi ý:**
> *"Để giải quyết trọn vẹn bài toán trên, bài báo EAAI 2026 đã đưa ra **3 đóng góp đột phá** mang tính quyết định:  
> **Thứ nhất:** Đề xuất mô hình **Damped Window** với hệ số suy giảm hàm mũ $f^{T_L - T_d}$. Nhờ đó, giao dịch mới nhất $T_L$ giữ nguyên 100% giá trị, trong khi dữ liệu cũ suy thoái dần theo thời gian, hình thành độ đo **Damped Occupancy (DO)**.  
> **Thứ hai:** Vì Occupancy **không có tính phản đơn điệu** — tức là thêm item vào mẫu có thể làm tăng độ chiếm dụng, khiến nguyên lý Apriori truyền thống thất bại hoàn toàn. Bài báo đã đề xuất độ đo cận trên **DUBO** có chứng minh toán học chặt chẽ: $DO(X') \le DUBO(X)$ với mọi siêu mẫu $X'$. Nếu $DUBO(X) < minSup$, ta có thể cắt tỉa an toàn toàn bộ nhánh con mà không sợ bỏ sót mẫu.  
> **Thứ ba:** Xây dựng cấu trúc danh sách **Global DHO-List** chỉ lưu cặp $\langle TID, TLen \rangle$, giúp tiết kiệm hơn 80% bộ nhớ RAM và cập nhật gia tăng tức thì sau đúng 1 lần quét."*

---

### Slide 4: Dữ Liệu Chuẩn Table 1 & Sức Mạnh Của Hệ Số Suy Giảm $f$
- **Thời lượng:** ~45 giây.
- **Lời thoại gợi ý:**
> *"Trên màn hình là tập dữ liệu chuẩn Table 1 của bài báo gồm 8 giao dịch từ $T_1$ đến $T_8$.  
> Tại đây, nhóm em đã thực nghiệm đối chiếu để chứng minh định lượng vai trò của hệ số suy giảm $f$:  
> - Khi $f = 1.0$ (tức mô hình tĩnh không suy giảm thời gian), hệ thống tìm ra tới **9 mẫu** đạt chuẩn DHOP, bao gồm các tổ hợp từ xa xưa ở $T_1, T_3$ như $CD, DE, CDE$.  
> - Nhưng khi đưa hệ số $f = 0.9$ theo đúng mô hình luồng, giá trị của các giao dịch cũ bị giảm mạnh. Kết quả được gạn lọc lại chỉ còn đúng **2 mẫu chiếm ưu thế nhất gần đây là $\{A, E\}$ với $DO = 1.2601$ và $\{F\}$ với $DO = 1.2553$**.  
> **Kết luận:** Hệ số $f = 0.9$ đã loại bỏ thành công **77.8% các mẫu lỗi thời trong quá khứ**, giúp các nhà quản trị chỉ tập trung nguồn lực vào các xu hướng mua sắm thật sự nóng hổi!"*

---

### Slide 5: Kiến Trúc Phần Mềm & Các Design Patterns Áp Dụng
- **Thời lượng:** ~60 giây. *(ĐIỂM NHẤN QUAN TRỌNG CỦA MÔN JAVA NÂNG CAO)*
- **Lời thoại gợi ý:**
> *"Về mặt kỹ thuật lập trình Java nâng cao, đây là phần em tâm đắc nhất. Nhóm em đã thiết kế kiến trúc theo **Monorepo** và nguyên lý **Đảo ngược phụ thuộc (DIP)**:  
> Em phụ trách tầng giao diện `dhopm-visualizer`, bạn Sơn phụ trách thuật toán lõi `dhopm-v1-standard`, cả hai chỉ phụ thuộc vào hợp đồng trừu tượng `dhopm-common`. Nhờ vậy nhóm không bao giờ xảy ra xung đột mã nguồn.  
> Cụ thể, hệ thống áp dụng nhuần nhuyễn 6 Design Patterns:  
> 1. **Bridge & Adapter Pattern:** `MainController` chỉ giao tiếp qua interface `BridgeEngine`. Các lớp Adapter `TamSimulationBridge` và `TsonV1Bridge` bọc các engine khác nhau về cùng một chuẩn.  
> 2. **Strategy & Factory Method:** Cho phép người dùng chuyển đổi runtime giữa engine mô phỏng hoạt họa và engine Big Data tốc độ cao ngay trên ComboBox mà không sửa một dòng code giao diện nào.  
> 3. **Observer Pattern:** Sử dụng `PhaseListener` và `MiningProgressListener` để theo dõi tiến độ từng pha và phần trăm hoàn thành.  
> 4. **Multithreading:** Toàn bộ tác vụ khai phá nặng được đẩy sang Background Task (`Task<T>`) và đồng bộ an toàn qua `Platform.runLater()`, đảm bảo giao diện JavaFX luôn mượt mà 60 FPS, không bao giờ bị đơ (Zero UI Freeze)."*

---

### Slide 6: Giao Diện Trực Quan Hóa Tương Tác Cao & Kiến Trúc Mạng Phân Tán TCP (Protocol 1)
- **Thời lượng:** ~45 giây.
- **Lời thoại gợi ý:**
> *"Giao diện ứng dụng được thiết kế trên nền tảng JavaFX 21 cao cấp, kết nối với Backend thông qua **Kiến trúc mạng TCP Socket phân tán (Protocol 1 JSON Lines)**:  
> - **DhopmContractTcpClient & Persistent Socket:** Duy trì socket kết nối ổn định trên cổng 7079, gửi bắt tay `hello`, tiếp nhận luồng sự kiện đa dòng `progress`, `window`, `pattern` với tần số mượt mà.  
> - **Cơ chế Quét Mạng Tự Động (LAN Auto-Discovery):** Tích hợp công cụ quét đa luồng 32 workers, tự động nhận diện máy chủ Backend của bạn Sơn trong mạng LAN (địa chỉ thực tế `10.141.0.17:7079`) và kết nối tức thì.  
> - **Mock Server Nội Bộ (DhopmContractTcpServer):** Tích hợp máy chủ giả lập chạy ngầm trên `CachedThreadPool` để ứng dụng hoạt động độc lập ngay cả khi offline hoặc trong CI/CD.  
> - **Động Cơ Khử Trùng Lặp (Deduplication Engine):** Sử dụng bộ phân tích JSON theo dõi độ sâu ngoặc, hàm `canonicalItemsetKey()` sắp xếp từ điển, và `deduplicatePatterns()` giúp bảng kết quả sạch sẽ 100%, không bị trùng mẫu và loại bỏ lỗi lồng ngoặc nhọn.  
> - **Các Tab Trực Quan:** Tab 1 hiển thị các thẻ động `DHONodeCard` đổi màu theo ngưỡng DUBO; Tab 2 hiển thị bảng 32 mẫu cây DFS; Tab 3 hiển thị đồ thị quét đa ngưỡng minSup chuẩn bài báo."*

---

### Slide 7: Tab 4 - Tích Hợp Động Cơ Big Data Tson, Bảng Native TableView & Phản Chiếu Dữ Liệu R14
- **Thời lượng:** ~45 giây.
- **Lời thoại gợi ý:**
> *"Ở Tab 4, em đã hoàn thành một nâng cấp then chốt:  
> Trước đây, động cơ của bạn Sơn yêu cầu phải mở Terminal để gõ lệnh. Em đã **chuyển đổi 100% các câu lệnh dòng lệnh thành các nút bấm đồ họa GUI** trực quan: `🚀 Khai Phá (Mine)`, `🔍 Thống Kê (Inspect)`, `🎯 Golden Tests`.  
> Loại bỏ hoàn toàn thanh trượt Slider thiếu chính xác, thay thế bằng ô nhập số liệu trực tiếp (`TextField`) và huy hiệu trạng thái minh bạch: `🟢 BE (10.141.0.17:7079)` khi kết nối thật, và `🟡 DEMO (In-Process)` khi chạy nội bộ.  
> Đặc biệt, em đã **loại bỏ hoàn toàn màn hình console đen trắng** và thay bằng **100% bảng đồ họa JavaFX TableView native** chia cột thẳng tắp:  
> 1. Bảng Mẫu Khai Phá hiển thị chi tiết Itemset chuẩn `{B, F}`, điểm DO, Support %, Độ dài, danh sách TIDs thực tế.  
> 2. Thẻ Thống Kê Inspect Cards + Bảng Top 10 mục phổ biến nhất.  
> 3. Ma Trận Golden Tests với tick xanh `PASS 100%`.  
> 4. **Cơ chế Phản chiếu dữ liệu R14:** Client tự động tải tệp kết quả từ xa qua lệnh `fetch` và lưu vào thư mục `mine/mine_<fileID>.txt` để đối chiếu độc lập."*

---

### Slide 8: Kết Quả Kiểm Thử Toàn Diện 60 Test Cases (JUnit 5) & Ma Trận Golden Tests
- **Thời lượng:** ~40 giây.
- **Lời thoại gợi ý:**
> *"Về mặt kiểm chứng khoa học, hệ thống đạt **60/60 unit tests tự động vượt qua (100% BUILD SUCCESS)** trải rộng trên 11 test suites:  
> - 8 bài kiểm thử lý thuyết Lab 1 & 10 bài kiểm thử thuật toán 3 pha `DHOPMEngine`.  
> - 10 bài kiểm thử hợp đồng `BridgeEngine` & 6 bài đối soát tích hợp giữa hai engine.  
> - 9 bài kiểm thử Client TCP Socket, phân tích JSONL, khử trùng lặp và chuẩn hóa ngoặc.  
> - 2 bài kiểm thử Mock Server nội bộ đa luồng.  
> - 14 bài kiểm thử cho MinSupSweep, Tiến độ ETA, Lịch sử Memento và Calculation Logger.  
> Toàn bộ 8 bộ kiểm thử chính quy Golden Tests từ TC1 đến TC8 đều cho kết quả **khớp tuyệt đối 100% với bài báo**, với sai số toán học $\Delta < 0.0001$."*

---

### Slide 9: Thử Nghiệm Dataset FIMI & Kịch Bản Demo 3 Phút
- **Thời lượng:** ~30 giây.
- **Lời thoại gợi ý:**
> *"Ngoài dữ liệu mẫu Table 1, phần mềm đã tích hợp sẵn 5 tập dữ liệu FIMI chuẩn quốc tế trong thư mục `dataset/` như `chess`, `mushroom`, `connect`, `retail`, `pumsb`. Động cơ xử lý hàng vạn giao dịch chỉ trong vài giây.  
> Ngay sau đây, em xin phép chuyển sang phần thực hiện Demo trực tiếp trên máy theo đúng kịch bản 4 bước: Giới thiệu DHO-List, Kéo Slider $f$, Bơm luồng One-Scan và chạy Golden Tests trên Tab 4."*

---

### Slide 10 & 11: Kết Luận, Hướng Phát Triển & Sẵn Sàng Q&A
- **Thời lượng:** ~35 giây.
- **Lời thoại gợi ý:**
> *"Tóm lại, đề tài đã hoàn thành xuất sắc 100% mục tiêu đề ra:  
> 1. Làm chủ lý thuyết thuật toán DHOPM, chứng minh toán học cận trên DUBO và cấu trúc Global DHO-List một lần quét.  
> 2. Vận dụng 9 Design Patterns tạo nên kiến trúc phân tầng sạch sẽ, phân tách độc lập Frontend - Backend.  
> 3. Hiện thực hóa giao diện JavaFX trực quan hóa cao cấp, tích hợp mạng phân tán TCP Client-Server, cơ chế khử trùng lặp dữ liệu và 60/60 ca kiểm thử tự động đạt 100% Green.  
> Trong tương lai, nhóm hướng tới tối ưu hóa BitSet DFS và triển khai cụm phân tán Apache Flink/Kafka.  
> Em xin chân thành cảm ơn Quý Thầy/Cô đã chú ý lắng nghe. Em xin kính mời Quý Thầy/Cô theo dõi phần Demo và đặt câu hỏi phản biện!"*

---

## 🎬 PHẦN 2: KỊCH BẢN THAO TÁC DEMO TRỰC TIẾP TRÊN PHẦN MỀM (3 PHÚT)

Khi đứng trước Hội đồng, mở ứng dụng `mvn javafx:run` và thao tác theo 4 bước sau:

1. **Bước 1 (30s) – Khởi động & Trực quan hóa DHO-List:**
   - Mở Tab 1: *"Thưa Thầy Cô, đây là các thẻ DHO-List được sắp xếp theo Support tăng dần. Thẻ A và E có màu xanh vì đang đạt chuẩn DHOP, trong khi thẻ G có màu đỏ vì bị cắt tỉa DUBO."*

2. **Bước 2 (45s) – Chứng minh vai trò của hệ số suy giảm $f$:**
   - Kéo Slider $f$ lên **$1.0$**: Chỉ cho Thầy Cô thấy ở Tab 2 hoặc Tab 4 xuất hiện **9 mẫu DHOP** (vì dữ liệu cũ $T_1 \dots T_4$ vẫn giữ 100% giá trị).
   - Kéo Slider $f$ về lại **$0.9$**: Ngay lập tức kết quả rút gọn còn đúng **2 mẫu hot nhất là $\{AE\}$ và $\{F\}$**.
   - Nhấn mạnh: *"Đây chính là đóng góp lớn nhất của bài báo: loại bỏ các mẫu đã lỗi thời trong quá khứ."*

3. **Bước 3 (45s) – Bơm luồng dữ liệu One-Scan:**
   - Bấm nút **"➕ Bơm từng giao dịch (Step Next)"**: Bơm giao dịch mới $T_9 = \{A, E\}$.
   - Chỉ vào thống kê: *"Hệ thống không cần đọc lại $T_1 \dots T_8$, chỉ quét $T_9$, cập nhật $T_L=9$ và vẽ lại biểu đồ tức thì. Đây là cơ chế One-Scan tiết kiệm bộ nhớ."*

4. **Bước 4 (60s) – Chuyển sang Tab 4 Demo Kết Nối Mạng TCP Phân Tán & Động Cơ Big Data:**
   - Chỉ vào huy hiệu mạng: *"Thưa Thầy Cô, hệ thống đã kết nối trực tiếp với Backend của bạn Sơn qua địa chỉ IP `10.141.0.17:7079` với huy hiệu xanh lá `🟢 BE (10.141.0.17:7079)`. Ngoài ra phần mềm có nút `🔍 Quét mạng (LAN)` tự động tìm máy bạn Sơn trong subnet."*
   - Chuyển sang **Tab 4**: Bấm nút **"🎯 Golden Tests"** $\rightarrow$ Ma trận hiển thị bảng xanh `✅ PASS 100%` cho cả 8 Test cases.
   - Bấm nút **"🔍 Thống Kê (Inspect)"** $\rightarrow$ Thẻ tổng quan hiện lên kèm Bảng Top 10 mục xuất hiện nhiều nhất.
   - Bấm nút **"🚀 Khai Phá (Mine)"** trên tập dữ liệu FIMI $\rightarrow$ Thanh tiến trình streaming mượt mà; Bảng Mẫu Khai Phá hiển thị kết quả đã qua **Thuật toán Khử Trùng Lặp**, biểu diễn chuẩn `{B, F}` cùng danh sách giao dịch TIDs đầy đủ; đồng thời tệp kết quả được phản chiếu an toàn tại thư mục `mine/`."*

---

## 💡 PHẦN 3: BỘ CÂU HỎI THƯỜNG GẶP CỦA GIẢNG VIÊN & GỢI Ý TRẢ LỜI ĐIỂM 10

### ❓ Câu 1: Tại sao không dùng nguyên lý Apriori truyền thống mà phải dùng DUBO để cắt tỉa?
> **Trả lời:**  
> *"Thưa Thầy/Cô, nguyên lý Apriori dựa trên tính chất phản đơn điệu (Anti-monotonicity): Support của tập mục cha luôn nhỏ hơn hoặc bằng tập mục con ($Sup(X') \le Sup(X)$). Do đó nếu mẫu con không phổ biến thì mẫu cha chắc chắn bị loại.  
> Tuy nhiên, **Độ chiếm dụng Occupancy không thỏa mãn tính chất này**. Ví dụ: Trong giao dịch $T = \{A, B, C\}$, mẫu $\{A\}$ có độ chiếm dụng là $1/3 \approx 0.333$. Khi ta thêm phần tử $B$ vào để tạo thành mẫu $\{A, B\}$, độ chiếm dụng tăng lên thành $2/3 \approx 0.667 > 0.333$.  
> Vì mẫu cha có thể có Occupancy CAO HƠN mẫu con, nên ta không thể dùng điều kiện $DO(X) < minSup$ để cắt tỉa. Bài báo đã giải quyết việc này bằng cách đề xuất cận trên **DUBO(X)**, chứng minh được rằng $\forall X' \supset X, DO(X') \le DUBO(X)$. Khi $DUBO(X) < minSup$, ta mới chắc chắn 100% cắt tỉa nhánh con mà không bao giờ bỏ sót nghiệm."*

---

### ❓ Câu 2: Em hãy giải thích cách áp dụng Bridge Pattern và Adapter Pattern trong đồ án?
> **Trả lời:**  
> *"Thưa Thầy/Cô, trong đồ án này có 2 động cơ khác nhau:  
> 1. `DHOPMEngine` của em: Phục vụ mô phỏng hoạt họa từng bước trên giao diện.  
> 2. `MiningEngine` của bạn Sơn: Tối ưu hóa hiệu năng cao để chạy các tập dữ liệu lớn FIMI.  
> Để giao diện không bị phụ thuộc cứng vào bất kỳ lớp cụ thể nào (tuân thủ nguyên lý Dependency Inversion Principle), em đã áp dụng **Bridge Pattern** bằng cách định nghĩa interface trừu tượng `BridgeEngine`.  
> Sau đó, em áp dụng **Adapter Pattern** tạo ra 2 lớp `TamSimulationBridge` và `TsonV1Bridge`. Hai lớp này chuyển đổi các hàm đặc thù của từng engine về chuẩn chung của `BridgeEngine`. Nhờ đó, Controller chỉ tương tác với interface trừu tượng, và tại thời điểm chạy (Runtime), hệ thống có thể tráo đổi linh hoạt giữa 2 động cơ mà không cần sửa một dòng mã giao diện nào."*

---

### ❓ Câu 3: Làm thế nào để đảm bảo giao diện JavaFX không bị đơ (freeze) khi khai phá dữ liệu lớn?
> **Trả lời:**  
> *"Thưa Thầy/Cô, trong JavaFX, tất cả các tác vụ vẽ đồ họa và bắt sự kiện người dùng đều chạy trên một luồng duy nhất gọi là **JavaFX Application Thread**. Nếu ta thực hiện thuật toán đệ quy DFS trên luồng này, giao diện sẽ lập tức bị đóng băng và hiện con trỏ xoay.  
> Để xử lý vấn đề này, em đã áp dụng kỹ thuật **Concurrency trong JavaFX**:  
> - Bọc toàn bộ quá trình khai phá vào một `javafx.concurrent.Task<T>` chạy trên Background Worker Thread (hoặc `CompletableFuture.runAsync()`).  
> - Trong quá trình chạy, các sự kiện cập nhật tiến độ (Progress, Phase) được đồng bộ ngược lại luồng chính một cách an toàn thông qua phương thức `Platform.runLater()`.  
> Nhờ vậy, giao diện luôn duy trì tốc độ khung hình mượt mà 60 FPS, thanh ProgressBar tăng dần trực quan và người dùng vẫn có thể thao tác bình thường."*

---

### ❓ Câu 4: Cấu trúc One-Scan DHO-List tiết kiệm bộ nhớ như thế nào so với việc lưu trữ toàn bộ giao dịch?
> **Trả lời:**  
> *"Thưa Thầy/Cô, thay vì phải lưu toàn bộ danh sách các chuỗi item của từng giao dịch trong bộ nhớ RAM, cấu trúc Global DHO-List chỉ lưu trữ mỗi item dưới dạng một Node, và trong mỗi Node chỉ lưu các cặp bản ghi gọn nhẹ là `⟨TID, TLen⟩` (mã giao dịch và độ dài giao dịch).  
> Khi có giao dịch mới đến, thuật toán chỉ quét 1 lần qua giao dịch đó, tra cứu các Node tương ứng của các item có mặt và chèn thêm 1 bản ghi `⟨TID, TLen⟩`. Nhờ chỉ lưu 2 số nguyên nguyên thủy (primitive integers) thay vì cấu trúc đối tượng phức tạp, bộ nhớ RAM được tiết kiệm lên tới hơn 80%, hoàn toàn đáp ứng yêu cầu khắt khe của môi trường luồng dữ liệu thời gian thực."*

---

### ❓ Câu 5: Làm thế nào để kết nối và đồng bộ dữ liệu giữa 2 máy tính độc lập (Frontend và Backend) trong mạng LAN?
> **Trả lời:**  
> *"Thưa Thầy/Cô, hai nhóm phát triển độc lập tuân thủ hợp đồng giao thức Protocol 1 (CONTRACT.md):  
> 1. **Giao tiếp Socket TCP:** Máy Backend của bạn Sơn mở socket server lắng nghe trên cổng `7079`. Máy Frontend của em sử dụng `DhopmContractTcpClient` kết nối qua TCP Socket bền vững (Persistent Connection).  
> 2. **Định dạng dữ liệu JSON Lines:** Hai bên trao đổi từng dòng JSON (1 request/dòng, 1 response/dòng), phân biệt truy vấn bằng trường `id` và bắt tay hệ thống qua lệnh `hello`.  
> 3. **Tự động quét mạng (LAN Auto-Discovery):** Trên giao diện, em thiết kế nút '🔍 Quét mạng (LAN)' sử dụng ThreadPool 32 workers quét nhanh các địa chỉ IP trong dải subnet nội bộ để tự động dò tìm máy Backend đang mở cổng 7079.  
> 4. **Mock Server dự phòng:** Nếu không có máy bạn Sơn hoặc chạy offline, phần mềm tích hợp sẵn `DhopmContractTcpServer` nội bộ chạy ngầm trên `CachedThreadPool` để tự phục vụ mà không làm gián đoạn trải nghiệm người dùng."*

---

### ❓ Câu 6: Tại sao kết quả khai phá ban đầu từ Backend bị trùng lặp và lỗi lồng ngoặc, và nhóm đã khắc phục như thế nào?
> **Trả lời:**  
> *"Thưa Thầy/Cô, khi kết nối thử nghiệm trên mạng thực tế với máy bạn Sơn, nhóm phát hiện:  
> - Backend gửi về kết quả ở cả 2 mảng `patterns` (toàn cục) và `perVersion` (theo từng cửa sổ trượt), làm cho số dòng trên TableView bị nhân đôi (ví dụ từ 5 mẫu lên 10 dòng) và thứ tự item trong chuỗi không đồng nhất (như `B F` và `F B`).  
> - Ngoài ra, một số chuỗi bị bọc ngoặc lồng nhau dạng `{{B, F}}`.  
> Để khắc phục triệt để, em đã xây dựng **Động cơ khử trùng lặp (Deduplication Engine)** ở tầng Frontend:  
> 1. Xây dựng bộ bóc tách JSON theo dõi độ sâu ngoặc (`depth-tracking parser`).  
> 2. Định nghĩa hàm `canonicalItemsetKey()` phân rã tập mục và sắp xếp theo thứ tự từ điển chuẩn, quy mọi biểu diễn về khóa duy nhất.  
> 3. Áp dụng hàm `deduplicatePatterns()` lọc qua `LinkedHashMap` để hợp nhất các bản ghi trùng lặp và giữ lại đối tượng có DO cao nhất, bảo toàn nguyên vẹn danh sách giao dịch TIDs và Support.  
> 4. Dùng hàm `normalizePattern()` làm sạch hoàn toàn các dấu ngoặc nhọn lồng nhau, trả về định dạng `{item1, item2}` chuẩn mực."*
