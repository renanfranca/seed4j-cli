package com.seed4j.cli.command.infrastructure.primary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seed4j.cli.shared.error.domain.Assert;
import com.seed4j.cli.shared.generation.domain.ExcludeFromGeneratedCodeCoverage;
import com.seed4j.module.application.Seed4JModulesApplicationService;
import com.seed4j.module.domain.Seed4JModuleSlug;
import com.seed4j.module.domain.Seed4JModuleToApply;
import com.seed4j.module.domain.nodejs.NodePackageManager;
import com.seed4j.module.domain.properties.Seed4JModuleProperties;
import com.seed4j.module.domain.properties.Seed4JPropertyDefaultValue;
import com.seed4j.module.domain.properties.Seed4JPropertyDescription;
import com.seed4j.module.domain.properties.Seed4JPropertyKey;
import com.seed4j.module.domain.properties.Seed4JPropertyType;
import com.seed4j.module.domain.resource.Seed4JModuleOperation;
import com.seed4j.module.domain.resource.Seed4JModulePropertiesDefinition;
import com.seed4j.module.domain.resource.Seed4JModulePropertyDefinition;
import com.seed4j.module.domain.resource.Seed4JModuleResource;
import com.seed4j.project.application.ProjectsApplicationService;
import com.seed4j.project.domain.ProjectPath;
import com.seed4j.project.domain.history.ModuleParameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParameterException;

class ApplyModuleSubCommand implements Callable<Integer> {

  private static final String PROJECT_PATH_OPTION = "--project-path";
  private static final String COMMIT_OPTION = "--commit";
  private static final String PLAN_OPTION = "--plan";
  private static final String FORMAT_OPTION = "--format";
  private static final String INIT_MODULE = "init";
  private static final String NODE_PACKAGE_MANAGER_PARAMETER = "nodePackageManager";

  private final Seed4JModulesApplicationService modules;
  private final Seed4JModuleResource module;
  private final CommandSpec commandSpec;
  private final ProjectsApplicationService projects;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public ApplyModuleSubCommand(Seed4JModulesApplicationService modules, Seed4JModuleResource module, ProjectsApplicationService projects) {
    this.modules = modules;
    this.module = module;
    this.projects = projects;
    this.commandSpec = buildCommandSpec(module.slug(), module.apiDoc().operation(), module.propertiesDefinition());
  }

  private CommandSpec buildCommandSpec(
    Seed4JModuleSlug moduleSlug,
    Seed4JModuleOperation operation,
    Seed4JModulePropertiesDefinition properties
  ) {
    CommandSpec spec = CommandSpec.wrapWithoutInspection(this).name(moduleSlug.get()).mixinStandardHelpOptions(true);
    spec.usageMessage().description(escape(operation)).width(160);

    if (INIT_MODULE.equals(moduleSlug.get())) {
      spec.usageMessage().footer(
        """

        Automation note:
        agents should run seed4j apply init --plan --format json;
        agents must not infer required values from folder names or examples;
        values marked safeToInfer=false require asking the user.
        """
      );
    }

    addOptions(spec, properties);

    return spec;
  }

  private String escape(Seed4JModuleOperation operation) {
    return operation.get().replace("%", "%%");
  }

  private void addOptions(CommandSpec spec, Seed4JModulePropertiesDefinition properties) {
    spec.addOption(
      OptionSpec.builder(PROJECT_PATH_OPTION)
        .description("Project Path Folder")
        .paramLabel("<projectpath>")
        .defaultValue(".")
        .type(String.class)
        .build()
    );

    spec.addOption(
      OptionSpec.builder(COMMIT_OPTION)
        .description("Initialize Git if needed and commit generated changes; --no-commit skips Git init and commit")
        .negatable(true)
        .type(Boolean.class)
        .build()
    );

    spec.addOption(
      OptionSpec.builder(PLAN_OPTION).description("Build a non-mutating apply plan").type(Boolean.class).defaultValue("false").build()
    );

    spec.addOption(
      OptionSpec.builder(FORMAT_OPTION)
        .description("Plan output format: text or json")
        .paramLabel("<text|json>")
        .defaultValue("text")
        .type(String.class)
        .build()
    );

    properties.stream().forEach(property ->
      spec.addOption(
        OptionSpec.builder(toDashedFormat(property.key()))
          .description(
            "%s%s".formatted(
              property.description().map(Seed4JPropertyDescription::get).orElse(""),
              property.isMandatory() ? " (required)" : ""
            )
          )
          .paramLabel("<%s%s>".formatted(property.key().get().toLowerCase(), property.isMandatory() ? "*" : ""))
          .type(toOptionType(property.type()))
          .build()
      )
    );
  }

