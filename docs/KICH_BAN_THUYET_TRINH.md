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

### Slide 6: Giao Diện Trực Quan Hóa Tương Tác Cao (Tabs 1, 2, 3)
- **Thời lượng:** ~30 giây.
- **Lời thoại gợi ý:**
> *"Giao diện ứng dụng được thiết kế trên nền tảng JavaFX với 3 Tab đầu tiên phục vụ mục đích trực quan hóa:  
> - **Tab 1:** Hiển thị cấu trúc Global DHO-List bằng các thẻ động `DHONodeCard` sắp xếp theo thứ tự Support tăng dần $G \prec B \prec A \prec C \prec D \prec E \prec F$. Thẻ tự động đổi màu: Xanh là đạt DHOP, Đỏ là bị cắt tỉa DUBO.  
> - **Tab 2:** Cung cấp bảng chi tiết 32 mẫu ứng viên trong cây duyệt DFS tương ứng với Table 4 của bài báo.  
> - **Tab 3:** Biểu đồ đường Live LineChart cập nhật thời gian thực điểm DO của các item so sánh trực tiếp với đường ngưỡng $minSup$ màu đỏ."*

---

### Slide 7: Tab 4 - Tích Hợp Động Cơ Big Data Tson & Bảng Native TableView
- **Thời lượng:** ~45 giây.
- **Lời thoại gợi ý:**
> *"Ở Tab 4, em đã hoàn thành một nâng cấp then chốt:  
> Trước đây, động cơ của bạn Sơn yêu cầu phải mở Terminal để gõ lệnh. Em đã **chuyển đổi 100% các câu lệnh dòng lệnh thành các nút bấm đồ họa GUI** trực quan: `🚀 Khai Phá (Mine)`, `🔍 Thống Kê (Inspect)`, `🎯 Golden Tests`.  
> Người dùng có thể chọn hoặc tự tay gõ bất kỳ giới hạn giao dịch nào vào ô ComboBox.  
> Đặc biệt, để giải quyết triệt để tình trạng lệch cột font chữ khi dùng console TextArea trên macOS, em đã **loại bỏ hoàn toàn màn hình console đen trắng** và thay bằng **100% bảng đồ họa JavaFX TableView native** chia cột thẳng tắp, hỗ trợ sắp xếp linh hoạt:  
> 1. Bảng Mẫu Khai Phá hiển thị chi tiết Itemset, DO, Support, Độ dài, TIDs.  
> 2. Thẻ Thống Kê Inspect Cards + Bảng Top 10 mục phổ biến nhất.  
> 3. Ma Trận Golden Tests với tick xanh `PASS 100%`."*

---

### Slide 8: Kết Quả Kiểm Thử & Ma Trận Golden Tests (TC1 - TC8)
- **Thời lượng:** ~30 giây.
- **Lời thoại gợi ý:**
> *"Về mặt kiểm chứng khoa học, hệ thống đạt **38/38 unit tests tự động vượt qua (100% BUILD SUCCESS)**.  
> Toàn bộ 8 bộ kiểm thử chính quy Golden Tests từ TC1 đến TC8 đều cho kết quả **khớp tuyệt đối 100% với bài báo**, với sai số toán học tối đa $\Delta < 0.0001$.  
> Điều này khẳng định tính đúng đắn và độ tin cậy tuyệt đối của phần mềm."*

---

### Slide 9: Thử Nghiệm Dataset FIMI & Kịch Bản Demo 3 Phút
- **Thời lượng:** ~30 giây.
- **Lời thoại gợi ý:**
> *"Ngoài dữ liệu mẫu Table 1, phần mềm đã tích hợp sẵn 5 tập dữ liệu FIMI chuẩn quốc tế trong thư mục `dataset/` như `chess`, `mushroom`, `connect`, `retail`, `pumsb`. Động cơ xử lý hàng vạn giao dịch chỉ trong vài giây.  
> Ngay sau đây, em xin phép chuyển sang phần thực hiện Demo trực tiếp trên máy theo đúng kịch bản 4 bước: Giới thiệu DHO-List, Kéo Slider $f$, Bơm luồng One-Scan và chạy Golden Tests trên Tab 4."*

---

### Slide 10 & 11: Kết Luận & Q&A
- **Thời lượng:** ~30 giây.
- **Lời thoại gợi ý:**
> *"Tóm lại, đề tài đã hoàn thành xuất sắc các mục tiêu cả về mặt lý thuyết giải thuật lẫn kỹ thuật lập trình Java nâng cao. Trong tương lai, nhóm dự định nâng cấp thuật toán bằng cấu trúc BitSet và tích hợp luồng dữ liệu thời gian thực thông qua Apache Kafka.  
> Em xin chân thành cảm ơn Quý Thầy/Cô đã chú ý lắng nghe. Em xin kính mời Quý Thầy/Cô đặt câu hỏi phản biện và theo dõi phần Demo trực tiếp phần mềm!"*

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

4. **Bước 4 (60s) – Chuyển sang Tab 4 Demo Động Cơ Big Data:**
   - Chuyển sang **Tab 4**: Bấm nút **"🎯 Golden Tests"** $\rightarrow$ Ma trận hiển thị bảng xanh `✅ PASS 100%` cho cả 8 Test cases.
   - Bấm nút **"🔍 Thống Kê (Inspect)"** $\rightarrow$ Thẻ tổng quan hiện lên kèm Bảng Top 10 mục xuất hiện nhiều nhất.
   - Bấm nút **"🚀 Khai Phá (Mine)"** trên tập dữ liệu FIMI $\rightarrow$ Bảng Mẫu Khai Phá (`tblTsonPatterns`) hiển thị các cột STT, Itemset X, DO(X), Support, TIDs thẳng tắp và đẹp mắt.

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
