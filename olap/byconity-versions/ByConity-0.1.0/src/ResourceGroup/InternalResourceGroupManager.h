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

#include <ResourceGroup/IResourceGroup.h>
#include <Parsers/IAST.h>

#include <Poco/Util/AbstractConfiguration.h>
#include <Poco/Util/Timer.h>

#include <atomic>
#include <regex>
#include <unordered_set>
#include <vector>

namespace DB
{
class InternalResourceGroupManager : public IResourceGroupManager
{
public:
    InternalResourceGroupManager() {}
    ~InternalResourceGroupManager() override = default;

    IResourceGroup * selectGroup(const Context & query_context, const IAST * ast) override;
    void initialize(const Poco::Util::AbstractConfiguration & config) override;
    void shutdown() override {}
private:
    std::unique_ptr<ResourceTask> resource_task;
};

}
