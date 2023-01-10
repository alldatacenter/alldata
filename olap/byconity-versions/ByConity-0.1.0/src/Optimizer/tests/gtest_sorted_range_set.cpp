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
#include <gtest/gtest.h>
#include "Common/FieldVisitors.h"
#include "Common/Stopwatch.h"
#include "Core/Field.h"
#include "DataTypes/DataTypeFactory.h"
#include "Optimizer/value_sets.h"
using namespace DB;
using namespace DB::Predicate;



class SortedRangeSetTest : public testing::Test
{
public:
    auto typeFromString(const std::string & str)
    {
        auto & data_type_factory = DataTypeFactory::instance();
        return data_type_factory.get(str);
    }

protected:
    void SetUp() {}

    void TearDown() {}

    SortedRangeSetTest()
    {
        type_int64 = typeFromString("Int64");
        type_float32 = typeFromString("Float32");
        type_float64 = typeFromString("Float64");
        type_string = typeFromString("String");
        field_0 = Int64(0);
        field_1 = Int64(1);
        field_2 = Int64(2);
        field_3 = Int64(3);
        field_4 = Int64(4);
        field_5 = Int64(5);
        field_6 = Int64(6);
        field_7 = Int64(7);
        field_8 = Int64(8);
        field_9 = Int64(9);
        field_10 = Int64(10);
        field_11 = Int64(11);
        field_40 = Int64(40);
        field_41 = Int64(41);
    }

    ~SortedRangeSetTest()
    {

    }
    DataTypePtr type_int64;
    DataTypePtr type_float32;
    DataTypePtr type_float64;
    DataTypePtr type_string;
    Field field_0;
    Field field_1;
    Field field_2;
    Field field_3;
    Field field_4;
    Field field_5;
    Field field_6;
    Field field_7;
    Field field_8;
    Field field_9;
    Field field_10;
    Field field_11;
    Field field_40;
    Field field_41;
};

TEST_F(SortedRangeSetTest, testEmptySet)
{
    SortedRangeSet range_set = SortedRangeSet::createNone(type_int64);
    EXPECT_EQ(range_set.getType()->getTypeId(), TypeIndex::Int64);
    ASSERT_TRUE(range_set.isNone());
    ASSERT_FALSE(range_set.isAll());
    ASSERT_FALSE(range_set.isSingleValue());
    ASSERT_TRUE(range_set.getRanges().empty());
    EXPECT_EQ(range_set.getRangesCount(), 0);
    EXPECT_EQ(range_set.complement(), SortedRangeSet::createAll(type_int64));
    ASSERT_FALSE(range_set.containsValue(Field(field_0)));
    //EXPECT_EQ(range_set.toString(), "SortedRangeSet[type=bigint, ranges=0, {}]"); //TODO:
}

TEST_F(SortedRangeSetTest, testEntireSet)
{
    SortedRangeSet range_set = SortedRangeSet::createAll(type_int64);
    EXPECT_EQ(range_set.getType()->getTypeId(), TypeIndex::Int64);
    ASSERT_FALSE(range_set.isNone());
    ASSERT_TRUE(range_set.isAll());
    ASSERT_FALSE(range_set.isSingleValue());
    EXPECT_EQ(range_set.getRangesCount(), 1);
    EXPECT_EQ(range_set.complement(), SortedRangeSet::createNone(type_int64));
    ASSERT_TRUE(range_set.containsValue(field_0));
    //EXPECT_EQ(range_set.toString(), "SortedRangeSet[type=bigint, ranges=1, {(<min>,<max>)}]");
}

