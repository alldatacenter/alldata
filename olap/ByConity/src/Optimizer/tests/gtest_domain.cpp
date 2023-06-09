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
#include "Common/Stopwatch.h"
#include "Core/Field.h"
#include "DataTypes/DataTypeFactory.h"
#include "Optimizer/domain.h"
#include <Common/tests/gtest_global_register.h>
#include <Common/tests/gtest_global_context.h>
using namespace DB;
using namespace DB::Predicate;



class DomainTest : public testing::Test
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

    DomainTest()
    {
        int64_type = typeFromString("Int64");
        float32_type = typeFromString("Float32");
        float64_type = typeFromString("Float64");
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

    ~DomainTest() {}
    DataTypePtr int64_type;
    DataTypePtr float32_type;
    DataTypePtr float64_type;
    DataTypePtr type_string;
    Field field_0 = Int64(0);
    Field field_1 = Int64(1);
    Field field_2 = Int64(2);
    Field field_3 = Int64(3);
    Field field_4 = Int64(4);
    Field field_5 = Int64(5);
    Field field_6 = Int64(6);
    Field field_7 = Int64(7);
    Field field_8 = Int64(8);
    Field field_9 = Int64(9);
    Field field_10 = Int64(10);
    Field field_11 = Int64(11);
    Field field_40 = Int64(40);
    Field field_41 = Int64(41);
};

TEST_F(DomainTest, testOrderableNone)
{
    Domain domain = Domain::none(int64_type);
    ASSERT_TRUE(domain.isNone());
    ASSERT_FALSE(domain.isAll());
    ASSERT_FALSE(domain.isSingleValue());
    ASSERT_FALSE(domain.isNullableSingleValue());
    ASSERT_FALSE(domain.isNullAllowed());
    ASSERT_EQ(domain.getValueSet(), createNone(int64_type));
    ASSERT_EQ(domain.getType(), int64_type);
    ASSERT_FALSE(domain.includesNullableValue(int64_type->getRange().value().min));
    ASSERT_FALSE(domain.includesNullableValue(field_0));
    ASSERT_FALSE(domain.includesNullableValue(int64_type->getRange().value().max));
    ASSERT_FALSE(domain.includesNullableValue(Field()));
    ASSERT_EQ(domain.complement(), Domain::all(int64_type));
    //ASSERT_EQ(domain.toString(), "NONE");
}

//TEST_F(DomainTest, testEquatableNone)
//{

//}


//    public void testUncomparableNone()
//{

//}

TEST_F(DomainTest, testOrderableAll)
{
    Domain domain = Domain::all(int64_type);
    ASSERT_FALSE(domain.isNone());
    ASSERT_TRUE(domain.isAll());
    ASSERT_FALSE(domain.isSingleValue());
    ASSERT_FALSE(domain.isNullableSingleValue());
    ASSERT_FALSE(domain.isOnlyNull());
    ASSERT_TRUE(domain.isNullAllowed());
    ASSERT_EQ(domain.getValueSet(), createAll(int64_type));
    ASSERT_EQ(domain.getType(), int64_type);
    ASSERT_TRUE(domain.includesNullableValue(int64_type->getRange().value().min));
    ASSERT_TRUE(domain.includesNullableValue(field_0));
    ASSERT_TRUE(domain.includesNullableValue(int64_type->getRange().value().max));
    ASSERT_TRUE(domain.includesNullableValue(Field()));
    ASSERT_EQ(domain.complement(), Domain::none(int64_type));
    //ASSERT_EQ(domain.toString(), "ALL");
}

