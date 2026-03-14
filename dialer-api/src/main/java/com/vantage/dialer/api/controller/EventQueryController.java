package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.store.EventStore;
import com.vantage.dialer.common.events.StandardEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/outbound")
public class EventQueryController {

    private final EventStore eventStore;

    public EventQueryController(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @GetMapping("/status/{callSessionId}")
    public ResponseEntity<StandardEvent> getStatus(@PathVariable String callSessionId) {
        return eventStore.getLatest(callSessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}