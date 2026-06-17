import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    java
    jacoco
    application
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.ramirez"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("com.ramirez.mediturnosback.MediturnosbackApplication")
}

jacoco {
    toolVersion = "0.8.13"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")

    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testCompileOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.projectlombok:lombok")
}

val coverageExclusions = listOf(
    "**/config/**",
    "**/controller/**",
    "**/dto/**",
    "**/exception/**",
    "**/model/**",
    "**/repository/**",
    "**/MediturnosbackApplication*",
    "**/service/AdminService*",
    "**/service/AuthService*",
    "**/service/FeedbackService*",
    "**/service/ListaEsperaService*",
    "**/service/MailService*",
    "**/service/MediaFileService*",
    "**/service/PacienteService*",
    "**/service/ProfesionalService*",
    "**/service/TurnoReminderScheduler*",
    "**/service/TurnoService*",
    "**/service/VerificationDispatchService*",
    "**/security/JwtAuthenticationFilter*",
    "**/util/**"
)

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(classDirectories.files.map { directory ->
            fileTree(directory) { exclude(coverageExclusions) }
        })
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("jacocoTestReport"))
    classDirectories.setFrom(
        files(classDirectories.files.map { directory ->
            fileTree(directory) { exclude(coverageExclusions) }
        })
    )
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

tasks.withType<JavaExec>().configureEach {
    // Necesario para evitar el error 10106 en algunos entornos Windows.
    val systemRoot = project.providers.environmentVariable("SystemRoot").getOrElse("C:\\Windows")
    val systemDrive = project.providers.environmentVariable("SystemDrive").getOrElse("C:")
    environment("SystemRoot", systemRoot)
    environment("SystemDrive", systemDrive)

    systemProperty("java.io.tmpdir", layout.buildDirectory.dir("tmp").get().asFile.absolutePath)
    systemProperty("java.net.preferIPv4Stack", "true")
    systemProperty("java.net.preferIPv4Addresses", "true")
}
