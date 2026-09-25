package com.sales.sales_service.Config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class RedisConfig {
    public static final String ORDERS = "sales:orders";
    public static final String CUSTOMERS = "sales:customers";
}
