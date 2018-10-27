/*
 * Copyright (C) 2018 The Android Open Source Project
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

#include <gtest/gtest.h>

#include <signal.h>
#include <sys/types.h>
#include <unistd.h>

#include <string>
#include <vector>

#include "tools/base/deploy/common/utils.h"
#include "tools/base/deploy/proto/deploy.pb.h"
#include "tools/base/deploy/test/fakes/fake_agent.h"

std::string server_path;
int main(int argc, char** argv) {
  testing::InitGoogleTest(&argc, argv);
  server_path = argv[1];
  return RUN_ALL_TESTS();
}

namespace deploy {

class AgentServerTest : public ::testing::Test {
 public:
  void SetUp() override { pid_ = -1; }

  void StartServer(int agent_count, const char* socket_name) {
    int input_pipe[2];
    ASSERT_EQ(pipe(input_pipe), 0);

    int output_pipe[2];
    ASSERT_EQ(pipe(output_pipe), 0);

    pid_ = fork();
    if (pid_ == 0) {
      close(input_pipe[1]);
      close(output_pipe[0]);

      dup2(input_pipe[0], STDIN_FILENO);
      dup2(output_pipe[1], STDOUT_FILENO);

      close(input_pipe[0]);
      close(output_pipe[1]);

      execlp(server_path.c_str(), "agent_server",
             to_string(agent_count).c_str(), socket_name, (char*)nullptr);
      return;
    }

    close(input_pipe[0]);
    close(output_pipe[1]);

    input_ = new MessagePipeWrapper(input_pipe[1]);
    output_ = new MessagePipeWrapper(output_pipe[0]);
  }

  void TearDown() override {
    int status;
    wait(&status);

    delete input_;
    delete output_;
  }

  deploy::MessagePipeWrapper* input_;
  deploy::MessagePipeWrapper* output_;

 private:
  pid_t pid_;
};

constexpr int MANY = 100;

TEST_F(AgentServerTest, ConnectSingleAgent) {
  StartServer(1, "ConnectSingleAgent");
  FakeAgent agent(0 /* pid */);
  ASSERT_TRUE(agent.Connect("ConnectSingleAgent"));
}

TEST_F(AgentServerTest, ConnectManyAgents) {
  StartServer(MANY, "ConnectManyAgents");
  for (int i = 0; i < MANY; ++i) {
    // This agent will close as soon as it goes out of scope.
    FakeAgent agent(i /* pid */);
    ASSERT_TRUE(agent.Connect("ConnectManyAgents"));
  }
}

TEST_F(AgentServerTest, ForwardSingleAgent) {
  StartServer(1, "ForwardSingleAgent");
  FakeAgent agent(0 /* pid */);
  ASSERT_TRUE(agent.Connect("ForwardSingleAgent"));
  ASSERT_TRUE(agent.RespondSuccess());

  std::string message;
  ASSERT_TRUE(output_->Read(&message));

  proto::AgentSwapResponse response;
  ASSERT_TRUE(response.ParseFromString(message));
  ASSERT_EQ(response.status(), proto::AgentSwapResponse::OK);
}

// Test forwarding a message from many agents to the server's output. The
// agents send the messages serially.
TEST_F(AgentServerTest, ForwardManyAgents) {
  StartServer(MANY, "ForwardManyAgents");
  for (int i = 0; i < MANY; ++i) {
    FakeAgent agent(i /* pid */);
    ASSERT_TRUE(agent.Connect("ForwardManyAgents"));
    ASSERT_TRUE(agent.RespondSuccess());
  }

  std::unordered_set<int> pids;
  for (int i = 0; i < MANY; ++i) {
    std::string message;
    ASSERT_TRUE(output_->Read(&message));

    proto::AgentSwapResponse response;
    ASSERT_TRUE(response.ParseFromString(message));
    ASSERT_EQ(response.status(), proto::AgentSwapResponse::OK);
    pids.emplace(response.pid());
  }

  ASSERT_EQ(pids.size(), MANY);
}

}  // namespace deploy
