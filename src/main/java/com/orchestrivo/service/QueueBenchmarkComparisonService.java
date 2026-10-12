package com.orchestrivo.service;

import com.google.gson.Gson;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class QueueBenchmarkComparisonService {

    public static class QueueComparisonRow {
        private int workers;
        private int batchSize;
        private double baselineDrainDurationMs;
        private double candidateDrainDurationMs;
        private double drainDurationDeltaMs;
        private double drainDurationChangePct;
        private double baselineThroughputJobsPerSec;
        private double candidateThroughputJobsPerSec;
        private double throughputDeltaJobsPerSec;
        private double throughputChangePct;
        private String speedupFactor;
        private double baselineE2EP50Ms;
        private double candidateE2EP50Ms;

        public int getWorkers() { return workers; }
        public int getBatchSize() { return batchSize; }
        public double getBaselineDrainDurationMs() { return baselineDrainDurationMs; }
        public double getCandidateDrainDurationMs() { return candidateDrainDurationMs; }
        public double getDrainDurationDeltaMs() { return drainDurationDeltaMs; }
        public double getDrainDurationChangePct() { return drainDurationChangePct; }
        public double getBaselineThroughputJobsPerSec() { return baselineThroughputJobsPerSec; }
        public double getCandidateThroughputJobsPerSec() { return candidateThroughputJobsPerSec; }
        public double getThroughputDeltaJobsPerSec() { return throughputDeltaJobsPerSec; }
        public double getThroughputChangePct() { return throughputChangePct; }
        public String getSpeedupFactor() { return speedupFactor; }
        public double getBaselineE2EP50Ms() { return baselineE2EP50Ms; }
        public double getCandidateE2EP50Ms() { return candidateE2EP50Ms; }
    }

    public static class QueueComparisonContainer {
        public List<QueueComparisonRow> comparison = new ArrayList<>();
    }

    public static class QueueComparisonSummary {
        private boolean available;
        private List<QueueComparisonRow> rows = new ArrayList<>();
        private QueueComparisonRow featuredRow;
        private String highlightSpeedup = "N/A";
        private String highlightLatencyReduction = "N/A";
        private String highlightThroughputGain = "N/A";

        public boolean isAvailable() { return available; }
        public List<QueueComparisonRow> getRows() { return rows; }
        public QueueComparisonRow getFeaturedRow() { return featuredRow; }
        public String getHighlightSpeedup() { return highlightSpeedup; }
        public String getHighlightLatencyReduction() { return highlightLatencyReduction; }
        public String getHighlightThroughputGain() { return highlightThroughputGain; }
    }

    public static QueueComparisonSummary getSummary() {
        QueueComparisonSummary summary = new QueueComparisonSummary();
        try {
            String json = null;
            Path path = Paths.get("docs/benchmark-results/comparison_matrix.json");
            if (Files.exists(path)) {
                json = Files.readString(path);
            } else {
                // Check classpath resource fallback
                java.io.InputStream in = QueueBenchmarkComparisonService.class.getClassLoader()
                        .getResourceAsStream("benchmark-results/comparison_matrix.json");
                if (in != null) {
                    try (in) {
                        json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
            }

            if (json == null || json.trim().isEmpty()) {
                summary.available = false;
                return summary;
            }
            QueueComparisonContainer container = new Gson().fromJson(json, QueueComparisonContainer.class);
            if (container != null && container.comparison != null && !container.comparison.isEmpty()) {
                summary.rows = container.comparison;
                summary.available = true;

                QueueComparisonRow feat = null;
                for (QueueComparisonRow r : summary.rows) {
                    if (r.getWorkers() == 1 && r.getBatchSize() == 20) {
                        feat = r;
                        break;
                    }
                }
                if (feat == null && !summary.rows.isEmpty()) {
                    feat = summary.rows.get(0);
                }
                summary.featuredRow = feat;
                if (feat != null) {
                    summary.highlightSpeedup = feat.getSpeedupFactor();
                    summary.highlightLatencyReduction = String.format("%.2f%%", feat.getDrainDurationChangePct());
                    summary.highlightThroughputGain = String.format("%s%.2f%%", feat.getThroughputChangePct() >= 0 ? "+" : "", feat.getThroughputChangePct());
                }
            } else {
                summary.available = false;
            }
        } catch (Exception e) {
            summary.available = false;
        }
        return summary;
    }
}
