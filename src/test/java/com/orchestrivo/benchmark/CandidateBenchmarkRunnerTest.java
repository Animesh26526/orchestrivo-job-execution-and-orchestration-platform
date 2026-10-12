package com.orchestrivo.benchmark;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class CandidateBenchmarkRunnerTest {

    @Test
    @Tag("benchmark")
    void runCandidateBenchmarkSuite() {
        BaselineBenchmarkRunner.main(new String[]{"CANDIDATE", "docs/benchmark-results/candidate.json"});
    }
}
