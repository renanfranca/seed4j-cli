package com.seed4j.cli.shared.distribution.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seed4j.cli.UnitTest;
import com.seed4j.cli.shared.distribution.domain.RuntimeExtensionSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@UnitTest
class ClasspathBuildCapabilitiesTest {

  @Test
  void shouldReadPackagedBuildCapabilities() {
    ClasspathBuildCapabilities capabilities = new ClasspathBuildCapabilities();

    assertThat(capabilities.runtimeExtensions()).isIn(RuntimeExtensionSupport.SUPPORTED, RuntimeExtensionSupport.SUSPENDED);
  }

  @Test
  void shouldDefaultToSupportedWhenDistributionResourceIsMissing() {
    ClasspathBuildCapabilities capabilities = new ClasspathBuildCapabilities(() -> null);

    assertThat(capabilities.runtimeExtensions()).isEqualTo(RuntimeExtensionSupport.SUPPORTED);
  }

  @Test
  void shouldDefaultToSupportedWhenRuntimeExtensionsPropertyIsAbsent() {
    ClasspathBuildCapabilities capabilities = capabilitiesFor("release-channel=stable\n");

    assertThat(capabilities.runtimeExtensions()).isEqualTo(RuntimeExtensionSupport.SUPPORTED);
  }

  @Test
  void shouldReturnSupportedWhenConfiguredAsSupported() {
    ClasspathBuildCapabilities capabilities = capabilitiesFor("runtime-extensions=supported\n");

    assertThat(capabilities.runtimeExtensions()).isEqualTo(RuntimeExtensionSupport.SUPPORTED);
  }

  @Test
  void shouldReturnSuspendedWhenConfiguredAsSuspended() {
    ClasspathBuildCapabilities capabilities = capabilitiesFor("runtime-extensions=suspended\n");

    assertThat(capabilities.runtimeExtensions()).isEqualTo(RuntimeExtensionSupport.SUSPENDED);
  }

  @Test
  void shouldRejectInvalidRuntimeExtensionsCapability() {
    ClasspathBuildCapabilities capabilities = capabilitiesFor("runtime-extensions=invalid\n");

    assertThatThrownBy(capabilities::runtimeExtensions)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Invalid runtime extension build capability");
  }

  @Test
  void shouldFailWhenDistributionPropertiesCannotBeRead() {
    ClasspathBuildCapabilities capabilities = new ClasspathBuildCapabilities(() -> {
      throw new IOException("Unreadable resource");
    });

    assertThatThrownBy(capabilities::runtimeExtensions)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Could not read build capabilities")
      .hasCauseInstanceOf(IOException.class);
  }

  private static ClasspathBuildCapabilities capabilitiesFor(String propertiesContent) {
    return new ClasspathBuildCapabilities(() -> new ByteArrayInputStream(propertiesContent.getBytes(StandardCharsets.UTF_8)));
  }
}

