package vn.edu.dlu.dhopm.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import vn.edu.dlu.dhopm.core.DUBOCalculator;
import vn.edu.dlu.dhopm.model.DHONode;
import vn.edu.dlu.dhopm.model.Entry;

/**
 * Custom UI Component bieu dien mot Node trong Global DHO-List.
 */
public class DHONodeCard extends VBox {

    public DHONodeCard(DHONode node, double f, int TL, double minSup) {
        setPadding(new Insets(10));
        setSpacing(6);
        setPrefWidth(240);
        getStyleClass().add("dho-node-card");

        double dubo = DUBOCalculator.calculate(node.getEntries(), f, TL);
        boolean isDhop = node.getDoValue() >= minSup - 1e-9;
        boolean isPruned = dubo < minSup - 1e-9;

        if (isDhop) {
            getStyleClass().add("dho-node-dhop");
        } else if (isPruned) {
            getStyleClass().add("dho-node-pruned");
        }

        // Header: Item Name & Support Badge
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label("Item " + node.getItemName());
        nameLabel.getStyleClass().add("node-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label supBadge = new Label("Sup: " + node.getSupport());
        supBadge.getStyleClass().add("badge-sup");

        header.getChildren().addAll(nameLabel, spacer, supBadge);

        // DO Row
        HBox doRow = new HBox();
        Label doTitle = new Label("DO(" + node.getItemName() + "):");
        doTitle.getStyleClass().add("stat-title");
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Label doVal = new Label(String.format("%.4f", node.getDoValue()));
        doVal.getStyleClass().add(isDhop ? "stat-val-dhop" : "stat-val");
        doRow.getChildren().addAll(doTitle, spacer1, doVal);

        // DUBO Row
        HBox duboRow = new HBox();
        Label duboTitle = new Label("DUBO(" + node.getItemName() + "):");
        duboTitle.getStyleClass().add("stat-title");
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        Label duboVal = new Label(String.format("%.4f", dubo));
        duboVal.getStyleClass().add(isPruned ? "stat-val-pruned" : "stat-val");
        duboRow.getChildren().addAll(duboTitle, spacer2, duboVal);

        // Entries Row
        StringBuilder sb = new StringBuilder();
        for (Entry e : node.getEntries()) {
            sb.append("<").append(e.tid()).append(",").append(e.tlen()).append("> ");
        }
        Label entriesLabel = new Label("Entries: " + sb.toString().trim());
        entriesLabel.getStyleClass().add("entries-label");
        entriesLabel.setWrapText(true);

        getChildren().addAll(header, doRow, duboRow, entriesLabel);
    }
}
