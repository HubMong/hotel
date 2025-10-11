package com.example.backend.admin.service;

import com.example.backend.admin.dto.PaymentExportJobStatusDto;
import com.example.backend.admin.dto.PaymentExportRequest;
import com.example.backend.admin.dto.PaymentSummaryDto;
import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentExportService {

    private static final Duration JOB_TTL = Duration.ofMinutes(30);
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Map<String, ExportJob> jobs = new ConcurrentHashMap<>();
    private final Map<String, ExportColumn> columnMap = buildColumnMap();

    @Qualifier("exportExecutor")
    private final Executor exportExecutor;

    private volatile BaseFont pdfBaseFont;

    public PaymentExportJobStatusDto startExportJob(PaymentExportRequest request,
                                                    Supplier<List<PaymentSummaryDto>> supplier) {
        String jobId = UUID.randomUUID().toString();
        ExportJob job = ExportJob.builder()
                .jobId(jobId)
                .format(request.getFormat().toLowerCase(Locale.ROOT))
                .status(PaymentExportJobStatusDto.Status.PENDING)
                .columns(resolveColumns(request.getColumns()))
                .requestedAt(LocalDateTime.now())
                .scope(request.getScope())
                .build();
        jobs.put(jobId, job);

        Runnable task = () -> processJob(job, request, supplier);
        if (request.isAsync()) {
            exportExecutor.execute(task);
        } else {
            task.run();
        }
        return job.toStatusDto();
    }

    public Optional<PaymentExportJobStatusDto> getJobStatus(String jobId) {
        ExportJob job = jobs.get(jobId);
        if (job == null) {
            return Optional.empty();
        }
        return Optional.of(job.toStatusDto());
    }

    public Optional<ExportPayload> getPayload(String jobId) {
        ExportJob job = jobs.get(jobId);
        if (job == null || job.getStatus() != PaymentExportJobStatusDto.Status.COMPLETED || job.getPayload() == null) {
            return Optional.empty();
        }
        return Optional.of(job.getPayload());
    }

    public void purge(String jobId) {
        jobs.remove(jobId);
    }

    @Scheduled(fixedDelay = 600_000)
    public void cleanupExpiredJobs() {
        LocalDateTime now = LocalDateTime.now();
        jobs.values().removeIf(job -> job.getExpiresAt() != null && now.isAfter(job.getExpiresAt()));
    }

    private void processJob(ExportJob job,
                            PaymentExportRequest request,
                            Supplier<List<PaymentSummaryDto>> supplier) {
        job.setStatus(PaymentExportJobStatusDto.Status.PROCESSING);
        try {
            List<PaymentSummaryDto> rows = supplier.get();
            job.setTotalRows(rows.size());

            ExportPayload payload = buildPayload(job, rows);
            job.setPayload(payload);
            job.setProcessedRows(rows.size());
            job.setStatus(PaymentExportJobStatusDto.Status.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setExpiresAt(job.getCompletedAt().plus(JOB_TTL));
        } catch (Exception ex) {
            log.error("결제 내보내기 작업 실패 - jobId={} format={}", job.getJobId(), job.getFormat(), ex);
            job.setStatus(PaymentExportJobStatusDto.Status.FAILED);
            job.setMessage(ex.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            job.setExpiresAt(job.getCompletedAt().plusMinutes(5));
        }
    }

    private ExportPayload buildPayload(ExportJob job, List<PaymentSummaryDto> rows) throws Exception {
        String ts = FILE_TS.format(LocalDateTime.now());
        String baseName = "payments-" + ts;
        switch (job.getFormat()) {
            case "pdf":
                return buildPdf(job.getColumns(), rows, baseName + ".pdf");
            case "zip":
                return buildZip(job.getColumns(), rows, baseName + ".zip");
            case "csv":
            default:
                return buildCsv(job.getColumns(), rows, baseName + ".csv");
        }
    }

    private ExportPayload buildCsv(List<ExportColumn> columns,
                                   List<PaymentSummaryDto> rows,
                                   String filename) throws Exception {
        byte[] data = buildCsvBytes(columns, rows);
        return ExportPayload.builder()
                .contentType("text/csv")
                .filename(filename)
                .resource(new ByteArrayResource(data))
                .build();
    }

    private byte[] buildCsvBytes(List<ExportColumn> columns, List<PaymentSummaryDto> rows) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             ICSVWriter csvWriter = new CSVWriterBuilder(writer)
                     .withSeparator(',')
                     .withQuoteChar('"')
                     .withEscapeChar('\\')
                     .withLineEnd("\n")
                     .build()) {
            csvWriter.writeNext(columns.stream().map(ExportColumn::getHeader).toArray(String[]::new));
            for (PaymentSummaryDto dto : rows) {
                String[] line = columns.stream()
                        .map(column -> column.getExtractor().apply(dto))
                        .toArray(String[]::new);
                csvWriter.writeNext(line, true);
            }
        }
        return baos.toByteArray();
    }

    private ExportPayload buildPdf(List<ExportColumn> columns,
                                   List<PaymentSummaryDto> rows,
                                   String filename) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, baos);
        document.open();
        document.add(new Paragraph("결제 내역 요약"));
        PdfPTable table = new PdfPTable(columns.size());
        table.setWidthPercentage(100);
        Font headerFont = resolvePdfFont(11f, Font.BOLD);
        Font bodyFont = resolvePdfFont(10f, Font.NORMAL);
        for (ExportColumn column : columns) {
            PdfPCell cell = new PdfPCell(new Paragraph(column.getHeader(), headerFont));
            cell.setPadding(6f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setBackgroundColor(new Color(240, 240, 240));
            table.addCell(cell);
        }
        table.setHeaderRows(1);
        for (PaymentSummaryDto dto : rows) {
            for (ExportColumn column : columns) {
                PdfPCell cell = new PdfPCell(new Paragraph(column.getExtractor().apply(dto), bodyFont));
                cell.setPadding(4f);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                table.addCell(cell);
            }
        }
        document.add(table);
        document.close();
        return ExportPayload.builder()
                .contentType("application/pdf")
                .filename(filename)
                .resource(new ByteArrayResource(baos.toByteArray()))
                .build();
    }

    private Font resolvePdfFont(float size, int style) {
        try {
            BaseFont baseFont = loadPdfBaseFont();
            return new Font(baseFont, size, style);
        } catch (Exception ex) {
            log.warn("PDF 한글 폰트 로딩 실패, 기본 폰트로 대체합니다.", ex);
            return new Font(Font.HELVETICA, size, style);
        }
    }

    private BaseFont loadPdfBaseFont() throws Exception {
        if (pdfBaseFont != null) {
            return pdfBaseFont;
        }
        synchronized (this) {
            if (pdfBaseFont != null) {
                return pdfBaseFont;
            }
            // 1. 클래스패스 내 내장 폰트 탐색
            ClassPathResource resource = new ClassPathResource("fonts/NotoSansKR-Regular.otf");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    byte[] fontBytes = is.readAllBytes();
                    pdfBaseFont = BaseFont.createFont("NotoSansKR-Regular", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, fontBytes, null);
                    return pdfBaseFont;
                }
            }
            // 2. 시스템에 설치된 한글 CJK 기본 폰트 사용
            try {
                pdfBaseFont = BaseFont.createFont("HYGoThic-Medium", "UniKS-UCS2-H", BaseFont.NOT_EMBEDDED);
                return pdfBaseFont;
            } catch (Exception ignored) {
                log.warn("시스템 한글 폰트(HYGoThic-Medium) 로딩 실패, Helvetica로 대체합니다.");
            }
            pdfBaseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            return pdfBaseFont;
        }
    }

    private ExportPayload buildZip(List<ExportColumn> columns,
                                   List<PaymentSummaryDto> rows,
                                   String filename) throws Exception {
        byte[] csv = buildCsvBytes(columns, rows);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            ZipEntry csvEntry = new ZipEntry("payments.csv");
            zos.putNextEntry(csvEntry);
            zos.write(csv);
            zos.closeEntry();

            String info = "Generated at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            ZipEntry readme = new ZipEntry("README.txt");
            zos.putNextEntry(readme);
            zos.write(info.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return ExportPayload.builder()
                .contentType("application/zip")
                .filename(filename)
                .resource(new ByteArrayResource(baos.toByteArray()))
                .build();
    }

    private List<ExportColumn> resolveColumns(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return new ArrayList<>(columnMap.values());
        }
        return requested.stream()
                .map(String::toLowerCase)
                .map(columnMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, ExportColumn> buildColumnMap() {
        Map<String, ExportColumn> map = new LinkedHashMap<>();
        addColumn(map, "paymentId", "결제ID", dto -> toStringSafe(dto.getPaymentId()));
        addColumn(map, "reservationId", "예약ID", dto -> toStringSafe(dto.getReservationId()));
        addColumn(map, "transactionId", "예약번호", dto -> nullSafe(dto.getTransactionId()));
        addColumn(map, "hotelName", "호텔명", dto -> nullSafe(dto.getHotelName()));
        addColumn(map, "userName", "고객명", dto -> nullSafe(dto.getUserName()));
        addColumn(map, "userEmail", "고객 이메일", dto -> nullSafe(dto.getUserEmail()));
        addColumn(map, "totalPrice", "총 결제금액", dto -> currency(dto.getTotalPrice()));
        addColumn(map, "paymentMethod", "결제수단", dto -> nullSafe(dto.getPaymentMethod()));
        addColumn(map, "paymentStatus", "상태", dto -> nullSafe(dto.getPaymentStatus()));
        addColumn(map, "createdAt", "결제일시", dto -> formatDateTime(dto.getCreatedAt()));
        addColumn(map, "canceledAt", "취소일시", dto -> formatDateTime(dto.getCanceledAt()));
        addColumn(map, "receiptUrl", "영수증", dto -> nullSafe(dto.getReceiptUrl()));
        addColumn(map, "refundedAmount", "환불 총액", dto -> currency(dto.getRefundedAmount()));
        addColumn(map, "remainingAmount", "잔여 환불 가능액", dto -> currency(dto.getRemainingAmount()));
        addColumn(map, "partialRefundApplied", "부분환불 여부", dto -> Boolean.TRUE.equals(dto.getPartialRefundApplied()) ? "Y" : "N");
        addColumn(map, "memo", "메모", dto -> nullSafe(dto.getMemo()));
        return map;
    }

    private void addColumn(Map<String, ExportColumn> map,
                           String key,
                           String header,
                           Function<PaymentSummaryDto, String> extractor) {
        map.put(key.toLowerCase(Locale.ROOT), new ExportColumn(key, header, extractor));
    }

    private String toStringSafe(Object value) {
        return value == null ? "" : value.toString();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String currency(Integer amount) {
        if (amount == null) {
            return "";
        }
        return String.format(Locale.KOREA, "%,d", amount);
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Getter
    @Builder
    private static class ExportJob {
        private final String jobId;
        private final String format;
        private final PaymentExportRequest.Scope scope;
        private List<ExportColumn> columns;
        private PaymentExportJobStatusDto.Status status;
        private String message;
        private long totalRows;
        private long processedRows;
        private LocalDateTime requestedAt;
        private LocalDateTime completedAt;
        private LocalDateTime expiresAt;
        private ExportPayload payload;

        PaymentExportJobStatusDto toStatusDto() {
            return PaymentExportJobStatusDto.builder()
                    .jobId(jobId)
                    .format(format)
                    .status(status)
                    .message(message)
                    .totalRows(totalRows)
                    .processedRows(processedRows)
                    .requestedAt(requestedAt)
                    .completedAt(completedAt)
                    .expiresAt(expiresAt)
                    .downloadPath(payload != null ? jobId : null)
                    .build();
        }

        void setTotalRows(long totalRows) {
            this.totalRows = totalRows;
        }

        void setProcessedRows(long processedRows) {
            this.processedRows = processedRows;
        }

        void setStatus(PaymentExportJobStatusDto.Status status) {
            this.status = status;
        }

        void setMessage(String message) {
            this.message = message;
        }

        void setCompletedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
        }

        void setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }

        void setPayload(ExportPayload payload) {
            this.payload = payload;
        }
    }

    @Builder
    @Getter
    public static class ExportPayload {
        private final String contentType;
        private final String filename;
        private final ByteArrayResource resource;
    }

    @Getter
    private static class ExportColumn {
        private final String key;
        private final String header;
        private final Function<PaymentSummaryDto, String> extractor;

        ExportColumn(String key, String header, Function<PaymentSummaryDto, String> extractor) {
            this.key = key;
            this.header = header;
            this.extractor = extractor;
        }
    }
}
