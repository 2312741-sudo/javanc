package vn.edu.dlu.dhopm.ui.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import vn.edu.dlu.dhopm.bridge.BridgeEngine;
import vn.edu.dlu.dhopm.bridge.EngineFactory;
import vn.edu.dlu.dhopm.bridge.EngineMode;
import vn.edu.dlu.dhopm.core.DatasetLoader;
import vn.edu.dlu.dhopm.core.DHOPMEngine;
import vn.edu.dlu.dhopm.core.StreamSimulator;
import vn.edu.dlu.dhopm.model.DHONode;
import vn.edu.dlu.dhopm.model.PatternResult;
import vn.edu.dlu.dhopm.model.Transaction;
import vn.edu.dlu.dhopm.ui.component.DHONodeCard;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller dieu khien toan bo giao dien JavaFX cho DHOPM Visualizer.
 * Version 2.0: Tích hợp Bridge Engine – hỗ trợ chuyển đổi giữa
 * Tâm Simulation Engine và Tson V1 Standard Engine tại runtime.
 */
public class MainController implements Initializable {

    // ── Engine Selector ──────────────────────────────────────────────────────
    @FXML private ComboBox<EngineMode> cbEngineMode;
    @FXML private Label lblEngineName;
    @FXML private Label lblLastPhase;
    @FXML private ProgressBar progressMining;

    // Controls & Sliders
    @FXML private Slider sliderF;
    @FXML private Slider sliderMinSup;
    @FXML private Label lblFValue;
    @FXML private Label lblMinSupValue;
    @FXML private Label lblAbsoluteMinSup;

    // Stream & Engine Status Labels
    @FXML private Label lblTL;
    @FXML private Label lblTransCount;
    @FXML private Label lblDhopCount;
    @FXML private Label lblVisitedCount;
    @FXML private Label lblPrunedCount;
    @FXML private Label lblStreamStatus;
    @FXML private Label lblSortOrder;

    // Action Buttons
    @FXML private Button btnStartStream;
    @FXML private Button btnPauseStream;
    @FXML private Button btnStepStream;
    @FXML private Button btnReset;

    // Visual Views
    @FXML private FlowPane dhoNodesPane;
    @FXML private TableView<PatternResult> tableResults;
    @FXML private TableColumn<PatternResult, String> colPattern;
    @FXML private TableColumn<PatternResult, Number> colSupport;
    @FXML private TableColumn<PatternResult, String> colDO;
    @FXML private TableColumn<PatternResult, String> colDUBO;
    @FXML private TableColumn<PatternResult, String> colStatus;
    @FXML private TableColumn<PatternResult, String> colTransactions;

    @FXML private LineChart<String, Number> lineChartDO;

    // ── Backend ──────────────────────────────────────────────────────────────
    /** Engine Tâm (dùng cho step-by-step animation + DHO-List visualization) */
    private DHOPMEngine tamEngine;
    private StreamSimulator streamSimulator;

    /** Bridge Engine hiện tại (có thể là Tâm hoặc Tson) */
    private BridgeEngine bridgeEngine;

