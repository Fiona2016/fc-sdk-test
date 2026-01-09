pluginManagement {
    repositories {
        // mavenLocal()  // ✅ 必须在最前面 本地测试
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Maven Central Portal Snapshots repository for FlashCat SDK snapshot versions
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            
            // Search this repository for all FlashCat SDK dependencies
            content {
                includeGroup("cloud.flashcat")
            }
        }
    }
}

rootProject.name = "fc-sdk-test"
include(":app")
