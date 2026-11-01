package com.seed4j.cli.command.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;

import com.seed4j.cli.UnitTest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
@EnabledIf(value = "localIntegrationBuild", disabledReason = "Exercises the pinned local integration distribution")
class LocalIntegrationPackagedJarIT {

  private static final String UPSTREAM_COMMIT = "e627bd665ef4cbb789c239daf2c7377b323912a5";
  private Path evidence;
  private Path home;
  private Path project;
  private Path jar;
  private int invocation;

  static boolean localIntegrationBuild() throws IOException {
    try (InputStream input = LocalIntegrationPackagedJarIT.class.getResourceAsStream("/META-INF/seed4j-cli-distribution.properties")) {
      Properties properties = new Properties();
      properties.load(input);
      return "local-integration".equals(properties.getProperty("release-channel"));
    }
  }

  @BeforeEach
  void prepareIsolatedProcessEnvironment(TestInfo test) throws IOException {
    Path evidenceRoot = Path.of(
      System.getProperty(
        "seed4j.integration.evidence",
        Path.of(System.getProperty("java.io.tmpdir"), "seed4j-cli-integration-evidence").toString()
      )
    ).toAbsolutePath();
    for (Path parent = evidenceRoot; parent != null; parent = parent.getParent()) {
      if (Files.exists(parent.resolve(".git"))) {
        throw new IllegalStateException("Integration projects must be outside every Git checkout: " + evidenceRoot);
      }
    }
    Files.createDirectories(evidenceRoot);
    evidence = Files.createTempDirectory(evidenceRoot, test.getTestMethod().orElseThrow().getName() + "-");
    home = Files.createDirectories(evidence.resolve("home"));
    project = evidence.resolve("project");
    Files.writeString(home.resolve(".gitconfig"), "[user]\n\tname = Integration Test\n\temail = integration@example.com\n");
    try (Stream<Path> files = Files.list(Path.of("target"))) {
      jar = files
        .filter(path -> path.getFileName().toString().matches("seed4j-cli-.*\\.jar"))
        .findFirst()
        .orElseThrow()
        .toAbsolutePath();
    }
  }

  @Test
  void shouldIdentifyPinnedBuildWithoutServerLibraries() throws Exception {
    Run version = cli("--version");
    List<String> libraries;
    try (JarFile packaged = new JarFile(jar.toFile())) {
      libraries = packaged
        .stream()
        .map(entry -> entry.getName())
        .filter(name -> name.startsWith("BOOT-INF/lib/"))
        .sorted()
        .toList();
    }
    Files.write(evidence.resolve("packaged-libraries.txt"), libraries);

    assertThat(version.exitCode()).isZero();
    assertThat(version.out()).contains(
      "Release channel: local-integration",
      "Seed4J version: 2.2.1-SNAPSHOT",
      "Seed4J upstream commit: " + UPSTREAM_COMMIT,
      "Runtime mode: standard"
    );
    assertThat(libraries).contains(
      "BOOT-INF/lib/seed4j-engine-2.2.1-SNAPSHOT.jar",
      "BOOT-INF/lib/seed4j-core-modules-2.2.1-SNAPSHOT.jar",
      "BOOT-INF/lib/seed4j-api-2.2.1-SNAPSHOT.jar"
    );
    assertThat(libraries).noneMatch(name ->
      name.matches(
        ".*(seed4j-main-snapshot|seed4j-server|/seed4j-2|spring-web|spring-webmvc|tomcat|jetty|undertow|liquibase|postgresql|h2-|mysql|mariadb|seed4j-test).*"
      )
    );
  }

  @Test
  void shouldDiscoverPackagedProvidersAndExplainUnavailableExtensionGenerator() throws Exception {
    Run list = cli("list");
    Run help = cli("apply", "--help");
    Run completion = cli("completion", "bash");
    Run unavailable = cli("apply", "seed4j-extension", "--project-path", project.toString());
    Run unavailableSet = cli("apply-set", "seed4j-extension", "--project-path", project.toString());

    assertThat(list.exitCode()).isZero();
    assertThat(list.out())
      .contains("Module", "Dependencies", "Description", "init", "prettier", "maven-java", "java-base")
      .doesNotContain("seed4j-extension");
    assertThat(help.exitCode()).isZero();
    assertThat(help.out()).contains("init", "prettier", "maven-java", "java-base").doesNotContain("seed4j-extension");
    assertThat(completion.exitCode()).isZero();
    assertThat(completion.out()).contains("init", "prettier", "maven-java", "java-base").doesNotContain("seed4j-extension");
    assertThat(unavailable.exitCode()).isEqualTo(2);
    assertThat(unavailable.err()).contains("suspended", "local integration").doesNotContain("Central Portal");
    assertThat(unavailableSet.exitCode()).isEqualTo(2);
    assertThat(unavailableSet.err()).contains("suspended", "local integration").doesNotContain("Central Portal");
    assertThat(project).doesNotExist();
  }

