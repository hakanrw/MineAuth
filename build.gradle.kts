plugins {
    id("java")
}

group = "core.unix.minecraft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT") // PaperMC dependency (compile-time only)

    implementation("net.dv8tion:JDA:5.0.0-beta.20") // Discord JDA (included in runtime jar)
}

tasks.withType<Jar>() {
    manifest {
        attributes["Main-Class"] = "core.unix.minecraft.BotHandler"
    }

    configurations["runtimeClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile)) {
            exclude("META-INF", "META-INF/**")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}