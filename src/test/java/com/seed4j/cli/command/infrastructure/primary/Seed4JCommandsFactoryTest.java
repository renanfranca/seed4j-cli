package com.seed4j.cli.command.infrastructure.primary;

import static com.seed4j.cli.command.infrastructure.primary.CliFixture.commandLine;
import static com.seed4j.cli.command.infrastructure.primary.CliFixture.setupProjectTestFolder;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seed4j.cli.IntegrationTest;
import com.seed4j.cli.command.domain.RuntimeDisplay;
import com.seed4j.module.application.Seed4JModulesApplicationService;
import com.seed4j.module.infrastructure.secondary.git.GitTestUtil;
import com.seed4j.project.application.ProjectsApplicationService;
import com.seed4j.project.domain.ProjectPath;
import com.seed4j.project.domain.history.ProjectHistory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
@IntegrationTest
class Seed4JCommandsFactoryTest {

  private static final String PROJECT_NAME = "projectName";
  private static final String BASE_NAME = "baseName";
  private static final String END_OF_LINE = "endOfLine";
  private static final String INDENT_SIZE = "indentSize";
  private static final String PACKAGE_NAME = "packageName";

  @Autowired
  private ProjectsApplicationService projects;

  @Autowired
  private Seed4JModulesApplicationService modules;

  @Test
  void shouldShowHelpMessageWhenNoCommand(CapturedOutput output) {
    String[] args = {};

    int exitCode = commandLine(modules, projects).execute(args);

    assertThat(exitCode).isEqualTo(2);
    assertThat(output)
      .contains("Seed4J CLI")
      .contains("-h, --help      Show this help message and exit.")
      .contains("-V, --version   Print version information and exit.")
      .contains("--debug")
      .contains("Enable runtime bootstrap diagnostics (extension mode only)")
      .contains("Commands:");
  }

  @Test
  void shouldAcceptDebugFlagInRootCommand(CapturedOutput output) {
    String[] args = { "--version", "--debug" };

    int exitCode = commandLine(modules, projects).execute(args);

    assertThat(exitCode).isZero();
    assertThat(output).contains("Seed4J CLI v1").contains("Seed4J version: 2");
  }

  @Test
  void shouldListInstallSubcommandWhenShowingExtensionHelp(CapturedOutput output) {
    String[] args = { "extension", "--help" };

    int exitCode = commandLine(modules, projects).execute(args);

    assertThat(exitCode).isZero();
    assertThat(output).contains("Manage runtime extensions").contains("install").contains("Install active runtime extension");
  }

  @Nested
  @DisplayName("list")
  class ListModules {

    @Test
    void shouldNotLeakTheExtensionOnlySlugWhenListingModulesInStandardRuntimeMode(CapturedOutput output) {
      String[] args = { "list" };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(output).doesNotContain("runtime-extension-list-only");
    }

    @Test
    void shouldRenderTypedDependenciesWhenModuleHasDependencies(CapturedOutput output) {
      String[] args = { "list" };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(output).containsPattern("(?m)^\\s{2}\\S+\\s{2,}(?:module|feature):\\S+.*\\s{2,}.+$");
    }

    @Test
    void shouldShowDependenciesColumnWithFallbackForListOutput(CapturedOutput output) {
      String[] args = { "list" };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(output)
        .containsPattern("(?m)^\\s{2}Module\\s{2,}Dependencies\\s{2,}Description\\s*$")
        .containsPattern("(?m)^\\s{2}init\\s{2,}-\\s{2,}Init project\\s*$");
    }

    @Test
    void shouldListModules(CapturedOutput output) {
      String[] args = { "list" };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(output)
        .contains("Available seed4j modules")
        .contains("init")
        .contains("Init project")
        .contains("prettier")
        .contains("Format project with prettier");
    }
  }

