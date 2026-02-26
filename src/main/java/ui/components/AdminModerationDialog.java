package ui.components;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import model.ModerationReport;

import java.util.List;

public final class AdminModerationDialog {
    private static final String DECISION_APPROVE = "APPROVE";
    private static final String DECISION_REJECT = "REJECT";
    private static final String DECISION_MANUAL = "MANUAL";

    private AdminModerationDialog() {
    }

    public static void show(
            ModerationReport report,
            String contentType,
            String identifier,
            Runnable onApproveRecommendation,
            Runnable onRejectRecommendation) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Admin AI Analysis");

        ButtonType applyType = new ButtonType("Apply Recommendation", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(applyType, closeType);

        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        String normalizedType = safe(contentType).isBlank() ? "Content" : safe(contentType);
        String normalizedDecision = normalizeDecision(report.getDecision());

        root.getChildren().add(new Label("Analyzed: " + normalizedType));
        if (!safe(identifier).isBlank()) {
            root.getChildren().add(new Label("Reference: " + identifier));
        }

        VBox summary = new VBox(6);
        summary.getChildren().addAll(
                new Label("AI Decision: " + safe(report.getDecision())),
                new Label("Recommended Admin Action: " + recommendedAction(report.getDecision())),
                new Label("Predicted Category: " + safe(report.getPredictedCategory())),
                new Label(String.format("Toxicity: %.3f (%s)", report.getToxicity(), toxicityBand(report.getToxicity()))),
                new Label(String.format("Relevance: %.3f (%s)", report.getRelevance(), relevanceBand(report.getRelevance()))),
                new Label(String.format("Quality: %.3f (%s)", report.getQualityScore(), qualityBand(report.getQualityScore()))),
                new Label(String.format("Duplicate Score: %.3f (%s)",
                        report.getDuplicateScore(), duplicateBand(report.getDuplicateScore()))),
                new Label(duplicateReferenceLabel(report)),
                new Label(String.format("Latency: total=%dms, perspective=%dms, python=%dms",
                        report.getTotalLatencyMs(),
                        report.getPerspectiveLatencyMs(),
                        report.getPythonLatencyMs())));

        Label plainEnglishTitle = new Label("Plain-English Summary");
        TextArea plainEnglish = textPane(buildPlainEnglishSummary(report), 210);

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

        Label applyHint = new Label(applyHint(normalizedDecision));
        root.getChildren().addAll(summary, plainEnglishTitle, plainEnglish, reasonsPane, perspectivePane, pythonPane,
                applyHint);

        dialog.getDialogPane().setContent(root);
        Node applyNode = dialog.getDialogPane().lookupButton(applyType);
        if (applyNode instanceof Button button) {
            button.setDisable(DECISION_MANUAL.equals(normalizedDecision));
        }

        dialog.showAndWait().ifPresent(selected -> {
            if (selected != applyType) {
                return;
            }
            if (DECISION_APPROVE.equals(normalizedDecision)) {
                if (onApproveRecommendation != null) {
                    onApproveRecommendation.run();
                }
                return;
            }
            if (DECISION_REJECT.equals(normalizedDecision)) {
                if (onRejectRecommendation != null) {
                    onRejectRecommendation.run();
                }
                return;
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Manual Review");
            alert.setHeaderText("No auto-apply recommendation");
            alert.setContentText("The AI recommended manual review for this item.");
            alert.showAndWait();
        });
    }

    private static String buildPlainEnglishSummary(ModerationReport report) {
        String decision = normalizeDecision(report.getDecision());
        StringBuilder sb = new StringBuilder();
        sb.append("What this means:\n");
        sb.append("- Toxicity is ").append(percent(report.getToxicity())).append(" (").append(toxicityBand(report.getToxicity()))
                .append("). Lower is better.\n");
        sb.append("- Relevance is ").append(percent(report.getRelevance())).append(" (").append(relevanceBand(report.getRelevance()))
                .append("). Higher means more on-topic.\n");
        sb.append("- Quality is ").append(percent(report.getQualityScore())).append(" (").append(qualityBand(report.getQualityScore()))
                .append(").\n");
        sb.append("- Suggested category is ").append(safe(report.getPredictedCategory()).isBlank() ? "General"
                : safe(report.getPredictedCategory())).append(".\n");
        sb.append("- Duplicate score is ").append(percent(report.getDuplicateScore())).append(" (")
                .append(duplicateBand(report.getDuplicateScore())).append(").\n");
        if (report.getDuplicateOfPostId() != null) {
            sb.append("- It looks similar to post #").append(report.getDuplicateOfPostId()).append(".\n");
        }
        if (DECISION_APPROVE.equals(decision)) {
            sb.append("AI recommendation: Approve this content.");
        } else if (DECISION_REJECT.equals(decision)) {
            sb.append("AI recommendation: Reject this content.");
        } else {
            sb.append("AI recommendation: Keep this in manual review (needs admin judgment).");
        }
        return sb.toString();
    }

    private static String applyHint(String normalizedDecision) {
        if (DECISION_APPROVE.equals(normalizedDecision) || DECISION_REJECT.equals(normalizedDecision)) {
            return "Apply Recommendation will update the moderation status immediately.";
        }
        return "Apply Recommendation is disabled because the AI did not return Approve/Reject.";
    }

    private static String normalizeDecision(String decision) {
        String value = safe(decision).trim().toUpperCase();
        if ("APPROVED".equals(value) || DECISION_APPROVE.equals(value)) {
            return DECISION_APPROVE;
        }
        if ("REJECTED".equals(value) || DECISION_REJECT.equals(value)) {
            return DECISION_REJECT;
        }
        return DECISION_MANUAL;
    }

    private static TextArea textPane(String content, double prefHeight) {
        TextArea area = new TextArea(content);
        area.setWrapText(true);
        area.setEditable(false);
        area.setPrefHeight(prefHeight);
        return area;
    }

    private static String toxicityBand(double score) {
        if (score < 0.20) {
            return "safe";
        }
        if (score < 0.50) {
            return "borderline";
        }
        if (score < 0.80) {
            return "concerning";
        }
        return "toxic";
    }

    private static String relevanceBand(double score) {
        if (score >= 0.60) {
            return "on-topic";
        }
        if (score >= 0.35) {
            return "somewhat on-topic";
        }
        return "off-topic";
    }

    private static String qualityBand(double score) {
        if (score >= 0.80) {
            return "strong";
        }
        if (score >= 0.55) {
            return "ok";
        }
        return "weak";
    }

    private static String duplicateBand(double score) {
        if (score >= 0.90) {
            return "near-duplicate or spam";
        }
        if (score >= 0.80) {
            return "possible duplicate";
        }
        return "not a duplicate concern";
    }

    private static String duplicateReferenceLabel(ModerationReport report) {
        if (report == null || report.getDuplicateOfPostId() == null) {
            return "Likely duplicate of: none identified";
        }
        return "Likely duplicate of: Post #" + report.getDuplicateOfPostId();
    }

    private static String recommendedAction(String decision) {
        String normalized = normalizeDecision(decision);
        if (DECISION_APPROVE.equals(normalized)) {
            return "Approve";
        }
        if (DECISION_REJECT.equals(normalized)) {
            return "Reject";
        }
        return "Manual review";
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

    private static String percent(double score) {
        return String.format("%.1f%%", Math.max(0.0, Math.min(score, 1.0)) * 100.0);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
