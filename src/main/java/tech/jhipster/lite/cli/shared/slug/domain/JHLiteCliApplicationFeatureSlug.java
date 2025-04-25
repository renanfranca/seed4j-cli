package tech.jhipster.lite.cli.shared.slug.domain;

import tech.jhipster.lite.cli.shared.generation.domain.ExcludeFromGeneratedCodeCoverage;
import tech.jhipster.lite.module.domain.resource.JHipsterFeatureSlugFactory;

@ExcludeFromGeneratedCodeCoverage
public enum JHLiteCliApplicationFeatureSlug implements JHipsterFeatureSlugFactory {

  // Add here the slugs of your features
  // e.g.: MY_FEATURE("my-feature")
  ;

  private final String slug;

  @SuppressWarnings("java:S1144")
  JHLiteCliApplicationFeatureSlug(String slug) {
    this.slug = slug;
  }

  @Override
  public String get() {
    return slug;
  }
}
