import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import java.nio.file.Files

plugins {
    application
    alias(libs.plugins.nativeBuildtools)
    alias(libs.plugins.osdetector)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":library"))

    implementation(libs.guava)

    implementation(libs.log4j.core)

    implementation(libs.picocli.picocli)

    implementation(libs.progressbar)

    annotationProcessor(libs.picocli.codegen)
}

application {
    // Define the main class for the application.
    mainClass = "com.github.xhea1.party.app.Party"
}

graalvmNative {
    toolchainDetection = true
    binaries {
        named("main") {
            val isLinux = osdetector.os.contains("linux")
            imageName = "party-${osdetector.classifier}-${rootProject.version}"
            buildArgs.add("-march=native")
            if (isLinux) {
                buildArgs.add("--static")
                buildArgs.add("--libc=musl")
                imageName = "party-static-${rootProject.version}"
            } else {
                logger.info("Skipping static/native-image flags on non-Linux (${osdetector.classifier}).")
            }
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(25))
                vendor.set(JvmVendorSpec.GRAAL_VM)
            })
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.GRAAL_VM
        // TODO: re-enable once i figure out how to use it
        //nativeImageCapable = true
    }
}

// copied from https://github.com/oss-review-toolkit/ort/blob/06059ddd268a9e58c2aaab203ed293f47aa94d3b/buildSrc/src/main/kotlin/ort-application-conventions.gradle.kts#L137-L165
tasks.named<BuildNativeImageTask>("nativeCompile") {
    // Gradle's "Copy" task cannot handle symbolic links, see https://github.com/gradle/gradle/issues/3982. That is why
    // links contained in the GraalVM distribution archive get broken during provisioning and are replaced by empty
    // files. Address this by recreating the links in the toolchain directory.
    val toolchainDir = options.get().javaLauncher.get().executablePath.asFile.parentFile.run {
        if (name == "bin") parentFile else this
    }


    val toolchainFiles = toolchainDir.walkTopDown().filter { it.isFile }
    val emptyFiles = toolchainFiles.filter { it.length() == 0L }


    // Find empty toolchain files that are named like other toolchain files and assume these should have been links.
    val links = toolchainFiles.mapNotNull { file ->
        emptyFiles.singleOrNull { it != file && it.name == file.name }?.let {
            file to it
        }
    }


    // Fix up symbolic links.
    links.forEach { (target, link) ->
        logger.quiet("Fixing up '$link' to link to '$target'.")


        if (link.delete()) {
            Files.createSymbolicLink(link.toPath(), target.toPath())
        } else {
            logger.warn("Unable to delete '$link'.")
        }
    }
}

val isLinuxBuild = osdetector.classifier.contains("linux")
tasks.named("assemble").configure {
    if (isLinuxBuild) {
        dependsOn("nativeCompile")
    } else {
        logger.quiet("assemble will not depend on nativeCompile on non-Linux (${osdetector.classifier}).")
    }
}