TEST_F(DomainTest, testFloatingPointOrderableAll)
{
    Domain domain = Domain::all(float32_type);
    ASSERT_FALSE(domain.isNone());
    ASSERT_TRUE(domain.isAll());
    ASSERT_FALSE(domain.isSingleValue());
    ASSERT_FALSE(domain.isNullableSingleValue());
    ASSERT_FALSE(domain.isOnlyNull());
    ASSERT_TRUE(domain.isNullAllowed());
    ASSERT_EQ(domain.getValueSet(), createAll(float32_type));
    ASSERT_EQ(domain.getType(), float32_type);
    ASSERT_TRUE(domain.includesNullableValue(Field(Float32(0.0))));
    ASSERT_TRUE(domain.includesNullableValue(FLT_MAX));
    ASSERT_TRUE(domain.includesNullableValue(FLT_MIN));
    ASSERT_TRUE(domain.includesNullableValue(Field()));
    ASSERT_TRUE(domain.includesNullableValue(Field(std::sqrt(-1))));
    ASSERT_EQ(domain.complement(), Domain::none(float32_type));
    //    ASSERT_EQ(domain.toString(), "ALL");

    domain = Domain::all(float64_type);
    ASSERT_FALSE(domain.isNone());
    ASSERT_TRUE(domain.isAll());
    ASSERT_FALSE(domain.isSingleValue());
    ASSERT_FALSE(domain.isNullableSingleValue());
    ASSERT_FALSE(domain.isOnlyNull());
    ASSERT_TRUE(domain.isNullAllowed());
    ASSERT_EQ(domain.getValueSet(), createAll(float64_type));
    ASSERT_EQ(domain.getType(), float64_type);
    ASSERT_TRUE(domain.includesNullableValue(Float32(0.0)));
    ASSERT_TRUE(domain.includesNullableValue(DBL_MAX));
    ASSERT_TRUE(domain.includesNullableValue(DBL_MIN));
    ASSERT_TRUE(domain.includesNullableValue(Field()));
    ASSERT_TRUE(domain.includesNullableValue(Field(std::sqrt(-1))));
    ASSERT_EQ(domain.complement(), Domain::none(float64_type));
    //ASSERT_EQ(domain.toString(), "ALL");
}

//@Test
//    public void testEquatableAll()
//{

//}

//@Test
//    public void testUncomparableAll()
//{

//}

TEST_F(DomainTest, testOrderableNullOnly)
{
    Domain domain = Domain::onlyNull(int64_type);
    ASSERT_FALSE(domain.isNone());
    ASSERT_FALSE(domain.isAll());
    ASSERT_FALSE(domain.isSingleValue());
    ASSERT_TRUE(domain.isNullAllowed());
    ASSERT_TRUE(domain.isNullableSingleValue());
    ASSERT_TRUE(domain.isOnlyNull());
    ASSERT_EQ(domain.getValueSet(), createNone(int64_type));
    ASSERT_EQ(domain.getType(), int64_type);
    ASSERT_FALSE(domain.includesNullableValue(int64_type->getRange().value().min));
    ASSERT_FALSE(domain.includesNullableValue(field_0));
    ASSERT_FALSE(domain.includesNullableValue(int64_type->getRange().value().max));
    ASSERT_TRUE(domain.includesNullableValue(Field()));
    ASSERT_EQ(domain.complement(), Domain::notNull(int64_type));
    ASSERT_EQ(domain.getNullableSingleValue(), Field());
    //ASSERT_EQ(domain.toString(), "[NULL]");
}
//
//@Test
//    public void testEquatableNullOnly()
//{

//}

//@Test
//    public void testUncomparableNullOnly()
//{
//
//}

TEST_F(DomainTest, testOrderableNotNull)
{
    Domain domain = Domain::notNull(int64_type);
    ASSERT_FALSE(domain.isNone());
    ASSERT_FALSE(domain.isAll());
    ASSERT_FALSE(domain.isSingleValue());
    ASSERT_FALSE(domain.isNullableSingleValue());
    ASSERT_FALSE(domain.isOnlyNull());
    ASSERT_FALSE(domain.isNullAllowed());
    ASSERT_EQ(domain.getValueSet(), createAll(int64_type));
    ASSERT_EQ(domain.getType(), int64_type);
    ASSERT_TRUE(domain.includesNullableValue(int64_type->getRange().value().min));
    ASSERT_TRUE(domain.includesNullableValue(field_0));
    ASSERT_TRUE(domain.includesNullableValue(int64_type->getRange().value().max));
    ASSERT_FALSE(domain.includesNullableValue(Field()));
    ASSERT_EQ(domain.complement(), Domain::onlyNull(int64_type));
    //ASSERT_EQ(domain.toString(), "[ SortedRangeSet[type=bigint, ranges=1, {(<min>,<max>)}] ]");
}

