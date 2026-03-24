package com.vantage.dialer.api.dto;

public record ProposalPresetResponse(
        String presetId,
        String description,
        boolean includeAgentOutbound,
        boolean includeIvr,
        boolean includeReporting,
        boolean includeWebRtc,
        boolean includeProvisioning,
        boolean includePricingBreakdown,
        String recommendedClientType,
        String recommendedAgentUiMode,
        String recommendedSupervisorUiMode,
        boolean recommendedDeployAfterProvision,
        boolean recommendedPerformRemoteChecks,
        long defaultMonthlyCallMinutes,
        long defaultMonthlyTtsUnits,
        long defaultMonthlySttMinutes,
        double defaultMonthlyRecordingGb,
        int defaultAgentCount,
        int defaultConcurrentChannels,
        double defaultMarginPercent) {
}
