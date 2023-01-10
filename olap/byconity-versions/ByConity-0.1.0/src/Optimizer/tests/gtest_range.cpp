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

#include <Optimizer/value_sets.h>
#include <iostream>
#include <DataTypes/DataTypeFactory.h>
#include <Core/Field.h>
#include <Common/FieldVisitors.h>
#include <Common/Stopwatch.h>
#include <gtest/gtest.h>

using namespace DB;
using namespace DB::Predicate;


class RangeTest : public testing::Test
{
public:
    auto typeFromString(const std::string & str)
    {
        auto & data_type_factory = DataTypeFactory::instance();
        return data_type_factory.get(str);
    }

protected:
    RangeTest()
    {
        type_int64 = typeFromString("Int64");
    }

    ~RangeTest() {}
    DataTypePtr type_int64;
    Field field_0 = Int64(0);
    Field field_1 = Int64(1);
    Field field_2 = Int64(2);
    Field field_3 = Int64(3);
    Field field_10 = Int64(10);

};

TEST_F(RangeTest, testInvertedBounds)
{
//    EXPECT_ANY_THROW(Range(typeFromString("String"), true, String("ab"), true, String("a")));
//
//    EXPECT_ANY_THROW(Range(typeFromString("Int64"), true, Field(Int64(1)), true, Field(Int64(0))));
}

TEST_F(RangeTest, testSingleValueExclusive)
{
//    // (10, 10]
//    EXPECT_ANY_THROW(Range(typeFromString("Int64"), false, Field(Int64(10)), true, Field(Int64(10))));
//
//    // [10, 10)
//    EXPECT_ANY_THROW(Range(typeFromString("Int64"), true, Field(Int64(10)), false, Field(Int64(10))));
//
//    // (10, 10)
//    EXPECT_ANY_THROW(Predicate::Range(typeFromString("Int64"), false, Field(Int64(10)), false, Field(Int64(10))));
}

TEST_F(RangeTest, testSingleValue)
{
    ASSERT_TRUE(Range(typeFromString("Int64"), true, Field(Int64(1)), true, Field(Int64(1))).isSingleValue());
    ASSERT_FALSE(Range(typeFromString("Int64"), true, Field(Int64(1)), true, Field(Int64(2))).isSingleValue());
    ASSERT_TRUE(Range(typeFromString("Float64"), true, Field(Float64(1.1)), true, Field(Float64(1.1))).isSingleValue());
    ASSERT_FALSE(Range(typeFromString("String"), true, Field(String("a")), true, Field(String("ab"))).isSingleValue());
}

TEST_F(RangeTest, testAllRange)
{
    Range range = Range::allRange(typeFromString("Int64"));

    ASSERT_TRUE(range.isLowUnbounded());
    ASSERT_FALSE(range.isLowInclusive());
    ASSERT_TRUE(range.getLowValue().isNull());

    ASSERT_TRUE(range.isHighUnbounded());
    ASSERT_FALSE(range.isHighInclusive());
    ASSERT_TRUE(range.getHighValue().isNull());

    ASSERT_FALSE(range.isSingleValue());
    ASSERT_TRUE(range.isAll());
    ASSERT_TRUE(range.getType()->getTypeId() == typeFromString("Int64")->getTypeId());
}

TEST_F(RangeTest, testGreaterThanRange)
{
    Range range = Range::greaterThanRange(typeFromString("Int64"), Field(Int64(1)));

    ASSERT_FALSE(range.isLowUnbounded());
    ASSERT_FALSE(range.isLowInclusive());
    ASSERT_TRUE(range.getLowValue() == Field(Int64(1)));

    ASSERT_TRUE(range.isHighUnbounded());
    ASSERT_FALSE(range.isHighInclusive());
    ASSERT_TRUE(range.getHighValue().isNull());

    ASSERT_FALSE(range.isSingleValue());
    ASSERT_FALSE(range.isAll());
    ASSERT_TRUE(range.getType()->getTypeId() == typeFromString("Int64")->getTypeId());
}

TEST_F(RangeTest, testGreaterThanOrEqualRange)
{
    Range range = Range::greaterThanOrEqualRange(typeFromString("Int64"), Field(Int64(1)));

    ASSERT_FALSE(range.isLowUnbounded());
    ASSERT_TRUE(range.isLowInclusive());
    ASSERT_TRUE(range.getLowValue() == Field(Int64(1)));

    ASSERT_TRUE(range.isHighUnbounded());
    ASSERT_FALSE(range.isHighInclusive());
    ASSERT_TRUE(range.getHighValue().isNull());

    ASSERT_FALSE(range.isSingleValue());
    ASSERT_FALSE(range.isAll());
    ASSERT_TRUE(range.getType()->getTypeId() == typeFromString("Int64")->getTypeId());
}

