package com.vantage.dialer.api.persistence.repository;

import com.vantage.dialer.api.agent.AgentStatus;
import com.vantage.dialer.api.campaign.LeadStatus;
import com.vantage.dialer.api.persistence.model.AgentEntity;
import com.vantage.dialer.api.persistence.model.AsteriskDeploymentJobEntity;
import com.vantage.dialer.api.persistence.model.CallSessionEntity;
import com.vantage.dialer.api.persistence.model.CallEventEntity;
import com.vantage.dialer.api.persistence.model.CostConfigurationEntity;
import com.vantage.dialer.api.persistence.model.DeploymentExecutionStatus;
import com.vantage.dialer.api.persistence.model.CustomerInstallationJobEntity;
import com.vantage.dialer.api.persistence.model.InstallationJobStatus;
import com.vantage.dialer.api.persistence.model.IvrStepEntity;
import com.vantage.dialer.api.persistence.model.IvrStepType;
import com.vantage.dialer.api.persistence.model.LeadEntity;
import com.vantage.dialer.api.persistence.model.PromptSourceType;
import com.vantage.dialer.api.persistence.model.QuoteSnapshotEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vantagedial;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RepositoryDataJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private CallSessionRepository callSessionRepository;

    @Autowired
    private QuoteSnapshotRepository quoteSnapshotRepository;

    @Autowired
    private AsteriskDeploymentJobRepository asteriskDeploymentJobRepository;

    @Autowired
    private CallEventRepository callEventRepository;

    @Autowired
    private CustomerInstallationJobRepository customerInstallationJobRepository;

    @Autowired
    private IvrStepRepository ivrStepRepository;

    @Autowired
    private CostConfigurationRepository costConfigurationRepository;

    @Test
    void agentRepositoryFindsByStatusInAgentIdOrder() {
        entityManager.persist(agent("agent-b", AgentStatus.AVAILABLE));
        entityManager.persist(agent("agent-a", AgentStatus.AVAILABLE));
        entityManager.persist(agent("agent-c", AgentStatus.BUSY));
        entityManager.flush();
        entityManager.clear();

        List<AgentEntity> availableAgents = agentRepository.findByStatusOrderByAgentIdAsc(AgentStatus.AVAILABLE);

        assertEquals(List.of("agent-a", "agent-b"), availableAgents.stream().map(AgentEntity::getAgentId).toList());
    }

    @Test
    void asteriskDeploymentJobRepositoryOrdersByCreatedAtDescending() throws InterruptedException {
        entityManager.persistFlushFind(asteriskDeploymentJob("deploy-1", "pkg-a"));
        pauseForClockTick();
        entityManager.persistFlushFind(asteriskDeploymentJob("deploy-2", "pkg-a"));
        pauseForClockTick();
        entityManager.persistFlushFind(asteriskDeploymentJob("deploy-3", "pkg-b"));
        entityManager.clear();

        List<AsteriskDeploymentJobEntity> allJobs = asteriskDeploymentJobRepository.findAllByOrderByCreatedAtDesc();
        List<AsteriskDeploymentJobEntity> packageJobs = asteriskDeploymentJobRepository.findByPackageIdOrderByCreatedAtDesc("pkg-a");

        assertEquals(List.of("deploy-3", "deploy-2", "deploy-1"),
                allJobs.stream().map(AsteriskDeploymentJobEntity::getDeploymentJobId).toList());
        assertEquals(List.of("deploy-2", "deploy-1"),
                packageJobs.stream().map(AsteriskDeploymentJobEntity::getDeploymentJobId).toList());
    }

    @Test
    void leadRepositoryFiltersAndCountsCampaignLeads() throws InterruptedException {
        LeadEntity first = entityManager.persistFlushFind(lead("lead-1", "campaign-a", LeadStatus.NEW));
        pauseForClockTick();
        LeadEntity second = entityManager.persistFlushFind(lead("lead-2", "campaign-a", LeadStatus.QUEUED));
        pauseForClockTick();
        entityManager.persistFlushFind(lead("lead-3", "campaign-a", LeadStatus.COMPLETED));
        entityManager.persistFlushFind(lead("lead-4", "campaign-b", LeadStatus.NEW));
        entityManager.clear();

        List<LeadEntity> campaignLeads = leadRepository.findByCampaignIdOrderByCreatedAtAsc("campaign-a");
        List<LeadEntity> queuedLeads = leadRepository.findByCampaignIdAndStatusOrderByCreatedAtAsc("campaign-a", LeadStatus.QUEUED);
        long activeCount = leadRepository.countByCampaignIdAndStatusIn(
                "campaign-a",
                Set.of(LeadStatus.NEW, LeadStatus.QUEUED, LeadStatus.IN_PROGRESS)
        );

        assertEquals(List.of("lead-1", "lead-2", "lead-3"), campaignLeads.stream().map(LeadEntity::getLeadId).toList());
        assertEquals(List.of("lead-2"), queuedLeads.stream().map(LeadEntity::getLeadId).toList());
        assertEquals(2L, activeCount);
        assertTrue(first.getCreatedAt().isBefore(second.getCreatedAt()));
    }

    @Test
    void leadEntityUpdatesUpdatedAtOnModification() throws InterruptedException {
        LeadEntity lead = entityManager.persistFlushFind(lead("lead-update", "campaign-update", LeadStatus.NEW));
        Instant createdAt = lead.getCreatedAt();
        Instant originalUpdatedAt = lead.getUpdatedAt();

        pauseForClockTick();
        lead.setAttempts(lead.getAttempts() + 1);
        lead.setStatus(LeadStatus.IN_PROGRESS);
        entityManager.flush();

        assertEquals(createdAt, lead.getCreatedAt());
        assertTrue(lead.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void callEventRepositoryFiltersSessionCampaignAndAgentTimelines() {
        entityManager.persist(callEvent("event-1", "session-a", "campaign-a", "agent-a", Instant.parse("2026-03-20T10:00:00Z")));
        entityManager.persist(callEvent("event-2", "session-a", "campaign-a", "agent-b", Instant.parse("2026-03-20T10:05:00Z")));
        entityManager.persist(callEvent("event-3", "session-b", "campaign-b", "agent-a", Instant.parse("2026-03-20T10:10:00Z")));
        entityManager.flush();
        entityManager.clear();

        List<CallEventEntity> sessionEvents = callEventRepository.findByCallSessionIdOrderByEventTimestampAsc("session-a");
        List<CallEventEntity> campaignEvents = callEventRepository.findByCampaignIdAndEventTimestampBetweenOrderByEventTimestampAsc(
                "campaign-a",
                Instant.parse("2026-03-20T09:59:00Z"),
                Instant.parse("2026-03-20T10:06:00Z")
        );
        List<CallEventEntity> agentEvents = callEventRepository.findByAgentIdAndEventTimestampBetweenOrderByEventTimestampAsc(
                "agent-a",
                Instant.parse("2026-03-20T09:59:00Z"),
                Instant.parse("2026-03-20T10:11:00Z")
        );

        assertEquals(List.of("event-1", "event-2"),
                sessionEvents.stream().map(CallEventEntity::getEventId).toList());
        assertEquals(List.of("event-1", "event-2"),
                campaignEvents.stream().map(CallEventEntity::getEventId).toList());
        assertEquals(List.of("event-1", "event-3"),
                agentEvents.stream().map(CallEventEntity::getEventId).toList());
    }

    @Test
    void callSessionRepositoryFiltersByCampaignAndDateWindowDescending() throws InterruptedException {
        CallSessionEntity first = entityManager.persistFlushFind(callSession("session-1", "campaign-a"));
        pauseForClockTick();
        CallSessionEntity second = entityManager.persistFlushFind(callSession("session-2", "campaign-a"));
        pauseForClockTick();
        entityManager.persistFlushFind(callSession("session-3", "campaign-b"));
        entityManager.clear();

        Instant from = first.getCreatedAt().minusSeconds(1);
        Instant to = second.getCreatedAt().plusSeconds(1);

        List<CallSessionEntity> sessions = callSessionRepository
                .findByCampaignIdAndCreatedAtBetweenOrderByCreatedAtDesc("campaign-a", from, to);

        assertEquals(List.of("session-2", "session-1"),
                sessions.stream().map(CallSessionEntity::getCallSessionId).toList());
    }

    @Test
    void callSessionRepositorySupportsCampaignAndGlobalDescendingViews() throws InterruptedException {
        CallSessionEntity first = entityManager.persistFlushFind(callSession("session-10", "campaign-z"));
        pauseForClockTick();
        CallSessionEntity second = entityManager.persistFlushFind(callSession("session-11", "campaign-z"));
        pauseForClockTick();
        CallSessionEntity third = entityManager.persistFlushFind(callSession("session-12", "campaign-y"));
        entityManager.clear();

        Instant from = first.getCreatedAt().minusSeconds(1);
        Instant to = third.getCreatedAt().plusSeconds(1);

        List<CallSessionEntity> campaignSessions = callSessionRepository.findByCampaignIdOrderByCreatedAtDesc("campaign-z");
        List<CallSessionEntity> allWindowSessions = callSessionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to);

        assertEquals(List.of("session-11", "session-10"),
                campaignSessions.stream().map(CallSessionEntity::getCallSessionId).toList());
        assertEquals(List.of("session-12", "session-11", "session-10"),
                allWindowSessions.stream().map(CallSessionEntity::getCallSessionId).toList());
    }

    @Test
    void callSessionEntityUpdatesUpdatedAtOnModification() throws InterruptedException {
        CallSessionEntity session = entityManager.persistFlushFind(callSession("session-update", "campaign-update"));
        Instant createdAt = session.getCreatedAt();
        Instant originalUpdatedAt = session.getUpdatedAt();

        pauseForClockTick();
        session.setStatus("CALL_BRIDGED");
        session.setLastEventType("CALL_BRIDGED");
        entityManager.flush();

        assertEquals(createdAt, session.getCreatedAt());
        assertTrue(session.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void customerInstallationJobRepositoryOrdersGlobalAndCustomerViewsDescending() throws InterruptedException {
        entityManager.persistFlushFind(customerInstallationJob("install-1", "customer-a"));
        pauseForClockTick();
        entityManager.persistFlushFind(customerInstallationJob("install-2", "customer-a"));
        pauseForClockTick();
        entityManager.persistFlushFind(customerInstallationJob("install-3", "customer-b"));
        entityManager.clear();

        List<CustomerInstallationJobEntity> allJobs = customerInstallationJobRepository.findAllByOrderByCreatedAtDesc();
        List<CustomerInstallationJobEntity> customerJobs = customerInstallationJobRepository.findAllByCustomerIdOrderByCreatedAtDesc("customer-a");

        assertEquals(List.of("install-3", "install-2", "install-1"),
                allJobs.stream().map(CustomerInstallationJobEntity::getInstallationJobId).toList());
        assertEquals(List.of("install-2", "install-1"),
                customerJobs.stream().map(CustomerInstallationJobEntity::getInstallationJobId).toList());
    }

    @Test
    void ivrStepRepositoryOrdersStepsByStepOrderAscending() {
        entityManager.persist(ivrStep("step-3", "flow-a", 3));
        entityManager.persist(ivrStep("step-1", "flow-a", 1));
        entityManager.persist(ivrStep("step-2", "flow-a", 2));
        entityManager.persist(ivrStep("step-x", "flow-b", 1));
        entityManager.flush();
        entityManager.clear();

        List<IvrStepEntity> steps = ivrStepRepository.findByIvrFlowIdOrderByStepOrderAsc("flow-a");

        assertEquals(List.of("step-1", "step-2", "step-3"),
                steps.stream().map(IvrStepEntity::getIvrStepId).toList());
    }

    @Test
    void costConfigurationRepositoryFindsCustomerSpecificConfiguration() {
        entityManager.persist(costConfiguration("config-default", null));
        entityManager.persist(costConfiguration("config-customer", "customer-a"));
        entityManager.flush();
        entityManager.clear();

        CostConfigurationEntity configuration = costConfigurationRepository.findByCustomerId("customer-a").orElseThrow();

        assertEquals("config-customer", configuration.getConfigurationId());
    }

    @Test
    void quoteSnapshotRepositoryFindsOrderedAndPreviousSnapshots() throws InterruptedException {
        QuoteSnapshotEntity first = entityManager.persistFlushFind(quoteSnapshot("quote-1", "install-1", "customer-a"));
        pauseForClockTick();
        QuoteSnapshotEntity second = entityManager.persistFlushFind(quoteSnapshot("quote-2", "install-1", "customer-a"));
        pauseForClockTick();
        entityManager.persistFlushFind(quoteSnapshot("quote-3", "install-1", "customer-b"));
        entityManager.clear();

        List<QuoteSnapshotEntity> customerSnapshots = quoteSnapshotRepository
                .findByInstallationJobIdAndCustomerIdOrderByCreatedAtDesc("install-1", "customer-a");
        QuoteSnapshotEntity previousSnapshot = quoteSnapshotRepository
                .findFirstByInstallationJobIdAndCustomerIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                        "install-1",
                        "customer-a",
                        second.getCreatedAt()
                )
                .orElseThrow();

        assertEquals(List.of("quote-2", "quote-1"),
                customerSnapshots.stream().map(QuoteSnapshotEntity::getQuoteSnapshotId).toList());
        assertEquals("quote-1", previousSnapshot.getQuoteSnapshotId());
        assertTrue(first.getCreatedAt().isBefore(second.getCreatedAt()));
    }

    @Test
    void quoteSnapshotRepositorySupportsInstallationCustomerAndGlobalViews() throws InterruptedException {
        QuoteSnapshotEntity first = entityManager.persistFlushFind(quoteSnapshot("quote-10", "install-10", "customer-x"));
        pauseForClockTick();
        QuoteSnapshotEntity second = entityManager.persistFlushFind(quoteSnapshot("quote-11", "install-10", "customer-x"));
        pauseForClockTick();
        QuoteSnapshotEntity third = entityManager.persistFlushFind(quoteSnapshot("quote-12", "install-20", "customer-x"));
        pauseForClockTick();
        QuoteSnapshotEntity fourth = entityManager.persistFlushFind(quoteSnapshot("quote-13", "install-10", "customer-y"));
        entityManager.clear();

        List<QuoteSnapshotEntity> installationSnapshots = quoteSnapshotRepository
                .findByInstallationJobIdOrderByCreatedAtDesc("install-10");
        List<QuoteSnapshotEntity> customerSnapshots = quoteSnapshotRepository
                .findByCustomerIdOrderByCreatedAtDesc("customer-x");
        List<QuoteSnapshotEntity> allSnapshots = quoteSnapshotRepository.findAllByOrderByCreatedAtDesc();
        QuoteSnapshotEntity previousInstallationSnapshot = quoteSnapshotRepository
                .findFirstByInstallationJobIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                        "install-10",
                        fourth.getCreatedAt()
                )
                .orElseThrow();

        assertEquals(List.of("quote-13", "quote-11", "quote-10"),
                installationSnapshots.stream().map(QuoteSnapshotEntity::getQuoteSnapshotId).toList());
        assertEquals(List.of("quote-12", "quote-11", "quote-10"),
                customerSnapshots.stream().map(QuoteSnapshotEntity::getQuoteSnapshotId).toList());
        assertEquals(List.of("quote-13", "quote-12", "quote-11", "quote-10"),
                allSnapshots.stream().map(QuoteSnapshotEntity::getQuoteSnapshotId).toList());
        assertEquals("quote-11", previousInstallationSnapshot.getQuoteSnapshotId());
        assertNotNull(first.getCreatedAt());
        assertTrue(first.getCreatedAt().isBefore(second.getCreatedAt()));
        assertTrue(second.getCreatedAt().isBefore(third.getCreatedAt()));
        assertTrue(third.getCreatedAt().isBefore(fourth.getCreatedAt()));
    }

    private AgentEntity agent(String agentId, AgentStatus status) {
        AgentEntity entity = new AgentEntity();
        entity.setAgentId(agentId);
        entity.setAgentName("Agent " + agentId);
        entity.setChannel("PJSIP/" + agentId);
        entity.setExtensionNumber(agentId.substring(agentId.length() - 1) + "001");
        entity.setSipUsername(agentId);
        entity.setSipPassword("secret");
        entity.setStatus(status);
        return entity;
    }

    private LeadEntity lead(String leadId, String campaignId, LeadStatus status) {
        LeadEntity entity = new LeadEntity();
        entity.setLeadId(leadId);
        entity.setCampaignId(campaignId);
        entity.setCustomerNumber("+1555" + leadId.substring(leadId.length() - 1) + "000");
        entity.setStatus(status);
        entity.setAttempts(0);
        return entity;
    }

    private CallSessionEntity callSession(String callSessionId, String campaignId) {
        CallSessionEntity entity = new CallSessionEntity();
        entity.setCallSessionId(callSessionId);
        entity.setCampaignId(campaignId);
        entity.setLeadId("lead-" + callSessionId);
        entity.setProvider("ASTERISK");
        entity.setCustomerNumber("+15550001");
        entity.setAgentId("agent-1");
        entity.setAgentChannel("PJSIP/1001");
        entity.setIvrFlowId("ivr-1");
        entity.setCallMode("AGENT_ASSISTED");
        entity.setStatus("CALL_CREATED");
        entity.setLastEventType("CALL_CREATED");
        entity.setLastEventAt(Instant.now());
        return entity;
    }

    private AsteriskDeploymentJobEntity asteriskDeploymentJob(String deploymentJobId, String packageId) {
        AsteriskDeploymentJobEntity entity = new AsteriskDeploymentJobEntity();
        entity.setDeploymentJobId(deploymentJobId);
        entity.setPackageId(packageId);
        entity.setPackageType("softphone");
        entity.setClientType("WEBRTC");
        entity.setStatus(DeploymentExecutionStatus.DEPLOYED);
        entity.setDryRun(false);
        entity.setDeployed(true);
        entity.setHost("pbx.internal");
        entity.setPort(22);
        entity.setRemoteBaseDirectory("/tmp/vantage");
        entity.setRemotePackageDirectory("/tmp/vantage/" + packageId);
        entity.setTargetDirectory("/etc/asterisk/generated");
        entity.setCommandsJson("[\"scp\"]");
        entity.setBundledFilesJson("[\"extensions.conf\"]");
        entity.setAgentIdsJson("[\"agent-1\"]");
        entity.setGeneratedAt(Instant.now());
        entity.setExecutedAt(Instant.now());
        entity.setMessage("completed");
        return entity;
    }

    private CallEventEntity callEvent(String eventId, String callSessionId, String campaignId, String agentId, Instant eventTimestamp) {
        CallEventEntity entity = new CallEventEntity();
        entity.setEventId(eventId);
        entity.setCallSessionId(callSessionId);
        entity.setCallLegId("leg-" + eventId);
        entity.setEventType("CALL_CREATED");
        entity.setEventTimestamp(eventTimestamp);
        entity.setProvider("ASTERISK");
        entity.setProviderCallId("provider-" + eventId);
        entity.setLegType("customer");
        entity.setCampaignId(campaignId);
        entity.setLeadId("lead-" + eventId);
        entity.setAgentId(agentId);
        entity.setPayloadJson("{\"event\":\"" + eventId + "\"}");
        return entity;
    }

    private CustomerInstallationJobEntity customerInstallationJob(String installationJobId, String customerId) {
        CustomerInstallationJobEntity entity = new CustomerInstallationJobEntity();
        entity.setInstallationJobId(installationJobId);
        entity.setInstallationName("Install " + installationJobId);
        entity.setCustomerId(customerId);
        entity.setClientType("WEBRTC");
        entity.setStatus(InstallationJobStatus.COMPLETED);
        entity.setDryRun(false);
        entity.setDeployAfterProvision(true);
        entity.setPerformRemoteChecks(true);
        entity.setAgentCount(5);
        entity.setPackageId("pkg-" + installationJobId);
        entity.setDeploymentJobId("deploy-" + installationJobId);
        entity.setRequestJson("{\"job\":\"" + installationJobId + "\"}");
        entity.setProvisionedAgentsJson("[]");
        entity.setProvisionedAgentIdsJson("[]");
        entity.setPreflightJson("{}");
        entity.setDeploymentJson("{}");
        entity.setCompletedAt(Instant.now());
        entity.setMessage("done");
        return entity;
    }

    private IvrStepEntity ivrStep(String ivrStepId, String ivrFlowId, int stepOrder) {
        IvrStepEntity entity = new IvrStepEntity();
        entity.setIvrStepId(ivrStepId);
        entity.setIvrFlowId(ivrFlowId);
        entity.setStepOrder(stepOrder);
        entity.setStepType(IvrStepType.PLAY_PROMPT);
        entity.setPromptSourceType(PromptSourceType.TEXT_TO_SPEECH);
        entity.setPromptValue("Welcome");
        entity.setDtmfMappingsJson("{\"1\":\"sales\"}");
        entity.setTargetAgentChannel("PJSIP/1001");
        entity.setFallbackAction("hangup");
        return entity;
    }

    private CostConfigurationEntity costConfiguration(String configurationId, String customerId) {
        CostConfigurationEntity entity = new CostConfigurationEntity();
        entity.setConfigurationId(configurationId);
        entity.setCustomerId(customerId);
        entity.setAsteriskServerMonthlyCost(100.0);
        entity.setAppServerMonthlyCost(50.0);
        entity.setEbsMonthlyCost(20.0);
        entity.setSnapshotMonthlyCost(10.0);
        entity.setVoiceMinuteCost(0.05);
        entity.setTtsUnitCost(0.01);
        entity.setSttMinuteCost(0.02);
        entity.setRecordingGbCost(0.03);
        return entity;
    }

    private QuoteSnapshotEntity quoteSnapshot(String snapshotId, String installationJobId, String customerId) {
        QuoteSnapshotEntity entity = new QuoteSnapshotEntity();
        entity.setQuoteSnapshotId(snapshotId);
        entity.setInstallationJobId(installationJobId);
        entity.setCustomerId(customerId);
        entity.setConfigurationId("config-" + snapshotId);
        entity.setRequestJson("{\"request\":\"" + snapshotId + "\"}");
        entity.setSummaryJson("{\"summary\":\"" + snapshotId + "\"}");
        entity.setFilePath("exports/" + snapshotId + ".json");
        return entity;
    }

    private void pauseForClockTick() throws InterruptedException {
        Thread.sleep(20L);
    }
}
