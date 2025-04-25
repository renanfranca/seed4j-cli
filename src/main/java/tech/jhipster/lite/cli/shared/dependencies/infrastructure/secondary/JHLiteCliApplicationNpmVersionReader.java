package tech.jhipster.lite.cli.shared.dependencies.infrastructure.secondary;

import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Repository;
import tech.jhipster.lite.cli.shared.dependencies.domain.JHLiteCliApplicationNpmVersionSource;
import tech.jhipster.lite.module.domain.ProjectFiles;
import tech.jhipster.lite.module.domain.npm.NpmPackagesVersions;
import tech.jhipster.lite.module.infrastructure.secondary.npm.FileSystemNpmVersionReader;
import tech.jhipster.lite.module.infrastructure.secondary.npm.NpmVersionsReader;

@Repository
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JHLiteCliApplicationNpmVersionReader implements NpmVersionsReader {

  private static final String PARENT_FOLDER = "/generator/j-h-lite-cli-application-dependencies/";

  private final FileSystemNpmVersionReader reader;

  public JHLiteCliApplicationNpmVersionReader(ProjectFiles projectFiles) {
    reader = new FileSystemNpmVersionReader(projectFiles, List.of(JHLiteCliApplicationNpmVersionSource.values()), PARENT_FOLDER);
  }

  @Override
  public NpmPackagesVersions get() {
    return reader.get();
  }
}
