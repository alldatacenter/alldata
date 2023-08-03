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

#include <IO/WriteHelpers.h>

namespace DB
{

enum class KeeperApiVersion : uint8_t
{
    ZOOKEEPER_COMPATIBLE = 0,
    WITH_FILTERED_LIST
};

inline constexpr auto current_keeper_api_version = KeeperApiVersion::WITH_FILTERED_LIST;

const std::string keeper_system_path = "/keeper";
const std::string keeper_api_version_path = keeper_system_path + "/api_version";

using PathWithData = std::pair<std::string_view, std::string>;
const std::vector<PathWithData> child_system_paths_with_data
{
    {keeper_api_version_path, toString(static_cast<uint8_t>(current_keeper_api_version))}
};

}
