package com.seed4j.cli.bootstrap.infrastructure.secondary;

import com.seed4j.cli.bootstrap.domain.InvalidRuntimeConfigurationException;
import com.seed4j.cli.bootstrap.domain.RuntimeMode;
import com.seed4j.cli.bootstrap.domain.RuntimeModeChangePlan;
import com.seed4j.cli.bootstrap.domain.RuntimeModeConfigurationRepository;
import com.seed4j.cli.shared.distribution.domain.RuntimeExtensionSupport;
import com.seed4j.cli.shared.error.domain.Assert;

public class BuildRuntimeModeConfigurationRepository implements RuntimeModeConfigurationRepository {

  private final RuntimeModeConfigurationRepository configuration;
  private final RuntimeExtensionSupport extensions;

  public BuildRuntimeModeConfigurationRepository(RuntimeModeConfigurationRepository configuration, RuntimeExtensionSupport extensions) {
    Assert.notNull("configuration", configuration);
    Assert.notNull("extensions", extensions);
    this.configuration = configuration;
    this.extensions = extensions;
  }

  @Override
  public RuntimeMode readMode() {
    RuntimeMode mode = configuration.readMode();
    if (mode == RuntimeMode.EXTENSION && extensions.suspended()) {
      throw new InvalidRuntimeConfigurationException(
        "Runtime extensions are suspended in this local integration. Select seed4j.runtime.mode: standard in ~/.config/seed4j-cli/config.yml."
      );
    }
    return mode;
  }

  @Override
  public RuntimeModeChangePlan prepareModeChange(RuntimeMode targetMode) {
    return configuration.prepareModeChange(targetMode);
  }
}
