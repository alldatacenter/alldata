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

#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <Optimizer/UnwrapCastInComparison.h>
#include <Parsers/ExpressionElementParsers.h>
#include <Parsers/ExpressionListParsers.h>
#include <Parsers/formatAST.h>
#include <Parsers/parseQuery.h>
#include <Common/FieldVisitors.h>
#include <Common/tests/gtest_global_context.h>
#include <Common/tests/gtest_global_register.h>
#include <Common/FieldVisitorToString.h>
#include <Optimizer/tests/gtest_optimizer_test_utils.h>

#include <gtest/gtest.h>

using namespace DB;

const DataTypePtr type_uint8 = std::make_shared<DataTypeUInt8>();
const DataTypePtr type_nullable_uint8 = makeNullable(std::make_shared<DataTypeUInt8>());
const DataTypePtr type_uint16 = std::make_shared<DataTypeUInt16>();
const DataTypePtr type_uint32 = std::make_shared<DataTypeUInt32>();
const DataTypePtr type_uint64 = std::make_shared<DataTypeUInt64>();
const DataTypePtr type_int8 = std::make_shared<DataTypeInt8>();
const DataTypePtr type_int32 = std::make_shared<DataTypeInt32>();
const DataTypePtr type_nullable_int32 = makeNullable(std::make_shared<DataTypeInt32>());
const DataTypePtr type_int64 = std::make_shared<DataTypeInt64>();
const DataTypePtr type_float32 = std::make_shared<DataTypeFloat32>();
const DataTypePtr type_float64 = std::make_shared<DataTypeFloat64>();
const DataTypePtr type_string = std::make_shared<DataTypeString>();

static void testCoerce(
    const Field & field, const DataTypePtr & src_type, const DataTypePtr & tgt_type, const Field & expect, const ContextPtr & context)
{
    auto actual = UnwrapCastInComparisonVisitor::coerce(field, src_type, tgt_type, context);
    if (actual != expect)
        GTEST_FAIL() << "Value " << applyVisitor(FieldVisitorToString(), field) << " of type " << src_type->getName()
                     << " is casted to value " << applyVisitor(FieldVisitorToString(), actual) << " of type " << tgt_type->getName()
                     << ", but expect: " << applyVisitor(FieldVisitorToString(), expect) << std::endl;
}

TEST(OptimizerUnwrapCastInComparisonTest, Coerce)
{
    auto & context = getContext().context;
    tryRegisterFunctions();

    testCoerce(123U, type_uint8, type_uint16, 123U, context);
    testCoerce(123U, type_uint16, type_uint16, 123U, context);
    testCoerce(256U, type_uint16, type_uint8, 0U, context);
    testCoerce(1U, type_uint8, type_float32, 1.0, context);
    testCoerce(1.2, type_float64, type_uint16, 1U, context);
    testCoerce(1, type_int32, type_string, "1", context);
    testCoerce(-1, type_nullable_int32, type_uint8, 255U, context);
    testCoerce(Null(), type_nullable_int32, type_nullable_uint8, Null(), context);
}

static void
testCompare(const Field & l, const DataTypePtr & lt, const Field & r, const DataTypePtr & rt, int expect, const ContextPtr & context)
{
    auto actual = UnwrapCastInComparisonVisitor::compare(l, lt, r, rt, context);
    if (actual != expect)
        GTEST_FAIL() << "Compare " << applyVisitor(FieldVisitorToString(), l) << ":" << lt->getName() << " with "
                     << applyVisitor(FieldVisitorToString(), r) << ":" << rt->getName() << " got " << actual << ", but expect: " << expect
                     << std::endl;
}

