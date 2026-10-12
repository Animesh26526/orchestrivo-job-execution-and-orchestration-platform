<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Scientific Benchmarking Methodology Guide - Orchestrivo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css">
    <style>
        .guide-container {
            max-width: 960px;
            margin: 0 auto;
        }
        .guide-section {
            margin-bottom: 32px;
        }
        .step-badge {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 28px;
            height: 28px;
            border-radius: 50%;
            background: var(--color-primary-600, #2563eb);
            color: #ffffff;
            font-weight: 700;
            font-size: 14px;
            margin-right: 10px;
            flex-shrink: 0;
        }
        .step-item {
            display: flex;
            align-items: flex-start;
            margin-bottom: 16px;
            padding: 12px 16px;
            background: #ffffff;
            border: 1px solid var(--color-slate-200, #e2e8f0);
            border-radius: var(--radius-sm, 6px);
        }
        .formula-box {
            background: var(--color-slate-900, #0f172a);
            color: #38bdf8;
            font-family: monospace;
            padding: 14px 18px;
            border-radius: var(--radius-sm, 6px);
            margin: 12px 0;
            font-size: 14px;
            line-height: 1.6;
        }
        .comparison-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 16px;
            margin-top: 12px;
        }
        @media (max-width: 768px) {
            .comparison-grid {
                grid-template-columns: 1fr;
            }
        }
        .callout-card {
            border-left: 4px solid var(--color-primary-500, #3b82f6);
            background: var(--color-slate-50, #f8fafc);
            padding: 16px 20px;
            border-radius: 0 6px 6px 0;
            margin: 16px 0;
        }
        .checklist-item {
            display: flex;
            align-items: flex-start;
            gap: 10px;
            margin-bottom: 10px;
            font-size: 14px;
        }
    </style>
</head>
<body>

<jsp:include page="/WEB-INF/views/layout/navbar.jsp"/>

<div class="app-container guide-container">
    <div class="breadcrumb-bar">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <span class="breadcrumb-separator">/</span>
        <a href="<%= request.getContextPath() %>/benchmarks">Benchmarks</a>
        <span class="breadcrumb-separator">/</span>
        <span>Methodology Guide</span>
    </div>

    <div class="page-header">
        <div class="page-title-group">
            <h1>Scientific Benchmarking Methodology Guide</h1>
            <p class="page-subtitle">Rigorous protocol, metrics separation, and reproducible performance measurement on the JVM</p>
        </div>
        <div class="page-actions">
            <a href="<%= request.getContextPath() %>/benchmarks" class="btn btn-secondary">&larr; Back to Benchmarks</a>
            <a href="<%= request.getContextPath() %>/benchmarks/run" class="btn btn-primary">+ Run Benchmark</a>
        </div>
    </div>

    <!-- Section 1: Core Concepts -->
    <div class="card guide-section">
        <div class="card-header">
            <h2 class="card-title">1. Baseline vs. Candidate Architecture</h2>
        </div>
        <p>In Orchestrivo, micro-benchmarks measure the host execution wall-clock time of scheduling algorithms. To make meaningful performance claims, runs are divided into two explicit categories:</p>
        
        <div class="comparison-grid">
            <div style="background: #f8fafc; border: 1px solid #e2e8f0; padding: 16px; border-radius: 6px;">
                <h3 style="color: var(--color-primary-700, #1d4ed8); font-size: 16px; margin-top: 0; display: flex; align-items: center; gap: 8px;">
                    <span class="badge badge-primary">BASELINE</span> Established Standard
                </h3>
                <p style="font-size: 14px; color: var(--color-slate-600); margin-bottom: 8px;">
                    Represents the canonical reference implementation, standard parameters, or initial algorithmic state. A baseline run must be executed on a clean, warmed JVM under controlled configuration.
                </p>
                <ul style="font-size: 13px; color: var(--color-slate-600); padding-left: 20px;">
                    <li>Serves as the reference point for speedup ($1.00\times$).</li>
                    <li>Should use fixed random seeds and identical process counts.</li>
                    <li>Can be pinned durably to feature on the system Dashboard.</li>
                </ul>
            </div>

            <div style="background: #f8fafc; border: 1px solid #e2e8f0; padding: 16px; border-radius: 6px;">
                <h3 style="color: #047857; font-size: 16px; margin-top: 0; display: flex; align-items: center; gap: 8px;">
                    <span class="badge badge-success">CANDIDATE</span> Optimized Experiment
                </h3>
                <p style="font-size: 14px; color: var(--color-slate-600); margin-bottom: 8px;">
                    Represents an optimized implementation, alternative data structure, or parameterized variation (such as varying the time quantum in Round Robin).
                </p>
                <ul style="font-size: 13px; color: var(--color-slate-600); padding-left: 20px;">
                    <li>Measured using the exact same hardware and configuration.</li>
                    <li>Evaluated for latency reduction, throughput delta, and stability.</li>
                    <li>Compared directly against the baseline run.</li>
                </ul>
            </div>
        </div>
    </div>

    <!-- Section 2: Actual Throughput vs. Latency Estimate -->
    <div class="card guide-section">
        <div class="card-header">
            <h2 class="card-title">2. Throughput Distinction: Actual Batch vs. Latency-Derived Estimate</h2>
        </div>
        <p>A common pitfall in system benchmarking is confusing the mathematical reciprocal of median latency with actual batch execution throughput. Orchestrivo measures and labels them independently:</p>

        <div style="margin: 16px 0;">
            <h4 style="margin-bottom: 6px; color: var(--color-slate-900);">A. Actual Measured Batch Throughput (Empirical)</h4>
            <div class="formula-box">
                Actual Batch Throughput = Completed Valid Runs / Elapsed Wall-Clock Batch Seconds<br/>
                Throughput = N_valid / (T_batch_end - T_batch_start)
            </div>
            <p style="font-size: 14px; color: var(--color-slate-600);">
                Measured with monotonic <code>System.nanoTime()</code> across the entire sequence of measured repetitions (strictly excluding warm-up cycles). Accounts for loop execution, memory allocations, and natural JVM scheduling variance.
            </p>
        </div>

        <div style="margin: 16px 0;">
            <h4 style="margin-bottom: 6px; color: var(--color-slate-900);">B. Latency-Derived Throughput Estimate (Theoretical)</h4>
            <div class="formula-box">
                Latency-Derived Throughput (Est.) = 1.0 / Median Latency (P50 in seconds)<br/>
                Throughput_est = 1,000,000,000.0 / Median_Nanos
            </div>
            <p style="font-size: 14px; color: var(--color-slate-600);">
                A theoretical reciprocal estimate showing the hypothetical maximum single-threaded throughput if every run matched the P50 median latency without any jitter, GC, or loop overhead.
            </p>
        </div>
    </div>

    <!-- Section 3: 12-Step Benchmarking Workflow -->
    <div class="card guide-section">
        <div class="card-header">
            <h2 class="card-title">3. The 12-Step Scientific Benchmarking Protocol</h2>
        </div>
        <p style="color: var(--color-slate-600); margin-bottom: 20px;">
            To achieve publishable, reproducible results, follow this 12-step experimental protocol:
        </p>

        <div class="step-item">
            <div class="step-badge">1</div>
            <div>
                <strong>Host Environment Stabilization</strong>
                <p style="font-size: 13px; color: var(--color-slate-600); margin: 2px 0 0 0;">Close background applications, disable CPU power-saving throttles, and record OS / JVM specifications.</p>
            </div>
        </div>

        <div class="step-item">
            <div class="step-badge">2</div>
            <div>
                <strong>Define Algorithmic Hypothesis</strong>
                <p style="font-size: 13px; color: var(--color-slate-600); margin: 2px 0 0 0;">State expected behavior (e.g., "Round Robin with quantum=4 reduces context switching vs quantum=1 while preserving fairness").</p>
            </div>
        </div>

        <div class="step-item">
            <div class="step-badge">3</div>
            <div>
                <strong>Deterministic Workload Generation</strong>
                <p style="font-size: 13px; color: var(--color-slate-600); margin: 2px 0 0 0;">Generate synthetic processes outside timing loops using a fixed random seed (e.g., seed <code>42</code>) to guarantee input identity.</p>
            </div>
        </div>

        <div class="step-item">
            <div class="step-badge">4</div>
            <div>
                <strong>JIT Compilation Warm-Up</strong>
                <p style="font-size: 13px; color: var(--color-slate-600); margin: 2px 0 0 0;">Execute untimed warm-up cycles (at least 5–10 iterations) to trigger JVM C1/C2 JIT compilation and tier-4 optimization.</p>
            </div>
        </div>

        <div class="step-item">
            <div class="step-badge">5</div>
            <div>
                <strong>Monotonic Nanosecond Timing</strong>
                <p style="font-size: 13px; color: var(--color-slate-600); margin: 2px 0 0 0;">Execute timed repetitions using <code>System.nanoTime()</code>, measuring pure strategy execution per iteration.</p>
            </div>
        </div>

        <div class="step-item">
            <div class="step-badge">6</div>
            <div>
                <strong>Strict Invariant Verification</strong>
                <p style="font-size: 13px; color: var(--color-slate-600); margin: 2px 0 0 0;">Verify every output schedule with <code>ScheduleValidator</code> (non-negative times, process integrity, single CPU concurrency, timeline continuity).</p>
            </div>
        </div>

        <div class="step-item">
            <div class="step-badge">7</div>
            <div>
                <strong>Batch Wall-Clock Measurement</strong>
                <p style="font-size: 13px; color: var(--color-slate-600); margin: 2px 0 0 0;">Record batch start and end nanoseconds across all measured iterations to compute true empirical batch throughput.</p>
            </div>
        </div>

        <div class="step-item">
            <div class="step-badge">8</div>
            <div>
                <strong>Statistical Distribution Analysis</strong>
                <p style="font-size: 13px; color: var(--color-slate-600); margin: 2px 0 0 0;">Compute median (P50), mean, minimum, maximum, standard deviation, and sample coefficient of variation (CV).</p>
            </div>
        </div>

        <div class="step-item">
            <div class="step-badge">9</div>
            <div>
                <strong>Audit & Provenance Logging</strong>
                <p style="font-size: 13px; color: var(--color-slate-600); margin: 2px 0 0 0;">Persist raw iteration durations and hardware environment metadata to MySQL with foreign keys and timestamps.</p>
            </div>
        </div>

        <div class="step-item">
            <div class="step-badge">10</div>
            <div>
                <strong>Controlled Pairwise Comparison</strong>
                <p style="font-size: 13px; color: var(--color-slate-600); margin: 2px 0 0 0;">Select Baseline and Candidate runs with matched process counts and seeds; verify "Controlled Match" status badge.</p>
            </div>
        </div>

        <div class="step-item">
            <div class="step-badge">11</div>
            <div>
                <strong>Delta & Speedup Computation</strong>
                <p style="font-size: 13px; color: var(--color-slate-600); margin: 2px 0 0 0;">Calculate Speedup ($P50_{baseline} / P50_{candidate}$), absolute latency delta, throughput delta, and stability change.</p>
            </div>
        </div>

        <div class="step-item">
            <div class="step-badge">12</div>
            <div>
                <strong>Durable Feature Pinning & Export</strong>
                <p style="font-size: 13px; color: var(--color-slate-600); margin: 2px 0 0 0;">Pin the canonical comparison to the Dashboard and export RFC-4180 CSV reports for documentation and GitHub publication.</p>
            </div>
        </div>
    </div>

    <!-- Section 4: Interpreting Metrics -->
    <div class="card guide-section">
        <div class="card-header">
            <h2 class="card-title">4. Interpreting Results & Statistical Jitter</h2>
        </div>
        
        <table class="data-table">
            <thead>
                <tr>
                    <th style="width: 200px;">Metric</th>
                    <th>Interpretation</th>
                    <th>Good Practice Rule</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td><strong>Speedup Factor</strong></td>
                    <td>Values &gt; 1.00x indicate speedup; &lt; 1.00x indicate regression.</td>
                    <td>Only claim speedup when workloads and algorithms are strictly matched.</td>
                </tr>
                <tr>
                    <td><strong>Median vs. Mean</strong></td>
                    <td>Median (P50) is robust against occasional GC pauses or OS interrupts.</td>
                    <td>Use Median as the primary decision metric; inspect Mean to detect outlier skew.</td>
                </tr>
                <tr>
                    <td><strong>Standard Deviation</strong></td>
                    <td>Measures timing variability and jitter across iterations.</td>
                    <td>Lower standard deviation means higher predictability and execution stability.</td>
                </tr>
                <tr>
                    <td><strong>Controlled Match</strong></td>
                    <td>Signals that algorithm, process count, seed, and quantum are matched.</td>
                    <td>Never claim algorithmic improvement if the process counts or seeds differed.</td>
                </tr>
            </tbody>
        </table>
    </div>

    <!-- Section 5: JVM Micro-benchmarking Caveats -->
    <div class="card guide-section">
        <div class="card-header">
            <h2 class="card-title">5. Known JVM Micro-benchmarking Caveats</h2>
        </div>
        <p style="color: var(--color-slate-600);">Be mindful of the inherent characteristics of JVM-level performance measurement:</p>

        <div class="callout-card">
            <h4 style="margin-top: 0; color: var(--color-slate-900);">1. JIT Warm-Up Requirements</h4>
            <p style="font-size: 13px; color: var(--color-slate-600); margin-bottom: 0;">
                The Java Virtual Machine interprets bytecode before compiling hot code to native machine instructions via the C1/C2 compilers. Always configure at least 5 warm-up iterations.
            </p>
        </div>

        <div class="callout-card">
            <h4 style="margin-top: 0; color: var(--color-slate-900);">2. Garbage Collection (GC) Noise</h4>
            <p style="font-size: 13px; color: var(--color-slate-600); margin-bottom: 0;">
                Periodic minor and major GC pauses introduce spike latencies. This is why Orchestrivo measures sequential iterations and uses median latency as the primary comparison metric.
            </p>
        </div>

        <div class="callout-card">
            <h4 style="margin-top: 0; color: var(--color-slate-900);">3. Operating System Scheduling & Thermal Throttling</h4>
            <p style="font-size: 13px; color: var(--color-slate-600); margin-bottom: 0;">
                Host OS context switches and hardware thermal throttling can cause variance across long runs. Orchestrivo records host CPU and OS specifications with each run.
            </p>
        </div>
    </div>

    <!-- Section 6: Reproducibility Checklist -->
    <div class="card guide-section">
        <div class="card-header">
            <h2 class="card-title">6. Reproducibility Checklist</h2>
        </div>
        <div class="checklist-item">
            <input type="checkbox" checked disabled>
            <span><strong>Fixed Seed:</strong> Both baseline and candidate runs must share the same seed (e.g. <code>42</code>).</span>
        </div>
        <div class="checklist-item">
            <input type="checkbox" checked disabled>
            <span><strong>Equal Process Count:</strong> Both runs must generate the same number of synthetic processes.</span>
        </div>
        <div class="checklist-item">
            <input type="checkbox" checked disabled>
            <span><strong>Equal Iterations:</strong> Both runs must use identical warm-up and measured iteration counts.</span>
        </div>
        <div class="checklist-item">
            <input type="checkbox" checked disabled>
            <span><strong>Same Host Environment:</strong> Comparison runs should be executed in the same session without rebooting.</span>
        </div>
        <div class="checklist-item">
            <input type="checkbox" checked disabled>
            <span><strong>Validated Invariants:</strong> 100% of generated schedules must pass validation before being compared.</span>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
<script src="<%= request.getContextPath() %>/assets/js/app.js"></script>
</body>
</html>
