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

#include <MergeTreeCommon/InsertionLabel.h>

#include <Catalog/MetastoreProxy.h>
#include <IO/ReadBufferFromString.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteBufferFromString.h>
#include <IO/WriteHelpers.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
}

InsertionLabel::InsertionLabel(UUID uuid, const std::string & name_, UInt64 txn_id_) : table_uuid(uuid), txn_id(txn_id_)
{
    validateName(name_);
    name = name_;
    create_time = time(nullptr);
}

void InsertionLabel::validateName(const std::string & name)
{
    for (auto & c : name)
    {
        if (!std::isalnum(c) && c != '#' && c != '-')
            throw Exception("Invalid label name, only allowed alphanumeric chars and two special chars {#, -}", ErrorCodes::BAD_ARGUMENTS);
    }
}

std::string InsertionLabel::serializeValue() const
{
    WriteBufferFromOwnString out;
    writeIntBinary(txn_id, out);
    writeIntBinary(create_time, out);
    UInt8 temp = status;
    writeIntBinary(temp, out);
    return out.str();
}

void InsertionLabel::parseValue(const std::string & data)
{
    ReadBufferFromString in(data);
    readValue(in);
}

void InsertionLabel::readValue(ReadBuffer & in)
{
    readIntBinary(txn_id, in);
    readIntBinary(create_time, in);
    UInt8 temp{0};
    readIntBinary(temp, in);
    status = static_cast<Status>(temp);
}
}