  @Test
  void shouldHideConfiguredResourcesFromPackagedDiscoveryHelpAndCompletion() throws Exception {
    Run visible = cli("list");
    Run visibleHelp = cli("apply", "--help");
    Run visibleCompletion = cli("completion", "bash");

    assertThat(visible.exitCode()).isZero();
    assertThat(visible.out()).contains("prettier", "init");
    assertThat(visibleHelp.exitCode()).isZero();
    assertThat(visibleHelp.out()).contains("prettier", "init");
    assertThat(visibleCompletion.exitCode()).isZero();
    assertThat(visibleCompletion.out()).contains("prettier", "init");

    Path config = home.resolve(".config/seed4j-cli/config.yml");
    Files.createDirectories(config.getParent());
    Files.writeString(config, "seed4j:\n  hidden-resources:\n    slugs:\n      - prettier\n");
    Map<String, String> before = snapshot(home, "before-hidden-resource-commands");

    Run hiddenList = cli("list");
    Run hiddenHelp = cli("apply", "--help");
    Run hiddenCompletion = cli("completion", "bash");
    Map<String, String> after = snapshot(home, "after-hidden-resource-commands");

    assertThat(hiddenList.exitCode()).isZero();
    assertThat(hiddenList.out()).contains("init").doesNotContain("prettier");
    assertThat(hiddenHelp.exitCode()).isZero();
    assertThat(hiddenHelp.out()).contains("init").doesNotContain("prettier");
    assertThat(hiddenCompletion.exitCode()).isZero();
    assertThat(hiddenCompletion.out()).contains("init").doesNotContain("prettier");
    assertThat(after).isEqualTo(before);
  }

  @Test
  void shouldPlanAndRejectInvalidExecutionWithoutCreatingDestination() throws Exception {
    Run singlePlan = cli("apply", "init", "--plan", "--project-path", project.toString());
    Run setPlan = cli("apply-set", "init", "--plan", "--project-path", project.toString());
    Run dependencyFailure = cli("apply", "maven-java", "--project-path", project.toString());
    Run invalidSet = cli("apply-set", "unknown-integration-module", "--project-path", project.toString());

    assertThat(singlePlan.exitCode()).isZero();
    assertThat(singlePlan.out()).contains("Missing required parameters:", "No changes were applied.");
    assertThat(setPlan.exitCode()).isEqualTo(2);
    assertThat(setPlan.out()).isEmpty();
    assertThat(setPlan.err()).contains("Status: INVALID", "No changes were applied.");
    assertThat(dependencyFailure.exitCode()).isEqualTo(2);
    assertThat(dependencyFailure.err()).contains("Missing required dependencies:", "module:init");
    assertThat(invalidSet.exitCode()).isEqualTo(2);
    assertThat(invalidSet.out()).isEmpty();
    assertThat(invalidSet.err()).contains("Unknown requested modules:", "Status: INVALID");
    assertThat(project).doesNotExist();
  }

