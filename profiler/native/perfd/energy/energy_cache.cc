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
#include "energy_cache.h"

using profiler::proto::EnergyDataResponse;

namespace profiler {

void EnergyCache::SaveEnergySample(
    const EnergyDataResponse::EnergySample& sample) {
  std::lock_guard<std::mutex> lock(samples_mutex_);
  int save_index;
  if (samples_size_ < samples_capacity_) {
    save_index = ToValidIndex(samples_head_ + samples_size_);
    samples_size_++;
  } else {
    save_index = samples_head_;
    samples_head_ = ToValidIndex(samples_head_ + 1);
  }
  samples_[save_index].CopyFrom(sample);
}

void EnergyCache::LoadEnergyData(int64_t start_time_excl, int64_t end_time_incl,
                                 EnergyDataResponse* response) {
  std::lock_guard<std::mutex> lock(samples_mutex_);

  int64_t latest_sample_timestamp = 0;
  for (int32_t i = 0; i < samples_size_; ++i) {
    EnergyDataResponse::EnergySample& sample =
        samples_[ToValidIndex(samples_head_ + i)];

    // Can we guarantee the samples be in monotonically increasing timestamp?
    // If so we can optimize this by stopping the moment we hit out later than
    // the end time.
    if (sample.timestamp() > start_time_excl &&
        sample.timestamp() <= end_time_incl) {
      if (sample.timestamp() > latest_sample_timestamp) {
        latest_sample_timestamp = sample.timestamp();
      }
      response->add_energy_samples()->CopyFrom(sample);
    }
  }
  response->set_latest_sample_timestamp(latest_sample_timestamp);
}

int32_t EnergyCache::ToValidIndex(int32_t current_index) const {
  return current_index % samples_capacity_;
}

}  // namespace profiler