TEST_F(SortedRangeSetTest, testSingleValue)
{

    SortedRangeSet range_set = SortedRangeSet::createFromUnsortedValues(type_int64, {field_10});

    SortedRangeSet complement = SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_10), Range::lessThanRange(type_int64, field_10)});

    EXPECT_EQ(range_set.getType()->getTypeId(), TypeIndex::Int64);
    ASSERT_FALSE(range_set.isNone());
    ASSERT_FALSE(range_set.isAll());
    ASSERT_TRUE(range_set.isSingleValue());
    EXPECT_EQ(range_set.getRanges(), Ranges{Range::equalRange(type_int64, field_10)});
    EXPECT_EQ(range_set.getRangesCount(), 1);
    EXPECT_EQ(range_set.complement(), complement);
    ASSERT_TRUE(range_set.containsValue(field_10));
    ASSERT_FALSE(range_set.containsValue(field_9));
    //    EXPECT_EQ(range_set.toString(), "SortedRangeSet[type=bigint, ranges=1, {[10]}]");
    //    EXPECT_EQ(
    //        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(VARCHAR, utf8Slice("LARGE PLATED NICKEL"))).toString(),
    //        "SortedRangeSet[type=varchar, ranges=1, {[LARGE PLATED NICKEL]}]");
}

TEST_F(SortedRangeSetTest, testBoundedSet)
{
    SortedRangeSet range_set = SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_10),
                                                                         Range::equalRange(type_int64, field_0),
                                                                         Range(type_int64, true, field_9, false, field_11),
                                                                         Range::equalRange(type_int64, field_0),
                                                                         Range(type_int64, true, field_2, true, field_4),
                                                                         Range(type_int64, false, field_4, true, field_5)});

    Ranges normalized_result = {Range::equalRange(type_int64, field_0),
                                Range(type_int64, true, field_2, true, field_5),
                                Range(type_int64, true, field_9, false, field_11)};

    SortedRangeSet complement = SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_0),
                                                                          Range(type_int64, false, field_0, false, field_2),
                                                                          Range(type_int64, false, field_5, false, field_9),
                                                                          Range::greaterThanOrEqualRange(type_int64, field_11)});

    EXPECT_EQ(range_set.getType(), type_int64);
    ASSERT_FALSE(range_set.isNone());
    ASSERT_FALSE(range_set.isAll());
    ASSERT_FALSE(range_set.isSingleValue());
    EXPECT_EQ(range_set.getRanges(), normalized_result);
    EXPECT_EQ(range_set.getRangesCount(), 3);
    EXPECT_EQ(range_set.complement(), complement);
    ASSERT_TRUE(range_set.containsValue(field_0));
    ASSERT_FALSE(range_set.containsValue(field_1));
    ASSERT_FALSE(range_set.containsValue(field_7));
    ASSERT_TRUE(range_set.containsValue(field_9));
    /*
    EXPECT_EQ(range_set.toString(), "SortedRangeSet[type=bigint, ranges=3, {[0], [2,5], [9,11)}]");
    EXPECT_EQ(
        range_set.toString(ToStringSession.INSTANCE, 2),
        "SortedRangeSet[type=bigint, ranges=3, {[0], ..., [9,11)}]");
    EXPECT_EQ(
        range_set.toString(ToStringSession.INSTANCE, 1),
        "SortedRangeSet[type=bigint, ranges=3, {[0], ...}]");*/
}


TEST_F(SortedRangeSetTest, testUnboundedSet)
{
    SortedRangeSet range_set = SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_10),
                                                                         Range::lessThanOrEqualRange(type_int64, field_0),
                                                                         Range(type_int64, true, field_2, false, field_4),
                                                                         Range(type_int64, true, field_4, false, field_6),
                                                                         Range(type_int64, false, field_1, false, field_2),
                                                                         Range(type_int64, false, field_9, false, field_11)});

    Ranges normalized_result = {Range::lessThanOrEqualRange(type_int64, field_0),
                                Range(type_int64, false, field_1, false, field_6),
                                Range::greaterThanRange(type_int64, field_9)};


    SortedRangeSet complement =  SortedRangeSet::createFromUnsortedRanges({Range(type_int64, false, field_0, true, field_1),
                                                                          Range(type_int64, true, field_6, true, field_9)});

    EXPECT_EQ(range_set.getType(), type_int64);
    ASSERT_FALSE(range_set.isNone());
    ASSERT_FALSE(range_set.isAll());
    ASSERT_FALSE(range_set.isSingleValue());
    EXPECT_EQ(range_set.getRanges(), normalized_result);
    EXPECT_EQ(range_set.getRangesCount(), 3);
    EXPECT_EQ(range_set.complement(), complement);
    ASSERT_TRUE(range_set.containsValue(field_0));
    ASSERT_TRUE(range_set.containsValue(field_4));
    ASSERT_FALSE(range_set.containsValue(field_7));
    //EXPECT_EQ(range_set.toString(), "SortedRangeSet[type=bigint, ranges=3, {(<min>,0], (1,6), (9,<max>)}]");
}

