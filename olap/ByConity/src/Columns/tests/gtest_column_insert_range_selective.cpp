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

#include <iostream>
#include <string>
#include <gtest/gtest.h>

#include <Columns/ColumnAggregateFunction.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnBitMap64.h>
#include <Columns/ColumnByteMap.h>
#include <Columns/ColumnConst.h>
#include <Columns/ColumnDecimal.h>
#include <Columns/ColumnFixedString.h>
#include <Columns/ColumnLowCardinality.h>
#include <Columns/ColumnMap.h>
#include <Columns/ColumnNothing.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnTuple.h>
#include <Columns/ColumnVector.h>
#include <Columns/ColumnsNumber.h>
#include <Columns/IColumn.h>
#include <DataTypes/DataTypeLowCardinality.h>
#include <DataTypes/DataTypeMap.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>


using namespace DB;

namespace UnitTest
{
/// compare InsertRangeSelective with following code :
/// for (size_t i = 0; i < length; i++)
///     insertFrom(src, selector[selector_start + i]);

static void compareInsertRangeSelectiveWithInsertFrom(const IColumn & src_column, bool direct_compare_element = false)
{
    const size_t total_size = src_column.size();
    const size_t length = total_size / 2;
    const size_t start = total_size / 3;
    auto dst_column = src_column.cloneEmpty();
    auto expect_column = src_column.cloneEmpty();
    IColumn::Selector selector(total_size, 0);

    for (size_t i = 0; i < length; i++)
        selector[start + i] = total_size - start - i;

    for (size_t i = 0; i < length; i++)
        expect_column->insertFrom(src_column, selector[start + i]);

    dst_column->insertRangeSelective(src_column, selector, start, length);

    EXPECT_EQ(expect_column->size(), dst_column->size());
    for (size_t i = 0; i < length; i++)
    {
        if (direct_compare_element)
        {
            EXPECT_EQ(true, (*expect_column)[i] == (*dst_column)[i]);
        }
        else
        {
            EXPECT_EQ(0, expect_column->compareAt(i, i, *dst_column, 1));
        }
    }
}


TEST(InsertRangeSelective, ColumnArrayTest)
{
    size_t max_size = 10;
    auto val = ColumnUInt32::create();
    auto off = ColumnUInt64::create();
    auto & val_data = val->getData();
    auto & off_data = off->getData();

    /* [1]
     * [1, 1]
     * [1, 1, 1]
     * ...
     * [2]
     * [2, 2]
     * [2, 2, 2]
     * ...
     */
    UInt64 cur_off = 0;
    for (int idx [[maybe_unused]] : {1, 2})
    {
        UInt32 cur = 0;
        for (int64_t i = 0; i < 64; ++i)
        {
            size_t s = (i % max_size) + 1;

            cur_off += s;
            off_data.push_back(cur_off);

            for (size_t j = 0; j < s; ++j)
                val_data.push_back(cur);

            if (s == max_size)
                ++cur;
        }
    }

    auto src_column = ColumnArray::create(std::move(val), std::move(off));
    compareInsertRangeSelectiveWithInsertFrom(*src_column);
}

TEST(InsertRangeSelective, ColumnBitMapTest)
{
    auto src_column = ColumnBitMap64::create();
    src_column->insertManyDefaults(100);
    compareInsertRangeSelectiveWithInsertFrom(*src_column);
}

TEST(InsertRangeSelective, ColumnConstTest)
{
    ColumnPtr nest_src_column = ColumnUInt64::create(1, 8);
    auto src_column = ColumnConst::create(nest_src_column, 100);
    compareInsertRangeSelectiveWithInsertFrom(*src_column);
}

TEST(InsertRangeSelective, ColumnUInt64Test)
{
    const size_t size = 100;
    auto src_column = ColumnUInt64::create();

    for (size_t i = 0; i < size; i++)
    {
        src_column->insert(i * 10);
    }
    compareInsertRangeSelectiveWithInsertFrom(*src_column);
}

TEST(InsertRangeSelective, ColumnFixedStringTest)
{
    const size_t size = 100;
    auto src_column = ColumnFixedString::create(100);
    for (size_t i = 0; i < size; i++)
    {
        src_column->insert("value: " + std::to_string(i));
    }
    compareInsertRangeSelectiveWithInsertFrom(*src_column);
}

TEST(InsertRangeSelective, ColumnLowcardinalityTest)
{
    MutableColumnPtr src_column = DataTypeLowCardinality(std::make_shared<DataTypeUInt8>()).createColumn();
    for (size_t i = 0; i < 100; ++i)
    {
        src_column->insert(i);
    }
    compareInsertRangeSelectiveWithInsertFrom(*src_column);
}

TEST(InsertRangeSelective, ColumnByteMapTest)
{
    const size_t size = 100;
    auto key_column = ColumnString::create();
    auto value_column = ColumnString::create();
    auto offset_column = ColumnVector<UInt64>::create();

    size_t offest_size = 0;
    for (size_t i = 0; i < size; i++)
    {
        key_column->insert("key: " + std::to_string(i));
        value_column->insert("value: " + std::to_string(i));
        if (i % 2 == 1)
        {
            offest_size += 2;
            offset_column->insert(offest_size);
        }
    }
    MutableColumnPtr src_column = ColumnByteMap::create(std::move(key_column), std::move(value_column), std::move(offset_column));
    compareInsertRangeSelectiveWithInsertFrom(*src_column, true);
}

TEST(InsertRangeSelective, ColumnMapTest)
{
    const size_t size = 100;
    auto col1 = ColumnUInt64::create();
    auto col2 = ColumnUInt64::create();
    auto & data1 = col1->getData();
    auto & data2 = col2->getData();
    auto off = ColumnUInt64::create();
    auto & off_data = off->getData();

    size_t offest_size = 0;
    for (uint64_t i = 0; i < size; ++i)
    {
        offest_size++;
        data1.push_back(i);
        data2.push_back(i * 2);
        off_data.push_back(offest_size);
    }
    auto src_column = ColumnMap::create(std::move(col1), std::move(col2), std::move(off));
    compareInsertRangeSelectiveWithInsertFrom(*src_column, true);
}

TEST(InsertRangeSelective, ColumnStringTest)
{
    const size_t size = 100;
    auto src_column = ColumnString::create();
    for (size_t i = 0; i < size; i++)
    {
        src_column->insert("value: " + std::to_string(i));
    }
    compareInsertRangeSelectiveWithInsertFrom(*src_column);
}

TEST(InsertRangeSelective, ColumnTupleTest)
{
    auto col1 = ColumnUInt64::create();
    auto col2 = ColumnUInt64::create();
    auto & data1 = col1->getData();
    auto & data2 = col2->getData();


    for (uint64_t i = 0; i < 100; ++i)
    {
        data1.push_back(i);
        data2.push_back(i * 2);
    }

    Columns columns;
    columns.emplace_back(std::move(col1));
    columns.emplace_back(std::move(col2));
    auto src_column = ColumnTuple::create(std::move(columns));
    compareInsertRangeSelectiveWithInsertFrom(*src_column);
}

TEST(InsertRangeSelective, ColumnNothingTest)
{
    auto src_column = ColumnNothing::create(100);
    compareInsertRangeSelectiveWithInsertFrom(*src_column);
}

}
