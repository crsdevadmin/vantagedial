package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.PlatformControlCenterResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformDeploymentSummaryFormatterTest {

    @Test
    void formatsDeploymentSummariesWithLabeledFields() {
        PlatformControlCenterResponse response = sampleResponse();

        String latestSummary = PlatformDeploymentSummaryFormatter.formatLatestSummary(response.latestDeploymentSummary());
        String latestDetail = PlatformDeploymentSummaryFormatter.formatLatestDetail(response.latestDeploymentDetail());
        String snapshot = PlatformDeploymentSummaryFormatter.formatSnapshot(response.deploymentSnapshot());
        String history = PlatformDeploymentSummaryFormatter.formatRecentHistory(response.recentDeploymentHistory());
        String counts = PlatformDeploymentSummaryFormatter.formatStatusCounts(response.deploymentStatusCounts());
        String overview = PlatformDeploymentSummaryFormatter.formatOverview(response.deploymentOverview());

        assertEquals("provider=ASTERISK, job=job-2, status=DEPLOYED, at=2026-03-22T11:05:00Z", latestSummary);
        assertEquals("provider=ASTERISK, job=job-2, status=DEPLOYED, at=2026-03-22T11:05:00Z", latestDetail);
        assertEquals("latest=job-2, recent=2, at=2026-03-22T11:05:00Z, provider=ASTERISK, recentProviders=ASTERISK, CISCO, status=DEPLOYED", snapshot);
        assertEquals("count=2, providers=ASTERISK, CISCO", history);
        assertEquals("total=2, pending=0, dryRun=0, success=1, failed=1", counts);
        assertTrue(overview.contains("latest=job-2"));
        assertTrue(overview.contains("recentJobIds=job-2, job-1"));
        assertTrue(overview.contains("recentDeployments=provider=ASTERISK, job=job-2, status=DEPLOYED, at=2026-03-22T11:05:00Z ; provider=CISCO, job=job-1, status=FAILED, at=2026-03-21T09:00:00Z"));
        assertTrue(overview.contains("mostRecentJob=job-2"));
        assertTrue(overview.contains("mostRecentProvider=ASTERISK"));
        assertTrue(overview.contains("mostRecentCommandsList=systemctl restart vantage"));
    }

    @Test
    void formatsNullAndEmptyValuesAsNA() {
        assertEquals("N/A", PlatformDeploymentSummaryFormatter.formatLatestSummary(null));
        assertEquals("N/A", PlatformDeploymentSummaryFormatter.formatLatestDetail(null));
        assertEquals("N/A", PlatformDeploymentSummaryFormatter.formatRecentDeployments(List.of()));
        assertEquals("N/A", PlatformDeploymentSummaryFormatter.formatFlatList(List.of()));
        assertEquals("N/A", PlatformDeploymentSummaryFormatter.formatGroupedStringLists(List.of()));
    }

    private PlatformControlCenterResponse sampleResponse() {
        CustomerCommandCenterService customerCommandCenterService = mock(CustomerCommandCenterService.class);
        TelephonyDeploymentAuditService deploymentAuditService = mock(TelephonyDeploymentAuditService.class);
        PlatformControlCenterService service = new PlatformControlCenterService(
                customerCommandCenterService,
                deploymentAuditService,
                PlatformServiceTestFixtures.objectMapper(),
                "./build/test-exports"
        );

        when(customerCommandCenterService.commandCenter()).thenReturn(
                PlatformServiceTestFixtures.customerCommandCenterResponse(2, 2)
        );
        when(deploymentAuditService.listDeploymentAudits(null)).thenReturn(
                PlatformServiceTestFixtures.sampleDeployments()
        );

        return service.controlCenter();
    }
}
