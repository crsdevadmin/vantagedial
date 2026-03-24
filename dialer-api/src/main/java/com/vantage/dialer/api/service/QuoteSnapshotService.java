package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CommercialAssumptionsResponse;
import com.vantage.dialer.api.dto.CommercialAssumptionsDeltaResponse;
import com.vantage.dialer.api.dto.CostEstimateRequest;
import com.vantage.dialer.api.dto.CostEstimateDeltaResponse;
import com.vantage.dialer.api.dto.InstallationQuoteSummaryResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotComparisonResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotComparisonBundleResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotBundleResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotDashboardBundleResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotDashboardResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotDashboardExportResponse;
import com.vantage.dialer.api.dto.QuoteProposalResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotSummaryResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotPreviousComparisonResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotTimelineBundleResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotTimelineEntryResponse;
import com.vantage.dialer.api.dto.QuoteTrendMetricsResponse;
import com.vantage.dialer.api.persistence.model.QuoteSnapshotEntity;
import com.vantage.dialer.api.persistence.repository.QuoteSnapshotRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class QuoteSnapshotService {

    private final QuoteSnapshotRepository repository;
    private final CustomerQuoteService customerQuoteService;
    private final ObjectMapper objectMapper;
    private final Path exportDirectory;

    public QuoteSnapshotService(QuoteSnapshotRepository repository,
                                CustomerQuoteService customerQuoteService,
                                ObjectMapper objectMapper,
                                @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.repository = repository;
        this.customerQuoteService = customerQuoteService;
        this.objectMapper = objectMapper;
        this.exportDirectory = Path.of(exportDirectory).resolve("quotes");
    }

    @Transactional
    public QuoteSnapshotResponse create(String installationJobId, CostEstimateRequest request) {
        InstallationQuoteSummaryResponse summary = customerQuoteService.quote(installationJobId, request);
        QuoteSnapshotEntity entity = new QuoteSnapshotEntity();
        entity.setQuoteSnapshotId(UUID.randomUUID().toString());
        entity.setInstallationJobId(installationJobId);
        entity.setCustomerId(summary.customerId());
        entity.setConfigurationId(summary.estimate().configurationId());
        entity.setRequestJson(writeJson(request));
        entity.setSummaryJson(writeJson(summary));

        try {
            Files.createDirectories(exportDirectory);
            Path output = exportDirectory.resolve(entity.getQuoteSnapshotId() + ".csv");
            Files.write(output, List.of(
                    "installationJobId,customerId,customerName,clientType,agentCount,monthlyCost,sellPrice,marginPercent,configurationId",
                    String.join(",",
                            safe(summary.installationJobId()),
                            safe(summary.customerId()),
                            safe(summary.customerName()),
                            safe(summary.clientType()),
                            String.valueOf(summary.provisionedAgentCount()),
                            String.valueOf(summary.estimate().totalEstimatedCost()),
                            String.valueOf(summary.estimate().suggestedSellPrice()),
                            String.valueOf(summary.estimate().desiredMarginPercent()),
                            safe(summary.estimate().configurationId()))
            ));
            entity.setFilePath(output.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate quote snapshot export", e);
        }

        return toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public QuoteSnapshotResponse get(String quoteSnapshotId) {
        return repository.findById(quoteSnapshotId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Unknown quote snapshot: " + quoteSnapshotId));
    }

    @Transactional(readOnly = true)
    public CommercialAssumptionsResponse getAssumptions(String quoteSnapshotId) {
        QuoteSnapshotEntity entity = repository.findById(quoteSnapshotId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown quote snapshot: " + quoteSnapshotId));
        return readSummary(entity.getSummaryJson()).commercialAssumptions();
    }

    @Transactional(readOnly = true)
    public QuoteSnapshotComparisonResponse compare(String baseQuoteSnapshotId, String targetQuoteSnapshotId) {
        QuoteSnapshotResponse base = get(baseQuoteSnapshotId);
        QuoteSnapshotResponse target = get(targetQuoteSnapshotId);
        return compare(base, target);
    }

    @Transactional(readOnly = true)
    public QuoteSnapshotPreviousComparisonResponse compareToPrevious(String quoteSnapshotId) {
        QuoteSnapshotEntity currentEntity = repository.findById(quoteSnapshotId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown quote snapshot: " + quoteSnapshotId));
        var previousEntity = findPreviousSnapshot(currentEntity);
        if (previousEntity.isEmpty()) {
            return new QuoteSnapshotPreviousComparisonResponse(
                    quoteSnapshotId,
                    null,
                    Instant.now(),
                    false,
                    "No previous quote snapshot found for the same installation/customer scope",
                    null
            );
        }

        QuoteSnapshotComparisonResponse comparison = compare(previousEntity.get().getQuoteSnapshotId(), quoteSnapshotId);
        return new QuoteSnapshotPreviousComparisonResponse(
                quoteSnapshotId,
                previousEntity.get().getQuoteSnapshotId(),
                comparison.comparedAt(),
                true,
                "Comparison generated successfully",
                comparison
        );
    }

    @Transactional(readOnly = true)
    public QuoteSnapshotComparisonBundleResponse generatePreviousComparisonBundle(String quoteSnapshotId) {
        QuoteSnapshotPreviousComparisonResponse previousComparison = compareToPrevious(quoteSnapshotId);
        Instant generatedAt = Instant.now();

        try {
            Path bundleDirectory = exportDirectory.resolve(quoteSnapshotId).resolve("comparison-to-previous");
            Files.createDirectories(bundleDirectory);

            Path comparisonJsonPath = bundleDirectory.resolve("comparison.json");
            Path summaryMarkdownPath = bundleDirectory.resolve("comparison-summary.md");
            Path readmePath = bundleDirectory.resolve("README.txt");

            Files.writeString(comparisonJsonPath, writeJson(previousComparison));
            Files.writeString(summaryMarkdownPath, buildComparisonMarkdown(previousComparison, generatedAt));
            Files.writeString(readmePath, buildComparisonReadme(previousComparison, generatedAt));

            return new QuoteSnapshotComparisonBundleResponse(
                    quoteSnapshotId,
                    previousComparison.previousQuoteSnapshotId(),
                    previousComparison.comparisonAvailable(),
                    bundleDirectory.toAbsolutePath().toString(),
                    comparisonJsonPath.toAbsolutePath().toString(),
                    summaryMarkdownPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate quote snapshot comparison bundle", e);
        }
    }

    @Transactional(readOnly = true)
    public List<QuoteSnapshotTimelineEntryResponse> timeline(String installationJobId, String customerId) {
        List<QuoteSnapshotResponse> snapshots = list(installationJobId, customerId);
        List<QuoteSnapshotTimelineEntryResponse> timeline = new ArrayList<>();
        QuoteSnapshotResponse previous = null;
        for (int index = snapshots.size() - 1; index >= 0; index--) {
            QuoteSnapshotResponse current = snapshots.get(index);
            if (previous == null) {
                timeline.add(new QuoteSnapshotTimelineEntryResponse(
                        current,
                        null,
                        false,
                        null,
                        null
                ));
            } else {
                QuoteSnapshotComparisonResponse comparison = compare(previous, current);
                timeline.add(new QuoteSnapshotTimelineEntryResponse(
                        current,
                        previous.quoteSnapshotId(),
                        true,
                        comparison.assumptionsDelta(),
                        comparison.estimateDelta()
                ));
            }
            previous = current;
        }
        java.util.Collections.reverse(timeline);
        return timeline;
    }

    @Transactional(readOnly = true)
    public QuoteSnapshotSummaryResponse summary(String installationJobId, String customerId) {
        List<QuoteSnapshotResponse> snapshots = list(installationJobId, customerId);
        if (snapshots.isEmpty()) {
            return new QuoteSnapshotSummaryResponse(
                    installationJobId,
                    customerId,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        QuoteSnapshotResponse latest = snapshots.get(0);
        QuoteSnapshotPreviousComparisonResponse latestComparison = compareToPrevious(latest.quoteSnapshotId());
        Double minimumSuggestedSellPrice = snapshots.stream()
                .map(snapshot -> snapshot.summary().estimate().suggestedSellPrice())
                .min(Double::compareTo)
                .orElse(null);
        Double maximumSuggestedSellPrice = snapshots.stream()
                .map(snapshot -> snapshot.summary().estimate().suggestedSellPrice())
                .max(Double::compareTo)
                .orElse(null);

        Double averageSuggestedSellPrice = snapshots.stream()
                .map(snapshot -> snapshot.summary().estimate().suggestedSellPrice())
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0d);
        Double averageEstimatedCost = snapshots.stream()
                .map(snapshot -> snapshot.summary().estimate().totalEstimatedCost())
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0d);
        Double latestSuggestedSellPriceDelta = latestComparison.comparisonAvailable() && latestComparison.comparison() != null
                ? latestComparison.comparison().estimateDelta().suggestedSellPriceDelta()
                : null;
        Double latestEstimatedCostDelta = latestComparison.comparisonAvailable() && latestComparison.comparison() != null
                ? latestComparison.comparison().estimateDelta().totalEstimatedCostDelta()
                : null;

        return new QuoteSnapshotSummaryResponse(
                installationJobId,
                customerId,
                snapshots.size(),
                latest,
                latestComparison,
                minimumSuggestedSellPrice,
                maximumSuggestedSellPrice,
                latest.summary().estimate().suggestedSellPrice(),
                latest.summary().estimate().totalEstimatedCost(),
                new QuoteTrendMetricsResponse(
                        averageSuggestedSellPrice,
                        averageEstimatedCost,
                        latestSuggestedSellPriceDelta,
                        latestEstimatedCostDelta,
                        trendDirection(latestSuggestedSellPriceDelta),
                        trendDirection(latestEstimatedCostDelta)
                )
        );
    }

    @Transactional(readOnly = true)
    public QuoteSnapshotTimelineBundleResponse generateTimelineBundle(String installationJobId, String customerId) {
        List<QuoteSnapshotTimelineEntryResponse> timeline = timeline(installationJobId, customerId);
        Instant generatedAt = Instant.now();

        try {
            String scopeDirectoryName = buildTimelineScopeDirectoryName(installationJobId, customerId);
            Path bundleDirectory = exportDirectory.resolve(scopeDirectoryName).resolve("timeline");
            Files.createDirectories(bundleDirectory);

            Path timelineJsonPath = bundleDirectory.resolve("timeline.json");
            Path timelineCsvPath = bundleDirectory.resolve("timeline.csv");
            Path summaryMarkdownPath = bundleDirectory.resolve("timeline-summary.md");
            Path readmePath = bundleDirectory.resolve("README.txt");

            Files.writeString(timelineJsonPath, writeJson(timeline));
            Files.write(timelineCsvPath, buildTimelineCsv(timeline));
            Files.writeString(summaryMarkdownPath, buildTimelineMarkdown(timeline, installationJobId, customerId, generatedAt));
            Files.writeString(readmePath, buildTimelineReadme(timeline, installationJobId, customerId, generatedAt));

            return new QuoteSnapshotTimelineBundleResponse(
                    installationJobId,
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    timelineJsonPath.toAbsolutePath().toString(),
                    timelineCsvPath.toAbsolutePath().toString(),
                    summaryMarkdownPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate quote snapshot timeline bundle", e);
        }
    }

    @Transactional(readOnly = true)
    public QuoteSnapshotDashboardResponse dashboard(String installationJobId, String customerId) {
        return new QuoteSnapshotDashboardResponse(
                installationJobId,
                customerId,
                Instant.now(),
                summary(installationJobId, customerId),
                timeline(installationJobId, customerId)
        );
    }

    @Transactional(readOnly = true)
    public QuoteSnapshotDashboardExportResponse exportDashboard(String installationJobId, String customerId) {
        QuoteSnapshotDashboardResponse dashboard = dashboard(installationJobId, customerId);
        Instant generatedAt = dashboard.generatedAt();

        try {
            String scopeDirectoryName = buildTimelineScopeDirectoryName(installationJobId, customerId);
            Path exportPath = exportDirectory.resolve(scopeDirectoryName).resolve("dashboard-export");
            Files.createDirectories(exportPath);

            Path dashboardJsonPath = exportPath.resolve("dashboard.json");
            Path dashboardCsvPath = exportPath.resolve("dashboard.csv");
            Path dashboardHtmlPath = exportPath.resolve("dashboard.html");
            Path readmePath = exportPath.resolve("README.txt");

            Files.writeString(dashboardJsonPath, writeJson(dashboard));
            Files.write(dashboardCsvPath, buildDashboardCsv(dashboard));
            Files.writeString(dashboardHtmlPath, buildDashboardHtml(dashboard.summary(), dashboard.timeline(), generatedAt));
            Files.writeString(readmePath, buildDashboardExportReadme(dashboard, generatedAt));

            return new QuoteSnapshotDashboardExportResponse(
                    installationJobId,
                    customerId,
                    exportPath.toAbsolutePath().toString(),
                    dashboardJsonPath.toAbsolutePath().toString(),
                    dashboardCsvPath.toAbsolutePath().toString(),
                    dashboardHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export quote dashboard", e);
        }
    }

    @Transactional(readOnly = true)
    public QuoteSnapshotDashboardBundleResponse generateDashboardBundle(String installationJobId, String customerId) {
        QuoteSnapshotDashboardResponse dashboard = dashboard(installationJobId, customerId);
        QuoteSnapshotSummaryResponse summary = dashboard.summary();
        List<QuoteSnapshotTimelineEntryResponse> timeline = dashboard.timeline();
        Instant generatedAt = dashboard.generatedAt();

        try {
            String scopeDirectoryName = buildTimelineScopeDirectoryName(installationJobId, customerId);
            Path bundleDirectory = exportDirectory.resolve(scopeDirectoryName).resolve("dashboard");
            Files.createDirectories(bundleDirectory);

            Path dashboardJsonPath = bundleDirectory.resolve("dashboard.json");
            Path summaryJsonPath = bundleDirectory.resolve("summary.json");
            Path timelineJsonPath = bundleDirectory.resolve("timeline.json");
            Path dashboardMarkdownPath = bundleDirectory.resolve("dashboard.md");
            Path dashboardHtmlPath = bundleDirectory.resolve("dashboard.html");
            Path readmePath = bundleDirectory.resolve("README.txt");

            Files.writeString(dashboardJsonPath, writeJson(dashboard));
            Files.writeString(summaryJsonPath, writeJson(summary));
            Files.writeString(timelineJsonPath, writeJson(timeline));
            Files.writeString(dashboardMarkdownPath, buildDashboardMarkdown(summary, timeline, generatedAt));
            Files.writeString(dashboardHtmlPath, buildDashboardHtml(summary, timeline, generatedAt));
            Files.writeString(readmePath, buildDashboardReadme(summary, timeline.size(), generatedAt));

            return new QuoteSnapshotDashboardBundleResponse(
                    installationJobId,
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    summaryJsonPath.toAbsolutePath().toString(),
                    timelineJsonPath.toAbsolutePath().toString(),
                    dashboardMarkdownPath.toAbsolutePath().toString(),
                    dashboardHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate quote snapshot dashboard bundle", e);
        }
    }

    @Transactional(readOnly = true)
    public QuoteSnapshotBundleResponse generateBundle(String quoteSnapshotId) {
        QuoteSnapshotEntity entity = repository.findById(quoteSnapshotId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown quote snapshot: " + quoteSnapshotId));
        InstallationQuoteSummaryResponse summary = readSummary(entity.getSummaryJson());
        Instant generatedAt = Instant.now();

        try {
            Path bundleDirectory = exportDirectory.resolve(entity.getQuoteSnapshotId());
            Files.createDirectories(bundleDirectory);

            Path summaryJsonPath = bundleDirectory.resolve("quote-summary.json");
            Path assumptionsJsonPath = bundleDirectory.resolve("quote-assumptions.json");
            Path requestJsonPath = bundleDirectory.resolve("quote-request.json");
            Path csvPath = bundleDirectory.resolve("quote-summary.csv");
            Path readmePath = bundleDirectory.resolve("README.txt");

            Files.writeString(summaryJsonPath, entity.getSummaryJson());
            Files.writeString(assumptionsJsonPath, writeJson(summary.commercialAssumptions()));
            Files.writeString(requestJsonPath, entity.getRequestJson());
            Files.write(csvPath, List.of(
                    "installationJobId,customerId,customerName,clientType,agentCount,monthlyCost,sellPrice,marginPercent,configurationId",
                    String.join(",",
                            safe(summary.installationJobId()),
                            safe(summary.customerId()),
                            safe(summary.customerName()),
                            safe(summary.clientType()),
                            String.valueOf(summary.provisionedAgentCount()),
                            String.valueOf(summary.estimate().totalEstimatedCost()),
                            String.valueOf(summary.estimate().suggestedSellPrice()),
                            String.valueOf(summary.estimate().desiredMarginPercent()),
                            safe(summary.estimate().configurationId()))
            ));
            Files.writeString(readmePath, buildReadme(summary, generatedAt));

            return new QuoteSnapshotBundleResponse(
                    entity.getQuoteSnapshotId(),
                    bundleDirectory.toAbsolutePath().toString(),
                    summaryJsonPath.toAbsolutePath().toString(),
                    assumptionsJsonPath.toAbsolutePath().toString(),
                    requestJsonPath.toAbsolutePath().toString(),
                    csvPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate quote snapshot bundle", e);
        }
    }

    @Transactional(readOnly = true)
    public QuoteProposalResponse generateProposal(String quoteSnapshotId) {
        QuoteSnapshotEntity entity = repository.findById(quoteSnapshotId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown quote snapshot: " + quoteSnapshotId));
        InstallationQuoteSummaryResponse summary = readSummary(entity.getSummaryJson());
        Instant generatedAt = Instant.now();

        try {
            Path proposalDirectory = exportDirectory.resolve(entity.getQuoteSnapshotId()).resolve("proposal");
            Files.createDirectories(proposalDirectory);

            Path proposalMarkdownPath = proposalDirectory.resolve("customer-proposal.md");
            Path proposalHtmlPath = proposalDirectory.resolve("customer-proposal.html");
            Path assumptionsJsonPath = proposalDirectory.resolve("commercial-assumptions.json");
            Path pricingBreakdownJsonPath = proposalDirectory.resolve("pricing-breakdown.json");

            Files.writeString(proposalMarkdownPath, buildProposalMarkdown(summary, generatedAt));
            Files.writeString(proposalHtmlPath, buildProposalHtml(summary, generatedAt));
            Files.writeString(assumptionsJsonPath, writeJson(summary.commercialAssumptions()));
            Files.writeString(pricingBreakdownJsonPath, entity.getSummaryJson());

            return new QuoteProposalResponse(
                    entity.getQuoteSnapshotId(),
                    proposalDirectory.toAbsolutePath().toString(),
                    proposalMarkdownPath.toAbsolutePath().toString(),
                    proposalHtmlPath.toAbsolutePath().toString(),
                    assumptionsJsonPath.toAbsolutePath().toString(),
                    pricingBreakdownJsonPath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate quote proposal artifact", e);
        }
    }

    @Transactional(readOnly = true)
    public List<QuoteSnapshotResponse> list(String installationJobId, String customerId) {
        boolean hasInstallation = installationJobId != null && !installationJobId.isBlank();
        boolean hasCustomer = customerId != null && !customerId.isBlank();

        List<QuoteSnapshotEntity> entities;
        if (hasInstallation && hasCustomer) {
            entities = repository.findByInstallationJobIdAndCustomerIdOrderByCreatedAtDesc(installationJobId, customerId);
        } else if (hasInstallation) {
            entities = repository.findByInstallationJobIdOrderByCreatedAtDesc(installationJobId);
        } else if (hasCustomer) {
            entities = repository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        } else {
            entities = repository.findAllByOrderByCreatedAtDesc();
        }
        return entities.stream().map(this::toResponse).toList();
    }

    private QuoteSnapshotResponse toResponse(QuoteSnapshotEntity entity) {
        return new QuoteSnapshotResponse(
                entity.getQuoteSnapshotId(),
                entity.getInstallationJobId(),
                entity.getCustomerId(),
                entity.getConfigurationId(),
                entity.getFilePath(),
                entity.getCreatedAt(),
                readSummary(entity.getSummaryJson())
        );
    }

    private QuoteSnapshotComparisonResponse compare(QuoteSnapshotResponse base, QuoteSnapshotResponse target) {
        return new QuoteSnapshotComparisonResponse(
                base.quoteSnapshotId(),
                target.quoteSnapshotId(),
                Instant.now(),
                base,
                target,
                buildAssumptionsDelta(base.summary().commercialAssumptions(), target.summary().commercialAssumptions()),
                buildEstimateDelta(base.summary().estimate(), target.summary().estimate())
        );
    }

    private java.util.Optional<QuoteSnapshotEntity> findPreviousSnapshot(QuoteSnapshotEntity entity) {
        if (entity.getCustomerId() != null && !entity.getCustomerId().isBlank()) {
            return repository.findFirstByInstallationJobIdAndCustomerIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                    entity.getInstallationJobId(),
                    entity.getCustomerId(),
                    entity.getCreatedAt());
        }
        return repository.findFirstByInstallationJobIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                entity.getInstallationJobId(),
                entity.getCreatedAt());
    }

    private InstallationQuoteSummaryResponse readSummary(String json) {
        try {
            return objectMapper.readValue(json, InstallationQuoteSummaryResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize quote snapshot summary", e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize quote snapshot payload", e);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String buildReadme(InstallationQuoteSummaryResponse summary, Instant generatedAt) {
        return String.join(System.lineSeparator(),
                "Vantage Dialer Quote Snapshot Bundle",
                "",
                "Generated At: " + generatedAt,
                "Installation Job Id: " + safe(summary.installationJobId()),
                "Customer Id: " + safe(summary.customerId()),
                "Customer Name: " + safe(summary.customerName()),
                "Client Type: " + safe(summary.clientType()),
                "Provisioned Agent Count: " + summary.provisionedAgentCount(),
                "Estimated Monthly Cost: " + summary.estimate().totalEstimatedCost(),
                "Suggested Sell Price: " + summary.estimate().suggestedSellPrice(),
                "Desired Margin Percent: " + summary.estimate().desiredMarginPercent(),
                "",
                "Files in this bundle:",
                "- quote-summary.json",
                "- quote-assumptions.json",
                "- quote-request.json",
                "- quote-summary.csv");
    }

    private String buildComparisonReadme(QuoteSnapshotPreviousComparisonResponse comparison, Instant generatedAt) {
        return String.join(System.lineSeparator(),
                "Vantage Dialer Quote Snapshot Comparison Bundle",
                "",
                "Generated At: " + generatedAt,
                "Current Quote Snapshot Id: " + safe(comparison.quoteSnapshotId()),
                "Previous Quote Snapshot Id: " + safe(comparison.previousQuoteSnapshotId()),
                "Comparison Available: " + comparison.comparisonAvailable(),
                "Message: " + safe(comparison.message()),
                "",
                "Files in this bundle:",
                "- comparison.json",
                "- comparison-summary.md");
    }

    private String buildTimelineReadme(List<QuoteSnapshotTimelineEntryResponse> timeline,
                                       String installationJobId,
                                       String customerId,
                                       Instant generatedAt) {
        return String.join(System.lineSeparator(),
                "Vantage Dialer Quote Snapshot Timeline Bundle",
                "",
                "Generated At: " + generatedAt,
                "Installation Job Id: " + safe(installationJobId),
                "Customer Id: " + safe(customerId),
                "Timeline Entry Count: " + timeline.size(),
                "",
                "Files in this bundle:",
                "- timeline.json",
                "- timeline.csv",
                "- timeline-summary.md");
    }

    private String buildDashboardReadme(QuoteSnapshotSummaryResponse summary,
                                        int timelineEntryCount,
                                        Instant generatedAt) {
        return String.join(System.lineSeparator(),
                "Vantage Dialer Quote Snapshot Dashboard Bundle",
                "",
                "Generated At: " + generatedAt,
                "Installation Job Id: " + safe(summary.installationJobId()),
                "Customer Id: " + safe(summary.customerId()),
                "Snapshot Count: " + summary.snapshotCount(),
                "Timeline Entry Count: " + timelineEntryCount,
                "",
                "Files in this bundle:",
                "- dashboard.json",
                "- summary.json",
                "- timeline.json",
                "- dashboard.md",
                "- dashboard.html");
    }

    private String buildDashboardExportReadme(QuoteSnapshotDashboardResponse dashboard, Instant generatedAt) {
        return String.join(System.lineSeparator(),
                "Vantage Dialer Quote Snapshot Dashboard Export",
                "",
                "Generated At: " + generatedAt,
                "Installation Job Id: " + safe(dashboard.installationJobId()),
                "Customer Id: " + safe(dashboard.customerId()),
                "Snapshot Count: " + (dashboard.summary() == null ? 0 : dashboard.summary().snapshotCount()),
                "",
                "Files in this export:",
                "- dashboard.json",
                "- dashboard.csv",
                "- dashboard.html");
    }

    private String buildComparisonMarkdown(QuoteSnapshotPreviousComparisonResponse previousComparison, Instant generatedAt) {
        List<String> lines = new ArrayList<>();
        lines.add("# Quote Comparison Summary");
        lines.add("");
        lines.add("- Generated: " + generatedAt);
        lines.add("- Current quote snapshot: " + safe(previousComparison.quoteSnapshotId()));
        lines.add("- Previous quote snapshot: " + safe(previousComparison.previousQuoteSnapshotId()));
        lines.add("- Comparison available: " + previousComparison.comparisonAvailable());
        lines.add("- Status: " + safe(previousComparison.message()));
        if (!previousComparison.comparisonAvailable() || previousComparison.comparison() == null) {
            return String.join(System.lineSeparator(), lines);
        }

        var comparison = previousComparison.comparison();
        var estimateDelta = comparison.estimateDelta();
        var assumptionsDelta = comparison.assumptionsDelta();

        lines.add("");
        lines.add("## Commercial Assumption Changes");
        lines.add("");
        lines.add("- Monthly call minutes delta: " + assumptionsDelta.monthlyCallMinutesDelta());
        lines.add("- Monthly TTS units delta: " + assumptionsDelta.monthlyTtsUnitsDelta());
        lines.add("- Monthly STT minutes delta: " + assumptionsDelta.monthlySttMinutesDelta());
        lines.add("- Monthly recording GB delta: " + assumptionsDelta.monthlyRecordingGbDelta());
        lines.add("- Agent count delta: " + assumptionsDelta.agentCountDelta());
        lines.add("- Concurrent channels delta: " + assumptionsDelta.concurrentChannelsDelta());
        lines.add("- Desired margin percent delta: " + assumptionsDelta.desiredMarginPercentDelta());
        lines.add("");
        lines.add("## Commercial Output Changes");
        lines.add("");
        lines.add("- Fixed infrastructure cost delta: " + estimateDelta.fixedInfrastructureCostDelta());
        lines.add("- Variable usage cost delta: " + estimateDelta.variableUsageCostDelta());
        lines.add("- Total estimated cost delta: " + estimateDelta.totalEstimatedCostDelta());
        lines.add("- Suggested sell price delta: " + estimateDelta.suggestedSellPriceDelta());
        lines.add("- Desired margin percent delta: " + estimateDelta.desiredMarginPercentDelta());
        return String.join(System.lineSeparator(), lines);
    }

    private String buildTimelineMarkdown(List<QuoteSnapshotTimelineEntryResponse> timeline,
                                         String installationJobId,
                                         String customerId,
                                         Instant generatedAt) {
        List<String> lines = new ArrayList<>();
        lines.add("# Quote Timeline Summary");
        lines.add("");
        lines.add("- Generated: " + generatedAt);
        lines.add("- Installation Job Id: " + safe(installationJobId));
        lines.add("- Customer Id: " + safe(customerId));
        lines.add("- Entry Count: " + timeline.size());
        lines.add("");
        lines.add("## Entries");
        for (QuoteSnapshotTimelineEntryResponse entry : timeline) {
            lines.add("");
            lines.add("### " + safe(entry.snapshot().quoteSnapshotId()));
            lines.add("- Created At: " + entry.snapshot().createdAt());
            lines.add("- Previous Quote Snapshot: " + safe(entry.previousQuoteSnapshotId()));
            lines.add("- Comparison Available: " + entry.comparisonAvailable());
            lines.add("- Suggested Sell Price: " + entry.snapshot().summary().estimate().suggestedSellPrice());
            lines.add("- Total Estimated Cost: " + entry.snapshot().summary().estimate().totalEstimatedCost());
            if (entry.comparisonAvailable() && entry.estimateDelta() != null) {
                lines.add("- Sell Price Delta: " + entry.estimateDelta().suggestedSellPriceDelta());
                lines.add("- Total Cost Delta: " + entry.estimateDelta().totalEstimatedCostDelta());
            }
        }
        return String.join(System.lineSeparator(), lines);
    }

    private String buildDashboardMarkdown(QuoteSnapshotSummaryResponse summary,
                                          List<QuoteSnapshotTimelineEntryResponse> timeline,
                                          Instant generatedAt) {
        List<String> lines = new ArrayList<>();
        lines.add("# Quote Dashboard");
        lines.add("");
        lines.add("- Generated: " + generatedAt);
        lines.add("- Installation Job Id: " + safe(summary.installationJobId()));
        lines.add("- Customer Id: " + safe(summary.customerId()));
        lines.add("- Snapshot Count: " + summary.snapshotCount());
        lines.add("- Latest Suggested Sell Price: " + summary.latestSuggestedSellPrice());
        lines.add("- Latest Estimated Cost: " + summary.latestEstimatedCost());
        lines.add("- Minimum Suggested Sell Price: " + summary.minimumSuggestedSellPrice());
        lines.add("- Maximum Suggested Sell Price: " + summary.maximumSuggestedSellPrice());

        if (summary.latestSnapshot() != null) {
            lines.add("");
            lines.add("## Latest Snapshot");
            lines.add("");
            lines.add("- Quote Snapshot Id: " + safe(summary.latestSnapshot().quoteSnapshotId()));
            lines.add("- Created At: " + summary.latestSnapshot().createdAt());
            lines.add("- Client Type: " + safe(summary.latestSnapshot().summary().clientType()));
            lines.add("- Provisioned Agent Count: " + summary.latestSnapshot().summary().provisionedAgentCount());
        }

        if (summary.latestComparison() != null) {
            lines.add("");
            lines.add("## Latest Comparison");
            lines.add("");
            lines.add("- Comparison Available: " + summary.latestComparison().comparisonAvailable());
            lines.add("- Previous Quote Snapshot Id: " + safe(summary.latestComparison().previousQuoteSnapshotId()));
            lines.add("- Message: " + safe(summary.latestComparison().message()));
        }

        if (summary.trendMetrics() != null) {
            lines.add("");
            lines.add("## Trend Metrics");
            lines.add("");
            lines.add("- Average Suggested Sell Price: " + summary.trendMetrics().averageSuggestedSellPrice());
            lines.add("- Average Estimated Cost: " + summary.trendMetrics().averageEstimatedCost());
            lines.add("- Latest Suggested Sell Price Delta: " + summary.trendMetrics().latestSuggestedSellPriceDelta());
            lines.add("- Latest Estimated Cost Delta: " + summary.trendMetrics().latestEstimatedCostDelta());
            lines.add("- Suggested Sell Price Trend: " + safe(summary.trendMetrics().suggestedSellPriceTrend()));
            lines.add("- Estimated Cost Trend: " + safe(summary.trendMetrics().estimatedCostTrend()));
        }

        lines.add("");
        lines.add("## Timeline Highlights");
        lines.add("");
        lines.add("- Timeline Entry Count: " + timeline.size());
        if (!timeline.isEmpty()) {
            QuoteSnapshotTimelineEntryResponse newest = timeline.get(0);
            QuoteSnapshotTimelineEntryResponse oldest = timeline.get(timeline.size() - 1);
            lines.add("- Newest Quote Snapshot: " + safe(newest.snapshot().quoteSnapshotId()));
            lines.add("- Oldest Quote Snapshot: " + safe(oldest.snapshot().quoteSnapshotId()));
        }
        return String.join(System.lineSeparator(), lines);
    }

    private String buildDashboardHtml(QuoteSnapshotSummaryResponse summary,
                                      List<QuoteSnapshotTimelineEntryResponse> timeline,
                                      Instant generatedAt) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\">")
                .append("<title>Quote Dashboard</title>")
                .append("<style>")
                .append("body{font-family:Segoe UI,Arial,sans-serif;background:#f5f1e8;color:#1f2a30;padding:24px;}")
                .append(".card{background:#fff;border-radius:14px;padding:18px;margin-bottom:16px;box-shadow:0 8px 24px rgba(0,0,0,.08);}")
                .append("h1,h2{margin:0 0 12px;}table{width:100%;border-collapse:collapse;}th,td{padding:10px;border-bottom:1px solid #ddd;text-align:left;}")
                .append(".muted{color:#5d6a70;}")
                .append("</style></head><body>");
        html.append("<h1>Quote Dashboard</h1>");
        html.append("<p class=\"muted\">Generated: ").append(generatedAt).append("</p>");
        html.append("<div class=\"card\"><h2>Summary</h2><table>");
        appendHtmlRow(html, "Installation Job Id", safe(summary.installationJobId()));
        appendHtmlRow(html, "Customer Id", safe(summary.customerId()));
        appendHtmlRow(html, "Snapshot Count", String.valueOf(summary.snapshotCount()));
        appendHtmlRow(html, "Latest Suggested Sell Price", String.valueOf(summary.latestSuggestedSellPrice()));
        appendHtmlRow(html, "Latest Estimated Cost", String.valueOf(summary.latestEstimatedCost()));
        appendHtmlRow(html, "Minimum Suggested Sell Price", String.valueOf(summary.minimumSuggestedSellPrice()));
        appendHtmlRow(html, "Maximum Suggested Sell Price", String.valueOf(summary.maximumSuggestedSellPrice()));
        html.append("</table></div>");

        if (summary.trendMetrics() != null) {
            html.append("<div class=\"card\"><h2>Trend Metrics</h2><table>");
            appendHtmlRow(html, "Average Suggested Sell Price", String.valueOf(summary.trendMetrics().averageSuggestedSellPrice()));
            appendHtmlRow(html, "Average Estimated Cost", String.valueOf(summary.trendMetrics().averageEstimatedCost()));
            appendHtmlRow(html, "Latest Suggested Sell Price Delta", String.valueOf(summary.trendMetrics().latestSuggestedSellPriceDelta()));
            appendHtmlRow(html, "Latest Estimated Cost Delta", String.valueOf(summary.trendMetrics().latestEstimatedCostDelta()));
            appendHtmlRow(html, "Suggested Sell Price Trend", safe(summary.trendMetrics().suggestedSellPriceTrend()));
            appendHtmlRow(html, "Estimated Cost Trend", safe(summary.trendMetrics().estimatedCostTrend()));
            html.append("</table></div>");
        }

        html.append("<div class=\"card\"><h2>Timeline Highlights</h2><table>");
        appendHtmlRow(html, "Timeline Entry Count", String.valueOf(timeline.size()));
        if (!timeline.isEmpty()) {
            appendHtmlRow(html, "Newest Quote Snapshot", safe(timeline.get(0).snapshot().quoteSnapshotId()));
            appendHtmlRow(html, "Oldest Quote Snapshot", safe(timeline.get(timeline.size() - 1).snapshot().quoteSnapshotId()));
        }
        html.append("</table></div></body></html>");
        return html.toString();
    }

    private List<String> buildDashboardCsv(QuoteSnapshotDashboardResponse dashboard) {
        List<String> lines = new ArrayList<>();
        lines.add("installationJobId,customerId,snapshotCount,latestQuoteSnapshotId,latestSuggestedSellPrice,latestEstimatedCost,minimumSuggestedSellPrice,maximumSuggestedSellPrice,averageSuggestedSellPrice,averageEstimatedCost,latestSuggestedSellPriceDelta,latestEstimatedCostDelta,suggestedSellPriceTrend,estimatedCostTrend");
        QuoteSnapshotSummaryResponse summary = dashboard.summary();
        QuoteTrendMetricsResponse trend = summary == null ? null : summary.trendMetrics();
        lines.add(String.join(",",
                safe(dashboard.installationJobId()),
                safe(dashboard.customerId()),
                String.valueOf(summary == null ? 0 : summary.snapshotCount()),
                safe(summary == null || summary.latestSnapshot() == null ? null : summary.latestSnapshot().quoteSnapshotId()),
                String.valueOf(summary == null ? null : summary.latestSuggestedSellPrice()),
                String.valueOf(summary == null ? null : summary.latestEstimatedCost()),
                String.valueOf(summary == null ? null : summary.minimumSuggestedSellPrice()),
                String.valueOf(summary == null ? null : summary.maximumSuggestedSellPrice()),
                String.valueOf(trend == null ? null : trend.averageSuggestedSellPrice()),
                String.valueOf(trend == null ? null : trend.averageEstimatedCost()),
                String.valueOf(trend == null ? null : trend.latestSuggestedSellPriceDelta()),
                String.valueOf(trend == null ? null : trend.latestEstimatedCostDelta()),
                safe(trend == null ? null : trend.suggestedSellPriceTrend()),
                safe(trend == null ? null : trend.estimatedCostTrend())
        ));
        return lines;
    }

    private void appendHtmlRow(StringBuilder html, String label, String value) {
        html.append("<tr><th>").append(label).append("</th><td>").append(value).append("</td></tr>");
    }

    private List<String> buildTimelineCsv(List<QuoteSnapshotTimelineEntryResponse> timeline) {
        List<String> lines = new ArrayList<>();
        lines.add("quoteSnapshotId,createdAt,installationJobId,customerId,previousQuoteSnapshotId,comparisonAvailable,totalEstimatedCost,suggestedSellPrice,fixedInfrastructureCostDelta,variableUsageCostDelta,totalEstimatedCostDelta,suggestedSellPriceDelta");
        for (QuoteSnapshotTimelineEntryResponse entry : timeline) {
            CostEstimateDeltaResponse estimateDelta = entry.estimateDelta();
            lines.add(String.join(",",
                    safe(entry.snapshot().quoteSnapshotId()),
                    String.valueOf(entry.snapshot().createdAt()),
                    safe(entry.snapshot().installationJobId()),
                    safe(entry.snapshot().customerId()),
                    safe(entry.previousQuoteSnapshotId()),
                    String.valueOf(entry.comparisonAvailable()),
                    String.valueOf(entry.snapshot().summary().estimate().totalEstimatedCost()),
                    String.valueOf(entry.snapshot().summary().estimate().suggestedSellPrice()),
                    String.valueOf(estimateDelta == null ? null : estimateDelta.fixedInfrastructureCostDelta()),
                    String.valueOf(estimateDelta == null ? null : estimateDelta.variableUsageCostDelta()),
                    String.valueOf(estimateDelta == null ? null : estimateDelta.totalEstimatedCostDelta()),
                    String.valueOf(estimateDelta == null ? null : estimateDelta.suggestedSellPriceDelta())
            ));
        }
        return lines;
    }

    private String buildTimelineScopeDirectoryName(String installationJobId, String customerId) {
        String installationPart = installationJobId == null || installationJobId.isBlank() ? "all-installations" : installationJobId;
        String customerPart = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
        return "timeline-" + installationPart + "-" + customerPart;
    }

    private String trendDirection(Double delta) {
        if (delta == null) {
            return "UNCHANGED";
        }
        if (delta > 0) {
            return "UP";
        }
        if (delta < 0) {
            return "DOWN";
        }
        return "UNCHANGED";
    }

    private String buildProposalMarkdown(InstallationQuoteSummaryResponse summary, Instant generatedAt) {
        var config = summary.customerConfiguration();
        String title = config == null || config.proposalTitle() == null ? "Vantage Dialer Proposal" : config.proposalTitle();
        String subtitle = config == null || config.proposalSubtitle() == null ? "" : config.proposalSubtitle();
        String brandName = config == null || config.brandDisplayName() == null ? safe(summary.customerName()) : config.brandDisplayName();
        String terms = config == null ? null : config.proposalTerms();
        boolean includePricingBreakdown = proposalFlag(config == null ? null : config.proposalIncludePricingBreakdown(), true);
        List<String> lines = new ArrayList<>();
        lines.add("# " + title);
        if (!subtitle.isBlank()) {
            lines.add("");
            lines.add(subtitle);
        }
        lines.add("");
        lines.add("Generated: " + generatedAt);
        lines.add("");
        lines.add("## Customer");
        lines.add("");
        lines.add("- Customer: " + brandName);
        lines.add("- Customer Id: " + safe(summary.customerId()));
        lines.add("- Installation: " + safe(summary.installationName()));
        lines.add("- Client Type: " + safe(summary.clientType()));
        lines.add("- Provisioned Agents: " + summary.provisionedAgentCount());
        if (summary.commercialAssumptions() != null) {
            lines.add("- Commercial assumptions source: " + safe(summary.commercialAssumptions().source()));
        }
        lines.add("");
        lines.add("## Commercial Summary");
        lines.add("");
        lines.add("- Estimated monthly infrastructure and usage cost: " + summary.estimate().totalEstimatedCost());
        lines.add("- Suggested monthly sell price: " + summary.estimate().suggestedSellPrice());
        lines.add("- Target margin percent: " + summary.estimate().desiredMarginPercent());
        if (summary.commercialAssumptions() != null) {
            lines.add("- Assumed monthly call minutes: " + summary.commercialAssumptions().monthlyCallMinutes());
            lines.add("- Assumed monthly TTS units: " + summary.commercialAssumptions().monthlyTtsUnits());
            lines.add("- Assumed monthly STT minutes: " + summary.commercialAssumptions().monthlySttMinutes());
            lines.add("- Assumed monthly recording GB: " + summary.commercialAssumptions().monthlyRecordingGb());
        }
        lines.add("");
        lines.add("## Included Scope");
        lines.add("");
        lines.add("- Single-tenant outbound dialer deployment");
        lines.addAll(proposalScopeItems(config));
        lines.add("");
        lines.add("## Deployment Notes");
        lines.add("");
        lines.add("- App stack is expected on server B");
        lines.add("- Asterisk is expected on server A");
        lines.add("- Customer-specific connection settings are available in the bootstrap bundle");
        if (terms != null && !terms.isBlank()) {
            lines.add("");
            lines.add("## Terms");
            lines.add("");
            lines.add(terms);
        }
        if (includePricingBreakdown) {
            lines.add("");
            lines.add("## Pricing Breakdown");
            lines.add("");
            lines.add("- Fixed infrastructure cost: " + summary.estimate().fixedInfrastructureCost());
            lines.add("- Variable usage cost: " + summary.estimate().variableUsageCost());
            lines.add("- Pricing configuration id: " + safe(summary.estimate().configurationId()));
        }
        return String.join(System.lineSeparator(), lines);
    }

    private String buildProposalHtml(InstallationQuoteSummaryResponse summary, Instant generatedAt) {
        var config = summary.customerConfiguration();
        String proposalTemplate = config == null || config.proposalTemplate() == null ? "STANDARD" : config.proposalTemplate();
        String title = config == null || config.proposalTitle() == null ? "Vantage Dialer Proposal" : config.proposalTitle();
        String subtitle = config == null || config.proposalSubtitle() == null ? "Presentation-ready monthly commercial summary." : config.proposalSubtitle();
        String brandName = config == null || config.brandDisplayName() == null ? safe(summary.customerName()) : config.brandDisplayName();
        String logoUrl = config == null ? null : config.brandLogoUrl();
        String primaryColor = config == null || config.brandPrimaryColor() == null ? "#1f2a2a" : config.brandPrimaryColor();
        String accentColor = config == null || config.brandAccentColor() == null ? "#1c7c54" : config.brandAccentColor();
        String terms = config == null ? null : config.proposalTerms();
        String scopeHtml = buildScopeHtml(config);
        String pricingHtml = buildPricingHtml(summary, config);
        return "EXECUTIVE".equalsIgnoreCase(proposalTemplate)
                ? buildExecutiveProposalHtml(summary, generatedAt, title, subtitle, brandName, logoUrl, primaryColor, accentColor, terms, scopeHtml, pricingHtml)
                : buildStandardProposalHtml(summary, generatedAt, title, subtitle, brandName, logoUrl, primaryColor, accentColor, terms, scopeHtml, pricingHtml);
    }

    private String buildStandardProposalHtml(InstallationQuoteSummaryResponse summary,
                                             Instant generatedAt,
                                             String title,
                                             String subtitle,
                                             String brandName,
                                             String logoUrl,
                                             String primaryColor,
                                             String accentColor,
                                             String terms,
                                             String scopeHtml,
                                             String pricingHtml) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Vantage Dialer Proposal</title>
                  <style>
                    :root {
                      --bg: #f5f1e8;
                      --panel: #fffdf8;
                      --ink: %s;
                      --muted: #66706b;
                      --accent: %s;
                      --border: #d9cfbf;
                    }
                    body {
                      margin: 0;
                      font-family: Georgia, "Times New Roman", serif;
                      background: linear-gradient(180deg, #f8f3e8 0%%, #efe6d6 100%%);
                      color: var(--ink);
                    }
                    .page {
                      max-width: 960px;
                      margin: 0 auto;
                      padding: 40px 24px 72px;
                    }
                    .hero, .section {
                      background: var(--panel);
                      border: 1px solid var(--border);
                      border-radius: 20px;
                      padding: 28px;
                      box-shadow: 0 12px 40px rgba(31, 42, 42, 0.06);
                    }
                    .hero {
                      margin-bottom: 20px;
                    }
                    .eyebrow {
                      color: var(--accent);
                      font-size: 13px;
                      letter-spacing: 0.16em;
                      text-transform: uppercase;
                      margin-bottom: 10px;
                    }
                    h1, h2 {
                      margin: 0 0 14px;
                      line-height: 1.1;
                    }
                    h1 {
                      font-size: 40px;
                    }
                    h2 {
                      font-size: 24px;
                      margin-top: 0;
                    }
                    p, li {
                      font-size: 16px;
                      line-height: 1.6;
                      color: var(--ink);
                    }
                    .muted {
                      color: var(--muted);
                    }
                    .grid {
                      display: grid;
                      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                      gap: 16px;
                      margin-top: 18px;
                    }
                    .card {
                      border: 1px solid var(--border);
                      border-radius: 16px;
                      padding: 18px;
                      background: #fffaf0;
                    }
                    .label {
                      font-size: 12px;
                      letter-spacing: 0.08em;
                      text-transform: uppercase;
                      color: var(--muted);
                    }
                    .value {
                      font-size: 28px;
                      margin-top: 8px;
                      color: var(--accent);
                    }
                    .section {
                      margin-top: 18px;
                    }
                    ul {
                      padding-left: 20px;
                      margin: 0;
                    }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <div class="eyebrow">Vantage Dialer Proposal</div>
                      %s
                      <h1>%s</h1>
                      <p class="muted">%s</p>
                      <p class="muted">Generated %s for customer %s (%s).</p>
                      <div class="grid">
                        <div class="card">
                          <div class="label">Suggested Monthly Price</div>
                          <div class="value">%s</div>
                        </div>
                        <div class="card">
                          <div class="label">Estimated Monthly Cost</div>
                          <div class="value">%s</div>
                        </div>
                        <div class="card">
                          <div class="label">Provisioned Agents</div>
                          <div class="value">%s</div>
                        </div>
                        <div class="card">
                          <div class="label">Client Type</div>
                          <div class="value">%s</div>
                        </div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Included Scope</h2>
                      <ul>%s</ul>
                    </section>
                    %s
                    %s
                    %s
                    <section class="section">
                      <h2>Deployment Notes</h2>
                      <ul>
                        <li>App stack is expected on server B</li>
                        <li>Asterisk is expected on server A</li>
                        <li>Customer-specific connection settings are available in the bootstrap bundle</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(primaryColor),
                escapeHtml(accentColor),
                logoUrl == null || logoUrl.isBlank()
                        ? ""
                        : "<img src=\"" + escapeHtml(logoUrl) + "\" alt=\"Brand logo\" style=\"max-height:56px;display:block;margin-bottom:16px;\">",
                escapeHtml(title),
                escapeHtml(subtitle),
                escapeHtml(generatedAt.toString()),
                escapeHtml(brandName),
                escapeHtml(safe(summary.customerId())),
                escapeHtml(String.valueOf(summary.estimate().suggestedSellPrice())),
                escapeHtml(String.valueOf(summary.estimate().totalEstimatedCost())),
                escapeHtml(String.valueOf(summary.provisionedAgentCount())),
                escapeHtml(safe(summary.clientType())),
                scopeHtml,
                buildCommercialAssumptionsHtml(summary),
                terms == null || terms.isBlank()
                        ? ""
                        : "<section class=\"section\"><h2>Terms</h2><p>" + escapeHtml(terms).replace("\n", "<br>") + "</p></section>",
                pricingHtml
        );
    }

    private String buildExecutiveProposalHtml(InstallationQuoteSummaryResponse summary,
                                              Instant generatedAt,
                                              String title,
                                              String subtitle,
                                              String brandName,
                                              String logoUrl,
                                              String primaryColor,
                                              String accentColor,
                                              String terms,
                                              String scopeHtml,
                                              String pricingHtml) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                  <style>
                    :root {
                      --ink: %s;
                      --accent: %s;
                      --paper: #fbf8f2;
                      --line: #d8ccbb;
                      --soft: #736b63;
                    }
                    body {
                      margin: 0;
                      font-family: "Segoe UI", Tahoma, sans-serif;
                      background: radial-gradient(circle at top left, #f3ebde, #e6dcc8 60%%, #dfd0b5 100%%);
                      color: var(--ink);
                    }
                    .page {
                      max-width: 980px;
                      margin: 0 auto;
                      padding: 36px 20px 64px;
                    }
                    .shell {
                      background: var(--paper);
                      border: 1px solid var(--line);
                      border-radius: 28px;
                      overflow: hidden;
                      box-shadow: 0 24px 60px rgba(0, 0, 0, 0.08);
                    }
                    .hero {
                      padding: 32px;
                      background: linear-gradient(135deg, rgba(255,255,255,0.92), rgba(255,248,235,0.92));
                      border-bottom: 1px solid var(--line);
                    }
                    .hero-top {
                      display: flex;
                      justify-content: space-between;
                      gap: 20px;
                      align-items: flex-start;
                    }
                    .pill {
                      display: inline-block;
                      padding: 8px 12px;
                      border-radius: 999px;
                      background: rgba(0,0,0,0.04);
                      color: var(--soft);
                      font-size: 12px;
                      letter-spacing: 0.12em;
                      text-transform: uppercase;
                    }
                    h1 {
                      font-size: 44px;
                      line-height: 1.02;
                      margin: 18px 0 10px;
                    }
                    .subtitle {
                      font-size: 18px;
                      color: var(--soft);
                      max-width: 720px;
                    }
                    .brand {
                      text-align: right;
                    }
                    .brand-name {
                      font-size: 14px;
                      letter-spacing: 0.1em;
                      text-transform: uppercase;
                      color: var(--soft);
                    }
                    .metrics {
                      display: grid;
                      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
                      gap: 14px;
                      margin-top: 24px;
                    }
                    .metric {
                      background: white;
                      border: 1px solid var(--line);
                      border-radius: 18px;
                      padding: 18px;
                    }
                    .metric-label {
                      font-size: 12px;
                      text-transform: uppercase;
                      letter-spacing: 0.08em;
                      color: var(--soft);
                    }
                    .metric-value {
                      font-size: 30px;
                      margin-top: 8px;
                      color: var(--accent);
                      font-weight: 700;
                    }
                    .content {
                      display: grid;
                      grid-template-columns: 1.2fr 0.8fr;
                      gap: 0;
                    }
                    .main, .side {
                      padding: 28px 32px;
                    }
                    .side {
                      background: rgba(0,0,0,0.02);
                      border-left: 1px solid var(--line);
                    }
                    h2 {
                      margin: 0 0 12px;
                      font-size: 22px;
                    }
                    p, li {
                      font-size: 15px;
                      line-height: 1.65;
                    }
                    ul {
                      margin: 0;
                      padding-left: 20px;
                    }
                    .section {
                      margin-top: 24px;
                    }
                    .aside-box {
                      background: white;
                      border: 1px solid var(--line);
                      border-radius: 16px;
                      padding: 18px;
                      margin-bottom: 16px;
                    }
                    .k {
                      color: var(--soft);
                      font-size: 12px;
                      text-transform: uppercase;
                      letter-spacing: 0.08em;
                    }
                    .v {
                      margin-top: 6px;
                      font-size: 18px;
                      font-weight: 600;
                    }
                    @media (max-width: 820px) {
                      .content {
                        grid-template-columns: 1fr;
                      }
                      .side {
                        border-left: 0;
                        border-top: 1px solid var(--line);
                      }
                      .hero-top {
                        flex-direction: column;
                      }
                      .brand {
                        text-align: left;
                      }
                    }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <div class="shell">
                      <section class="hero">
                        <div class="hero-top">
                          <div>
                            <span class="pill">Executive Proposal</span>
                            <h1>%s</h1>
                            <div class="subtitle">%s</div>
                          </div>
                          <div class="brand">
                            %s
                            <div class="brand-name">%s</div>
                            <div class="brand-name">%s</div>
                          </div>
                        </div>
                        <div class="metrics">
                          <div class="metric">
                            <div class="metric-label">Suggested Monthly Price</div>
                            <div class="metric-value">%s</div>
                          </div>
                          <div class="metric">
                            <div class="metric-label">Estimated Cost</div>
                            <div class="metric-value">%s</div>
                          </div>
                          <div class="metric">
                            <div class="metric-label">Agents</div>
                            <div class="metric-value">%s</div>
                          </div>
                          <div class="metric">
                            <div class="metric-label">Client Type</div>
                            <div class="metric-value">%s</div>
                          </div>
                        </div>
                      </section>
                      <div class="content">
                        <section class="main">
                          <div class="section">
                            <h2>Scope</h2>
                            <ul>%s</ul>
                          </div>
                          <div class="section">
                            <h2>Deployment Model</h2>
                            <ul>
                              <li>Asterisk on server A</li>
                              <li>App stack on server B</li>
                              <li>Configuration and sales artifacts generated from one install record</li>
                            </ul>
                          </div>
                          %s
                          %s
                        </section>
                        <aside class="side">
                          <div class="aside-box">
                            <div class="k">Customer</div>
                            <div class="v">%s</div>
                          </div>
                          <div class="aside-box">
                            <div class="k">Customer Id</div>
                            <div class="v">%s</div>
                          </div>
                          <div class="aside-box">
                            <div class="k">Installation</div>
                            <div class="v">%s</div>
                          </div>
                          <div class="aside-box">
                            <div class="k">Fixed Infrastructure Cost</div>
                            <div class="v">%s</div>
                          </div>
                          <div class="aside-box">
                            <div class="k">Variable Usage Cost</div>
                            <div class="v">%s</div>
                          </div>
                          <div class="aside-box">
                            <div class="k">Target Margin</div>
                            <div class="v">%s</div>
                          </div>
                          <div class="aside-box">
                            <div class="k">Pricing Configuration</div>
                            <div class="v">%s</div>
                          </div>
                          %s
                        </aside>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(title),
                escapeHtml(primaryColor),
                escapeHtml(accentColor),
                escapeHtml(title),
                escapeHtml(subtitle),
                logoUrl == null || logoUrl.isBlank()
                        ? ""
                        : "<img src=\"" + escapeHtml(logoUrl) + "\" alt=\"Brand logo\" style=\"max-height:56px;display:block;margin-bottom:12px;\">",
                escapeHtml(brandName),
                escapeHtml(generatedAt.toString()),
                escapeHtml(String.valueOf(summary.estimate().suggestedSellPrice())),
                escapeHtml(String.valueOf(summary.estimate().totalEstimatedCost())),
                escapeHtml(String.valueOf(summary.provisionedAgentCount())),
                escapeHtml(safe(summary.clientType())),
                scopeHtml,
                buildCommercialAssumptionsHtml(summary),
                terms == null || terms.isBlank()
                        ? ""
                        : "<div class=\"section\"><h2>Terms</h2><p>" + escapeHtml(terms).replace("\n", "<br>") + "</p></div>",
                escapeHtml(brandName),
                escapeHtml(safe(summary.customerId())),
                escapeHtml(safe(summary.installationName())),
                escapeHtml(String.valueOf(summary.estimate().fixedInfrastructureCost())),
                escapeHtml(String.valueOf(summary.estimate().variableUsageCost())),
                escapeHtml(String.valueOf(summary.estimate().desiredMarginPercent())),
                escapeHtml(safe(summary.estimate().configurationId())),
                pricingHtml
        );
    }

    private List<String> proposalScopeItems(com.vantage.dialer.api.dto.CustomerConfigurationResponse config) {
        List<String> items = new ArrayList<>();
        if (proposalFlag(config == null ? null : config.proposalIncludeAgentOutbound(), true)) {
            items.add("- Agent-assisted outbound calling");
        }
        if (proposalFlag(config == null ? null : config.proposalIncludeIvr(), true)) {
            items.add("- Outbound IVR foundation");
        }
        if (proposalFlag(config == null ? null : config.proposalIncludeReporting(), true)) {
            items.add("- Operational and campaign reporting");
        }
        if (proposalFlag(config == null ? null : config.proposalIncludeWebRtc(), true)) {
            items.add("- Browser softphone and WebRTC-ready client support");
        }
        if (proposalFlag(config == null ? null : config.proposalIncludeProvisioning(), true)) {
            items.add("- API-first provisioning and deployment artifacts");
        }
        return items;
    }

    private String buildScopeHtml(com.vantage.dialer.api.dto.CustomerConfigurationResponse config) {
        List<String> items = new ArrayList<>();
        items.add("<li>Single-tenant outbound dialer deployment</li>");
        proposalScopeItems(config).forEach(item -> items.add("<li>" + escapeHtml(item.substring(2)) + "</li>"));
        return String.join("", items);
    }

    private String buildPricingHtml(InstallationQuoteSummaryResponse summary,
                                    com.vantage.dialer.api.dto.CustomerConfigurationResponse config) {
        if (!proposalFlag(config == null ? null : config.proposalIncludePricingBreakdown(), true)) {
            return "";
        }
        return """
                <section class="section">
                  <h2>Pricing Breakdown</h2>
                  <ul>
                    <li>Fixed infrastructure cost: %s</li>
                    <li>Variable usage cost: %s</li>
                    <li>Target margin percent: %s</li>
                    <li>Pricing configuration id: %s</li>
                  </ul>
                </section>
                """.formatted(
                escapeHtml(String.valueOf(summary.estimate().fixedInfrastructureCost())),
                escapeHtml(String.valueOf(summary.estimate().variableUsageCost())),
                escapeHtml(String.valueOf(summary.estimate().desiredMarginPercent())),
                escapeHtml(safe(summary.estimate().configurationId()))
        );
    }

    private String buildCommercialAssumptionsHtml(InstallationQuoteSummaryResponse summary) {
        CommercialAssumptionsResponse assumptions = summary.commercialAssumptions();
        if (assumptions == null) {
            return "";
        }
        return """
                <section class="section">
                  <h2>Commercial Assumptions</h2>
                  <ul>
                    <li>Source: %s</li>
                    <li>Monthly call minutes: %s</li>
                    <li>Monthly TTS units: %s</li>
                    <li>Monthly STT minutes: %s</li>
                    <li>Monthly recording GB: %s</li>
                    <li>Agent count: %s</li>
                    <li>Concurrent channels: %s</li>
                    <li>Desired margin percent: %s</li>
                  </ul>
                </section>
                """.formatted(
                escapeHtml(safe(assumptions.source())),
                escapeHtml(String.valueOf(assumptions.monthlyCallMinutes())),
                escapeHtml(String.valueOf(assumptions.monthlyTtsUnits())),
                escapeHtml(String.valueOf(assumptions.monthlySttMinutes())),
                escapeHtml(String.valueOf(assumptions.monthlyRecordingGb())),
                escapeHtml(String.valueOf(assumptions.agentCount())),
                escapeHtml(String.valueOf(assumptions.concurrentChannels())),
                escapeHtml(String.valueOf(assumptions.desiredMarginPercent()))
        );
    }

    private boolean proposalFlag(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private CommercialAssumptionsDeltaResponse buildAssumptionsDelta(CommercialAssumptionsResponse base,
                                                                     CommercialAssumptionsResponse target) {
        if (base == null || target == null) {
            return null;
        }
        return new CommercialAssumptionsDeltaResponse(
                diffLong(base.monthlyCallMinutes(), target.monthlyCallMinutes()),
                diffLong(base.monthlyTtsUnits(), target.monthlyTtsUnits()),
                diffLong(base.monthlySttMinutes(), target.monthlySttMinutes()),
                diffDouble(base.monthlyRecordingGb(), target.monthlyRecordingGb()),
                diffInt(base.agentCount(), target.agentCount()),
                diffInt(base.concurrentChannels(), target.concurrentChannels()),
                diffDouble(base.desiredMarginPercent(), target.desiredMarginPercent())
        );
    }

    private CostEstimateDeltaResponse buildEstimateDelta(com.vantage.dialer.api.dto.CostEstimateResponse base,
                                                         com.vantage.dialer.api.dto.CostEstimateResponse target) {
        if (base == null || target == null) {
            return null;
        }
        return new CostEstimateDeltaResponse(
                diffDouble(base.fixedInfrastructureCost(), target.fixedInfrastructureCost()),
                diffDouble(base.variableUsageCost(), target.variableUsageCost()),
                diffDouble(base.totalEstimatedCost(), target.totalEstimatedCost()),
                diffDouble(base.suggestedSellPrice(), target.suggestedSellPrice()),
                diffDouble(base.desiredMarginPercent(), target.desiredMarginPercent())
        );
    }

    private Long diffLong(Long base, Long target) {
        if (base == null || target == null) {
            return null;
        }
        return target - base;
    }

    private Integer diffInt(Integer base, Integer target) {
        if (base == null || target == null) {
            return null;
        }
        return target - base;
    }

    private Double diffDouble(Double base, Double target) {
        if (base == null || target == null) {
            return null;
        }
        return target - base;
    }

    private Double diffDouble(double base, double target) {
        return target - base;
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
