load("//tools/base/bazel:coverage.bzl", "coverage_java_test")
load("//tools/base/bazel:coverage_report.bzl", "coverage_report")
load("//tools/base/bazel:bazel.bzl", "iml_module")

java_library(
    name = "utils",
    srcs = glob(["src/com/android/tools/utils/*.java"]),
    visibility = [
        "//tools/base/bazel/sdk:__pkg__",
    ],
)

java_binary(
    name = "jasmin-compiler",
    srcs = ["src/com/android/tools/jasmin/JasminCompiler.java"],
    main_class = "com.android.tools.jasmin.JasminCompiler",
    visibility = ["//visibility:public"],
    deps = [
        ":utils",
        "//prebuilts/tools/common/jasmin",
    ],
)

config_setting(
    name = "agent_coverage",
    values = {
        "define": "agent_coverage=true",
    },
    visibility = ["//visibility:public"],
)

java_library(
    name = "groovy-compiler",
    srcs = glob(["src/com/android/tools/groovy/*.java"]),
    # Note that version 2.3 of groovy, embeds timestamps into the generated classes
    # making it unsuitable for bazel or any other reasonable tool.
    # TODO: Update and use the prebuilts dependency
    deps = [
        ":utils",
        "//tools/idea/.idea/libraries:Ant",
        "//tools/idea/.idea/libraries:Groovy",
    ],
)

java_binary(
    name = "groovy_stub_gen",
    main_class = "com.android.tools.groovy.GroovyStubGenerator",
    visibility = ["//visibility:public"],
    runtime_deps = [":groovy-compiler"],
)

java_binary(
    name = "groovyc",
    main_class = "com.android.tools.groovy.GroovyCompiler",
    visibility = ["//visibility:public"],
    runtime_deps = [":groovy-compiler"],
)

java_binary(
    name = "unzipper",
    main_class = "com.android.tools.utils.Unzipper",
    visibility = ["//visibility:public"],
    runtime_deps = [":utils"],
)

java_binary(
    name = "kotlinc",
    srcs = ["src/com/android/tools/kotlin/KotlinCompiler.java"],
    data = ["//prebuilts/tools/common/kotlin-plugin-ij:kotlin_plugin"],
    main_class = "com.android.tools.kotlin.KotlinCompiler",
    visibility = ["//visibility:public"],
    runtime_deps = [
        "//prebuilts/tools/common/kotlin-plugin-ij:Kotlin/kotlinc/lib/kotlin-stdlib",
        "//prebuilts/tools/common/m2/repository/org/jetbrains/kotlin/kotlin-reflect/1.3.11:jar",
    ],
    deps = [
        ":utils",
        "//prebuilts/tools/common/kotlin-plugin-ij:Kotlin/kotlinc/lib/kotlin-compiler",
    ],
)

java_binary(
    name = "formc",
    srcs = ["src/com/android/tools/form/FormCompiler.java"],
    main_class = "com.android.tools.form.FormCompiler",
    visibility = ["//visibility:public"],
    runtime_deps = ["//prebuilts/tools/common/kotlin-plugin-ij:Kotlin/kotlinc/lib/kotlin-stdlib"],
    deps = [
        ":utils",
        "//prebuilts/tools/common/bazel/formc-deps",
    ],
)

java_library(
    name = "bazel_generator",
    srcs = glob(["src/com/android/tools/bazel/**"]),
    visibility = ["//visibility:public"],
    runtime_deps = [
        "//tools/idea/.idea/libraries:JDOM",
    ],
    deps = [
        "//tools/idea/.idea/libraries:Guava",
        "//tools/idea/.idea/libraries:Trove4j",
        "//tools/idea/.idea/libraries:jetbrains-annotations-java5",
        "//tools/idea/build/lib:jps",
    ],
)

java_import(
    name = "langtools",
    jars = ["//prebuilts/studio/jdk:langtools"],
    visibility = ["//visibility:public"],
)

