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
#include <Core/Block.h>


namespace DB
{
/** Common part for implementation of MySQLBlockInputStream, MongoDBBlockInputStream and others.
  */
struct ExternalResultDescription
{
    enum struct ValueType
    {
        vtUInt8,
        vtUInt16,
        vtUInt32,
        vtUInt64,
        vtInt8,
        vtInt16,
        vtInt32,
        vtInt64,
        vtFloat32,
        vtFloat64,
        vtEnum8,
        vtEnum16,
        vtString,
        vtDate,
        vtDate32,
        vtDateTime,
        vtUUID,
        vtDateTime64,
        vtDecimal32,
        vtDecimal64,
        vtDecimal128,
        vtDecimal256,
        vtArray,
        vtFixedString
    };

    Block sample_block;
    std::vector<std::pair<ValueType, bool /* is_nullable */>> types;

    void init(const Block & sample_block_);
};

}
