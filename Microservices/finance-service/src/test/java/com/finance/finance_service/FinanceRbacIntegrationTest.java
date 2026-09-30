package com.finance.finance_service;

import com.finance.finance_service.Config.SecurityConfig;
import com.finance.finance_service.Controller.InvoiceController;
import com.finance.finance_service.Service.InvoicePdfService;
import com.finance.finance_service.Service.InvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InvoiceController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "application.security.jwt.secret-key=0123456789abcdef0123456789abcdef")
class FinanceRbacIntegrationTest {
    @Autowired MockMvc mvc;
    @MockBean InvoiceService invoiceService;
    @MockBean InvoicePdfService invoicePdfService;

    @Test
    void financeRoleCanReadInvoices() throws Exception {
        when(invoiceService.list()).thenReturn(List.of());
        mvc.perform(get("/api/finance/invoices").with(user("finance").authorities(() -> "ROLE_FINANCE_USER")))
                .andExpect(status().isOk());
    }

    @Test
    void otherBusinessRolesCannotReadInvoices() throws Exception {
        mvc.perform(get("/api/finance/invoices").with(user("sales").authorities(() -> "ROLE_SALES_USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/finance/invoices").with(user("inventory").authorities(() -> "ROLE_INVENTORY_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestsCannotReadInvoices() throws Exception {
        mvc.perform(get("/api/finance/invoices")).andExpect(status().isUnauthorized());
    }
}