java_binary(
    name = "iml_to_build",
    srcs = ["src/com/android/tools/binaries/ImlToBazel.java"],
    main_class = "com.android.tools.binaries.ImlToBazel",
    visibility = ["//visibility:public"],
    deps = [
        ":bazel_generator",
        ":utils",
    ],
)

java_binary(
    name = "bin2c",
    srcs = ["src/com/android/tools/binaries/Bin2C.java"],
    main_class = "com.android.tools.binaries.Bin2C",
    visibility = ["//visibility:public"],
    deps = ["//tools/base/third_party:com.google.guava_guava"],
)

java_binary(
    name = "singlejar",
    main_class = "com.android.tools.utils.SingleJar",
    visibility = ["//visibility:public"],
    runtime_deps = [":utils"],
)

java_binary(
    name = "properties_merger",
    main_class = "com.android.tools.utils.PropertiesMerger",
    visibility = ["//visibility:public"],
    runtime_deps = [":utils"],
)

java_library(
    name = "maven",
    srcs = glob(["src/com/android/tools/maven/**/*.java"]),
    deps = [
        "//tools/base/third_party:com.google.code.findbugs_jsr305",
        "//tools/base/third_party:com.google.guava_guava",
        "//tools/base/third_party:org.apache.maven_maven-aether-provider",
        "//tools/base/third_party:org.eclipse.aether_aether-connector-basic",
        "//tools/base/third_party:org.eclipse.aether_aether-transport-http",
    ],
)

java_binary(
    name = "matryoshka",
    srcs = ["src/com/android/tools/binaries/Matryoshka.java"],
    main_class = "com.android.tools.binaries.Matryoshka",
    visibility = ["//visibility:public"],
)

java_binary(
    name = "pom_generator",
    srcs = ["src/com/android/tools/binaries/PomGenerator.java"],
    jvm_flags = ["-Xmx1g"],
    main_class = "com.android.tools.binaries.PomGenerator",
    visibility = ["//visibility:public"],
    runtime_deps = [
        "//prebuilts/tools/common/m2/repository/org/slf4j/slf4j-simple/1.7.2:jar",
    ],
    deps = [
        ":maven",
        "//tools/base/third_party:com.google.guava_guava",
        "//tools/base/third_party:org.apache.maven_maven-aether-provider",
    ],
)

java_binary(
    name = "repo_builder",
    srcs = ["src/com/android/tools/binaries/RepoBuilder.java"],
    main_class = "com.android.tools.binaries.RepoBuilder",
    visibility = ["//visibility:public"],
    runtime_deps = [
        "//prebuilts/tools/common/m2/repository/org/slf4j/slf4j-simple/1.7.2:jar",
    ],
    deps = [
        ":maven",
        ":utils",
        "//tools/base/third_party:com.google.guava_guava",
        "//tools/base/third_party:org.apache.maven_maven-aether-provider",
    ],
)

# See README.md
java_binary(
    name = "java_import_generator",
    srcs = ["src/com/android/tools/binaries/JavaImportGenerator.java"],
    main_class = "com.android.tools.binaries.JavaImportGenerator",
    visibility = ["//visibility:public"],
    runtime_deps = [
        "//prebuilts/tools/common/m2/repository/org/slf4j/slf4j-simple/1.7.2:jar",
    ],
    deps = [
        ":maven",
        ":utils",
        "//tools/base/third_party:com.google.code.findbugs_jsr305",
        "//tools/base/third_party:com.google.guava_guava",
        "//tools/base/third_party:org.apache.maven_maven-aether-provider",
    ],
)

# See README.md
java_binary(
    name = "third_party_build_generator",
    srcs = [
        "src/com/android/tools/binaries/JavaImportGenerator.java",
        "src/com/android/tools/binaries/ThirdPartyBuildGenerator.java",
    ],
    main_class = "com.android.tools.binaries.ThirdPartyBuildGenerator",
    visibility = ["//visibility:public"],
    runtime_deps = [
        "//prebuilts/tools/common/m2/repository/org/slf4j/slf4j-simple/1.7.2:jar",
    ],
    deps = [
        ":maven",
        ":utils",
        "//tools/base/third_party:com.google.code.findbugs_jsr305",
        "//tools/base/third_party:com.google.guava_guava",
        "//tools/base/third_party:org.apache.maven_maven-aether-provider",
    ],
)

