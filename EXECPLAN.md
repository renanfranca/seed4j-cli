# Integrate the CLI with the modular Seed4J engine

## Purpose and success

Deliver a local executable CLI using `com.seed4j:seed4j-engine` and `seed4j-core-modules:2.2.1-SNAPSHOT` built from exactly `3cab6f6592bbe0eb1cc5811a530626e9a141a044`. Preserve discovery, help, completion, planning, application and historical parameters from CLI revision `d46e3b45bc7eaac2e665c02b972087a590bd6c85`. Suspend extensions explicitly in this build.

## Context and limits

Work on the existing `seed4j-engine-integration` branch. Do not change `/home/renanfranca/projects/seed4j`, publish artifacts, follow newer upstream commits, or send PR feedback. Isolated upstream checkout, Maven repository and evidence live at `/tmp/seed4j-modular-integration.IHbCWw`. Java 25 and Node 24 are available. AGENTS.md reserves the full `clean verify` gate for the user unless explicitly authorized; the user subsequently authorized the agent to run it. Engine owns historical persistence; CLI must use its API without JSON readers or extra writes. Local build metadata must use a new provenance field, preserving experimental snapshot derivation.

## Milestones

1. Inspect pinned upstream and historical sources; build engine/core and engine test fixtures in the isolated Maven repository using `./mvnw -Dmaven.repo.local=/tmp/seed4j-modular-integration.IHbCWw/m2 -pl seed4j-core-modules -am clean install -DskipTests`. Record exact SHA and source findings.
2. Migrate `pom.xml`, `Seed4JCliApp`, history adapters, command fixtures/tests and build metadata. Add typed capability handling to bootstrap and command adapters. Verify focused behavior tests and compile using the isolated repository.
3. Add packaged integration tests covering A1 through A9 from the supplied plan: composition/provenance, discovery, side effect free plans, cross process init/maven/java, both legacy history layouts, apply sets and failures, and suspension of extensions. Preserve command contracts and prove concrete generated content.
4. Update README, Commands, architecture and a reproducible local integration guide. Run `./mvnw -Dmaven.repo.local=/tmp/seed4j-modular-integration.IHbCWw/m2 clean test`, package without repeating tests, execute selected Failsafe scenarios, `npm run prettier:check` and `habit-hooks`. Audit each criterion against both source and observed evidence. Hand off JAR and ask user for `clean verify` with the same repository.

## Progress

- [x] Confirm clean branch and toolchain; reference CLI revision is current HEAD.
- [x] Create isolated directory and clone upstream without modifying original checkout.
- [x] Fetch and inspect pinned SHA, build modular artifacts.
- [x] Implement migration and typed suspension; behavior and architecture tests passed.
- [x] Prove A1 through A9 with tests and recorded observations.
- [x] Reconcile documentation, formatting, habit findings and final audit.
- [x] Run the complete `clean verify` gate after explicit user authorization; record exit 1 from JaCoCo.
- [x] Resolve coverage gaps before claiming the complete gate passes.

## Validation

All A1 through A9 behavior checks passed. The [acceptance evidence report](/tmp/seed4j-modular-integration.IHbCWw/evidence/ACCEPTANCE.md) audits source expectations separately from observations and gives commands, exit codes, output, snapshots and reports for every criterion. Its evidence root is `/tmp/seed4j-modular-integration.IHbCWw/evidence`.

