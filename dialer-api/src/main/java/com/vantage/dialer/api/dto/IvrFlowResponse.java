package com.vantage.dialer.api.dto;

import java.util.List;

public record IvrFlowResponse(
        String ivrFlowId,
        String name,
        String description,
        List<IvrStepRequest> steps) {
}