TEST_F(SortedRangeSetTest, testCreateWithRanges)
{
    // two low-unbounded, first shorter
    EXPECT_EQ(SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_5), Range::lessThanRange(type_int64, field_10)}).getRanges(),
              Ranges{Range::lessThanRange(type_int64, field_10)});
    EXPECT_EQ(SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_10), Range::lessThanOrEqualRange(type_int64, field_10)}).getRanges(),
              Ranges{Range::lessThanOrEqualRange(type_int64, field_10)});

    // two low-unbounded, second shorter
    EXPECT_EQ(SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_10), Range::lessThanRange(type_int64, field_5)}).getRanges(),
              Ranges{Range::lessThanRange(type_int64, field_10)});
    EXPECT_EQ(SortedRangeSet::createFromUnsortedRanges({Range::lessThanOrEqualRange(type_int64, field_10), Range::lessThanRange(type_int64, field_10)}).getRanges(),
              Ranges{Range::lessThanOrEqualRange(type_int64, field_10)});

    // two high-unbounded, first shorter
    EXPECT_EQ(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_10), Range::greaterThanRange(type_int64, field_5)}).getRanges(),
              Ranges{Range::greaterThanRange(type_int64, field_5)});
    EXPECT_EQ(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_10), Range::greaterThanOrEqualRange(type_int64, field_10)}).getRanges(),
              Ranges{Range::greaterThanOrEqualRange(type_int64, field_10)});

    // two high-unbounded, second shorter
    EXPECT_EQ(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_5), Range::greaterThanRange(type_int64, field_10)}).getRanges(),
              Ranges{Range::greaterThanRange(type_int64, field_5)});
    EXPECT_EQ(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanOrEqualRange(type_int64, field_10), Range::greaterThanRange(type_int64, field_10)}).getRanges(),
              Ranges{Range::greaterThanOrEqualRange(type_int64, field_10)});
}

TEST_F(SortedRangeSetTest, testGetSingleValue)
{
    EXPECT_EQ(SortedRangeSet::createFromUnsortedValues(type_int64, Array{field_0}).getSingleValue(), field_0);
    /*
    assertThatThrownBy(() -> SortedRangeSet::createAll(type_int64).getSingleValue())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("SortedRangeSet does not have just a single value");*/
}

TEST_F(SortedRangeSetTest, testSpan)
{
    EXPECT_THROW(SortedRangeSet::createNone(type_int64).getSpan(), Exception);
    EXPECT_EQ(SortedRangeSet::createAll(type_int64).getSpan(), Range::allRange(type_int64));
    EXPECT_EQ(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).getSpan(), Range::equalRange(type_int64, field_0));
    EXPECT_EQ(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)}).getSpan(), Range(type_int64, true, field_0, true, field_1));
    EXPECT_EQ(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::greaterThanRange(type_int64, field_1)}).getSpan(), Range::greaterThanOrEqualRange(type_int64, field_0));
    EXPECT_EQ(SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_0), Range::greaterThanRange(type_int64, field_1)}).getSpan(), Range::allRange(type_int64));
}

