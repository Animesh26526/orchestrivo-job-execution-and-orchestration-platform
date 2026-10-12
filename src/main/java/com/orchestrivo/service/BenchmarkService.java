package com.orchestrivo.service;

import com.google.gson.Gson;
import com.orchestrivo.domain.*;
import com.orchestrivo.model.BenchmarkMeasurement;
import com.orchestrivo.model.BenchmarkRunRecord;
import com.orchestrivo.model.PagedResult;
import com.orchestrivo.persistence.BenchmarkRepository;
import com.orchestrivo.scheduling.SchedulingStrategy;
import com.orchestrivo.scheduling.factory.SchedulingStrategyFactory;
import com.orchestrivo.util.WorkloadGenerator;
import com.orchestrivo.validation.ScheduleValidator;

import java.util.*;

public class BenchmarkService {

    public static final int MIN_PROCESS_COUNT = 1;
    public static final int MAX_PROCESS_COUNT = 500;
    public static final int MIN_WARMUP = 1;
    public static final int MAX_WARMUP = 100;
    public static final int MIN_REPETITIONS = 1;
    public static final int MAX_REPETITIONS = 1000;
    public static final int MIN_QUANTUM = 1;
    public static final int MAX_QUANTUM = 100;

    private final BenchmarkRepository benchmarkRepository;
    private final ScheduleValidator validator;
    private final SchedulingStrategyFactory strategyFactory;

    public BenchmarkService() {
        this(null);
    }

    public BenchmarkService(BenchmarkRepository benchmarkRepository) {
        this.benchmarkRepository = benchmarkRepository;
        this.validator = new ScheduleValidator();
        this.strategyFactory = new SchedulingStrategyFactory();
    }

    public static class BenchmarkConfig {
        private String algorithmName;
        private int quantum = 3;
        private int processCount = 50;
        private int minArrival = 0;
        private int maxArrival = 20;
        private int minBurst = 1;
        private int maxBurst = 15;
        private long seed = 42L;
        private int warmupIterations = 5;
        private int measuredIterations = 15;
        private String runType = "STANDARD";
        private String benchmarkGroup;

        public BenchmarkConfig() {}

        public String getAlgorithmName() { return algorithmName; }
        public void setAlgorithmName(String algorithmName) { this.algorithmName = algorithmName; }
        public int getQuantum() { return quantum; }
        public void setQuantum(int quantum) { this.quantum = quantum; }
        public int getProcessCount() { return processCount; }
        public void setProcessCount(int processCount) { this.processCount = processCount; }
        public int getMinArrival() { return minArrival; }
        public void setMinArrival(int minArrival) { this.minArrival = minArrival; }
        public int getMaxArrival() { return maxArrival; }
        public void setMaxArrival(int maxArrival) { this.maxArrival = maxArrival; }
        public int getMinBurst() { return minBurst; }
        public void setMinBurst(int minBurst) { this.minBurst = minBurst; }
        public int getMaxBurst() { return maxBurst; }
        public void setMaxBurst(int maxBurst) { this.maxBurst = maxBurst; }
        public long getSeed() { return seed; }
        public void setSeed(long seed) { this.seed = seed; }
        public int getWarmupIterations() { return warmupIterations; }
        public void setWarmupIterations(int warmupIterations) { this.warmupIterations = warmupIterations; }
        public int getMeasuredIterations() { return measuredIterations; }
        public void setMeasuredIterations(int measuredIterations) { this.measuredIterations = measuredIterations; }
        public String getRunType() { return runType != null ? runType : "STANDARD"; }
        public void setRunType(String runType) { this.runType = runType; }
        public String getBenchmarkGroup() { return benchmarkGroup; }
        public void setBenchmarkGroup(String benchmarkGroup) { this.benchmarkGroup = benchmarkGroup; }
    }

