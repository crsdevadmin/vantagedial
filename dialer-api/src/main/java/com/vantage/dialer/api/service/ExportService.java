package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.ReportExportRequest;
import com.vantage.dialer.api.dto.ReportExportResponse;
import com.vantage.dialer.api.persistence.model.ExportJobStatus;
import com.vantage.dialer.api.persistence.model.ReportExportJobEntity;
import com.vantage.dialer.api.persistence.repository.ReportExportJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class ExportService {

    private final ReportExportJobRepository reportExportJobRepository;
    private final ReportingService reportingService;
    private final ObjectMapper objectMapper;
    private final Path exportDirectory;

    public ExportService(ReportExportJobRepository reportExportJobRepository,
                         ReportingService reportingService,
                         ObjectMapper objectMapper,
                         @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.reportExportJobRepository = reportExportJobRepository;
        this.reportingService = reportingService;
        this.objectMapper = objectMapper;
        this.exportDirectory = Path.of(exportDirectory);
    }

    @Transactional
    public ReportExportResponse generateCampaignSummaryExport(ReportExportRequest request) {
        ReportExportJobEntity job = new ReportExportJobEntity();
        job.setExportJobId(UUID.randomUUID().toString());
        job.setExportType(request.getExportType() == null ? "campaign-summary" : request.getExportType());
        job.setStatus(ExportJobStatus.PENDING);
        job.setRequestJson(writeJson(request));
        reportExportJobRepository.save(job);

        try {
            Files.createDirectories(exportDirectory);
            Instant from = parseDate(request.getFrom(), LocalDate.now().minusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC));
            Instant to = parseDate(request.getTo(), Instant.now());
            var summary = reportingService.getCampaignSummary(request.getCampaignId(), from, to);
            List<String> lines = List.of(
                    "campaignId,totalSessions,completedSessions,failedSessions,bridgedSessions,answeredSessions",
                    String.join(",",
                            summary.campaignId(),
                            String.valueOf(summary.totalSessions()),
                            String.valueOf(summary.completedSessions()),
                            String.valueOf(summary.failedSessions()),
                            String.valueOf(summary.bridgedSessions()),
                            String.valueOf(summary.answeredSessions()))
            );
            Path file = exportDirectory.resolve(job.getExportJobId() + ".csv");
            Files.write(file, lines);

            job.setStatus(ExportJobStatus.COMPLETED);
            job.setFilePath(file.toAbsolutePath().toString());
            job.setRowCount(Math.max(0, lines.size() - 1));
            job.setCompletedAt(Instant.now());
            reportExportJobRepository.save(job);
        } catch (IOException ex) {
            job.setStatus(ExportJobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            job.setCompletedAt(Instant.now());
            reportExportJobRepository.save(job);
        }

        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public ReportExportResponse getExport(String exportJobId) {
        return reportExportJobRepository.findById(exportJobId).map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Unknown export job: " + exportJobId));
    }

    private ReportExportResponse toResponse(ReportExportJobEntity entity) {
        return new ReportExportResponse(
                entity.getExportJobId(),
                entity.getExportType(),
                entity.getStatus().name(),
                entity.getFilePath(),
                entity.getRowCount(),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getErrorMessage()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize export request", e);
        }
    }

    private Instant parseDate(String value, Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Instant.parse(value);
    }
}
