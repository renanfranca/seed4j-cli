package tech.jhipster.lite.cli.shared.dependencies.domain;

import tech.jhipster.lite.module.domain.npm.NpmVersionSource;
import tech.jhipster.lite.module.domain.npm.NpmVersionSourceFactory;

public enum JHLiteCliApplicationNpmVersionSource implements NpmVersionSourceFactory {
  J_H_LITE_CLI_APPLICATION("j-h-lite-cli-application");

  private final String source;

  JHLiteCliApplicationNpmVersionSource(String source) {
    this.source = source;
  }

  @Override
  public NpmVersionSource build() {
    return new NpmVersionSource(source);
  }
}
