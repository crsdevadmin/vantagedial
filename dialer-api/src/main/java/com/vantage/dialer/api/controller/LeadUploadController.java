package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.campaign.Lead;
import com.vantage.dialer.api.campaign.LeadStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/campaigns")
public class LeadUploadController {

    private final LeadStore leadStore;

    public LeadUploadController(LeadStore leadStore) {
        this.leadStore = leadStore;
    }

    @PostMapping("/{campaignId}/leads/upload")
    public ResponseEntity<String> upload(
            @PathVariable String campaignId,
            @RequestParam("file") MultipartFile file) {

        int added = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                // Skip header
                if (firstLine && line.toLowerCase().contains("customernumber")) {
                    firstLine = false;
                    continue;
                }

                String number = line.split(",")[0].trim();

                String leadId = UUID.randomUUID().toString();

                leadStore.addLead(
                        new Lead(leadId, campaignId, number)
                );

                added++;
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }

        return ResponseEntity.ok("Upload successful. Leads added: " + added);
    }
}