  @Nested
  @DisplayName("apply")
  class ApplyModule {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldNotApplyWithoutModuleSlugSubcommand(CapturedOutput output) {
      String[] args = { "apply" };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isEqualTo(2);
      assertThat(output).contains("Missing required subcommand").contains("init").contains("prettier");
    }

    @Test
    void shouldEscapeCommandDescriptionInHelpCommand(CapturedOutput output) {
      String[] args = { "apply", "--help" };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(output).doesNotContain(
        "[picocli WARN] Could not format 'Add JaCoCo for code coverage reporting and 100% coverage check' (Underlying error: Conversion = c, Flags =  ). Using raw String: '%n' format strings have not been replaced with newlines. Please ensure to escape '%' characters with another '%'."
      );
    }

    @Test
    void shouldDisplayModuleSlugsInHelpCommand(CapturedOutput output) {
      String[] args = { "apply", "--help" };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(output)
        .contains(
          """
          Apply seed4j specific module
          """
        )
        .contains("Automation note: agents should run seed4j apply init --plan --format json")
        .contains("before executing init.")
        .contains("init")
        .contains("Init project")
        .contains("prettier")
        .contains("Format project with prettier");
    }

    @Test
    void shouldDisplayModuleSlugsInAlphabeticalOrderInApplyHelpCommand(CapturedOutput output) {
      String[] args = { "apply", "--help" };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(output.toString().indexOf("angular-core"))
        .withFailMessage("Command 'angular-core' should appear before 'gradle-java' in alphabetical order")
        .isLessThan(output.toString().indexOf("gradle-java"));
    }

    @Test
    void shouldExplainCommitOptionInitializesGitAndNoCommitSkipsGit(CapturedOutput output) {
      String[] args = { "apply", "init", "--help" };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(output)
        .contains("Initialize Git if needed and commit generated changes")
        .contains("--no-commit skips Git init and commit");
    }

    @Test
    void shouldShowAutomationNoteInInitHelp(CapturedOutput output) {
      String[] args = { "apply", "init", "--help" };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(output)
        .contains("Automation note")
        .contains("agents should run seed4j apply init --plan --format json")
        .contains("agents must not infer required values from folder names or examples")
        .contains("values marked safeToInfer=false require asking the user");
    }

    @Test
    void shouldApplyInitModuleWithRequiredOptions() throws IOException {
      Path projectPath = setupProjectTestFolder();
      String[] args = {
        "apply",
        "init",
        "--project-path",
        projectPath.toString(),
        "--base-name",
        "seed4jSampleApplication",
        "--project-name",
        "Seed4J Sample Application",
        "--node-package-manager",
        "npm",
      };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(GitTestUtil.getCommits(projectPath)).contains("Apply module: init");
      assertThat(projectPropertyValue(projectPath, PROJECT_NAME)).isEqualTo("Seed4J Sample Application");
      assertThat(projectPropertyValue(projectPath, BASE_NAME)).isEqualTo("seed4jSampleApplication");
    }

    @Test
    void shouldNotApplyInitModuleMissingRequiredOptions(CapturedOutput output) throws IOException {
      Path projectPath = setupProjectTestFolder();
      String[] args = { "apply", "init", "--project-path", projectPath.toString() };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isEqualTo(2);
      assertThat(GitTestUtil.getCommits(projectPath)).isEmpty();
      assertThat(output)
        .contains("Missing required")
        .contains("'--base-name=<basename*>'")
        .contains("'--project-name=<projectname*>'")
        .contains("Project short name (only letters and numbers) (required)")
        .contains("Project full name (required)")
        .contains("Run seed4j apply init --plan --format json before executing init");
    }

