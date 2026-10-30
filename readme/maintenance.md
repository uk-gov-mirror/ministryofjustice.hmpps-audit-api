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

### Temporary Parquet test endpoints

Both endpoints require `ROLE_AUDIT_INTEGRATION_TEST`. They bypass SQS and make
no S3 writes; existing ingestion is unchanged.

1. `POST /internal/integration-test/STAFF/parquet-batches` saves 10 synthetic STAFF
   events directly to Postgres and returns `{"batchId":"<uuid>"}`.
2. `GET /internal/integration-test/STAFF/parquet-batches/{batchId}` returns those
   events as a single downloadable Snappy Parquet file using the existing schema.

Each creation request produces a separate batch. Downloads exclude ordinary audit
events and other batches. An unknown batch returns 404; an incomplete or oversized
batch returns 409. Test records remain in Postgres for repeat downloads.
These endpoints are temporary and should be removed after validating aggregation.
