package com.orchestrivo.model;

public class AlgorithmConfigSpec {
    private String algorithmName;
    private int quantum = 3;

    public AlgorithmConfigSpec() {}

    public AlgorithmConfigSpec(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public AlgorithmConfigSpec(String algorithmName, int quantum) {
        this.algorithmName = algorithmName;
        this.quantum = quantum;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public void setAlgorithmName(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public int getQuantum() {
        return quantum;
    }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }
}
