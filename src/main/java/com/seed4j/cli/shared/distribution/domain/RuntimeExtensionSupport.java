package com.seed4j.cli.shared.distribution.domain;

public enum RuntimeExtensionSupport {
  SUPPORTED,
  SUSPENDED;

  public boolean suspended() {
    return this == SUSPENDED;
  }
}
