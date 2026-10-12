package com.orchestrivo.scheduling;

import com.orchestrivo.domain.ScheduleResult;
import com.orchestrivo.domain.SchedulingConfig;
import com.orchestrivo.domain.Workload;

public interface SchedulingStrategy {
    ScheduleResult schedule(Workload workload, SchedulingConfig config);
}
