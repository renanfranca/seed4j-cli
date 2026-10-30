package com.seed4j.cli.shared.distribution.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;

import com.seed4j.cli.UnitTest;
import com.seed4j.cli.shared.distribution.domain.RuntimeExtensionSupport;
import org.junit.jupiter.api.Test;

@UnitTest
class ClasspathBuildCapabilitiesTest {

  @Test
  void shouldReadPackagedBuildCapabilitiesForLocalIntegration() {
    ClasspathBuildCapabilities capabilities = new ClasspathBuildCapabilities();

    assertThat(capabilities.runtimeExtensions()).isEqualTo(RuntimeExtensionSupport.SUSPENDED);
  }
}
