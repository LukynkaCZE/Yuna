import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    application
    kotlin("jvm") version "2.2.0-RC"
    id("io.ktor.plugin") version "2.2.3"
}

repositories {
    mavenCentral()
    maven {
        name = "devOSReleases"
        url = uri("https://mvn.devos.one/releases")
    }
}

dependencies {
    implementation("cz.lukynka:pretty-log:1.4")
    implementation("dev.kord:kord-core:0.14.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    testImplementation(kotlin("test"))
}


tasks.test {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}

application {
    mainClass.set("MainKt")
}

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.Mdd.H")
val current: String = LocalDateTime.now().format(formatter)

group = "cz.lukynka"
version = current

val gitBranch = "git rev-parse --abbrev-ref HEAD".runCommand()
val gitCommit = "git rev-parse --short=8 HEAD".runCommand()

tasks.register("generateVersionFile") {
    val outputDir = file("$buildDir/generated/resources/")
    val outputFile = file("$outputDir/yuna.ver")
    outputs.file(outputFile)
    doLast {
        outputDir.mkdirs()
        outputFile.writeText("$current|$gitBranch|$gitCommit")
    }
}

application {
    mainClass.set("MainKt")
}


tasks.named("processResources") {
    dependsOn("generateVersionFile")
}

sourceSets["main"].resources.srcDir("$buildDir/generated/resources/")


fun String.runCommand(
    workingDir: File = File("."),
    timeoutAmount: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS
): String = ProcessBuilder(split("\\s(?=(?:[^'\"`]*(['\"`])[^'\"`]*\\1)*[^'\"`]*$)".toRegex()))
    .directory(workingDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()
    .apply { waitFor(timeoutAmount, timeoutUnit) }
    .run {
        val error = errorStream.bufferedReader().readText().trim()
        if (error.isNotEmpty()) {
            throw Exception(error)
        }
        inputStream.bufferedReader().readText().trim()
    }