  @Test
  void shouldCreateProjectAndContinueAcrossIndependentProcesses() throws Exception {
    Run init = cli(
      "apply",
      "init",
      "--project-path",
      project.toString(),
      "--project-name",
      "Integration App",
      "--base-name",
      "integrationApp",
      "--node-package-manager",
      "npm"
    );
    List<JsonNode> initHistory = history();
    Run initialCommits = git("log", "--reverse", "--pretty=%s");
    Map<String, String> before = snapshot(project, "before-plan");
    Run plan = cli("apply", "maven-java", "--project-path", project.toString(), "--plan");
    Run setPlan = cli(
      "apply-set",
      "maven-java",
      "--project-path",
      project.toString(),
      "--package-name",
      "com.example.integration",
      "--plan"
    );
    Map<String, String> after = snapshot(project, "after-plan");
    Run maven = cli("apply", "maven-java", "--project-path", project.toString(), "--package-name", "com.example.integration");
    List<JsonNode> mavenHistory = history();
    Run commits = git("log", "--reverse", "--pretty=%s");
    Run java = cli("apply", "java-base", "--project-path", project.toString());

    assertThat(init.exitCode()).isZero();
    assertThat(project.resolve("README.md")).content().contains("Integration App");
    assertThat(initHistory)
      .extracting(action -> action.path("module").asString())
      .containsExactly("init");
    assertThat(initHistory.getFirst().path("properties").path("projectName").asString()).isEqualTo("Integration App");
    assertThat(initHistory.getFirst().path("properties").path("baseName").asString()).isEqualTo("integrationApp");
    assertThat(initHistory.getFirst().path("properties").path("nodePackageManager").asString()).isEqualTo("npm");
    assertThat(initialCommits.exitCode()).isZero();
    assertThat(initialCommits.out().lines()).containsExactly("Apply module: init");
    assertThat(plan.exitCode()).isZero();
    assertThat(plan.out()).contains(
      "module:init - already applied",
      "projectName: Integration App",
      "baseName: integrationApp",
      "Source: project history"
    );
    assertThat(setPlan.exitCode()).isZero();
    assertThat(setPlan.out()).contains("Status: VALID");
    assertThat(after).isEqualTo(before);
    assertThat(maven.exitCode()).isZero();
    assertThat(project.resolve("pom.xml"))
      .content()
      .contains(
        "<groupId>com.example.integration</groupId>",
        "<artifactId>integration-app</artifactId>",
        "<name>integrationApp</name>",
        "<description>Integration App</description>"
      );
    assertThat(mavenHistory)
      .extracting(action -> action.path("module").asString())
      .containsExactly("init", "maven-java");
    assertThat(commits.exitCode()).isZero();
    assertThat(commits.out().lines()).containsExactly("Apply module: init", "Apply module: maven-java");
    assertThat(java.exitCode()).isZero();
    assertThat(project.resolve("src/main/java/com/example/integration/shared/error/domain/Assert.java"))
      .content()
      .contains("package com.example.integration.shared.error.domain;", "public final class Assert");
    assertThat(history())
      .extracting(action -> action.path("module").asString())
      .containsExactly("init", "maven-java", "java-base");
  }

