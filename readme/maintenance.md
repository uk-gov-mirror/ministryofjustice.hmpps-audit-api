[< Back](../README.md)
---

## Maintenance

### Common gradle tasks 

To list project dependencies, run:

```
./gradlew dependencies
```

To check for dependency updates, run:
```
./gradlew dependencyUpdates --warning-mode all
```

To run an OWASP dependency check, run:
```
./gradlew clean dependencyCheckAnalyze --info
```

To upgrade the gradle wrapper version, run:
```
./gradlew wrapper --gradle-version=<VERSION>
```

To automatically update project dependencies, run:
```
./gradlew useLatestVersions
```

#### Ktlint Gradle Tasks

To run Ktlint check:
```
./gradlew ktlintCheck
```

To run Ktlint format:
```
./gradlew ktlintFormat
```

To register pre-commit check to run Ktlint format:
```
./gradlew addKtlintFormatGitPreCommitHook 
```

...or to register pre-commit check to only run Ktlint check:
```
./gradlew addKtlintCheckGitPreCommitHook
```

### Daily audit export (experimental)

Exports one UTC day of staff audit events from Postgres into a single Parquet
file in the existing S3 bucket. It is a manual, synchronous trigger used to test daily
consolidation against real infrastructure; it does not change how events are ingested or queried.

```
POST /audit/STAFF/daily-export/{yyyy-MM-dd}   (requires ROLE_AUDIT)
```

- The file is written to `daily/year=Y/month=M/day=D/audit.parquet`. Re-running a day overwrites it.
- Only staff audit events are supported for now. Only past UTC days are accepted. Days with no events do not produce a file.
- The response includes the event count, S3 key and file size. Each run is also logged and
  sent to Application Insights as `hmpps-audit-daily-export` with the duration.
