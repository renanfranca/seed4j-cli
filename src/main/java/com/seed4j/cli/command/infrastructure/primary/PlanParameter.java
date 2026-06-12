package com.seed4j.cli.command.infrastructure.primary;

record PlanParameter(String name, Object value, PlanValueSource source, boolean safeToInfer) {}