TEST(OptimizerUnwrapCastInComparisonTest, Compare)
{
    const auto & context = getContext().context;
    tryRegisterFunctions();

    testCompare(10U, type_uint8, 0U, type_uint8, 1, context);
    testCompare(10U, type_uint8, 10U, type_uint8, 0, context);
    testCompare(10U, type_uint8, 20U, type_uint8, -1, context);
    testCompare(10U, type_uint8, 0U, type_uint16, 1, context);
    testCompare(10U, type_uint8, 10U, type_uint16, 0, context);
    testCompare(10U, type_uint8, 20U, type_uint16, -1, context);
    testCompare(10U, type_uint8, -100, type_int32, 1, context);
    testCompare(10U, type_uint8, 0, type_int32, 1, context);
    testCompare(10U, type_uint8, 10, type_int32, 0, context);
    testCompare(10U, type_uint8, 20, type_int32, -1, context);
    testCompare(-100, type_int32, 10U, type_uint8, -1, context);
    testCompare(0, type_int32, 10U, type_uint8, -1, context);
    testCompare(10, type_int32, 10U, type_uint8, 0, context);
    testCompare(20, type_int32, 10U, type_uint8, 1, context);
    testCompare(1U, type_uint8, 0.9, type_float32, 1, context);
    testCompare(1U, type_uint8, 1.0, type_float32, 0, context);
    testCompare(1U, type_uint8, 1.1, type_float32, -1, context);
    testCompare(1U, type_uint8, 0.9, type_float64, 1, context);
    testCompare(1U, type_uint8, 1.0, type_float64, 0, context);
    testCompare(1U, type_uint8, 1.1, type_float64, -1, context);
    testCompare(10U, type_uint8, 0, type_nullable_int32, 1, context);
    testCompare(10U, type_uint8, 10, type_nullable_int32, 0, context);
    testCompare(10U, type_uint8, 20, type_nullable_int32, -1, context);

    testCompare(10U, type_uint32, 5L, type_int32, 1, context);
    testCompare(10U, type_uint32, 10L, type_int32, 0, context);
    testCompare(10U, type_uint32, 15L, type_int32, -1, context);
    testCompare(10U, type_uint32, -10L, type_int32, 1, context);
    testCompare(10U, type_uint64, 5L, type_int64, 1, context);
    testCompare(10U, type_uint64, 10L, type_int64, 0, context);
    testCompare(10U, type_uint64, 15L, type_int64, -1, context);
    testCompare(10U, type_uint64, -10L, type_int64, 1, context);
}

static void testCase(const String & expr_in, const String & expr_out, ContextMutablePtr context)
{
    static NameToType column_types{
        {"uint8", type_uint8},
        {"nullable_uint8", type_nullable_uint8},
        {"uint32", type_uint32},
        {"int8", type_int8},
        {"int32", type_int32},
        {"int64", type_int64},
        {"float32", type_float32},
        {"float64", type_float64},
    };
    static ParserExpression parser{ParserSettings::ANSI};

    auto ast_in = parseQuery(parser, expr_in, 0, 0);
    auto ast_out = unwrapCastInComparison(ast_in, context, column_types);
    auto rewritten_expr = serializeAST(*ast_out, true);

    if (rewritten_expr != expr_out)
        GTEST_FAIL() << "Expr: " << expr_in << " is rewritten to: " << rewritten_expr << ", but expect: " << expr_out << std::endl;
}