    @Test
    void shouldPlanInitWithMissingRequiredInputsWithoutChangingProject(CapturedOutput output) throws IOException {
      Path projectPath = setupProjectTestFolder();
      Path historyFile = projectPath.resolve(".seed4j").resolve("modules").resolve("history.json");
      String originalHistory = Files.readString(historyFile);
      String[] args = { "apply", "init", "--project-path", projectPath.toString(), "--plan", "--format", "json" };

      int exitCode = commandLine(modules, projects).execute(args);

      JsonNode plan = jsonPlan(output);
      assertThat(exitCode).isZero();
      assertThat(GitTestUtil.getCommits(projectPath)).isEmpty();
      assertThat(Files.readString(historyFile)).isEqualTo(originalHistory);
      assertThat(plan.get("status").asText()).isEqualTo("NEEDS_USER_INPUT");
      assertThat(plan.get("executable").asBoolean()).isFalse();
      assertThat(plan.get("parameters").get("projectPath").get("source").asText()).isEqualTo("EXPLICIT");
      assertThat(plan.get("parameters").get("projectName").get("source").asText()).isEqualTo("MISSING");
      assertThat(plan.get("parameters").get("projectName").get("safeToInfer").asBoolean()).isFalse();
      assertThat(plan.get("parameters").get("projectName").get("exampleValue").asText()).isEqualTo("Seed4J Sample Application");
      assertThat(plan.get("parameters").get("projectName").get("defaultValue").isNull()).isTrue();
      assertThat(plan.get("parameters").get("baseName").get("source").asText()).isEqualTo("MISSING");
      assertThat(plan.get("parameters").get("baseName").get("safeToInfer").asBoolean()).isFalse();
      assertThat(plan.get("parameters").get("baseName").get("exampleValue").asText()).isEqualTo("seed4jSampleApplication");
      assertThat(plan.get("parameters").get("baseName").get("defaultValue").isNull()).isTrue();
      assertThat(plan.get("parameters").get("nodePackageManager").get("source").asText()).isEqualTo("MISSING");
      assertThat(plan.get("parameters").get("nodePackageManager").get("safeToInfer").asBoolean()).isFalse();
      assertThat(plan.get("parameters").get("nodePackageManager").get("exampleValue").asText()).isEqualTo("npm");
      assertThat(plan.get("parameters").get("nodePackageManager").get("defaultValue").isNull()).isTrue();
      assertThat(textValues(plan.get("parameters").get("nodePackageManager").get("allowedValues"))).containsExactly("npm", "pnpm");
      assertThat(plan.get("parameters").get("endOfLine").get("source").asText()).isEqualTo("DEFAULT");
      assertThat(plan.get("parameters").get("endOfLine").get("value").asText()).isEqualTo("lf");
      assertThat(plan.get("parameters").get("endOfLine").get("defaultValue").asText()).isEqualTo("lf");
      assertThat(plan.get("parameters").get("endOfLine").get("safeToInfer").asBoolean()).isTrue();
      assertThat(plan.get("parameters").get("indentSize").get("source").asText()).isEqualTo("DEFAULT");
      assertThat(plan.get("parameters").get("indentSize").get("value").asInt()).isEqualTo(2);
      assertThat(plan.get("parameters").get("indentSize").get("defaultValue").asInt()).isEqualTo(2);
      assertThat(plan.get("parameters").get("indentSize").get("safeToInfer").asBoolean()).isTrue();
      assertThat(plan.get("missingParameters").findValuesAsText("name")).containsExactlyInAnyOrder(
        "project-name",
        "base-name",
        "node-package-manager"
      );
      assertThat(plan.get("invalidParameters")).isEmpty();
      assertThat(plan.get("unresolvedExecutionDecisions").findValuesAsText("name")).containsExactly("commit");
      assertThat(plan.get("unresolvedExecutionDecisions").get(0).get("safeToInfer").asBoolean()).isFalse();
      assertThat(plan.get("nextAction").asText()).contains("Ask the user").contains("do not generate an executable command");
    }