  @ExcludeFromGeneratedCodeCoverage(reason = "There is no Seed4J module using a property with the BOOLEAN type")
  private static Class<?> toOptionType(Seed4JPropertyType type) {
    return switch (type) {
      case BOOLEAN -> boolean.class;
      case INTEGER -> int.class;
      case STRING -> String.class;
    };
  }

  private static String toDashedFormat(Seed4JPropertyKey key) {
    StringBuilder dashed = new StringBuilder("--");
    for (char c : key.get().toCharArray()) {
      if (Character.isUpperCase(c)) {
        dashed.append('-').append(Character.toLowerCase(c));
      } else {
        dashed.append(c);
      }
    }
    return dashed.toString();
  }

  private static String toCamelCaseFormat(String dashed) {
    Assert.notBlank("dashed", dashed);

    String withoutPrefix = dashed.substring(2);
    StringBuilder camelCase = new StringBuilder();

    boolean capitalizeNext = false;
    for (char c : withoutPrefix.toCharArray()) {
      if (c == '-') {
        capitalizeNext = true;
      } else if (capitalizeNext) {
        camelCase.append(Character.toUpperCase(c));
        capitalizeNext = false;
      } else {
        camelCase.append(c);
      }
    }

    return camelCase.toString();
  }

  public CommandSpec commandSpec() {
    return commandSpec;
  }

  @Override
  public Integer call() throws JsonProcessingException {
    if (planning()) {
      printPlan();

      return ExitCode.OK;
    }

    Seed4JModuleProperties properties = new Seed4JModuleProperties(projectPath(), commitEnabled(), parameters());
    Seed4JModuleToApply moduleToApply = new Seed4JModuleToApply(new Seed4JModuleSlug(module.slug().get()), properties);
    modules.apply(moduleToApply);

    return ExitCode.OK;
  }

  private boolean planning() {
    return commandSpec.findOption(PLAN_OPTION).getValue();
  }

  private void printPlan() throws JsonProcessingException {
    if (!INIT_MODULE.equals(module.slug().get())) {
      throw new ParameterException(commandSpec.commandLine(), "--plan currently supports only the init module");
    }

    if (planFormat() != PlanFormat.JSON) {
      throw new ParameterException(commandSpec.commandLine(), "--plan currently supports only --format json");
    }

    commandSpec.commandLine().getOut().println(objectMapper.writeValueAsString(plan()));
  }

  private PlanFormat planFormat() {
    try {
      return PlanFormat.from(commandSpec.findOption(FORMAT_OPTION).getValue());
    } catch (IllegalArgumentException exception) {
      throw new ParameterException(commandSpec.commandLine(), exception.getMessage());
    }
  }

  private ApplyModulePlan plan() {
    Map<String, Object> explicitParameters = parametersFromOptions();
    Map<String, Object> historyParameters = projects.getHistory(new ProjectPath(projectPath())).latestProperties().get();
    Map<String, PlanParameter> planParameters = planParameters(explicitParameters, historyParameters);
    List<MissingPlanParameter> missingParameters = missingParameters(planParameters);
    List<InvalidPlanParameter> invalidParameters = invalidParameters(planParameters);
    Map<String, PlanExecutionDecision> executionDecisions = executionDecisions();
    List<UnresolvedExecutionDecision> unresolvedExecutionDecisions = unresolvedExecutionDecisions(executionDecisions);
    boolean executable = missingParameters.isEmpty() && invalidParameters.isEmpty() && unresolvedExecutionDecisions.isEmpty();

    return new ApplyModulePlan(
      executable ? ApplyPlanStatus.RESOLVED : ApplyPlanStatus.NEEDS_USER_INPUT,
      executable,
      module.slug().get(),
      planParameters,
      executionDecisions,
      missingParameters,
      invalidParameters,
      unresolvedExecutionDecisions,
      nextAction(executable, invalidParameters)
    );
  }

