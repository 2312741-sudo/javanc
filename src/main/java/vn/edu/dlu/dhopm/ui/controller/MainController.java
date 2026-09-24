package vn.edu.dlu.dhopm.ui.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import vn.edu.dlu.dhopm.bridge.BridgeEngine;
import vn.edu.dlu.dhopm.bridge.EngineFactory;
import vn.edu.dlu.dhopm.bridge.EngineMode;
import vn.edu.dlu.dhopm.bridge.TsonToolsService;
import vn.edu.dlu.dhopm.bridge.TsonToolsService.DatasetItem;
import vn.edu.dlu.dhopm.bridge.TsonToolsService.MineExecutionResult;
import vn.edu.dlu.dhopm.bridge.TsonToolsService.InspectReport;
import vn.edu.dlu.dhopm.core.DatasetLoader;
import vn.edu.dlu.dhopm.core.DHOPMEngine;
import vn.edu.dlu.dhopm.core.StreamSimulator;
import vn.edu.dlu.dhopm.model.DHONode;
import vn.edu.dlu.dhopm.model.PatternResult;
import vn.edu.dlu.dhopm.model.Transaction;
import vn.edu.dlu.dhopm.ui.component.DHONodeCard;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller điều khiển toàn bộ giao diện JavaFX cho DHOPM Visualizer.
 * Version 3.0: Tích hợp đầy đủ bộ công cụ Tson V1 từ README:
 * <ul>
 *   <li>Dropdown chọn dataset FIMI (chess, retail, mushroom, connect, kosarak...)</li>
 *   <li>Nút "🚀 Khai Phá (Mine)" thay thế lệnh CLI mine</li>
 *   <li>Nút "📊 Thống Kê (Inspect)" thay thế lệnh CLI inspect</li>
 *   <li>Nút "🏆 Golden TC1-TC8" thay thế lệnh CLI golden</li>
 *   <li>Nút "🔬 Top DO (Detail)" thay thế lệnh CLI detail</li>
 *   <li>Tab Console &amp; Benchmark với KPI cards thời gian, heap, throughput</li>
 * </ul>
 *
 * @author Nguyễn Thanh Tâm (2312741) &amp; Nguyễn Thanh Sơn
 */
public class MainController implements Initializable {

    // ── Engine Selector ──────────────────────────────────────────────────────
    @FXML private ComboBox<EngineMode> cbEngineMode;
    @FXML private Label lblEngineName;
    @FXML private Label lblLastPhase;
    @FXML private ProgressBar progressMining;

    // ── Tson FIMI Tools & Datasets Panel ──────────────────────────────────────
    @FXML private ComboBox<DatasetItem> cbTsonDataset;
    @FXML private ComboBox<String> cbTsonLimit;
    @FXML private Button btnBrowseDataset;
    @FXML private Button btnTsonMine;
    @FXML private Button btnTsonInspect;
    @FXML private Button btnTsonDetail;
    @FXML private Button btnTsonGolden;

    // ── Controls & Sliders ───────────────────────────────────────────────────
    @FXML private Slider sliderF;
    @FXML private Slider sliderMinSup;
    @FXML private Label lblFValue;
    @FXML private Label lblMinSupValue;
    @FXML private Label lblAbsoluteMinSup;

    // ── Stream & Engine Status Labels ────────────────────────────────────────
    @FXML private Label lblTL;
    @FXML private Label lblTransCount;
    @FXML private Label lblDhopCount;
    @FXML private Label lblVisitedCount;
    @FXML private Label lblPrunedCount;
    @FXML private Label lblStreamStatus;
    @FXML private Label lblSortOrder;

    // ── Action Buttons ───────────────────────────────────────────────────────
    @FXML private Button btnStartStream;
    @FXML private Button btnPauseStream;
    @FXML private Button btnStepStream;
    @FXML private Button btnReset;