| Criterion | Observed evidence                                                                                                                                                              |
| --------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| A1        | Packaged version with exact SHA, nested library inventory and Maven runtime tree; invalid Maven SHA rejected.                                                                  |
| A2        | Packaged list, apply help and bash completion expose the four required modules and hide the extension generator.                                                               |
| A3        | Absent destinations remain absent; both planners preserve existing project file/history/Git snapshots.                                                                         |
| A4        | Fresh init, maven plan and maven apply in separate processes preserve concrete parameters, ordered history and two default commits.                                            |
| A5        | Fixed legacy aggregated and individual action fixtures reuse values, honor explicit options and remain unchanged after both planners.                                          |
| A6        | Both commit variants execute init before maven-java, report SUCCEEDED and retain explicitly reapplied modules/history.                                                         |
| A7        | Real commands preserve missing-dependency, invalid-set and corrupt-history exits and state; migrated command tests prove opaque API failure and partial-success/skip behavior. |
| A8        | Packaged java-base writes concrete Java sources in com.example.integration using persisted parameters.                                                                         |
| A9        | Install/enable/disable return 2 without changing config/artifacts/cache; extension selection returns 1 before artifact validation, even when artifacts are absent.             |

Final command results:

- Upstream artifact preparation: exit 0, exact SHA confirmed in `upstream-sha.txt`. Its Failsafe configuration also ran 208 Cucumber scenarios despite `skipTests`, all green; this is not a complete upstream validation gate.
- `./mvnw -B -Dmaven.repo.local=/tmp/seed4j-modular-integration.IHbCWw/m2 clean test`: exit 0, 647 tests reported, no failures/errors and one skipped extension container. See `cli-clean-test.log` and copied Surefire reports.
- `package -DskipTests` with the same repository: exit 0. Deliverables: `target/seed4j-cli-0.0.0-SNAPSHOT.jar` and `/tmp/seed4j-modular-integration.IHbCWw/seed4j-cli-local-integration.jar`. SHA-256 is recorded in `evidence/jar-sha256.txt`.
- Selected `failsafe:integration-test failsafe:verify`: exit 0, 13 reported, 11 passed and two old extension loading tests explicitly skipped. The local integration test itself has ten passing scenarios and no skips. See `packaged.log` and copied Failsafe reports.
- Negative SHA validation: `-Dseed4j.integration-commit=invalid validate` returns the expected exit 1 from Maven RequireProperty.
- `npm run prettier:check`: exit 0.
- `habit-hooks`: exit 0, no automated findings. The hook also suggested reviewer delegation; this session did not authorize delegation, so a local structural review used the refactor-design skill and both review references. No additional material refactor was justified.

The user subsequently authorized the full gate. After resolving the in-process coverage gaps for `ClasspathBuildCapabilities`, `BuildRuntimeModeConfigurationRepository`, `UnavailableModuleDiagnosticRenderer`, and `ExtensionRuntimeCommandsTest` (covering both supported and suspended capability states in-process), `./mvnw -B clean verify` exits **0** with 0 Checkstyle violations, all Surefire and Failsafe tests passing, and 100% JaCoCo line and branch coverage (`0 missed lines, 0 missed branches` across all 316 classes).

## Risks and recovery

An initial packaged run put test projects under `target/`; upstream JGit discovered the parent CLI repository and created six commits. The run was stopped and a mixed reset to the initial reference removed only those test-created commits, preserving all edits. The final branch remains at the initial reference with uncommitted implementation files. Packaged tests now reject an evidence root inside any Git checkout and default to `/tmp`. Only the final `packaged-verified` run counts as acceptance evidence. The upstream JGit behavior is documented; its source was not changed.

No blocking engine API incompatibility was observed in A1 through A9. The original Seed4J checkout was not modified. No publication or PR feedback occurred. To reproduce, follow the local guide with a fresh isolated checkout and Maven repository; to discard this integration, remove its uncommitted changes and disposable temporary directory only after preserving any wanted artifacts/evidence. Do not remove the temporary Maven repository before the contributor gate.

## Documentation

The [local integration guide](documentation/local-engine-integration.md) gives exact pinned checkout/build/test commands, standard-mode configuration, packaged identity, legacy history field/filename shapes, evidence layout and extension suspension. README, Commands, the documentation index and hexagonal architecture guide link or describe the local exception. Published experimental provenance still derives from the full-SHA Maven version; the new local field is `integration-upstream-commit`, never the forbidden legacy `seed4j-upstream-commit`.
