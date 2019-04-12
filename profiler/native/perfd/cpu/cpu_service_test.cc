/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include "perfd/cpu/cpu_service.h"

#include <gmock/gmock.h>
#include <gtest/gtest.h>
#include "perfd/cpu/fake_atrace.h"
#include "perfd/cpu/fake_perfetto.h"
#include "perfd/cpu/fake_simpleperf.h"
#include "utils/device_info_helper.h"
#include "utils/fake_clock.h"
#include "utils/fs/memory_file_system.h"
#include "utils/termination_service.h"

using grpc::ServerContext;
using grpc::Status;
using profiler::proto::CpuProfilerMode;
using profiler::proto::CpuProfilerType;
using profiler::proto::CpuProfilingAppStartRequest;
using profiler::proto::CpuProfilingAppStartResponse;
using profiler::proto::CpuProfilingAppStopRequest;
using profiler::proto::CpuProfilingAppStopResponse;

using std::string;
using testing::HasSubstr;
using testing::Return;
using testing::SaveArg;
using testing::StartsWith;

namespace profiler {

namespace {
const char* const kAmExecutable = "/aaaaa/system/bin/am";
const char* const kProfileStart = "profile start";
const char* const kProfileStop = "profile stop";
}  // namespace

// A subclass of ActivityManager that we want to test. The only difference is it
// has a public constructor.
class TestActivityManager final : public ActivityManager {
 public:
  explicit TestActivityManager(std::unique_ptr<BashCommandRunner> bash)
      : ActivityManager(std::move(bash)) {}
};

// A mock BashCommandRunner that mocks the execution of command.
// We need the mock to run tests across platforms to examine the commands
// generated by ActivityManager.
class MockBashCommandRunner final : public BashCommandRunner {
 public:
  explicit MockBashCommandRunner(const std::string& executable_path)
      : BashCommandRunner(executable_path) {}
  MOCK_CONST_METHOD2(RunAndReadOutput,
                     bool(const std::string& cmd, std::string* output));
};

// A subclass of TerminationService that we want to test. The only difference is
// it has a public constructor and destructor.
class TestTerminationService final : public TerminationService {
 public:
  explicit TestTerminationService() = default;
  ~TestTerminationService() = default;
};

// This needs to be a struct to set default visibility to public for functions /
// members of testing::Test
struct CpuServiceTest : testing::Test {
  std::unique_ptr<CpuServiceImpl> ConfigureDefaultCpuServiceImpl(
      const profiler::proto::DaemonConfig::CpuConfig& config) {
    // Set up CPU service.
    return std::unique_ptr<CpuServiceImpl>(new CpuServiceImpl(
        &clock_, &cache_, &sampler_, &thread_monitor_, config,
        termination_service_.get(), ActivityManager::Instance(),
        std::unique_ptr<SimpleperfManager>(new SimpleperfManager(
            &clock_, std::unique_ptr<Simpleperf>(new FakeSimpleperf()))),
        std::unique_ptr<AtraceManager>(new AtraceManager(
            std::unique_ptr<FileSystem>(new MemoryFileSystem()), &clock_, 50,
            std::unique_ptr<Atrace>(new FakeAtrace(&clock_, false)))),
        std::unique_ptr<PerfettoManager>(new PerfettoManager(
            &clock_, std::unique_ptr<Perfetto>(new FakePerfetto())))));
  }

  // Helper function to run atrace test.
  // TODO: Update function to validate perfetto is run on Q instead of atrace
  // with perfetto flag enabled.
  void RunAtraceTest(int feature_level, bool enable_perfetto,
                     bool expect_perfetto) {
    DeviceInfoHelper::SetDeviceInfo(feature_level);
    const int64_t kSessionId = 123;
    const int32_t kPid = 456;
    // Need to create an app cache for test to store profiling is running.
    cache_.AllocateAppCache(kPid);
    profiler::proto::DaemonConfig::CpuConfig config;
    config.set_use_perfetto(enable_perfetto);
    std::unique_ptr<CpuServiceImpl> cpu_service =
        ConfigureDefaultCpuServiceImpl(config);
    // Start an atrace recording.
    ServerContext context;
    CpuProfilingAppStartRequest start_request;
    start_request.mutable_session()->set_session_id(kSessionId);
    start_request.mutable_session()->set_pid(kPid);
    start_request.mutable_configuration()->set_profiler_mode(
        CpuProfilerMode::SAMPLED);
    start_request.mutable_configuration()->set_profiler_type(
        CpuProfilerType::ATRACE);
    start_request.mutable_configuration()->set_buffer_size_in_mb(8);
    CpuProfilingAppStartResponse start_response;

    // Expect a success result.
    EXPECT_TRUE(
        cpu_service
            ->StartProfilingApp(&context, &start_request, &start_response)
            .ok());
    EXPECT_EQ(start_response.status(), CpuProfilingAppStartResponse::SUCCESS);

    // Validate state.
    EXPECT_EQ(cpu_service->atrace_manager()->IsProfiling(), !expect_perfetto);
    EXPECT_EQ(cpu_service->perfetto_manager()->IsProfiling(), expect_perfetto);

    CpuProfilingAppStopRequest stop_request;
    stop_request.mutable_session()->set_session_id(kSessionId);
    stop_request.mutable_session()->set_pid(kPid);
    stop_request.set_profiler_type(CpuProfilerType::ATRACE);

    // Stop profiling.
    // nullptr for response tells cpu_service this is a test and does not
    // validate output file.
    EXPECT_TRUE(
        cpu_service->StopProfilingApp(&context, &stop_request, nullptr).ok());

    // Validate state.
    EXPECT_FALSE(cpu_service->atrace_manager()->IsProfiling());
    EXPECT_FALSE(cpu_service->perfetto_manager()->IsProfiling());

    // This needs to happen otherwise the termination handler attempts to call
    // shutdown on the CpuService which causes a segfault.
    termination_service_.reset(nullptr);
  }

