package com.seed4j.cli.command.infrastructure.primary;

record PlanExecutionDecision(String name, Object value, PlanValueSource source, boolean safeToInfer) {}
