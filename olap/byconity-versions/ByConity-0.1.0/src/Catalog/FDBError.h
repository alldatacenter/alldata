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

namespace DB::FDB
{

enum FDBError
{
#define CREATE_ENUM(name, number) \
    FDB_##name = number,
#define ERROR(name, number, description) \
    CREATE_ENUM(name, number)
#include <foundationdb/error_definitions.h>
#undef CREATE_ENUM
};

}