TEST(OptimizerUnwrapCastInComparisonTest, DISABLED_UnwrapCastInComparison)
{
    auto context = Context::createCopy(getContext().context);
    tryRegisterFunctions();

    // unmatched expr
    testCase("uint8", "uint8", context);
    testCase("1+1", "1 + 1", context);
    testCase("3>2", "3 > 2", context);
    testCase("cast(uint8, 'UInt32') = uint32", "cast(uint8, 'UInt32') = uint32", context);
    // toXXX conversions
    testCase("toInt32(uint8) = 255", "uint8 = 255", context);
    testCase("toInt32OrZero(nullable_uint8) != 256", "trueIfNotNull(nullable_uint8)", context);
    testCase("toInt32OrNull(nullable_uint8) <= -1", "falseIfNotNull(nullable_uint8)", context);
    // null & NaN
    testCase("cast(uint8, 'UInt32') = null", "cast(uint8, 'UInt32') = NULL", context);
    testCase("cast(uint8, 'UInt32') = 0 / 0", "cast(uint8, 'UInt32') = (0 / 0)", context);
    testCase("cast(uint8, 'UInt32') = 0.5 / 0", "cast(uint8, 'UInt32') = (0.5 / 0)", context);
    // uncomparable types
    testCase("cast(uint8, 'UInt32') = toDate('2021-01-01')", "cast(uint8, 'UInt32') = toDate('2021-01-01')", context);
    // non-monotonic cast && non-injective cast
    testCase("cast(int8, 'UInt32') = 1", "cast(int8, 'UInt32') = 1", context);
    testCase("cast(uint32, 'UInt16') = 1", "cast(uint32, 'UInt16') = 1", context);
    testCase("cast(int32, 'Float32') = 16777225", "cast(int32, 'Float32') = 16777225", context);
    testCase("cast(int64, 'Float64') = 9007199254741991", "cast(int64, 'Float64') = 9007199254741991", context);
    // source type does not have range
    testCase("cast(float32, 'Float64') = 1.0", "cast(float32, 'Float64') = 1.", context);
    // unwrap cast by out-of-range literal
    testCase("cast(uint8, 'Int32') = 256", "0", context);
    testCase("cast(uint8, 'Int32') != 256", "1", context);
    testCase("cast(uint8, 'Int32') > 256", "0", context);
    testCase("cast(uint8, 'Int32') < 256", "1", context);
    testCase("cast(uint8, 'Int32') >= 256", "0", context);
    testCase("cast(uint8, 'Int32') <= 256", "1", context);
    testCase("cast(uint8, 'Int32') = 255", "uint8 = 255", context);
    testCase("cast(uint8, 'Int32') != 255", "uint8 != 255", context);
    testCase("cast(uint8, 'Int32') > 255", "0", context);
    testCase("cast(uint8, 'Int32') < 255", "uint8 != 255", context);
    testCase("cast(uint8, 'Int32') >= 255", "uint8 = 255", context);
    testCase("cast(uint8, 'Int32') <= 255", "1", context);
    testCase("cast(uint8, 'Int32') = 0", "uint8 = 0", context);
    testCase("cast(uint8, 'Int32') != 0", "uint8 != 0", context);
    testCase("cast(uint8, 'Int32') > 0", "uint8 != 0", context);
    testCase("cast(uint8, 'Int32') < 0", "0", context);
    testCase("cast(uint8, 'Int32') >= 0", "1", context);
    testCase("cast(uint8, 'Int32') <= 0", "uint8 = 0", context);
    testCase("cast(uint8, 'Int32') = -1", "0", context);
    testCase("cast(uint8, 'Int32') != -1", "1", context);
    testCase("cast(uint8, 'Int32') > -1", "1", context);
    testCase("cast(uint8, 'Int32') < -1", "0", context);
    testCase("cast(uint8, 'Int32') >= -1", "1", context);
    testCase("cast(uint8, 'Int32') <= -1", "0", context);
    // unwrap cast by add cast for constant
    testCase("cast(uint8, 'Int32') = 1", "uint8 = 1", context);
    testCase("cast(uint8, 'Int32') != 1", "uint8 != 1", context);
    testCase("cast(uint8, 'Int32') > 1", "uint8 > 1", context);
    testCase("cast(uint8, 'Int32') < 1", "uint8 < 1", context);
    testCase("cast(uint8, 'Int32') >= 1", "uint8 >= 1", context);
    testCase("cast(uint8, 'Int32') <= 1", "uint8 <= 1", context);
    testCase("cast(uint8, 'Int32') = 1.2", "0", context);
    testCase("cast(uint8, 'Int32') != 1.2", "1", context);
    testCase("cast(uint8, 'Int32') > 1.2", "uint8 > 1", context);
    testCase("cast(uint8, 'Int32') < 1.2", "uint8 <= 1", context);
    testCase("cast(uint8, 'Int32') >= 1.2", "uint8 > 1", context);
    testCase("cast(uint8, 'Int32') <= 1.2", "uint8 <= 1", context);
    // check casted literal types
    testCase("cast(int8, 'Int32') <= 120", "int8 <= 120", context);
    testCase("cast(int32, 'Int64') <= 2000", "int32 <= cast(2000, 'Int32')", context);
    // nullable type
    testCase("cast(uint8, 'Int32') = cast(128, 'Nullable(UInt32)')", "uint8 = cast(128, 'Nullable(UInt8)')", context);
    testCase("cast(uint8, 'Nullable(Int32)') = 256", "0", context);
    testCase("cast(uint8, 'Nullable(Int32)') != 256", "1", context);
    testCase("cast(nullable_uint8, 'Nullable(Int32)') = 256", "falseIfNotNull(nullable_uint8)", context);
    testCase("cast(nullable_uint8, 'Nullable(Int32)') != 256", "trueIfNotNull(nullable_uint8)", context);
    // recursive rewrite
    testCase("cast(uint8 + 1, 'Int32') > 90 and uint8", "((uint8 + 1) > cast(90, 'UInt16')) AND uint8", context);
    testCase("cast(cast(uint8, 'Int16'), 'Int32') > 90", "uint8 > 90", context);
}

