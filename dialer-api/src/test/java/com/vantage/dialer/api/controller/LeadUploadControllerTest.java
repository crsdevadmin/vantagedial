package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.campaign.Lead;
import com.vantage.dialer.api.campaign.LeadStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeadUploadControllerTest {

    @Test
    void uploadParsesCsvSkipsHeaderAndBlankLinesAndAddsLeads() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new LeadUploadController(leadStore));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "leads.csv",
                "text/csv",
                "customerNumber\n+15550001\n\n+15550002,extra\n".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/campaigns/camp-1/leads/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("\"Upload successful. Leads added: 2\""));

        ArgumentCaptor<Lead> captor = ArgumentCaptor.forClass(Lead.class);
        verify(leadStore, org.mockito.Mockito.times(2)).addLead(captor.capture());
        List<Lead> leads = captor.getAllValues();
        assertEquals(List.of("+15550001", "+15550002"), leads.stream().map(Lead::getCustomerNumber).toList());
        assertTrue(leads.stream().allMatch(lead -> "camp-1".equals(lead.getCampaignId())));
    }

    @Test
    void uploadEmptyFileReturnsSuccessWithZeroLeads() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new LeadUploadController(leadStore));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                "\n\n".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/campaigns/camp-2/leads/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("\"Upload successful. Leads added: 0\""));

        verifyNoInteractions(leadStore);
    }

    @Test
    void uploadFailureReturnsBadRequestBody() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        LeadUploadController controller = new LeadUploadController(leadStore);
        MultipartFile file = mock(MultipartFile.class);
        IOException boom = new IOException("boom");
        when(file.getInputStream()).thenThrow(boom);

        ResponseEntity<String> response = controller.upload("camp-3", file);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Upload failed: boom", response.getBody());
        verifyNoInteractions(leadStore);
    }
}
