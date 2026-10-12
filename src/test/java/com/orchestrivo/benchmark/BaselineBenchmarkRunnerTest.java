package com.orchestrivo.benchmark;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class BaselineBenchmarkRunnerTest {

    @Test
    @Tag("benchmark")
    void runBaselineBenchmarkSuite() {
        BaselineBenchmarkRunner.main(new String[]{"BASELINE", "docs/benchmark-results/baseline.json"});
    }
}