TEST_F(SortedRangeSetTest, testOverlaps)
{
    ASSERT_TRUE(SortedRangeSet::createAll(type_int64).overlaps(SortedRangeSet::createAll(type_int64)));
    ASSERT_FALSE(SortedRangeSet::createAll(type_int64).overlaps(SortedRangeSet::createNone(type_int64)));
    ASSERT_TRUE(SortedRangeSet::createAll(type_int64).overlaps(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0})));
    ASSERT_TRUE(SortedRangeSet::createAll(type_int64).overlaps(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)})));
    ASSERT_TRUE(SortedRangeSet::createAll(type_int64).overlaps(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)})));
    ASSERT_TRUE(SortedRangeSet::createAll(type_int64).overlaps(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0), Range::lessThanRange(type_int64, field_0)})));

    ASSERT_FALSE(SortedRangeSet::createNone(type_int64).overlaps(SortedRangeSet::createAll(type_int64)));
    ASSERT_FALSE(SortedRangeSet::createNone(type_int64).overlaps(SortedRangeSet::createNone(type_int64)));
    ASSERT_FALSE(SortedRangeSet::createNone(type_int64).overlaps(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0})));
    ASSERT_FALSE(SortedRangeSet::createNone(type_int64).overlaps(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)})));
    ASSERT_FALSE(SortedRangeSet::createNone(type_int64).overlaps(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)})));
    ASSERT_FALSE(SortedRangeSet::createNone(type_int64).overlaps(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0), Range::lessThanRange(type_int64, field_0)})));

    ASSERT_TRUE(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).overlaps(SortedRangeSet::createAll(type_int64)));
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).overlaps(SortedRangeSet::createNone(type_int64)));
    ASSERT_TRUE(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).overlaps(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0})));
    ASSERT_TRUE(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).overlaps(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)})));
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).overlaps(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)})));
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).overlaps(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0), Range::lessThanRange(type_int64, field_0)})));

    ASSERT_TRUE(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)}).overlaps(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_1)})));
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)}).overlaps(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_2)})));
    ASSERT_TRUE(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanOrEqualRange(type_int64, field_0)}).overlaps(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)})));
    ASSERT_TRUE(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)}).overlaps(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanOrEqualRange(type_int64, field_0)})));
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_0)}).overlaps(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)})));
}

TEST_F(SortedRangeSetTest, testContains)
{
    ASSERT_TRUE(SortedRangeSet::createAll(type_int64).contains(SortedRangeSet::createAll(type_int64)));
    ASSERT_TRUE(SortedRangeSet::createAll(type_int64).contains(SortedRangeSet::createNone(type_int64)));
    ASSERT_TRUE(SortedRangeSet::createAll(type_int64).contains(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0})));
    ASSERT_TRUE(SortedRangeSet::createAll(type_int64).contains(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)})));
    ASSERT_TRUE(SortedRangeSet::createAll(type_int64).contains(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)})));
    ASSERT_TRUE(SortedRangeSet::createAll(type_int64).contains(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0), Range::lessThanRange(type_int64, field_0)})));

    ASSERT_FALSE(SortedRangeSet::createNone(type_int64).contains(SortedRangeSet::createAll(type_int64)));
    ASSERT_TRUE(SortedRangeSet::createNone(type_int64).contains(SortedRangeSet::createNone(type_int64)));
    ASSERT_FALSE(SortedRangeSet::createNone(type_int64).contains(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0})));
    ASSERT_FALSE(SortedRangeSet::createNone(type_int64).contains(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)})));
    ASSERT_FALSE(SortedRangeSet::createNone(type_int64).contains(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)})));
    ASSERT_FALSE(SortedRangeSet::createNone(type_int64).contains(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0), Range::lessThanRange(type_int64, field_0)})));

    ValueSet value_set = SortedRangeSet::createFromUnsortedValues(type_int64, {field_0});
    EXPECT_NO_THROW(ASSERT_TRUE(std::get<SortedRangeSet>(value_set).contains(std::get<SortedRangeSet>(value_set))));

    ASSERT_FALSE(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).contains(SortedRangeSet::createAll(type_int64)));
    ASSERT_TRUE(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).contains(SortedRangeSet::createNone(type_int64)));
    ASSERT_TRUE(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).contains(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0})));
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).contains(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)})));
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).contains(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)})));
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).contains(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0), Range::lessThanRange(type_int64, field_0)})));

    ASSERT_TRUE(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)}).contains(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_1)})));
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)}).contains(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_1), Range::equalRange(type_int64, field_2)})));
    ASSERT_TRUE(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanOrEqualRange(type_int64, field_0)}).contains(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)})));
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)}).contains(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanOrEqualRange(type_int64, field_0)})));
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_0)}).contains(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)})));

    Range range_a(type_int64, true, field_0, true, field_2);
    Range range_b(type_int64, true, field_4, true, field_6);
    Range range_c(type_int64, true, field_8, true, field_10);
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedRanges({range_a, range_b}).contains(SortedRangeSet::createFromUnsortedRanges({range_c})));
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedRanges({range_b, range_c}).contains(SortedRangeSet::createFromUnsortedRanges({range_a})));
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedRanges({range_a, range_c}).contains(SortedRangeSet::createFromUnsortedRanges({range_b})));
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedRanges({range_a, range_b}).contains(SortedRangeSet::createFromUnsortedRanges({range_b, range_c})));
    ASSERT_TRUE(SortedRangeSet::createFromUnsortedRanges({range_a, range_b, range_c}).contains(SortedRangeSet::createFromUnsortedRanges({range_a})));
    ASSERT_TRUE(SortedRangeSet::createFromUnsortedRanges({range_a, range_b, range_c}).contains(SortedRangeSet::createFromUnsortedRanges({range_b})));
    ASSERT_TRUE(SortedRangeSet::createFromUnsortedRanges({range_a, range_b, range_c}).contains(SortedRangeSet::createFromUnsortedRanges({range_c})));
    ASSERT_TRUE(SortedRangeSet::createFromUnsortedRanges({range_a, range_b, range_c}).contains(
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_4), Range::equalRange(type_int64, field_6), Range::equalRange(type_int64, field_9)})));
    ASSERT_FALSE(SortedRangeSet::createFromUnsortedRanges({range_a, range_b, range_c}).contains(
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_1), Range(type_int64, true, field_6, true, field_10)})));
}