    @Test
    void shouldReportDefaultProjectPathInInitPlan(CapturedOutput output) throws IOException {
      String[] args = { "apply", "init", "--plan", "--format", "json" };

      int exitCode = commandLine(modules, projects).execute(args);

      JsonNode plan = jsonPlan(output);
      assertThat(exitCode).isZero();
      assertThat(plan.get("status").asText()).isEqualTo("NEEDS_USER_INPUT");
      assertThat(plan.get("executable").asBoolean()).isFalse();
      assertThat(plan.get("parameters").get("projectPath").get("value").asText()).isEqualTo(".");
      assertThat(plan.get("parameters").get("projectPath").get("source").asText()).isEqualTo("DEFAULT");
      assertThat(plan.get("unresolvedExecutionDecisions").findValuesAsText("name")).containsExactly("commit");
    }

    @Test
    void shouldReportInvalidNodePackageManagerInInitPlanWithoutChangingProject(CapturedOutput output) throws IOException {
      Path projectPath = setupProjectTestFolder();
      Path historyFile = projectPath.resolve(".seed4j").resolve("modules").resolve("history.json");
      String originalHistory = Files.readString(historyFile);
      String[] yarnArgs = {
        "apply",
        "init",
        "--project-path",
        projectPath.toString(),
        "--base-name",
        "seed4jSampleApplication",
        "--project-name",
        "Seed4J Sample Application",
        "--node-package-manager",
        "yarn",
        "--commit",
        "--plan",
        "--format",
        "json",
      };
      String[] yarnClassicArgs = {
        "apply",
        "init",
        "--project-path",
        projectPath.toString(),
        "--base-name",
        "seed4jSampleApplication",
        "--project-name",
        "Seed4J Sample Application",
        "--node-package-manager",
        "yarn-classic",
        "--commit",
        "--plan",
        "--format",
        "json",
      };

      int yarnExitCode = commandLine(modules, projects).execute(yarnArgs);
      JsonNode yarnPlan = jsonPlan(output);
      int yarnClassicExitCode = commandLine(modules, projects).execute(yarnClassicArgs);
      JsonNode yarnClassicPlan = jsonPlan(output);

      assertThat(yarnExitCode).isZero();
      assertThat(yarnClassicExitCode).isZero();
      assertThat(GitTestUtil.getCommits(projectPath)).isEmpty();
      assertThat(Files.readString(historyFile)).isEqualTo(originalHistory);
      assertThat(yarnPlan.get("status").asText()).isEqualTo("NEEDS_USER_INPUT");
      assertThat(yarnPlan.get("executable").asBoolean()).isFalse();
      assertThat(yarnPlan.get("parameters").get("nodePackageManager").get("value").asText()).isEqualTo("yarn");
      assertThat(yarnPlan.get("parameters").get("nodePackageManager").get("valid").asBoolean()).isFalse();
      assertThat(textValues(yarnPlan.get("parameters").get("nodePackageManager").get("allowedValues"))).containsExactly("npm", "pnpm");
      assertThat(yarnPlan.get("invalidParameters").get(0).get("name").asText()).isEqualTo("node-package-manager");
      assertThat(yarnPlan.get("invalidParameters").get(0).get("value").asText()).isEqualTo("yarn");
      assertThat(textValues(yarnPlan.get("invalidParameters").get(0).get("allowedValues"))).containsExactly("npm", "pnpm");
      assertThat(yarnClassicPlan.get("status").asText()).isEqualTo("NEEDS_USER_INPUT");
      assertThat(yarnClassicPlan.get("executable").asBoolean()).isFalse();
      assertThat(yarnClassicPlan.get("invalidParameters").get(0).get("name").asText()).isEqualTo("node-package-manager");
      assertThat(yarnClassicPlan.get("invalidParameters").get(0).get("value").asText()).isEqualTo("yarn-classic");
      assertThat(textValues(yarnClassicPlan.get("invalidParameters").get(0).get("allowedValues"))).containsExactly("npm", "pnpm");
      assertThat(yarnClassicPlan.get("nextAction").asText()).contains("Ask the user").contains("node-package-manager");
    }

