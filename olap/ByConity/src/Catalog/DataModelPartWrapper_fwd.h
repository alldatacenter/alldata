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
#include <vector>

namespace DB
{
class DataModelPartWrapper;
class ServerDataPart;

using DataModelPartWrapperPtr = std::shared_ptr<DataModelPartWrapper>;
using DataModelPartWrapperVector = std::vector<DataModelPartWrapperPtr>;
using ServerDataPartPtr = std::shared_ptr<const ServerDataPart>;
using ServerDataPartsVector = std::vector<ServerDataPartPtr>;
}