  private Map<String, PlanParameter> planParameters(Map<String, Object> explicitParameters, Map<String, Object> historyParameters) {
    Map<String, PlanParameter> planParameters = new LinkedHashMap<>();
    OptionSpec projectPathOption = commandSpec.findOption(PROJECT_PATH_OPTION);
    PlanValueSource projectPathSource = explicitlyProvided(projectPathOption) ? PlanValueSource.EXPLICIT : PlanValueSource.DEFAULT;
    planParameters.put(
      "projectPath",
      new PlanParameter("project-path", projectPath(), projectPathSource, true, true, List.of(), null, ".")
    );

    module
      .propertiesDefinition()
      .stream()
      .forEach(property -> {
        String propertyName = property.key().get();
        String dashedPropertyName = dashedName(toDashedFormat(property.key()));
        PlanValueSource source = parameterSource(propertyName, explicitParameters, historyParameters);
        Object defaultValue = defaultValue(property);
        Object value = switch (source) {
          case EXPLICIT -> explicitParameters.get(propertyName);
          case PROJECT_HISTORY -> historyParameters.get(propertyName);
          case DEFAULT -> defaultValue;
          case MISSING -> null;
        };
        List<String> allowedValues = allowedValues(propertyName);
        planParameters.put(
          propertyName,
          new PlanParameter(
            dashedPropertyName,
            value,
            source,
            !property.isMandatory(),
            validParameter(source, value, allowedValues),
            allowedValues,
            property.isMandatory() ? defaultValue : null,
            property.isMandatory() ? null : defaultValue
          )
        );
      });

    return planParameters;
  }

  private PlanValueSource parameterSource(
    String propertyName,
    Map<String, Object> explicitParameters,
    Map<String, Object> historyParameters
  ) {
    if (explicitParameters.containsKey(propertyName)) {
      return PlanValueSource.EXPLICIT;
    }

    if (historyParameters.containsKey(propertyName)) {
      return PlanValueSource.PROJECT_HISTORY;
    }

    if (optionalPropertyWithDefault(propertyName).isPresent()) {
      return PlanValueSource.DEFAULT;
    }

    return PlanValueSource.MISSING;
  }

  private Optional<Seed4JModulePropertyDefinition> optionalPropertyWithDefault(String propertyName) {
    return module
      .propertiesDefinition()
      .stream()
      .filter(property -> property.key().get().equals(propertyName))
      .filter(property -> !property.isMandatory())
      .filter(property -> property.defaultValue().isPresent())
      .findFirst();
  }

  private Object defaultValue(Seed4JModulePropertyDefinition property) {
    return property
      .defaultValue()
      .map(Seed4JPropertyDefaultValue::get)
      .map(defaultValue -> typedValue(defaultValue, property.type()))
      .orElse(null);
  }

  private Object typedValue(String value, Seed4JPropertyType type) {
    return switch (type) {
      case BOOLEAN -> Boolean.valueOf(value);
      case INTEGER -> Integer.valueOf(value);
      case STRING -> value;
    };
  }

  private List<String> allowedValues(String propertyName) {
    if (NODE_PACKAGE_MANAGER_PARAMETER.equals(propertyName)) {
      return Arrays.stream(NodePackageManager.values()).map(NodePackageManager::propertyKey).toList();
    }

    return List.of();
  }

  private Boolean validParameter(PlanValueSource source, Object value, List<String> allowedValues) {
    if (source == PlanValueSource.MISSING) {
      return false;
    }

    if (allowedValues.isEmpty()) {
      return true;
    }

    return allowedValues.contains(String.valueOf(value));
  }

  private List<MissingPlanParameter> missingParameters(Map<String, PlanParameter> planParameters) {
    return module
      .propertiesDefinition()
      .stream()
      .filter(Seed4JModulePropertyDefinition::isMandatory)
      .map(property -> planParameters.get(property.key().get()))
      .filter(parameter -> parameter.source() == PlanValueSource.MISSING)
      .map(parameter -> new MissingPlanParameter(parameter.name(), false))
      .toList();
  }

  private List<InvalidPlanParameter> invalidParameters(Map<String, PlanParameter> planParameters) {
    return planParameters
      .values()
      .stream()
      .filter(parameter -> Boolean.FALSE.equals(parameter.valid()))
      .filter(parameter -> parameter.source() != PlanValueSource.MISSING)
      .map(parameter -> new InvalidPlanParameter(parameter.name(), parameter.value(), parameter.allowedValues(), false))
      .toList();
  }

