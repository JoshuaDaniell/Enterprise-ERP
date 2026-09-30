package com.sales.sales_service;

import com.sales.sales_service.Config.SecurityConfig;
import com.sales.sales_service.Security.JwtService;
import com.sales.sales_service.Service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.sales.sales_service.Controller.OrderController.class)
@Import(SecurityConfig.class)
class SalesRbacIntegrationTest {
    @Autowired MockMvc mvc;
    @MockBean OrderService orderService;
    @MockBean JwtService jwtService;

    @Test
    void salesRoleCanReadSalesOrders() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of());
        mvc.perform(get("/api/orders").with(user("sales").authorities(() -> "ROLE_SALES_USER")))
                .andExpect(status().isOk());
    }

    @Test
    void otherBusinessRolesCannotReadSalesOrders() throws Exception {
        mvc.perform(get("/api/orders").with(user("inventory").authorities(() -> "ROLE_INVENTORY_USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/orders").with(user("finance").authorities(() -> "ROLE_FINANCE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestsCannotReadSalesOrders() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }
}
