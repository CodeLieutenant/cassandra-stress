# Changelog

## [v3.17.3](https://github.com/scylladb/cassandra-stress/releases/tag/v3.17.3) (2025-01-28)

[Full Changelog](https://github.com/scylladb/cassandra-stress/compare/v3.17.2...v3.17.3)

**Merged pull requests:**

- New driver version [\#41](https://github.com/scylladb/cassandra-stress/pull/41) ([dkropachev](https://github.com/dkropachev))

## [v3.17.2](https://github.com/scylladb/cassandra-stress/releases/tag/v3.17.3) (2025-01-21)

[Full Changelog](https://github.com/scylladb/cassandra-stress/compare/v3.17.1...v3.17.2)

**Implemented enhancements:**

- Make it possible to build with custom driver jar [\#39](https://github.com/scylladb/cassandra-stress/pull/39) ([dkropachev](https://github.com/dkropachev))
- feature\(packaging\): debian and rpm packaging [\#37](https://github.com/scylladb/cassandra-stress/pull/37) ([CodeLieutenant](https://github.com/CodeLieutenant))

## [v3.17.1](https://github.com/scylladb/cassandra-stress/releases/tag/v3.17.3) (2024-12-23)

[Full Changelog](https://github.com/scylladb/cassandra-stress/compare/v3.17.0...v3.17.1)

**Merged pull requests:**

- docs: add github action workflows to update README on DockerHub [\#35](https://github.com/scylladb/cassandra-stress/pull/35) ([CodeLieutenant](https://github.com/CodeLieutenant))

## [v3.17.0](https://github.com/scylladb/cassandra-stress/releases/tag/v3.17.3) (2024-11-19)

[Full Changelog](https://github.com/scylladb/cassandra-stress/compare/v3.16.0...v3.17.0)

**Merged pull requests:**

- Switch to `ReplicaOrdering.RANDOM` for select LBPs [\#32](https://github.com/scylladb/cassandra-stress/pull/32) ([Bouncheck](https://github.com/Bouncheck))

## [v3.16.0](https://github.com/scylladb/cassandra-stress/releases/tag/v3.17.3) (2024-11-18)

[Full Changelog](https://github.com/scylladb/cassandra-stress/compare/v3.15.0...v3.16.0)

**Merged pull requests:**

- Add support for hostname verification [\#31](https://github.com/scylladb/cassandra-stress/pull/31) ([dimakr](https://github.com/dimakr))

## [v3.15.0](https://github.com/scylladb/cassandra-stress/releases/tag/v3.17.3) (2024-10-21)

[Full Changelog](https://github.com/scylladb/cassandra-stress/compare/v3.14.0...v3.15.0)

**Implemented enhancements:**

- Print thread dump on specific signals [\#27](https://github.com/scylladb/cassandra-stress/pull/27) ([Bouncheck](https://github.com/Bouncheck))
- Replace uninterruptible wait [\#26](https://github.com/scylladb/cassandra-stress/pull/26) ([Bouncheck](https://github.com/Bouncheck))
- fix\(release\): use organization secrets for pushing to dockerhub [\#24](https://github.com/scylladb/cassandra-stress/pull/24) ([CodeLieutenant](https://github.com/CodeLieutenant))

**Merged pull requests:**

- Make it use DCAwareRoundRobinPolicy unless rack is provided [\#21](https://github.com/scylladb/cassandra-stress/pull/21) ([dkropachev](https://github.com/dkropachev))

## [v3.14.0](https://github.com/scylladb/cassandra-stress/releases/tag/v3.17.3) (2024-09-03)

[Full Changelog](https://github.com/scylladb/cassandra-stress/compare/v3.13.0...v3.14.0)

**Implemented enhancements:**

- feat\(cassandra.in.sh\): Refactor script for readability and consistency [\#17](https://github.com/scylladb/cassandra-stress/pull/17) ([Daniel-Boll](https://github.com/Daniel-Boll))

**Merged pull requests:**

- feature\(docker\): adding support for dependabot [\#19](https://github.com/scylladb/cassandra-stress/pull/19) ([CodeLieutenant](https://github.com/CodeLieutenant))
- README.md: reformat and add note on running c-s with container [\#18](https://github.com/scylladb/cassandra-stress/pull/18) ([tchaikov](https://github.com/tchaikov))

## [v3.13.0](https://github.com/scylladb/cassandra-stress/releases/tag/v3.17.3) (2024-08-21)

[Full Changelog](https://github.com/scylladb/cassandra-stress/compare/v3.12.2...v3.13.0)

**Merged pull requests:**

- fix\(docker\): Use ubuntu and eclipse-temurin base image [\#14](https://github.com/scylladb/cassandra-stress/pull/14) ([CodeLieutenant](https://github.com/CodeLieutenant))
- improve\(runMulti\): Improve threadCount steps to find max throughput [\#12](https://github.com/scylladb/cassandra-stress/pull/12) ([roydahan](https://github.com/roydahan))

## [v3.12.2](https://github.com/scylladb/cassandra-stress/releases/tag/v3.17.3) (2024-08-11)

[Full Changelog](https://github.com/scylladb/cassandra-stress/compare/v3.12.1...v3.12.2)

**Fixed bugs:**

- fix\(docker\): adding bash to support SCT for Scylla enterprice [\#9](https://github.com/scylladb/cassandra-stress/pull/9) ([CodeLieutenant](https://github.com/CodeLieutenant))

## [v3.12.1](https://github.com/scylladb/cassandra-stress/releases/tag/v3.17.3) (2024-07-31)

[Full Changelog](https://github.com/scylladb/cassandra-stress/compare/v3.12.0...v3.12.1)

## [v3.12.0](https://github.com/scylladb/cassandra-stress/releases/tag/v3.17.3) (2024-07-22)

[Full Changelog](https://github.com/scylladb/cassandra-stress/compare/v3.11.14...v3.12.0)

**Merged pull requests:**

- feat: Recognize IncrementalCompactionStrategy [\#4](https://github.com/scylladb/cassandra-stress/pull/4) ([CodeLieutenant](https://github.com/CodeLieutenant))
- feat: remove legacy JMX progress support [\#3](https://github.com/scylladb/cassandra-stress/pull/3) ([CodeLieutenant](https://github.com/CodeLieutenant))
- cassandra-stress: Make default repl. strategy NetworkTopologyStrategy [\#2](https://github.com/scylladb/cassandra-stress/pull/2) ([CodeLieutenant](https://github.com/CodeLieutenant))
- cassandra-stress: delay before retry [\#1](https://github.com/scylladb/cassandra-stress/pull/1) ([CodeLieutenant](https://github.com/CodeLieutenant))

## [v3.11.14](https://github.com/scylladb/cassandra-stress/releases/tag/v3.17.3) (2024-07-18)

[Full Changelog](https://github.com/scylladb/cassandra-stress/compare/1f91e99223b0d1b7ed8390400d4a06ac08e4aa85...v3.11.14)



\* *This Changelog was automatically generated by [github_changelog_generator](https://github.com/github-changelog-generator/github-changelog-generator)*
