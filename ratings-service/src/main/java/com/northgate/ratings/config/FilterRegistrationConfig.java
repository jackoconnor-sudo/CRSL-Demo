package com.northgate.ratings.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<AdminApiFilter> adminApiFilter(
            @Value("${northgate.admin.token:}") String adminToken) {
        FilterRegistrationBean<AdminApiFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AdminApiFilter(adminToken));
        registration.addUrlPatterns("/api/admin/*");
        registration.setOrder(1);
        return registration;
    }
}
