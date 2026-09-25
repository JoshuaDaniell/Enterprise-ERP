package com.finance.finance_service.Config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class RedisConfig {
    public static final String INVOICES = "finance:invoices";
    public static final String DASHBOARD = "finance:dashboard";
}
