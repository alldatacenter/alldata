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

#include <Interpreters/SystemLog.h>
#include <Core/NamesAndAliases.h>

namespace DB
{

struct MutationLogElement
{
    enum Type
    {
        MUTATION_START = 1,
        MUTATION_KILL = 2,
        MUTATION_FINISH = 3,
        MUTATION_ABORT = 4,
    };

    Type event_type = MUTATION_START;
    time_t event_time = 0;
    String database_name;
    String table_name;
    String mutation_id;
    String query_id;
    time_t create_time = 0;
    UInt64 block_number = 0;
    Strings commands;

    static std::string name() { return "MutationLog"; }

    static NamesAndTypesList getNamesAndTypes();
    static NamesAndAliases getNamesAndAliases() { return {}; }

    void appendToBlock(MutableColumns & columns) const;
};

/// Instead of typedef - to allow forward declaration.
class MutationLog : public SystemLog<MutationLogElement>
{
    using SystemLog<MutationLogElement>::SystemLog;
};

}
