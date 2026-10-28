package com.seed4j.cli.command.infrastructure.primary;

final class SuspendedRuntimeExtensionDiagnostic {

  private SuspendedRuntimeExtensionDiagnostic() {}

  static String message() {
    return "Runtime extensions are suspended in this local integration. No changes were applied.";
  }
}
