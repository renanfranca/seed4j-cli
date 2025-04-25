package tech.jhipster.lite.cli.shared.dependencies.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static tech.jhipster.lite.cli.shared.dependencies.domain.JHLiteCliApplicationNpmVersionSource.J_H_LITE_CLI_APPLICATION;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.jhipster.lite.cli.UnitTest;
import tech.jhipster.lite.module.domain.ProjectFiles;
import tech.jhipster.lite.module.domain.npm.NpmPackageName;
import tech.jhipster.lite.module.domain.npm.NpmPackageVersion;

@UnitTest
@ExtendWith(MockitoExtension.class)
class JHLiteCliApplicationNpmVersionReaderTest {

  @Mock
  private ProjectFiles projectFiles;

  @InjectMocks
  private JHLiteCliApplicationNpmVersionReader reader;

  @Test
  void shouldGetVersionFromCustomSource() {
    mockProjectFiles();

    NpmPackageVersion version = reader.get().get(new NpmPackageName("vue"), J_H_LITE_CLI_APPLICATION.build());

    assertThat(version).isEqualTo(new NpmPackageVersion("1.2.3"));
  }

  private void mockProjectFiles() {
    when(projectFiles.readString(anyString())).thenReturn(
      """
      {
        "dependencies": {
          "vue": "1.2.3"
        },
      }
      """
    );
  }
}