TEST_F(SortedRangeSetTest, testContainsValue)
{
    // type_int64 all
    SortedRangeSet value_set_1 = SortedRangeSet::createAll(type_int64);
    ASSERT_TRUE(value_set_1.containsValue(type_int64->getRange().value().min));
    ASSERT_TRUE(value_set_1.containsValue(field_0));
    ASSERT_TRUE(value_set_1.containsValue(Int64(42)));
    ASSERT_TRUE(value_set_1.containsValue(type_int64->getRange().value().max));

    // type_int64 ranges
    SortedRangeSet value_set_2 = SortedRangeSet::createFromUnsortedRanges({Range(type_int64, true, field_10, true, Int64(41))});
    ASSERT_FALSE(value_set_2.containsValue(field_9));
    ASSERT_TRUE(value_set_2.containsValue(field_10));
    ASSERT_TRUE(value_set_2.containsValue(field_11));
    ASSERT_TRUE(value_set_2.containsValue(field_10));
    ASSERT_TRUE(value_set_2.containsValue(Int64(30)));
    ASSERT_TRUE(value_set_2.containsValue(Int64(41)));
    ASSERT_FALSE(value_set_2.containsValue(Int64(42)));

    SortedRangeSet value_set_3 = SortedRangeSet::createFromUnsortedRanges({Range(type_int64, false, field_10, false, Int64(41))});
    ASSERT_FALSE(value_set_3.containsValue(field_10));
    ASSERT_TRUE(value_set_3.containsValue(field_11));
    ASSERT_TRUE(value_set_3.containsValue(field_40));
    ASSERT_FALSE(value_set_3.containsValue(Int64(41)));

    //TODO:test float32
    SortedRangeSet value_set_4 = SortedRangeSet::createAll(type_float32);
    ASSERT_TRUE(value_set_4.containsValue(Field(Float32(42.0))));
    ASSERT_TRUE(value_set_4.containsValue(std::sqrt(-1.0)));

    SortedRangeSet value_set_5 = SortedRangeSet::createFromUnsortedRanges({Range(type_float32, true, Float32(10.0), true, Float32(41.0))});
    ASSERT_FALSE(value_set_5.containsValue(Float32(9.99999)));
    ASSERT_TRUE(value_set_5.containsValue(Float32(10.0)));
    ASSERT_TRUE(value_set_5.containsValue(Float32(41.0)));
    ASSERT_FALSE(value_set_5.containsValue(Float32(41.00001)));
    ASSERT_FALSE(value_set_5.containsValue(Float32(std::sqrt(-1.0))));

    SortedRangeSet value_set_6 = SortedRangeSet::createFromUnsortedRanges({Range(type_float32, false, Float32(10.0), false, Float32(41.0))});
    ASSERT_FALSE(value_set_6.containsValue(Float32(10.0)));
    ASSERT_TRUE(value_set_6.containsValue(Float32(10.00001)));
    ASSERT_TRUE(value_set_6.containsValue(Float32(40.99999)));
    ASSERT_FALSE(value_set_6.containsValue(Float32(41.0)));
    ASSERT_FALSE(value_set_6.containsValue(Float32(std::sqrt(-1.0))));

    // Float64 all
    SortedRangeSet value_set_7 = SortedRangeSet::createAll(type_float64);
    ASSERT_TRUE(value_set_7.containsValue(Float64(42.0)));
    ASSERT_TRUE(value_set_7.containsValue(std::sqrt(-1.0)));

    // Float64 range
    SortedRangeSet value_set_8 = SortedRangeSet::createFromUnsortedRanges({Range(type_float64, true, Float64(10.0), true, Float64(41.0))});
    ASSERT_FALSE(value_set_8.containsValue(Float64(9.999999999999999)));
    ASSERT_TRUE(value_set_8.containsValue(Float64(10.0)));
    ASSERT_TRUE(value_set_8.containsValue(Float64(41.0)));
    ASSERT_FALSE(value_set_8.containsValue(Float64(41.00000000000001)));
    ASSERT_FALSE(value_set_8.containsValue(std::sqrt(-1.0)));

    SortedRangeSet value_set_9 = SortedRangeSet::createFromUnsortedRanges({Range(type_float64, false, Float64(10.0), false, Float64(41.0))});
    ASSERT_FALSE(value_set_9.containsValue(Float64(10.0)));
    ASSERT_TRUE(value_set_9.containsValue(Float64(10.00000000000001)));
    ASSERT_TRUE(value_set_9.containsValue(Float64(40.99999999999999)));
    ASSERT_FALSE(value_set_9.containsValue(Float64(41.0)));
    ASSERT_FALSE(value_set_9.containsValue(std::sqrt(-1.0)));

}

