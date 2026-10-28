package com.seed4j.cli.command.infrastructure.primary;

import com.seed4j.cli.command.application.DistributionMetadataApplicationService;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Model.CommandSpec;

@Component
class ExtensionCommand implements Seed4JCommand {

  private final DistributionMetadataApplicationService distribution;
  private final ExtensionInstallCommand extensionInstallCommand;
  private final ExtensionEnableCommand extensionEnableCommand;
  private final ExtensionDisableCommand extensionDisableCommand;

  ExtensionCommand(
    ExtensionInstallCommand extensionInstallCommand,
    ExtensionEnableCommand extensionEnableCommand,
    ExtensionDisableCommand extensionDisableCommand,
    DistributionMetadataApplicationService distribution
  ) {
    this.distribution = distribution;
    this.extensionInstallCommand = extensionInstallCommand;
    this.extensionEnableCommand = extensionEnableCommand;
    this.extensionDisableCommand = extensionDisableCommand;
  }

  @Override
  public CommandSpec spec() {
    CommandSpec spec = CommandSpec.wrapWithoutInspection(this).name("extension").mixinStandardHelpOptions(true);
    spec
      .usageMessage()
      .description(
        distribution.metadata().identity().channel().localIntegration()
          ? SuspendedRuntimeExtensionDiagnostic.message()
          : "Manage runtime extensions"
      );
    spec.addSubcommand(extensionInstallCommand.name(), extensionInstallCommand.spec());
    spec.addSubcommand(extensionEnableCommand.name(), extensionEnableCommand.spec());
    spec.addSubcommand(extensionDisableCommand.name(), extensionDisableCommand.spec());

    return spec;
  }

  @Override
  public String name() {
    return "extension";
  }
}