    @Test
    void shouldPlanExplicitInitInputsAsExecutableWithoutChangingProject(CapturedOutput output) throws IOException {
      Path projectPath = setupProjectTestFolder();
      Path historyFile = projectPath.resolve(".seed4j").resolve("modules").resolve("history.json");
      String originalHistory = Files.readString(historyFile);
      String[] args = {
        "apply",
        "init",
        "--project-path",
        projectPath.toString(),
        "--base-name",
        "seed4jSampleApplication",
        "--project-name",
        "Seed4J Sample Application",
        "--node-package-manager",
        "npm",
        "--no-commit",
        "--plan",
        "--format",
        "json",
      };

      int exitCode = commandLine(modules, projects).execute(args);

      JsonNode plan = jsonPlan(output);
      assertThat(exitCode).isZero();
      assertThat(GitTestUtil.getCommits(projectPath)).isEmpty();
      assertThat(Files.readString(historyFile)).isEqualTo(originalHistory);
      assertThat(plan.get("status").asText()).isEqualTo("RESOLVED");
      assertThat(plan.get("executable").asBoolean()).isTrue();
      assertThat(plan.get("parameters").get("projectName").get("value").asText()).isEqualTo("Seed4J Sample Application");
      assertThat(plan.get("parameters").get("projectName").get("source").asText()).isEqualTo("EXPLICIT");
      assertThat(plan.get("parameters").get("baseName").get("value").asText()).isEqualTo("seed4jSampleApplication");
      assertThat(plan.get("parameters").get("baseName").get("source").asText()).isEqualTo("EXPLICIT");
      assertThat(plan.get("parameters").get("nodePackageManager").get("value").asText()).isEqualTo("npm");
      assertThat(plan.get("parameters").get("nodePackageManager").get("source").asText()).isEqualTo("EXPLICIT");
      assertThat(plan.get("parameters").get("nodePackageManager").get("valid").asBoolean()).isTrue();
      assertThat(plan.get("executionDecisions").get("commit").get("value").asBoolean()).isFalse();
      assertThat(plan.get("executionDecisions").get("commit").get("source").asText()).isEqualTo("EXPLICIT");
      assertThat(plan.get("executionDecisions").get("commit").get("safeToInfer").asBoolean()).isFalse();
      assertThat(plan.get("missingParameters")).isEmpty();
      assertThat(plan.get("invalidParameters")).isEmpty();
      assertThat(plan.get("unresolvedExecutionDecisions")).isEmpty();
    }

    @Test
    void shouldPlanPnpmInitInputsAsExecutableWithoutChangingProject(CapturedOutput output) throws IOException {
      Path projectPath = setupProjectTestFolder();
      Path historyFile = projectPath.resolve(".seed4j").resolve("modules").resolve("history.json");
      String originalHistory = Files.readString(historyFile);
      String[] args = {
        "apply",
        "init",
        "--project-path",
        projectPath.toString(),
        "--base-name",
        "seed4jSampleApplication",
        "--project-name",
        "Seed4J Sample Application",
        "--node-package-manager",
        "pnpm",
        "--commit",
        "--plan",
        "--format",
        "json",
      };

      int exitCode = commandLine(modules, projects).execute(args);

      JsonNode plan = jsonPlan(output);
      assertThat(exitCode).isZero();
      assertThat(GitTestUtil.getCommits(projectPath)).isEmpty();
      assertThat(Files.readString(historyFile)).isEqualTo(originalHistory);
      assertThat(plan.get("status").asText()).isEqualTo("RESOLVED");
      assertThat(plan.get("executable").asBoolean()).isTrue();
      assertThat(plan.get("parameters").get("nodePackageManager").get("value").asText()).isEqualTo("pnpm");
      assertThat(plan.get("parameters").get("nodePackageManager").get("valid").asBoolean()).isTrue();
      assertThat(plan.get("executionDecisions").get("commit").get("value").asBoolean()).isTrue();
      assertThat(plan.get("missingParameters")).isEmpty();
      assertThat(plan.get("invalidParameters")).isEmpty();
      assertThat(plan.get("unresolvedExecutionDecisions")).isEmpty();
    }

