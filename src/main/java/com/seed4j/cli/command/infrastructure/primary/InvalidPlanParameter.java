package com.seed4j.cli.command.infrastructure.primary;

import java.util.List;

record InvalidPlanParameter(String name, Object value, List<String> allowedValues, boolean safeToInfer) {}
