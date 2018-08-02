load("//tools/base/bazel:proto.bzl", "cc_grpc_proto_library", "java_proto_library")

package(default_visibility = [
    "//tools/base/deploy:__subpackages__",
])

cc_grpc_proto_library(
    name = "cc_proto",
    srcs = glob(["*.proto"]),
    # Doesn't build with this set to zero; unsure if bug in the bazel genrule or not.
    grpc_support = 1,
    tags = ["no_windows"],
)

java_proto_library(
    name = "java_proto",
    srcs = glob(["*.proto"]),
    # Seems to be ok here.
    grpc_support = 0,
)

cc_binary(
    name = "proto_tool",
    srcs = [
        "proto_tool.cc",
    ],
    copts = ["-I$(GENDIR)/tools/base/deploy/proto"],
    tags = [
        "no_windows",
    ],
    deps = [
        "//tools/base/deploy/proto:cc_proto",
    ],
)