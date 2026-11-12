
package com.clearpath.clearpath_api.controller;

import com.clearpath.clearpath_api.service.MerchantPaymentReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/clearpath/merchant-reports")
public class MerchantPaymentReportController {

    private final MerchantPaymentReportService reportService;

    public MerchantPaymentReportController(
            MerchantPaymentReportService reportService) {
        this.reportService = reportService;
    }

    // Existing single-report API — preserved
    @PostMapping
    public MerchantPaymentReportService.MerchantReportResponse
    submitReport(@RequestBody MerchantReportRequest request) {

        return reportService.saveReport(
                request.paypalOrderId(),
                request.merchantOrderId(),
                request.paymentStatus(),
                request.amount(),
                request.currency()
        );
    }

    // New bulk CSV import API
    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BulkImportResponse importCsv(
            @RequestPart("file") MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Please upload a CSV file");
        }

        List<RowResult> results = new ArrayList<>();
        int totalRows = 0;
        int importedRows = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();

            if (headerLine == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "CSV file is empty");
            }

            // Remove UTF-8 BOM, if present
            headerLine = headerLine.replace("\uFEFF", "");

            List<String> headers = parseCsvLine(headerLine);
            Map<String, Integer> columns = new HashMap<>();

            for (int i = 0; i < headers.size(); i++) {
                columns.put(headers.get(i).trim().toLowerCase(Locale.ROOT), i);
            }

            List<String> required = List.of(
                    "paypalorderid", "merchantorderid",
                    "paymentstatus", "amount", "currency"
            );

            for (String column : required) {
                if (!columns.containsKey(column)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Missing required CSV column: " + column);
                }
            }

            String line;
            int rowNumber = 1; // Header is row 1

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                if (line.isBlank()) {
                    continue;
                }

                totalRows++;

                try {
                    List<String> values = parseCsvLine(line);

                    String paypalOrderId = getValue(values, columns, "paypalorderid");
                    String merchantOrderId = getValue(values, columns, "merchantorderid");
                    String paymentStatus = getValue(values, columns, "paymentstatus");
                    BigDecimal amount = new BigDecimal(
                            getValue(values, columns, "amount"));
                    String currency = getValue(values, columns, "currency");

                    if (paypalOrderId.isBlank()
                            || merchantOrderId.isBlank()
                            || paymentStatus.isBlank()
                            || currency.isBlank()
                            || amount.signum() < 0) {
                        throw new IllegalArgumentException(
                                "Required values are missing or amount is negative");
                    }

                    reportService.saveReport(
                            paypalOrderId,
                            merchantOrderId,
                            paymentStatus,
                            amount,
                            currency
                    );

                    importedRows++;
                    results.add(new RowResult(
                            rowNumber, paypalOrderId, "IMPORTED", "Report saved"));

                } catch (Exception ex) {
                    results.add(new RowResult(
                            rowNumber, null, "FAILED",
                            ex.getMessage() == null
                                    ? "Invalid row" : ex.getMessage()));
                }
            }
        }

        return new BulkImportResponse(
                totalRows,
                importedRows,
                totalRows - importedRows,
                results
        );
    }

    private String getValue(
            List<String> values,
            Map<String, Integer> columns,
            String column) {

        int index = columns.get(column);

        if (index >= values.size()) {
            throw new IllegalArgumentException(
                    "Missing value for column: " + column);
        }

        return values.get(index).trim();
    }

    // Supports quoted CSV values and escaped quotes
    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                if (inQuotes && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        if (inQuotes) {
            throw new IllegalArgumentException("Unclosed CSV quote");
        }

        values.add(current.toString());
        return values;
    }

    public record MerchantReportRequest(
            String paypalOrderId,
            String merchantOrderId,
            String paymentStatus,
            BigDecimal amount,
            String currency
    ) {}

    public record BulkImportResponse(
            int totalRows,
            int importedRows,
            int failedRows,
            List<RowResult> results
    ) {}

    public record RowResult(
            int rowNumber,
            String paypalOrderId,
            String status,
            String message
    ) {}
}
