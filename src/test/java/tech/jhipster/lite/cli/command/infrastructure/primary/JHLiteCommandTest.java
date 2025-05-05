package tech.jhipster.lite.cli.command.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.jhipster.lite.TestProjects.newTestFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import picocli.CommandLine;
import tech.jhipster.lite.module.infrastructure.secondary.git.GitTestUtil;

@ExtendWith({ SpringExtension.class, OutputCaptureExtension.class })
@ContextConfiguration(classes = CommandContextTestConfig.class)
class JHLiteCommandTest {

  @Autowired
  private JHLiteCommand jhliteCommand;

  @Autowired
  private CommandLine.IFactory factory;

  @Test
  void shouldShowHelpMessageWhenNoCommand(CapturedOutput output) {
    String[] args = {};
    CommandLine cmd = new CommandLine(jhliteCommand, factory);

    int exitCode = cmd.execute(args);

    assertThat(exitCode).isEqualTo(2);
    assertThat(output.toString()).contains(
      """
      JHLite CLI Application
        -h, --help      Show this help message and exit.
        -V, --version   Print version information and exit.

      Commands:
      """
    );
  }

  @Test
  void shouldListModules(CapturedOutput output) {
    String[] args = { "list" };
    CommandLine cmd = new CommandLine(jhliteCommand, factory);

    int exitCode = cmd.execute(args);

    assertThat(exitCode).isZero();
    assertThat(output.toString()).contains("Listing all jhipster-lite modules");
    assertThat(output.toString()).contains("init");
    assertThat(output.toString()).contains("prettier");
  }

  @Test
  void shouldApplyInitModuleWithDefaultOptions() throws IOException {
    String projectFolder = newTestFolder();
    Path projectPath = Path.of(projectFolder);
    Files.createDirectories(projectPath);
    loadGitConfig(projectPath);
    String[] args = { "apply", "init", "--project-path", projectFolder };
    CommandLine cmd = new CommandLine(jhliteCommand, factory);

    int exitCode = cmd.execute(args);

    assertThat(exitCode).isZero();
    assertThat(GitTestUtil.getCommits(projectPath)).contains("Apply module: init");
  }

  @Test
  void shouldApplyInitModuleWithCommit() throws IOException {
    String projectFolder = newTestFolder();
    Path projectPath = Path.of(projectFolder);
    Files.createDirectories(projectPath);
    loadGitConfig(projectPath);
    String[] args = { "apply", "init", "--project-path", projectFolder, "--commit" };
    CommandLine cmd = new CommandLine(jhliteCommand, factory);

    int exitCode = cmd.execute(args);

    assertThat(exitCode).isZero();
    assertThat(GitTestUtil.getCommits(projectPath)).contains("Apply module: init");
  }

  @Test
  void shouldApplyInitModuleWithoutCommit() throws IOException {
    String projectFolder = newTestFolder();
    Path projectPath = setupProjectTestFolder(projectFolder);
    String[] args = { "apply", "init", "--project-path", projectFolder, "--no-commit" };
    CommandLine cmd = new CommandLine(jhliteCommand, factory);

    int exitCode = cmd.execute(args);

    assertThat(exitCode).isZero();
    assertThat(GitTestUtil.getCommits(projectPath)).isEmpty();
  }

  //TODO - use ProjectHistory to check if --package-name was applied correctly
  /**
   *
   * ProjectHistory history = projects.getHistory(new ProjectPath("."));
   *     return history.latestProperties().
   */

  private static Path setupProjectTestFolder(String projectFolder) throws IOException {
    Path projectPath = Path.of(projectFolder);
    Files.createDirectories(projectPath);
    loadGitConfig(projectPath);

    return projectPath;
  }

  private static void loadGitConfig(Path project) {
    GitTestUtil.execute(project, "init");
    GitTestUtil.execute(project, "config", "init.defaultBranch", "main");
    GitTestUtil.execute(project, "config", "user.email", "\"test@jhipster.com\"");
    GitTestUtil.execute(project, "config", "user.name", "\"Test\"");
  }
}
