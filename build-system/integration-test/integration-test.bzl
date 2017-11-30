load('//tools/base/bazel:kotlin.bzl', 'kotlin_library', 'kotlin_jar')

# A gradle integration test
#
# Usage:
# gradle_integration_test(
#     name = 'name',
#     srcs = glob(['**/*.java']) - Note this doesn't compile kotlin and java together.
#     deps = test classes output
#     data = test data: SDK parts and test projects.
#     maven_repos = Absolute targets for maven repos containing the plugin(s) under test
#     shard_count = 8)
def gradle_integration_test(
    name, srcs, deps, data, maven_repos, resources=[], runtime_deps=[], shard_count=1, **kwargs):

  kotlin_srcs = [src for src in srcs if src.endswith('.kt')]
  java_srcs = [src for src in srcs if src.endswith('.java')]
  test_classes_target_names = []
  if not kotlin_srcs and not java_srcs:
    fail('No sources found for gradle_integration_test ' + name)

  if kotlin_srcs:
    kotlin_name = name + '.kotlin.gradle_integration_test'
    kotlin_jar(
        name = kotlin_name,
        srcs = ['src'],
        inputs = kotlin_srcs,
        deps = deps,
    )
    kotlin_java_import_name = kotlin_name + '.java_import'
    native.java_import(
      name = kotlin_java_import_name,
      jars = [kotlin_name],
    )
    test_classes_target_names += [kotlin_java_import_name]

  if java_srcs:
    java_name = name + '.java.gradle_integration_test'
    test_classes_target_names += [java_name]
    native.java_library(
        name = java_name,
        srcs = java_srcs,
        deps =  deps,
    )

  # Stringy conversion of repo to its target and file name
  # //tools/base/build-system/integration-test/application:gradle_plugin
  # to
  # //tools/base/build-system/integration-test/application:gradle_plugin_repo.zip
  # tools/base/build-system/integration-test/application/gradle_plugin_repo.zip,
  if not all([maven_repo.startswith('//') for maven_repo in maven_repos]):
    fail('All maven repos should be absolute targets.')

  zip_targets = [maven_repo + '_repo.zip' for maven_repo in maven_repos]
  zip_file_names = ','.join([maven_repo[2:].replace(':','/') + '_repo.zip' for maven_repo in maven_repos])

  native.java_test(
      name = name,
      timeout = 'eternal',
      data = data + zip_targets,
      jvm_flags = [
          '-Dtest.suite.jar=.gradle_integration_test.jar',
          '-Dfile.encoding=UTF-8',
          '-Dsun.jnu.encoding=UTF-8',
          '-Dmaven.repo.local=/tmp/localMavenRepo',  # For gradle publishing, writing to ~/.m2
          '-Dtest.excludeCategories=com.android.build.gradle.integration.common.category.DeviceTests,com.android.build.gradle.integration.common.category.DeviceTestsQuarantine,com.android.build.gradle.integration.common.category.OnlineTests',
          '-Dtest.android.build.gradle.integration.repos=' + zip_file_names,
      ],
      resources = resources,
      shard_count = shard_count,
      tags = [
          'block-network',
          'cpu:3',
          'gradle_integration',
          'no_test_mac',  # b/69151132 Time out frequently when run on mac.
          'slow',
      ],
      test_class = 'com.android.build.gradle.integration.BazelIntegrationTestsSuite',
      runtime_deps = [
          # Need to put this on the classpath before TestRunner_deploy.jar which contains
          # old JUnit classes. See https://github.com/bazelbuild/bazel/issues/2146.
          '//tools/base/third_party:junit_junit',
      ] + runtime_deps + [':' + name for name in test_classes_target_names],
      **kwargs
  )