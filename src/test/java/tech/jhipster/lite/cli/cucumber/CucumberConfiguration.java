package tech.jhipster.lite.cli.cucumber;

import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import tech.jhipster.lite.cli.JHLiteCliApplicationApp;
import tech.jhipster.lite.cli.cucumber.CucumberConfiguration.CucumberRestTemplateConfiguration;
import tech.jhipster.lite.cli.cucumber.rest.CucumberRestTemplate;
import tech.jhipster.lite.cucumber.rest.CucumberRestTestContext;

@ActiveProfiles("test")
@CucumberContextConfiguration
@SpringBootTest(
  classes = { JHLiteCliApplicationApp.class, CucumberRestTemplateConfiguration.class },
  webEnvironment = WebEnvironment.RANDOM_PORT
)
public class CucumberConfiguration {

  private final TestRestTemplate rest;

  CucumberConfiguration(TestRestTemplate rest) {
    this.rest = rest;
  }

  @Before
  public void resetTestContext() {
    CucumberRestTestContext.reset();
  }

  @Before
  public void loadInterceptors() {
    ClientHttpRequestFactory requestFactory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());

    RestTemplate template = rest.getRestTemplate();
    template.setRequestFactory(requestFactory);
    template.setInterceptors(List.of(saveLastResultInterceptor()));
    template.getMessageConverters().addFirst(new StringHttpMessageConverter(StandardCharsets.UTF_8));
  }

  private ClientHttpRequestInterceptor saveLastResultInterceptor() {
    return (request, body, execution) -> {
      ClientHttpResponse response = execution.execute(request, body);

      CucumberRestTestContext.addResponse(request, response, execution, body);

      return response;
    };
  }

  @TestConfiguration
  static class CucumberRestTemplateConfiguration {

    @Bean
    CucumberRestTemplate cucumberRestTemplate(TestRestTemplate rest) {
      return new CucumberRestTemplate(rest);
    }
  }
}
