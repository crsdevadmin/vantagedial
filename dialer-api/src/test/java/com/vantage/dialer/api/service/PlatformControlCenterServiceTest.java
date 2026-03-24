package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerCommandCenterResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterBundleResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformControlCenterServiceTest {

    @Test
    void returnsEmptyLatestDeploymentDetailWhenNoDeploymentsExist() {
        CustomerCommandCenterService customerCommandCenterService = mock(CustomerCommandCenterService.class);
        TelephonyDeploymentAuditService deploymentAuditService = mock(TelephonyDeploymentAuditService.class);
        PlatformControlCenterService service = service(customerCommandCenterService, deploymentAuditService, "./build/test-exports");

        when(customerCommandCenterService.commandCenter()).thenReturn(PlatformServiceTestFixtures.customerCommandCenterResponse(2, 2));
        when(deploymentAuditService.listDeploymentAudits(null)).thenReturn(List.of());

        PlatformControlCenterResponse response = service.controlCenter();

        assertTrue(response.healthy());
        assertEquals("Platform control center is healthy", response.statusMessage());
        assertEquals(List.of(), response.recentDeploymentJobIds());
        assertNotNull(response.latestDeploymentDetail());
        assertNull(response.latestDeploymentJobId());
        assertNull(response.latestDeploymentDetail().deploymentJobId());
        assertEquals(0, response.deploymentStatusCounts().totalDeployments());
        assertEquals(0, response.deploymentSnapshot().recentDeploymentCount());
        assertEquals(0, response.deploymentOverview().recentDeploymentCount());
        assertNull(response.deploymentOverview().mostRecentDeploymentJobId());
    }

    @Test
    void buildsDeploymentRollupsFromLatestAndRecentHistory() {
        CustomerCommandCenterService customerCommandCenterService = mock(CustomerCommandCenterService.class);
        TelephonyDeploymentAuditService deploymentAuditService = mock(TelephonyDeploymentAuditService.class);
        PlatformControlCenterService service = service(customerCommandCenterService, deploymentAuditService, "./build/test-exports");

        when(customerCommandCenterService.commandCenter()).thenReturn(PlatformServiceTestFixtures.customerCommandCenterResponse(2, 2));
        when(deploymentAuditService.listDeploymentAudits(null)).thenReturn(PlatformServiceTestFixtures.sampleDeployments());

        PlatformControlCenterResponse response = service.controlCenter();

        assertEquals(List.of("job-2", "job-1"), response.recentDeploymentJobIds());
        assertEquals(List.of("ASTERISK", "CISCO"), response.recentDeploymentProviders());
        assertEquals(List.of("Deployment completed", "gateway push failed"), response.recentDeploymentMessages());
        assertEquals("job-2", response.latestDeploymentJobId());
        assertEquals("ASTERISK", response.latestDeploymentProvider());
        assertEquals(List.of("agent-1", "agent-2"), response.latestDeploymentAgentIds());
        assertEquals(2, response.deploymentSnapshot().recentDeploymentCount());
        assertEquals("ASTERISK", response.deploymentSnapshot().latestDeploymentProvider());
        assertEquals(2, response.deploymentStatusCounts().totalDeployments());
        assertEquals(1, response.deploymentStatusCounts().successfulDeployments());
        assertEquals(1, response.deploymentStatusCounts().failedDeployments());
        assertEquals("job-2", response.deploymentOverview().mostRecentDeploymentJobId());
        assertEquals("ASTERISK", response.deploymentOverview().mostRecentDeploymentProvider());
        assertEquals(List.of("agent-1", "agent-2"), response.deploymentOverview().mostRecentDeploymentAgentIds());
        assertEquals("gateway push failed", response.deploymentOverview().recentDeploymentErrorMessages().get(1));
        assertEquals("Deployment failures require attention", response.statusMessage());
    }

    @Test
    void generateBundleWritesFormattedDeploymentOutputs(@TempDir Path tempDir) throws Exception {
        CustomerCommandCenterService customerCommandCenterService = mock(CustomerCommandCenterService.class);
        TelephonyDeploymentAuditService deploymentAuditService = mock(TelephonyDeploymentAuditService.class);
        PlatformControlCenterService service = service(customerCommandCenterService, deploymentAuditService, tempDir.toString());

        when(customerCommandCenterService.commandCenter()).thenReturn(PlatformServiceTestFixtures.customerCommandCenterResponse(2, 2));
        when(deploymentAuditService.listDeploymentAudits(null)).thenReturn(PlatformServiceTestFixtures.sampleDeployments());

        PlatformControlCenterBundleResponse bundle = service.generateBundle();

        Path jsonPath = Path.of(bundle.controlCenterJsonPath());
        Path htmlPath = Path.of(bundle.controlCenterHtmlPath());
        Path readmePath = Path.of(bundle.readmePath());

        assertTrue(Files.exists(jsonPath));
        assertTrue(Files.exists(htmlPath));
        assertTrue(Files.exists(readmePath));

        String json = Files.readString(jsonPath);
        String html = Files.readString(htmlPath);
        String readme = Files.readString(readmePath);

        assertEquals("ASTERISK", new ObjectMapper().readTree(json).get("latestDeploymentProvider").asText());
        assertTrue(readme.contains("Latest deployment detail: provider=ASTERISK, job=job-2, status=DEPLOYED"));
        assertTrue(readme.contains("Recent deployment history: count=2, providers=ASTERISK, CISCO"));
        assertTrue(readme.contains("Deployment overview: latest=job-2"));
        assertTrue(html.contains("Latest deployment detail: provider=ASTERISK, job=job-2, status=DEPLOYED"));
        assertTrue(html.contains("Recent deployment history: count=2, providers=ASTERISK, CISCO"));
    }

    private PlatformControlCenterService service(CustomerCommandCenterService customerCommandCenterService,
                                                 TelephonyDeploymentAuditService deploymentAuditService,
                                                 String exportDirectory) {
        return new PlatformControlCenterService(
                customerCommandCenterService,
                deploymentAuditService,
                PlatformServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
