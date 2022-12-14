load("//tools/base/bazel:android.bzl", "select_android")

package(default_visibility = ["//visibility:public"])

cc_library(
    name = "profiler_utils",
    srcs = [
        "activity_manager.cc",
        "background_queue.cc",
        "bash_command.cc",
        "config.cc",
        "current_process.cc",
        "file_cache.cc",
        "file_reader.cc",
        "fs/dir.cc",
        "fs/disk_file_system.cc",
        "fs/file.cc",
        "fs/file_system.cc",
        "fs/memory_file_system.cc",
        "fs/path.cc",
        "memory_map.cc",
        "native_backtrace.cc",
        "installer.cc",
        "package_manager.cc",
        "procfs_files.cc",
        "socket_utils.cc",
        "stopwatch.cc",
        "tokenizer.cc",
        "trace.cc",
    ] + select({
        "//tools/base/bazel:darwin": [
            "clock.mac.cc",
            "current_process.mac.cc",
            "filesystem_notifier.mac.cc",
            "thread_name.mac.cc",
        ],
        "//conditions:default": [
            "clock.cc",
            "current_process.linux.cc",
            "filesystem_notifier.cc",
            "thread_name.cc",
        ],
    }) + select_android(
        [
            "bash_command.android.cc",
            "device_info.android.cc",
            "log.android.cc",
            "process_manager.android.cc",
        ],
        [
            "bash_command.host.cc",
            "device_info.host.cc",
            "log.cc",
            "process_manager.host.cc",
        ],
    ),
    hdrs = [
        "activity_manager.h",
        "android_studio_version.h",
        "background_queue.h",
        "bash_command.h",
        "circular_buffer.h",
        "clock.h",
        "config.h",
        "count_down_latch.h",
        "current_process.h",
        "device_info.h",
        "fake_clock.h",
        "file_cache.h",
        "file_reader.h",
        "filesystem_notifier.h",
        "fs/dir.h",
        "fs/disk_file_system.h",
        "fs/file.h",
        "fs/file_system.h",
        "fs/memory_file_system.h",
        "fs/path.h",
        "installer.h",
        "log.h",
        "memory_map.h",
        "native_backtrace.h",
        "package_manager.h",
        "process_manager.h",
        "procfs_files.h",
        "producer_consumer_queue.h",
        "shared_mutex.h",
        "socket_utils.h",
        "stopwatch.h",
        "thread_name.h",
        "time_value_buffer.h",
        "tokenizer.h",
        "trace.h",
        "trie.h",
        "uid_fetcher.h",
    ],
    copts = [
        "-Itools/base/profiler/native",
        "-I$(GENDIR)/tools/base/profiler",
    ],
    tags = ["no_windows"],
    deps = [
        "//tools/base/profiler/proto:profiler_cc_proto",
    ],
)

cc_test(
    name = "profiler_utils_test",
    srcs = [
        "activity_manager_test.cc",
        "background_queue_test.cc",
        "circular_buffer_test.cc",
        "clock_test.cc",
        "count_down_latch_test.cc",
        "current_process_test.cc",
        "file_cache_test.cc",
        "file_reader_test.cc",
        "fs/file_system_test.cc",
        "memory_map_test.cc",
        "native_backtrace_test.cc",
        "producer_consumer_queue_test.cc",
        "shared_mutex_test.cc",
        "stopwatch_test.cc",
        "time_value_buffer_test.cc",
        "tokenizer_test.cc",
        "trie_test.cc",
        "uid_fetcher_test.cc",
    ],
    copts = [
        "-Itools/base/profiler/native",
        "-I$(GENDIR)/tools/base/profiler",
    ],
    data = [
        "//tools/base/profiler/native/testdata:utils",
    ],
    linkstatic = 1,
    tags = ["no_windows"],
    deps = [
        ":profiler_utils",
        "//external:gmock_main",
        "//tools/base/profiler/native/test:testutils",
    ],
)
