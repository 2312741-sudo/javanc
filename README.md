# ⚡ DHOPM Stream Visualizer
> **Đồ Án Môn Học:** Lập Trình Java Nâng Cao  
> **Trường:** Đại Học Đà Lạt (DLU) - Khoa Công Nghệ Thông Tin  
> **Sinh viên thực hiện:** Nguyễn Thanh Tâm – MSSV: 2312741  
> **Thành viên nhóm:** Nguyễn Thanh Sơn ([Tson-dev/JVNC](https://github.com/Tson-dev/JVNC))  
> **Dựa trên bài báo:** *"Damped window based high occupancy pattern mining with one scanning of data streams"*,  
> Engineering Applications of Artificial Intelligence (EAAI), Vol. 174, 2026.  
> DOI: [10.1016/j.engappai.2026.114511](https://doi.org/10.1016/j.engappai.2026.114511)

---

## 📖 1. Giới Thiệu Đề Tài

Thuật toán **DHOPM** *(Damped High Occupancy Pattern Mining)* giải quyết bài toán khai phá các tổ hợp mục chiếm tỷ lệ lớn trong giao dịch (High Occupancy) trên **luồng dữ liệu thời gian thực (Data Streams)**:

- **Độ đo suy giảm theo thời gian (Damped Occupancy - DO):** Nhân thêm hệ số suy giảm $f^{T_L - T_d}$ ($0 < f \leq 1$), giúp dữ liệu cũ dần mất giá trị và ưu tiên các xu hướng mới nhất.
- **Cấu trúc DHO-List một lần quét (One-Scan):** Duy trì và cập nhật dữ liệu gia tăng tức thì trong bộ nhớ RAM mà không cần đọc lại các giao dịch cũ.
- **Cận trên DUBO (Damped Upper Bound Occupancy):** Cắt tỉa không gian tìm kiếm cực kỳ hiệu quả mà vẫn đảm bảo tính đúng đắn 100% (không bỏ sót bất kỳ mẫu DHOP nào).

Ứng dụng **DHOPM Stream Visualizer** là phần mềm desktop viết bằng **JavaFX**, trực quan hóa hoạt động của thuật toán theo dữ liệu chuẩn của bài báo và các bản cập nhật stream sau đó.

### 🔎 Ghi Chú Kiểm Tra Lại Theo Bài Báo

Trong quá trình rà soát lại bước đầu, đã xác nhận một số điểm quan trọng cần lưu ý:
- **TID phải được xử lý theo giá trị số**, không phụ thuộc thứ tự nhập dữ liệu.
- **Các item trùng trong cùng một giao dịch phải được chuẩn hóa** để tránh làm sai số lượng support.
- **Hệ số suy giảm $f$ phải được kiểm tra chặt chẽ** trong miền hợp lệ trước khi tính DO/DUBO.
- **Kết quả minh họa trong README được xác minh bằng unit test** với dữ liệu chuẩn $T_1 \dots T_8$, không chỉ bằng mô tả sơ đồ.

---

## 🛠️ 2. Yêu Cầu Môi Trường & Công Nghệ

| Thành phần | Phiên bản | Ghi chú |
|---|---|---|
| **JDK** | Java 17+ (khuyên dùng 17 hoặc 21 LTS) | Java 25 cũng được hỗ trợ |
| **Apache Maven** | 3.8+ | Build tool chính |
| **JavaFX** | 21.0.2 | Controls, FXML, Graphics |
| **JUnit** | 5.10.2 | JUnit Jupiter unit testing |

---

## 🚀 3. Hướng Dẫn Biên Dịch & Chạy Ứng Dụng

### Cách 1: Sử dụng Terminal (Khuyên dùng)

Mở Terminal và thực hiện:

```bash
# Di chuyển vào thư mục dự án
cd dhopm-visualizer

# 1. Chạy toàn bộ Unit Tests (18 Lab1 + 10 Bridge Tests)
mvn test

# 2. In bảng đối chiếu chi tiết Lab 1 vs Lab 2 ra màn hình Console
mvn compile exec:java -Dexec.mainClass="vn.edu.dlu.dhopm.core.Lab1VerificationRunner"

# 3. Khởi chạy giao diện trực quan hóa JavaFX Desktop
mvn javafx:run
```

#### 💡 Khởi chạy nhanh bằng script `run.sh` (macOS/Linux):
```bash
chmod +x run.sh

./run.sh         # Mở giao diện đồ họa JavaFX
./run.sh test    # Chạy toàn bộ Unit Tests
./run.sh verify  # In bảng đối chiếu kết quả thực nghiệm
```

### ✅ Kết Quả Kiểm Thử Xác Nhận

| Lệnh | Kết quả |
|---|---|
| `mvn test` | `28/28 tests PASS, BUILD SUCCESS` |
| `Lab1VerificationRunner` | `BUILD SUCCESS` (Sai số max $\Delta < 0.0001$) |
| `mvn javafx:run` | `BUILD SUCCESS` |

### Cách 2: Mở bằng IDE (IntelliJ IDEA / Eclipse / VS Code)
1. Mở IDE, chọn **Open Project** và trỏ đến thư mục `dhopm-visualizer`.
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
   - Tô màu phân loại: 🟢 Xanh lá (đạt DHOP) · 🔴 Đỏ nhạt (bị cắt tỉa) · ⚪ Trắng (node trung gian).
4. **Tab 2: Bảng Khai Phá Chi Tiết (DFS Tree):**
   - Bảng hiển thị toàn bộ 32 mẫu ứng viên được duyệt trong cây DFS.
   - Cung cấp $DO(X)$, $DUBO(X)$, trạng thái cắt tỉa, và danh sách các giao dịch chứa mẫu.
5. **Tab 3: Biểu Đồ So Sánh Điểm Số DO (Live LineChart):**
   - Biểu đồ đường trực quan thể hiện giá trị $DO$ của các Item so với đường ngưỡng $minSup$.

---

## 🏛️ 5. Kiến Trúc Mã Nguồn Chuẩn Java Nâng Cao

```
vn.edu.dlu.dhopm/
├── Main.java                       # Entry point khởi động ứng dụng
├── bridge/                         # ★ TẦNG CẦU NỐI (Bridge Layer – Xem Mục 7)
│   ├── BridgeEngine.java           # Interface hợp đồng chung (DIP)
│   ├── EngineMode.java             # Enum chiến lược engine (Strategy Pattern)
│   ├── EngineFactory.java          # Factory tạo engine đúng loại (Factory Method)
│   ├── TamSimulationBridge.java    # Adapter bọc DHOPMEngine của Tâm
│   └── TsonV1Bridge.java           # Adapter bọc MiningEngine của Tson
├── model/                          # Tầng Model dữ liệu (Records & POJO)
│   ├── Entry.java                  # Record <TID, TLen>
│   ├── Transaction.java            # Giao dịch luồng
│   ├── DHONode.java                # Node trong DHO-List
│   ├── DHOList.java                # Cấu trúc Global DHO-List
│   └── PatternResult.java          # Kết quả khai phá từng mẫu
├── core/                           # Thuật toán cốt lõi & Đa luồng
│   ├── DHOPMEngine.java            # 3 pha: Construct/Update, Reconstruct, Mine DFS
│   ├── DUBOCalculator.java         # Tính toán cận trên DUBO theo Định nghĩa 6
│   ├── StreamSimulator.java        # Đa luồng giả lập luồng dữ liệu stream
│   └── DatasetLoader.java          # Nạp dữ liệu bài báo & sinh dữ liệu
├── event/                          # Observer Pattern
│   ├── StreamListener.java         # Bắn sự kiện khi có batch mới
│   └── MiningListener.java         # Bắn sự kiện khi khai phá xong
└── ui/                             # Giao diện JavaFX (MVC Pattern)
    ├── DHOPMApplication.java       # Khởi tạo Scene & Window
    ├── controller/
    │   └── MainController.java     # Xử lý sự kiện UI, Slider, Tables
    └── component/
        └── DHONodeCard.java        # Custom UI Card hiển thị Node DHO-List
```

### Các Kỹ Thuật Java Nâng Cao Được Áp Dụng:
- **Multithreading & Concurrency:** `StreamSimulator` chạy trên background thread, đồng bộ với JavaFX Application Thread thông qua `Platform.runLater()`.
- **Design Patterns:**
  - `Bridge Pattern` – Tách rời giao diện người dùng khỏi thuật toán thông qua `BridgeEngine`.
  - `Adapter Pattern` – `TamSimulationBridge` và `TsonV1Bridge` bọc 2 engine khác nhau về cùng một interface.
  - `Strategy Pattern` – `EngineMode` enum cho phép chọn thuật toán tại runtime không sửa code UI.
  - `Factory Method` – `EngineFactory` tạo engine đúng loại, Open/Closed Principle.
  - `Observer Pattern` – `StreamListener`, `MiningListener`, `PhaseListener`, `MiningProgressListener`.
  - `MVC (Model-View-Controller)` – Phân tách rõ ràng giữa thuật toán thuần và hiển thị FXML.
- **Java Records & Modern Java:** Sử dụng `record Entry`, `record PatternResult`, Stream API, Lambda Expressions, Switch Expressions.
- **Unit Testing (JUnit 5):** 28 test cases kiểm tra Lab 1 numbers, Bridge contract, streaming, edge cases.

---

## 🌉 7. Kiến Trúc Cầu Nối (Bridge Architecture) – Tâm & Tson Phát Triển Độc Lập

> Đây là phần **kỹ thuật quan trọng nhất** của đồ án – áp dụng nguyên lý **DIP (Dependency Inversion Principle)** và **3 Design Patterns** chuẩn công nghiệp để 2 thành viên không bao giờ bị xung đột mã nguồn (merge conflict).

### 📊 Sơ Đồ Tổng Quan

```
┌────────────────────────────────────────────────────────────────────────┐
│                  TÂM: dhopm-ui / dhopm-visualizer                      │
│   (JavaFX Visualizer · DHO Cards · Step-by-Step · Biểu đồ DO · Table) │
│   MainController chỉ phụ thuộc vào → BridgeEngine (interface)         │
└─────────────────────────────┬──────────────────────────────────────────┘
                              │  gọi qua BridgeEngine (interface)
              ┌───────────────┼───────────────────┐
              ▼                                   ▼
┌─────────────────────┐               ┌───────────────────────┐
│  TamSimulationBridge│               │    TsonV1Bridge        │
│  (Adapter Pattern)  │               │    (Adapter Pattern)   │
│  bọc DHOPMEngine    │               │    bọc MiningEngine    │
│  (nội bộ Tâm)       │               │    (của Tson)          │
└─────────────────────┘               └───────────────────────┘
              │                                   │
              ▼                                   ▼
┌──────────────────────────────────────────────────────────────────────┐
│          TẦNG HỢP ĐỒNG CHUNG: dhopm-common (của Tson)               │
│  Engine · PhaseAwareEngine · PhaseListener · MiningProgressListener   │
│  MineResult · Pattern · Transaction · MiningConfig                   │
└──────────────────────────────────────────────────────────────────────┘
                              │  thực thi bởi
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌─────────────────────┐          ┌────────────────────────┐
│  dhopm-v1-standard  │          │  dhopm-v2-optimized     │
│  (Tson làm – V1)    │          │  (Tson làm – V2 sau này)│
│  MiningEngine.java  │          │  (BitSet, parallel DFS) │
└─────────────────────┘          └────────────────────────┘
```

---

### 🧱 TẦNG 1: Hợp Đồng Chung (dhopm-common – nguồn sự thật duy nhất)

Module `dhopm-common` của Tson đóng vai trò **"Bản Hiến Pháp"** chung của cả nhóm.  
Sau khi hai bên thống nhất, module này **rất hiếm khi sửa** – chỉ sửa khi cả 2 người cùng đồng ý.

| Interface / Class | Vai trò |
|---|---|
| `Engine` | Contract cơ bản: `name()`, `loadBatch()`, `mineNow()` |
| `PhaseAwareEngine` | Mở rộng Engine với `setPhaseListener()` – theo dõi 3 pha |
| `ProgressAwareEngine` | Mở rộng Engine với `setMiningProgressListener()` |
| `PhaseListener` | Callback `onPhase(phase, startNs, endNs)` → vẽ biểu đồ thời gian |
| `MiningProgressListener` | Callback `onProgress(fraction)` → cập nhật ProgressBar UI |
| `MineResult` | Record kết quả: `patterns`, `totalTransactions`, `lastTid`, `minSup` |
| `Pattern` | Record mẫu: `items[]`, `dampedOccupancy`, `tids[]` |
| `Transaction` | Record giao dịch: `tid`, `items[]` |
| `MiningConfig` | Config: `partial` (∂), `decayFactor` (f), `epsilon`, `workers` |

---

### 🔌 TẦNG 2: Cầu Nối Mã Nguồn (Bridge / Adapter Layer)

#### File [`BridgeEngine.java`](src/main/java/vn/edu/dlu/dhopm/bridge/BridgeEngine.java)
Interface hợp đồng phía UI – Tâm chỉ phụ thuộc vào đây, không bao giờ import class của Tson trực tiếp:

```java
public interface BridgeEngine {
    String engineName();
    void feedTransactions(List<Transaction> batch);      // Nạp batch dữ liệu (One-Scan)
    List<PatternResult> executeAndGetResults();           // Khai phá & trả kết quả UI
    void onPhaseUpdate(BiConsumer<String, Double> cb);   // Observer: tiến độ từng pha
    void onMiningProgress(Consumer<Double> cb);          // Observer: phần trăm hoàn thành
    void reset();                                        // Đặt lại trạng thái
    long transactionCount();                             // Số giao dịch đã nạp
    int lastTid();                                       // TL hiện tại
}
```

#### File [`TsonV1Bridge.java`](src/main/java/vn/edu/dlu/dhopm/bridge/TsonV1Bridge.java)
Adapter bọc `MiningEngine` của Tson. Tích hợp theo 3 bước:

**Bước 1 – Clone và install jar của Tson vào Maven local repo:**
```bash
# Clone repo Tson (nếu chưa có)
git clone https://github.com/Tson-dev/JVNC.git /tmp/tson_jvnc

# Install vào Maven local repository
cd /tmp/tson_jvnc/implementation
mvn install -DskipTests
```

**Bước 2 – Thêm dependency vào `pom.xml` của repo Tâm:**
```xml
<dependency>
    <groupId>dhopm</groupId>
    <artifactId>dhopm-common</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>dhopm</groupId>
    <artifactId>dhopm-v1-standard</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Bước 3 – Bỏ comment các import trong `TsonV1Bridge.java`** (đã được comment sẵn với hướng dẫn chi tiết từng dòng).

---

### 🌿 TẦNG 3: Cầu Nối Git – Quy Trình Không Bao Giờ Bị Conflict

#### Cấu Trúc Monorepo Thống Nhất (Đề Xuất)

```
JVNC/ (Root Monorepo – Repo chung trên GitHub)
├── dhopm-common/           ← Tầng hợp đồng (cả 2 đọc, hiếm khi sửa)
├── dhopm-v1-standard/      ← TSON LÀM 100% ở đây
├── dhopm-v2-optimized/     ← TSON LÀM sau (BitSet DFS)
├── dhopm-bench/            ← TSON LÀM (Benchmark FIMI)
├── dhopm-ui/               ← TÂM LÀM 100% ở đây (JavaFX + Bridge Layer)
└── dataset/                ← Tập dữ liệu chung (chess.dat, retail.dat, ...)
```

#### Quy Tắc Branch Không Bao Giờ Xung Đột

```bash
# Tâm làm việc trên nhánh riêng
git checkout -b feat/ui-visualizer
# Sửa: chỉ dhopm-ui/** và bridge/**
git push origin feat/ui-visualizer

# Tson làm việc trên nhánh riêng
git checkout -b feat/engine-v1
# Sửa: chỉ dhopm-v1-standard/** và dhopm-bench/**
git push origin feat/engine-v1

# Merge vào main: Git tự auto-merge (không bao giờ conflict vì khác thư mục)
```

#### Khi Tson Cải Tiến Engine – Tâm Cập Nhật Trong 3 Lệnh:
```bash
cd /tmp/tson_jvnc/implementation
git pull                    # Lấy code mới nhất của Tson
mvn install -DskipTests     # Re-install jar vào Maven local repo
cd dhopm-visualizer
mvn clean package           # Build lại UI với engine mới nhất
```

---

### 🔁 Luồng Dữ Liệu Từ Đầu Đến Cuối

```
[JavaFX UI: MainController]
    │ ComboBox chọn EngineMode
    ▼
[EngineFactory.create(mode, ∂, f)]
    │ Strategy Pattern
    ▼
[BridgeEngine interface]
    │ Adapter Pattern
    ├── TamSimulationBridge → DHOPMEngine (step-by-step animation)
    └── TsonV1Bridge        → MiningEngine (high performance / FIMI)
              │ Observer Pattern (PhaseListener, ProgressListener)
              ▼
         [MineResult: patterns, totalTx, lastTid, minSup]
              │ Conversion: Pattern → PatternResult
              ▼
    [TableView<PatternResult>] + [LineChart DO] + [FlowPane DHOCards]
```

---

## 🎤 8. Kịch Bản Thuyết Trình 3 Phút Ghi Điểm Tuyệt Đối

Khi báo cáo trước Thầy/Cô, bạn hãy thực hiện theo đúng 4 bước sau:

1. **Bước 1 – Giới thiệu bài toán (30 giây)**  
   *"Em xin phép demo ứng dụng DHOPM Stream Visualizer. Thuật toán này tìm các tổ hợp mặt hàng chiếm tỷ lệ lớn trong giỏ hàng và ưu tiên xu hướng gần đây trên luồng dữ liệu."*

2. **Bước 2 – Chứng minh hiệu quả của hệ số suy giảm $f$ (45 giây)**  
   - Kéo Slider $f$ lên **$1.0$**: Chỉ cho thầy cô thấy bảng kết quả có tới **9 mẫu** (dữ liệu cũ $T_1 \dots T_4$ vẫn được tính nguyên giá trị).
   - Kéo Slider $f$ về **$0.9$**: Ngay lập tức kết quả rút gọn lại chỉ còn đúng **2 mẫu hot nhất gần đây là $AE$ và $F$** ($DO(AE)=1.2601, DO(F)=1.2553$).
   - Nhấn mạnh: *"Đây chính là đóng góp lớn nhất của bài báo: loại bỏ các mẫu đã lỗi thời trong quá khứ."*

3. **Bước 3 – Demo tính năng One-Scan Stream (45 giây)**  
   - Bấm nút **"➕ Bơm từng giao dịch (Step Next)"**: Bơm giao dịch mới $T_9 = \{A, E\}$.
   - Chỉ vào thống kê: *"Hệ thống không cần đọc lại $T_1 \dots T_8$, chỉ quét $T_9$, cập nhật $T_L=9$ và vẽ lại biểu đồ tức thì. Đây là cơ chế One-Scan tiết kiệm bộ nhớ."*

4. **Bước 4 – Demo Kiến Trúc Bridge & Design Patterns (30 giây)**  
   - *"Em đã thiết kế theo Bridge Pattern và Adapter Pattern. UI này có thể chạy với 2 engine khác nhau: engine animation của em để demo step-by-step, và engine tốc độ cao của bạn Tson để xử lý dataset 1 triệu giao dịch chess, retail, kosarak – mà không cần sửa một dòng code giao diện nào."*

---

## 📋 9. Bảng Đối Chiếu Kết Quả Lab 1 & Lab 2

| Mẫu | $DO$ tính tay (Lab 1) | $DO$ phần mềm (Lab 2) | Sai số $\Delta$ | Đánh giá |
|---|---|---|---|---|
| $F$ | $1.2553$ | $1.2553$ | $< 0.0001$ | ✅ Chính xác |
| $AE$ | $1.2601$ | $1.2601$ | $< 0.0001$ | ✅ Chính xác |
| $BE$ | $0.9477$ | $0.9477$ | $< 0.0001$ | ✅ Chính xác |
| $ABE$ | $0.6859$ | $0.6859$ | $< 0.0001$ | ✅ Chính xác |
| $CE$ | $0.8573$ | $0.8573$ | $< 0.0001$ | ✅ Chính xác |
| $ACE$ | $0.6190$ | $0.6190$ | $< 0.0001$ | ✅ Chính xác |
| $ADE$ | $0.5740$ | $0.5740$ | $< 0.0001$ | ✅ Chính xác |
| $AEF$ | $0.5740$ | $0.5740$ | $< 0.0001$ | ✅ Chính xác |
| $AEG$ | $0.5740$ | $0.5740$ | $< 0.0001$ | ✅ Chính xác |

Tất cả 18 unit tests của Lab 1 đều PASS với sai số $\Delta < 10^{-4}$.

---

## 📂 10. Cấu Trúc Thư Mục Đầy Đủ

```
dhopm-visualizer/
├── src/
│   ├── main/
│   │   ├── java/vn/edu/dlu/dhopm/
│   │   │   ├── Main.java
│   │   │   ├── bridge/                  ★ Bridge Layer
│   │   │   │   ├── BridgeEngine.java
│   │   │   │   ├── EngineMode.java
│   │   │   │   ├── EngineFactory.java
│   │   │   │   ├── TamSimulationBridge.java
│   │   │   │   └── TsonV1Bridge.java
│   │   │   ├── core/
│   │   │   │   ├── DHOPMEngine.java
│   │   │   │   ├── DUBOCalculator.java
│   │   │   │   ├── StreamSimulator.java
│   │   │   │   ├── DatasetLoader.java
│   │   │   │   └── Lab1VerificationRunner.java
│   │   │   ├── model/
│   │   │   │   ├── Entry.java
│   │   │   │   ├── Transaction.java
│   │   │   │   ├── DHONode.java
│   │   │   │   ├── DHOList.java
│   │   │   │   └── PatternResult.java
│   │   │   ├── event/
│   │   │   │   ├── StreamListener.java
│   │   │   │   └── MiningListener.java
│   │   │   └── ui/
│   │   │       ├── DHOPMApplication.java
│   │   │       ├── controller/MainController.java
│   │   │       └── component/DHONodeCard.java
│   │   └── resources/vn/edu/dlu/dhopm/ui/
│   │       └── main-view.fxml
│   └── test/
│       └── java/vn/edu/dlu/dhopm/
│           ├── bridge/
│           │   └── BridgeEngineTest.java  ★ 10 tests Bridge Layer
│           └── core/
│               └── Lab1VerificationTest.java  ★ 18 tests Lab 1
├── docs/
│   ├── 2312741_NguyenThanhTam_Lab2.docx
│   └── DHOPM_Lab2_SoSanh_KetQua.xlsx
├── pom.xml
├── run.sh
└── README.md
```

---

## 🔗 11. Liên Kết Tham Khảo

- **Repo của Tson (dhopm-common + dhopm-v1-standard):** [https://github.com/Tson-dev/JVNC](https://github.com/Tson-dev/JVNC)
- **Repo của Tâm (dhopm-ui / Visualizer):** [https://github.com/2312441-sudo/javanc](https://github.com/2312441-sudo/javanc)
- **Bài báo gốc:** DOI [10.1016/j.engappai.2026.114511](https://doi.org/10.1016/j.engappai.2026.114511)
- **JavaFX 21 Documentation:** [openjfx.io](https://openjfx.io)
- **JUnit 5 Documentation:** [junit.org/junit5](https://junit.org/junit5)