void prepareDB(ContextMutablePtr & context)
{
    // table for uint8
    executeTestDBQuery(
        "create table tbl_ui8 ("
        "  uint8 UInt8"
        ") ENGINE = Memory", context);

    executeTestDBQuery("insert into tbl_ui8 values"
                       "(0),"
                       "(1),"
                       "(2),"
                       "(3),"
                       "(4),"
                       "(5),"
                       "(10),"
                       "(128),"
                       "(201),"
                       "(254),"
                       "(255)", context);

    // table for nullable_uint8
    executeTestDBQuery(
        "create table tbl_nui8 ("
        "  id Int32,"
        "  nullable_uint8 Nullable(UInt8)"
        ") ENGINE = Memory", context);

    executeTestDBQuery("insert into tbl_nui8 values"
                       "(1, 0),"
                       "(2, 1),"
                       "(3, 2),"
                       "(4, 10),"
                       "(5, 128),"
                       "(6, 201),"
                       "(7, 254),"
                       "(8, 255),"
                       "(9, NULL)", context);

    // table for int8
    executeTestDBQuery(
        "create table tbl_i8 ("
        "  int8 Int8"
        ") ENGINE = Memory", context);

    executeTestDBQuery("insert into tbl_i8 values"
                       "(-128),"
                       "(-127),"
                       "(-31),"
                       "(-1),"
                       "(0),"
                       "(1),"
                       "(67),"
                       "(126),"
                       "(127)", context);

    // table for uint32
    executeTestDBQuery(
        "create table tbl_ui32 ("
        "  uint32 UInt32"
        ") ENGINE = Memory", context);

    executeTestDBQuery("insert into tbl_ui32 values"
                       "(0),"
                       "(1),"
                       "(100),"
                       "(99994),"
                       "(99995),"
                       "(99996),"
                       "(99997),"
                       "(99998),"
                       "(99999),"
                       "(100860),"
                       "(4294967285),"
                       "(4294967286),"
                       "(4294967287),"
                       "(4294967288),"
                       "(4294967289),"
                       "(4294967290),"
                       "(4294967291),"
                       "(4294967292),"
                       "(4294967293),"
                       "(4294967294),"
                       "(4294967295)", context);

    // table for int32
    executeTestDBQuery(
        "create table tbl_i32 ("
        "  int32 Int32"
        ") ENGINE = Memory", context);

    executeTestDBQuery("insert into tbl_i32 values"
                       "(-2147483648),"
                       "(-2147483647),"
                       "(-10086),"
                       "(-1234),"
                       "(-73),"
                       "(-1),"
                       "(0),"
                       "(1),"
                       "(2),"
                       "(3),"
                       "(128),"
                       "(16777222),"
                       "(16777223),"
                       "(16777224),"
                       "(16777225),"
                       "(16777226),"
                       "(16777227),"
                       "(16777228),"
                       "(2147483646),"
                       "(2147483647)", context);

    // table for int64
    executeTestDBQuery(
        "create table tbl_i64 ("
        "  int64 Int64"
        ") ENGINE = Memory", context);

    executeTestDBQuery("insert into tbl_i64 values"
                       "(-9223372036854775808),"
                       "(-9223372036854775807),"
                       "(-9007199254742000),"
                       "(-9007199254741999),"
                       "(-9007199254741998),"
                       "(-9007199254741997),"
                       "(-9007199254741996),"
                       "(-9007199254741995),"
                       "(-9007199254741994),"
                       "(-9007199254741993),"
                       "(-9007199254741992),"
                       "(-9007199254741991),"
                       "(-9007199254741990),"
                       "(-9007199254741989),"
                       "(-9007199254741988),"
                       "(-9007199254741987),"
                       "(-9007199254741986),"
                       "(-1),"
                       "(0),"
                       "(1),"
                       "(9223372036854775806),"
                       "(9223372036854775807)", context);

    // table for float32
    executeTestDBQuery(
        "create table tbl_f32 ("
        "  id Int32,"
        "  float32 Float32"
        ") ENGINE = Memory", context);

    executeTestDBQuery("insert into tbl_f32 values"
                       "(1, -1.1),"
                       "(2, -0),"
                       "(3, 0.01),"
                       "(4, 1.3),"
                       "(5, -100.13),"
                       "(6, 200.123),"
                       "(7, 13156),"
                       "(8, -0.9087),"
                       "(9, -123.23),"
                       "(10, 1898274.12389879),"
                       "(11, -2.4028235e27),"
                       "(10, 3.4028235e38)", context);
}

