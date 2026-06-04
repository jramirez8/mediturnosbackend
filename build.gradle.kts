plugins {
	java
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

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<JavaExec>().configureEach {
    // Estas líneas son críticas para solucionar el error 10106 en Windows
    val systemRoot = project.providers.environmentVariable("SystemRoot").getOrElse("C:\\Windows")
    val systemDrive = project.providers.environmentVariable("SystemDrive").getOrElse("C:")
    environment("SystemRoot", systemRoot)
    environment("SystemDrive", systemDrive)

    systemProperty("java.io.tmpdir", layout.buildDirectory.dir("tmp").get().asFile.absolutePath)
    systemProperty("java.net.preferIPv4Stack", "true")
    systemProperty("java.net.preferIPv4Addresses", "true")
}
