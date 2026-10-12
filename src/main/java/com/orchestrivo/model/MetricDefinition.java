package com.orchestrivo.model;

/**
 * Metadata definition for a platform metric across scheduling simulation,
 * algorithm micro-benchmarking, and operational queue telemetry.
 */
public class MetricDefinition {

    public enum Direction {
        LOWER_IS_BETTER,
        HIGHER_IS_BETTER,
        NEUTRAL
    }

    private final String key;
    private final String displayName;
    private final String category;
    private final String unit;
    private final String formula;
    private final Direction direction;
    private final String applicableTypes;
    private final String dataSource;
    private final String timingBoundary;
    private final String limitationsNotes;

    public MetricDefinition(String key, String displayName, String category, String unit,
                            String formula, Direction direction, String applicableTypes,
                            String dataSource, String timingBoundary, String limitationsNotes) {
        this.key = key;
        this.displayName = displayName;
        this.category = category;
        this.unit = unit;
        this.formula = formula;
        this.direction = direction;
        this.applicableTypes = applicableTypes;
        this.dataSource = dataSource;
        this.timingBoundary = timingBoundary;
        this.limitationsNotes = limitationsNotes;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public String getCategory() { return category; }
    public String getUnit() { return unit; }
    public String getFormula() { return formula; }
    public Direction getDirection() { return direction; }
    public String getApplicableTypes() { return applicableTypes; }
    public String getDataSource() { return dataSource; }
    public String getTimingBoundary() { return timingBoundary; }
    public String getLimitationsNotes() { return limitationsNotes; }

    public boolean isLowerIsBetter() {
        return direction == Direction.LOWER_IS_BETTER;
    }

    public boolean isHigherIsBetter() {
        return direction == Direction.HIGHER_IS_BETTER;
    }

    public boolean isNeutral() {
        return direction == Direction.NEUTRAL;
    }
}
