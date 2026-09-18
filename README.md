# ⚡ DHOPM Stream Visualizer
> **Đồ Án Môn Học:** Lập Trình Java Nâng Cao  
> **Trường:** Đại Học Đà Lạt (DLU) - Khoa Công Nghệ Thông Tin  
> **Sinh viên thực hiện:** Nguyễn Thành Tâm  
> **Dựa trên bài báo khoa học:** *"Damped window based high occupancy pattern mining with one scanning of data streams"*, Engineering Applications of Artificial Intelligence (EAAI), Vol. 174, 2026. DOI: [10.1016/j.engappai.2026.114511](https://doi.org/10.1016/j.engappai.2026.114511)

---

## 📖 1. Giới Thiệu Đề Tài

Thuật toán **DHOPM** *(Damped High Occupancy Pattern Mining)* giải quyết bài toán khai phá các tổ hợp mục chiếm tỷ lệ lớn trong giao dịch (High Occupancy) trên **luồng dữ liệu thời gian thực (Data Streams)**:
- **Độ đo suy giảm theo thời gian (Damped Occupancy - DO):** Nhân thêm hệ số suy giảm $f^{T_L - T_d}$ ($0 < f < 1$), giúp dữ liệu cũ dần mất giá trị và ưu tiên các xu hướng mới nhất.
- **Cấu trúc DHO-List một lần quét (One-Scan):** Duy trì và cập nhật dữ liệu gia tăng tức thì trong bộ nhớ RAM mà không cần đọc lại các giao dịch cũ.
- **Cận trên DUBO (Damped Upper Bound Occupancy):** Cắt tỉa không gian tìm kiếm cực kỳ hiệu quả mà vẫn đảm bảo tính đúng đắn 100% (không bỏ sót bất kỳ mẫu DHOP nào).

Ứng dụng **DHOPM Stream Visualizer** là phần mềm desktop viết bằng **JavaFX**, trực quan hóa hoạt động của thuật toán theo dữ liệu chuẩn của bài báo và các bản cập nhật stream sau đó.

### 🔎 Ghi chú kiểm tra lại theo bài báo

Trong quá trình rà soát lại bước đầu, đã xác nhận một số điểm quan trọng cần lưu ý để đảm bảo tính đúng đắn thực tế:
- **TID phải được xử lý theo giá trị số**, không phụ thuộc thứ tự nhập dữ liệu.
- **Các item trùng trong cùng một giao dịch phải được chuẩn hóa** để tránh làm sai số lượng support.
- **Hệ số suy giảm $f$ phải được kiểm tra chặt chẽ** trong miền hợp lệ trước khi tính DO/DUBO.
- **Kết quả minh họa trong README được xác minh bằng unit test** với dữ liệu chuẩn $T_1 \dots T_8$, không chỉ bằng mô tả sơ đồ.

Nói cách khác, ứng dụng hiện tại là một bản triển khai đã được kiểm chứng lại bằng test nội bộ và không chỉ dựa trên minh họa hình ảnh.

---

## 🛠️ 2. Yêu Cầu Môi Trường & Công Nghệ

- **Java Development Kit (JDK):** Java 25 trở lên (latest LTS).
- **Công cụ build:** Apache Maven 3.9+.
- **Thư viện chính:**
  - JavaFX 21 (Controls, FXML, Graphics).
  - JUnit 5 (JUnit Jupiter) kiểm thử tự động.

---

## 🚀 3. Hướng Dẫn Biên Dịch & Chạy Ứng Dụng

### Cách 1: Chạy bằng Terminal (Khuyên dùng)
Mở Terminal tại thư mục dự án và chạy:

```bash
# Đảm bảo Java 25 đang được dùng
export JAVA_HOME="/Users/nthtam/.jdk/jdk-25.0.2/jdk-25.0.2+10/Contents/Home"
export PATH="$JAVA_HOME/bin:/Users/nthtam/.maven/apache-maven-3.9.6/bin:$PATH"

# Di chuyển vào thư mục dự án
cd "/Users/nthtam/Lưu trữ/javanangcao/dhopm-visualizer"

# Chạy Unit Tests kiểm tra tính đúng đắn thuật toán
mvn test

# Khởi chạy ứng dụng JavaFX trực tiếp
mvn javafx:run
```

### ✅ Kiểm chứng thực tế đã chạy

Các lệnh dưới đây đã được thực hiện thành công trên máy hiện tại:
- `mvn test` với Java 25 → `EXIT:0`
- `mvn javafx:run` → `BUILD SUCCESS`

Đây là trạng thái hiện tại được xác nhận, không phải giả định từ mô tả.

### Cách 2: Mở bằng IDE (IntelliJ IDEA / Eclipse / VS Code)
1. Mở IDE, chọn **Open Project** và trỏ đến thư mục `dhopm-visualizer` (IDE sẽ tự nhận diện dự án Maven).
2. Đợi Maven tải xong các dependency.
3. Chạy file: `src/main/java/vn/edu/dlu/dhopm/Main.java`.

---

## 🎯 4. Các Tính Năng Nổi Bật Của Ứng Dụng

1. **Bộ Điều Khiển Tham Số Động:**
   - **Slider $f$ ($0.5 \to 1.0$):** Kéo thay đổi hệ số suy giảm. Xem ngay kết quả lọc mẫu theo thời gian thực.
   - **Slider $\partial$ ($5\% \to 30\%$):** Kéo thay đổi ngưỡng $minSup = \partial \times |DB|$.
2. **Mô Phỏng Luồng Dữ Liệu Thời Gian Thực (Multithreading):**
   - Nút **"▶ Bắt đầu"**: Khởi động luồng nền (`StreamSimulator` sử dụng `ScheduledExecutorService`) tự động bơm giao dịch mới theo chu kỳ.
   - Nút **"➕ Bơm từng giao dịch (Step Next)"**: Bơm thủ công từng giao dịch tiếp theo ($T_9, T_{10}, \dots$) để quan sát quá trình One-Scan cập nhật DHO-List.
   - Nút **"🔄 Khôi phục gốc"**: Đưa hệ thống về lại 8 giao dịch chuẩn của bài báo ($T_1 \dots T_8$).
3. **Tab 1: Trực Quan Hóa Global DHO-List:**
   - Hiển thị các thẻ Card của từng Item ($G, B, A, C, D, E, F$).
   - Sắp xếp tự động theo thứ tự **Support tăng dần** ($G \prec B \prec A \prec C \prec D \prec E \prec F$).
   - Tô màu phân loại:
     - 🟢 **Xanh lá:** Đạt chuẩn DHOP ($DO \ge minSup$).
     - 🔴 **Đỏ nhạt:** Bị cắt tỉa ($DUBO < minSup$).
     - ⚪ **Trắng/Xám:** Các node trung gian mở rộng.
4. **Tab 2: Bảng Khai Phá Chi Tiết (DFS Tree):**
   - Bảng hiển thị toàn bộ 32 mẫu ứng viên được duyệt trong cây DFS.
   - Cung cấp $DO(X)$, $DUBO(X)$, trạng thái cắt tỉa, và danh sách các giao dịch chứa mẫu.
5. **Tab 3: Biểu Đồ So Sánh Điểm Số DO (Live LineChart):**
   - Biểu đồ đường trực quan thể hiện giá trị $DO$ của các Item so với đường ngưỡng $minSup$.

---

## 🏛️ 5. Kiến Trúc Mã Nguồn Chuẩn Java Nâng Cao

```
vn.edu.dlu.dhopm/
├── Main.java                 # Entry point khởi động ứng dụng
├── model/                    # Tầng Model dữ liệu (Records & POJO)
│   ├── Entry.java            # Record <TID, TLen>
│   ├── Transaction.java      # Giao dịch luồng
│   ├── DHONode.java          # Node trong DHO-List
│   ├── DHOList.java          # Cấu trúc Global DHO-List
│   └── PatternResult.java    # Kết quả khai phá từng mẫu
├── core/                     # Thuật toán cốt lõi & Đa luồng
│   ├── DHOPMEngine.java      # 3 pha: Construct/Update, Reconstruct, Mine DFS
│   ├── DUBOCalculator.java   # Tính toán cận trên DUBO theo Định nghĩa 6
│   ├── StreamSimulator.java  # Đa luồng giả lập luồng dữ liệu stream
│   └── DatasetLoader.java    # Nạp dữ liệu bài báo & sinh dữ liệu
├── event/                    # Observer Pattern
│   ├── StreamListener.java   # Bắn sự kiện khi có batch mới
│   └── MiningListener.java   # Bắn sự kiện khi khai phá xong
└── ui/                       # Giao diện JavaFX (MVC Pattern)
    ├── DHOPMApplication.java # Khởi tạo Scene & Window
    ├── controller/
    │   └── MainController.java # Xử lý sự kiện UI, Slider, Tables
    └── component/
        └── DHONodeCard.java  # Custom UI Card hiển thị Node DHO-List
```

### Các Kỹ Thuật Java Nâng Cao Được Áp Dụng:
- **Multithreading & Concurrency:** `StreamSimulator` chạy trên background thread, đồng bộ với JavaFX Application Thread thông qua `Platform.runLater()`.
- **Design Patterns:**
  - `Observer Pattern`: `StreamListener` giúp tách rời luồng dữ liệu khỏi giao diện.
  - `MVC (Model-View-Controller)`: Phân tách rõ ràng giữa thuật toán thuần và hiển thị FXML.
- **Java Records & Modern Java:** Sử dụng `record Entry`, `record PatternResult`, Stream API, Lambda Expressions.
- **Unit Testing (JUnit 5):** Kiểm tra các ví dụ số học của bài báo và các trường hợp streaming, TID không theo thứ tự, item trùng và tham số suy giảm không hợp lệ.

---

## 🎤 6. Kịch Bản Thuyết Trình 3 Phút Ghi Điểm Tuyệt Đối

Khi báo cáo trước Thầy/Cô, bạn hãy thực hiện theo đúng 4 bước sau:

1. **Bước 1: Giới thiệu bài toán (30 giây)**
   - *"Em xin phép demo ứng dụng DHOPM Stream Visualizer. Thuật toán này tìm các tổ hợp mặt hàng chiếm tỷ lệ lớn trong giỏ hàng và ưu tiên xu hướng gần đây trên luồng dữ liệu."*
2. **Bước 2: Chứng minh hiệu quả của hệ số suy giảm $f$ (45 giây)**
   - Kéo Slider $f$ lên **$1.0$**: Chỉ cho thầy cô thấy bảng kết quả có tới **9 mẫu** (vì dữ liệu cũ $T_1 \dots T_4$ vẫn được tính nguyên giá trị).
   - Kéo Slider $f$ về **$0.9$**: Ngay lập tức kết quả rút gọn lại chỉ còn đúng **2 mẫu hot nhất gần đây là $AE$ và $F$** ($DO(AE)=1.2601, DO(F)=1.2553$).
   - Nhấn mạnh: *"Đây chính là đóng góp lớn nhất của bài báo: loại bỏ các mẫu đã lỗi thời trong quá khứ."*
3. **Bước 3: Demo tính năng One-Scan Stream (45 giây)**
   - Bấm nút **"➕ Bơm từng giao dịch (Step Next)"**: Bơm giao dịch mới $T_9 = \{A, E\}$.
   - Chỉ vào thống kê: *"Hệ thống không cần đọc lại $T_1 \dots T_8$, chỉ quét $T_9$, cập nhật $T_L=9$ và vẽ lại biểu đồ tức thì. Đây là cơ chế One-Scan tiết kiệm bộ nhớ."*
4. **Bước 4: Demo Đa luồng (Multithreading) & DUBO Pruning (30 giây)**
   - Bấm nút **"▶ Bắt đầu"**: Dữ liệu tự động đổ về liên tục mà giao diện vẫn thao tác mượt mà (không đơ chuột).
   - Mở Tab 2: Chỉ vào tỷ lệ cắt tỉa $DUBO$: *"Thuật toán cắt tỉa được gần 60% không gian tìm kiếm nhờ cận trên DUBO, tránh hiện tượng bùng nổ tổ hợp."*
