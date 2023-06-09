/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include <memory>
#include <ostream>
#include <brpc/channel.h>
#include <brpc/options.pb.h>
#include <butil/containers/doubly_buffered_data.h>
#include <Common/Brpc/BrpcChannelPoolOptions.h>
#include <Common/Brpc/QueryableConfigHolder.h>
#include <common/logger_useful.h>

namespace DB
{
/// Config Example:
///  <brpc>
///      <channel_pool>
///          <rpc_default>
///              <max_connections>8</max_connections>
///              <load_balancer>rr</load_balancer>
///              <channel_options>
///                  <timeout_ms>3000</timeout_ms>
///              </channel_options>
///          </rpc_default>
///          <stream_default>
///              <max_connections>8</max_connections>
///              <load_balancer>rr</load_balancer>
///              <channel_options>
///                  <timeout_ms>3000</timeout_ms>
///              </channel_options>
///          </stream_default>
///          <pool_name_1>
///              <max_connections>8</max_connections>
///              <load_balancer>rr</load_balancer>
///              <channel_options>
///                  <timeout_ms>3000</timeout_ms>
///              </channel_options>
///          </pool_name_1>
///      </channel_pool>
///  </brpc>

/// held by BrpcApplication(singleton), use std::cout for log
class BrpcChannelPoolConfigHolder : public QueryableConfigHolder<BrpcChannelPoolConfigHolder, BrpcChannelPoolOptions::PoolOptionsMap>
{
public:
    using PoolOptionsMap = BrpcChannelPoolOptions::PoolOptionsMap;
    explicit BrpcChannelPoolConfigHolder() = default;
    static inline std::string name{"channel_pool"};
    void afterInit(const PoolOptionsMap * conf_ptr) override;
    bool hasChanged(const PoolOptionsMap * old_conf_ptr, const PoolOptionsMap * new_conf_ptr) override;
    void onChange(const PoolOptionsMap * old_conf_ptr, const PoolOptionsMap * new_conf_ptr) override;
    std::unique_ptr<PoolOptionsMap> createTypedConfig(RawConfAutoPtr conf_ptr) noexcept override;

private:
    static void fillWithConfig(
        BrpcChannelPoolOptions::PoolOptions & options,
        const BrpcChannelPoolOptions::PoolOptions & default_options,
        RawConfAutoPtr & pool_options_conf_ptr,
        const std::string & tag_prefix);
};
std::ostream & operator<<(std::ostream & os, const BrpcChannelPoolConfigHolder::PoolOptionsMap & pool_options_map);

}
