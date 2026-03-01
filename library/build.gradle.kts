plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<Javadoc> {
    isFailOnError = false
}

dependencies {
    implementation(libs.guava)

    implementation(libs.log4j.core)

    implementation(libs.okhttp)
    implementation(libs.bundles.jackson)

    api(libs.jspecify)

    // Testing dependencies
    testImplementation(libs.junit)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "party-tools"

            from(components["java"])

            pom {
                name.set("Party Tools")
                description.set("Java Tools for working with *.party services (kemono/coomer)")
                url.set("https://github.com/Xhea1/party-tools")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/xhea1/party-tools")

            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