    private final ObservableList<PatternResult> tableData = FXCollections.observableArrayList();
    private EngineMode currentMode = EngineMode.TAM_SIMULATION;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initEngineSelectorPanel();
        initTamEngine();
        initTableView();
        initEventHandlers();
        loadInitialPaperData();
    }

    // ── Engine Selector ──────────────────────────────────────────────────────

    private void initEngineSelectorPanel() {
        cbEngineMode.setItems(FXCollections.observableArrayList(EngineMode.values()));
        cbEngineMode.setValue(EngineMode.TAM_SIMULATION);

        // Đổi engine khi chọn từ ComboBox
        cbEngineMode.setOnAction(e -> switchEngine(cbEngineMode.getValue()));

        // Format tên hiển thị
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

    private void switchEngine(EngineMode mode) {
        if (mode == null || mode == currentMode) return;
        currentMode = mode;

        // Dừng stream nếu đang chạy
        streamSimulator.stop();
        btnStartStream.setDisable(false);
        btnPauseStream.setDisable(true);
        lblStreamStatus.setText("Sẵn sàng");
        lblStreamStatus.setStyle("-fx-text-fill: #2563eb;");

        // Tạo BridgeEngine mới theo mode được chọn
        buildBridgeEngine();

        // Nạp lại dữ liệu gốc vào engine mới
        loadInitialPaperData();

        // Cập nhật nhãn engine
        lblEngineName.setText("⚡ " + bridgeEngine.engineName());

        // Bật/tắt nút step (chỉ Tâm mới có animation step-by-step)
        boolean isTam = mode == EngineMode.TAM_SIMULATION;
        btnStartStream.setDisable(!isTam);
        btnStepStream.setDisable(!isTam);
        lblSortOrder.setVisible(isTam);
    }

    private void buildBridgeEngine() {
        double f = sliderF != null ? sliderF.getValue() : 0.9;
        double ratio = sliderMinSup != null ? sliderMinSup.getValue() : 0.15;
        bridgeEngine = EngineFactory.create(currentMode, ratio, f);

        // Gắn Phase Observer: hiển thị pha + thời gian lên lblLastPhase
        bridgeEngine.onPhaseUpdate((phase, ms) ->
                lblLastPhase.setText(phase + "  (" + String.format("%.2f", ms) + " ms)")
        );

        // Gắn Progress Observer: cập nhật ProgressBar
        bridgeEngine.onMiningProgress(fraction ->
                progressMining.setProgress(fraction)
        );
    }

    // ── Tâm Engine (step-by-step, DHO-List visualization) ────────────────────

    private void initTamEngine() {
        tamEngine = new DHOPMEngine();
        streamSimulator = new StreamSimulator(tamEngine);

        tamEngine.addStreamListener((newBatch, currentTL) ->
                Platform.runLater(this::updateUI));

        // Tạo bridge engine mặc định (Tâm mode)
        buildBridgeEngine();
    }

    // ── TableView ────────────────────────────────────────────────────────────

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

        // Format màu sắc dòng theo trạng thái DHOP / Pruned
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

    // ── Event Handlers ────────────────────────────────────────────────────────

    private void initEventHandlers() {
        // Slider f
        sliderF.valueProperty().addListener((obs, oldVal, newVal) -> {
            double f = Math.round(newVal.doubleValue() * 100.0) / 100.0;
            lblFValue.setText(String.format("%.2f", f));
            recalculateAndRender();
        });

        // Slider minSup ratio
        sliderMinSup.valueProperty().addListener((obs, oldVal, newVal) -> {
            double ratio = Math.round(newVal.doubleValue() * 100.0) / 100.0;
            lblMinSupValue.setText(String.format("%.0f%%", ratio * 100));
            recalculateAndRender();
        });

        // Buttons
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

    // ── Data Loading ──────────────────────────────────────────────────────────

    private void loadInitialPaperData() {
        streamSimulator.stop();
        btnPauseStream.setDisable(true);
        lblStreamStatus.setText("Sẵn sàng");
        lblStreamStatus.setStyle("-fx-text-fill: #2563eb;");

        // Reset cả 2 engine
        tamEngine.reset();
        if (bridgeEngine != null) bridgeEngine.reset();

        List<Transaction> paperData = DatasetLoader.getPaperDataset();

        // Nạp vào Tâm engine (để DHO-List visualization, stream)
        tamEngine.phase1_constructOrUpdate(paperData);
        streamSimulator.setStreamQueue(DatasetLoader.getSyntheticStreamDataset());

        // Nạp vào Bridge engine (để mining)
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

    // ── Render ────────────────────────────────────────────────────────────────

    private void recalculateAndRender() {
        double f = sliderF.getValue();
        double ratio = sliderMinSup.getValue();
        int totalTrans = tamEngine.getAllTransactions().size();
        double minSup = totalTrans * ratio;

        lblAbsoluteMinSup.setText(String.format("%.2f", minSup));

        List<PatternResult> results;
        if (currentMode == EngineMode.TAM_SIMULATION || bridgeEngine == null) {
            // ── Chế độ Tâm: khai phá trực tiếp qua DHOPMEngine (đầy đủ DFS tree)
            int TL = tamEngine.getCurrentTL();
            results = tamEngine.phase3_mine(minSup, f, TL);

            lblTL.setText("T" + TL);
            updateDHONodes(f, TL, minSup);
        } else {
            // ── Chế độ Tson: dùng Bridge engine
            bridgeEngine.reset();
            bridgeEngine.feedTransactions(tamEngine.getAllTransactions());
            results = bridgeEngine.executeAndGetResults();

            lblTL.setText("T" + bridgeEngine.lastTid());
            // DHO-List nodes vẫn từ Tâm engine để visualize
            updateDHONodes(f, tamEngine.getCurrentTL(), minSup);
        }

        // Thống kê
        List<PatternResult> dhops = results.stream().filter(PatternResult::isDHOP).toList();
        lblTransCount.setText(String.valueOf(totalTrans));
        lblDhopCount.setText(dhops.size() + " mẫu");
        lblVisitedCount.setText(results.size() + " mẫu");

        long prunedCount = results.stream().filter(PatternResult::isPruned).count();
        String prunedText = results.isEmpty() ? "0 mẫu"
                : prunedCount + " mẫu (" + String.format("%.1f%%", ((double) prunedCount / results.size()) * 100) + ")";
        lblPrunedCount.setText(prunedText);

        // TableView + Chart
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

    public void updateUI() {
        recalculateAndRender();
    }
}
