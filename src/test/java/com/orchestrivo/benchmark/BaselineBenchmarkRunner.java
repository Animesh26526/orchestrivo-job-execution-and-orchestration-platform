package com.orchestrivo.benchmark;

import java.nio.file.Path;
import java.nio.file.Paths;

public class BaselineBenchmarkRunner {

    public static void main(String[] args) {
        String suiteLabel = (args != null && args.length > 0) ? args[0] : "BASELINE";
        String outputFile = (args != null && args.length > 1) ? args[1] : "docs/benchmark-results/baseline.json";
        String commitHash = "eb15e4bdbff9aed151eea150b1bc0274ae856498";

        System.out.println("Starting Benchmark Suite: " + suiteLabel + " targeting " + outputFile);

        QueueBenchmarkHarness harness = new QueueBenchmarkHarness();
        harness.initialize();

        // Matrix 1: Worker scaling on controlled batch size of 20 jobs
        int[] workerCounts = {1, 2, 4, 8};
        int[] batchSizes = {20};
        int warmup = 1;
        int measured = 3;

        QueueBenchmarkHarness.BenchmarkSuiteReport report = harness.runSuite(
                suiteLabel + "_WORKER_SCALING",
                workerCounts,
                batchSizes,
                warmup,
                measured,
                commitHash
        );

        // Matrix 2: Batch size scaling on fixed 4 workers
        int[] batchWorker = {4};
        int[] multiBatchSizes = {50, 100};
        QueueBenchmarkHarness.BenchmarkSuiteReport batchReport = harness.runSuite(
                suiteLabel + "_BATCH_SCALING",
                batchWorker,
                multiBatchSizes,
                warmup,
                measured,
                commitHash
        );

        // Combine configurations into full report
        report.configurations.addAll(batchReport.configurations);
        report.suiteLabel = suiteLabel;

        try {
            Path path = Paths.get(outputFile);
            QueueBenchmarkHarness.saveReportToJson(report, path);
            System.out.println("Successfully saved benchmark suite report to: " + path.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to write benchmark report: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
