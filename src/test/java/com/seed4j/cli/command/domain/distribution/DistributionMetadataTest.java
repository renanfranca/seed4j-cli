package com.seed4j.cli.command.domain.distribution;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seed4j.cli.UnitTest;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

@UnitTest
class DistributionMetadataTest {

  private static final Seed4JUpstreamCommit UPSTREAM_COMMIT = new Seed4JUpstreamCommit("0123456789abcdef0123456789abcdef01234567");

  @Test
  void shouldRejectMalformedUpstreamCommit() {
    String invalidCommit = "not-a-full-sha";

    assertThatThrownBy(() -> new Seed4JUpstreamCommit(invalidCommit))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Seed4J upstream commit must be a 40-character lowercase hexadecimal SHA");
  }

  @Test
  void shouldRejectExperimentalIdentityWithoutUpstreamCommit() {
    Seed4JDependencyCoordinate coordinate = Seed4JDependencyCoordinate.versioned(
      "io.github.renanfranca",
      "seed4j-main-snapshot",
      "snapshot-version"
    );

    assertThatThrownBy(() -> new DistributionIdentity(ReleaseChannel.EXPERIMENTAL, coordinate, Optional.empty()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Experimental distribution identity requires dependency version and upstream commit");
  }

  @Test
  void shouldRejectExperimentalIdentityWithoutDependencyVersion() {
    Seed4JDependencyCoordinate coordinate = Seed4JDependencyCoordinate.unversioned("io.github.renanfranca", "seed4j-main-snapshot");

    assertThatThrownBy(() -> new DistributionIdentity(ReleaseChannel.EXPERIMENTAL, coordinate, Optional.of(UPSTREAM_COMMIT)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Experimental distribution identity requires dependency version and upstream commit");
  }

  @Test
  void shouldRejectUnavailableModulesForStableDistribution() {
    DistributionIdentity identity = DistributionMetadata.stable().identity();
    Seed4JModuleAvailability availability = new Seed4JModuleAvailability(Set.of(new DistributionModuleSlug("seed4j-extension")));

    assertThatThrownBy(() -> new DistributionMetadata(identity, availability))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Stable distribution must make every Seed4J module available");
  }

  @Test
  void shouldRejectLocalIntegrationWithoutUpstreamCommit() {
    Seed4JDependencyCoordinate coordinate = Seed4JDependencyCoordinate.versioned("com.seed4j", "seed4j-engine", "2.2.1-SNAPSHOT");

    assertThatThrownBy(() -> new DistributionIdentity(ReleaseChannel.LOCAL_INTEGRATION, coordinate, Optional.empty()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Local integration requires dependency version and upstream commit");
  }

  @Test
  void shouldRejectLocalIntegrationWithoutDependencyVersion() {
    Seed4JDependencyCoordinate coordinate = Seed4JDependencyCoordinate.unversioned("com.seed4j", "seed4j-engine");

    assertThatThrownBy(() -> new DistributionIdentity(ReleaseChannel.LOCAL_INTEGRATION, coordinate, Optional.of(UPSTREAM_COMMIT)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Local integration requires dependency version and upstream commit");
  }

  @Test
  void shouldRejectAvailableExtensionGeneratorInLocalIntegration() {
    DistributionIdentity identity = new DistributionIdentity(
      ReleaseChannel.LOCAL_INTEGRATION,
      Seed4JDependencyCoordinate.versioned("com.seed4j", "seed4j-engine", "2.2.1-SNAPSHOT"),
      Optional.of(UPSTREAM_COMMIT)
    );

    assertThatThrownBy(() -> new DistributionMetadata(identity, Seed4JModuleAvailability.allAvailable()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Local integration must make only seed4j-extension unavailable");
  }
}
