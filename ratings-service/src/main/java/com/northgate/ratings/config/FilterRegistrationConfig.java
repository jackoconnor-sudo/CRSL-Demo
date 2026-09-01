package com.northgate.ratings.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<AdminApiFilter> adminApiFilter() {
        FilterRegistrationBean<AdminApiFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AdminApiFilter());
        registration.addUrlPatterns("/api/admin/*");
        registration.setOrder(1);
        return registration;
    }
}
