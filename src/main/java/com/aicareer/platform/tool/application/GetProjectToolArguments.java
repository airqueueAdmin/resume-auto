package com.aicareer.platform.tool.application;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record GetProjectToolArguments(@NotNull UUID projectId) {
}