# See README.md
java_binary(
    name = "add_dependency",
    srcs = [
        "src/com/android/tools/binaries/AddDependency.java",
        "src/com/android/tools/binaries/JavaImportGenerator.java",
    ],
    main_class = "com.android.tools.binaries.AddDependency",
    visibility = ["//visibility:public"],
    runtime_deps = [
        "//prebuilts/tools/common/m2/repository/org/slf4j/slf4j-simple/1.7.2:jar",
    ],
    deps = [
        ":maven",
        ":utils",
        "//tools/base/third_party:com.google.code.findbugs_jsr305",
        "//tools/base/third_party:com.google.guava_guava",
        "//tools/base/third_party:org.apache.maven_maven-aether-provider",
    ],
)

java_binary(
    name = "jarjar",
    main_class = "com.tonicsystems.jarjar.Main",
    visibility = ["//visibility:public"],
    runtime_deps = [
        "//prebuilts/tools/common/m2/repository/net/sf/jopt-simple/jopt-simple/4.9:jar",
        "//prebuilts/tools/common/m2/repository/org/anarres/jarjar/jarjar-command/1.0.0:jar",
        "//prebuilts/tools/common/m2/repository/org/anarres/jarjar/jarjar-core/1.0.0:jar",
        "//prebuilts/tools/common/m2/repository/org/ow2/asm/asm-commons/5.0.4:jar",
        "//prebuilts/tools/common/m2/repository/org/ow2/asm/asm/5.0.4:jar",
        "//prebuilts/tools/common/m2/repository/org/slf4j/slf4j-api/1.7.2:jar",
        "//prebuilts/tools/common/m2/repository/org/slf4j/slf4j-simple/1.7.2:jar",
    ],
)

java_library(
    name = "coverage_report_generator",
    srcs = [
        "src/com/android/tools/coverage/CoverageReportGenerator.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        ":utils",
        "//prebuilts/tools/common/m2/repository/org/jacoco/org.jacoco.core/0.8.2:jar",
        "//prebuilts/tools/common/m2/repository/org/jacoco/org.jacoco.report/0.8.2:jar",
        "//prebuilts/tools/common/m2/repository/org/ow2/asm/asm-commons/6.2.1:jar",
        "//prebuilts/tools/common/m2/repository/org/ow2/asm/asm-tree/6.2.1:jar",
        "//prebuilts/tools/common/m2/repository/org/ow2/asm/asm/6.2.1:jar",
        "//tools/base/third_party:com.google.guava_guava",
    ],
)

java_binary(
    name = "gradlew",
    main_class = "com.android.tools.gradle.GradleW",
    visibility = ["//visibility:public"],
    runtime_deps = [":gradle"],
)

java_binary(
    name = "apply_diff",
    srcs = ["src/com/android/tools/binaries/ApplyDiff.java"],
    main_class = "com.android.tools.binaries.ApplyDiff",
    visibility = ["//visibility:public"],
    deps = [
        "//tools/base/testutils:tools.testutils",
    ],
)

