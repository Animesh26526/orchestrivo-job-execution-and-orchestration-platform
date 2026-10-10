package com.orchestrivo.scheduling.factory;

import com.orchestrivo.domain.AlgorithmType;
import com.orchestrivo.scheduling.SchedulingStrategy;
import com.orchestrivo.scheduling.strategies.FcfsStrategy;
import com.orchestrivo.scheduling.strategies.RoundRobinStrategy;
import com.orchestrivo.scheduling.strategies.SjfStrategy;
import com.orchestrivo.scheduling.strategies.SrtfStrategy;

public class SchedulingStrategyFactory {
    
    public SchedulingStrategy getStrategy(AlgorithmType type) {
        if (type == null) {
            throw new IllegalArgumentException("AlgorithmType cannot be null");
        }
        
        return switch (type) {
            case FCFS -> new FcfsStrategy();
            case SJF -> new SjfStrategy();
            case SRTF -> new SrtfStrategy();
            case ROUND_ROBIN -> new RoundRobinStrategy();
            default -> throw new UnsupportedOperationException("Unsupported algorithm type: " + type);
        };
    }
}
