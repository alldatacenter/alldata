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

#include <cstddef>
#include <map>
#include <Core/Defines.h>
#include <Core/Types.h>

namespace DB
{

/**
 * Mode of opening a file for write.
 */
enum class WriteMode
{
    Rewrite,
    Append,
    Create
};

struct WriteSettings
{
    size_t buffer_size = DBMS_DEFAULT_BUFFER_SIZE;
    WriteMode mode = WriteMode::Rewrite;
    std::map<String, String> file_meta = {};
};

}