TEST_F(SortedRangeSetTest, testContainsValueRejectNull)
{
    SortedRangeSet all_value_set = SortedRangeSet::createAll(type_int64);
    SortedRangeSet none_value_set = SortedRangeSet::createNone(type_int64);
    SortedRangeSet range_value_set = SortedRangeSet::createFromUnsortedRanges({Range(type_int64, false, field_10, false, field_41)});
//    ASSERT_THROW(all_value_set.containsValue(Field()), Exception);
//    ASSERT_THROW(none_value_set.containsValue(Field()), Exception);
//    ASSERT_THROW(range_value_set.containsValue(Field()), Exception);
}

TEST_F(SortedRangeSetTest, testIntersect)
{
    ASSERT_EQ(
        SortedRangeSet::createNone(type_int64).intersect(
            SortedRangeSet::createNone(type_int64)),
        SortedRangeSet::createNone(type_int64));

    ASSERT_EQ(
        SortedRangeSet::createAll(type_int64).intersect(
            SortedRangeSet::createAll(type_int64)),
        SortedRangeSet::createAll(type_int64));

    ASSERT_EQ(
        SortedRangeSet::createNone(type_int64).intersect(
            SortedRangeSet::createAll(type_int64)),
        SortedRangeSet::createNone(type_int64));

    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_1),Range::equalRange(type_int64, field_2),Range::equalRange(type_int64, field_3)}).intersect(
            SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_2),Range::equalRange(type_int64, field_4)})),
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_2)}));

    ASSERT_EQ(
        SortedRangeSet::createAll(type_int64).intersect(
            SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_2), Range::equalRange(type_int64, field_4)})),
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_2), Range::equalRange(type_int64, field_4)}));

    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range(type_int64, true, field_0, false, field_4)}).intersect(
            SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_2), Range::greaterThanRange(type_int64, field_3)})),
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_2), Range(type_int64, false, field_3, false, field_4)}));

    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanOrEqualRange(type_int64, field_0)}).intersect(
            SortedRangeSet::createFromUnsortedRanges({Range::lessThanOrEqualRange(type_int64, field_0)})),
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0)}));

    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanOrEqualRange(type_int64, Int64(-1))}).intersect(
            SortedRangeSet::createFromUnsortedRanges({Range::lessThanOrEqualRange(type_int64, field_1)})),
        SortedRangeSet::createFromUnsortedRanges({Range(type_int64, true, Int64(-1), true, field_1)}));
}

