package tech.jhipster.lite.cli.command.infrastructure.primary;

import org.springframework.context.annotation.*;
import tech.jhipster.lite.module.application.JHipsterModulesApplicationService;

@Configuration
@ComponentScan(
  basePackages = { "tech.jhipster.lite.cli", "tech.jhipster.lite.module" },
  excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "tech.jhipster.lite.cli.cucumber.*")
)
class CommandContextTestConfig {

  @Bean
  @Primary
  @Scope("prototype")
  public ApplyCommand applyCommand(JHipsterModulesApplicationService modules) {
    return new ApplyCommand(modules);
  }
}
