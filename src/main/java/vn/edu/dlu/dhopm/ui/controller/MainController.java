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
 */
public class MainController implements Initializable {

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

    // Backend engine & data
    private DHOPMEngine engine;
    private StreamSimulator streamSimulator;
    private final ObservableList<PatternResult> tableData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initEngine();
        initTableView();
        initEventHandlers();
        loadInitialPaperData();
    }

    private void initEngine() {
        engine = new DHOPMEngine();
        streamSimulator = new StreamSimulator(engine);

        // Lang nghe su kien khi co batch moi qua stream
        engine.addStreamListener((newBatch, currentTL) -> Platform.runLater(() -> {
            updateUI();
        }));
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

        // Format mau sac dong theo trang thai DHOP / Pruned
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
            streamSimulator.start(1500); // bom moi 1.5 giay
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
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đã bơm hết toàn bộ giao dịch trong luồng mẫu!");
                alert.show();
            }
        });

        btnReset.setOnAction(e -> loadInitialPaperData());
    }

    /**
     * Nap 8 giao dich goc cua bai bao de khoi dau.
     */
    private void loadInitialPaperData() {
        streamSimulator.stop();
        btnStartStream.setDisable(false);
        btnPauseStream.setDisable(true);
        lblStreamStatus.setText("Sẵn sàng");
        lblStreamStatus.setStyle("-fx-text-fill: #2563eb;");

        engine.reset();
        List<Transaction> paperData = DatasetLoader.getPaperDataset();
        engine.phase1_constructOrUpdate(paperData);

        // Nap hang doi cac giao dich tiep theo cho stream
        streamSimulator.setStreamQueue(DatasetLoader.getSyntheticStreamDataset());

        sliderF.setValue(0.90);
        sliderMinSup.setValue(0.15);

        updateUI();
    }

    /**
     * Tinh toan lai toan bo khi f hoac minSup thay doi, hoac khi co du lieu moi.
     */
    private void recalculateAndRender() {
        double f = sliderF.getValue();
        double ratio = sliderMinSup.getValue();
        int totalTrans = engine.getAllTransactions().size();
        double minSup = totalTrans * ratio;

        lblAbsoluteMinSup.setText(String.format("%.2f", minSup));

        // Khai pha DFS
        int TL = engine.getCurrentTL();
        List<PatternResult> results = engine.phase3_mine(minSup, f, TL);
        List<PatternResult> dhops = results.stream().filter(PatternResult::isDHOP).toList();

        // Cap nhat cac Label thong ke
        lblTL.setText("T" + TL);
        lblTransCount.setText(String.valueOf(totalTrans));
        lblDhopCount.setText(dhops.size() + " mẫu");
        lblVisitedCount.setText(results.size() + " mẫu");

        long prunedCount = results.stream().filter(PatternResult::isPruned).count();
        lblPrunedCount.setText(prunedCount + " mẫu (" + String.format("%.1f%%", ((double) prunedCount / results.size()) * 100) + ")");

        // Cap nhat thu tu sap xep Support
        List<DHONode> sortedNodes = engine.getGlobalList().getSortedNodes();
        StringBuilder orderSb = new StringBuilder();
        for (int i = 0; i < sortedNodes.size(); i++) {
            orderSb.append(sortedNodes.get(i).getItemName())
                   .append(" (S=").append(sortedNodes.get(i).getSupport()).append(")");
            if (i < sortedNodes.size() - 1) {
                orderSb.append(" ≺ ");
            }
        }
        lblSortOrder.setText(orderSb.toString());

        // 1. Ve danh sach DHO-List Nodes
        dhoNodesPane.getChildren().clear();
        for (DHONode node : sortedNodes) {
            dhoNodesPane.getChildren().add(new DHONodeCard(node, f, TL, minSup));
        }

        // 2. Cap nhat TableView
        tableData.setAll(results);

        // 3. Cap nhat Line Chart DO
        updateChart(results, f, minSup);
    }

    private void updateChart(List<PatternResult> results, double f, double minSup) {
        lineChartDO.getData().clear();

        // Series 1: DO cua cac Item don le theo thu tu sap xep
        XYChart.Series<String, Number> itemSeries = new XYChart.Series<>();
        itemSeries.setName("DO(Items đơn lẻ)");

        for (DHONode node : engine.getGlobalList().getSortedNodes()) {
            itemSeries.getData().add(new XYChart.Data<>(node.getItemName(), node.getDoValue()));
        }

        // Series 2: Nguong minSup
        XYChart.Series<String, Number> minSupSeries = new XYChart.Series<>();
        minSupSeries.setName("Ngưỡng minSup (" + String.format("%.2f", minSup) + ")");
        for (DHONode node : engine.getGlobalList().getSortedNodes()) {
            minSupSeries.getData().add(new XYChart.Data<>(node.getItemName(), minSup));
        }

        lineChartDO.getData().addAll(itemSeries, minSupSeries);
    }

    public void updateUI() {
        recalculateAndRender();
    }
}
