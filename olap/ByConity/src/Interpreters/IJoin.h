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

#include <memory>
#include <vector>

#include <Core/Names.h>
#include <Columns/IColumn.h>
#include <DataStreams/IBlockStream_fwd.h>
#include <Interpreters/Context_fwd.h>

namespace DB
{

class Block;
struct ExtraBlock;
using ExtraBlockPtr = std::shared_ptr<ExtraBlock>;

class TableJoin;

class IJoin;
using JoinPtr = std::shared_ptr<IJoin>;

enum JoinType : UInt8
{
    Hash = 0,
    Merge,
    NestedLoop,
    Switcher,
};

class IJoin
{
public:
    virtual ~IJoin() = default;

    virtual JoinType getType() const = 0;

    virtual const TableJoin & getTableJoin() const = 0;

    /// Add block of data from right hand of JOIN.
    /// @returns false, if some limit was exceeded and you should not insert more data.
    virtual bool addJoinedBlock(const Block & block, bool check_limits = true) = 0;

    /// Join the block with data from left hand of JOIN to the right hand data (that was previously built by calls to addJoinedBlock).
    /// Could be called from different threads in parallel.
    virtual void joinBlock(Block & block, std::shared_ptr<ExtraBlock> & not_processed) = 0;

    /// Set/Get totals for right table
    virtual void setTotals(const Block & block) = 0;
    virtual const Block & getTotals() const = 0;

    virtual size_t getTotalRowCount() const = 0;
    virtual size_t getTotalByteCount() const = 0;
    virtual bool alwaysReturnsEmptySet() const { return false; }

    /// StorageJoin/Dictionary is already filled. No need to call addJoinedBlock.
    /// Different query plan is used for such joins.
    virtual bool isFilled() const { return false; }

    virtual BlockInputStreamPtr createStreamWithNonJoinedRows(const Block &, UInt64) const { return {}; }

    virtual void serialize(WriteBuffer &) const { throw Exception("Not implement join serialize", ErrorCodes::NOT_IMPLEMENTED); }
    static JoinPtr deserialize(ReadBuffer &, ContextPtr) { throw Exception("Not implement join deserialize", ErrorCodes::NOT_IMPLEMENTED); }
};

}