  FakeClock clock_;
  FileCache file_cache_{
      std::unique_ptr<profiler::FileSystem>(new profiler::MemoryFileSystem()),
      "/"};
  CpuCache cache_{100, &clock_, &file_cache_};
  CpuUsageSampler sampler_{&clock_, &cache_};
  ThreadMonitor thread_monitor_{&clock_, &cache_};
  std::unique_ptr<TestTerminationService> termination_service_{
      new TestTerminationService()};
};

TEST_F(CpuServiceTest, StopSimpleperfTraceWhenPerfdTerminated) {
  const int64_t kSessionId = 123;
  const int32_t kPid = 456;
  profiler::proto::DaemonConfig::CpuConfig config;
  std::unique_ptr<CpuServiceImpl> cpu_service =
      ConfigureDefaultCpuServiceImpl(config);

  // Start a Simpleperf recording.
  ServerContext context;
  CpuProfilingAppStartRequest start_request;
  start_request.mutable_session()->set_session_id(kSessionId);
  start_request.mutable_session()->set_pid(kPid);
  start_request.mutable_configuration()->set_profiler_mode(
      CpuProfilerMode::SAMPLED);
  start_request.mutable_configuration()->set_profiler_type(
      CpuProfilerType::SIMPLEPERF);
  CpuProfilingAppStartResponse start_response;
  cpu_service->StartProfilingApp(&context, &start_request, &start_response);
  // Now, verify that no command has been issued to kill simpleperf.
  auto* fake_simpleperf = dynamic_cast<FakeSimpleperf*>(
      cpu_service->simpleperf_manager()->simpleperf());
  EXPECT_FALSE(fake_simpleperf->GetKillSimpleperfCalled());
  // Simulate that perfd is killed.
  termination_service_.reset(nullptr);
  // Now, verify that command to kill simpleperf has been issued.
  EXPECT_TRUE(fake_simpleperf->GetKillSimpleperfCalled());
}

TEST_F(CpuServiceTest, StopArtTraceWhenPerfdTerminated) {
  const int64_t kSessionId = 123;
  const int32_t kPid = 456;

  // Set up test Activity Manager
  string trace_path;
  string output_string;
  string cmd_1, cmd_2;
  std::unique_ptr<BashCommandRunner> bash{
      new MockBashCommandRunner(kAmExecutable)};
  EXPECT_CALL(
      *(static_cast<MockBashCommandRunner*>(bash.get())),
      RunAndReadOutput(testing::A<const string&>(), testing::A<string*>()))
      .Times(2)
      .WillOnce(DoAll(SaveArg<0>(&cmd_1), Return(true)))
      .WillOnce(DoAll(SaveArg<0>(&cmd_2), Return(true)));

  // This test requires a customized ActivityManager instead of using the
  // default as such we construct the CpuServiceImpl below.
  TestActivityManager activity_manager{std::move(bash)};
  profiler::proto::DaemonConfig::CpuConfig cpu_config;
  CpuServiceImpl cpu_service{
      &clock_,
      &cache_,
      &sampler_,
      &thread_monitor_,
      cpu_config,
      termination_service_.get(),
      &activity_manager,
      std::unique_ptr<SimpleperfManager>(new SimpleperfManager(
          &clock_, std::unique_ptr<Simpleperf>(new FakeSimpleperf()))),
      std::unique_ptr<AtraceManager>(new AtraceManager(
          std::unique_ptr<FileSystem>(new MemoryFileSystem()), &clock_, 50,
          std::unique_ptr<Atrace>(new FakeAtrace(&clock_)))),
      std::unique_ptr<PerfettoManager>(new PerfettoManager(
          &clock_, std::unique_ptr<Perfetto>(new FakePerfetto())))};

  // Start an ART recording.
  ServerContext context;
  CpuProfilingAppStartRequest start_request;
  start_request.mutable_session()->set_session_id(kSessionId);
  start_request.mutable_session()->set_pid(kPid);
  start_request.mutable_configuration()->set_profiler_mode(
      CpuProfilerMode::SAMPLED);
  start_request.mutable_configuration()->set_profiler_type(
      CpuProfilerType::ART);
  CpuProfilingAppStartResponse start_response;
  cpu_service.StartProfilingApp(&context, &start_request, &start_response);
  EXPECT_THAT(cmd_1, StartsWith(kAmExecutable));
  EXPECT_THAT(cmd_1, HasSubstr(kProfileStart));

  // Simulate that perfd is killed.
  termination_service_.reset(nullptr);
  // Now, verify that a command has been issued to stop ART recording.
  EXPECT_THAT(cmd_2, StartsWith(kAmExecutable));
  EXPECT_THAT(cmd_2, HasSubstr(kProfileStop));
}

TEST_F(CpuServiceTest, AtraceRunsOnOWithPerfettoEnabled) {
  RunAtraceTest(DeviceInfo::O, true, false);
}

TEST_F(CpuServiceTest, AtraceRunsOnOWithPerfettoDisabled) {
  RunAtraceTest(DeviceInfo::O, false, false);
}

TEST_F(CpuServiceTest, PerfettoRunsOnPWithPerfettoEnabled) {
  RunAtraceTest(DeviceInfo::P, true, true);
}

TEST_F(CpuServiceTest, AtraceRunsOnQWithPerfettoDisabled) {
  RunAtraceTest(DeviceInfo::Q, false, false);
}

TEST_F(CpuServiceTest, PerfettoRunsOnQWithPerfettoEnabled) {
  RunAtraceTest(DeviceInfo::Q, true, true);
}

}  // namespace profiler
