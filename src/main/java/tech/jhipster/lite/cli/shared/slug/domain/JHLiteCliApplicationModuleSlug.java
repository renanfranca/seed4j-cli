package tech.jhipster.lite.cli.shared.slug.domain;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import tech.jhipster.lite.cli.shared.generation.domain.ExcludeFromGeneratedCodeCoverage;
import tech.jhipster.lite.module.domain.resource.JHipsterModuleRank;
import tech.jhipster.lite.module.domain.resource.JHipsterModuleSlugFactory;

@ExcludeFromGeneratedCodeCoverage
public enum JHLiteCliApplicationModuleSlug implements JHipsterModuleSlugFactory {

  // Add here the slugs of your modules
  // e.g.: MY_MODULE("my-module", JHipsterModuleRank.RANK_B),
  ;

  private static final Map<String, JHLiteCliApplicationModuleSlug> moduleSlugMap = Stream.of(values()).collect(
    Collectors.toMap(JHLiteCliApplicationModuleSlug::get, Function.identity())
  );
  private final String slug;
  private final JHipsterModuleRank rank;

  @SuppressWarnings("java:S1144")
  JHLiteCliApplicationModuleSlug(String slug, JHipsterModuleRank rank) {
    this.slug = slug;
    this.rank = rank;
  }

  @Override
  public String get() {
    return slug;
  }

  @Override
  public JHipsterModuleRank rank() {
    return rank;
  }

  public static Optional<JHLiteCliApplicationModuleSlug> fromString(String slug) {
    return Optional.ofNullable(moduleSlugMap.get(slug));
  }
}
