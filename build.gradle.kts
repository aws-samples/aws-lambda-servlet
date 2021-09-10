plugins {
    kotlin("jvm") version "1.5.20"
    id("java")
    id("maven-publish")
    id("java-library")
    id("application")
    id("idea")
}

publishing.publications.create<MavenPublication>("maven").from(components["java"])

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

val gradleDependencyVersion = "7.0"

tasks.wrapper {
    gradleVersion = gradleDependencyVersion
    distributionType = Wrapper.DistributionType.ALL
}

tasks.distZip { enabled = true }
tasks.distTar { enabled = true }

group = "local"
version = "1.0-SNAPSHOT"

val awsLambdaJavaCoreVersion = "1.2.1"
val awsLambdaJavaLog4j2Version = "1.2.0"
val log4jVersion = "2.14.1"
val vavrVersion = "0.10.3"
val junitVersion = "4.13.2"
val awsCdkConstructsForJavaVersion = "0.16.19"

repositories {
    mavenCentral()
    mavenLocal()
    maven(url = "https://repo.gradle.org/gradle/libs-releases-local/")
    maven(url = "https://jitpack.io")
}

dependencies {
    annotationProcessor("com.google.auto.service:auto-service:1.0")
    api("com.google.auto.service:auto-service-annotations:1.0")
    api("javax.servlet:javax.servlet-api:4.0.1")

    // Lambda core and logging
    implementation("com.amazonaws:aws-lambda-java-core:$awsLambdaJavaCoreVersion")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j18-impl:$log4jVersion")
    runtimeOnly("com.amazonaws:aws-lambda-java-log4j2:$awsLambdaJavaLog4j2Version")

    api("org.glassfish.jersey.containers:jersey-container-servlet:2.34")
    implementation("com.github.aws-samples:aws-cdk-constructs-for-java:$awsCdkConstructsForJavaVersion")

    implementation("com.squareup:javapoet:1.13.0")
    implementation("io.vavr:vavr:$vavrVersion")

    testImplementation("junit:junit:$junitVersion")
}
