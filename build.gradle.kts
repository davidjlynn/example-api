import com.github.gradle.node.npm.task.NpxTask

plugins {
  alias(libs.plugins.versions)
  alias(libs.plugins.spotless)
  alias(libs.plugins.openapiGenerator)
  alias(libs.plugins.nebulaRelease)
  alias(libs.plugins.nodeGradle)
  `maven-publish`
}

group = "com.davidjlynn"

repositories { mavenCentral() }

val yaml by configurations.creating

val updateVersionInOpenapiSpecTask =
  tasks.register<Copy>("updateVersionInOpenapiSpec") {
    from("src/main/openapi")
    into("build/generated/openapi-files")
    expand("version" to "${project.version}")
  }

openApiValidate { inputSpec.set("build/generated/openapi-files/openapi.yaml") }

tasks.openApiValidate { dependsOn(updateVersionInOpenapiSpecTask) }

tasks.check { dependsOn(tasks.openApiValidate) }

node {
  download = true
  version = libs.versions.node.get()
}

tasks.register<NpxTask>("openApiBundle") {
  group = "openapi tools"
  inputs.files(fileTree("build/generated/openapi-files"))
  outputs.dir("build/generated/openapi-bundled")
  command = "@redocly/cli@${libs.versions.redocly.get()}"
  args =
    listOf<String>(
      "bundle",
      "build/generated/openapi-files/openapi.yaml",
      "-o",
      "build/generated/openapi-bundled/openapi.yaml",
    )
  dependsOn(updateVersionInOpenapiSpecTask)
}

spotless {
  format("misc") {
    target(".gitattributes", ".gitignore")
    trimTrailingWhitespace()
    leadingTabsToSpaces(2)
    endWithNewline()
  }
  yaml {
    target("src/**/*.yaml")
    jackson()
      .yamlFeature("MINIMIZE_QUOTES", true)
      .yamlFeature("WRITE_DOC_START_MARKER", false)
      .yamlFeature("INDENT_ARRAYS_WITH_INDICATOR", true)
  }
  kotlinGradle { ktfmt().googleStyle() }
}

val openapiFile = layout.buildDirectory.file("generated/openapi-bundled/openapi.yaml")
val myOpenapi = artifacts.add("yaml", openapiFile.get().asFile) { builtBy("openApiBundle") }

publishing {
  publications {
    create<MavenPublication>("openapi") {
      artifact(myOpenapi)
      artifactId = "example-api"
      groupId = "com.davidjlynn"
    }
  }
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/davidjlynn/example-api")
      credentials {
        username = System.getenv("GITHUB_ACTOR")
        password = System.getenv("GITHUB_TOKEN")
      }
    }
  }
}
