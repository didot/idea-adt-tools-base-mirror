load("//tools/base/bazel:bazel.bzl", "iml_module")

iml_module(
    name = "idea.platform-impl_and_others",
    # do not sort: must match IML order
    srcs = [
        "idea/RegExpSupport/src",
        "idea/RegExpSupport/gen",
        "idea/xml/xml-analysis-impl/src",
        "idea/xml/xml-psi-impl/src",
        "idea/xml/xml-psi-impl/gen",
        "idea/platform/usageView/src",
        "idea/platform/lvcs-impl/src",
        "idea/platform/vcs-impl/src",
        "idea/xml/impl/src",
        "idea/spellchecker/src",
        "idea/spellchecker/gen",
        "idea/xml/relaxng/src",
        "idea/xml/dom-openapi/src",
        "idea/json/src",
        "idea/json/gen",
        "idea/images/src",
        "idea/platform/xdebugger-impl/src",
        "idea/xml/dom-impl/src",
        "idea/platform/lang-impl/src",
        "idea/platform/lang-impl/gen",
        "idea/platform/diff-impl/src",
        "idea/platform/configuration-store-impl/src",
        "idea/platform/platform-impl/src",
        "idea/platform/built-in-server/src",
        "idea/platform/testFramework/src",
        "idea/xml/xml-structure-view-impl/src",
    ],
    javacopts = ["-extra_checks:off"],
    # do not sort: must match IML order
    resources = [
        "idea/xml/xml-analysis-impl/resources",
        "idea/xml/xml-psi-impl/resources",
        "idea/xml/impl/resources",
        "idea/spellchecker/resources",
        "idea/platform/platform-resources/src",
        "idea/json/resources",
        "idea/platform/lang-impl/resources",
    ],
    tags = ["managed"],
    test_srcs = [
        "idea/RegExpSupport/test",
        "idea/platform/vcs-impl/testSrc",
        "idea/spellchecker/testSrc",
        "idea/xml/relaxng/test",
        "idea/platform/xdebugger-impl/testSrc",
        "idea/platform/testFramework/testSrc",
    ],
    test_tags = ["manual"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    exports = [
        "//tools/idea/xml/xml-analysis-api",
        "//tools/idea/xml/xml-psi-api",
        "//tools/idea/platform/vcs-api",
        "//tools/idea/xml/openapi:xml-openapi",
        "//tools/idea/.idea/libraries:CGLIB",
        "//tools/idea/platform/lang-api",
        "//tools/idea/.idea/libraries:cli-parser",
        "//tools/idea/platform/indexing-impl",
        "//tools/idea/platform/projectModel-impl",
        "//tools/idea/platform/analysis-impl",
        "//tools/idea/platform/structure-view-impl",
        "//tools/idea/platform/diff-api",
        "//tools/idea/platform/platform-api",
        "//tools/idea/.idea/libraries:commons-codec",
        "//tools/idea/platform/lvcs-api",
        "//tools/idea/platform/core-impl",
        "//tools/idea/.idea/libraries:miglayout-swing",
        "//tools/idea/.idea/libraries:Netty",
        "//tools/idea/platform/editor-ui-ex",
        "//tools/idea/.idea/libraries:KotlinJavaRuntime",
        "//tools/idea/platform/built-in-server-api",
        "//tools/idea/.idea/libraries:JUnit4",
        "//tools/idea/.idea/libraries:Log4J",
        "//tools/idea/.idea/libraries:Mocks",
        "//tools/idea/java/java-runtime",
        "//tools/idea/.idea/libraries:Groovy",
    ],
    # do not sort: must match IML order
    deps = [
        "//tools/idea/platform/lang-api[module]",
        "//tools/idea/.idea/libraries:JUnit4",
        "//tools/idea/.idea/libraries:Jaxen",
        "//tools/idea/xml/xml-psi-api[module]",
        "//tools/idea/platform/analysis-impl[module]",
        "//tools/idea/platform/core-impl[module]",
        "//tools/idea/xml/xml-analysis-api[module]",
        "//tools/idea/platform/projectModel-api[module]",
        "//tools/idea/platform/core-api[module]",
        "//tools/idea/platform/projectModel-impl[module]",
        "//tools/idea/platform/indexing-impl[module]",
        "//tools/idea/.idea/libraries:Xerces",
        "//tools/idea/.idea/libraries:XmlBeans",
        "//tools/idea/.idea/libraries:picocontainer",
        "//tools/idea/platform/vcs-api[module]",
        "//tools/idea/platform/lvcs-api[module]",
        "//tools/idea/.idea/libraries:jcip",
        "//tools/idea/platform/diff-api[module]",
        "//tools/idea/platform/platform-api[module]",
        "//tools/idea/.idea/libraries:commons-codec",
        "//tools/idea/jps/model-serialization:jps-model-serialization[module]",
        "//tools/idea/.idea/libraries:Guava",
        "//tools/idea/.idea/libraries:gson",
        "//tools/idea/xml/openapi:xml-openapi[module]",
        "//tools/idea/platform/icons[module]",
        "//tools/idea/.idea/libraries:swingx",
        "//tools/idea/.idea/libraries:Netty",
        "//tools/idea/platform/xdebugger-api[module]",
        "//tools/idea/platform/built-in-server-api[module]",
        "//tools/idea/xml/relaxng/lib:rngom-20051226-patched",
        "//tools/idea/xml/relaxng/lib:isorelax",
        "//tools/idea/xml/relaxng/lib:trang-core",
        "//tools/idea/xml/relaxng/lib:jing",
        "//tools/idea/platform/extensions[module]",
        "//tools/idea/platform/util[module]",
        "//tools/idea/.idea/libraries:commons-imaging",
        "//tools/idea/.idea/libraries:asm",
        "//tools/idea/.idea/libraries:CGLIB",
        "//tools/idea/platform/boot[module]",
        "//tools/idea/.idea/libraries:OroMatcher",
        "//tools/idea/.idea/libraries:Velocity",
        "//tools/idea/.idea/libraries:xpp3-1.1.4-min",
        "//tools/idea/.idea/libraries:cli-parser",
        "//tools/idea/platform/indexing-api[module]",
        "//tools/idea/.idea/libraries:Snappy-Java",
        "//tools/idea/jps/model-impl:jps-model-impl[module]",
        "//tools/idea/platform/structure-view-impl[module]",
        "//tools/idea/.idea/libraries:commons-logging",
        "//tools/idea/platform/vcs-api/vcs-api-core[module]",
        "//tools/idea/.idea/libraries:KotlinJavaRuntime",
        "//tools/idea/platform/bootstrap[module]",
        "//tools/idea/.idea/libraries:Mac",
        "//tools/idea/.idea/libraries:Log4J",
        "//tools/idea/.idea/libraries:JavaHelp",
        "//tools/idea/.idea/libraries:jna",
        "//tools/idea/.idea/libraries:winp",
        "//tools/idea/.idea/libraries:miglayout-swing",
        "//tools/idea/.idea/libraries:jayatana",
        "//tools/idea/platform/editor-ui-ex[module]",
        "//tools/idea/.idea/libraries:http-client",
        "//tools/idea/.idea/libraries:imgscalr",
        "//tools/idea/.idea/libraries:Slf4j",
        "//tools/idea/.idea/libraries:com.twelvemonkeys.imageio_imageio-tiff_3.2.1",
        "//tools/analytics-library/tracker:analytics-tracker[module]",
        "//tools/analytics-library/shared:analytics-shared[module]",
        "//tools/analytics-library/publisher:analytics-publisher[module]",
        "//tools/base/common:studio.common[module]",
        "//tools/analytics-library/protos:analytics-protos[module]",
        "//tools/idea/.idea/libraries:pty4j",
        "//tools/idea/.idea/libraries:XmlRPC",
        "//tools/idea/platform/testFramework/bootstrap:tests_bootstrap[module]",
        "//tools/idea/resources-en[module]",
        "//tools/idea/.idea/libraries:Mocks[test]",
        "//tools/idea/java/java-runtime[module]",
        "//tools/idea/.idea/libraries:Groovy",
        "//tools/idea/.idea/libraries:assertJ[test]",
        "//tools/idea/xml/xml-structure-view-api[module]",
    ],
)

iml_module(
    name = "idea.lang-tests",
    tags = ["managed"],
    test_srcs = ["idea/platform/lang-impl/testSources"],
    test_tags = ["manual"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    exports = [
        "//tools/idea/platform/lang-api",
        "//tools:idea.platform-impl_and_others",
        "//tools/idea/.idea/libraries:cli-parser",
        "//tools/idea/platform/indexing-impl",
        "//tools/idea/platform/projectModel-impl",
        "//tools/idea/platform/analysis-impl",
        "//tools/idea/platform/structure-view-impl",
    ],
    # do not sort: must match IML order
    deps = [
        "//tools/idea/platform/boot[module]",
        "//tools/idea/platform/lang-api[module]",
        "//tools/idea/platform/vcs-api[module]",
        "//tools/idea/.idea/libraries:OroMatcher",
        "//tools/idea/.idea/libraries:JUnit4[test]",
        "//tools/idea/.idea/libraries:Velocity",
        "//tools:idea.platform-impl_and_others[module]",
        "//tools/idea/.idea/libraries:xpp3-1.1.4-min",
        "//tools/idea/.idea/libraries:cli-parser",
        "//tools/idea/platform/indexing-api[module]",
        "//tools/idea/platform/indexing-impl[module]",
        "//tools/idea/.idea/libraries:Snappy-Java",
        "//tools/idea/platform/projectModel-impl[module]",
        "//tools/idea/.idea/libraries:Groovy[test]",
        "//tools/idea/.idea/libraries:swingx",
        "//tools/idea/.idea/libraries:Guava",
        "//tools/idea/.idea/libraries:gson",
        "//tools/idea/jps/model-impl:jps-model-impl[module]",
        "//tools/idea/platform/analysis-impl[module]",
        "//tools/idea/platform/structure-view-impl[module]",
        "//tools/idea/.idea/libraries:commons-logging",
        "//tools/idea/.idea/libraries:Mocks[test]",
    ],
)

iml_module(
    name = "idea.diff-tests",
    tags = ["managed"],
    test_srcs = ["idea/platform/diff-impl/tests"],
    test_tags = ["manual"],
    visibility = ["//visibility:public"],
    exports = ["//tools/idea/platform/diff-api"],
    # do not sort: must match IML order
    deps = [
        "//tools/idea/platform/core-api[module]",
        "//tools/idea/platform/diff-api[module]",
        "//tools:idea.platform-impl_and_others[module]",
        "//tools/idea/platform/platform-api[module]",
        "//tools/idea/platform/vcs-api/vcs-api-core[module]",
        "//tools/idea/.idea/libraries:mockito[test]",
        "//tools/idea/.idea/libraries:KotlinTest[test]",
    ],
)

iml_module(
    name = "idea.configuration-store-tests",
    tags = ["managed"],
    test_srcs = ["idea/platform/configuration-store-impl/testSrc"],
    test_tags = ["manual"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//tools/idea/.idea/libraries:KotlinJavaRuntime",
        "//tools/idea/platform/util[module]",
        "//tools/idea/platform/core-api[module]",
        "//tools/idea/platform/platform-api[module]",
        "//tools/idea/platform/projectModel-impl[module]",
        "//tools/idea/.idea/libraries:assertJ[test]",
        "//tools:idea.platform-impl_and_others[module]",
        "//tools/idea/plugins/eclipse[module, test]",
        "//tools/idea/platform/util:util-tests[module, test]",
    ],
)

iml_module(
    name = "idea.built-in-server-tests",
    tags = ["managed"],
    test_srcs = ["idea/platform/built-in-server/testSrc"],
    test_tags = ["manual"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//tools/idea/platform/projectModel-api[module]",
        "//tools/idea/platform/projectModel-impl[module]",
        "//tools:idea.platform-impl_and_others[module]",
        "//tools/idea/.idea/libraries:Netty",
        "//tools/idea/xml/openapi:xml-openapi[module]",
        "//tools/idea/platform/xdebugger-api[module]",
        "//tools/idea/.idea/libraries:Guava",
        "//tools/idea/.idea/libraries:gson",
        "//tools/idea/.idea/libraries:XmlRPC",
        "//tools/idea/.idea/libraries:commons-imaging",
        "//tools/idea/platform/built-in-server-api[module]",
        "//tools/idea/platform/vcs-api[module]",
        "//tools/idea/.idea/libraries:KotlinJavaRuntime[test]",
        "//tools/idea/.idea/libraries:assertJ[test]",
    ],
)

iml_module(
    name = "idea.java-analysis-impl",
    # do not sort: must match IML order
    srcs = [
        "idea/java/java-analysis-impl/src",
        "idea/plugins/InspectionGadgets/InspectionGadgetsAnalysis/src",
    ],
    tags = ["managed"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    exports = [
        "//tools/idea/platform/analysis-impl",
        "//tools/idea/java/java-indexing-impl",
        "//tools/idea/java/java-psi-impl",
        "//tools/idea/platform/projectModel-impl",
        "//tools/idea/java/java-analysis-api",
        "//tools/idea/.idea/libraries:asm5",
    ],
    # do not sort: must match IML order
    deps = [
        "//tools/idea/platform/analysis-impl[module]",
        "//tools/idea/java/java-indexing-impl[module]",
        "//tools/idea/java/java-psi-impl[module]",
        "//tools/idea/platform/projectModel-impl[module]",
        "//tools/idea/java/java-analysis-api[module]",
        "//tools/idea/resources-en[module]",
        "//tools:idea.platform-impl_and_others[module]",
        "//tools/idea/.idea/libraries:asm5",
    ],
)

iml_module(
    name = "idea.java-impl",
    # do not sort: must match IML order
    srcs = [
        "idea/java/java-impl/src",
        "idea/java/java-impl/gen",
        "idea/plugins/InspectionGadgets/src",
        "idea/plugins/IntentionPowerPak/src",
        "idea/plugins/generate-tostring/src",
    ],
    javacopts = ["-extra_checks:off"],
    resources = ["idea/plugins/generate-tostring/resources"],
    tags = ["managed"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    exports = [
        "//tools:idea.platform-impl_and_others",
        "//tools/idea/java/java-psi-impl",
        "//tools/idea/java/java-indexing-impl",
        "//tools:idea.java-analysis-impl",
        "//tools/idea/java/java-structure-view",
    ],
    # do not sort: must match IML order
    deps = [
        "//tools/idea/platform/util[module]",
        "//tools/idea/java/openapi[module]",
        "//tools/idea/.idea/libraries:Trove4j",
        "//tools/idea/.idea/libraries:OroMatcher",
        "//tools:idea.platform-impl_and_others[module]",
        "//tools/idea/java/java-runtime[module]",
        "//tools/idea/java/compiler/openapi:compiler-openapi[module]",
        "//tools/idea/java/jsp-openapi[module]",
        "//tools/idea/java/jsp-spi[module]",
        "//tools/idea/java/execution/openapi:execution-openapi[module]",
        "//tools/idea/.idea/libraries:asm",
        "//tools/idea/platform/icons[module]",
        "//tools/idea/.idea/libraries:jcip",
        "//tools/idea/.idea/libraries:Groovy",
        "//tools/idea/java/java-psi-impl[module]",
        "//tools/idea/java/java-indexing-impl[module]",
        "//tools/idea/java/java-indexing-api[module]",
        "//tools/idea/jps/model-impl:jps-model-impl[module]",
        "//tools:idea.java-analysis-impl[module]",
        "//tools/idea/platform/external-system-api[module]",
        "//tools/idea/.idea/libraries:asm5",
        "//tools/idea/.idea/libraries:Guava",
        "//tools/idea/.idea/libraries:Xerces",
        "//tools/idea/.idea/libraries:Velocity",
        "//tools/idea/java/java-structure-view[module]",
        "//tools/idea/.idea/libraries:nekohtml",
    ],
)

iml_module(
    name = "idea.java-tests",
    exclude = ["idea/java/java-tests/testSrc/com/intellij/index/IndexTestGenerator.scala"],
    tags = ["managed"],
    test_srcs = [
        "idea/java/java-tests/testSrc",
        "idea/plugins/InspectionGadgets/testsrc",
        "idea/plugins/IntentionPowerPak/testSrc",
        "idea/plugins/generate-tostring/testSrc",
    ],
    test_tags = ["manual"],
    visibility = ["//visibility:public"],
    exports = ["//tools/idea/plugins/java-i18n"],
    # do not sort: must match IML order
    deps = [
        "//tools:idea.compiler-impl_and_others[module]",
        "//tools:idea.java-impl[module]",
        "//tools/idea/community-resources[module]",
        "//tools/idea/platform/platform-api[module]",
        "//tools/idea/.idea/libraries:Velocity",
        "//tools/idea/plugins/java-i18n[module]",
        "//tools/idea/.idea/libraries:asm5",
        "//tools/idea/java/compiler/instrumentation-util[module]",
        "//tools/idea/.idea/libraries:Groovy",
        "//tools/idea/plugins/IntelliLang:IntelliLang-java[module]",
        "//tools/idea/plugins/IntelliLang:IntelliLang-xml[module]",
        "//tools/idea/plugins/junit[module, test]",
        "//tools/idea/plugins/testng[module, test]",
        "//tools/idea/plugins/ui-designer[module, test]",
        "//tools/idea/plugins/eclipse[module, test]",
        "//tools/idea/java/execution/openapi:execution-openapi[module]",
        "//tools/idea/platform/platform-tests[module, test]",
        "//tools/idea/java/java-indexing-api[module, test]",
        "//tools/idea/plugins/junit_rt[module, test]",
        "//tools/idea/plugins/properties/properties-psi-api[module, test]",
        "//tools/idea/plugins/java-decompiler/plugin:java-decompiler-plugin[module, test]",
        "//tools:idea.platform-impl_and_others[module]",
        "//tools/idea/platform/util:util-tests[module, test]",
        "//tools/idea/.idea/libraries:Mocks[test]",
        "//tools/idea/plugins/groovy:jetgroovy[module, test]",
        "//tools:idea.lang-tests[module, test]",
        "//tools/idea/.idea/libraries:mockito[test]",
        "//tools/idea/.idea/libraries:assertJ[test]",
        "//tools/idea/.idea/libraries:KotlinTest[test]",
        "//tools/idea/platform/built-in-server-api[module, test]",
    ],
)

iml_module(
    name = "idea.compiler-impl_and_others",
    # do not sort: must match IML order
    srcs = [
        "idea/java/idea-ui/src",
        "idea/platform/external-system-impl/src",
        "idea/java/testFramework/src",
        "idea/java/execution/impl/src",
        "idea/java/debugger/impl/src",
        "idea/java/compiler/impl/src",
    ],
    resources = ["idea/platform/external-system-impl/resources"],
    tags = ["managed"],
    test_srcs = [
        "idea/platform/external-system-impl/testSrc",
        "idea/java/compiler/impl/testSrc",
    ],
    test_tags = ["manual"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    exports = [
        "//tools/idea/java/openapi",
        "//tools/idea/platform/util",
        "//tools/idea/platform/lang-api",
        "//tools:idea.java-impl",
        "//tools:idea.platform-impl_and_others",
        "//tools/idea/java/execution/openapi:execution-openapi",
        "//tools/idea/platform/testRunner",
        "//tools/idea/java/debugger/openapi:debugger-openapi",
        "//tools/idea/java/compiler/openapi:compiler-openapi",
    ],
    # do not sort: must match IML order
    deps = [
        "//tools/idea/java/openapi[module]",
        "//tools/idea/java/compiler/openapi:compiler-openapi[module]",
        "//tools:idea.java-impl[module]",
        "//tools/idea/.idea/libraries:OroMatcher",
        "//tools:idea.platform-impl_and_others[module]",
        "//tools/idea/.idea/libraries:Guava",
        "//tools/idea/platform/external-system-api[module]",
        "//tools/idea/platform/projectModel-impl[module]",
        "//tools/idea/java/execution/openapi:execution-openapi[module]",
        "//tools/idea/platform/vcs-api[module]",
        "//tools/idea/.idea/libraries:Groovy",
        "//tools/idea/platform/testRunner[module]",
        "//tools/idea/platform/smRunner[module]",
        "//tools/idea/.idea/libraries:JUnit4",
        "//tools/idea/platform/util[module]",
        "//tools/idea/.idea/libraries:JDOM",
        "//tools/idea/.idea/libraries:Log4J",
        "//tools/idea/platform/lang-api[module]",
        "//tools/idea/java/java-runtime[module]",
        "//tools/idea/.idea/libraries:jgoodies-forms",
        "//tools/idea/java/java-indexing-api[module]",
        "//tools/idea/.idea/libraries:Coverage",
        "//tools/idea/java/debugger/openapi:debugger-openapi[module]",
        "//tools/idea/resources[module]",
        "//tools/idea/platform/xdebugger-api[module]",
        "//tools/idea/java/jsp-openapi[module]",
        "//tools/idea/jps/jps-builders[module]",
        "//tools/idea/.idea/libraries:Trove4j",
        "//tools/idea/java/compiler/instrumentation-util[module]",
        "//tools/idea/.idea/libraries:asm5",
        "//tools/idea/platform/platform-api[module]",
        "//tools/idea/jps/jps-launcher[module]",
        "//tools/idea/.idea/libraries:Netty",
        "//tools/idea/jps/model-impl:jps-model-impl[module]",
        "//tools:idea.java-analysis-impl[module]",
        "//tools/idea/.idea/libraries:KotlinJavaRuntime",
    ],
)

iml_module(
    name = "fest-swing",
    srcs = ["swing-testing/fest-swing/src/main/java"],
    javacopts = ["-extra_checks:off"],
    tags = ["managed"],
    test_resources = ["swing-testing/fest-swing/src/test/resources"],
    test_srcs = ["swing-testing/fest-swing/src/test/java"],
    test_tags = ["manual"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    exports = [
        "//tools:swing-testing/fest-swing/lib/fest-reflect-2.0-SNAPSHOT",
        "//tools:swing-testing/fest-swing/lib/fest-util-1.3.0-SNAPSHOT",
    ],
    # do not sort: must match IML order
    deps = [
        "//tools:swing-testing/fest-swing/lib/fest-reflect-2.0-SNAPSHOT",
        "//tools:swing-testing/fest-swing/lib/fest-util-1.3.0-SNAPSHOT",
        "//tools:swing-testing/fest-swing/lib/fest-assert-1.5.0-SNAPSHOT",
        "//tools:swing-testing/fest-swing/lib/jsr305-1.3.9",
        "//tools/idea/.idea/libraries:JUnit4[test]",
        "//tools/idea/.idea/libraries:mockito[test]",
        "//tools:swing-testing/fest-swing/lib/MultithreadedTC-1.01[test]",
    ],
)

java_import(
    name = "swing-testing/fest-swing/lib/fest-reflect-2.0-SNAPSHOT",
    jars = ["swing-testing/fest-swing/lib/fest-reflect-2.0-SNAPSHOT.jar"],
    tags = ["managed"],
    visibility = ["//visibility:public"],
)

java_import(
    name = "swing-testing/fest-swing/lib/fest-util-1.3.0-SNAPSHOT",
    jars = ["swing-testing/fest-swing/lib/fest-util-1.3.0-SNAPSHOT.jar"],
    tags = ["managed"],
    visibility = ["//visibility:public"],
)

java_import(
    name = "swing-testing/fest-swing/lib/fest-assert-1.5.0-SNAPSHOT",
    jars = ["swing-testing/fest-swing/lib/fest-assert-1.5.0-SNAPSHOT.jar"],
    tags = ["managed"],
    visibility = ["//visibility:public"],
)

java_import(
    name = "swing-testing/fest-swing/lib/jsr305-1.3.9",
    jars = ["swing-testing/fest-swing/lib/jsr305-1.3.9.jar"],
    tags = ["managed"],
    visibility = ["//visibility:public"],
)

java_import(
    name = "swing-testing/fest-swing/lib/MultithreadedTC-1.01",
    jars = ["swing-testing/fest-swing/lib/MultithreadedTC-1.01.jar"],
    tags = ["managed"],
    visibility = ["//visibility:public"],
)

# TODO: move back to idea/plugins/svn4idea when we can avoid conflict with "build" dir there
iml_module(
    name = "idea.svn4idea",
    # do not sort: must match IML order
    srcs = [
        "idea/plugins/svn4idea/resources",
        "idea/plugins/svn4idea/src",
    ],
    tags = ["managed"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//tools/idea/platform/util[module]",
        "//tools/idea/.idea/libraries:JDOM",
        "//tools/idea/.idea/libraries:JUnit4[test]",
        "//tools/idea/.idea/libraries:Log4J",
        "//tools/idea/platform/vcs-api[module]",
        "//tools/idea/platform/platform-api[module]",
        "//tools:idea.platform-impl_and_others[module]",
        "//tools/idea/.idea/libraries:jna",
        "//tools/idea/.idea/libraries:pty4j",
        "//tools/idea/.idea/libraries:purejavacomm",
        "//tools/idea/.idea/libraries:http-client",
        "//tools/idea/.idea/libraries:sqlite",
    ],
)

# TODO: move back to idea/plugins/svn4idea when we can avoid conflict with "build" dir there
iml_module(
    name = "idea.svn4idea-tests",
    tags = ["managed"],
    test_srcs = ["idea/plugins/svn4idea/testSource"],
    test_tags = ["manual"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//tools/idea/.idea/libraries:JUnit4[test]",
        "//tools:idea.svn4idea[module]",
        "//tools/idea/platform/vcs-api[module]",
        "//tools:idea.platform-impl_and_others[module]",
        "//tools/idea/platform/lang-api[module]",
        "//tools/idea/platform/vcs-tests[module, test]",
    ],
)
