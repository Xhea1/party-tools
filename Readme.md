# Party (kemono/coomer) Tools

Tools for working with *.party (kemono/coomer).

## Usage

You can use this library in your project via [jitpack.io](https://jitpack.io/).

### Gradle

For Gradle use the following configuration:

````groovy
repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven {
                url = uri('https://jitpack.io')
            }
        }
        filter {
            // this repository *only* contains artifacts with group "com.github.xhea1"
            includeGroup 'com.github.xhea1'
        }
    }
}

dependencies {
    // party tools
    implementation 'com.github.xhea1:party-tools:v0.1'
}
````