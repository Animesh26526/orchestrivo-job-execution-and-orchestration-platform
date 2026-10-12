package com.orchestrivo.model;

public enum ComparisonMode {
    MODE_A_CONTROLLED(
            "Mode A: Controlled Comparison (Identical Workload)",
            "All compared runs execute against identical process specifications. Direct algorithm comparisons and winner highlights are scientifically valid.",
            "badge-success"
    ),
    MODE_B_WORKLOAD_SCALING(
            "Mode B: Workload Scaling Analysis (Same Algorithm)",
            "The same algorithm is evaluated across differing workload sizes or process counts. Metrics reflect problem-size scaling; normalized per-process trends are highlighted.",
            "badge-primary"
    ),
    MODE_C_EXPLORATORY(
            "Mode C: Exploratory Comparison (Differing Workloads & Algorithms)",
            "Runs use both different algorithms and different workload inputs. Numerical metrics are shown for inspection, but direct winner attribution cannot be scientifically controlled.",
            "badge-warning"
    );

    private final String title;
    private final String description;
    private final String badgeClass;

    ComparisonMode(String title, String description, String badgeClass) {
        this.title = title;
        this.description = description;
        this.badgeClass = badgeClass;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