    @Test
    void shouldPlanInitInputsFromProjectHistoryAsExecutableWithExplicitCommit(CapturedOutput output) throws IOException {
      Path projectPath = setupProjectTestFolder();
      String[] initModuleArgs = {
        "apply",
        "init",
        "--project-path",
        projectPath.toString(),
        "--base-name",
        "seed4jSampleApplication",
        "--project-name",
        "Seed4J Sample Application",
        "--node-package-manager",
        "npm",
        "--no-commit",
      };
      int initModuleExitCode = commandLine(modules, projects).execute(initModuleArgs);
      Path historyFile = projectPath.resolve(".seed4j").resolve("modules").resolve("history.json");
      String historyAfterInit = Files.readString(historyFile);
      String[] args = { "apply", "init", "--project-path", projectPath.toString(), "--commit", "--plan", "--format", "json" };

      int exitCode = commandLine(modules, projects).execute(args);

      JsonNode plan = jsonPlan(output);
      assertThat(initModuleExitCode).isZero();
      assertThat(exitCode).isZero();
      assertThat(GitTestUtil.getCommits(projectPath)).isEmpty();
      assertThat(Files.readString(historyFile)).isEqualTo(historyAfterInit);
      assertThat(plan.get("status").asText()).isEqualTo("RESOLVED");
      assertThat(plan.get("executable").asBoolean()).isTrue();
      assertThat(plan.get("parameters").get("projectName").get("value").asText()).isEqualTo("Seed4J Sample Application");
      assertThat(plan.get("parameters").get("projectName").get("source").asText()).isEqualTo("PROJECT_HISTORY");
      assertThat(plan.get("parameters").get("baseName").get("value").asText()).isEqualTo("seed4jSampleApplication");
      assertThat(plan.get("parameters").get("baseName").get("source").asText()).isEqualTo("PROJECT_HISTORY");
      assertThat(plan.get("parameters").get("nodePackageManager").get("value").asText()).isEqualTo("npm");
      assertThat(plan.get("parameters").get("nodePackageManager").get("source").asText()).isEqualTo("PROJECT_HISTORY");
      assertThat(plan.get("executionDecisions").get("commit").get("value").asBoolean()).isTrue();
      assertThat(plan.get("executionDecisions").get("commit").get("source").asText()).isEqualTo("EXPLICIT");
      assertThat(plan.get("missingParameters")).isEmpty();
      assertThat(plan.get("invalidParameters")).isEmpty();
      assertThat(plan.get("unresolvedExecutionDecisions")).isEmpty();
    }

    private List<String> textValues(JsonNode jsonArray) {
      List<String> values = new ArrayList<>();
      jsonArray.forEach(value -> values.add(value.asText()));

      return values;
    }

    private JsonNode jsonPlan(CapturedOutput output) throws IOException {
      String json = output
        .getOut()
        .lines()
        .filter(line -> line.startsWith("{"))
        .reduce((first, second) -> second)
        .orElseThrow();

      return objectMapper.readTree(json);
    }

    @Test
    void shouldApplyInitModuleWithCommitDefaultValue() throws IOException {
      Path projectPath = setupProjectTestFolder();
      String[] args = {
        "apply",
        "init",
        "--project-path",
        projectPath.toString(),
        "--base-name",
        "seed4jSampleApplication",
        "--project-name",
        "Seed4J Sample Application",
        "--node-package-manager",
        "npm",
      };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(GitTestUtil.getCommits(projectPath)).contains("Apply module: init");
    }