TEST_F(RangeTest, testLessThanRange)
{
    Range range = Range::lessThanRange(typeFromString("Int64"), Field(Int64(1)));

    ASSERT_TRUE(range.isLowUnbounded());
    ASSERT_FALSE(range.isLowInclusive());
    ASSERT_TRUE(range.getLowValue().isNull());

    ASSERT_FALSE(range.isHighUnbounded());
    ASSERT_FALSE(range.isHighInclusive());
    ASSERT_TRUE(range.getHighValue() == Field(Int64(1)));

    ASSERT_FALSE(range.isSingleValue());
    ASSERT_FALSE(range.isAll());
    ASSERT_TRUE(range.getType()->getTypeId() == typeFromString("Int64")->getTypeId());
}

TEST_F(RangeTest, testLessThanOrEqualRange)
{
    Range range = Range::lessThanOrEqualRange(typeFromString("Int64"), Field(Int64(1)));

    ASSERT_TRUE(range.isLowUnbounded());
    ASSERT_FALSE(range.isLowInclusive());
    ASSERT_TRUE(range.getLowValue().isNull());

    ASSERT_FALSE(range.isHighUnbounded());
    ASSERT_TRUE(range.isHighInclusive());
    ASSERT_TRUE(range.getHighValue() == Field(Int64(1)));

    ASSERT_FALSE(range.isSingleValue());
    ASSERT_FALSE(range.isAll());
    ASSERT_TRUE(range.getType()->getTypeId() == typeFromString("Int64")->getTypeId());
}

TEST_F(RangeTest, testEqualRange)
{
    Range range = Range::equalRange(typeFromString("Int64"), Field(Int64(1)));

    ASSERT_FALSE(range.isLowUnbounded());
    ASSERT_TRUE(range.isLowInclusive());
    ASSERT_TRUE(range.getLowValue() == Field(Int64(1)));

    ASSERT_FALSE(range.isHighUnbounded());
    ASSERT_TRUE(range.isHighInclusive());
    ASSERT_TRUE(range.getLowValue() == Field(Int64(1)));

    ASSERT_TRUE(range.isSingleValue());
    ASSERT_FALSE(range.isAll());
    ASSERT_TRUE(range.getType()->getTypeId() == typeFromString("Int64")->getTypeId());
}

TEST_F(RangeTest, testRange)
{
    //(0,2]
    Range range = Range(typeFromString("Int64"), false, Field(Int64(0)), true, Field(Int64(2)));
    ASSERT_FALSE(range.isLowUnbounded());
    ASSERT_FALSE(range.isLowInclusive());
    ASSERT_TRUE(range.getLowValue() == Field(Int64(0)));

    ASSERT_FALSE(range.isHighUnbounded());
    ASSERT_TRUE(range.isHighInclusive());
    ASSERT_TRUE(range.getHighValue() == Field(Int64(2)));

    ASSERT_FALSE(range.isSingleValue());
    ASSERT_FALSE(range.isAll());
    ASSERT_TRUE(range.getType()->getTypeId() == typeFromString("Int64")->getTypeId());
}

TEST_F(RangeTest, testGetSingleValue)
{
    ASSERT_TRUE(Range::equalRange(typeFromString("Int64"), Field(Int64(0))).getSingleValue() == Field(Int64(0)));
//    try
//    {
//        Range::lessThanRange(typeFromString("Int64"), Field(Int64(0))).getSingleValue();
//    }
//    catch (...)
//    {
//        std::cerr << "Range does not have just a single value" << std::endl;
//    }
}

TEST_F(RangeTest, testContains)
{
    ASSERT_TRUE(Range::allRange(type_int64).contains(Range::allRange(type_int64)));
    ASSERT_TRUE(Range::allRange(type_int64).contains(Range::equalRange(type_int64, field_0)));
    ASSERT_TRUE(Range::allRange(type_int64).contains(Range::greaterThanRange(type_int64, field_0)));
    ASSERT_TRUE(Range::equalRange(type_int64, field_0).contains(Range::equalRange(type_int64, field_0)));
    ASSERT_FALSE(Range::equalRange(type_int64, field_0).contains(Range::greaterThanRange(type_int64, field_0)));
    ASSERT_FALSE(Range::equalRange(type_int64, field_0).contains(Range::greaterThanOrEqualRange(type_int64, field_0)));
    ASSERT_FALSE(Range::equalRange(type_int64, field_0).contains(Range::allRange(type_int64)));
    ASSERT_TRUE(Range::greaterThanOrEqualRange(type_int64, field_0).contains(Range::greaterThanRange(type_int64, field_0)));
    ASSERT_TRUE(Range::greaterThanRange(type_int64, field_0).contains(Range::greaterThanRange(type_int64, field_1)));
    ASSERT_FALSE(Range::greaterThanRange(type_int64, field_0).contains(Range::lessThanRange(type_int64, field_0)));
    ASSERT_TRUE(Range(type_int64, true, field_0, true, field_2).contains(Range(type_int64, true, field_1, true, field_2)));
    ASSERT_FALSE(Range(type_int64, true, field_0, true, field_2).contains(Range(type_int64, true, field_1, false, field_3)));
}

