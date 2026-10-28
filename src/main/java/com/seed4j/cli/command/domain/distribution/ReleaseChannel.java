package com.seed4j.cli.command.domain.distribution;

public enum ReleaseChannel {
  STABLE,
  EXPERIMENTAL,
  LOCAL_INTEGRATION;

  public boolean localIntegration() {
    return this == LOCAL_INTEGRATION;
  }

  public boolean experimental() {
    return this == EXPERIMENTAL;
  }
}