TEST_F(SortedRangeSetTest, testUnion)
{
    ASSERT_EQ(SortedRangeSet::createNone(type_int64).unionn(SortedRangeSet::createNone(type_int64)), SortedRangeSet::createNone(type_int64));
    ASSERT_EQ(SortedRangeSet::createAll(type_int64).unionn(SortedRangeSet::createAll(type_int64)), SortedRangeSet::createAll(type_int64));
    ASSERT_EQ(SortedRangeSet::createNone(type_int64).unionn(SortedRangeSet::createAll(type_int64)), SortedRangeSet::createAll(type_int64));

    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_1), Range::equalRange(type_int64, field_2)}).unionn(
            SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_2), Range::equalRange(type_int64, field_3)})),
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_1), Range::equalRange(type_int64, field_2), Range::equalRange(type_int64, field_3)}));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_1), Range::equalRange(type_int64, field_2)}).unionn(
            SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_2), Range::equalRange(type_int64, field_3)})),
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_1), Range::equalRange(type_int64, field_2), Range::equalRange(type_int64, field_3)}));

    ASSERT_EQ(SortedRangeSet::createAll(type_int64).unionn(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0)})),
              SortedRangeSet::createAll(type_int64));

    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range(type_int64, true, field_0, false, field_4)}).unionn(
            SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_3)})),
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanOrEqualRange(type_int64, field_0)}));

    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanOrEqualRange(type_int64, field_0)}).unionn(
            SortedRangeSet::createFromUnsortedRanges({Range::lessThanOrEqualRange(type_int64, field_0)})),
        SortedRangeSet::createFromUnsortedRanges({Range::allRange(type_int64)}));

    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)}).unionn(
            SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_0)})),
        SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).complement());

    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range(type_int64, true, field_0, false, field_10)}).unionn(
            SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_9)})),
        SortedRangeSet::createFromUnsortedRanges({Range(type_int64, true, field_0, false, field_10)}));

    // two low-unbounded, first shorter
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_5)}).unionn(
            SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_10)})),
        SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_10)}));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_10)}).unionn(
            SortedRangeSet::createFromUnsortedRanges({Range::lessThanOrEqualRange(type_int64, field_10)})),
        SortedRangeSet::createFromUnsortedRanges({Range::lessThanOrEqualRange(type_int64, field_10)}));

    // two low-unbounded, second shorter
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_10)}).unionn(
            SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_5)})),
        SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_10)}));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::lessThanOrEqualRange(type_int64, field_10)}).unionn(
            SortedRangeSet::createFromUnsortedRanges({Range::lessThanRange(type_int64, field_10)})),
        SortedRangeSet::createFromUnsortedRanges({Range::lessThanOrEqualRange(type_int64, field_10)}));

    // two high-unbounded, first shorter
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_10)}).unionn(
            SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_5)})),
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_5)}));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_10)}).unionn(
            SortedRangeSet::createFromUnsortedRanges({Range::greaterThanOrEqualRange(type_int64, field_10)})),
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanOrEqualRange(type_int64, field_10)}));

    // two high-unbounded, second shorter
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_5)}).unionn(
            SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_10)})),
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_5)}));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanOrEqualRange(type_int64, field_10)}).unionn(
            SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_10)})),
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanOrEqualRange(type_int64, field_10)}));

    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range(type_string, true, Field("LARGE PLATED "), false, Field("LARGE PLATED!"))}).unionn(
            SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_string, Field("LARGE PLATED NICKEL"))})),
        SortedRangeSet::createFromUnsortedRanges({Range(type_string, true, Field("LARGE PLATED "), false, Field("LARGE PLATED!"))}));
}