  private Map<String, PlanExecutionDecision> executionDecisions() {
    Map<String, PlanExecutionDecision> executionDecisions = new LinkedHashMap<>();
    OptionSpec commitOption = commandSpec.findOption(COMMIT_OPTION);
    PlanValueSource source = explicitlyProvided(commitOption) ? PlanValueSource.EXPLICIT : PlanValueSource.MISSING;
    executionDecisions.put("commit", new PlanExecutionDecision("commit", commitOption.getValue(), source, false));

    return executionDecisions;
  }

  private List<UnresolvedExecutionDecision> unresolvedExecutionDecisions(Map<String, PlanExecutionDecision> executionDecisions) {
    List<UnresolvedExecutionDecision> unresolvedExecutionDecisions = new ArrayList<>();
    PlanExecutionDecision commitDecision = executionDecisions.get("commit");

    if (commitDecision.source() == PlanValueSource.MISSING) {
      unresolvedExecutionDecisions.add(new UnresolvedExecutionDecision("commit", false));
    }

    return unresolvedExecutionDecisions;
  }

  private String nextAction(boolean executable, List<InvalidPlanParameter> invalidParameters) {
    if (executable) {
      return "The plan is executable. Run apply without --plan when the user confirms.";
    }

    if (!invalidParameters.isEmpty()) {
      String parameterNames = invalidParameters.stream().map(InvalidPlanParameter::name).collect(Collectors.joining(", "));

      return "Ask the user for valid values for %s and do not generate an executable command.".formatted(parameterNames);
    }

    return "Ask the user for values marked safeToInfer=false and do not generate an executable command.";
  }

  private String projectPath() {
    return commandSpec.findOption(PROJECT_PATH_OPTION).getValue();
  }

  private boolean commitEnabled() {
    Boolean commit = commandSpec.findOption(COMMIT_OPTION).getValue();

    return commit == null || commit;
  }

  private Map<String, Object> parameters() {
    ModuleParameters moduleParameters = projects
      .getHistory(new ProjectPath(projectPath()))
      .latestProperties()
      .merge(new ModuleParameters(parametersFromOptions()));

    validateRequiredOptions(moduleParameters);
    validateParameterValues(moduleParameters);

    return moduleParameters.get();
  }

  private Map<String, Object> parametersFromOptions() {
    Map<String, Object> map = new HashMap<>();

    commandSpec
      .options()
      .stream()
      .filter(option -> option.getValue() != null)
      .filter(option -> moduleParameterOption(option.longestName()))
      .forEach(option -> map.put(toCamelCaseFormat(option.longestName()), option.getValue()));

    return map;
  }

  private boolean moduleParameterOption(String optionName) {
    return !List.of(PROJECT_PATH_OPTION, COMMIT_OPTION, PLAN_OPTION, FORMAT_OPTION).contains(optionName);
  }

  private boolean explicitlyProvided(OptionSpec option) {
    return !option.originalStringValues().isEmpty();
  }

  private static String dashedName(String optionName) {
    return optionName.substring(2);
  }

  private void validateRequiredOptions(ModuleParameters moduleParameters) {
    List<OptionSpec> missingOptions = module
      .propertiesDefinition()
      .stream()
      .filter(Seed4JModulePropertyDefinition::isMandatory)
      .filter(property -> !moduleParameters.get().containsKey(property.key().get()))
      .map(property -> commandSpec.findOption(toDashedFormat(property.key())))
      .toList();

    if (!missingOptions.isEmpty()) {
      String missingOptionsDescription = missingOptions
        .stream()
        .map(option -> "'%s=%s'".formatted(option.longestName(), option.paramLabel()))
        .collect(Collectors.joining(", "));

      throw new MissingParameterException(
        commandSpec.commandLine(),
        missingOptions.stream().map(ArgSpec.class::cast).toList(),
        "Missing required options: %s%nRun seed4j apply init --plan --format json before executing init.".formatted(
          missingOptionsDescription
        )
      );
    }
  }

  private void validateParameterValues(ModuleParameters moduleParameters) {
    Object nodePackageManager = moduleParameters.get().get(NODE_PACKAGE_MANAGER_PARAMETER);
    List<String> allowedPackageManagers = allowedValues(NODE_PACKAGE_MANAGER_PARAMETER);

    if (nodePackageManager != null && !allowedPackageManagers.contains(String.valueOf(nodePackageManager))) {
      throw new ParameterException(
        commandSpec.commandLine(),
        "Invalid value for --node-package-manager: %s%nAllowed values: %s%nRun seed4j apply init --plan --format json before executing init.".formatted(
          nodePackageManager,
          String.join(", ", allowedPackageManagers)
        )
      );
    }
  }
}
