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

#include <Core/Block.h>
#include <gtest/gtest.h>

#include <Columns/ColumnsNumber.h>
#include <DataStreams/BlocksListBlockInputStream.h>
#include <DataStreams/CheckSortedBlockInputStream.h>
#include <DataTypes/DataTypesNumber.h>


using namespace DB;


static SortDescription getSortDescription(const std::vector<std::string> & column_names)
{
    SortDescription descr;
    for (const auto & column : column_names)
    {
        descr.emplace_back(column, 1, 1);
    }
    return descr;
}

static Block getSortedBlockWithSize(
    const std::vector<std::string> & columns,
    size_t rows, size_t stride, size_t start)
{
    ColumnsWithTypeAndName cols;
    size_t size_of_row_in_bytes = columns.size() * sizeof(UInt64);
    for (size_t i = 0; i * sizeof(UInt64) < size_of_row_in_bytes; i++)
    {
        auto column = ColumnUInt64::create(rows, 0);
        for (size_t j = 0; j < rows; ++j)
        {
            column->getElement(j) = start;
            start += stride;
        }
        cols.emplace_back(std::move(column), std::make_shared<DataTypeUInt64>(), columns[i]);
    }
    return Block(cols);
}


static Block getUnSortedBlockWithSize(const std::vector<std::string> & columns, size_t rows, size_t stride, size_t start, size_t bad_row, size_t bad_column, size_t bad_value)
{
    ColumnsWithTypeAndName cols;
    size_t size_of_row_in_bytes = columns.size() * sizeof(UInt64);
    for (size_t i = 0; i * sizeof(UInt64) < size_of_row_in_bytes; i++)
    {
        auto column = ColumnUInt64::create(rows, 0);
        for (size_t j = 0; j < rows; ++j)
        {
            if (bad_row == j && bad_column == i)
                column->getElement(j) = bad_value;
            else if (i < bad_column)
                column->getElement(j) = 0;
            else
                column->getElement(j) = start;

            start += stride;
        }
        cols.emplace_back(std::move(column), std::make_shared<DataTypeUInt64>(), columns[i]);
    }
    return Block(cols);
}

static Block getEqualValuesBlockWithSize(
    const std::vector<std::string> & columns, size_t rows)
{
    ColumnsWithTypeAndName cols;
    size_t size_of_row_in_bytes = columns.size() * sizeof(UInt64);
    for (size_t i = 0; i * sizeof(UInt64) < size_of_row_in_bytes; i++)
    {
        auto column = ColumnUInt64::create(rows, 0);
        for (size_t j = 0; j < rows; ++j)
            column->getElement(j) = 0;

        cols.emplace_back(std::move(column), std::make_shared<DataTypeUInt64>(), columns[i]);
    }
    return Block(cols);
}


TEST(CheckSortedBlockInputStream, CheckGoodCase)
{
    std::vector<std::string> key_columns{"K1", "K2", "K3"};
    auto sort_description = getSortDescription(key_columns);

    BlocksList blocks;
    for (size_t i = 0; i < 3; ++i)
        blocks.push_back(getSortedBlockWithSize(key_columns, 10, 1, i * 10));

    BlockInputStreamPtr stream = std::make_shared<BlocksListBlockInputStream>(std::move(blocks));

    CheckSortedBlockInputStream sorted(stream, sort_description);

    EXPECT_NO_THROW(sorted.read());
    EXPECT_NO_THROW(sorted.read());
    EXPECT_NO_THROW(sorted.read());
    EXPECT_EQ(sorted.read(), Block());
}

TEST(CheckSortedBlockInputStream, CheckBadLastRow)
{
    std::vector<std::string> key_columns{"K1", "K2", "K3"};
    auto sort_description = getSortDescription(key_columns);
    BlocksList blocks;
    blocks.push_back(getSortedBlockWithSize(key_columns, 100, 1, 100));
    blocks.push_back(getSortedBlockWithSize(key_columns, 100, 1, 200));
    blocks.push_back(getSortedBlockWithSize(key_columns, 100, 1, 0));
    blocks.push_back(getSortedBlockWithSize(key_columns, 100, 1, 300));

    BlockInputStreamPtr stream = std::make_shared<BlocksListBlockInputStream>(std::move(blocks));

    CheckSortedBlockInputStream sorted(stream, sort_description);

    EXPECT_NO_THROW(sorted.read());
    EXPECT_NO_THROW(sorted.read());

#ifndef ABORT_ON_LOGICAL_ERROR
    EXPECT_THROW(sorted.read(), DB::Exception);
#endif
}


TEST(CheckSortedBlockInputStream, CheckUnsortedBlock1)
{
    std::vector<std::string> key_columns{"K1", "K2", "K3"};
    auto sort_description = getSortDescription(key_columns);
    BlocksList blocks;
    blocks.push_back(getUnSortedBlockWithSize(key_columns, 100, 1, 0, 5, 1, 77));

    BlockInputStreamPtr stream = std::make_shared<BlocksListBlockInputStream>(std::move(blocks));

    CheckSortedBlockInputStream sorted(stream, sort_description);

#ifndef ABORT_ON_LOGICAL_ERROR
    EXPECT_THROW(sorted.read(), DB::Exception);
#endif
}

TEST(CheckSortedBlockInputStream, CheckUnsortedBlock2)
{
    std::vector<std::string> key_columns{"K1", "K2", "K3"};
    auto sort_description = getSortDescription(key_columns);
    BlocksList blocks;
    blocks.push_back(getUnSortedBlockWithSize(key_columns, 100, 1, 0, 99, 2, 77));

    BlockInputStreamPtr stream = std::make_shared<BlocksListBlockInputStream>(std::move(blocks));

    CheckSortedBlockInputStream sorted(stream, sort_description);

#ifndef ABORT_ON_LOGICAL_ERROR
    EXPECT_THROW(sorted.read(), DB::Exception);
#endif
}

TEST(CheckSortedBlockInputStream, CheckUnsortedBlock3)
{
    std::vector<std::string> key_columns{"K1", "K2", "K3"};
    auto sort_description = getSortDescription(key_columns);
    BlocksList blocks;
    blocks.push_back(getUnSortedBlockWithSize(key_columns, 100, 1, 0, 50, 0, 77));

    BlockInputStreamPtr stream = std::make_shared<BlocksListBlockInputStream>(std::move(blocks));

    CheckSortedBlockInputStream sorted(stream, sort_description);

#ifndef ABORT_ON_LOGICAL_ERROR
    EXPECT_THROW(sorted.read(), DB::Exception);
#endif
}

TEST(CheckSortedBlockInputStream, CheckEqualBlock)
{
    std::vector<std::string> key_columns{"K1", "K2", "K3"};
    auto sort_description = getSortDescription(key_columns);
    BlocksList blocks;
    blocks.push_back(getEqualValuesBlockWithSize(key_columns, 100));
    blocks.push_back(getEqualValuesBlockWithSize(key_columns, 10));
    blocks.push_back(getEqualValuesBlockWithSize(key_columns, 1));

    BlockInputStreamPtr stream = std::make_shared<BlocksListBlockInputStream>(std::move(blocks));

    CheckSortedBlockInputStream sorted(stream, sort_description);

    EXPECT_NO_THROW(sorted.read());
    EXPECT_NO_THROW(sorted.read());
    EXPECT_NO_THROW(sorted.read());
}
