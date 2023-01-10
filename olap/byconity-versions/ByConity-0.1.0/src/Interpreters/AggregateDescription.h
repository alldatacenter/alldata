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

#include <AggregateFunctions/IAggregateFunction.h>
#include <Core/ColumnNumbers.h>
#include <Core/Names.h>
#include <Core/Types.h>

namespace DB
{

namespace JSONBuilder { class JSONMap; }

class ReadBuffer;
class WriteBuffer;

struct AggregateDescription
{
    AggregateFunctionPtr function;
    Array parameters;        /// Parameters of the (parametric) aggregate function.
    ColumnNumbers arguments;
    Names argument_names;    /// used if no `arguments` are specified.
    String column_name;      /// What name to use for a column with aggregate function values
    String mask_column;

    void explain(WriteBuffer & out, size_t indent) const; /// Get description for EXPLAIN query.
    void explain(JSONBuilder::JSONMap & map) const;

    void serialize(WriteBuffer & buf) const;
    void deserialize(ReadBuffer & buf);
};

using AggregateDescriptions = std::vector<AggregateDescription>;

}
