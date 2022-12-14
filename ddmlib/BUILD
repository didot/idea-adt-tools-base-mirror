load("//tools/base/bazel:bazel.bzl", "iml_module")
load("//tools/base/bazel:maven.bzl", "maven_java_library", "maven_pom")
load("//tools/base/bazel:coverage.bzl", "coverage_java_test")

# managed by go/iml_to_build
iml_module(
    name = "studio.android.sdktools.ddmlib",
    srcs = ["src/main/java"],
    iml_files = ["android.sdktools.ddmlib.iml"],
    javacopts = ["-Xep:MissingCasesInEnumSwitch:WARN"],
    test_coverage = True,
    test_data = ["//prebuilts/studio/sdk:platform-tools"],
    test_resources = ["src/test/resources"],
    test_srcs = ["src/test/java"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    exports = [
        "//tools/base/common:studio.android.sdktools.common",
        "//tools/idea/.idea/libraries:kxml2",
    ],
    # do not sort: must match IML order
    deps = [
        "//tools/base/common:studio.android.sdktools.common[module]",
        "//tools/idea/.idea/libraries:kxml2",
        "//tools/idea/.idea/libraries:Mocks[test]",
        "//tools/idea/.idea/libraries:JUnit4[test]",
        "//tools/base/testutils:studio.android.sdktools.testutils[module, test]",
        "//tools/idea/.idea/libraries:mockito[test]",
        "//tools/idea/.idea/libraries:truth[test]",
        "//tools/base/fakeadbserver:studio.android.sdktools.fakeadbserver[module, test]",
        "//tools/idea/.idea/libraries:kotlin-stdlib-jdk8",
    ],
)

maven_java_library(
    name = "tools.ddmlib",
    srcs = glob(["src/main/java/**"]),
    javacopts = ["-Xep:MissingCasesInEnumSwitch:WARN"],
    pom = ":pom",
    resource_strip_prefix = "tools/base/ddmlib",
    visibility = ["//visibility:public"],
    deps = [
        "//tools/base/annotations",
        "//tools/base/common:tools.common",
        "//tools/base/third_party:com.google.guava_guava",
        "//tools/base/third_party:net.sf.kxml_kxml2",
    ],
)

maven_pom(
    name = "pom",
    artifact = "ddmlib",
    group = "com.android.tools.ddms",
    source = "//tools/buildSrc/base:base_version",
)

coverage_java_test(
    name = "tools.tests",
    srcs = glob(["src/test/java/**"]),
    data = ["//prebuilts/studio/sdk:platform-tools"],
    jvm_flags = ["-Dtest.suite.jar=tools.tests.jar"],
    resources = glob(["src/test/resources/**"]),
    test_class = "com.android.testutils.JarTestSuite",
    deps = [
        ":tools.ddmlib",
        "//tools/base/annotations",
        "//tools/base/common:tools.common",
        "//tools/base/fakeadbserver:tools.fakeadbserver",
        "//tools/base/testutils:tools.testutils",
        "//tools/base/third_party:com.google.guava_guava",
        "//tools/base/third_party:com.google.truth_truth",
        "//tools/base/third_party:junit_junit",
        "//tools/base/third_party:org.easymock_easymock",
        "//tools/base/third_party:org.mockito_mockito-core",
    ],
)
