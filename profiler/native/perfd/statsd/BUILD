package(default_visibility = ["//visibility:public"])

cc_library(
    name = "statsd",
    srcs = [
        "pulled_atoms/mobile_bytes_transfer.cc",
        "pulled_atoms/wifi_bytes_transfer.cc",
        "statsd_subscriber.cc",
    ],
    hdrs = [
        "statsd_subscriber.h",
    ] + glob(["*/*.h"]),
    copts = [
        "-Itools/base/transport",
        "-I$(GENDIR)/tools/base/profiler/native/perfd",
    ],
    tags = ["no_windows"],
    deps = [
        "//tools/base/profiler/native/perfd/statsd/proto:statsd_proto",
        "//tools/base/transport/utils",
    ],
)

cc_test(
    name = "statsd_test",
    srcs = [
        "statsd_subscriber_test.cc",
    ],
    copts = [
        "-Itools/base/transport",
        "-I$(GENDIR)/tools/base/profiler/native/perfd",
    ],
    linkstatic = 1,
    tags = ["no_windows"],
    deps = [
        ":statsd",
        "//external:gmock_main",
    ],
)