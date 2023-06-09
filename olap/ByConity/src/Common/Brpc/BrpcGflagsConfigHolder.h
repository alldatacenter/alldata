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
#include <Poco/Util/AbstractConfiguration.h>
#include <Common/Brpc/SealedConfigHolder.h>
#include <common/logger_useful.h>

namespace DB
{
/// Config example:
/// <brpc>
///    <gflags>
///        <event_dispatcher_num>10</event_dispatcher_num>
///        <defer_close_second>13</defer_close_second>
///    </gflags>
///</brpc>
class BrpcGflagsConfigHolder : public SealedConfigHolder<BrpcGflagsConfigHolder, RawConfig, RawConfigDeleter>
{
public:
    static inline std::string name{"gflags"};
    explicit BrpcGflagsConfigHolder() { logger = &Poco::Logger::get("BrpcGflagsConfigHolder"); }
    void afterInit(const RawConfig * config_ptr) override;
    void onChange(const RawConfig * old_conf_ptr, const RawConfig * new_conf_ptr) override;
    bool hasChanged(const RawConfig * old_conf_ptr, const RawConfig * new_conf_ptr) override;

private:
    Poco::Logger * logger;
};

}