    @Test
    void shouldNotApplyInitModuleWithInvalidNodePackageManager(CapturedOutput output) throws IOException {
      Path projectPath = setupProjectTestFolder();
      Path historyFile = projectPath.resolve(".seed4j").resolve("modules").resolve("history.json");
      String originalHistory = Files.readString(historyFile);
      String[] yarnArgs = {
        "apply",
        "init",
        "--project-path",
        projectPath.toString(),
        "--base-name",
        "seed4jSampleApplication",
        "--project-name",
        "Seed4J Sample Application",
        "--node-package-manager",
        "yarn",
      };
      String[] yarnClassicArgs = {
        "apply",
        "init",
        "--project-path",
        projectPath.toString(),
        "--base-name",
        "seed4jSampleApplication",
        "--project-name",
        "Seed4J Sample Application",
        "--node-package-manager",
        "yarn-classic",
      };

      int yarnExitCode = commandLine(modules, projects).execute(yarnArgs);
      int yarnClassicExitCode = commandLine(modules, projects).execute(yarnClassicArgs);

      assertThat(yarnExitCode).isEqualTo(2);
      assertThat(yarnClassicExitCode).isEqualTo(2);
      assertThat(GitTestUtil.getCommits(projectPath)).isEmpty();
      assertThat(Files.readString(historyFile)).isEqualTo(originalHistory);
      assertThat(output)
        .contains("Invalid value for --node-package-manager: yarn")
        .contains("Invalid value for --node-package-manager: yarn-classic")
        .contains("Allowed values: npm, pnpm")
        .contains("Run seed4j apply init --plan --format json before executing init")
        .doesNotContain("Exception")
        .doesNotContain("at com.seed4j");
    }

    private Object projectPropertyValue(Path projectPath, String propertyKey) {
      ProjectHistory history = projects.getHistory(new ProjectPath(projectPath.toString()));
      return history.latestProperties().parameters().getOrDefault(propertyKey, null);
    }

    @Test
    void shouldApplyInitModuleWithCommit() throws IOException {
      Path projectPath = setupProjectTestFolder();
      String[] args = {
        "apply",
        "init",
        "--project-path",
        projectPath.toString(),
        "--base-name",
        "seed4jSampleApplication",
        "--project-name",
        "Seed4J Sample Application",
        "--node-package-manager",
        "npm",
        "--commit",
      };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(GitTestUtil.getCommits(projectPath)).contains("Apply module: init");
    }

    @Test
    void shouldApplyInitModuleWithoutCommit() throws IOException {
      Path projectPath = setupProjectTestFolder();
      String[] args = {
        "apply",
        "init",
        "--project-path",
        projectPath.toString(),
        "--base-name",
        "seed4jSampleApplication",
        "--project-name",
        "Seed4J Sample Application",
        "--node-package-manager",
        "npm",
        "--no-commit",
      };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(GitTestUtil.getCommits(projectPath)).isEmpty();
    }

