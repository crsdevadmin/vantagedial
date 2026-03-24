package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.CostEstimateRequest;
import com.vantage.dialer.api.dto.InstallationQuoteSummaryResponse;
import com.vantage.dialer.api.dto.QuoteProposalResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotBundleResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotComparisonBundleResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotDashboardBundleResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotDashboardExportResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotDashboardResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotSummaryResponse;
import com.vantage.dialer.api.persistence.model.QuoteSnapshotEntity;
import com.vantage.dialer.api.persistence.repository.QuoteSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuoteSnapshotServiceTest {

    private static final String INSTALLATION_JOB_ID = "install-1";
    private static final String CUSTOMER_ID = "customer-1";

    @Test
    void createPersistsSnapshotAndWritesCsv(@TempDir Path tempDir) throws Exception {
        QuoteSnapshotRepository repository = mock(QuoteSnapshotRepository.class);
        CustomerQuoteService customerQuoteService = mock(CustomerQuoteService.class);
        QuoteSnapshotService service = service(repository, customerQuoteService, tempDir.toString());

        InstallationQuoteSummaryResponse summary =
                CustomerServiceTestFixtures.installationQuoteSummaryResponse(INSTALLATION_JOB_ID, CUSTOMER_ID);
        CostEstimateRequest request = new CostEstimateRequest();
        request.setCustomerId(CUSTOMER_ID);

        when(customerQuoteService.quote(INSTALLATION_JOB_ID, request)).thenReturn(summary);
        when(repository.save(any(QuoteSnapshotEntity.class))).thenAnswer(invocation -> {
            QuoteSnapshotEntity entity = invocation.getArgument(0);
            setField(entity, "createdAt", Instant.parse("2026-03-22T12:10:00Z"));
            return entity;
        });

        QuoteSnapshotResponse response = service.create(INSTALLATION_JOB_ID, request);

        assertEquals(INSTALLATION_JOB_ID, response.installationJobId());
        assertEquals(CUSTOMER_ID, response.customerId());
        assertEquals(149.5, response.summary().estimate().suggestedSellPrice());
        assertNotNull(response.quoteSnapshotId());
        assertTrue(response.filePath().endsWith(".csv"));
        assertTrue(Files.exists(Path.of(response.filePath())));
        String csv = Files.readString(Path.of(response.filePath()));
        assertTrue(csv.contains("installationJobId,customerId,customerName"));
        assertTrue(csv.contains("Acme Corp"));
        verify(repository).save(any(QuoteSnapshotEntity.class));
    }

    @Test
    void summaryTimelineDashboardAndComparisonReflectPreviousSnapshot(@TempDir Path tempDir) throws Exception {
        QuoteSnapshotRepository repository = mock(QuoteSnapshotRepository.class);
        CustomerQuoteService customerQuoteService = mock(CustomerQuoteService.class);
        QuoteSnapshotService service = service(repository, customerQuoteService, tempDir.toString());

        QuoteSnapshotEntity previous = snapshotEntity(
                "quote-1",
                Instant.parse("2026-03-21T10:00:00Z"),
                summaryWithPrice(120.0, 90.0, 800L)
        );
        QuoteSnapshotEntity latest = snapshotEntity(
                "quote-2",
                Instant.parse("2026-03-22T10:00:00Z"),
                summaryWithPrice(149.5, 112.0, 1000L)
        );

        when(repository.findByInstallationJobIdAndCustomerIdOrderByCreatedAtDesc(INSTALLATION_JOB_ID, CUSTOMER_ID))
                .thenReturn(List.of(latest, previous));
        when(repository.findById("quote-2")).thenReturn(Optional.of(latest));
        when(repository.findById("quote-1")).thenReturn(Optional.of(previous));
        when(repository.findFirstByInstallationJobIdAndCustomerIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                eq(INSTALLATION_JOB_ID), eq(CUSTOMER_ID), eq(Instant.parse("2026-03-22T10:00:00Z"))))
                .thenReturn(Optional.of(previous));

        QuoteSnapshotSummaryResponse summary = service.summary(INSTALLATION_JOB_ID, CUSTOMER_ID);
        QuoteSnapshotDashboardResponse dashboard = service.dashboard(INSTALLATION_JOB_ID, CUSTOMER_ID);
        QuoteSnapshotComparisonBundleResponse comparisonBundle = service.generatePreviousComparisonBundle("quote-2");
        QuoteSnapshotDashboardBundleResponse dashboardBundle = service.generateDashboardBundle(INSTALLATION_JOB_ID, CUSTOMER_ID);
        QuoteSnapshotDashboardExportResponse dashboardExport = service.exportDashboard(INSTALLATION_JOB_ID, CUSTOMER_ID);

        assertEquals(2, summary.snapshotCount());
        assertEquals("quote-2", summary.latestSnapshot().quoteSnapshotId());
        assertEquals(29.5, summary.trendMetrics().latestSuggestedSellPriceDelta());
        assertEquals("UP", summary.trendMetrics().suggestedSellPriceTrend());
        assertEquals(2, dashboard.timeline().size());
        assertEquals("quote-2", dashboard.timeline().get(0).snapshot().quoteSnapshotId());
        assertEquals("quote-1", dashboard.timeline().get(0).previousQuoteSnapshotId());

        String comparisonMarkdown = Files.readString(Path.of(comparisonBundle.summaryMarkdownPath()));
        String dashboardMarkdown = Files.readString(Path.of(dashboardBundle.dashboardMarkdownPath()));
        String dashboardHtml = Files.readString(Path.of(dashboardBundle.dashboardHtmlPath()));
        String dashboardCsv = Files.readString(Path.of(dashboardExport.dashboardCsvPath()));

        assertTrue(comparisonMarkdown.contains("Suggested sell price delta: 29.5"));
        assertTrue(dashboardMarkdown.contains("Latest Comparison"));
        assertTrue(dashboardMarkdown.contains("Previous Quote Snapshot Id: quote-1"));
        assertTrue(dashboardHtml.contains("Quote Dashboard"));
        assertTrue(dashboardHtml.contains("Timeline Entry Count"));
        assertTrue(dashboardCsv.contains("quote-2"));
    }

    @Test
    void bundleAndProposalWriteSnapshotArtifacts(@TempDir Path tempDir) throws Exception {
        QuoteSnapshotRepository repository = mock(QuoteSnapshotRepository.class);
        CustomerQuoteService customerQuoteService = mock(CustomerQuoteService.class);
        QuoteSnapshotService service = service(repository, customerQuoteService, tempDir.toString());

        QuoteSnapshotEntity latest = snapshotEntity(
                "quote-2",
                Instant.parse("2026-03-22T10:00:00Z"),
                summaryWithPrice(149.5, 112.0, 1000L)
        );

        when(repository.findById("quote-2")).thenReturn(Optional.of(latest));

        QuoteSnapshotBundleResponse bundle = service.generateBundle("quote-2");
        QuoteProposalResponse proposal = service.generateProposal("quote-2");

        JsonNode summaryJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.summaryJsonPath())));
        JsonNode assumptionsJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.assumptionsJsonPath())));
        String readme = Files.readString(Path.of(bundle.readmePath()));
        String proposalMarkdown = Files.readString(Path.of(proposal.proposalMarkdownPath()));
        String proposalHtml = Files.readString(Path.of(proposal.proposalHtmlPath()));

        assertEquals("Acme Corp", summaryJson.get("customerName").asText());
        assertEquals("preset", assumptionsJson.get("source").asText());
        assertTrue(readme.contains("Suggested Sell Price: 149.5"));
        assertTrue(proposalMarkdown.contains("## Included Scope"));
        assertTrue(proposalMarkdown.contains("Suggested monthly sell price: 149.5"));
        assertTrue(proposalHtml.contains("Vantage Dialer Proposal"));
        assertTrue(proposalHtml.contains("Acme Corp"));
    }

    private QuoteSnapshotService service(QuoteSnapshotRepository repository,
                                         CustomerQuoteService customerQuoteService,
                                         String exportDirectory) {
        return new QuoteSnapshotService(
                repository,
                customerQuoteService,
                CustomerServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }

    private QuoteSnapshotEntity snapshotEntity(String quoteSnapshotId,
                                               Instant createdAt,
                                               InstallationQuoteSummaryResponse summary) throws Exception {
        QuoteSnapshotEntity entity = new QuoteSnapshotEntity();
        entity.setQuoteSnapshotId(quoteSnapshotId);
        entity.setInstallationJobId(summary.installationJobId());
        entity.setCustomerId(summary.customerId());
        entity.setConfigurationId(summary.estimate().configurationId());
        entity.setRequestJson(CustomerServiceTestFixtures.objectMapper().writeValueAsString(new CostEstimateRequest()));
        entity.setSummaryJson(CustomerServiceTestFixtures.objectMapper().writeValueAsString(summary));
        entity.setFilePath("quotes/" + quoteSnapshotId + ".csv");
        setField(entity, "createdAt", createdAt);
        return entity;
    }

    private InstallationQuoteSummaryResponse summaryWithPrice(double sellPrice,
                                                              double estimatedCost,
                                                              long monthlyCallMinutes) {
        return new InstallationQuoteSummaryResponse(
                INSTALLATION_JOB_ID,
                CUSTOMER_ID,
                "Acme Softphone",
                "Acme Corp",
                "SOFTPHONE",
                "COMPLETED",
                2,
                List.of("1001", "1002"),
                null,
                new com.vantage.dialer.api.dto.CommercialAssumptionsResponse(
                        "preset",
                        monthlyCallMinutes,
                        2000L,
                        100L,
                        5.0,
                        2,
                        10,
                        30.0
                ),
                new com.vantage.dialer.api.dto.CostEstimateResponse(
                        CUSTOMER_ID,
                        "default",
                        40.0,
                        estimatedCost - 40.0,
                        estimatedCost,
                        sellPrice,
                        30.0
                ),
                Instant.parse("2026-03-22T12:00:00Z")
        );
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