    // ── Center Views & TabPane ───────────────────────────────────────────────
    @FXML private TabPane mainTabPane;
    @FXML private FlowPane dhoNodesPane;
    @FXML private TableView<PatternResult> tableResults;
    @FXML private TableColumn<PatternResult, String> colPattern;
    @FXML private TableColumn<PatternResult, Number> colSupport;
    @FXML private TableColumn<PatternResult, String> colDO;
    @FXML private TableColumn<PatternResult, String> colDUBO;
    @FXML private TableColumn<PatternResult, String> colStatus;
    @FXML private TableColumn<PatternResult, String> colTransactions;

    @FXML private LineChart<String, Number> lineChartDO;

    // ── Tab 4: Tson Console & KPI Cards ──────────────────────────────────────
    @FXML private Label lblKpiTime;
    @FXML private Label lblKpiPhases;
    @FXML private Label lblKpiHeap;
    @FXML private Label lblKpiPatterns;
    @FXML private Label lblKpiMinSup;
    @FXML private Label lblKpiThroughput;
    @FXML private Label lblKpiTrans;
    @FXML private TextArea txtTsonConsole;
    @FXML private Button btnCopyConsole;
    @FXML private Button btnClearConsole;

    // ── Backend Services ─────────────────────────────────────────────────────
    private DHOPMEngine tamEngine;
    private StreamSimulator streamSimulator;
    private BridgeEngine bridgeEngine;
    private final TsonToolsService tsonService = new TsonToolsService();

    private final ObservableList<PatternResult> tableData = FXCollections.observableArrayList();
    private EngineMode currentMode = EngineMode.TAM_SIMULATION;

