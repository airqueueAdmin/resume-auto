package com.aicareer.platform.tool.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tools")
public record ToolProperties(Duration approvalTtl) {
}
