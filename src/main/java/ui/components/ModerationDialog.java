package ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import model.ModerationReport;

import java.util.List;

public final class ModerationDialog {
    private ModerationDialog() {
    }

    public static void show(ModerationReport report) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Moderation Analysis");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        VBox summary = new VBox(6);
        summary.getChildren().addAll(
                new Label("Decision: " + safe(report.getDecision())),
                new Label(String.format("Toxicity: %.3f", report.getToxicity())),
                new Label(String.format("Relevance: %.3f  (%s)", report.getRelevance(), safe(report.getPredictedCategory()))),
                new Label(String.format("Duplicate Score: %.3f", report.getDuplicateScore())),
                new Label(String.format("Quality Score: %.3f", report.getQualityScore())),
                new Label(String.format("Latency: total=%dms, perspective=%dms, python=%dms",
                        report.getTotalLatencyMs(),
                        report.getPerspectiveLatencyMs(),
                        report.getPythonLatencyMs())));

        TitledPane reasonsPane = new TitledPane("Reasons", textPane(toBullets(report.getReasons()), 120));
        reasonsPane.setExpanded(true);

        TitledPane perspectivePane = new TitledPane(
                "Perspective Raw Output",
                textPane(safe(report.getPerspectiveRaw()), 160));
        perspectivePane.setExpanded(false);

        TitledPane pythonPane = new TitledPane(
                "Python Raw Output",
                textPane(safe(report.getPythonRaw()), 140));
        pythonPane.setExpanded(false);

        root.getChildren().addAll(summary, reasonsPane, perspectivePane, pythonPane);
        dialog.getDialogPane().setContent(root);
        dialog.showAndWait();
    }

    private static TextArea textPane(String content, double prefHeight) {
        TextArea area = new TextArea(content);
        area.setWrapText(true);
        area.setEditable(false);
        area.setPrefHeight(prefHeight);
        return area;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String toBullets(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "- No additional reasons";
        }
        StringBuilder sb = new StringBuilder();
        for (String reason : reasons) {
            sb.append("- ").append(reason == null ? "" : reason).append('\n');
        }
        return sb.toString().trim();
    }
}
