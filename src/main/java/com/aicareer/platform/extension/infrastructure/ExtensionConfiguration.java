package com.aicareer.platform.extension.infrastructure;

import com.aicareer.platform.extension.application.ExtensionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(ExtensionProperties.class)
public class ExtensionConfiguration implements WebMvcConfigurer {

    private final ExtensionProperties properties;

    public ExtensionConfiguration(ExtensionProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/extension/**")
                .allowedOriginPatterns(properties.allowedOriginPatterns().toArray(String[]::new))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(false)
                .maxAge(600);
    }
}
