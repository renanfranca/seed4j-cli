package com.seed4j.cli.command.infrastructure.primary;

import com.seed4j.cli.shared.error.domain.Assert;
import java.util.Arrays;

enum PlanFormat {
  TEXT,
  JSON;

  static PlanFormat from(String value) {
    Assert.notBlank("value", value);

    return Arrays.stream(values())
      .filter(format -> format.name().equalsIgnoreCase(value))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unsupported plan format: " + value));
  }
}
