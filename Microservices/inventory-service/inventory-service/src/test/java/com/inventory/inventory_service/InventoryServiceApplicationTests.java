package com.inventory.inventory_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "application.security.jwt.secret-key=0123456789abcdef0123456789abcdef")
class InventoryServiceApplicationTests {
	@Autowired MockMvc mvc;

	@Test
	void contextLoads() {
	}

	@Test
	void onlyInventoryRoleCanAccessProducts() throws Exception {
		mvc.perform(get("/api/products/sku/not-found").with(user("inventory").authorities(() -> "ROLE_INVENTORY_USER")))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/products").with(user("sales").authorities(() -> "ROLE_SALES_USER")))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/products").with(user("finance").authorities(() -> "ROLE_FINANCE_USER")))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
	}

}