  @ParameterizedTest
  @ValueSource(strings = { "aggregated", "specific" })
  void shouldReuseLegacyHistoryAndPreferExplicitOptionsWithoutRewriting(String layout) throws Exception {
    Path historyDirectory = Files.createDirectories(project.resolve(".seed4j/modules"));
    String fixture = layout.equals("aggregated") ? "legacy-aggregated.json" : "legacy-action.json";
    String filename = layout.equals("aggregated") ? "history.json" : "20261020120000-init.json";
    try (InputStream input = getClass().getResourceAsStream("/history/" + fixture)) {
      Files.copy(input, historyDirectory.resolve(filename));
    }
    Map<String, String> before = snapshot(project, "before-legacy-plan");

    Run plan = cli("apply", "maven-java", "--project-path", project.toString(), "--project-name", "Explicit Integration App", "--plan");
    Run setPlan = cli(
      "apply-set",
      "maven-java",
      "--project-path",
      project.toString(),
      "--project-name",
      "Explicit Integration App",
      "--package-name",
      "com.example.integration",
      "--plan"
    );
    Map<String, String> after = snapshot(project, "after-legacy-plan");

    assertThat(plan.exitCode()).isZero();
    assertThat(plan.out())
      .contains(
        "module:init - already applied",
        "projectName: Explicit Integration App\n  Source: explicit CLI input",
        "baseName: legacyIntegrationApp\n  Source: project history"
      )
      .doesNotContain("projectName: Legacy Integration App");
    assertThat(setPlan.exitCode()).isZero();
    assertThat(setPlan.out()).contains("Explicit Integration App", "legacyIntegrationApp", "Status: VALID");
    assertThat(after).isEqualTo(before);
    assertThat(project.resolve(".git")).doesNotExist();
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void shouldExecuteSetsInDependencyOrderAndReapplyExplicitRequests(boolean commit) throws Exception {
    List<String> arguments = new ArrayList<>(
      List.of(
        "apply-set",
        "maven-java",
        "init",
        "--project-path",
        project.toString(),
        "--project-name",
        "Integration App",
        "--base-name",
        "integrationApp",
        "--node-package-manager",
        "npm",
        "--package-name",
        "com.example.integration"
      )
    );
    if (!commit) {
      arguments.add("--no-commit");
    }

    Run applied = cli(arguments.toArray(String[]::new));
    List<JsonNode> firstHistory = history();
    Run commits = commit ? git("log", "--reverse", "--pretty=%s") : new Run(0, "", "");
    Run reapplied = commit
      ? cli("apply-set", "maven-java", "--project-path", project.toString())
      : cli("apply-set", "maven-java", "--project-path", project.toString(), "--no-commit");

    assertThat(applied.exitCode()).isZero();
    assertThat(applied.out()).containsSubsequence("1. init", "2. maven-java").contains("Module set status: SUCCEEDED");
    assertThat(firstHistory)
      .extracting(action -> action.path("module").asString())
      .containsExactly("init", "maven-java");
    assertThat(project.resolve("README.md")).content().contains("Integration App");
    assertThat(project.resolve("pom.xml")).content().contains("<groupId>com.example.integration</groupId>");
    if (commit) {
      assertThat(commits.exitCode()).isZero();
      assertThat(commits.out().lines()).containsExactly("Apply module: init", "Apply module: maven-java");
    } else {
      assertThat(project.resolve(".git")).doesNotExist();
    }
    assertThat(reapplied.exitCode()).isZero();
    assertThat(reapplied.out()).contains("reapplied", "Module set status: SUCCEEDED");
    assertThat(history())
      .extracting(action -> action.path("module").asString())
      .containsExactly("init", "maven-java", "maven-java");
  }

  @Test
  void shouldRejectCorruptPersistedHistoryBeforeAnyExecution() throws Exception {
    Path history = Files.createDirectories(project.resolve(".seed4j/modules")).resolve("history.json");
    Files.writeString(history, "{invalid history");
    Map<String, String> before = snapshot(project, "before-corrupt-history");

    Run single = cli("apply", "init", "--project-path", project.toString());
    Run set = cli("apply-set", "init", "--project-path", project.toString());
    Map<String, String> after = snapshot(project, "after-corrupt-history");

    assertThat(single.exitCode()).isEqualTo(1);
    assertThat(single.err()).contains("Can't read project history");
    assertThat(set.exitCode()).isEqualTo(1);
    assertThat(set.out()).isEmpty();
    assertThat(set.err()).contains("ERROR: Unable to complete module set preflight.", "No changes were applied.");
    assertThat(after).isEqualTo(before);
  }

  @Test
  void shouldSuspendExtensionCommandsAndRejectExtensionModeBeforeCheckingArtifacts() throws Exception {
    Path config = home.resolve(".config/seed4j-cli/config.yml");
    Files.createDirectories(config.getParent());
    Files.writeString(config, "seed4j:\n  runtime:\n    mode: standard\n");
    Path extension = evidence.resolve("extension.jar");
    try (java.util.jar.JarOutputStream output = new java.util.jar.JarOutputStream(Files.newOutputStream(extension))) {
      output.putNextEntry(new java.util.jar.JarEntry("fixture.txt"));
      output.write("local integration".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      output.closeEntry();
    }
    Path active = Files.createDirectories(config.getParent().resolve("runtime/active"));
    Files.copy(extension, active.resolve("extension.jar"));
    Files.writeString(active.resolve("metadata.yml"), "distribution:\n  id: integration\n  version: 1\n");
    Path cache = Files.createDirectories(config.getParent().resolve("runtime/cache"));
    Files.writeString(cache.resolve("sentinel"), "preserve cache");
    Map<String, String> before = snapshot(home, "before-extension-commands");

    Run help = cli("extension", "--help");
    Run install = cli("extension", "install", extension.toString(), "--distribution-id", "integration", "--distribution-version", "1");
    Run enable = cli("extension", "enable");
    Run disable = cli("extension", "disable");
    Map<String, String> afterCommands = snapshot(home, "after-extension-commands");
    Files.writeString(config, "seed4j:\n  runtime:\n    mode: extension\n");
    Map<String, String> beforeBootstrap = snapshot(home, "before-extension-bootstrap");
    Run bootstrap = cli("--version");
    Map<String, String> afterBootstrap = snapshot(home, "after-extension-bootstrap");
    Files.delete(active.resolve("extension.jar"));
    Files.delete(active.resolve("metadata.yml"));
    Map<String, String> beforeMissing = snapshot(home, "before-missing-artifacts");
    Run missing = cli("--version");
    Map<String, String> afterMissing = snapshot(home, "after-missing-artifacts");

    assertThat(help.exitCode()).isZero();
    assertThat(help.out()).contains("suspended", "local integration", "install", "enable", "disable");
    assertThat(install.exitCode()).isEqualTo(2);
    assertThat(install.err()).contains("suspended", "No changes were applied.");
    assertThat(enable.exitCode()).isEqualTo(2);
    assertThat(enable.err()).contains("suspended");
    assertThat(disable.exitCode()).isEqualTo(2);
    assertThat(disable.err()).contains("suspended");
    assertThat(afterCommands).isEqualTo(before);
    assertThat(bootstrap.exitCode()).isEqualTo(1);
    assertThat(bootstrap.err()).contains("suspended", "seed4j.runtime.mode: standard", "config.yml");
    assertThat(afterBootstrap).isEqualTo(beforeBootstrap);
    assertThat(missing.exitCode()).isEqualTo(1);
    assertThat(missing.err()).isEqualTo(bootstrap.err());
    assertThat(afterMissing).isEqualTo(beforeMissing);
  }

  private Run cli(String... arguments) throws IOException, InterruptedException {
    List<String> command = new ArrayList<>(
      List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Duser.home=" + home, "-jar", jar.toString())
    );
    command.addAll(List.of(arguments));
    return run(command);
  }

  private Run run(List<String> command) throws IOException, InterruptedException {
    String name = "%02d".formatted(++invocation);
    Path stdout = evidence.resolve(name + ".stdout");
    Path stderr = evidence.resolve(name + ".stderr");
    Files.write(evidence.resolve(name + ".command"), command);
    ProcessBuilder builder = new ProcessBuilder(command)
      .directory(evidence.toFile())
      .redirectOutput(stdout.toFile())
      .redirectError(stderr.toFile());
    builder.environment().put("GIT_CONFIG_GLOBAL", home.resolve(".gitconfig").toString());
    builder.environment().put("GIT_CONFIG_NOSYSTEM", "1");
    builder.environment().put("HOME", home.toString());
    Process process = builder.start();
    if (!process.waitFor(60, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new IllegalStateException("Packaged CLI timed out; evidence: " + evidence);
    }
    Files.writeString(evidence.resolve(name + ".exit"), Integer.toString(process.exitValue()));
    return new Run(process.exitValue(), Files.readString(stdout), Files.readString(stderr));
  }

  private Run git(String... arguments) throws IOException, InterruptedException {
    List<String> command = new ArrayList<>(List.of("git", "-C", project.toString()));
    command.addAll(List.of(arguments));
    return run(command);
  }

  private List<JsonNode> history() throws IOException {
    List<JsonNode> actions = new ArrayList<>();
    try (Stream<Path> files = Files.list(project.resolve(".seed4j/modules"))) {
      for (Path file : files
        .filter(path -> path.toString().endsWith(".json"))
        .sorted()
        .toList()) {
        actions.add(JsonMapper.builder().build().readTree(Files.readString(file)));
      }
    }
    return List.copyOf(actions);
  }

  private Map<String, String> snapshot(Path root, String name) throws IOException, NoSuchAlgorithmException {
    Map<String, String> contents = new LinkedHashMap<>();
    try (Stream<Path> files = Files.walk(root)) {
      for (Path file : files.sorted().toList()) {
        String relative = root.relativize(file).toString();
        contents.put(
          relative,
          Files.isDirectory(file)
            ? "directory"
            : Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)))
        );
      }
    }
    Files.writeString(evidence.resolve(name + ".snapshot"), contents.toString());
    return Map.copyOf(contents);
  }

  private record Run(int exitCode, String out, String err) {}
}
