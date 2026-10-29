package com.seed4j.cli.shared.distribution.infrastructure.secondary;

import com.seed4j.cli.shared.distribution.domain.RuntimeExtensionSupport;
import com.seed4j.cli.shared.error.domain.Assert;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.springframework.stereotype.Component;

@Component
public class ClasspathBuildCapabilities {

  private static final String DISTRIBUTION_RESOURCE = "/META-INF/seed4j-cli-distribution.properties";

  private final DistributionPropertiesStream propertiesStream;

  public ClasspathBuildCapabilities() {
    this(() -> ClasspathBuildCapabilities.class.getResourceAsStream(DISTRIBUTION_RESOURCE));
  }

  ClasspathBuildCapabilities(DistributionPropertiesStream propertiesStream) {
    Assert.notNull("propertiesStream", propertiesStream);
    this.propertiesStream = propertiesStream;
  }

  public RuntimeExtensionSupport runtimeExtensions() {
    try {
      InputStream input = propertiesStream.open();
      if (input == null) {
        return RuntimeExtensionSupport.SUPPORTED;
      }
      Properties properties = new Properties();
      try {
        properties.load(input);
      } finally {
        input.close();
      }
      return switch (properties.getProperty("runtime-extensions", "supported")) {
        case "supported" -> RuntimeExtensionSupport.SUPPORTED;
        case "suspended" -> RuntimeExtensionSupport.SUSPENDED;
        default -> throw new IllegalStateException("Invalid runtime extension build capability");
      };
    } catch (IOException exception) {
      throw new IllegalStateException("Could not read build capabilities", exception);
    }
  }

  @FunctionalInterface
  interface DistributionPropertiesStream {
    InputStream open() throws IOException;
  }
}