//TEST_F(DomainTest, testEquatableNotNull)
//{

//}

//TEST_F(DomainTest, testUncomparableNotNull)
//{
//
//}

TEST_F(DomainTest, testOrderableSingleValue)
{
    Domain domain = Domain::singleValue(int64_type, field_0);
    ASSERT_FALSE(domain.isNone());
    ASSERT_FALSE(domain.isAll());
    ASSERT_TRUE(domain.isSingleValue());
    ASSERT_TRUE(domain.isNullableSingleValue());
    ASSERT_FALSE(domain.isOnlyNull());
    ASSERT_FALSE(domain.isNullAllowed());
    ASSERT_EQ(domain.getValueSet(), createValueSet({Range::equalRange(int64_type, field_0)}));
    ASSERT_EQ(domain.getType(), int64_type);
    ASSERT_FALSE(domain.includesNullableValue(int64_type->getRange().value().min));
    ASSERT_TRUE(domain.includesNullableValue(field_0));
    ASSERT_FALSE(domain.includesNullableValue(int64_type->getRange().value().max));
    ASSERT_EQ(domain.complement(), Domain(createValueSet({Range::lessThanRange(int64_type, field_0), Range::greaterThanRange(int64_type, field_0)}), true));
    ASSERT_EQ(domain.getSingleValue(), field_0);
    ASSERT_EQ(domain.getNullableSingleValue(), field_0);
    //ASSERT_EQ(domain.toString(), "[ SortedRangeSet[type=bigint, ranges=1, {[0]}] ]");

    //    assertThatThrownBy(() -> Domain(createValueSet({Range.range(int64_type, field_1, true, field_2, true)), false).getSingleValue())
    //        .isInstanceOf(IllegalStateException.class)
    //        .hasMessage("Domain is not a single value");
}

//TEST_F(DomainTest, testEquatableSingleValue)
//{
//
//}


//    public void testUncomparableSingleValue()
//{
//
//}

TEST_F(DomainTest, testOverlaps)
{
    ASSERT_TRUE(Domain::all(int64_type).overlaps(Domain::all(int64_type)));
    ASSERT_FALSE(Domain::all(int64_type).overlaps(Domain::none(int64_type)));
    ASSERT_TRUE(Domain::all(int64_type).overlaps(Domain::notNull(int64_type)));
    ASSERT_TRUE(Domain::all(int64_type).overlaps(Domain::onlyNull(int64_type)));
    ASSERT_TRUE(Domain::all(int64_type).overlaps(Domain::singleValue(int64_type, field_0)));

    ASSERT_FALSE(Domain::none(int64_type).overlaps(Domain::all(int64_type)));
    ASSERT_FALSE(Domain::none(int64_type).overlaps(Domain::none(int64_type)));
    ASSERT_FALSE(Domain::none(int64_type).overlaps(Domain::notNull(int64_type)));
    ASSERT_FALSE(Domain::none(int64_type).overlaps(Domain::onlyNull(int64_type)));
    ASSERT_FALSE(Domain::none(int64_type).overlaps(Domain::singleValue(int64_type, field_0)));

    ASSERT_TRUE(Domain::notNull(int64_type).overlaps(Domain::all(int64_type)));
    ASSERT_FALSE(Domain::notNull(int64_type).overlaps(Domain::none(int64_type)));
    ASSERT_TRUE(Domain::notNull(int64_type).overlaps(Domain::notNull(int64_type)));
    ASSERT_FALSE(Domain::notNull(int64_type).overlaps(Domain::onlyNull(int64_type)));
    ASSERT_TRUE(Domain::notNull(int64_type).overlaps(Domain::singleValue(int64_type, field_0)));

    ASSERT_TRUE(Domain::onlyNull(int64_type).overlaps(Domain::all(int64_type)));
    ASSERT_FALSE(Domain::onlyNull(int64_type).overlaps(Domain::none(int64_type)));
    ASSERT_FALSE(Domain::onlyNull(int64_type).overlaps(Domain::notNull(int64_type)));
    ASSERT_TRUE(Domain::onlyNull(int64_type).overlaps(Domain::onlyNull(int64_type)));
    ASSERT_FALSE(Domain::onlyNull(int64_type).overlaps(Domain::singleValue(int64_type, field_0)));

    ASSERT_TRUE(Domain::singleValue(int64_type, field_0).overlaps(Domain::all(int64_type)));
    ASSERT_FALSE(Domain::singleValue(int64_type, field_0).overlaps(Domain::none(int64_type)));
    ASSERT_TRUE(Domain::singleValue(int64_type, field_0).overlaps(Domain::notNull(int64_type)));
    ASSERT_FALSE(Domain::singleValue(int64_type, field_0).overlaps(Domain::onlyNull(int64_type)));
    ASSERT_TRUE(Domain::singleValue(int64_type, field_0).overlaps(Domain::singleValue(int64_type, field_0)));
}

