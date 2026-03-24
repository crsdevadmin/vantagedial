package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.PlatformHealthBundleResponse;
import com.vantage.dialer.api.dto.PlatformHealthResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformHealthServiceTest {

    @Test
    void healthReflectsControlCenterDeploymentRollups() {
        CustomerCommandCenterService customerCommandCenterService = mock(CustomerCommandCenterService.class);
        TelephonyDeploymentAuditService deploymentAuditService = mock(TelephonyDeploymentAuditService.class);
        PlatformHealthService service = service(customerCommandCenterService, deploymentAuditService, "./build/test-exports");

        when(customerCommandCenterService.commandCenter()).thenReturn(
                PlatformServiceTestFixtures.customerCommandCenterResponse(2, 2)
        );
        when(deploymentAuditService.listDeploymentAudits(null)).thenReturn(
                PlatformServiceTestFixtures.sampleDeployments()
        );

        PlatformHealthResponse health = service.health();

        assertFalse(health.healthy());
        assertEquals("Deployment failures require attention", health.statusMessage());
        assertEquals(2, health.totalDeployments());
        assertEquals(1, health.failedDeployments());
        assertEquals("ASTERISK", health.latestDeploymentProvider());
        assertEquals("job-2", health.deploymentOverview().mostRecentDeploymentJobId());
        assertEquals("ASTERISK", health.deploymentOverview().mostRecentDeploymentProvider());
        assertEquals(2, health.deploymentSnapshot().recentDeploymentCount());
        assertEquals("gateway push failed", health.recentDeploymentErrorMessages().get(1));
    }

    @Test
    void generateBundleIncludesDeploymentSummaryLines(@TempDir Path tempDir) throws Exception {
        CustomerCommandCenterService customerCommandCenterService = mock(CustomerCommandCenterService.class);
        TelephonyDeploymentAuditService deploymentAuditService = mock(TelephonyDeploymentAuditService.class);
        PlatformHealthService service = service(customerCommandCenterService, deploymentAuditService, tempDir.toString());

        when(customerCommandCenterService.commandCenter()).thenReturn(
                PlatformServiceTestFixtures.customerCommandCenterResponse(2, 2)
        );
        when(deploymentAuditService.listDeploymentAudits(null)).thenReturn(
                PlatformServiceTestFixtures.sampleDeployments()
        );

        PlatformHealthBundleResponse bundle = service.generateBundle();

        String json = Files.readString(Path.of(bundle.healthJsonPath()));
        String html = Files.readString(Path.of(bundle.healthHtmlPath()));
        String readme = Files.readString(Path.of(bundle.readmePath()));

        assertEquals("ASTERISK", new ObjectMapper().readTree(json).get("latestDeploymentProvider").asText());
        assertTrue(readme.contains("Latest deployment detail: provider=ASTERISK, job=job-2, status=DEPLOYED"));
        assertTrue(readme.contains("Recent deployment history: count=2, providers=ASTERISK, CISCO"));
        assertTrue(html.contains("Latest deployment detail: provider=ASTERISK, job=job-2, status=DEPLOYED"));
        assertTrue(html.contains("Recent deployment history: count=2, providers=ASTERISK, CISCO"));
    }

    private PlatformHealthService service(CustomerCommandCenterService customerCommandCenterService,
                                          TelephonyDeploymentAuditService deploymentAuditService,
                                          String exportDirectory) {
        PlatformControlCenterService controlCenterService = new PlatformControlCenterService(
                customerCommandCenterService,
                deploymentAuditService,
                PlatformServiceTestFixtures.objectMapper(),
                exportDirectory
        );
        return new PlatformHealthService(
                controlCenterService,
                PlatformServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
