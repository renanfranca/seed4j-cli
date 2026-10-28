package com.seed4j.cli.command.infrastructure.secondary;

import com.seed4j.cli.bootstrap.infrastructure.primary.JavaRuntimeExtensionModeSwitcher;
import com.seed4j.cli.bootstrap.infrastructure.primary.JavaRuntimeExtensionModeSwitcher.JavaRuntimeExtensionModeSwitch;
import com.seed4j.cli.bootstrap.infrastructure.primary.JavaRuntimeExtensionModeSwitcher.JavaRuntimeExtensionModeSwitchException;
import com.seed4j.cli.command.domain.RuntimeExtensionModeSwitchException;
import com.seed4j.cli.command.domain.RuntimeExtensionModeSwitchResult;
import com.seed4j.cli.command.domain.RuntimeExtensionModeSwitcher;
import com.seed4j.cli.shared.distribution.domain.RuntimeExtensionsSuspendedException;
import com.seed4j.cli.shared.distribution.infrastructure.secondary.ClasspathBuildCapabilities;
import com.seed4j.cli.shared.error.domain.Assert;
import org.springframework.stereotype.Component;

@Component
public class BootstrapRuntimeExtensionModeSwitcher implements RuntimeExtensionModeSwitcher {

  private final ClasspathBuildCapabilities capabilities;
  private final JavaRuntimeExtensionModeSwitcher runtimeExtensionModeSwitcher;

  public BootstrapRuntimeExtensionModeSwitcher(
    JavaRuntimeExtensionModeSwitcher runtimeExtensionModeSwitcher,
    ClasspathBuildCapabilities capabilities
  ) {
    Assert.notNull("capabilities", capabilities);
    this.capabilities = capabilities;
    Assert.notNull("runtimeExtensionModeSwitcher", runtimeExtensionModeSwitcher);

    this.runtimeExtensionModeSwitcher = runtimeExtensionModeSwitcher;
  }

  @Override
  public RuntimeExtensionModeSwitchResult enable() {
    requireExtensionSupport();
    try {
      JavaRuntimeExtensionModeSwitch modeSwitch = runtimeExtensionModeSwitcher.enable();

      return new RuntimeExtensionModeSwitchResult(modeSwitch.configPath());
    } catch (JavaRuntimeExtensionModeSwitchException exception) {
      throw new RuntimeExtensionModeSwitchException(exception.getMessage(), exception);
    }
  }

  private void requireExtensionSupport() {
    if (capabilities.runtimeExtensions().suspended()) {
      throw new RuntimeExtensionsSuspendedException();
    }
  }

  @Override
  public RuntimeExtensionModeSwitchResult disable() {
    requireExtensionSupport();
    try {
      JavaRuntimeExtensionModeSwitch modeSwitch = runtimeExtensionModeSwitcher.disable();

      return new RuntimeExtensionModeSwitchResult(modeSwitch.configPath());
    } catch (JavaRuntimeExtensionModeSwitchException exception) {
      throw new RuntimeExtensionModeSwitchException(exception.getMessage(), exception);
    }
  }
}
