plugins {
  alias(libs.plugins.versions)
  alias(libs.plugins.spotless)
  alias(libs.plugins.openapiGenerator)
  alias(libs.plugins.nebulaRelease)
  `maven-publish`
}

group = "com.davidjlynn"

repositories { mavenCentral() }

openApiValidate { inputSpec.set("src/main/openapi/openapi.yaml") }

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