    public void validateConfig(BenchmarkConfig config) {
        if (config == null) throw new IllegalArgumentException("Benchmark configuration cannot be null.");
        if (config.getAlgorithmName() == null || config.getAlgorithmName().trim().isEmpty()) {
            throw new IllegalArgumentException("Algorithm name is required.");
        }
        AlgorithmType type;
        try {
            type = AlgorithmType.valueOf(config.getAlgorithmName().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported algorithm: " + config.getAlgorithmName());
        }
        if (config.getProcessCount() < MIN_PROCESS_COUNT || config.getProcessCount() > MAX_PROCESS_COUNT) {
            throw new IllegalArgumentException("Process count must be between " + MIN_PROCESS_COUNT + " and " + MAX_PROCESS_COUNT + ".");
        }
        if (config.getWarmupIterations() < MIN_WARMUP || config.getWarmupIterations() > MAX_WARMUP) {
            throw new IllegalArgumentException("Warm-up iterations must be between " + MIN_WARMUP + " and " + MAX_WARMUP + ".");
        }
        if (config.getMeasuredIterations() < MIN_REPETITIONS || config.getMeasuredIterations() > MAX_REPETITIONS) {
            throw new IllegalArgumentException("Measured iterations must be between " + MIN_REPETITIONS + " and " + MAX_REPETITIONS + ".");
        }
        if (config.getMinArrival() < 0) {
            throw new IllegalArgumentException("Minimum arrival time cannot be negative.");
        }
        if (config.getMaxArrival() < config.getMinArrival()) {
            throw new IllegalArgumentException("Maximum arrival time must be greater than or equal to minimum arrival time.");
        }
        if (config.getMinBurst() < 1) {
            throw new IllegalArgumentException("Minimum burst time must be strictly positive (at least 1).");
        }
        if (config.getMaxBurst() < config.getMinBurst()) {
            throw new IllegalArgumentException("Maximum burst time must be greater than or equal to minimum burst time.");
        }
        if (type == AlgorithmType.ROUND_ROBIN && (config.getQuantum() < MIN_QUANTUM || config.getQuantum() > MAX_QUANTUM)) {
            throw new IllegalArgumentException("Time quantum must be between " + MIN_QUANTUM + " and " + MAX_QUANTUM + ".");
        }
    }

    public void validateConfig(AlgorithmType algorithmType, int processCount, int warmupIterations, int measuredIterations, Integer quantum) {
        if (algorithmType == null) {
            throw new IllegalArgumentException("Algorithm must be specified.");
        }
        if (algorithmType == AlgorithmType.ROUND_ROBIN && quantum == null) {
            throw new IllegalArgumentException("Time quantum is required for Round Robin.");
        }
        BenchmarkConfig config = new BenchmarkConfig();
        config.setAlgorithmName(algorithmType.name());
        config.setProcessCount(processCount);
        config.setWarmupIterations(warmupIterations);
        config.setMeasuredIterations(measuredIterations);
        if (quantum != null) {
            config.setQuantum(quantum);
        }
        validateConfig(config);
    }

    public BenchmarkRunRecord runBenchmark(long userId, BenchmarkConfig config) {
        validateConfig(config);

        AlgorithmType type = AlgorithmType.valueOf(config.getAlgorithmName().trim().toUpperCase());
        SchedulingStrategy strategy = strategyFactory.getStrategy(type);
        SchedulingConfig schedulingConfig = new SchedulingConfig(type, config.getQuantum());

        // 1. Generate workload deterministically outside the timing loop
        List<ProcessSpec> processes = WorkloadGenerator.generateProcesses(
                config.getProcessCount(),
                config.getMinArrival(), config.getMaxArrival(),
                config.getMinBurst(), config.getMaxBurst(),
                config.getSeed()
        );
        Workload workload = new Workload(processes);

        // 2. JVM warm-up iterations (untimed)
        for (int i = 0; i < config.getWarmupIterations(); i++) {
            strategy.schedule(workload, schedulingConfig);
        }

        // 3. Measured repetitions
        List<BenchmarkMeasurement> measurements = new ArrayList<>();
        List<Long> durations = new ArrayList<>();
        boolean allValid = true;

        for (int i = 1; i <= config.getMeasuredIterations(); i++) {
            long startNanos = System.nanoTime();
            ScheduleResult result = strategy.schedule(workload, schedulingConfig);
            long elapsedNanos = System.nanoTime() - startNanos;

            ValidationResult val = validator.validate(workload, result);
            if (!val.isValid()) {
                allValid = false;
            }

            measurements.add(new BenchmarkMeasurement(i, elapsedNanos));
            durations.add(elapsedNanos);
        }

        // 4. Statistical calculation
        Collections.sort(durations);
        long min = durations.get(0);
        long max = durations.get(durations.size() - 1);
        double median = computeMedian(durations);
        double stdDev = computeStdDev(durations);

        // 5. Environment info
        Map<String, Object> env = new HashMap<>();
        env.put("javaVersion", System.getProperty("java.version", "unknown"));
        env.put("javaVendor", System.getProperty("java.vendor", "unknown"));
        env.put("osName", System.getProperty("os.name", "unknown"));
        env.put("osArch", System.getProperty("os.arch", "unknown"));
        env.put("availableProcessors", Runtime.getRuntime().availableProcessors());

        BenchmarkRunRecord record = new BenchmarkRunRecord();
        record.setOwnerUserId(userId);
        record.setAlgorithmName(type.name());
        record.setProcessCount(config.getProcessCount());
        record.setSeed(config.getSeed());
        record.setWarmupIterations(config.getWarmupIterations());
        record.setMeasuredIterations(config.getMeasuredIterations());
        record.setMedianNanos(median);
        record.setMinNanos(min);
        record.setMaxNanos(max);
        record.setStdDevNanos(stdDev);
        record.setValidationPassed(allValid);
        record.setEnvironmentInfo(new Gson().toJson(env));
        record.setRunType(config.getRunType() != null ? config.getRunType() : "STANDARD");
        record.setBenchmarkGroup(config.getBenchmarkGroup());
        record.setMeasurements(measurements);

        return benchmarkRepository.save(record);
    }

    public static double computeMedian(List<Long> sortedDurations) {
        if (sortedDurations == null || sortedDurations.isEmpty()) return 0.0;
        int size = sortedDurations.size();
        if (size % 2 == 1) {
            return sortedDurations.get(size / 2);
        } else {
            return (sortedDurations.get(size / 2 - 1) + sortedDurations.get(size / 2)) / 2.0;
        }
    }

    public static double computeStdDev(List<Long> durations) {
        if (durations == null || durations.size() <= 1) return 0.0;
        double sum = 0.0;
        for (long d : durations) {
            sum += d;
        }
        double mean = sum / durations.size();
        double varianceSum = 0.0;
        for (long d : durations) {
            double diff = d - mean;
            varianceSum += diff * diff;
        }
        return Math.sqrt(varianceSum / (durations.size() - 1));
    }

    public PagedResult<BenchmarkRunRecord> getBenchmarksPaged(long userId, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(50, pageSize));
        int total = benchmarkRepository.countByOwnerId(userId);
        int offset = (safePage - 1) * safePageSize;
        List<BenchmarkRunRecord> items = benchmarkRepository.findWithFilter(userId, offset, safePageSize);
        return new PagedResult<>(items, safePage, safePageSize, total);
    }

