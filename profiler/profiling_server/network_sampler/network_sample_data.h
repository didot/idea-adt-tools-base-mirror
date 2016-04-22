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
#ifndef NETWORK_SAMPLE_DATA_H_
#define NETWORK_SAMPLE_DATA_H_

#include <cstdint>

namespace network_sampler {

enum class NetworkSampleType {
  TRAFFIC,
  CONNECTION,
};

struct NetworkSampleData {
  NetworkSampleType type_;
  int connections_;
  uint64_t send_bytes_;
  uint64_t receive_bytes_;
};

} // namespace network_sampler

#endif // NETWORK_SAMPLE_DATA_H_
