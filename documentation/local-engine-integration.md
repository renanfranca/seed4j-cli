# Local integration with the modular Seed4J engine

This branch builds a local CLI against Seed4J commit
`3cab6f6592bbe0eb1cc5811a530626e9a141a044` from PR #14974. It uses
`com.seed4j:seed4j-engine:2.2.1-SNAPSHOT` and
`com.seed4j:seed4j-core-modules:2.2.1-SNAPSHOT`, with the transitive API module.
It does not follow later PR commits. This is a local build identity, not an npm channel.

The command compatibility reference is CLI revision
`d46e3b45bc7eaac2e665c02b972087a590bd6c85`, especially
[Commands](Commands.md) and the [apply-set specification](../.agent/specifications/apply-set-execution.md).
Runtime extensions are the approved exception to that reference.

## Prepare isolated upstream artifacts

Use Java 25 and Node.js 22 or newer. Run these commands from the CLI checkout.
Keep the temporary directory until all CLI builds and verification are finished.
Do not use or modify an existing Seed4J checkout or the shared Maven repository.

```bash
cli_checkout="$PWD"
integration_dir="$(mktemp -d /tmp/seed4j-modular-integration.XXXXXX)"
upstream_commit=3cab6f6592bbe0eb1cc5811a530626e9a141a044
mkdir -p "$integration_dir/evidence" "$integration_dir/m2"
git init "$integration_dir/upstream"
git -C "$integration_dir/upstream" fetch --depth=1 https://github.com/seed4j/seed4j.git "$upstream_commit"
git -C "$integration_dir/upstream" checkout --detach FETCH_HEAD
test "$(git -C "$integration_dir/upstream" rev-parse HEAD)" = "$upstream_commit"
git -C "$integration_dir/upstream" rev-parse HEAD > "$integration_dir/evidence/upstream-sha.txt"
cd "$integration_dir/upstream"
./mvnw -B -Dmaven.repo.local="$integration_dir/m2" \
  -pl seed4j-core-modules -am clean install -DskipTests \
  > "$integration_dir/evidence/upstream-build.log" 2>&1
cd "$cli_checkout"
```

This reactor builds the API, engine, reusable test support and core modules without
building the server or frontend. The CLI uses the engine's `tests` classifier for
`GitTestUtil`; it does not depend on `seed4j-test`. `-DskipTests` preserves test
compilation needed to produce the engine fixtures. At the pinned revision,
upstream Failsafe still runs 208 core Cucumber scenarios with this command.
Artifact preparation is not a substitute for a complete upstream validation gate.

## Build and exercise the CLI

```bash
npm ci --ignore-scripts
./mvnw -B -Dmaven.repo.local="$integration_dir/m2" clean test \
  > "$integration_dir/evidence/cli-clean-test.log" 2>&1
./mvnw -B -Dmaven.repo.local="$integration_dir/m2" package -DskipTests \
  > "$integration_dir/evidence/cli-package.log" 2>&1
./mvnw -B -Dmaven.repo.local="$integration_dir/m2" \
  -Dseed4j.integration.evidence="$integration_dir/evidence/packaged" \
  -Dit.test=LocalIntegrationPackagedJarIT,AgentSkillInstallationCliIT,ExtensionRuntimeBootstrapListPackagedJarSmokeIT,ExtensionRuntimeBootstrapApplyPackagedJarSmokeIT \
  failsafe:integration-test failsafe:verify \
  > "$integration_dir/evidence/packaged.log" 2>&1
./mvnw -B -Dmaven.repo.local="$integration_dir/m2" dependency:tree \
  -Dscope=runtime -DoutputFile="$integration_dir/evidence/runtime-dependencies.txt"
npm run prettier:check
habit-hooks
```

Run these build phases sequentially: `clean` removes the JAR used by the packaged
tests. The final artifact is `target/seed4j-cli-0.0.0-SNAPSHOT.jar`.
The Maven validation phase requires a full lowercase SHA in
`seed4j.integration-commit`. Runtime metadata for a local build also requires the SHA;
invalid local metadata fails instead of claiming a stable identity.

The complete local gate is a separate contributor action:

```bash
./mvnw -B -Dmaven.repo.local="$integration_dir/m2" clean verify \
  > "$integration_dir/evidence/cli-clean-verify.log" 2>&1
```

Report its exit code and a concise summary of failures. This includes coverage and
other checks beyond the agent's `clean test` and selected Failsafe runs.

## Identify and run the local JAR

Use an isolated home and a project outside every existing Git checkout:

```bash
mkdir -p "$integration_dir/home"
git config --file "$integration_dir/home/.gitconfig" user.name 'Integration Test'
git config --file "$integration_dir/home/.gitconfig" user.email 'integration@example.com'
java -Duser.home="$integration_dir/home" -jar target/seed4j-cli-0.0.0-SNAPSHOT.jar --version
java -Duser.home="$integration_dir/home" -jar target/seed4j-cli-0.0.0-SNAPSHOT.jar \
  apply init --project-path "$integration_dir/example" \
  --project-name 'Integration App' --base-name integrationApp --node-package-manager npm
java -Duser.home="$integration_dir/home" -jar target/seed4j-cli-0.0.0-SNAPSHOT.jar \
  apply maven-java --project-path "$integration_dir/example" --plan
java -Duser.home="$integration_dir/home" -jar target/seed4j-cli-0.0.0-SNAPSHOT.jar \
  apply maven-java --project-path "$integration_dir/example" --package-name com.example.integration
java -Duser.home="$integration_dir/home" -jar target/seed4j-cli-0.0.0-SNAPSHOT.jar \
  apply java-base --project-path "$integration_dir/example"
```

Version output includes:

```text
Release channel: local-integration
Seed4J version: 2.2.1-SNAPSHOT
Seed4J upstream commit: 3cab6f6592bbe0eb1cc5811a530626e9a141a044
Runtime mode: standard
```

The packaged metadata uses `integration-upstream-commit` for this local provenance.
It does not restore the forbidden legacy `seed4j-upstream-commit` field. Experimental
published snapshots still derive provenance from the complete SHA in their Maven
version. There is no new npm dist tag, publishing flow or extension format.

## Extension suspension and configuration

The local build packages `runtime-extensions=suspended`; adapters translate that
capability into `RuntimeExtensionSupport.SUSPENDED`. This value is build metadata,
not a user configuration option.

`seed4j-extension` is absent from list, apply help, apply-set catalogs and completion.
An explicit request exits `2` with a local integration suspension diagnostic. It
does not attribute this restriction to Central Portal retention.

The `extension` group remains visible. `extension --help` explains the suspension.
Valid `extension install`, `extension enable` and `extension disable` requests exit
`2` and leave configuration, artifacts and caches unchanged.

If `~/.config/seed4j-cli/config.yml` selects extension mode, bootstrap exits `1`
before loading or validating any extension artifact, even when artifacts are missing.
Select standard mode manually to use this build:

```yaml
seed4j:
  runtime:
    mode: standard
```

Bootstrap never rewrites this selection automatically. Tests for supported extension
installation and packaged loading are explicitly conditioned on build capability;
their skipped results are not evidence that extensions work with this integration.
New extensions, Maven resolution, CI adaptation and publication remain future work.

## History and evidence

The CLI reads history exclusively with `ProjectHistoryApplicationService.get(...)`
and the `com.seed4j.history` types. `EngineModuleSetPlanningHistoryReader` adapts
that API to the existing planning port. The engine alone writes application history.
No CLI JSON reader, migration writer or extra history entry is involved.

Both `.seed4j/modules/history.json` with an `actions` array and individual
`.seed4j/modules/yyyyMMddHHmmss-<module>.json` or
`.seed4j/modules/yyyyMMddHHmmssSSS-<module>.json` records retain their previous meaning. Each action carries
`module`, ISO instant `date`, and `properties`. Explicit options override stored
properties. Planning reads those formats without rewriting them.
The fixed fixtures in `src/test/resources/history/` follow `PersistedProjectHistory`,
`PersistedProjectAction` and `FileSystemProjectsRepository` at historical upstream
revision `4646ed8310c45e1e0585985a06b9e06f11ee5ead`; their expected values are not
generated by the new reader.

`LocalIntegrationPackagedJarIT` retains commands, separate stdout/stderr, exit codes,
file/directory snapshots and generated projects under the configured evidence root.
Its default root is `/tmp/seed4j-cli-integration-evidence`. It refuses a root inside a
Git checkout: upstream JGit searches parent directories and can commit the parent
repository when a generated project is nested inside it. Always keep test projects
outside checkouts, including outside `target/`.

The tests compare generated README, POM, Java sources, persisted parameters and
ordered application records. They verify default Git commits, `--no-commit`, explicit
reapplication, legacy history, planning preservation, failure diagnostics and extension
suspension. `packaged-libraries.txt` records the actual nested libraries; the Maven
runtime tree is a second composition inventory. Neither inventory should contain the
monolith, server, Spring MVC, Liquibase or database drivers.

The execution report in [EXECPLAN.md](../EXECPLAN.md) distinguishes source contracts,
observed A1 through A9 evidence, skipped tests and the pending complete gate.