TEST_F(SortedRangeSetTest, testSubtract)
{
    ASSERT_EQ(
        SortedRangeSet::createAll(type_int64).subtract(SortedRangeSet::createAll(type_int64)),
        SortedRangeSet::createNone(type_int64));
    ASSERT_EQ(
        SortedRangeSet::createAll(type_int64).subtract(SortedRangeSet::createNone(type_int64)),
        SortedRangeSet::createAll(type_int64));
    ASSERT_EQ(
        SortedRangeSet::createAll(type_int64).subtract(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0})),
        SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).complement());
    ASSERT_EQ(
        SortedRangeSet::createAll(type_int64).subtract(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)})),
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)}).complement());
    ASSERT_EQ(
        SortedRangeSet::createAll(type_int64).subtract(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)})),
        SortedRangeSet::createFromUnsortedRanges({Range::lessThanOrEqualRange(type_int64, field_0)}));

    ASSERT_EQ(
        SortedRangeSet::createNone(type_int64).subtract(SortedRangeSet::createAll(type_int64)),
        SortedRangeSet::createNone(type_int64));
    ASSERT_EQ(
        SortedRangeSet::createNone(type_int64).subtract(SortedRangeSet::createNone(type_int64)),
        SortedRangeSet::createNone(type_int64));
    ASSERT_EQ(
        SortedRangeSet::createNone(type_int64).subtract(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0})),
        SortedRangeSet::createNone(type_int64));
    ASSERT_EQ(
        SortedRangeSet::createNone(type_int64).subtract(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)})),
        SortedRangeSet::createNone(type_int64));
    ASSERT_EQ(
        SortedRangeSet::createNone(type_int64).subtract(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)})),
        SortedRangeSet::createNone(type_int64));

    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).subtract(SortedRangeSet::createAll(type_int64)),
        SortedRangeSet::createNone(type_int64));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).subtract(SortedRangeSet::createNone(type_int64)),
        SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).subtract(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0})),
        SortedRangeSet::createNone(type_int64));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).subtract(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)})),
        SortedRangeSet::createNone(type_int64));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}).subtract(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)})),
        SortedRangeSet::createFromUnsortedValues(type_int64, {field_0}));

    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)}).subtract(SortedRangeSet::createAll(type_int64)),
        SortedRangeSet::createNone(type_int64));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)}).subtract(SortedRangeSet::createNone(type_int64)),
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)}));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)}).subtract(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0})),
        SortedRangeSet::createFromUnsortedValues(type_int64, {field_1}));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)}).subtract(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)})),
        SortedRangeSet::createNone(type_int64));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)}).subtract(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)})),
        SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0)}));

    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, {field_0})}).subtract(SortedRangeSet::createAll(type_int64)),
        SortedRangeSet::createNone(type_int64));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, {field_0})}).subtract(SortedRangeSet::createNone(type_int64)),
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)}));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)}).subtract(SortedRangeSet::createFromUnsortedValues(type_int64, {field_0})),
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)}));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)}).subtract(SortedRangeSet::createFromUnsortedRanges({Range::equalRange(type_int64, field_0), Range::equalRange(type_int64, field_1)})),
        SortedRangeSet::createFromUnsortedRanges({Range(type_int64, false, field_0, false, field_1), Range::greaterThanRange(type_int64, field_1)}));
    ASSERT_EQ(
        SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)}).subtract(SortedRangeSet::createFromUnsortedRanges({Range::greaterThanRange(type_int64, field_0)})),
        SortedRangeSet::createNone(type_int64));
}

//TODO:testJsonSerialization(); testExpandRangesForDenseType

//int main(int argc, char **argv)
//{
//    ::testing::InitGoogleTest(&argc, argv);
//    return RUN_ALL_TESTS();
//}
