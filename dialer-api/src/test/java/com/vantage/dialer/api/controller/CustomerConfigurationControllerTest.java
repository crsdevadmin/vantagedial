package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.dto.CustomerConfigurationRequest;
import com.vantage.dialer.api.dto.CustomerConfigurationResponse;
import com.vantage.dialer.api.dto.ProposalPresetResponse;
import com.vantage.dialer.api.service.CustomerConfigurationService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerConfigurationControllerTest {

    @Test
    void createBindsRequestBodyAndReturnsServiceResponse() throws Exception {
        CustomerConfigurationService service = mock(CustomerConfigurationService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new CustomerConfigurationController(service));
        CustomerConfigurationRequest request = request("cust-1", "Acme");

        when(service.createOrUpdate(any(CustomerConfigurationRequest.class)))
                .thenAnswer(invocation -> response(
                        invocation.getArgument(0, CustomerConfigurationRequest.class).getCustomerId(),
                        invocation.getArgument(0, CustomerConfigurationRequest.class).getCustomerName()
                ));

        mockMvc.perform(post("/customers")
                        .contentType(APPLICATION_JSON)
                        .content(ControllerTestSupport.json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("cust-1"))
                .andExpect(jsonPath("$.customerName").value("Acme"));

        ArgumentCaptor<CustomerConfigurationRequest> captor = ArgumentCaptor.forClass(CustomerConfigurationRequest.class);
        verify(service).createOrUpdate(captor.capture());
        assertEquals("cust-1", captor.getValue().getCustomerId());
        assertEquals("Acme", captor.getValue().getCustomerName());
    }

    @Test
    void updateOverridesCustomerIdFromPathVariable() throws Exception {
        CustomerConfigurationService service = mock(CustomerConfigurationService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new CustomerConfigurationController(service));
        CustomerConfigurationRequest request = request("ignored-id", "Updated");

        when(service.createOrUpdate(any(CustomerConfigurationRequest.class)))
                .thenAnswer(invocation -> response(
                        invocation.getArgument(0, CustomerConfigurationRequest.class).getCustomerId(),
                        invocation.getArgument(0, CustomerConfigurationRequest.class).getCustomerName()
                ));

        mockMvc.perform(put("/customers/cust-path")
                        .contentType(APPLICATION_JSON)
                        .content(ControllerTestSupport.json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("cust-path"))
                .andExpect(jsonPath("$.customerName").value("Updated"));

        ArgumentCaptor<CustomerConfigurationRequest> captor = ArgumentCaptor.forClass(CustomerConfigurationRequest.class);
        verify(service).createOrUpdate(captor.capture());
        assertEquals("cust-path", captor.getValue().getCustomerId());
    }

    @Test
    void listLookupAndPresetsEndpointsReturnSerializedPayloads() throws Exception {
        CustomerConfigurationService service = mock(CustomerConfigurationService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new CustomerConfigurationController(service));

        when(service.list()).thenReturn(List.of(response("cust-1", "Acme")));
        when(service.find("cust-1")).thenReturn(Optional.of(response("cust-1", "Acme")));
        when(service.listProposalPresets()).thenReturn(List.of(new ProposalPresetResponse(
                "preset-1", "Softphone preset", true, true, true, true, true, true,
                "SOFTPHONE", "SOFTPHONE", "WEB", true, true, 1000L, 200L, 30L, 5.5, 3, 12, 30.0
        )));

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value("cust-1"));

        mockMvc.perform(get("/customers/cust-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Acme"));

        mockMvc.perform(get("/customers/proposal-presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].presetId").value("preset-1"))
                .andExpect(jsonPath("$[0].recommendedClientType").value("SOFTPHONE"));
    }

    @Test
    void getPropagatesUnknownCustomerError() {
        CustomerConfigurationService service = mock(CustomerConfigurationService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new CustomerConfigurationController(service));

        when(service.find("missing")).thenReturn(Optional.empty());

        ServletException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ServletException.class,
                () -> mockMvc.perform(get("/customers/missing")).andReturn()
        );

        IllegalArgumentException cause = assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("Unknown customer: missing", cause.getMessage());
    }

    private CustomerConfigurationRequest request(String customerId, String customerName) {
        CustomerConfigurationRequest request = new CustomerConfigurationRequest();
        request.setCustomerId(customerId);
        request.setCustomerName(customerName);
        request.setSipDomain("sip.example.com");
        return request;
    }

    private CustomerConfigurationResponse response(String customerId, String customerName) {
        return new CustomerConfigurationResponse(
                customerId,
                customerName,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
