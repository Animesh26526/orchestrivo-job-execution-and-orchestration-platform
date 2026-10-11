package com.orchestrivo.util;

import com.google.gson.Gson;
import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.domain.Workload;
import com.orchestrivo.model.WorkloadRecord;

import java.util.*;

public class WorkloadGenerator {

    public static final int MIN_PROCESS_COUNT = 1;
    public static final int MAX_PROCESS_COUNT = 200;

    public static class GeneratorConfig {
        private String name;
        private int processCount;
        private int minArrival;
        private int maxArrival;
        private int minBurst;
        private int maxBurst;
        private long seed;

        public GeneratorConfig() {}

        public GeneratorConfig(String name, int processCount, int minArrival, int maxArrival, int minBurst, int maxBurst, long seed) {
            this.name = name;
            this.processCount = processCount;
            this.minArrival = minArrival;
            this.maxArrival = maxArrival;
            this.minBurst = minBurst;
            this.maxBurst = maxBurst;
            this.seed = seed;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
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
    }

    public static void validateConfig(GeneratorConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Generator configuration cannot be null.");
        }
        if (config.getName() == null || config.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Workload name cannot be empty.");
        }
        if (config.getProcessCount() < MIN_PROCESS_COUNT || config.getProcessCount() > MAX_PROCESS_COUNT) {
            throw new IllegalArgumentException("Process count must be between " + MIN_PROCESS_COUNT + " and " + MAX_PROCESS_COUNT + ".");
        }
        if (config.getMinArrival() < 0) {
            throw new IllegalArgumentException("Minimum arrival time cannot be negative.");
        }
        if (config.getMaxArrival() < config.getMinArrival()) {
            throw new IllegalArgumentException("Maximum arrival time must be greater than or equal to minimum arrival time.");
        }
        if (config.getMinBurst() <= 0) {
            throw new IllegalArgumentException("Minimum burst time must be strictly positive.");
        }
        if (config.getMaxBurst() < config.getMinBurst()) {
            throw new IllegalArgumentException("Maximum burst time must be greater than or equal to minimum burst time.");
        }
    }

    public static List<ProcessSpec> generateProcesses(GeneratorConfig config) {
        validateConfig(config);
        return generateProcesses(config.getProcessCount(), config.getMinArrival(), config.getMaxArrival(),
                config.getMinBurst(), config.getMaxBurst(), config.getSeed());
    }

    public static List<ProcessSpec> generateProcesses(int processCount, int minArrival, int maxArrival, int minBurst, int maxBurst, long seed) {
        if (processCount <= 0) {
            throw new IllegalArgumentException("Process count must be strictly positive.");
        }
        if (minArrival < 0) {
            throw new IllegalArgumentException("Minimum arrival time cannot be negative.");
        }
        if (maxArrival < minArrival) {
            throw new IllegalArgumentException("Maximum arrival time must be greater than or equal to minimum arrival time.");
        }
        if (minBurst <= 0) {
            throw new IllegalArgumentException("Minimum burst time must be strictly positive.");
        }
        if (maxBurst < minBurst) {
            throw new IllegalArgumentException("Maximum burst time must be greater than or equal to minimum burst time.");
        }

        Random rng = new Random(seed);
        List<ProcessSpec> list = new ArrayList<>();

        int arrivalSpan = maxArrival - minArrival + 1;
        int burstSpan = maxBurst - minBurst + 1;

        for (int i = 1; i <= processCount; i++) {
            String pid = "P" + i;
            int arrival = minArrival + rng.nextInt(arrivalSpan);
            int burst = minBurst + rng.nextInt(burstSpan);
            list.add(new ProcessSpec(pid, arrival, burst));
        }

        return list;
    }

    public static WorkloadRecord generateWorkloadRecord(long userId, GeneratorConfig config) {
        validateConfig(config);
        List<ProcessSpec> processes = generateProcesses(config);

        WorkloadRecord record = new WorkloadRecord();
        record.setOwnerUserId(userId);
        record.setName(config.getName().trim());
        record.setDescription("Generated deterministically with seed " + config.getSeed() + " (" + config.getProcessCount() + " processes)");
        record.setSourceType("GENERATED");
        record.setGenerationSeed(config.getSeed());

        Map<String, Object> configMap = new HashMap<>();
        configMap.put("processCount", config.getProcessCount());
        configMap.put("minArrival", config.getMinArrival());
        configMap.put("maxArrival", config.getMaxArrival());
        configMap.put("minBurst", config.getMinBurst());
        configMap.put("maxBurst", config.getMaxBurst());
        configMap.put("seed", config.getSeed());
        record.setGenerationConfig(new Gson().toJson(configMap));

        record.setWorkload(new Workload(processes));
        return record;
    }
}
