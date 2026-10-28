package com.seed4j.cli;

import com.seed4j.cli.bootstrap.composition.PreSpringBootstrapConfiguration;
import com.seed4j.cli.shared.generation.domain.ExcludeFromGeneratedCodeCoverage;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@ExcludeFromGeneratedCodeCoverage(reason = "Not testing logs and System.exit behavior")
@SpringBootApplication(scanBasePackages = { "com.seed4j.cli", "com.seed4j.module", "com.seed4j.history", "com.seed4j.shared" })
public class Seed4JCliApp {

  interface BootstrapExitCodeResolver {
    int exitCodeFor(String[] args);
  }

  static void main(String[] args) {
    System.exit(productionBootstrapExitCodeResolver().exitCodeFor(args));
  }

  static BootstrapExitCodeResolver productionBootstrapExitCodeResolver() {
    return PreSpringBootstrapConfiguration.preSpringBootstrapRunner()::exitCodeFor;
  }
}