    private Path selectedCustomDatasetPath = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initEngineSelectorPanel();
        initTsonToolsPanel();
        initTamEngine();
        initTableView();
        initEventHandlers();
        initConsolePanel();
        loadInitialPaperData();
    }

    // ── Panel 0: Engine Selector ─────────────────────────────────────────────

    private void initEngineSelectorPanel() {
        cbEngineMode.setItems(FXCollections.observableArrayList(EngineMode.values()));
        cbEngineMode.setValue(EngineMode.TAM_SIMULATION);
        cbEngineMode.setOnAction(e -> switchEngine(cbEngineMode.getValue()));

        cbEngineMode.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(EngineMode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
        cbEngineMode.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(EngineMode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
    }

    // ── Panel 0.5: Tson Datasets & Commands ───────────────────────────────────

    private void initTsonToolsPanel() {
        // 1. Nạp danh sách datasets
        List<DatasetItem> presets = TsonToolsService.getPresetDatasets();
        cbTsonDataset.setItems(FXCollections.observableArrayList(presets));
        cbTsonDataset.setValue(presets.get(0)); // Mặc định default.dat

        // Khi chọn dataset, tự động gợi ý tham số partial (∂) chuẩn
        cbTsonDataset.setOnAction(e -> {
            DatasetItem item = cbTsonDataset.getValue();
            if (item != null) {
                sliderMinSup.setValue(item.defaultPartial());
                appendConsoleLog(String.format("[DATASET] Đã chọn: %s | Gợi ý ∂ = %.2f%%%n",
                        item.displayName(), item.defaultPartial() * 100));
            }
        });

        // 2. ComboBox Limit
        cbTsonLimit.setItems(FXCollections.observableArrayList(
                "Toàn bộ (Đầy đủ)",
                "1,000 tx",
                "5,000 tx",
                "10,000 tx",
                "50,000 tx"
        ));
        cbTsonLimit.setValue("Toàn bộ (Đầy đủ)");

        // 3. Nút Browse file ngoài
        btnBrowseDataset.setOnAction(e -> handleBrowseDataset());

        // 4. Các nút tương ứng bộ lệnh CLI của Tson
        btnTsonMine.setOnAction(e -> executeTsonMine());
        btnTsonInspect.setOnAction(e -> executeTsonInspect());
        btnTsonDetail.setOnAction(e -> executeTsonDetail());
        btnTsonGolden.setOnAction(e -> executeTsonGolden());
    }

    private void handleBrowseDataset() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn tập tin dữ liệu (.dat / .txt)");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tập dữ liệu (*.dat, *.txt)", "*.dat", "*.txt"),
                new FileChooser.ExtensionFilter("Tất cả tập tin", "*.*")
        );
        File file = fc.showOpenDialog(btnBrowseDataset.getScene().getWindow());
        if (file != null) {
            selectedCustomDatasetPath = file.toPath();
            DatasetItem customItem = new DatasetItem(
                    file.getName(),
                    "📁 " + file.getName() + " (Tùy chọn)",
                    0.15,
                    file.getAbsolutePath()
            );
            cbTsonDataset.getItems().add(customItem);
            cbTsonDataset.setValue(customItem);
            appendConsoleLog("[FILE] Đã chọn file ngoài: " + file.getAbsolutePath());
        }
    }

    private Path resolveDatasetPath() {
        if (selectedCustomDatasetPath != null) {
            return selectedCustomDatasetPath;
        }
        DatasetItem item = cbTsonDataset.getValue();
        String filename = (item != null) ? item.filename() : "default.dat";

        // Thử tìm trong thư mục dataset của dự án
        Path p = Paths.get("dataset", filename);
        if (java.nio.file.Files.exists(p)) return p;

        p = Paths.get("/tmp/tson_jvnc/dataset", filename);
        if (java.nio.file.Files.exists(p)) return p;

        return Paths.get(filename);
    }

    private long resolveLimit() {
        String val = cbTsonLimit.getValue();
        if (val == null || val.startsWith("Toàn bộ")) return 0;
        if (val.startsWith("1,000")) return 1000;
        if (val.startsWith("5,000")) return 5000;
        if (val.startsWith("10,000")) return 10000;
        if (val.startsWith("50,000")) return 50000;
        return 0;
    }

    // ── Tson Actions (Lệnh CLI biến thành Nút Bấm) ───────────────────────────

    /**
     * Nút "🚀 Khai Phá (Mine)": Chạy mining bằng engine Tson trên dataset đã chọn.
     */
    private void executeTsonMine() {
        Path path = resolveDatasetPath();
        double partial = sliderMinSup.getValue();
        double f = sliderF.getValue();
        long limit = resolveLimit();

        // Chuyển sang Tson Engine mode
        if (cbEngineMode.getValue() != EngineMode.TSON_V1_STANDARD) {
            cbEngineMode.setValue(EngineMode.TSON_V1_STANDARD);
        }

        // Chuyển sang Tab 4 (Console) để xem tiến trình
        mainTabPane.getSelectionModel().select(3);

        setButtonsDisable(true);
        progressMining.setProgress(-1); // Indeterminate spinner
        appendConsoleLog(String.format("%n[LỆNH MINE] Bắt đầu khai phá %s với ∂=%.4f, f=%.2f, limit=%d...%n",
                path.getFileName(), partial, f, limit));

        Task<MineExecutionResult> task = new Task<>() {
            @Override
            protected MineExecutionResult call() throws Exception {
                return tsonService.runMine(
                        path, partial, f, 4, limit,
                        phaseMsg -> Platform.runLater(() -> {
                            lblLastPhase.setText(phaseMsg);
                            appendConsoleLog("  " + phaseMsg);
                        }),
                        progress -> Platform.runLater(() -> progressMining.setProgress(progress))
                );
            }
        };

        task.setOnSucceeded(e -> {
            setButtonsDisable(false);
            progressMining.setProgress(1.0);
            MineExecutionResult res = task.getValue();

            // Cập nhật Console
            appendConsoleLog(res.logOutput());

            // Cập nhật KPI Cards
            lblKpiTime.setText(String.format("%,d ms", res.totalMs()));
            lblKpiPhases.setText(String.format("Constr: %dms | Reconst: %dms | Mine: %dms",
                    res.constrMs(), res.reconstMs(), res.miningMs()));
            lblKpiHeap.setText(String.format("%.1f MB", res.peakHeapMb()));
            lblKpiPatterns.setText(String.format("%,d mẫu", res.patternCount()));
            lblKpiMinSup.setText(String.format("minSup = %.2f", res.rawResult().minSup()));
            double throughput = res.totalMs() > 0 ? (res.totalTransactions() * 1000.0) / res.totalMs() : 0;
            lblKpiThroughput.setText(String.format("%,.0f tx/s", throughput));
            lblKpiTrans.setText(String.format("Tổng giao dịch: %,d", res.totalTransactions()));

            // Cập nhật TableView và biểu đồ
            tableData.setAll(res.uiPatterns());
            lblDhopCount.setText(res.patternCount() + " mẫu");
            lblVisitedCount.setText(res.patternCount() + " mẫu");
            lblTransCount.setText(String.valueOf(res.totalTransactions()));
            lblTL.setText("T" + res.rawResult().lastTid());

            // Cập nhật biểu đồ đường nếu có mẫu
            updateChartFromPatterns(res.uiPatterns(), res.rawResult().minSup());
        });

        task.setOnFailed(e -> {
            setButtonsDisable(false);
            progressMining.setProgress(0);
            Throwable err = task.getException();
            appendConsoleLog("[LỖI] Khai phá thất bại: " + err.getMessage());
            err.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Lỗi khi chạy Tson Mine: " + err.getMessage()).show();
        });

        new Thread(task, "TsonMineWorker").start();
    }

    /**
     * Nút "📊 Thống kê (Inspect)": Phân tích tập dữ liệu không cần mining.
     */
    private void executeTsonInspect() {
        Path path = resolveDatasetPath();
        double partial = sliderMinSup.getValue();
        long limit = resolveLimit();

        mainTabPane.getSelectionModel().select(3); // Mở Tab Console
        setButtonsDisable(true);
        appendConsoleLog(String.format("%n[LỆNH INSPECT] Đang phân tích tập tin %s...%n", path.getFileName()));

        Task<InspectReport> task = new Task<>() {
            @Override
            protected InspectReport call() throws Exception {
                return tsonService.runInspect(path, partial, limit, 10);
            }
        };

        task.setOnSucceeded(e -> {
            setButtonsDisable(false);
            InspectReport rep = task.getValue();
            appendConsoleLog(rep.textOutput());

            // Cập nhật các label thống kê
            lblTransCount.setText(String.format("%,d", rep.totalTransactions()));
            lblTL.setText("T" + rep.lastTid());
            lblKpiTrans.setText(String.format("Tổng giao dịch: %,d", rep.totalTransactions()));
            lblKpiMinSup.setText(String.format("minSup = %.2f", rep.minSupAbsolute()));
        });

        task.setOnFailed(e -> {
            setButtonsDisable(false);
            appendConsoleLog("[LỖI] Inspect thất bại: " + task.getException().getMessage());
        });

        new Thread(task, "TsonInspectWorker").start();
    }

    /**
     * Nút "🏆 Golden TC1-TC8": Chạy bộ kiểm thử vàng nghiệm thu của Tson.
     */
    private void executeTsonGolden() {
        mainTabPane.getSelectionModel().select(3); // Mở Tab Console
        setButtonsDisable(true);
        progressMining.setProgress(-1);
        appendConsoleLog(String.format("%n[LỆNH GOLDEN] Bắt đầu chạy bộ kiểm định TestKit TC1 - TC8...%n"));

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return tsonService.runGoldenTestKit();
            }
        };

        task.setOnSucceeded(e -> {
            setButtonsDisable(false);
            progressMining.setProgress(1.0);
            appendConsoleLog(task.getValue());
        });

        task.setOnFailed(e -> {
            setButtonsDisable(false);
            progressMining.setProgress(0);
            appendConsoleLog("[LỖI] Golden TestKit thất bại: " + task.getException().getMessage());
        });

        new Thread(task, "TsonGoldenWorker").start();
    }

    /**
     * Nút "🔬 Top DO (Detail)": Debug chi tiết các mẫu DO cao nhất.
     */
    private void executeTsonDetail() {
        Path path = resolveDatasetPath();
        double partial = sliderMinSup.getValue();
        double f = sliderF.getValue();
        long limit = resolveLimit();

        mainTabPane.getSelectionModel().select(3);
        setButtonsDisable(true);
        appendConsoleLog(String.format("%n[LỆNH DETAIL] Đang lọc chi tiết Top mẫu theo DO cho %s...%n", path.getFileName()));

        Task<MineExecutionResult> task = new Task<>() {
            @Override
            protected MineExecutionResult call() throws Exception {
                return tsonService.runMine(path, partial, f, 4, limit, null, null);
            }
        };

        task.setOnSucceeded(e -> {
            setButtonsDisable(false);
            MineExecutionResult res = task.getValue();
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("=== CHI TIẾT TOP 15 MẪU THEO DO (DETAIL DEBUG) ===%n"));
            sb.append(String.format("  Tập tin: %s | ∂=%.4f | f=%.2f | Tổng mẫu: %,d%n",
                    path.getFileName(), partial, f, res.patternCount()));
            sb.append(String.format("--------------------------------------------------%n"));
            sb.append(String.format("  %-24s %-12s %-10s%n", "Mẫu (Itemset)", "DO", "Support (TIDs)"));
            sb.append(String.format("--------------------------------------------------%n"));

            int limitTop = Math.min(15, res.uiPatterns().size());
            for (int i = 0; i < limitTop; i++) {
                PatternResult p = res.uiPatterns().get(i);
                sb.append(String.format("  %-24s %-12.6f %-10d%n", p.pattern(), p.doValue(), p.support()));
            }
            sb.append(String.format("==================================================%n"));
            appendConsoleLog(sb.toString());

            tableData.setAll(res.uiPatterns());
        });

        task.setOnFailed(e -> {
            setButtonsDisable(false);
            appendConsoleLog("[LỖI] Detail thất bại: " + task.getException().getMessage());
        });

        new Thread(task, "TsonDetailWorker").start();
    }

    private void setButtonsDisable(boolean disable) {
        btnTsonMine.setDisable(disable);
        btnTsonInspect.setDisable(disable);
        btnTsonDetail.setDisable(disable);
        btnTsonGolden.setDisable(disable);
        btnStartStream.setDisable(disable);
    }

    // ── Console Panel ────────────────────────────────────────────────────────

    private void initConsolePanel() {
        btnCopyConsole.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(txtTsonConsole.getText());
            clipboard.setContent(content);
            appendConsoleLog("[HỆ THỐNG] Đã sao chép toàn bộ nhật ký vào Clipboard!");
        });

        btnClearConsole.setOnAction(e -> txtTsonConsole.clear());

        appendConsoleLog("⚡ DHOPM Stream Visualizer v3.0 sẵn sàng.");
        appendConsoleLog("  • Bấm '🚀 Khai Phá Dataset' để chạy engine Tson không cần gõ lệnh.");
        appendConsoleLog("  • Bấm '🏆 Chạy TestKit Vàng' để soát 8 test cases chuẩn bài báo.");
        appendConsoleLog("  • Bấm '📊 Thống kê' để phân tích đặc trưng dataset FIMI.");
    }

    private void appendConsoleLog(String text) {
        Platform.runLater(() -> {
            txtTsonConsole.appendText(text + "\n");
            txtTsonConsole.positionCaret(txtTsonConsole.getText().length());
        });
    }

    // ── Switch Engine ────────────────────────────────────────────────────────

    private void switchEngine(EngineMode mode) {
        if (mode == null || mode == currentMode) return;
        currentMode = mode;

        streamSimulator.stop();
        btnStartStream.setDisable(false);
        btnPauseStream.setDisable(true);
        lblStreamStatus.setText("Sẵn sàng");
        lblStreamStatus.setStyle("-fx-text-fill: #2563eb;");

        buildBridgeEngine();
        loadInitialPaperData();

        lblEngineName.setText("⚡ " + bridgeEngine.engineName());
        boolean isTam = mode == EngineMode.TAM_SIMULATION;
        btnStartStream.setDisable(!isTam);
        btnStepStream.setDisable(!isTam);
        lblSortOrder.setVisible(isTam);
    }

    private void buildBridgeEngine() {
        double f = sliderF != null ? sliderF.getValue() : 0.9;
        double ratio = sliderMinSup != null ? sliderMinSup.getValue() : 0.15;
        bridgeEngine = EngineFactory.create(currentMode, ratio, f);

        bridgeEngine.onPhaseUpdate((phase, ms) ->
                lblLastPhase.setText(phase + " (" + String.format("%.2f", ms) + " ms)")
        );

        bridgeEngine.onMiningProgress(fraction ->
                progressMining.setProgress(fraction)
        );
    }

    private void initTamEngine() {
        tamEngine = new DHOPMEngine();
        streamSimulator = new StreamSimulator(tamEngine);
        tamEngine.addStreamListener((newBatch, currentTL) -> Platform.runLater(this::updateUI));
        buildBridgeEngine();
    }

    private void initTableView() {
        colPattern.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().pattern()));
        colSupport.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().support()));
        colDO.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedDO()));
        colDUBO.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedDUBO()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        colTransactions.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().transactionIds().stream()
                        .map(tid -> "T" + tid)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("")
        ));

        tableResults.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(PatternResult item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item.isDHOP()) {
                    setStyle("-fx-background-color: #dcfce7; -fx-font-weight: bold;");
                } else if (item.isPruned()) {
                    setStyle("-fx-background-color: #fee2e2;");
                } else {
                    setStyle("");
                }
            }
        });

        tableResults.setItems(tableData);
    }

    private void initEventHandlers() {
        sliderF.valueProperty().addListener((obs, oldVal, newVal) -> {
            double f = Math.round(newVal.doubleValue() * 100.0) / 100.0;
            lblFValue.setText(String.format("%.2f", f));
            recalculateAndRender();
        });

        sliderMinSup.valueProperty().addListener((obs, oldVal, newVal) -> {
            double ratio = Math.round(newVal.doubleValue() * 100.0) / 100.0;
            lblMinSupValue.setText(String.format("%.0f%%", ratio * 100));
            recalculateAndRender();
        });

        btnStartStream.setOnAction(e -> {
            streamSimulator.start(1500);
            btnStartStream.setDisable(true);
            btnPauseStream.setDisable(false);
            lblStreamStatus.setText("Đang chạy ●");
            lblStreamStatus.setStyle("-fx-text-fill: #16a34a;");
        });

        btnPauseStream.setOnAction(e -> {
            streamSimulator.stop();
            btnStartStream.setDisable(false);
            btnPauseStream.setDisable(true);
            lblStreamStatus.setText("Tạm dừng ⏸");
            lblStreamStatus.setStyle("-fx-text-fill: #ea580c;");
        });

        btnStepStream.setOnAction(e -> {
            Transaction t = streamSimulator.stepNext();
            if (t != null) {
                updateUI();
            } else {
                new Alert(Alert.AlertType.INFORMATION, "Đã bơm hết toàn bộ giao dịch trong luồng mẫu!").show();
            }
        });

        btnReset.setOnAction(e -> {
            bridgeEngine.reset();
            loadInitialPaperData();
        });
    }

    private void loadInitialPaperData() {
        streamSimulator.stop();
        btnPauseStream.setDisable(true);
        lblStreamStatus.setText("Sẵn sàng");
        lblStreamStatus.setStyle("-fx-text-fill: #2563eb;");

        tamEngine.reset();
        if (bridgeEngine != null) bridgeEngine.reset();

        List<Transaction> paperData = DatasetLoader.getPaperDataset();
        tamEngine.phase1_constructOrUpdate(paperData);
        streamSimulator.setStreamQueue(DatasetLoader.getSyntheticStreamDataset());

        if (bridgeEngine != null) {
            bridgeEngine.feedTransactions(paperData);
        }

        sliderF.setValue(0.90);
        sliderMinSup.setValue(0.15);
        progressMining.setProgress(0);
        lblLastPhase.setText("—");
        lblEngineName.setText("⚡ " + (bridgeEngine != null ? bridgeEngine.engineName() : "Tâm-Simulation"));

        updateUI();
    }

    private void recalculateAndRender() {
        double f = sliderF.getValue();
        double ratio = sliderMinSup.getValue();
        int totalTrans = tamEngine.getAllTransactions().size();
        double minSup = totalTrans * ratio;

        lblAbsoluteMinSup.setText(String.format("%.2f", minSup));

        List<PatternResult> results;
        if (currentMode == EngineMode.TAM_SIMULATION || bridgeEngine == null) {
            int TL = tamEngine.getCurrentTL();
            results = tamEngine.phase3_mine(minSup, f, TL);
            lblTL.setText("T" + TL);
            updateDHONodes(f, TL, minSup);
        } else {
            bridgeEngine.reset();
            bridgeEngine.feedTransactions(tamEngine.getAllTransactions());
            results = bridgeEngine.executeAndGetResults();
            lblTL.setText("T" + bridgeEngine.lastTid());
            updateDHONodes(f, tamEngine.getCurrentTL(), minSup);
        }

        List<PatternResult> dhops = results.stream().filter(PatternResult::isDHOP).toList();
        lblTransCount.setText(String.valueOf(totalTrans));
        lblDhopCount.setText(dhops.size() + " mẫu");
        lblVisitedCount.setText(results.size() + " mẫu");

        long prunedCount = results.stream().filter(PatternResult::isPruned).count();
        String prunedText = results.isEmpty() ? "0 mẫu"
                : prunedCount + " mẫu (" + String.format("%.1f%%", ((double) prunedCount / results.size()) * 100) + ")";
        lblPrunedCount.setText(prunedText);

        tableData.setAll(results);
        updateChart(results, f, minSup);
    }

    private void updateDHONodes(double f, int TL, double minSup) {
        List<DHONode> sortedNodes = tamEngine.getGlobalList().getSortedNodes();

        StringBuilder orderSb = new StringBuilder();
        for (int i = 0; i < sortedNodes.size(); i++) {
            orderSb.append(sortedNodes.get(i).getItemName())
                    .append(" (S=").append(sortedNodes.get(i).getSupport()).append(")");
            if (i < sortedNodes.size() - 1) orderSb.append(" ≺ ");
        }
        lblSortOrder.setText(orderSb.toString());

        dhoNodesPane.getChildren().clear();
        for (DHONode node : sortedNodes) {
            dhoNodesPane.getChildren().add(new DHONodeCard(node, f, TL, minSup));
        }
    }

    private void updateChart(List<PatternResult> results, double f, double minSup) {
        lineChartDO.getData().clear();

        XYChart.Series<String, Number> itemSeries = new XYChart.Series<>();
        itemSeries.setName("DO(Items đơn lẻ)");
        for (DHONode node : tamEngine.getGlobalList().getSortedNodes()) {
            itemSeries.getData().add(new XYChart.Data<>(node.getItemName(), node.getDoValue()));
        }

        XYChart.Series<String, Number> minSupSeries = new XYChart.Series<>();
        minSupSeries.setName("Ngưỡng minSup (" + String.format("%.2f", minSup) + ")");
        for (DHONode node : tamEngine.getGlobalList().getSortedNodes()) {
            minSupSeries.getData().add(new XYChart.Data<>(node.getItemName(), minSup));
        }

        lineChartDO.getData().addAll(itemSeries, minSupSeries);
    }

    private void updateChartFromPatterns(List<PatternResult> patterns, double minSup) {
        lineChartDO.getData().clear();

        XYChart.Series<String, Number> patternSeries = new XYChart.Series<>();
        patternSeries.setName("DO mẫu tìm được");
        int count = Math.min(10, patterns.size());
        for (int i = 0; i < count; i++) {
            PatternResult p = patterns.get(i);
            patternSeries.getData().add(new XYChart.Data<>(p.pattern(), p.doValue()));
        }

        XYChart.Series<String, Number> minSupSeries = new XYChart.Series<>();
        minSupSeries.setName("Ngưỡng minSup (" + String.format("%.2f", minSup) + ")");
        for (int i = 0; i < count; i++) {
            PatternResult p = patterns.get(i);
            minSupSeries.getData().add(new XYChart.Data<>(p.pattern(), minSup));
        }

        lineChartDO.getData().addAll(patternSeries, minSupSeries);
    }

    public void updateUI() {
        recalculateAndRender();
    }
}
