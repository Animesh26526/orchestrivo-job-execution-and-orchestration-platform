package com.orchestrivo.util;

import com.orchestrivo.domain.ProcessSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CsvWorkloadParserTest {

    @Test
    void testParseValidStandardCsv() {
        String csv = "process_id,arrival_time,burst_time\n" +
                     "P1,0,5\n" +
                     "P2,2,3\n" +
                     "P3,4,1\n";

        CsvWorkloadParser.ParseResult result = CsvWorkloadParser.parse(csv);

        assertTrue(result.isSuccess(), "Parser should succeed for valid CSV");
        assertEquals(3, result.getProcesses().size());
        assertEquals("P1", result.getProcesses().get(0).getProcessId());
        assertEquals(0, result.getProcesses().get(0).getArrivalTime());
        assertEquals(5, result.getProcesses().get(0).getBurstTime());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testParseWithExtraColumnsGracefullyTolerated() {
        String csv = "process_id,arrival_time,burst_time,notes\n" +
                     "P1,0,5,extra_info\n" +
                     "P2,1,4,more_info\n";

        CsvWorkloadParser.ParseResult result = CsvWorkloadParser.parse(csv);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getProcesses().size());
        assertEquals("P1", result.getProcesses().get(0).getProcessId());
        assertEquals(5, result.getProcesses().get(0).getBurstTime());
    }

    @Test
    void testParseWithAlternateHeaderAliasesAndBom() {
        String csv = "\uFEFFpid,arrival,burst\n" +
                     "P10,1,8\n";

        CsvWorkloadParser.ParseResult result = CsvWorkloadParser.parse(csv);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getProcesses().size());
        assertEquals("P10", result.getProcesses().get(0).getProcessId());
        assertEquals(1, result.getProcesses().get(0).getArrivalTime());
        assertEquals(8, result.getProcesses().get(0).getBurstTime());
    }

    @Test
    void testParseQuotedAndTrimmedFields() {
        String csv = "\"process_id\",\"arrival_time\",\"burst_time\"\n" +
                     " \"P1\" , \" 3 \" , \" 7 \" \n";

        CsvWorkloadParser.ParseResult result = CsvWorkloadParser.parse(csv);

        assertTrue(result.isSuccess());
        ProcessSpec p = result.getProcesses().get(0);
        assertEquals("P1", p.getProcessId());
        assertEquals(3, p.getArrivalTime());
        assertEquals(7, p.getBurstTime());
    }

    @Test
    void testParseEmptyAndNullContent() {
        CsvWorkloadParser.ParseResult resNull = CsvWorkloadParser.parse((String) null);
        assertFalse(resNull.isSuccess());
        assertTrue(resNull.getErrors().get(0).contains("empty"));

        CsvWorkloadParser.ParseResult resEmpty = CsvWorkloadParser.parse("   \n  \n");
        assertFalse(resEmpty.isSuccess());
        assertTrue(resEmpty.getErrors().get(0).contains("empty"));
    }

    @Test
    void testParseMissingRequiredHeaders() {
        String csv = "process_id,arrival_time\nP1,0\n";
        CsvWorkloadParser.ParseResult result = CsvWorkloadParser.parse(csv);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().get(0).contains("Invalid header format") || result.getErrors().get(0).contains("Missing required headers"));
    }

    @Test
    void testParseDuplicateProcessId() {
        String csv = "process_id,arrival_time,burst_time\n" +
                     "P1,0,5\n" +
                     "P1,2,3\n";

        CsvWorkloadParser.ParseResult result = CsvWorkloadParser.parse(csv);

        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Duplicate process ID 'P1'"));
    }

    @Test
    void testParseNegativeArrivalTime() {
        String csv = "process_id,arrival_time,burst_time\n" +
                     "P1,-1,5\n";

        CsvWorkloadParser.ParseResult result = CsvWorkloadParser.parse(csv);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().get(0).contains("Arrival time cannot be negative"));
    }

    @Test
    void testParseZeroOrNegativeBurstTime() {
        String csv = "process_id,arrival_time,burst_time\n" +
                     "P1,0,0\n" +
                     "P2,1,-5\n";

        CsvWorkloadParser.ParseResult result = CsvWorkloadParser.parse(csv);

        assertFalse(result.isSuccess());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Burst time must be strictly positive"));
        assertTrue(result.getErrors().get(1).contains("Burst time must be strictly positive"));
    }

    @Test
    void testParseInvalidNumberFormat() {
        String csv = "process_id,arrival_time,burst_time\n" +
                     "P1,abc,10\n" +
                     "P2,0,xyz\n";

        CsvWorkloadParser.ParseResult result = CsvWorkloadParser.parse(csv);

        assertFalse(result.isSuccess());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Invalid arrival time integer"));
        assertTrue(result.getErrors().get(1).contains("Invalid burst time integer"));
    }

    @Test
    void testParseReservedProcessId() {
        String csv = "process_id,arrival_time,burst_time\n" +
                     "IDLE,0,5\n";

        CsvWorkloadParser.ParseResult result = CsvWorkloadParser.parse(csv);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().get(0).contains("reserved"));
    }

    @Test
    void testParseExceedingRowLimit() {
        StringBuilder sb = new StringBuilder("process_id,arrival_time,burst_time\n");
        for (int i = 1; i <= 505; i++) {
            sb.append("P").append(i).append(",0,1\n");
        }

        CsvWorkloadParser.ParseResult result = CsvWorkloadParser.parse(sb.toString());

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Exceeded maximum allowed process limit")));
    }
}
