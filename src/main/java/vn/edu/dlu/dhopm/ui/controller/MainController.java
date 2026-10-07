package vn.edu.dlu.dhopm.ui.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.collections.transformation.FilteredList;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import vn.edu.dlu.dhopm.bridge.*;
import vn.edu.dlu.dhopm.bridge.TsonToolsService.DatasetItem;
import vn.edu.dlu.dhopm.bridge.TsonToolsService.MineExecutionResult;
import vn.edu.dlu.dhopm.bridge.TsonToolsService.InspectReport;
import vn.edu.dlu.dhopm.history.MiningHistoryManager;
import vn.edu.dlu.dhopm.history.MiningRunMemento;
import vn.edu.dlu.dhopm.log.CalculationLogEntry;
import vn.edu.dlu.dhopm.log.CalculationLogger;
import vn.edu.dlu.dhopm.core.DatasetLoader;
import vn.edu.dlu.dhopm.core.DHOPMEngine;
import vn.edu.dlu.dhopm.core.MinSupSweepService;
import vn.edu.dlu.dhopm.core.StreamSimulator;
import vn.edu.dlu.dhopm.model.DHONode;
import vn.edu.dlu.dhopm.model.MinSupSweepPoint;
import vn.edu.dlu.dhopm.model.PatternResult;
import vn.edu.dlu.dhopm.model.Transaction;
import vn.edu.dlu.dhopm.ui.component.DHONodeCard;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller điều khiển toàn bộ giao diện JavaFX cho DHOPM Visualizer.
 * Version 4.0: Chuyển đổi 100% Terminal text sang các bảng TableView đồ họa cao cấp,
 * thẳng hàng tuyệt đối, hỗ trợ sắp xếp và co giãn cột mượt mà.
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
    @FXML private Button btnTsonStop;
    @FXML private Button btnTsonInspect;
    @FXML private Button btnTsonDetail;
    @FXML private Button btnTsonGolden;
    @FXML private Label lblMiningEta;
    @FXML private Label lblTab4EtaStatus;

    // ── Active Background Task & Engine Cancellation ──────────────────────────
    private Task<?> activeTsonTask = null;
    private Thread activeTsonThread = null;
    private final java.util.concurrent.atomic.AtomicReference<dhopm.v1.engine.MiningEngine> activeMiningEngine = new java.util.concurrent.atomic.AtomicReference<>(null);

    // ── Controls & Sliders ───────────────────────────────────────────────────
    @FXML private Slider sliderF;
    @FXML private Slider sliderMinSup;
    @FXML private TextField txtMinSupRatio;
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
    @FXML private TableColumn<PatternResult, String> colSupport;
    @FXML private TableColumn<PatternResult, String> colDO;
    @FXML private TableColumn<PatternResult, String> colDUBO;
    @FXML private TableColumn<PatternResult, String> colStatus;
    @FXML private TableColumn<PatternResult, String> colTransactions;

    // ── Tab 3: minSup Sweep Controls & Paper Benchmark Figures ───────────────
    @FXML private TextField txtSweepStart;
    @FXML private TextField txtSweepEnd;
    @FXML private TextField txtSweepStep;
    @FXML private Button btnPresetPaper;
    @FXML private Button btnPresetFine;
    @FXML private Button btnRunSweep;
    @FXML private Button btnCancelSweep;
    @FXML private ProgressBar progressSweep;
    @FXML private Label lblSweepStatus;
    @FXML private Label lblSweepEta;
    @FXML private Label lblSweepCountBadge;

    @FXML private Button btnChartFig11;
    @FXML private Button btnChartFig6;
    @FXML private Button btnChartFig13;
    @FXML private Button btnChartModeDO;
    @FXML private Button btnChartModeHistory;
    @FXML private LineChart<String, Number> lineChartFig11;
    @FXML private LineChart<String, Number> lineChartFig6;
    @FXML private LineChart<String, Number> lineChartFig13;
    @FXML private LineChart<String, Number> lineChartDO;
    @FXML private LineChart<String, Number> lineChartHistory;
    @FXML private BarChart<String, Number> barChartDistribution;

    @FXML private Button btnViewSweepTable;
    @FXML private Button btnViewHistoryTable;
    @FXML private Button btnClearHistory;

    @FXML private TableView<MinSupSweepPoint> tblSweepResults;
    @FXML private TableColumn<MinSupSweepPoint, String> colSwpPartial;
    @FXML private TableColumn<MinSupSweepPoint, String> colSwpMinSup;
    @FXML private TableColumn<MinSupSweepPoint, String> colSwpDhops;
    @FXML private TableColumn<MinSupSweepPoint, String> colSwpPruned;
    @FXML private TableColumn<MinSupSweepPoint, String> colSwpPrunedRatio;
    @FXML private TableColumn<MinSupSweepPoint, String> colSwpCandidates;
    @FXML private TableColumn<MinSupSweepPoint, String> colSwpRuntime;
    @FXML private TableColumn<MinSupSweepPoint, String> colSwpHeap;

    @FXML private TableView<MiningRunMemento> tblMiningHistory;
    @FXML private TableColumn<MiningRunMemento, Number> colHistRunId;
    @FXML private TableColumn<MiningRunMemento, String> colHistTime;
    @FXML private TableColumn<MiningRunMemento, String> colHistEngine;
    @FXML private TableColumn<MiningRunMemento, String> colHistDataset;
    @FXML private TableColumn<MiningRunMemento, String> colHistF;
    @FXML private TableColumn<MiningRunMemento, String> colHistPartial;
    @FXML private TableColumn<MiningRunMemento, String> colHistMinSup;
    @FXML private TableColumn<MiningRunMemento, String> colHistTx;
    @FXML private TableColumn<MiningRunMemento, String> colHistDhops;
    @FXML private TableColumn<MiningRunMemento, String> colHistPruned;
    @FXML private TableColumn<MiningRunMemento, String> colHistRuntime;

    private final MinSupSweepService sweepService = new MinSupSweepService();
    private final ObservableList<MinSupSweepPoint> sweepData = FXCollections.observableArrayList();
    private Task<List<MinSupSweepPoint>> activeSweepTask;

    // ── Tab 4: Tson Results & Benchmark TableViews & KPI Cards ───────────────
    @FXML private Label lblKpiTime;
    @FXML private Label lblKpiPhases;
    @FXML private Label lblKpiHeap;
    @FXML private Label lblKpiPatterns;
    @FXML private Label lblKpiMinSup;
    @FXML private Label lblKpiThroughput;
    @FXML private Label lblKpiTrans;

    @FXML private Label lblTsonViewTitle;
    @FXML private Button btnViewPatterns;
    @FXML private Button btnViewInspect;
    @FXML private Button btnViewGolden;

    // View 1: Mẫu DHOPs TableView
    @FXML private TableView<PatternResult> tblTsonPatterns;
    @FXML private TableColumn<PatternResult, Number> colTsonIndex;
    @FXML private TableColumn<PatternResult, String> colTsonPattern;
    @FXML private TableColumn<PatternResult, String> colTsonDO;
    @FXML private TableColumn<PatternResult, String> colTsonSupport;
    @FXML private TableColumn<PatternResult, Number> colTsonLength;
    @FXML private TableColumn<PatternResult, String> colTsonTids;

    // View 2: Inspect Card & TableView
    @FXML private VBox boxTsonInspect;
    @FXML private Label lblInspectFile;
    @FXML private Label lblInspectTx;
    @FXML private Label lblInspectTL;
    @FXML private Label lblInspectItems;
    @FXML private Label lblInspectAvgLen;
    @FXML private Label lblInspectMaxLen;
    @FXML private TableView<TopItemStat> tblInspectTopItems;
    @FXML private TableColumn<TopItemStat, Number> colInspectRank;
    @FXML private TableColumn<TopItemStat, String> colInspectItem;
    @FXML private TableColumn<TopItemStat, String> colInspectSupport;
    @FXML private TableColumn<TopItemStat, String> colInspectPercentage;

    // View 3: Golden Matrix TableView
    @FXML private TableView<GoldenRow> tblTsonGolden;
    @FXML private TableColumn<GoldenRow, String> colGoldenCase;
    @FXML private TableColumn<GoldenRow, String> colGoldenF;
    @FXML private TableColumn<GoldenRow, String> colGoldenPartial;
    @FXML private TableColumn<GoldenRow, String> colGoldenMinSup;
    @FXML private TableColumn<GoldenRow, String> colGoldenExpected;
    @FXML private TableColumn<GoldenRow, String> colGoldenActual;
    @FXML private TableColumn<GoldenRow, String> colGoldenStatus;

    // ── Tab 5: Nhật ký chi tiết từng phép tính ───────────────────────────────
    @FXML private TextField txtLogFilter;
    @FXML private ComboBox<String> cbLogPhaseFilter;
    @FXML private Button btnClearLog;
    @FXML private Button btnExportLog;
    @FXML private TableView<CalculationLogEntry> tblCalculationLog;
    @FXML private TableColumn<CalculationLogEntry, Number> colLogId;
    @FXML private TableColumn<CalculationLogEntry, String> colLogTime;
    @FXML private TableColumn<CalculationLogEntry, String> colLogPhase;
    @FXML private TableColumn<CalculationLogEntry, String> colLogTarget;
    @FXML private TableColumn<CalculationLogEntry, String> colLogFormula;
    @FXML private TableColumn<CalculationLogEntry, String> colLogComparison;
    @FXML private TableColumn<CalculationLogEntry, String> colLogDecision;
    @FXML private TextArea txtLogDetail;

    // ── Backend Services ─────────────────────────────────────────────────────
    private DHOPMEngine tamEngine;
    private StreamSimulator streamSimulator;
    private BridgeEngine bridgeEngine;
    private final TsonToolsService tsonService = new TsonToolsService();

    private final ObservableList<PatternResult> tableData = FXCollections.observableArrayList();
    private final ObservableList<PatternResult> tsonPatternsData = FXCollections.observableArrayList();
    private final ObservableList<TopItemStat> inspectTopItemsData = FXCollections.observableArrayList();
    private final ObservableList<GoldenRow> goldenRowsData = FXCollections.observableArrayList();

    private EngineMode currentMode = EngineMode.TAM_SIMULATION;
    private Path selectedCustomDatasetPath = null;
    private long activeTotalTransactions = 8;

    private String formatSupportWithPercent(int support) {
        long total = activeTotalTransactions > 0 ? activeTotalTransactions :
                (tamEngine != null && !tamEngine.getAllTransactions().isEmpty() ? tamEngine.getAllTransactions().size() : 8);
        if (total <= 0) {
            return String.format(java.util.Locale.US, "%,d tx", support);
        }
        double pct = (100.0 * support) / (double) total;
        return String.format(java.util.Locale.US, "%,d tx (%.1f%%)", support, pct);
    }

    private static int extractSupportInt(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            String cleaned = s.replaceAll("\\s*tx.*", "").replaceAll("[^0-9]", "");
            return cleaned.isEmpty() ? 0 : Integer.parseInt(cleaned);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initEngineSelectorPanel();
        initTsonToolsPanel();
        initTamEngine();
        initTableView();
        initTsonTableViews();
        initMiningHistoryViews();
        initCalculationLogView();
        initEventHandlers();
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
        cbTsonDataset.setValue(presets.get(0));

        cbTsonDataset.setOnAction(e -> {
            DatasetItem item = cbTsonDataset.getValue();
            if (item != null) {
                sliderMinSup.setValue(item.defaultPartial());
            }
        });

        // 2. ComboBox Limit (Editable: vừa chọn nhanh, vừa tự gõ số tùy ý)
        cbTsonLimit.setEditable(true);
        cbTsonLimit.setItems(FXCollections.observableArrayList(
                "Toàn bộ (0)",
                "1,000 tx",
                "5,000 tx",
                "10,000 tx",
                "25,000 tx",
                "50,000 tx",
                "100,000 tx"
        ));
        cbTsonLimit.setValue("Toàn bộ (0)");
        cbTsonLimit.getEditor().setPromptText("Tự gõ số (VD: 2500)");

        // 3. Nút Browse file ngoài
        btnBrowseDataset.setOnAction(e -> handleBrowseDataset());

        // 4. Các nút tương ứng bộ lệnh CLI của Tson
        btnTsonMine.setOnAction(e -> executeTsonMine());
        if (btnTsonStop != null) {
            btnTsonStop.setOnAction(e -> handleStopMining());
        }
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
        }
    }

    private Path resolveDatasetPath() {
        if (selectedCustomDatasetPath != null) {
            return selectedCustomDatasetPath;
        }
        DatasetItem item = cbTsonDataset.getValue();
        String filename = (item != null) ? item.filename() : "default.dat";

        Path p = Paths.get("dataset", filename);
        if (java.nio.file.Files.exists(p)) return p;

        p = Paths.get("/tmp/tson_jvnc/dataset", filename);
        if (java.nio.file.Files.exists(p)) return p;

        return Paths.get(filename);
    }

    private long resolveLimit() {
        String val = null;
        if (cbTsonLimit.getEditor() != null && cbTsonLimit.getEditor().getText() != null) {
            val = cbTsonLimit.getEditor().getText().trim();
        }
        if (val == null || val.isBlank()) {
            val = cbTsonLimit.getValue();
        }
        if (val == null || val.isBlank() || val.startsWith("Toàn bộ") || val.equalsIgnoreCase("all") || val.equals("0")) {
            return 0;
        }

        String cleaned = val.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) return 0;
        try {
            long parsed = Long.parseLong(cleaned);
            return Math.max(0, parsed);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ── Tab 4: Tson TableViews Setup ──────────────────────────────────────────

    private void initTsonTableViews() {
        // ── 1. Setup View Switcher Buttons
        btnViewPatterns.setOnAction(e -> showTsonView(1));
        btnViewInspect.setOnAction(e -> showTsonView(2));
        btnViewGolden.setOnAction(e -> showTsonView(3));

        // ── 2. View 1: tblTsonPatterns (Mẫu DHOPs)
        colTsonIndex.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(1 + tblTsonPatterns.getItems().indexOf(cellData.getValue())));
        colTsonIndex.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });
        colTsonPattern.setCellValueFactory(cellData -> {
            String raw = cellData.getValue().pattern();
            String[] parts = raw.split("[,\\s]+");
            return new SimpleStringProperty("{" + String.join(", ", parts) + "}");
        });
        colTsonDO.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.6f", cellData.getValue().doValue())));
        colTsonSupport.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatSupportWithPercent(cellData.getValue().support())));
        colTsonSupport.setComparator((s1, s2) -> Integer.compare(extractSupportInt(s1), extractSupportInt(s2)));
        colTsonLength.setCellValueFactory(cellData -> {
            String raw = cellData.getValue().pattern().replace("{", "").replace("}", "").trim();
            return new SimpleIntegerProperty(raw.split("[,\\s]+").length);
        });
        colTsonTids.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().transactionIds().stream()
                        .limit(8)
                        .map(tid -> "T" + tid)
                        .reduce((a, b) -> a + ", " + b)
                        .map(str -> cellData.getValue().transactionIds().size() > 8 ? str + "..." : str)
                        .orElse("")));

        tblTsonPatterns.setItems(tsonPatternsData);

        // ── 3. View 2: tblInspectTopItems (Top Items)
        colInspectRank.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().rank()));
        colInspectItem.setCellValueFactory(cellData -> new SimpleStringProperty("Mục " + cellData.getValue().item()));
        colInspectSupport.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedSupport()));
        colInspectPercentage.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedPercentage()));
        tblInspectTopItems.setItems(inspectTopItemsData);

        // ── 4. View 3: tblTsonGolden (Ma Trận Vàng)
        colGoldenCase.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().caseName()));
        colGoldenF.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedF()));
        colGoldenPartial.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedPartial()));
        colGoldenMinSup.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedMinSup()));
        colGoldenExpected.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().expected()));
        colGoldenActual.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().actual()));
        colGoldenStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().status()));

        colGoldenStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("PASS")) {
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    }
                }
            }
        });

        tblTsonGolden.setItems(goldenRowsData);

        // Mặc định hiển thị View 1
        showTsonView(1);
    }

    private void showTsonView(int viewIndex) {
        tblTsonPatterns.setVisible(viewIndex == 1);
        tblTsonPatterns.setManaged(viewIndex == 1);
        boxTsonInspect.setVisible(viewIndex == 2);
        boxTsonInspect.setManaged(viewIndex == 2);
        tblTsonGolden.setVisible(viewIndex == 3);
        tblTsonGolden.setManaged(viewIndex == 3);

        btnViewPatterns.setStyle(viewIndex == 1 ? "-fx-background-color: #2563eb; -fx-text-fill: white;" : "");
        btnViewInspect.setStyle(viewIndex == 2 ? "-fx-background-color: #059669; -fx-text-fill: white;" : "");
        btnViewGolden.setStyle(viewIndex == 3 ? "-fx-background-color: #7c3aed; -fx-text-fill: white;" : "");

        if (viewIndex == 1) {
            lblTsonViewTitle.setText("📋 Danh Sách Mẫu Chi Tiết (Tson V1 Engine)");
        } else if (viewIndex == 2) {
            lblTsonViewTitle.setText("📊 Phân Tích Thuộc Tính & Top Mặt Hàng (Inspect)");
        } else {
            lblTsonViewTitle.setText("🏆 Bảng Ma Trận Kiểm Định 8 Bài Toán Vàng (Golden TC1 - TC8)");
        }
    }

    // ── Tab 3: Mining History & Multi-Chart Setup ─────────────────────────────

    // ── Tab 3: Mining History & Multi-Chart Setup (minSup Sweep & Paper Figures) ──

    private void initMiningHistoryViews() {
        // ── 1. Cấu hình bảng Memento Lịch sử (tblMiningHistory)
        if (colHistRunId != null) {
            colHistRunId.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getRunId()));
            colHistTime.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimestamp()));
            colHistEngine.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEngineName()));
            colHistDataset.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDatasetName()));
            colHistF.setCellValueFactory(cellData -> new SimpleStringProperty(String.format(java.util.Locale.US, "%.2f", cellData.getValue().getF())));
            colHistPartial.setCellValueFactory(cellData -> new SimpleStringProperty(String.format(java.util.Locale.US, "%.1f%%", cellData.getValue().getPartial() * 100.0)));
            colHistMinSup.setCellValueFactory(cellData -> new SimpleStringProperty(String.format(java.util.Locale.US, "%.2f", cellData.getValue().getMinSup())));
            colHistTx.setCellValueFactory(cellData -> new SimpleStringProperty(String.format(java.util.Locale.US, "%,d", cellData.getValue().getTotalTransactions())));
            colHistDhops.setCellValueFactory(cellData -> new SimpleStringProperty(String.format(java.util.Locale.US, "%,d mẫu", cellData.getValue().getDhopCount())));
            colHistPruned.setCellValueFactory(cellData -> new SimpleStringProperty(String.format(java.util.Locale.US, "%,d (%.0f%%)",
                    cellData.getValue().getPrunedCount(), cellData.getValue().getPrunedRatio() * 100.0)));
            colHistRuntime.setCellValueFactory(cellData -> new SimpleStringProperty(String.format(java.util.Locale.US, "%,d ms", cellData.getValue().getRuntimeMs())));

            tblMiningHistory.setItems(MiningHistoryManager.getInstance().getHistory());

            MiningHistoryManager.getInstance().getHistory().addListener((javafx.collections.ListChangeListener<MiningRunMemento>) c -> {
                updateHistoryCharts();
            });

            tblMiningHistory.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.getTopPatterns().isEmpty()) {
                    if (newVal.getTotalTransactions() > 0) {
                        activeTotalTransactions = newVal.getTotalTransactions();
                    }
                    tableData.setAll(newVal.getTopPatterns());
                    tsonPatternsData.setAll(newVal.getTopPatterns());
                    if (tableResults != null) tableResults.refresh();
                    if (tblTsonPatterns != null) tblTsonPatterns.refresh();
                    updateChartFromPatterns(newVal.getTopPatterns(), newVal.getMinSup());
                }
            });
        }

        // ── 2. Cấu hình bảng Quét minSup (tblSweepResults)
        if (tblSweepResults != null) {
            colSwpPartial.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPartialPercentFormatted()));
            colSwpMinSup.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMinSupFormatted()));
            colSwpDhops.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDhopCountFormatted()));
            colSwpPruned.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrunedCountFormatted()));
            colSwpPrunedRatio.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrunedRatioFormatted()));
            colSwpCandidates.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTotalCandidatesFormatted()));
            colSwpRuntime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRuntimeFormatted()));
            colSwpHeap.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHeapFormatted()));

            tblSweepResults.setItems(sweepData);

            tblSweepResults.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    sliderMinSup.setValue(newVal.partial());
                    if (txtMinSupRatio != null) {
                        txtMinSupRatio.setText(newVal.getPartialPercentFormatted());
                    }
                }
            });
        }

        // ── 3. Bộ nút chuyển đổi Biểu Đồ Bài Báo EAAI 2026
        if (btnChartFig11 != null) btnChartFig11.setOnAction(e -> showChartFigure(1));
        if (btnChartFig6 != null) btnChartFig6.setOnAction(e -> showChartFigure(2));
        if (btnChartFig13 != null) btnChartFig13.setOnAction(e -> showChartFigure(3));
        if (btnChartModeDO != null) btnChartModeDO.setOnAction(e -> showChartFigure(4));
        if (btnChartModeHistory != null) btnChartModeHistory.setOnAction(e -> showChartFigure(5));

        // ── 4. Bộ nút chuyển đổi Bảng Dữ Liệu
        if (btnViewSweepTable != null) btnViewSweepTable.setOnAction(e -> switchTableView(1));
        if (btnViewHistoryTable != null) btnViewHistoryTable.setOnAction(e -> switchTableView(2));
        switchTableView(1);

        // ── 5. Nút Presets Quét
        if (btnPresetPaper != null) {
            btnPresetPaper.setOnAction(e -> {
                txtSweepStart.setText("5%");
                txtSweepEnd.setText("30%");
                txtSweepStep.setText("5%");
            });
        }
        if (btnPresetFine != null) {
            btnPresetFine.setOnAction(e -> {
                txtSweepStart.setText("2%");
                txtSweepEnd.setText("20%");
                txtSweepStep.setText("2%");
            });
        }

        // ── 6. Nút Hành Động Quét Dải minSup
        if (btnRunSweep != null) btnRunSweep.setOnAction(e -> executeSweepBenchmark());
        if (btnCancelSweep != null) btnCancelSweep.setOnAction(e -> cancelSweepBenchmark());

        if (btnClearHistory != null) {
            btnClearHistory.setOnAction(e -> {
                MiningHistoryManager.getInstance().clearHistory();
                updateHistoryCharts();
            });
        }

        // ── 7. Khởi tạo sẵn dữ liệu Quét Mặc Định theo Bài Báo (default.dat 8 tx)
        try {
            List<MinSupSweepPoint> initialPoints = sweepService.generateInitialPaperBenchmarkPoints(tamEngine, sliderF.getValue());
            if (!initialPoints.isEmpty()) {
                sweepData.setAll(initialPoints);
                updateSweepCharts(initialPoints);
                if (lblSweepCountBadge != null) {
                    lblSweepCountBadge.setText(initialPoints.size() + " mốc minSup đã quét");
                }
            }
        } catch (Exception ignored) {}

        showChartFigure(1); // Mặc định mở Fig 11 (Runtime vs minSup)
    }

    /**
     * Chuyển đổi hiển thị biểu đồ tương ứng các Figures trong bài báo.
     * 1: Fig 11 (Runtime vs minSup)
     * 2: Fig 6 (DHOPs & DUBO Pruning vs minSup)
     * 3: Fig 13 (Peak Heap RAM vs minSup)
     * 4: Fig 2 (Item DO vs minSup)
     * 5: History LineChart
     */
    private void showChartFigure(int mode) {
        if (lineChartFig11 != null) { lineChartFig11.setVisible(mode == 1); lineChartFig11.setManaged(mode == 1); }
        if (lineChartFig6 != null) { lineChartFig6.setVisible(mode == 2); lineChartFig6.setManaged(mode == 2); }
        if (lineChartFig13 != null) { lineChartFig13.setVisible(mode == 3); lineChartFig13.setManaged(mode == 3); }
        if (lineChartDO != null) { lineChartDO.setVisible(mode == 4); lineChartDO.setManaged(mode == 4); }
        if (lineChartHistory != null) { lineChartHistory.setVisible(mode == 5); lineChartHistory.setManaged(mode == 5); }
        if (barChartDistribution != null) { barChartDistribution.setVisible(false); barChartDistribution.setManaged(false); }

        String active = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold;";
        if (btnChartFig11 != null) btnChartFig11.setStyle(mode == 1 ? active : "");
        if (btnChartFig6 != null) btnChartFig6.setStyle(mode == 2 ? active : "");
        if (btnChartFig13 != null) btnChartFig13.setStyle(mode == 3 ? active : "");
        if (btnChartModeDO != null) btnChartModeDO.setStyle(mode == 4 ? active : "");
        if (btnChartModeHistory != null) btnChartModeHistory.setStyle(mode == 5 ? active : "");
    }

    /**
     * Chuyển đổi giữa Bảng Quét minSup (1) và Bảng Lịch Sử Memento (2).
     */
    private void switchTableView(int mode) {
        if (tblSweepResults != null) {
            tblSweepResults.setVisible(mode == 1);
            tblSweepResults.setManaged(mode == 1);
        }
        if (tblMiningHistory != null) {
            tblMiningHistory.setVisible(mode == 2);
            tblMiningHistory.setManaged(mode == 2);
        }
        String active = "-fx-background-color: #0f172a; -fx-text-fill: white; -fx-font-weight: bold;";
        if (btnViewSweepTable != null) btnViewSweepTable.setStyle(mode == 1 ? active : "");
        if (btnViewHistoryTable != null) btnViewHistoryTable.setStyle(mode == 2 ? active : "");
    }

    /**
     * Cập nhật toàn bộ các biểu đồ chuẩn bài báo từ danh sách điểm quét minSup.
     */
    private void updateSweepCharts(List<MinSupSweepPoint> points) {
        if (points == null || points.isEmpty()) return;

        // 1. Figure 11: Runtime vs minSup (∂)
        if (lineChartFig11 != null) {
            lineChartFig11.getData().clear();
            XYChart.Series<String, Number> sRuntime = new XYChart.Series<>();
            sRuntime.setName("⏱️ Runtime DHOPM (ms)");
            for (MinSupSweepPoint p : points) {
                sRuntime.getData().add(new XYChart.Data<>(p.getPartialPercentFormatted(), p.runtimeMs()));
            }
            lineChartFig11.getData().add(sRuntime);
        }

        // 2. Figure 6: DHOPs & DUBO Pruning vs minSup (∂)
        if (lineChartFig6 != null) {
            lineChartFig6.getData().clear();
            XYChart.Series<String, Number> sDhop = new XYChart.Series<>();
            sDhop.setName("🟢 Mẫu DHOP Hợp Lệ");

            XYChart.Series<String, Number> sPruned = new XYChart.Series<>();
            sPruned.setName("🔴 Cắt Tỉa Bởi DUBO");

            XYChart.Series<String, Number> sTotal = new XYChart.Series<>();
            sTotal.setName("⚪ Tổng Ứng Viên DFS Duyệt");

            for (MinSupSweepPoint p : points) {
                sDhop.getData().add(new XYChart.Data<>(p.getPartialPercentFormatted(), p.dhopCount()));
                sPruned.getData().add(new XYChart.Data<>(p.getPartialPercentFormatted(), p.prunedCount()));
                sTotal.getData().add(new XYChart.Data<>(p.getPartialPercentFormatted(), p.totalCandidates()));
            }
            lineChartFig6.getData().addAll(sDhop, sPruned, sTotal);
        }

        // 3. Figure 13: Memory Heap vs minSup (∂)
        if (lineChartFig13 != null) {
            lineChartFig13.getData().clear();
            XYChart.Series<String, Number> sHeap = new XYChart.Series<>();
            sHeap.setName("💾 Bộ Nhớ Peak Heap RAM (MB)");
            for (MinSupSweepPoint p : points) {
                sHeap.getData().add(new XYChart.Data<>(p.getPartialPercentFormatted(), p.peakHeapMb()));
            }
            lineChartFig13.getData().add(sHeap);
        }
    }

    /**
     * Thực thi chạy quét thực nghiệm theo dải minSup (Sweep Benchmark Runner).
     */
    private void executeSweepBenchmark() {
        double start = parseRatioText(txtSweepStart != null ? txtSweepStart.getText() : "5%", 0.05);
        double end = parseRatioText(txtSweepEnd != null ? txtSweepEnd.getText() : "30%", 0.30);
        double step = parseRatioText(txtSweepStep != null ? txtSweepStep.getText() : "5%", 0.05);

        List<Double> steps = MinSupSweepService.generatePartialSteps(start, end, step);
        double f = sliderF.getValue();
        EngineMode mode = cbEngineMode.getValue();
        Path path = resolveDatasetPath();
        long limit = resolveLimit();

        if (btnRunSweep != null) btnRunSweep.setDisable(true);
        if (btnCancelSweep != null) btnCancelSweep.setDisable(false);
        if (progressSweep != null) progressSweep.setProgress(-1);
        if (lblSweepStatus != null) lblSweepStatus.setText("Đang khởi động quá trình quét " + steps.size() + " mốc minSup...");

        Task<List<MinSupSweepPoint>> task = new Task<>() {
            @Override
            protected List<MinSupSweepPoint> call() throws Exception {
                return sweepService.executeSweep(
                        steps, f, mode, tamEngine, tsonService, path, limit,
                        progress -> Platform.runLater(() -> {
                            if (progressSweep != null) {
                                progressSweep.setProgress((double) progress.currentStep() / progress.totalSteps());
                            }
                            if (lblSweepStatus != null) {
                                lblSweepStatus.setText(progress.message());
                            }
                        }),
                        this::isCancelled
                );
            }
        };

        activeSweepTask = task;

        task.setOnSucceeded(e -> {
            activeSweepTask = null;
            if (btnRunSweep != null) btnRunSweep.setDisable(false);
            if (btnCancelSweep != null) btnCancelSweep.setDisable(true);
            if (progressSweep != null) progressSweep.setProgress(1.0);

            List<MinSupSweepPoint> result = task.getValue();
            if (result != null && !result.isEmpty()) {
                sweepData.setAll(result);
                updateSweepCharts(result);
                if (lblSweepCountBadge != null) {
                    lblSweepCountBadge.setText(result.size() + " mốc minSup đã quét");
                }
                if (lblSweepStatus != null) {
                    lblSweepStatus.setText("✅ Đã hoàn tất quét " + result.size() + " mốc minSup thành công!");
                }
                showChartFigure(1); // Mở Figure 11 (Runtime vs minSup)
                switchTableView(1); // Mở Bảng Sweep
            }
        });

        task.setOnCancelled(e -> {
            activeSweepTask = null;
            if (btnRunSweep != null) btnRunSweep.setDisable(false);
            if (btnCancelSweep != null) btnCancelSweep.setDisable(true);
            if (progressSweep != null) progressSweep.setProgress(0.0);
            if (lblSweepStatus != null) lblSweepStatus.setText("⏹ Đã dừng quét theo yêu cầu.");
        });

        task.setOnFailed(e -> {
            activeSweepTask = null;
            if (btnRunSweep != null) btnRunSweep.setDisable(false);
            if (btnCancelSweep != null) btnCancelSweep.setDisable(true);
            if (progressSweep != null) progressSweep.setProgress(0.0);
            Throwable ex = task.getException();
            if (lblSweepStatus != null) {
                lblSweepStatus.setText("❌ Lỗi khi quét: " + (ex != null ? ex.getMessage() : "Không xác định"));
            }
        });

        Thread t = new Thread(task, "DHOPM-MinSup-Sweep-Worker");
        t.setDaemon(true);
        t.start();
    }

    private void cancelSweepBenchmark() {
        if (activeSweepTask != null && activeSweepTask.isRunning()) {
            activeSweepTask.cancel(true);
            if (lblSweepStatus != null) {
                lblSweepStatus.setText("Đang yêu cầu dừng quét...");
            }
        }
    }

    private void updateHistoryCharts() {
        var runs = MiningHistoryManager.getInstance().getHistory();
        if (runs.isEmpty()) {
            if (lineChartHistory != null) lineChartHistory.getData().clear();
            if (barChartDistribution != null) barChartDistribution.getData().clear();
            return;
        }

        // 1. LineChart History
        if (lineChartHistory != null) {
            lineChartHistory.getData().clear();
            XYChart.Series<String, Number> dhopSeries = new XYChart.Series<>();
            dhopSeries.setName("Số mẫu DHOP");

            XYChart.Series<String, Number> prunedSeries = new XYChart.Series<>();
            prunedSeries.setName("Số mẫu Bị cắt tỉa");

            XYChart.Series<String, Number> timeSeries = new XYChart.Series<>();
            timeSeries.setName("Thời gian chạy (ms)");

            for (MiningRunMemento r : runs) {
                String label = "#" + r.getRunId() + " (" + r.getDatasetName() + ")";
                dhopSeries.getData().add(new XYChart.Data<>(label, r.getDhopCount()));
                prunedSeries.getData().add(new XYChart.Data<>(label, r.getPrunedCount()));
                timeSeries.getData().add(new XYChart.Data<>(label, r.getRuntimeMs()));
            }
            lineChartHistory.getData().addAll(dhopSeries, prunedSeries, timeSeries);
        }

        // 2. BarChart Distribution
        if (barChartDistribution != null) {
            barChartDistribution.getData().clear();
            XYChart.Series<String, Number> barDhopSeries = new XYChart.Series<>();
            barDhopSeries.setName("🟢 Mẫu DHOP hợp lệ");

            XYChart.Series<String, Number> barPrunedSeries = new XYChart.Series<>();
            barPrunedSeries.setName("🔴 Mẫu Bị Cắt Tỉa (DUBO)");

            for (MiningRunMemento r : runs) {
                String label = "#" + r.getRunId();
                barDhopSeries.getData().add(new XYChart.Data<>(label, r.getDhopCount()));
                barPrunedSeries.getData().add(new XYChart.Data<>(label, r.getPrunedCount()));
            }
            barChartDistribution.getData().addAll(barDhopSeries, barPrunedSeries);
        }
    }

    private static double parseRatioText(String text, double defaultValue) {
        if (text == null || text.isBlank()) return defaultValue;
        String clean = text.trim().replace("%", "").replace(",", ".").trim();
        try {
            double parsed = Double.parseDouble(clean);
            return parsed > 1.0 ? parsed / 100.0 : parsed;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ── Tab 5: Calculation Log Setup ──────────────────────────────────────────

    private void initCalculationLogView() {
        if (tblCalculationLog == null) return;

        colLogId.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().id()));
        colLogTime.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().timestamp()));
        colLogPhase.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().phase()));
        colLogTarget.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().target()));
        colLogFormula.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().formula()));
        colLogComparison.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().comparison()));
        colLogDecision.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().decision()));

        colLogDecision.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("DHOP")) {
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else if (item.contains("CẮT TỈA")) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else if (item.contains("MỞ RỘNG")) {
                        setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: #7c3aed; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    }
                }
            }
        });

        cbLogPhaseFilter.setItems(FXCollections.observableArrayList(
                "Tất cả các pha",
                "Pha 1: Construct",
                "Pha 2: Reconstruct",
                "Pha 3: Mining DFS",
                "Tson V1"
        ));
        cbLogPhaseFilter.setValue("Tất cả các pha");

        FilteredList<CalculationLogEntry> filteredLogs = new FilteredList<>(CalculationLogger.getInstance().getLogs(), p -> true);

        Runnable applyFilter = () -> {
            String text = txtLogFilter.getText() != null ? txtLogFilter.getText().trim().toLowerCase() : "";
            String phase = cbLogPhaseFilter.getValue();

            filteredLogs.setPredicate(entry -> {
                boolean matchesPhase = phase == null || phase.equals("Tất cả các pha") || entry.phase().contains(phase);
                if (!matchesPhase) return false;

                if (text.isEmpty()) return true;
                return entry.target().toLowerCase().contains(text)
                        || entry.formula().toLowerCase().contains(text)
                        || entry.decision().toLowerCase().contains(text);
            });
        };

        txtLogFilter.textProperty().addListener((obs, oldVal, newVal) -> applyFilter.run());
        cbLogPhaseFilter.setOnAction(e -> applyFilter.run());

        tblCalculationLog.setItems(filteredLogs);

        tblCalculationLog.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && txtLogDetail != null) {
                txtLogDetail.setText(String.format(
                        "Bước #%d [%s] - %s%n" +
                        "• Đối tượng: %s%n" +
                        "• Công thức tính toán chi tiết:%n  %s%n" +
                        "• So sánh với ngưỡng: %s%n" +
                        "• Quyết định thuật toán: %s",
                        newVal.id(), newVal.timestamp(), newVal.phase(),
                        newVal.target(), newVal.formula(), newVal.comparison(), newVal.decision()
                ));
            } else if (txtLogDetail != null) {
                txtLogDetail.clear();
            }
        });

        if (btnClearLog != null) {
            btnClearLog.setOnAction(e -> {
                CalculationLogger.getInstance().clear();
                if (txtLogDetail != null) txtLogDetail.clear();
            });
        }

        if (btnExportLog != null) {
            btnExportLog.setOnAction(e -> {
                String fullLog = CalculationLogger.getInstance().exportToString();
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(fullLog);
                clipboard.setContent(content);

                new Alert(Alert.AlertType.INFORMATION,
                        "Đã sao chép " + CalculationLogger.getInstance().getLogs().size() +
                        " dòng nhật ký tính toán vào Clipboard máy tính!").show();
            });
        }
    }

    // ── Tson Actions (Lệnh CLI biến thành Nút Bấm Đồ Họa) ─────────────────────

    private boolean checkDenseRiskConfirmation(Path path, long limit) {
        if (limit <= 0 || limit > 100) return true;
        String name = path.getFileName().toString().toLowerCase();
        boolean isDense = name.contains("connect") || name.contains("chess") || name.contains("pumsb");
        if (!isDense) return true;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cảnh báo tập dữ liệu dày đặc (Dense Dataset)");
        alert.setHeaderText("Nguy cơ bùng nổ tổ hợp với giới hạn " + limit + " giao dịch!");
        alert.setContentText(
                "Tập dữ liệu '" + path.getFileName() + "' rất dày đặc (37 - 74 items / giao dịch).\n" +
                "Khi đặt giới hạn quá nhỏ (" + limit + " giao dịch), các items xuất hiện trùng lặp trong toàn bộ các dòng, " +
                "dẫn đến hơn 274 TỶ TỔ HỢP (2^38 mẫu) khiến thuật toán duyệt nhánh rất lâu!\n\n" +
                "👉 Khuyến nghị:\n" +
                "• Dùng nút '📊 Thống kê (Inspect)' để xem nhanh phân tích đặc trưng.\n" +
                "• Hoặc thử nghiệm với tập chuẩn 'default.dat (8 tx)' hoặc tập thưa 'retail.dat'.\n" +
                "• Hoặc chọn 'Toàn bộ (0)' để áp dụng đúng ngưỡng minSup chuẩn của FIMI.\n\n" +
                "Bấm OK để tiếp tục chạy, hoặc Cancel để chọn lại cấu hình."
        );
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Nút "🚀 Khai Phá (Mine)": Chạy mining bằng engine Tson trên dataset đã chọn.
     */
    private void executeTsonMine() {
        Path path = resolveDatasetPath();
        double partial = sliderMinSup.getValue();
        double f = sliderF.getValue();
        long limit = resolveLimit();

        if (!checkDenseRiskConfirmation(path, limit)) {
            return;
        }

        if (cbEngineMode.getValue() != EngineMode.TSON_V1_STANDARD) {
            cbEngineMode.setValue(EngineMode.TSON_V1_STANDARD);
        }

        // Mở Tab 4 và kích hoạt View 1 (Bảng mẫu)
        mainTabPane.getSelectionModel().select(3);
        showTsonView(1);

        setButtonsDisable(true);
        progressMining.setProgress(-1);
        if (lblMiningEta != null) lblMiningEta.setText("Đang chuẩn bị...");
        if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("⏳ Đang chuẩn bị...");
        lblKpiTime.setText("Đang chạy...");

        Task<MineExecutionResult> task = new Task<>() {
            @Override
            protected MineExecutionResult call() throws Exception {
                return tsonService.runMine(
                        path, partial, f, 4, limit,
                        phaseMsg -> Platform.runLater(() -> lblLastPhase.setText(phaseMsg)),
                        info -> Platform.runLater(() -> {
                            progressMining.setProgress(info.fraction());
                            String etaStr = info.formatEtaStatus();
                            if (lblMiningEta != null) lblMiningEta.setText(etaStr);
                            if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("⏳ " + etaStr);
                            lblKpiTime.setText(etaStr);
                        }),
                        engine -> activeMiningEngine.set(engine)
                );
            }
        };

        activeTsonTask = task;

        task.setOnSucceeded(e -> {
            activeTsonTask = null;
            activeMiningEngine.set(null);
            setButtonsDisable(false);
            progressMining.setProgress(1.0);
            if (lblMiningEta != null) lblMiningEta.setText("Hoàn tất 100%");
            if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("✅ Hoàn tất");
            MineExecutionResult res = task.getValue();

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

            // Cập nhật TableView Tab 4 và TableView Tab 2
            activeTotalTransactions = res.totalTransactions();
            tsonPatternsData.setAll(res.uiPatterns());
            tableData.setAll(res.uiPatterns());
            if (tableResults != null) tableResults.refresh();
            if (tblTsonPatterns != null) tblTsonPatterns.refresh();

            lblDhopCount.setText(res.patternCount() + " mẫu");
            lblVisitedCount.setText(res.patternCount() + " mẫu");
            lblTransCount.setText(String.valueOf(res.totalTransactions()));
            lblTL.setText("T" + res.rawResult().lastTid());

            updateChartFromPatterns(res.uiPatterns(), res.rawResult().minSup());

            // Lưu Memento lịch sử khai phá
            MiningHistoryManager.getInstance().recordRun(
                    "Tson V1 Standard",
                    path.getFileName().toString() + (limit > 0 ? " (" + limit + " tx)" : ""),
                    f, partial, res.rawResult().minSup(), (int) res.totalTransactions(),
                    res.patternCount(), 0, res.patternCount(),
                    res.totalMs(), res.peakHeapMb(), res.uiPatterns()
            );

            showTsonView(1);
        });

        task.setOnCancelled(e -> {
            activeTsonTask = null;
            activeMiningEngine.set(null);
            setButtonsDisable(false);
            progressMining.setProgress(0);
            if (lblMiningEta != null) lblMiningEta.setText("Đã dừng");
            if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("🛑 Đã dừng");
            lblLastPhase.setText("Đã hủy khai phá");
            lblKpiTime.setText("Đã dừng");
        });

        task.setOnFailed(e -> {
            activeTsonTask = null;
            activeMiningEngine.set(null);
            setButtonsDisable(false);
            progressMining.setProgress(0);
            Throwable err = task.getException();
            if (err instanceof java.util.concurrent.CancellationException ||
                (err.getMessage() != null && (err.getMessage().contains("dừng") || err.getMessage().contains("interrupted")))) {
                if (lblMiningEta != null) lblMiningEta.setText("Đã dừng");
                if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("🛑 Đã dừng");
                lblLastPhase.setText("Đã dừng khai phá");
                lblKpiTime.setText("Đã dừng");
                return;
            }
            if (lblMiningEta != null) lblMiningEta.setText("Lỗi");
            if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("❌ Lỗi");
            err.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Lỗi khi chạy Tson Mine: " + err.getMessage()).show();
        });

        Thread worker = new Thread(task, "TsonMineWorker");
        activeTsonThread = worker;
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Nút "📊 Thống kê (Inspect)": Phân tích tập dữ liệu không cần mining.
     */
    private void executeTsonInspect() {
        Path path = resolveDatasetPath();
        double partial = sliderMinSup.getValue();
        long limit = resolveLimit();

        mainTabPane.getSelectionModel().select(3);
        setButtonsDisable(true);
        if (lblMiningEta != null) lblMiningEta.setText("Đang phân tích...");
        if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("⏳ Đang phân tích...");

        Task<InspectReport> task = new Task<>() {
            @Override
            protected InspectReport call() throws Exception {
                return tsonService.runInspect(path, partial, limit, 15);
            }
        };

        activeTsonTask = task;

        task.setOnCancelled(e -> {
            activeTsonTask = null;
            setButtonsDisable(false);
            if (lblMiningEta != null) lblMiningEta.setText("Đã dừng inspect");
            if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("🛑 Đã dừng inspect");
        });

        task.setOnSucceeded(e -> {
            activeTsonTask = null;
            setButtonsDisable(false);
            if (lblMiningEta != null) lblMiningEta.setText("Hoàn tất thống kê");
            if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("✅ Hoàn tất thống kê");
            InspectReport rep = task.getValue();

            // Cập nhật thẻ tổng quan Inspect
            lblInspectFile.setText(path.getFileName().toString());
            lblInspectTx.setText(String.format("%,d giao dịch %s", rep.totalTransactions(), limit > 0 ? "(limit=" + limit + ")" : "(toàn bộ)"));
            lblInspectTL.setText("TID = " + rep.lastTid());
            lblInspectItems.setText(String.format("%,d mặt hàng khác biệt", rep.distinctItems()));
            lblInspectAvgLen.setText(String.format("%.2f mục / giao dịch", rep.avgLength()));
            lblInspectMaxLen.setText(rep.maxLength() + " mục");

            // Nạp bảng Top Items
            inspectTopItemsData.setAll(rep.toTopItemStats());

            // Cập nhật các label thống kê ở sidebar
            lblTransCount.setText(String.format("%,d", rep.totalTransactions()));
            lblTL.setText("T" + rep.lastTid());
            lblKpiTrans.setText(String.format("Tổng giao dịch: %,d", rep.totalTransactions()));
            lblKpiMinSup.setText(String.format("minSup = %.2f", rep.minSupAbsolute()));

            showTsonView(2);
        });

        task.setOnFailed(e -> {
            activeTsonTask = null;
            setButtonsDisable(false);
            Throwable err = task.getException();
            if (err instanceof java.util.concurrent.CancellationException) {
                if (lblMiningEta != null) lblMiningEta.setText("Đã dừng inspect");
                return;
            }
            new Alert(Alert.AlertType.ERROR, "Lỗi khi phân tích tập dữ liệu: " + err.getMessage()).show();
        });

        Thread worker = new Thread(task, "TsonInspectWorker");
        activeTsonThread = worker;
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Nút "🏆 Golden TC1-TC8": Chạy bộ kiểm thử vàng nghiệm thu của Tson.
     */
    private void executeTsonGolden() {
        mainTabPane.getSelectionModel().select(3);
        setButtonsDisable(true);
        progressMining.setProgress(-1);
        if (lblMiningEta != null) lblMiningEta.setText("Đang chạy 8 test...");
        if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("⏳ Đang chạy Golden TestKit...");

        Task<List<GoldenRow>> task = new Task<>() {
            @Override
            protected List<GoldenRow> call() {
                return tsonService.runGoldenTestKitRows();
            }
        };

        activeTsonTask = task;

        task.setOnCancelled(e -> {
            activeTsonTask = null;
            setButtonsDisable(false);
            progressMining.setProgress(0);
            if (lblMiningEta != null) lblMiningEta.setText("Đã dừng TestKit");
            if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("🛑 Đã dừng TestKit");
        });

        task.setOnSucceeded(e -> {
            activeTsonTask = null;
            setButtonsDisable(false);
            progressMining.setProgress(1.0);
            if (lblMiningEta != null) lblMiningEta.setText("TestKit 100% hoàn tất");
            if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("🏆 Golden TC1-8 Hoàn tất");
            goldenRowsData.setAll(task.getValue());
            showTsonView(3);
        });

        task.setOnFailed(e -> {
            activeTsonTask = null;
            setButtonsDisable(false);
            progressMining.setProgress(0);
            Throwable err = task.getException();
            if (err instanceof java.util.concurrent.CancellationException) {
                return;
            }
            new Alert(Alert.AlertType.ERROR, "Lỗi khi chạy Golden TestKit: " + err.getMessage()).show();
        });

        Thread worker = new Thread(task, "TsonGoldenWorker");
        activeTsonThread = worker;
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Nút "🔬 Top DO (Detail)": Debug chi tiết các mẫu DO cao nhất.
     */
    private void executeTsonDetail() {
        Path path = resolveDatasetPath();
        double partial = sliderMinSup.getValue();
        double f = sliderF.getValue();
        long limit = resolveLimit();

        if (!checkDenseRiskConfirmation(path, limit)) {
            return;
        }

        mainTabPane.getSelectionModel().select(3);
        setButtonsDisable(true);
        if (lblMiningEta != null) lblMiningEta.setText("Đang chuẩn bị...");
        if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("⏳ Đang trích xuất Top DO...");
        lblKpiTime.setText("Đang chạy...");

        Task<MineExecutionResult> task = new Task<>() {
            @Override
            protected MineExecutionResult call() throws Exception {
                return tsonService.runMine(
                        path, partial, f, 4, limit,
                        phaseMsg -> Platform.runLater(() -> lblLastPhase.setText(phaseMsg)),
                        info -> Platform.runLater(() -> {
                            progressMining.setProgress(info.fraction());
                            String etaStr = info.formatEtaStatus();
                            if (lblMiningEta != null) lblMiningEta.setText(etaStr);
                            if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("⏳ " + etaStr);
                            lblKpiTime.setText(etaStr);
                        }),
                        engine -> activeMiningEngine.set(engine)
                );
            }
        };

        activeTsonTask = task;

        task.setOnCancelled(e -> {
            activeTsonTask = null;
            activeMiningEngine.set(null);
            setButtonsDisable(false);
            if (lblMiningEta != null) lblMiningEta.setText("Đã dừng");
            if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("🛑 Đã dừng");
        });

        task.setOnSucceeded(e -> {
            activeTsonTask = null;
            activeMiningEngine.set(null);
            setButtonsDisable(false);
            if (lblMiningEta != null) lblMiningEta.setText("Hoàn tất");
            if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("✅ Hoàn tất");
            MineExecutionResult res = task.getValue();

            // Cập nhật TableView Tab 4 và Tab 2
            tsonPatternsData.setAll(res.uiPatterns());
            tableData.setAll(res.uiPatterns());

            // Cập nhật KPI Cards
            lblKpiTime.setText(String.format("%,d ms", res.totalMs()));
            lblKpiPhases.setText(String.format("Constr: %dms | Reconst: %dms | Mine: %dms",
                    res.constrMs(), res.reconstMs(), res.miningMs()));
            lblKpiHeap.setText(String.format("%.1f MB", res.peakHeapMb()));
            lblKpiPatterns.setText(String.format("%,d mẫu", res.patternCount()));
            lblKpiMinSup.setText(String.format("minSup = %.2f", res.rawResult().minSup()));

            // Lưu Memento
            MiningHistoryManager.getInstance().recordRun(
                    "Tson Top DO",
                    path.getFileName().toString() + (limit > 0 ? " (" + limit + " tx)" : ""),
                    f, partial, res.rawResult().minSup(), (int) res.totalTransactions(),
                    res.patternCount(), 0, res.patternCount(),
                    res.totalMs(), res.peakHeapMb(), res.uiPatterns()
            );

            showTsonView(1);
        });

        task.setOnFailed(e -> {
            activeTsonTask = null;
            activeMiningEngine.set(null);
            setButtonsDisable(false);
            Throwable err = task.getException();
            if (err instanceof java.util.concurrent.CancellationException) {
                return;
            }
            new Alert(Alert.AlertType.ERROR, "Lỗi khi lọc chi tiết mẫu: " + err.getMessage()).show();
        });

        Thread worker = new Thread(task, "TsonDetailWorker");
        activeTsonThread = worker;
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Dừng ngay lập tức tác vụ khai phá hoặc nạp dữ liệu ở chế độ Tson.
     */
    private void handleStopMining() {
        if (lblMiningEta != null) lblMiningEta.setText("Đang dừng...");
        if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("🛑 Đang dừng...");
        lblLastPhase.setText("Yêu cầu dừng tác vụ...");

        // 1. Đóng MiningEngine (đóng ThreadPool của Tson ngay lập tức)
        dhopm.v1.engine.MiningEngine engine = activeMiningEngine.getAndSet(null);
        if (engine != null) {
            try {
                engine.close();
            } catch (Exception ignored) {}
        }

        // 2. Hủy Task JavaFX
        if (activeTsonTask != null && activeTsonTask.isRunning()) {
            activeTsonTask.cancel(true);
        }

        // 3. Interrupt worker thread
        if (activeTsonThread != null && activeTsonThread.isAlive()) {
            activeTsonThread.interrupt();
        }

        // 4. Hủy bridgeEngine nếu đang chạy
        if (bridgeEngine != null) {
            bridgeEngine.cancel();
        }

        setButtonsDisable(false);
        progressMining.setProgress(0);
        if (lblMiningEta != null) lblMiningEta.setText("Đã dừng");
        if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("🛑 Đã dừng");
        lblLastPhase.setText("Đã dừng bởi người dùng");
    }

    private void setButtonsDisable(boolean disable) {
        btnTsonMine.setDisable(disable);
        btnTsonInspect.setDisable(disable);
        btnTsonDetail.setDisable(disable);
        btnTsonGolden.setDisable(disable);
        btnStartStream.setDisable(disable);
        if (btnTsonStop != null) {
            btnTsonStop.setDisable(!disable);
        }
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

        bridgeEngine.onMiningProgressInfo(info -> {
            progressMining.setProgress(info.fraction());
            String eta = info.formatEtaStatus();
            if (lblMiningEta != null) lblMiningEta.setText(eta);
            if (lblTab4EtaStatus != null) lblTab4EtaStatus.setText("⏳ " + eta);
        });
    }

    private void initTamEngine() {
        tamEngine = new DHOPMEngine();
        streamSimulator = new StreamSimulator(tamEngine);
        tamEngine.addStreamListener((newBatch, currentTL) -> Platform.runLater(this::updateUI));
        buildBridgeEngine();
    }

    private void initTableView() {
        colPattern.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().pattern()));
        colSupport.setCellValueFactory(data ->
                new SimpleStringProperty(formatSupportWithPercent(data.getValue().support())));
        colSupport.setComparator((s1, s2) -> Integer.compare(extractSupportInt(s1), extractSupportInt(s2)));
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
            lblFValue.setText(String.format(java.util.Locale.US, "%.2f", f));
            recalculateAndRender();
        });

        sliderMinSup.valueProperty().addListener((obs, oldVal, newVal) -> {
            double ratio = Math.round(newVal.doubleValue() * 1000.0) / 1000.0;
            if (txtMinSupRatio != null && !txtMinSupRatio.isFocused()) {
                txtMinSupRatio.setText(String.format(java.util.Locale.US, "%.1f%%", ratio * 100.0));
            }
            if (lblMinSupValue != null) {
                lblMinSupValue.setText(String.format(java.util.Locale.US, "%.1f%%", ratio * 100.0));
            }
            recalculateAndRender();
        });

        if (txtMinSupRatio != null) {
            txtMinSupRatio.setOnAction(e -> handleMinSupTextCommit());
            txtMinSupRatio.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    handleMinSupTextCommit();
                }
            });
        }

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

    private void handleMinSupTextCommit() {
        if (txtMinSupRatio == null) return;
        String text = txtMinSupRatio.getText();
        if (text == null || text.isBlank()) return;
        String clean = text.trim().replace("%", "").replace(",", ".").trim();
        try {
            double parsed = Double.parseDouble(clean);
            double ratio;
            if (parsed > 1.0) {
                ratio = parsed / 100.0;
            } else {
                ratio = parsed;
            }
            ratio = Math.max(0.0001, Math.min(1.0, ratio));

            if (ratio > sliderMinSup.getMax()) {
                sliderMinSup.setMax(Math.ceil(ratio * 10.0) / 10.0);
            }
            if (ratio < sliderMinSup.getMin()) {
                sliderMinSup.setMin(0.0);
            }
            sliderMinSup.setValue(ratio);
            txtMinSupRatio.setText(String.format(java.util.Locale.US, "%.1f%%", ratio * 100.0));
            recalculateAndRender();
        } catch (NumberFormatException ignored) {
            txtMinSupRatio.setText(String.format(java.util.Locale.US, "%.1f%%", sliderMinSup.getValue() * 100.0));
        }
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
        if (txtMinSupRatio != null) txtMinSupRatio.setText("15.0%");
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

        lblAbsoluteMinSup.setText(String.format(java.util.Locale.US, "%.2f", minSup));

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
                : prunedCount + " mẫu (" + String.format(java.util.Locale.US, "%.1f%%", ((double) prunedCount / results.size()) * 100) + ")";
        lblPrunedCount.setText(prunedText);

        activeTotalTransactions = totalTrans;
        tableData.setAll(results);
        tsonPatternsData.setAll(results);
        if (tableResults != null) tableResults.refresh();
        if (tblTsonPatterns != null) tblTsonPatterns.refresh();
        updateChart(results, f, minSup);

        // Lưu Memento lịch sử mining
        long simulatedTimeMs = Math.max(1, (long) (results.size() * 0.15));
        MiningHistoryManager.getInstance().recordRun(
                currentMode.getDisplayName(),
                "default.dat (Stream " + totalTrans + " tx)",
                f, ratio, minSup, totalTrans,
                dhops.size(), (int) prunedCount, results.size(),
                simulatedTimeMs, 14.5, results
        );
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