TEST_F(DomainTest, testContains)
{
    ASSERT_TRUE(Domain::all(int64_type).contains(Domain::all(int64_type)));
    ASSERT_TRUE(Domain::all(int64_type).contains(Domain::none(int64_type)));
    ASSERT_TRUE(Domain::all(int64_type).contains(Domain::notNull(int64_type)));
    ASSERT_TRUE(Domain::all(int64_type).contains(Domain::onlyNull(int64_type)));
    ASSERT_TRUE(Domain::all(int64_type).contains(Domain::singleValue(int64_type, field_0)));

    ASSERT_FALSE(Domain::none(int64_type).contains(Domain::all(int64_type)));
    ASSERT_TRUE(Domain::none(int64_type).contains(Domain::none(int64_type)));
    ASSERT_FALSE(Domain::none(int64_type).contains(Domain::notNull(int64_type)));
    ASSERT_FALSE(Domain::none(int64_type).contains(Domain::onlyNull(int64_type)));
    ASSERT_FALSE(Domain::none(int64_type).contains(Domain::singleValue(int64_type, field_0)));

    ASSERT_FALSE(Domain::notNull(int64_type).contains(Domain::all(int64_type)));
    ASSERT_TRUE(Domain::notNull(int64_type).contains(Domain::none(int64_type)));
    ASSERT_TRUE(Domain::notNull(int64_type).contains(Domain::notNull(int64_type)));
    ASSERT_FALSE(Domain::notNull(int64_type).contains(Domain::onlyNull(int64_type)));
    ASSERT_TRUE(Domain::notNull(int64_type).contains(Domain::singleValue(int64_type, field_0)));

    ASSERT_FALSE(Domain::onlyNull(int64_type).contains(Domain::all(int64_type)));
    ASSERT_TRUE(Domain::onlyNull(int64_type).contains(Domain::none(int64_type)));
    ASSERT_FALSE(Domain::onlyNull(int64_type).contains(Domain::notNull(int64_type)));
    ASSERT_TRUE(Domain::onlyNull(int64_type).contains(Domain::onlyNull(int64_type)));
    ASSERT_FALSE(Domain::onlyNull(int64_type).contains(Domain::singleValue(int64_type, field_0)));

    ASSERT_FALSE(Domain::singleValue(int64_type, field_0).contains(Domain::all(int64_type)));
    ASSERT_TRUE(Domain::singleValue(int64_type, field_0).contains(Domain::none(int64_type)));
    ASSERT_FALSE(Domain::singleValue(int64_type, field_0).contains(Domain::notNull(int64_type)));
    ASSERT_FALSE(Domain::singleValue(int64_type, field_0).contains(Domain::onlyNull(int64_type)));
    ASSERT_TRUE(Domain::singleValue(int64_type, field_0).contains(Domain::singleValue(int64_type, field_0)));
}

