package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.store.EventStore;
import com.vantage.dialer.api.service.CallSessionService;
import com.vantage.dialer.api.dto.CallTimelineBundleResponse;
import com.vantage.dialer.api.dto.OperatorWrapUpRequest;
import com.vantage.dialer.api.dto.OperatorWrapUpResponse;
import com.vantage.dialer.api.service.ReportingService;
import com.vantage.dialer.common.events.StandardEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/outbound")
public class EventQueryController {

    private final EventStore eventStore;
    private final ReportingService reportingService;
    private final CallSessionService callSessionService;

    public EventQueryController(EventStore eventStore,
                                ReportingService reportingService,
                                CallSessionService callSessionService) {
        this.eventStore = eventStore;
        this.reportingService = reportingService;
        this.callSessionService = callSessionService;
    }

    @GetMapping("/status/{callSessionId}")
    public ResponseEntity<StandardEvent> getStatus(@PathVariable String callSessionId) {
        return eventStore.getLatest(callSessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/timeline/{callSessionId}")
    public List<StandardEvent> getTimeline(@PathVariable String callSessionId) {
        return reportingService.getTimeline(callSessionId);
    }

    @PostMapping("/timeline/{callSessionId}/bundle")
    public CallTimelineBundleResponse generateTimelineBundle(@PathVariable String callSessionId) {
        return reportingService.generateCallTimelineBundle(callSessionId);
    }

    @GetMapping("/sessions/{callSessionId}")
    public ResponseEntity<?> getCallSession(@PathVariable String callSessionId) {
        return callSessionService.getCallSession(callSessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/sessions/{callSessionId}/wrap-up")
    public OperatorWrapUpResponse saveWrapUp(@PathVariable String callSessionId,
                                             @RequestBody OperatorWrapUpRequest request) {
        return callSessionService.saveOperatorWrapUp(callSessionId, request);
    }
}
