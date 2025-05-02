package tech.jhipster.lite.cli.command.infrastructure.primary;

import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import tech.jhipster.lite.module.application.JHipsterModulesApplicationService;
import tech.jhipster.lite.module.domain.JHipsterModuleSlug;
import tech.jhipster.lite.module.domain.JHipsterModuleToApply;
import tech.jhipster.lite.module.domain.properties.JHipsterModuleProperties;

@Component
@CommandLine.Command(name = "apply", description = "Apply jhipster-lite specific module")
class ApplyCommand implements Callable<Integer> {

  private final JHipsterModulesApplicationService modules;

  @CommandLine.Option(names = "--project-path", description = "Project Path Folder", defaultValue = ".")
  private String projectPath;

  @CommandLine.Option(names = "--commit", description = "Commit changes", negatable = true)
  private Boolean commit;

  @CommandLine.Parameters(description = "Module Slug to be applied")
  private String moduleSlug;

  public ApplyCommand(JHipsterModulesApplicationService modules) {
    this.modules = modules;
  }

  @Override
  public Integer call() {
    JHipsterModuleProperties properties = new JHipsterModuleProperties(projectPath, commit(), null);
    JHipsterModuleToApply moduleToApply = new JHipsterModuleToApply(new JHipsterModuleSlug(moduleSlug), properties);
    modules.apply(moduleToApply);

    return 0;
  }

  private boolean commit() {
    return commit == null || commit;
  }
}