void testExecutionCase(const String & expr, const String & table, ContextMutablePtr context)
{
    static NameToType column_types{
        {"uint8", type_uint8},
        {"nullable_uint8", type_nullable_uint8},
        {"uint32", type_uint32},
        {"int8", type_int8},
        {"int32", type_int32},
        {"int64", type_int64},
        {"float32", type_float32},
        {"float64", type_float64},
    };
    static ParserExpression parser{ParserSettings::ANSI};

    auto ast_in = parseQuery(parser, expr, 0, 0);
    auto ast_out = unwrapCastInComparison(ast_in, context, column_types);
    auto rewritten_expr = serializeAST(*ast_out, true);

#ifdef DEBUG
    bool log_query = true;
#else
    bool log_query = false;
#endif

    auto expect_res = executeTestDBQuery("select " + expr + " from " + table, context, log_query);
    auto actual_res = executeTestDBQuery("select " + rewritten_expr + " from " + table, context, log_query);

    if (expect_res != actual_res)
        GTEST_FAIL() << "Execution test fails, input expr: " << expr << std::endl;
}

TEST(OptimizerUnwrapCastInComparisonTest, DISABLED_UnwrapCastInComparisonByExecution)
{
    auto context = Context::createCopy(getContext().context);
    initTestDB(context);
    prepareDB(context);

    testExecutionCase("toInt32(uint8) = 255", "tbl_ui8", context);
    testExecutionCase("toInt32OrZero(nullable_uint8) != 256", "tbl_nui8", context);
    testExecutionCase("toInt32OrNull(nullable_uint8) <= -1", "tbl_nui8", context);
    testExecutionCase("cast(uint8, 'UInt32') = null", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'UInt32') = 0 / 0", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'UInt32') = 0.5 / 0", "tbl_ui8", context);
    testExecutionCase("cast(int8, 'UInt32') = 1", "tbl_i8", context);
    testExecutionCase("cast(uint32, 'UInt16') = 1", "tbl_ui32", context);
    testExecutionCase("cast(int32, 'Float32') = 16777224", "tbl_i32", context);
    testExecutionCase("cast(int32, 'Float32') = 16777225", "tbl_i32", context);
    testExecutionCase("cast(int32, 'Float32') = 16777226", "tbl_i32", context);
    testExecutionCase("cast(int64, 'Float64') = -9007199254741991", "tbl_i64", context);
    testExecutionCase("cast(int64, 'Float64') = -9007199254741992", "tbl_i64", context);
    testExecutionCase("cast(int64, 'Float64') = -9007199254741993", "tbl_i64", context);
    testExecutionCase("cast(int64, 'Float64') = -9007199254741994", "tbl_i64", context);
    testExecutionCase("cast(float32, 'Float64') = 1.0", "tbl_f32", context);
    testExecutionCase("cast(uint8, 'Int32') = 256", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') != 256", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') > 256", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') < 256", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') >= 256", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') <= 256", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') = 255", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') != 255", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') > 255", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') < 255", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') >= 255", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') <= 255", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') = 0", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') != 0", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') > 0", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') < 0", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') >= 0", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') <= 0", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') = -1", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') != -1", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') > -1", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') < -1", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') >= -1", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') <= -1", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') = 1", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') != 1", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') > 1", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') < 1", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') >= 1", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') <= 1", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') = 1.2", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') != 1.2", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') > 1.2", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') < 1.2", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') >= 1.2", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Int32') <= 1.2", "tbl_ui8", context);
    testExecutionCase("cast(int8, 'Int32') <= 120", "tbl_i8", context);
    testExecutionCase("cast(int32, 'Int64') <= 2000", "tbl_i32", context);
    testExecutionCase("cast(uint8, 'Int32') = cast(128, 'Nullable(UInt32)')", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Nullable(Int32)') = 256", "tbl_ui8", context);
    testExecutionCase("cast(uint8, 'Nullable(Int32)') != 256", "tbl_ui8", context);
    testExecutionCase("cast(nullable_uint8, 'Nullable(Int32)') = 256", "tbl_nui8", context);
    testExecutionCase("cast(nullable_uint8, 'Nullable(Int32)') != 256", "tbl_nui8", context);
    testExecutionCase("cast(uint8 + 1, 'Int32') > 90 and uint8", "tbl_ui8", context);
    testExecutionCase("cast(cast(uint8, 'Int16'), 'Int32') > 90", "tbl_ui8", context);
}
