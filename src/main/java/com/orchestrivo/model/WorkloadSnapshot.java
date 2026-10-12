package com.orchestrivo.model;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.orchestrivo.domain.ProcessSpec;
import com.orchestrivo.domain.Workload;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Canonical workload snapshot representation supporting schema versioning,
 * process integrity verification, and backward compatibility with legacy raw JSON arrays.
 */
public class WorkloadSnapshot {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    private int schemaVersion = CURRENT_SCHEMA_VERSION;
    private Long seed;
    private int processCount;
    private int totalBurstTime;
    private int minArrival;
    private int maxArrival;
    private List<ProcessSpec> processes = new ArrayList<>();

    public WorkloadSnapshot() {
    }

    public WorkloadSnapshot(int schemaVersion, Long seed, List<ProcessSpec> processes) {
        this.schemaVersion = schemaVersion > 0 ? schemaVersion : CURRENT_SCHEMA_VERSION;
        this.seed = seed;
        setProcesses(processes);
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public int getProcessCount() {
        return processCount;
    }

    public void setProcessCount(int processCount) {
        this.processCount = processCount;
    }

    public int getTotalBurstTime() {
        return totalBurstTime;
    }

    public void setTotalBurstTime(int totalBurstTime) {
        this.totalBurstTime = totalBurstTime;
    }

    public int getMinArrival() {
        return minArrival;
    }

    public void setMinArrival(int minArrival) {
        this.minArrival = minArrival;
    }

    public int getMaxArrival() {
        return maxArrival;
    }

    public void setMaxArrival(int maxArrival) {
        this.maxArrival = maxArrival;
    }

    public List<ProcessSpec> getProcesses() {
        return processes != null ? processes : Collections.emptyList();
    }

    public void setProcesses(List<ProcessSpec> processes) {
        if (processes == null || processes.isEmpty()) {
            this.processes = new ArrayList<>();
            this.processCount = 0;
            this.totalBurstTime = 0;
            this.minArrival = 0;
            this.maxArrival = 0;
            return;
        }

        int totalBurst = 0;
        int minArr = Integer.MAX_VALUE;
        int maxArr = Integer.MIN_VALUE;

        List<ProcessSpec> copy = new ArrayList<>();
        for (ProcessSpec p : processes) {
            if (p == null) {
                throw new IllegalArgumentException("Process specification in snapshot cannot be null.");
            }
            if (p.getProcessId() == null || p.getProcessId().trim().isEmpty()) {
                throw new IllegalArgumentException("Process specification ID cannot be blank.");
            }
            if (p.getArrivalTime() < 0) {
                throw new IllegalArgumentException("Process arrival time cannot be negative: " + p.getArrivalTime());
            }
            if (p.getBurstTime() <= 0) {
                throw new IllegalArgumentException("Process burst time must be strictly positive: " + p.getBurstTime());
            }

            copy.add(p);
            totalBurst += p.getBurstTime();
            if (p.getArrivalTime() < minArr) minArr = p.getArrivalTime();
            if (p.getArrivalTime() > maxArr) maxArr = p.getArrivalTime();
        }

        this.processes = copy;
        this.processCount = copy.size();
        this.totalBurstTime = totalBurst;
        this.minArrival = minArr == Integer.MAX_VALUE ? 0 : minArr;
        this.maxArrival = maxArr == Integer.MIN_VALUE ? 0 : maxArr;
    }

    public static String serialize(Workload workload, Long seed) {
        List<ProcessSpec> procs = (workload != null && workload.getProcesses() != null)
                ? workload.getProcesses()
                : Collections.emptyList();
        return serialize(procs, seed);
    }

    public static String serialize(List<ProcessSpec> processes, Long seed) {
        WorkloadSnapshot snapshot = new WorkloadSnapshot(CURRENT_SCHEMA_VERSION, seed, processes);
        return new Gson().toJson(snapshot);
    }

    /**
     * Deserializes a snapshot JSON string with support for both modern versioned objects
     * and legacy JSON arrays without schema version.
     */
    public static WorkloadSnapshot deserialize(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new WorkloadSnapshot(CURRENT_SCHEMA_VERSION, null, Collections.emptyList());
        }

        String trimmed = json.trim();
        Gson gson = new Gson();

        try {
            if (trimmed.startsWith("[")) {
                // Legacy unversioned raw JSON array format
                Type listType = new TypeToken<List<ProcessSpec>>() {}.getType();
                List<ProcessSpec> legacyList = gson.fromJson(trimmed, listType);
                return new WorkloadSnapshot(1, null, legacyList);
            } else if (trimmed.startsWith("{")) {
                // Modern versioned JSON object
                WorkloadSnapshot snap = gson.fromJson(trimmed, WorkloadSnapshot.class);
                if (snap == null) {
                    throw new IllegalArgumentException("Parsed workload snapshot is null.");
                }
                if (snap.getSchemaVersion() <= 0) {
                    snap.setSchemaVersion(1);
                }
                // Validate processes and re-derive metadata
                snap.setProcesses(snap.getProcesses());
                return snap;
            } else {
                throw new IllegalArgumentException("Malformed workload snapshot: expected JSON object or array.");
            }
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Malformed workload snapshot JSON: " + e.getMessage(), e);
        }
    }

    public static List<ProcessSpec> extractProcesses(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            WorkloadSnapshot snap = deserialize(snapshotJson);
            return snap.getProcesses();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static boolean areSnapshotsEquivalent(String snapA, String snapB) {
        if (Objects.equals(snapA, snapB)) return true;
        if (snapA == null || snapB == null) return false;

        List<ProcessSpec> procsA = extractProcesses(snapA);
        List<ProcessSpec> procsB = extractProcesses(snapB);
        return !procsA.isEmpty() && procsA.equals(procsB);
    }
}