TEST_F(DomainTest, testIntersect)
{
    ASSERT_EQ(
        Domain::all(int64_type).intersect(Domain::all(int64_type)),
        Domain::all(int64_type));

    ASSERT_EQ(
        Domain::none(int64_type).intersect(Domain::none(int64_type)),
        Domain::none(int64_type));

    ASSERT_EQ(
        Domain::all(int64_type).intersect(Domain::none(int64_type)),
        Domain::none(int64_type));

    ASSERT_EQ(
        Domain::notNull(int64_type).intersect(Domain::onlyNull(int64_type)),
        Domain::none(int64_type));

    ASSERT_EQ(
        Domain::singleValue(int64_type, field_0).intersect(Domain::all(int64_type)),
        Domain::singleValue(int64_type, field_0));

    ASSERT_EQ(
        Domain::singleValue(int64_type, field_0).intersect(Domain::onlyNull(int64_type)),
        Domain::none(int64_type));

    ASSERT_EQ(
        Domain(createValueSet({Range::equalRange(int64_type, field_1)}), true).intersect(Domain(createValueSet({Range::equalRange(int64_type, field_2)}), true)),
        Domain::onlyNull(int64_type));

    ASSERT_EQ(
        Domain(createValueSet({Range::equalRange(int64_type, field_1)}), true).intersect(Domain(createValueSet({Range::equalRange(int64_type, field_1), Range::equalRange(int64_type, field_2)}), false)),
        Domain::singleValue(int64_type, field_1));
}

TEST_F(DomainTest, testUnion)
{
    ASSERT_EQ(Domain::all(int64_type).unionn(Domain::all(int64_type)), Domain::all(int64_type));
    ASSERT_EQ(Domain::none(int64_type).unionn(Domain::none(int64_type)), Domain::none(int64_type));
    ASSERT_EQ(Domain::all(int64_type).unionn(Domain::none(int64_type)), Domain::all(int64_type));
    ASSERT_EQ(Domain::notNull(int64_type).unionn(Domain::onlyNull(int64_type)), Domain::all(int64_type));
    ASSERT_EQ(Domain::singleValue(int64_type, field_0).unionn(Domain::all(int64_type)), Domain::all(int64_type));
    ASSERT_EQ(Domain::singleValue(int64_type, field_0).unionn(Domain::notNull(int64_type)), Domain::notNull(int64_type));
    ASSERT_EQ(Domain::singleValue(int64_type, field_0).unionn(Domain::onlyNull(int64_type)), Domain(createValueSet({Range::equalRange(int64_type, field_0)}), true));

    ASSERT_EQ(Domain(createValueSet({Range::equalRange(int64_type, field_1)}), true).unionn(
                  Domain(createValueSet({Range::equalRange(int64_type, field_2)}), true)),
              Domain(createValueSet({Range::equalRange(int64_type, field_1), Range::equalRange(int64_type, field_2)}), true));

    ASSERT_EQ(Domain(createValueSet({Range::equalRange(int64_type, field_1)}), true).unionn(
                  Domain(createValueSet({Range::equalRange(int64_type, field_1), Range::equalRange(int64_type, field_2)}), false)),
              Domain(createValueSet({Range::equalRange(int64_type, field_1), Range::equalRange(int64_type, field_2)}), true));

    ASSERT_EQ(
        Domain(createValueSet({Range::lessThanOrEqualRange(int64_type, 20L)}), true).unionn(
            Domain(createValueSet({Range::greaterThanOrEqualRange(int64_type, 10L)}), true)),
        Domain::all(int64_type));

    ASSERT_EQ(
        Domain(createValueSet({Range::lessThanOrEqualRange(int64_type, 20L)}), false).unionn(
            Domain(createValueSet({Range::greaterThanOrEqualRange(int64_type, 10L)}), false)),
        Domain(createAll(int64_type), false));
}

