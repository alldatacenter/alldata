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


namespace DB
{

struct KafkaLogElement
{
    enum Type
    {
        EMPTY = 0,
        POLL = 1,
        PARSE_ERROR = 2,
        WRITE = 3,
        EXCEPTION = 4,
        EMPTY_MESSAGE = 5,
        FILTER = 6,
        COMMIT = 7,
    };
    Type event_type = EMPTY;

    time_t event_time = 0;
    UInt32 duration_ms = 0;

    String cnch_database;
    String cnch_table;
    String database;
    String table;
    String consumer;

    UInt64 metric = 0;
    UInt64 bytes = 0;

    UInt8 has_error = 0;
    String last_exception;

    static std::string name() { return "KafkaLog"; }
    static NamesAndTypesList getNamesAndTypes();
    static NamesAndAliases getNamesAndAliases() { return {}; }
    void appendToBlock(MutableColumns & columns) const;
};

/// Instead of typedef - to allow forward declaration.
class KafkaLog : public SystemLog<KafkaLogElement>
{
    using SystemLog<KafkaLogElement>::SystemLog;
};

}
