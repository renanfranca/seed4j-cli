package com.seed4j.cli.command.infrastructure.primary;

import java.util.List;

record PlanParameter(
  String name,
  Object value,
  PlanValueSource source,
  boolean safeToInfer,
  Boolean valid,
  List<String> allowedValues,
  Object exampleValue,
  Object defaultValue
) {}
