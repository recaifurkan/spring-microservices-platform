package com.example.frontend.config;

import com.example.frontend.filter.TraceIdResponseFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TraceIdResponseFilter traceIdResponseFilter;

    public WebMvcConfig(TraceIdResponseFilter traceIdResponseFilter) {
        this.traceIdResponseFilter = traceIdResponseFilter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceIdResponseFilter)
                .addPathPatterns("/**");
    }
}