TEST_F(RangeTest, testSpan)
{
    ASSERT_TRUE(Range::greaterThanRange(type_int64, field_1).span(Range::lessThanOrEqualRange(type_int64, field_2)) == Range::allRange(type_int64));
    ASSERT_TRUE(Range::greaterThanRange(type_int64, field_2).span(Range::lessThanOrEqualRange(type_int64, field_0)) == Range::allRange(type_int64));
    ASSERT_TRUE(Range(type_int64, true, field_1, false, field_3).span(Range::equalRange(type_int64, field_2)) == Range(type_int64, true, field_1, false, field_3));
    ASSERT_TRUE(Range(type_int64, true, field_1, false, field_3).span(Range(type_int64, false, field_2, false, field_10)) == Range(type_int64, true, field_1, false, field_10));
    ASSERT_TRUE(Range::greaterThanRange(type_int64, field_1).span(Range::equalRange(type_int64, field_0)) == Range::greaterThanOrEqualRange(type_int64, field_0));
    ASSERT_TRUE(Range::greaterThanRange(type_int64, field_1).span(Range::greaterThanOrEqualRange(type_int64, field_10)) == Range::greaterThanRange(type_int64, field_1));
    ASSERT_TRUE(Range::lessThanRange(type_int64, field_1).span(Range::lessThanOrEqualRange(type_int64, field_1)) == Range::lessThanOrEqualRange(type_int64, field_1));
    ASSERT_TRUE(Range::allRange(type_int64).span(Range::lessThanOrEqualRange(type_int64, field_1)) == Range::allRange(type_int64));
}

TEST_F(RangeTest, testOverlaps)
{
    ASSERT_TRUE(Range::greaterThanRange(type_int64, field_1).overlaps(Range::lessThanOrEqualRange(type_int64, field_2)));
    ASSERT_FALSE(Range::greaterThanRange(type_int64, field_2).overlaps(Range::lessThanRange(type_int64, field_2)));
    ASSERT_TRUE(Range(type_int64, true, field_1, false, field_3).overlaps(Range::equalRange(type_int64, field_2)));
    ASSERT_TRUE(Range(type_int64, true, field_1, false, field_3).overlaps(Range(type_int64, false, field_2, false, field_10)));
    ASSERT_FALSE(Range(type_int64, true, field_1, false, field_3).overlaps(Range(type_int64, true, field_3, false, field_10)));
    ASSERT_TRUE(Range(type_int64, true, field_1, true, field_3).overlaps(Range(type_int64, true, field_3, false, field_10)));
    ASSERT_TRUE(Range::allRange(type_int64).overlaps(Range::equalRange(type_int64, type_int64->getRange().value().max)));
}

TEST_F(RangeTest, testIntersect)
{
    std::optional<Range> range_1 = Range::greaterThanRange(type_int64, field_1).intersect(Range::lessThanOrEqualRange(type_int64, field_2));
    ASSERT_TRUE(range_1.has_value());
    ASSERT_TRUE(range_1.value() == Range(type_int64, false, field_1, true, field_2));

    std::optional<Range> range_2 = Range(type_int64, true, field_1, false, field_3).intersect(Range::equalRange(type_int64, field_2));
    ASSERT_TRUE(range_2.has_value());
    ASSERT_TRUE(range_2 == Range::equalRange(type_int64, field_2));

    std::optional<Range> range_3 = Range(type_int64, true, field_1, false, field_3).intersect(Range(type_int64, false, field_2, false, field_10));
    ASSERT_TRUE(range_3.has_value());
    ASSERT_TRUE(range_3 == Range(type_int64, false, field_2, false, field_3));

    std::optional<Range> range_4 = Range(type_int64, true, field_1, true, field_3).intersect(Range(type_int64, true, field_3, false, field_10));
    ASSERT_TRUE(range_4.has_value());
    ASSERT_TRUE(range_4 == Range::equalRange(type_int64, field_3));

    std::optional<Range> range_5 = Range::allRange(type_int64).intersect(Range::equalRange(type_int64, type_int64->getRange().value().max));
    ASSERT_TRUE(range_5.has_value());
    ASSERT_TRUE(range_5 == Range::equalRange(type_int64, type_int64->getRange().value().max));
}


TEST_F(RangeTest, testExceptionalIntersect)
{
    Range greater_than_2 = Range::greaterThanRange(type_int64, field_2);
    Range less_than_2 = Range::lessThanRange(type_int64, field_2);
    ASSERT_FALSE(greater_than_2.intersect(less_than_2).has_value());

    Range range_1_to_3_Exclusive = Range(type_int64, true, field_1, false, field_3);
    Range range_3_to_10 = Range(type_int64, true, field_3, false, field_10);
    ASSERT_FALSE(range_1_to_3_Exclusive.intersect(range_3_to_10).has_value());
}

//int main(int argc, char **argv)
//{
//    ::testing::InitGoogleTest(&argc, argv);
//    return RUN_ALL_TESTS();
//}
