package com.orchestrivo.benchmark;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ComparisonMatrixGenerator {

    public static class ComparisonRow {
        public int workers;
        public int batchSize;
        public double baselineDrainDurationMs;
        public double candidateDrainDurationMs;
        public double drainDurationDeltaMs;
        public double drainDurationChangePct;
        public double baselineThroughputJobsPerSec;
        public double candidateThroughputJobsPerSec;
        public double throughputDeltaJobsPerSec;
        public double throughputChangePct;
        public String speedupFactor;
        public double baselineE2EP50Ms;
        public double candidateE2EP50Ms;
    }

    public static class ComparisonWrapper {
        public List<ComparisonRow> comparison = new ArrayList<>();
    }

    public static void main(String[] args) throws IOException {
        Path baselinePath = Paths.get("docs/benchmark-results/baseline.json");
        Path candidatePath = Paths.get("docs/benchmark-results/candidate.json");
        Path outputCsv = Paths.get("docs/benchmark-results/comparison_matrix.csv");
        Path outputJson = Paths.get("docs/benchmark-results/comparison_matrix.json");

        generateMatrix(baselinePath, candidatePath, outputCsv, outputJson);
    }

    public static ComparisonWrapper generateMatrix(Path baselinePath, Path candidatePath, Path outputCsv, Path outputJson) throws IOException {
        Gson gson = new Gson();
        String baseContent = Files.readString(baselinePath);
        String candContent = Files.readString(candidatePath);

        QueueBenchmarkHarness.BenchmarkSuiteReport baseReport = gson.fromJson(baseContent, QueueBenchmarkHarness.BenchmarkSuiteReport.class);
        QueueBenchmarkHarness.BenchmarkSuiteReport candReport = gson.fromJson(candContent, QueueBenchmarkHarness.BenchmarkSuiteReport.class);

        Map<String, QueueBenchmarkHarness.MatrixConfigurationResult> candMap = new LinkedHashMap<>();
        for (QueueBenchmarkHarness.MatrixConfigurationResult c : candReport.configurations) {
            candMap.put(c.workerCount + "_" + c.batchSize, c);
        }

        ComparisonWrapper wrapper = new ComparisonWrapper();
        StringBuilder csv = new StringBuilder();
        csv.append("workers,batchSize,baselineDrainDurationMs,candidateDrainDurationMs,drainDurationDeltaMs,drainDurationChangePct,baselineThroughputJobsPerSec,candidateThroughputJobsPerSec,throughputDeltaJobsPerSec,throughputChangePct,speedupFactor,baselineE2EP50Ms,candidateE2EP50Ms\n");

        for (QueueBenchmarkHarness.MatrixConfigurationResult b : baseReport.configurations) {
            String key = b.workerCount + "_" + b.batchSize;
            QueueBenchmarkHarness.MatrixConfigurationResult c = candMap.get(key);
            if (c == null) continue;

            ComparisonRow row = new ComparisonRow();
            row.workers = b.workerCount;
            row.batchSize = b.batchSize;
            row.baselineDrainDurationMs = Math.round(b.medianDrainDurationMs * 100.0) / 100.0;
            row.candidateDrainDurationMs = Math.round(c.medianDrainDurationMs * 100.0) / 100.0;
            row.drainDurationDeltaMs = Math.round((row.candidateDrainDurationMs - row.baselineDrainDurationMs) * 100.0) / 100.0;
            row.drainDurationChangePct = Math.round(((row.candidateDrainDurationMs - row.baselineDrainDurationMs) / row.baselineDrainDurationMs) * 10000.0) / 100.0;

            row.baselineThroughputJobsPerSec = Math.round(b.medianDrainThroughputJobsPerSec * 100.0) / 100.0;
            row.candidateThroughputJobsPerSec = Math.round(c.medianDrainThroughputJobsPerSec * 100.0) / 100.0;
            row.throughputDeltaJobsPerSec = Math.round((row.candidateThroughputJobsPerSec - row.baselineThroughputJobsPerSec) * 100.0) / 100.0;
            row.throughputChangePct = Math.round(((row.candidateThroughputJobsPerSec - row.baselineThroughputJobsPerSec) / row.baselineThroughputJobsPerSec) * 10000.0) / 100.0;

            double speedup = row.candidateDrainDurationMs > 0 ? (row.baselineDrainDurationMs / row.candidateDrainDurationMs) : 1.0;
            row.speedupFactor = String.format(Locale.US, "%.2fx", speedup);

            row.baselineE2EP50Ms = Math.round(b.p50EndToEndLatencyMs * 100.0) / 100.0;
            row.candidateE2EP50Ms = Math.round(c.p50EndToEndLatencyMs * 100.0) / 100.0;

            wrapper.comparison.add(row);

            csv.append(String.format(Locale.US, "%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s,%.1f,%.1f\n",
                    row.workers, row.batchSize,
                    row.baselineDrainDurationMs, row.candidateDrainDurationMs, row.drainDurationDeltaMs, row.drainDurationChangePct,
                    row.baselineThroughputJobsPerSec, row.candidateThroughputJobsPerSec, row.throughputDeltaJobsPerSec, row.throughputChangePct,
                    row.speedupFactor, row.baselineE2EP50Ms, row.candidateE2EP50Ms));
        }

        if (outputCsv != null) {
            Files.createDirectories(outputCsv.getParent());
            Files.writeString(outputCsv, csv.toString());
        }
        if (outputJson != null) {
            Files.createDirectories(outputJson.getParent());
            Gson pretty = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(outputJson, pretty.toJson(wrapper));
        }

        System.out.println("Generated comparison matrix: " + wrapper.comparison.size() + " configurations.");
        return wrapper;
    }
}