    public Optional<BenchmarkRunRecord> getBenchmarkById(long id, long userId) {
        Optional<BenchmarkRunRecord> opt = benchmarkRepository.findById(id);
        if (opt.isPresent() && opt.get().getOwnerUserId() == userId) {
            return opt;
        }
        return Optional.empty();
    }

    public com.orchestrivo.model.BenchmarkComparisonResult compareRuns(BenchmarkRunRecord baseline, BenchmarkRunRecord candidate) {
        return new com.orchestrivo.model.BenchmarkComparisonResult(baseline, candidate);
    }

    public com.orchestrivo.model.BenchmarkComparisonResult compareRuns(long baselineId, long candidateId, long userId) {
        Optional<BenchmarkRunRecord> baseOpt = getBenchmarkById(baselineId, userId);
        if (baseOpt.isEmpty()) {
            throw new IllegalArgumentException("Baseline benchmark run #" + baselineId + " not found or unauthorized.");
        }
        Optional<BenchmarkRunRecord> candOpt = getBenchmarkById(candidateId, userId);
        if (candOpt.isEmpty()) {
            throw new IllegalArgumentException("Candidate benchmark run #" + candidateId + " not found or unauthorized.");
        }
        return compareRuns(baseOpt.get(), candOpt.get());
    }

    public List<BenchmarkRunRecord> getBaselineRuns(long userId) {
        if (benchmarkRepository == null) return Collections.emptyList();
        return benchmarkRepository.findByRunType(userId, "BASELINE");
    }

    public List<BenchmarkRunRecord> getRunsByGroup(long userId, String group) {
        if (benchmarkRepository == null || group == null) return Collections.emptyList();
        return benchmarkRepository.findByBenchmarkGroup(userId, group);
    }
}