java_library(
    name = "gradle",
    srcs = glob(["src/com/android/tools/gradle/*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        ":utils",
        "//tools/base/annotations",
        "//tools/base/build-system:gradle-tooling-api",
        "//tools/base/common:tools.common",
        "//tools/base/testutils:tools.testutils",
        "//tools/base/third_party:com.google.guava_guava",
    ],
)

java_library(
    name = "gradle_benchmarks",
    testonly = 1,
    srcs = glob(["testSrc/com/android/tools/gradle/**/*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "//tools/analytics-library/protos/src/main/proto",
        "//tools/analytics-library/shared:tools.analytics-shared",
        "//tools/base/annotations",
        "//tools/base/bazel:gradle",
        "//tools/base/common:tools.common",
        "//tools/base/perf-logger:studio.perf-logger_testlib",
        "//tools/base/third_party:com.google.guava_guava",
    ],
)

java_binary(
    name = "antlr",
    srcs = ["src/com/android/tools/binaries/AntlrCompiler.java"],
    main_class = "com.android.tools.binaries.AntlrCompiler",
    visibility = ["//visibility:public"],
    deps = [
        ":utils",
        "//tools/base/third_party:com.google.guava_guava",
        "//tools/base/third_party:org.antlr_antlr",
    ],
)

config_setting(
    name = "host_darwin",
    values = {"host_cpu": "darwin"},
    visibility = ["//visibility:public"],
)

config_setting(
    name = "host_windows",
    values = {"host_cpu": "x64_windows"},
    visibility = ["//visibility:public"],
)

config_setting(
    name = "remote_windows",
    values = {"define": "REMOTE_WINDOWS=true"},
    visibility = ["//visibility:public"],
)

config_setting(
    name = "darwin",
    values = {"cpu": "darwin"},
    visibility = ["//visibility:public"],
)

config_setting(
    name = "windows",
    values = {"cpu": "x64_windows"},
    visibility = ["//visibility:public"],
)

config_setting(
    name = "android_cpu_x86",
    values = {"cpu": "x86"},
    visibility = ["//visibility:public"],
)

config_setting(
    name = "android_cpu_x86_64",
    values = {"cpu": "x86_64"},
    visibility = ["//visibility:public"],
)

config_setting(
    name = "android_cpu_arm",
    values = {"cpu": "armeabi-v7a"},
    visibility = ["//visibility:public"],
)

config_setting(
    name = "android_cpu_arm_64",
    values = {"cpu": "arm64-v8a"},
    visibility = ["//visibility:public"],
)

config_setting(
    name = "android_cpu_armeabi",
    values = {"cpu": "armeabi"},
    visibility = ["//visibility:public"],
)

config_setting(
    name = "coverage",
    values = {"define": "coverage=true"},
    visibility = ["//visibility:public"],
)

coverage_java_test(
    name = "iml_to_build_tests",
    srcs = ["testSrc/com/android/tools/binaries/ImlToBazelTest.java"],
    data = ["//tools/base/bazel/test:iml_to_bazel"],
    tags = [
        "no_test_windows",
    ],
    test_class = "com.android.tools.binaries.ImlToBazelTest",
    deps = [
        ":bazel_generator",
        ":iml_to_build",
        ":utils",
        "//tools/base/third_party:junit_junit",
    ],
)

coverage_java_test(
    name = "bin2c_tests",
    srcs = ["testSrc/com/android/tools/binaries/Bin2CTest.java"],
    test_class = "com.android.tools.binaries.Bin2CTest",
    deps = [
        ":bin2c",
        "//tools/base/third_party:com.google.guava_guava",
        "//tools/base/third_party:junit_junit",
    ],
)

coverage_java_test(
    name = "third_party_up_to_date",
    srcs = ["testSrc/com/android/tools/binaries/ThirdPartyBuildConsistencyTest.java"],
    data = [
        "//tools/base/third_party:BUILD",
        "//tools/buildSrc/base:dependencies.properties",
    ],
    tags = [
        "no_test_windows",
    ],
    test_class = "com.android.tools.binaries.ThirdPartyBuildConsistencyTest",
    deps = [
        ":maven",
        ":third_party_build_generator",
        ":utils",
        "//tools/base/testutils:tools.testutils",
        "//tools/base/third_party:com.google.guava_guava",
        "//tools/base/third_party:junit_junit",
        "//tools/base/third_party:org.apache.maven_maven-aether-provider",
    ],
)

java_binary(
    name = "BazelLintWrapper",
    srcs = ["src/com/android/tools/binaries/BazelLintWrapper.java"],
    main_class = "com.android.tools.binaries.BazelLintWrapper",
    visibility = ["//visibility:public"],
    deps = [
        "//tools/base/lint/cli",
        "//tools/base/third_party:com.google.guava_guava",
    ],
)
