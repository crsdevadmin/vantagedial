package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record QuoteSnapshotDashboardResponse(
        String installationJobId,
        String customerId,
        Instant generatedAt,
        QuoteSnapshotSummaryResponse summary,
        List<QuoteSnapshotTimelineEntryResponse> timeline) {
}
