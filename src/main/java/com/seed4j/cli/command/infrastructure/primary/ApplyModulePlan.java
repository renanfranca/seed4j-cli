package com.seed4j.cli.command.infrastructure.primary;

import java.util.List;
import java.util.Map;

record ApplyModulePlan(
  ApplyPlanStatus status,
  boolean executable,
  String module,
  Map<String, PlanParameter> parameters,
  Map<String, PlanExecutionDecision> executionDecisions,
  List<MissingPlanParameter> missingParameters,
  List<InvalidPlanParameter> invalidParameters,
  List<UnresolvedExecutionDecision> unresolvedExecutionDecisions,
  String nextAction
) {}
