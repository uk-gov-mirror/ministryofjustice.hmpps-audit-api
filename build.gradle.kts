import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.14"
  kotlin("plugin.spring") version "2.4.21"
  kotlin("plugin.jpa") version "2.4.21"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
  all {
    resolutionStrategy {
      //  used by hadoop but 2.0.2 has vulnerability remove when hadoop has been upgraded
      force("org.apache.commons:commons-configuration2:2.15.0")
      //  used by hadoop but 2.11.0 has vulnerability remove when hadoop has been upgraded
      force("io.airlift:aircompressor:2.0.3")
      force("commons-beanutils:commons-beanutils:1.11.0")
      // Mitigate CVE-2026-54399 until upstream BOMs/plugins converge on a fixed baseline.
      force("org.apache.httpcomponents.client5:httpclient5:5.6.1")
    }
  }
}

dependencyCheck {
  suppressionFiles.add("audit-suppressions.xml")
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:3.0.3")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-webflux")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.4.1")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")

  implementation("org.apache.commons:commons-lang3:3.21.0")
  implementation("software.amazon.awssdk:s3:2.57.4")
  implementation("software.amazon.awssdk:athena:2.57.4")
  implementation("org.apache.parquet:parquet-avro:1.18.1")
  implementation("org.apache.avro:avro:1.12.2")
  implementation("org.apache.hadoop:hadoop-client:3.5.0") {
    exclude(group = "com.google.protobuf", module = "protobuf-java")
    exclude(group = "org.apache.hadoop.thirdparty", module = "hadoop-shaded-protobuf_3_25")
    exclude(group = "dnsjava", module = "dnsjava")
    exclude(group = "org.jline", module = "jline")
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
    exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
    exclude(group = "com.google.guava", module = "guava")
    exclude(group = "org.eclipse.jetty", module = "jetty-servlet")
    exclude(group = "org.eclipse.jetty", module = "jetty-webapp")
    exclude(group = "org.eclipse.jetty.websocket", module = "websocket-common")
    exclude(group = "org.eclipse.jetty.websocket", module = "websocket-client")
    exclude(group = "commons-beanutils", module = "commons-beanutils")
  }

  implementation("commons-beanutils:commons-beanutils:1.11.0")
  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("org.glassfish.jaxb:jaxb-runtime:4.0.9")

  runtimeOnly("com.h2database:h2:2.5.252")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.14")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:3.0.3")
  testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  testImplementation("org.mockito:mockito-core:5.24.0")

  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
}

kotlin {
  jvmToolchain(25)
}

tasks {
  withType<KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
  }
}

tasks.test {
  systemProperty("spring.profiles.active", System.getProperty("spring.profiles.active"))
}
