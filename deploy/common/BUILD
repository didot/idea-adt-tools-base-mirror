load("//tools/base/bazel:android.bzl", "select_android", "ANDROID_COPTS", "ANDROID_LINKOPTS")

cc_library(
    name = "common_lib",
    srcs = [
        "event.cc",
        "message_pipe_wrapper.cc",
        "size_buffer.h",
        "socket.cc",
        "trace.cc",
        "utils.cc",
    ],
    hdrs = [
        "event.h",
        "message_pipe_wrapper.h",
        "socket.h",
        "trace.h",
        "utils.h",
    ],
    copts = ANDROID_COPTS,
    linkopts = ANDROID_LINKOPTS,
    tags = ["no_windows"],
    visibility = ["//visibility:public"],
    deps = [
        ":log",
        "//tools/base/deploy/proto:cc_proto",
    ],
)

cc_library(
    name = "log",
    srcs = select_android(
        ["log.android.cc"],
        ["log.cc"],
    ),
    hdrs = [
        "log.h",
    ],
    tags = ["no_windows"],
    visibility = ["//tools/base/deploy:__subpackages__"],
)

cc_test(
    name = "common_test",
    size = "medium",
    srcs = [
        "message_pipe_wrapper_test.cc",
        "socket_test.cc",
    ],
    linkstatic = 1,
    tags = [
        "no_test_mac",
        "no_windows",
    ],
    visibility = ["//visibility:public"],
    deps = [
        ":common_lib",
        "//external:gmock_main",
    ],
)
