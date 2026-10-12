package com.orchestrivo.util;

import com.orchestrivo.domain.ExecutionSegment;
import com.orchestrivo.domain.ProcessSpec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CsvWorkloadParser {

    public static final int MAX_ROW_LIMIT = 500;
    public static final int MAX_CHAR_LIMIT = 1_000_000; // 1 MB approx

    public static class ParseResult {
        private final List<ProcessSpec> processes;
        private final List<String> errors;

        public ParseResult(List<ProcessSpec> processes, List<String> errors) {
            this.processes = processes != null ? processes : new ArrayList<>();
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        public boolean isSuccess() {
            return errors.isEmpty() && !processes.isEmpty();
        }

        public List<ProcessSpec> getProcesses() {
            return processes;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public static ParseResult parse(String csvContent) {
        if (csvContent == null || csvContent.trim().isEmpty()) {
            List<String> errors = new ArrayList<>();
            errors.add("CSV content is empty.");
            return new ParseResult(null, errors);
        }

        if (csvContent.length() > MAX_CHAR_LIMIT) {
            List<String> errors = new ArrayList<>();
            errors.add("CSV file exceeds maximum allowed size (1 MB).");
            return new ParseResult(null, errors);
        }

        return parse(new StringReader(csvContent));
    }

    public static ParseResult parse(Reader reader) {
        List<ProcessSpec> processes = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        try (BufferedReader br = new BufferedReader(reader)) {
            String line = br.readLine();
            int lineNumber = 1;

            if (line == null) {
                errors.add("CSV content is empty.");
                return new ParseResult(processes, errors);
            }

            // Remove UTF-8 BOM if present
            if (line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }

            // Parse headers
            List<String> headers = parseCsvLine(line);
            if (headers.size() < 3) {
                errors.add("Line 1: Invalid header format. Expected headers: process_id,arrival_time,burst_time");
                return new ParseResult(processes, errors);
            }

            int pidCol = -1;
            int arrCol = -1;
            int burstCol = -1;

            for (int i = 0; i < headers.size(); i++) {
                String h = headers.get(i).trim().toLowerCase();
                if (h.equals("process_id") || h.equals("processid") || h.equals("process_key") || h.equals("pid")) {
                    pidCol = i;
                } else if (h.equals("arrival_time") || h.equals("arrivaltime") || h.equals("arrival")) {
                    arrCol = i;
                } else if (h.equals("burst_time") || h.equals("bursttime") || h.equals("burst")) {
                    burstCol = i;
                }
            }

            if (pidCol == -1 || arrCol == -1 || burstCol == -1) {
                errors.add("Line 1: Missing required headers. Found: " + headers + ". Expected: process_id,arrival_time,burst_time");
                return new ParseResult(processes, errors);
            }

            int rowCount = 0;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }

                rowCount++;
                if (rowCount > MAX_ROW_LIMIT) {
                    errors.add("Line " + lineNumber + ": Exceeded maximum allowed process limit of " + MAX_ROW_LIMIT + " rows.");
                    break;
                }

                List<String> cols = parseCsvLine(line);
                int maxRequiredIndex = Math.max(pidCol, Math.max(arrCol, burstCol));
                if (cols.size() <= maxRequiredIndex) {
                    errors.add("Line " + lineNumber + ": Insufficient columns. Expected at least " + (maxRequiredIndex + 1) + " columns, got " + cols.size());
                    continue;
                }

                String rawPid = cols.get(pidCol).trim();
                String rawArrival = cols.get(arrCol).trim();
                String rawBurst = cols.get(burstCol).trim();

                if (rawPid.isEmpty()) {
                    errors.add("Line " + lineNumber + ": Process ID cannot be empty.");
                    continue;
                }
                if (ExecutionSegment.IDLE_PROCESS_ID.equalsIgnoreCase(rawPid)) {
                    errors.add("Line " + lineNumber + ": Process ID '" + rawPid + "' is reserved.");
                    continue;
                }
                if (!seenIds.add(rawPid)) {
                    errors.add("Line " + lineNumber + ": Duplicate process ID '" + rawPid + "'.");
                    continue;
                }

                int arrival;
                try {
                    arrival = Integer.parseInt(rawArrival);
                    if (arrival < 0) {
                        errors.add("Line " + lineNumber + ": Arrival time cannot be negative (" + arrival + ").");
                        continue;
                    }
                } catch (NumberFormatException e) {
                    errors.add("Line " + lineNumber + ": Invalid arrival time integer '" + rawArrival + "'.");
                    continue;
                }

                int burst;
                try {
                    burst = Integer.parseInt(rawBurst);
                    if (burst <= 0) {
                        errors.add("Line " + lineNumber + ": Burst time must be strictly positive (" + burst + ").");
                        continue;
                    }
                } catch (NumberFormatException e) {
                    errors.add("Line " + lineNumber + ": Invalid burst time integer '" + rawBurst + "'.");
                    continue;
                }

                processes.add(new ProcessSpec(rawPid, arrival, burst));
            }

            if (processes.isEmpty() && errors.isEmpty()) {
                errors.add("CSV file contains no valid data rows.");
            }

        } catch (IOException e) {
            errors.add("Failed to read CSV input: " + e.getMessage());
        }

        return new ParseResult(processes, errors);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++; // Skip escaped quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    values.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        values.add(sb.toString());
        return values;
    }
}
