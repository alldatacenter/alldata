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

#include <DataStreams/IBlockInputStream.h>


namespace DB
{

/** A stream of blocks from which you can read the next block from an explicitly provided list.
  * Also see OneBlockInputStream.
  */
class BlocksListBlockInputStream : public IBlockInputStream
{
public:
    /// Acquires the ownership of the block list.
    BlocksListBlockInputStream(BlocksList && list_)
        : list(std::move(list_)), it(list.begin()), end(list.end()) {}

    /// Uses a list of blocks lying somewhere else.
    BlocksListBlockInputStream(BlocksList::iterator & begin_, BlocksList::iterator & end_)
        : it(begin_), end(end_) {}

    String getName() const override { return "BlocksList"; }

protected:
    Block getHeader() const override
    {
        if (list.empty())
        {
            return {};
        }
        else
        {
            return list.front().cloneEmpty();
        }
    }

    Block readImpl() override
    {
        if (it == end)
            return Block();

        Block res = *it;
        ++it;
        return res;
    }

private:
    BlocksList list;
    BlocksList::iterator it;
    const BlocksList::iterator end;
};

}
