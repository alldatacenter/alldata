/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <vector>
#include <string>
#include <set>
#include <unordered_set>
#include <unordered_map>


namespace DB
{

using Names = std::vector<std::string>;
using NameSet = std::unordered_set<std::string>;
using NameOrderedSet = std::set<std::string>;
using NameToNameMap = std::unordered_map<std::string, std::string>;
using NameToNameSetMap = std::unordered_map<std::string, NameSet>;
using NameToNameVector = std::vector<std::pair<std::string, std::string>>;
using NameToIndexMap = std::unordered_map<std::string, size_t>;

using NameWithAlias = std::pair<std::string, std::string>;
using NamesWithAliases = std::vector<NameWithAlias>;

}
