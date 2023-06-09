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

#include <string>
#include <memory>
#include <Core/UUID.h>

namespace DB
{
class ReadBuffer;

struct InsertionLabel
{
    enum Status : UInt8
    {
        Precommitted = 0,
        Committed = 1,
    };

    UUID table_uuid;
    std::string name;
    UInt64 txn_id{0};
    time_t create_time{0};
    Status status{Precommitted};

    InsertionLabel() = default;
    InsertionLabel(UUID uuid, const std::string & name, UInt64 txn_id = 0);

    void commit() { status = Committed; }

    static void validateName(const std::string & name);

    std::string serializeValue() const;
    void parseValue(const std::string & data);
    void readValue(ReadBuffer & in);
};

using InsertionLabelPtr = std::shared_ptr<InsertionLabel>;

}
