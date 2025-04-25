package tech.jhipster.lite.cli.cucumber;

import static io.cucumber.junit.platform.engine.Constants.*;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectDirectories;
import org.junit.platform.suite.api.Suite;
import tech.jhipster.lite.cli.ComponentTest;

@Suite(failIfNoTests = false)
@ComponentTest
@IncludeEngines("cucumber")
@SuppressWarnings("java:S2187")
@SelectDirectories("src/test/features")
@ConfigurationParameter(
  key = GLUE_PROPERTY_NAME,
  value = "tech.jhipster.lite.cli, tech.jhipster.lite.module.infrastructure.primary, tech.jhipster.lite.project.infrastructure.primary"
)
@ConfigurationParameter(
  key = PLUGIN_PROPERTY_NAME,
  value = "pretty, json:target/cucumber/cucumber.json, html:target/cucumber/cucumber.htm, junit:target/cucumber/TEST-cucumber.xml"
)
public class CucumberTest {}
