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

tasks.register<Copy>("updateVersionInOpenapiSpec") {
  from ("src/main/openapi")
  into ("build/generated/openapi-files")

  val fullVersion = project.version

//  filter {
//       line -> line.replaceAll("FULL_API_VERSION", "$fullVersion")
//  }
}

openApiValidate {
  inputSpec.set("build/generated/openapi-files/openapi.yaml")
//  dependsOn(tasks.updateVersionInOpenapiSpec)
}
//tasks.check.dependsOn(tasks.openApiValidate)

node {
  download = true
  version = "22.11.0"
}

tasks.register<NpxTask>("openApiBundle"){
  group = "openapi tools"
  inputs.files(fileTree("build/generated/openapi-files"))
  outputs.dir("build/generated/openapi-bundled")
  command = "@redocly/cli@1.25.11"
//  args = [
//    "bundle",
//  "build/generated/openapi-files/openapi.yaml",
//  "-o",
//  "build/generated/openapi-bundled/openapi.yaml"
//  ]
//  dependsOn(tasks.updateVersionInOpenapiSpec)
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

val openapiFile = layout.buildDirectory.file("build/generated/openapi-files/openapi.yaml")
val myOpenapi = artifacts.add("yaml", openapiFile.get().asFile) {
  builtBy("openApiBundle")
}

publishing {
  publications {
    create<MavenPublication>("openapi") {
      artifact ( myOpenapi)
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