TEST_F(DomainTest, testSubtract)
{
    ASSERT_EQ(
        Domain::all(int64_type).subtract(Domain::all(int64_type)),
        Domain::none(int64_type));
    ASSERT_EQ(
        Domain::all(int64_type).subtract(Domain::none(int64_type)),
        Domain::all(int64_type));
    ASSERT_EQ(
        Domain::all(int64_type).subtract(Domain::notNull(int64_type)),
        Domain::onlyNull(int64_type));
    ASSERT_EQ(
        Domain::all(int64_type).subtract(Domain::onlyNull(int64_type)),
        Domain::notNull(int64_type));
    ASSERT_EQ(
        Domain::all(int64_type).subtract(Domain::singleValue(int64_type, field_0)),
        Domain(createValueSet({Range::lessThanRange(int64_type, field_0), Range::greaterThanRange(int64_type, field_0)}), true));

    ASSERT_EQ(
        Domain::none(int64_type).subtract(Domain::all(int64_type)),
        Domain::none(int64_type));
    ASSERT_EQ(
        Domain::none(int64_type).subtract(Domain::none(int64_type)),
        Domain::none(int64_type));
    ASSERT_EQ(
        Domain::none(int64_type).subtract(Domain::notNull(int64_type)),
        Domain::none(int64_type));
    ASSERT_EQ(
        Domain::none(int64_type).subtract(Domain::onlyNull(int64_type)),
        Domain::none(int64_type));
    ASSERT_EQ(
        Domain::none(int64_type).subtract(Domain::singleValue(int64_type, field_0)),
        Domain::none(int64_type));

    ASSERT_EQ(
        Domain::notNull(int64_type).subtract(Domain::all(int64_type)),
        Domain::none(int64_type));
    ASSERT_EQ(
        Domain::notNull(int64_type).subtract(Domain::none(int64_type)),
        Domain::notNull(int64_type));
    ASSERT_EQ(
        Domain::notNull(int64_type).subtract(Domain::notNull(int64_type)),
        Domain::none(int64_type));
    ASSERT_EQ(
        Domain::notNull(int64_type).subtract(Domain::onlyNull(int64_type)),
        Domain::notNull(int64_type));
    ASSERT_EQ(
        Domain::notNull(int64_type).subtract(Domain::singleValue(int64_type, field_0)),
        Domain(createValueSet({Range::lessThanRange(int64_type, field_0), Range::greaterThanRange(int64_type, field_0)}), false));

    ASSERT_EQ(
        Domain::onlyNull(int64_type).subtract(Domain::all(int64_type)),
        Domain::none(int64_type));
    ASSERT_EQ(
        Domain::onlyNull(int64_type).subtract(Domain::none(int64_type)),
        Domain::onlyNull(int64_type));
    ASSERT_EQ(
        Domain::onlyNull(int64_type).subtract(Domain::notNull(int64_type)),
        Domain::onlyNull(int64_type));
    ASSERT_EQ(
        Domain::onlyNull(int64_type).subtract(Domain::onlyNull(int64_type)),
        Domain::none(int64_type));
    ASSERT_EQ(
        Domain::onlyNull(int64_type).subtract(Domain::singleValue(int64_type, field_0)),
        Domain::onlyNull(int64_type));

    ASSERT_EQ(
        Domain::singleValue(int64_type, field_0).subtract(Domain::all(int64_type)),
        Domain::none(int64_type));
    ASSERT_EQ(
        Domain::singleValue(int64_type, field_0).subtract(Domain::none(int64_type)),
        Domain::singleValue(int64_type, field_0));
    ASSERT_EQ(
        Domain::singleValue(int64_type, field_0).subtract(Domain::notNull(int64_type)),
        Domain::none(int64_type));
    ASSERT_EQ(
        Domain::singleValue(int64_type, field_0).subtract(Domain::onlyNull(int64_type)),
        Domain::singleValue(int64_type, field_0));
    ASSERT_EQ(
        Domain::singleValue(int64_type, field_0).subtract(Domain::singleValue(int64_type, field_0)),
        Domain::none(int64_type));

    ASSERT_EQ(
        Domain(createValueSet({Range::equalRange(int64_type, field_1)}), true).subtract(Domain(createValueSet({Range::equalRange(int64_type, field_2)}), true)),
        Domain::singleValue(int64_type, field_1));

    ASSERT_EQ(
        Domain(createValueSet({Range::equalRange(int64_type, field_1)}), true).subtract(Domain(createValueSet({Range::equalRange(int64_type, field_1), Range::equalRange(int64_type, field_2)}), false)),
        Domain::onlyNull(int64_type));
}

//int main(int argc, char **argv)
//{
//    ::testing::InitGoogleTest(&argc, argv);
//    return RUN_ALL_TESTS();
//}
