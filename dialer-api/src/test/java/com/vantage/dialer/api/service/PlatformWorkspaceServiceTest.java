package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.PlatformWorkspaceResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformWorkspaceServiceTest {

    @Test
    void workspaceIncludesControlCenterRollupsAndRecentAudits() {
        CustomerCommandCenterService customerCommandCenterService = mock(CustomerCommandCenterService.class);
        TelephonyDeploymentAuditService deploymentAuditService = mock(TelephonyDeploymentAuditService.class);
        CustomerPortfolioService customerPortfolioService = mock(CustomerPortfolioService.class);
        PlatformWorkspaceService service = service(
                customerCommandCenterService,
                deploymentAuditService,
                customerPortfolioService,
                "./build/test-exports"
        );

        when(customerCommandCenterService.commandCenter()).thenReturn(
                PlatformServiceTestFixtures.customerCommandCenterResponse(2, 2)
        );
        when(customerPortfolioService.portfolio()).thenReturn(
                PlatformServiceTestFixtures.customerPortfolioResponse(2, 2)
        );
        when(deploymentAuditService.listDeploymentAudits(null)).thenReturn(
                PlatformServiceTestFixtures.sampleDeployments()
        );

        PlatformWorkspaceResponse workspace = service.workspace();

        assertFalse(workspace.healthy());
        assertEquals("Deployment failures require attention", workspace.statusMessage());
        assertEquals(2, workspace.customerPortfolio().totalCustomers());
        assertEquals(2, workspace.recentDeploymentAudits().size());
        assertEquals("job-2", workspace.latestDeploymentJobId());
        assertEquals("ASTERISK", workspace.latestDeploymentProvider());
        assertEquals("ASTERISK", workspace.deploymentOverview().mostRecentDeploymentProvider());
        assertEquals(2, workspace.deploymentSnapshot().recentDeploymentCount());
        assertEquals(2, workspace.recentDeploymentProviders().size());
    }

    private PlatformWorkspaceService service(CustomerCommandCenterService customerCommandCenterService,
                                            TelephonyDeploymentAuditService deploymentAuditService,
                                            CustomerPortfolioService customerPortfolioService,
                                            String exportDirectory) {
        PlatformControlCenterService controlCenterService = new PlatformControlCenterService(
                customerCommandCenterService,
                deploymentAuditService,
                PlatformServiceTestFixtures.objectMapper(),
                exportDirectory
        );
        return new PlatformWorkspaceService(
                controlCenterService,
                customerPortfolioService,
                deploymentAuditService,
                PlatformServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
