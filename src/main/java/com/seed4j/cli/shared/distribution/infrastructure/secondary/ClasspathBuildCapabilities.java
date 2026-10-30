package com.seed4j.cli.shared.distribution.infrastructure.secondary;

import com.seed4j.cli.shared.distribution.domain.RuntimeExtensionSupport;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.springframework.stereotype.Component;

@Component
public class ClasspathBuildCapabilities {

  public RuntimeExtensionSupport runtimeExtensions() {
    try (InputStream input = ClasspathBuildCapabilities.class.getResourceAsStream("/META-INF/seed4j-cli-distribution.properties")) {
      if (input == null) {
        return RuntimeExtensionSupport.SUPPORTED;
      }
      Properties properties = new Properties();
      properties.load(input);
      return switch (properties.getProperty("runtime-extensions", "supported")) {
        case "supported" -> RuntimeExtensionSupport.SUPPORTED;
        case "suspended" -> RuntimeExtensionSupport.SUSPENDED;
        default -> throw new IllegalStateException("Invalid runtime extension build capability");
      };
    } catch (IOException exception) {
      throw new IllegalStateException("Could not read build capabilities", exception);
    }
  }
}