    @Test
    void shouldNotApplyModuleWithInvalidBaseName() throws IOException {
      Path projectPath = setupProjectTestFolder();
      String[] args = {
        "apply",
        "init",
        "--project-path",
        projectPath.toString(),
        "--base-name",
        "my.New@pp",
        "--project-name",
        "Seed4J Sample Application",
        "--node-package-manager",
        "npm",
      };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void shouldApplyInitModuleWithIndentation() throws IOException {
      Path projectPath = setupProjectTestFolder();
      String[] args = {
        "apply",
        "init",
        "--project-path",
        projectPath.toString(),
        "--base-name",
        "seed4JSampleApplication",
        "--project-name",
        "Seed4J Sample Application",
        "--node-package-manager",
        "npm",
        "--indent-size",
        "4",
      };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(projectPropertyValue(projectPath, INDENT_SIZE)).isEqualTo(4);
    }

    @Test
    void shouldApplyInitModuleWithEndOfLine() throws IOException {
      Path projectPath = setupProjectTestFolder();
      String[] args = {
        "apply",
        "init",
        "--project-path",
        projectPath.toString(),
        "--base-name",
        "seed4jSampleApplication",
        "--project-name",
        "Seed4J Sample Application",
        "--node-package-manager",
        "npm",
        "--end-of-line",
        "lf",
      };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(projectPropertyValue(projectPath, END_OF_LINE)).isEqualTo("lf");
    }

    @Test
    void shouldReuseParametersFromPreviousModuleApplications() throws IOException {
      Path projectPath = setupProjectTestFolder();
      String[] initModuleArgs = {
        "apply",
        "init",
        "--project-path",
        projectPath.toString(),
        "--base-name",
        "seed4jSampleApplication",
        "--project-name",
        "Seed4J Sample Application",
        "--node-package-manager",
        "npm",
      };
      int initModuleExitCode = commandLine(modules, projects).execute(initModuleArgs);
      assertThat(initModuleExitCode).isZero();
      String[] mavenJavaModuleArgs = {
        "apply",
        "maven-java",
        "--project-path",
        projectPath.toString(),
        "--package-name",
        "com.my.company",
      };

      int mavenJavaModuleExitCode = commandLine(modules, projects).execute(mavenJavaModuleArgs);

      assertThat(mavenJavaModuleExitCode).isZero();
      assertThat(projectPropertyValue(projectPath, PROJECT_NAME)).isEqualTo("Seed4J Sample Application");
      assertThat(projectPropertyValue(projectPath, BASE_NAME)).isEqualTo("seed4jSampleApplication");
      assertThat(projectPropertyValue(projectPath, PACKAGE_NAME)).isEqualTo("com.my.company");
    }

    @Test
    void shouldRenderVersionOutputUsingProjectBuildMetadata(CapturedOutput output) {
      String[] args = { "--version" };
      RuntimeDisplay runtimeDisplay = RuntimeDisplay.extension(
        Optional.of(new com.seed4j.cli.command.domain.RuntimeDistributionId("company-extension")),
        Optional.of(new com.seed4j.cli.command.domain.RuntimeDistributionVersion("1.0.0"))
      );

      int exitCode = commandLine(modules, projects, runtimeDisplay, "9.9.9", "8.8.8").execute(args);

      assertThat(exitCode).isZero();
      assertThat(output).contains("Seed4J CLI v9.9.9").contains("Seed4J version: 8.8.8");
    }

    @Test
    void shouldUseSafeFallbackWhenNoVersionMetadataIsAvailable(CapturedOutput output) {
      String[] args = { "--version" };
      RuntimeDisplay runtimeDisplay = RuntimeDisplay.standard();

      int exitCode = commandLine(modules, projects, runtimeDisplay, "", "").execute(args);

      assertThat(exitCode).isZero();
      assertThat(output)
        .contains("Seed4J CLI vunknown")
        .contains("Seed4J version: unknown")
        .contains("Runtime mode: standard")
        .doesNotContain("vnull")
        .doesNotContain("version: null")
        .doesNotContain("Distribution ID")
        .doesNotContain("Distribution version");
    }

    @Test
    void shouldShowVersion(CapturedOutput output) {
      String[] args = { "--version" };

      int exitCode = commandLine(modules, projects).execute(args);

      assertThat(exitCode).isZero();
      assertThat(output)
        .contains("Seed4J CLI v1")
        .contains("Seed4J version: 2")
        .contains("Runtime mode: standard")
        .doesNotContain("Distribution ID")
        .doesNotContain("Distribution version");
    }

    @Test
    void shouldShowRuntimeModeAndDistributionInVersionOutput(CapturedOutput output) {
      String[] args = { "--version" };
      RuntimeDisplay runtimeDisplay = RuntimeDisplay.extension(
        Optional.of(new com.seed4j.cli.command.domain.RuntimeDistributionId("company-extension")),
        Optional.of(new com.seed4j.cli.command.domain.RuntimeDistributionVersion("1.0.0"))
      );

      int exitCode = commandLine(modules, projects, runtimeDisplay).execute(args);

      assertThat(exitCode).isZero();
      assertThat(output)
        .contains("Seed4J CLI v1")
        .contains("Seed4J version: 2")
        .contains("Runtime mode: extension")
        .contains("Distribution ID: company-extension")
        .contains("Distribution version: 1.0.0");
    }
  }
}
