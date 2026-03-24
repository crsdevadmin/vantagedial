package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.CampaignSummaryResponse;
import com.vantage.dialer.api.dto.ReportExportRequest;
import com.vantage.dialer.api.dto.ReportExportResponse;
import com.vantage.dialer.api.persistence.model.ExportJobStatus;
import com.vantage.dialer.api.persistence.model.ReportExportJobEntity;
import com.vantage.dialer.api.persistence.repository.ReportExportJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExportServiceTest {

    private static final Instant EXPLICIT_FROM = Instant.parse("2026-03-22T00:00:00Z");
    private static final Instant EXPLICIT_TO = Instant.parse("2026-03-23T00:00:00Z");

    @Test
    void generateCampaignSummaryExportWritesCsvAndCompletesJob(@TempDir Path tempDir) throws Exception {
        ReportExportJobRepository repository = mock(ReportExportJobRepository.class);
        ReportingService reportingService = mock(ReportingService.class);
        ExportService service = new ExportService(
                repository,
                reportingService,
                CustomerServiceTestFixtures.objectMapper(),
                tempDir.toString()
        );

        ReportExportRequest request = new ReportExportRequest();
        request.setCampaignId("campaign-1");
        request.setFrom(EXPLICIT_FROM.toString());
        request.setTo(EXPLICIT_TO.toString());

        when(repository.save(any(ReportExportJobEntity.class))).thenAnswer(invocation -> {
            ReportExportJobEntity entity = invocation.getArgument(0);
            if (entity.getCreatedAt() == null) {
                setField(entity, "createdAt", Instant.parse("2026-03-23T08:00:00Z"));
            }
            return entity;
        });
        when(reportingService.getCampaignSummary(
                "campaign-1",
                EXPLICIT_FROM,
                EXPLICIT_TO))
                .thenReturn(new CampaignSummaryResponse("campaign-1", 10, 6, 2, 5, 7));

        ReportExportResponse response = service.generateCampaignSummaryExport(request);

        String csv = Files.readString(Path.of(response.filePath()));
        assertEquals("campaign-summary", response.exportType());
        assertEquals("COMPLETED", response.status());
        assertEquals(1, response.rowCount());
        assertTrue(csv.contains("campaignId,totalSessions,completedSessions,failedSessions,bridgedSessions,answeredSessions"));
        assertTrue(csv.contains("campaign-1,10,6,2,5,7"));

        ArgumentCaptor<ReportExportJobEntity> captor = ArgumentCaptor.forClass(ReportExportJobEntity.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        ReportExportJobEntity finalSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(ExportJobStatus.COMPLETED, finalSaved.getStatus());
        assertNotNull(finalSaved.getCompletedAt());
    }

    @Test
    void generateCampaignSummaryExportUsesDefaultTypeAndFallbackDates(@TempDir Path tempDir) throws Exception {
        ReportExportJobRepository repository = mock(ReportExportJobRepository.class);
        ReportingService reportingService = mock(ReportingService.class);
        ExportService service = new ExportService(
                repository,
                reportingService,
                CustomerServiceTestFixtures.objectMapper(),
                tempDir.toString()
        );

        ReportExportRequest request = new ReportExportRequest();
        request.setCampaignId("campaign-2");

        when(repository.save(any(ReportExportJobEntity.class))).thenAnswer(invocation -> {
            ReportExportJobEntity entity = invocation.getArgument(0);
            if (entity.getCreatedAt() == null) {
                setField(entity, "createdAt", Instant.parse("2026-03-23T08:00:00Z"));
            }
            return entity;
        });

        Instant before = Instant.now();
        Instant expectedFallbackFrom = LocalDate.now().minusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC);
        when(reportingService.getCampaignSummary(any(), any(), any()))
                .thenReturn(new CampaignSummaryResponse("campaign-2", 2, 1, 0, 1, 1));

        ReportExportResponse response = service.generateCampaignSummaryExport(request);

        Instant after = Instant.now();
        ArgumentCaptor<String> campaignCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(reportingService).getCampaignSummary(campaignCaptor.capture(), fromCaptor.capture(), toCaptor.capture());

        assertEquals("campaign-summary", response.exportType());
        assertEquals("campaign-2", campaignCaptor.getValue());
        assertEquals(expectedFallbackFrom, fromCaptor.getValue());
        assertTrue(!toCaptor.getValue().isBefore(before) && !toCaptor.getValue().isAfter(after));

        ArgumentCaptor<ReportExportJobEntity> jobCaptor = ArgumentCaptor.forClass(ReportExportJobEntity.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(jobCaptor.capture());
        ReportExportJobEntity firstSaved = jobCaptor.getAllValues().get(0);
        assertEquals("campaign-summary", firstSaved.getExportType());
        assertNotNull(firstSaved.getRequestJson());
        assertTrue(firstSaved.getRequestJson().contains("\"campaignId\":\"campaign-2\""));
    }

    @Test
    void generateCampaignSummaryExportMarksJobFailedWhenExportDirectoryCannotBeCreated(@TempDir Path tempDir) throws Exception {
        ReportExportJobRepository repository = mock(ReportExportJobRepository.class);
        ReportingService reportingService = mock(ReportingService.class);
        Path invalidExportRoot = Files.writeString(tempDir.resolve("exports-file"), "not-a-directory");
        ExportService service = new ExportService(
                repository,
                reportingService,
                CustomerServiceTestFixtures.objectMapper(),
                invalidExportRoot.toString()
        );

        ReportExportRequest request = new ReportExportRequest();
        request.setCampaignId("campaign-3");
        request.setExportType("custom-export");

        when(repository.save(any(ReportExportJobEntity.class))).thenAnswer(invocation -> {
            ReportExportJobEntity entity = invocation.getArgument(0);
            if (entity.getCreatedAt() == null) {
                setField(entity, "createdAt", Instant.parse("2026-03-23T08:00:00Z"));
            }
            return entity;
        });

        ReportExportResponse response = service.generateCampaignSummaryExport(request);

        assertEquals("custom-export", response.exportType());
        assertEquals("FAILED", response.status());
        assertEquals(0, response.rowCount());
        assertNotNull(response.completedAt());
        assertNotNull(response.errorMessage());
        verifyNoInteractions(reportingService);

        ArgumentCaptor<ReportExportJobEntity> jobCaptor = ArgumentCaptor.forClass(ReportExportJobEntity.class);
        verify(repository, org.mockito.Mockito.atLeast(2)).save(jobCaptor.capture());
        ReportExportJobEntity finalSaved = jobCaptor.getAllValues().get(jobCaptor.getAllValues().size() - 1);
        assertEquals(ExportJobStatus.FAILED, finalSaved.getStatus());
        assertNotNull(finalSaved.getCompletedAt());
        assertNotNull(finalSaved.getErrorMessage());
    }

    @Test
    void getExportMapsStoredEntity() throws Exception {
        ReportExportJobRepository repository = mock(ReportExportJobRepository.class);
        ReportingService reportingService = mock(ReportingService.class);
        ExportService service = new ExportService(
                repository,
                reportingService,
                CustomerServiceTestFixtures.objectMapper(),
                "./build/test-exports"
        );

        ReportExportJobEntity entity = new ReportExportJobEntity();
        entity.setExportJobId("export-1");
        entity.setExportType("campaign-summary");
        entity.setStatus(ExportJobStatus.FAILED);
        entity.setFilePath("exports/export-1.csv");
        entity.setRowCount(0);
        entity.setCompletedAt(Instant.parse("2026-03-23T08:10:00Z"));
        entity.setErrorMessage("disk full");
        setField(entity, "createdAt", Instant.parse("2026-03-23T08:00:00Z"));

        when(repository.findById("export-1")).thenReturn(Optional.of(entity));

        ReportExportResponse response = service.getExport("export-1");

        assertEquals("export-1", response.exportJobId());
        assertEquals("FAILED", response.status());
        assertEquals("exports/export-1.csv", response.filePath());
        assertEquals("disk full", response.errorMessage());
        assertEquals(Instant.parse("2026-03-23T08:00:00Z"), response.createdAt());
    }

    @Test
    void getExportUnknownJobThrowsHelpfulError() {
        ReportExportJobRepository repository = mock(ReportExportJobRepository.class);
        ReportingService reportingService = mock(ReportingService.class);
        ExportService service = new ExportService(
                repository,
                reportingService,
                CustomerServiceTestFixtures.objectMapper(),
                "./build/test-exports"
        );

        when(repository.findById("missing-export")).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.getExport("missing-export"));

        assertEquals("Unknown export job: missing-export", error.getMessage());
        verify(repository).findById("missing-export");
        verify(reportingService, never()).getCampaignSummary(any(), any(), any());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
