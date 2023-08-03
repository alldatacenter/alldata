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

#include <Optimizer/tests/gtest_optimizer_test_utils.h>

#include <Optimizer/ExpressionInterpreter.h>
#include <Parsers/parseQuery.h>
#include <Parsers/formatAST.h>
#include <Parsers/ExpressionListParsers.h>
#include <DataTypes/DataTypeFactory.h>
#include <Common/tests/gtest_global_context.h>
#include <Common/tests/gtest_global_register.h>

#include <gtest/gtest.h>

using namespace DB;
using namespace std;

struct ExprTestCase
{
    String input;
    String expected;
};

std::ostream & operator<<(std::ostream & ostr, const ExprTestCase & test_case)
{
    return ostr << "Case: " << test_case.input;
}

const static ExpressionInterpreter::IdentifierTypes column_types = [](){
    unordered_map<String, String> map
        {
            {"unbound_uint8", "UInt8"},
            {"unbound_uint16", "UInt16"},
            {"unbound_uint32", "UInt32"},
            {"unbound_uint64", "UInt64"},
            {"unbound_uint128", "UInt128"},
            {"unbound_uint256", "UInt256"},

            {"unbound_int8", "Int8"},
            {"unbound_int16", "Int16"},
            {"unbound_int32", "Int32"},
            {"unbound_int64", "Int64"},
            {"unbound_int128", "Int128"},
            {"unbound_int256", "Int256"},

            {"unbound_float32", "Float32"},
            {"unbound_float64", "Float64"},

            {"unbound_decimal9_2", "Decimal(9, 2)"},

            {"unbound_string", "String"},

            {"unbound_date", "Date"},
            {"unbound_datetime", "DateTime"},

            {"unbound_ipv4", "IPv4"},

            {"unbound_int8_array", "Array(UInt8)"},

            {"unbound_nullable_uint8", "Nullable(UInt8)"},
            {"unbound_nullable_uint32", "Nullable(UInt32)"},
            {"unbound_nullable_uint64", "Nullable(UInt64)"},

            {"bound_uint8", "UInt8"},
        };

    ExpressionInterpreter::IdentifierTypes result_map;
    const auto & factory = DataTypeFactory::instance();
    for (const auto & [column, type]: map)
        result_map.emplace(column, factory.get(type));
    return result_map;
}();

const static ExpressionInterpreter::IdentifierValues column_values
    {
        {"bound_uint8", 1U},
    };

using InterpreterPtr = std::unique_ptr<ExpressionInterpreter>;

class OptimizeExpressionTest : public ::testing::TestWithParam<ExprTestCase>
{
public:
    static ParserExpression parser;
    static InterpreterPtr interpreter;

    static void SetUpTestSuite()
    {
        tryRegisterFunctions();
        ExpressionInterpreter::InterpretSetting setting
            {
                .identifier_types = column_types,
                .identifier_values = column_values,
                .enable_null_simplify = false,
                .enable_function_simplify = false
            };
        interpreter = std::make_unique<ExpressionInterpreter>(std::move(setting), getContext().context);
    }
};

ParserExpression OptimizeExpressionTest::parser {};
InterpreterPtr OptimizeExpressionTest::interpreter {};

class OptimizePredicateTest : public ::testing::TestWithParam<ExprTestCase>
{
public:
    static ParserExpression parser;
    static InterpreterPtr interpreter;

    static void SetUpTestSuite()
    {
        tryRegisterFunctions();
        ExpressionInterpreter::InterpretSetting setting
            {
                .identifier_types = column_types,
                .identifier_values = column_values,
                .enable_null_simplify = true,
                .enable_function_simplify = true
            };
        interpreter = std::make_unique<ExpressionInterpreter>(std::move(setting), getContext().context);
    }
};

ParserExpression OptimizePredicateTest::parser {};
InterpreterPtr OptimizePredicateTest::interpreter {};

TEST_P(OptimizeExpressionTest, test)
{
    const auto & p = GetParam();
    auto input_ast = parseQuery(OptimizeExpressionTest::parser, p.input, 0, 1000);
    auto output_ast = OptimizeExpressionTest::interpreter->optimizeExpression(input_ast).second;
    auto output = serializeAST(*output_ast, true);
    ASSERT_EQ(output, p.expected);
}

TEST_P(OptimizePredicateTest, test)
{
    const auto & p = GetParam();
    auto input_ast = parseQuery(OptimizePredicateTest::parser, p.input, 0, 1000);
    auto output_ast = OptimizePredicateTest::interpreter->optimizePredicate(input_ast);
    auto output = serializeAST(*output_ast, true);
    ASSERT_EQ(output, p.expected);
}

INSTANTIATE_TEST_SUITE_P(TestExpressionInterpreter,
                         OptimizeExpressionTest,
                         ::testing::ValuesIn(
                             std::initializer_list<ExprTestCase> {
                                 /* literals & identifiers */
                                 {"1", "1"},
                                 {"unbound_uint8", "unbound_uint8"},

                                 /* ordinary function evaluation */
                                 // normal constant folding
                                 {"1 + 5 * 0.6 - 2", "2."},
                                 {"-1 + (1000000 - 1000000)", "cast(-1, 'Int64')"},
                                 {"toDecimal32(3, 2) + toDecimal32(4.5, 4)", "'7.5000'"},
                                 {"10 and (0 or not 1)", "0"},
                                 {"substring(concat('Hello, ', 'World!'), 3, 8)", "'llo, Wor'"},
                                 {"lower(rpad('ABC', 7, '*')) = 'abc****'", "lower(rpad('ABC', 7, '*')) = 'abc****'"},
                                 {"toTimeZone(toDateTime('2020-12-31 22:00:00', 'UTC'), 'Asia/Shanghai')", "cast(1609452000, 'DateTime(\\'Asia/Shanghai\\')')"},
                                 {"toYear(toDate('1994-01-01') + INTERVAL '1' YEAR)", "1995"},
                                 {"CAST(geohashDecode('ypzpgxczbzur').1, 'UInt32')", "cast(99, 'UInt32')"},
                                 {"multiIf(1 = 0, 'a', 1 > 0, 'b', 'c')", "'b'"},
                                 // constant folding by getConstantResultForNonConstArguments
                                 {"unbound_uint8 AND toUInt64(0)", "0"},
                                 {"unbound_uint8 OR toUInt64(1)", "1"},
                                 {"toTypeName(unbound_nullable_uint32)", "'Nullable(UInt32)'"},
                                 // constant folding for sub expression
                                 {"1 + 2 + unbound_uint16", "cast(3, 'UInt16') + unbound_uint16"},
                                 {"unbound_uint16 + (1 + 2)", "unbound_uint16 + cast(3, 'UInt16')"},
                                 {"(1 + 2 + unbound_uint16) - (3 + 4 - unbound_uint16)", "(cast(3, 'UInt16') + unbound_uint16) - (cast(7, 'UInt16') - unbound_uint16)"},
                                 // undeterministic functions
                                 {"rand()", "rand()"},
                                 //// {"now()", "now()"},
                                 //// {"currentDatabase()", "currentDatabase()"},

                                 // lambda expression
                                 {"arrayMap(x -> (x + 2), [1, 2, 3])", "arrayMap(x -> (x + 2), [1, 2, 3])"},
                                 {"arrayMap(x -> (x + 2), unbound_int8_array)", "arrayMap(x -> (x + 2), unbound_int8_array)"},
                                 {"arrayMap(x -> (x + 2), arrayConcat([1, 2], [3, 4]))", "arrayMap(x -> (x + 2), [1, 2, 3, 4])"},
                                 {"length(arrayMap(x -> (x + 2), arrayConcat([1, 2], [3, 4]))) + (1 + 2)", "length(arrayMap(x -> (x + 2), [1, 2, 3, 4])) + cast(3, 'UInt16')"},

                                 /* functions not evaluate */
                                 {"arrayJoin([1, 2, 3])", "arrayJoin([1, 2, 3])"},

                                 /* IN statement */
                                 {"1 IN (1, 2, 3)", "1"},
                                 {"4 NOT IN (1, 2, 3)", "1"},
                                 {"(1, 2) IN ((1, 2), (2, 3), (3, 4))", "1"},
                                 {"(1, (1, 2)) IN ((1, (2, 3)), (2, (1, 2)))", "0"},
                                 {"1 IN (1)", "1"},
                                 {"(1, 2) IN (1, 2)", "1"},
                                 {"CAST('2022-01-02', 'Date') IN ('2022-01-01', '2022-01-02', '2022-01-03')", "1"},
                                 {"unbound_uint8 IN (1, 2, 3)", "unbound_uint8 IN (1, 2, 3)"},
                                 {"(1 + 2 + unbound_uint8) IN (1, 2, 3)", "(cast(3, 'UInt16') + unbound_uint8) IN (1, 2, 3)"},

                                 /* identifier substitution */
                                 {"1 + bound_uint8", "cast(2, 'UInt16')"},
                                 {"1 + bound_uint8 + 2 + unbound_uint8", "cast(4, 'UInt32') + unbound_uint8"},
                             }));

INSTANTIATE_TEST_SUITE_P(TestExpressionInterpreter,
                         OptimizePredicateTest,
                         ::testing::ValuesIn(
                             std::initializer_list<ExprTestCase> {
                                 /* return type check */
                                 {"unbound_string = 'foo'", "unbound_string = 'foo'"},
                                 {"unbound_string = CAST('foo', 'Nullable(String)')", "unbound_string = cast('foo', 'Nullable(String)')"},
                                 {"unbound_string = CAST('foo', 'LowCardinality(String)')", "unbound_string = toLowCardinality(cast('foo', 'String'))"},
                                 {"unbound_string = CAST('foo', 'LowCardinality(Nullable(String))')", "unbound_string = toLowCardinality(cast('foo', 'Nullable(String)'))"},

                                 /* IN statement */
                                 // IN statement simplify
                                 {"unbound_uint8 IN (1, 1, 2, 2, 2, 3)", "unbound_uint8 IN (1, 2, 3)"},
                                 {"unbound_string IN ('foo', 'foo', 'bar')", "unbound_string IN ('foo', 'bar')"},
                                 {"unbound_date IN ('2022-01-01', '2022-01-01', '2022-01-02')", "unbound_date IN (cast(18993, 'Date'), cast(18994, 'Date'))"},
                                 {"unbound_uint32 IN (1, 1, 1)", "unbound_uint32 = 1"},
                                 {"unbound_string NOT IN ('foo', 'foo', 'foo')", "unbound_string != 'foo'"},

                                 /* null simplify */
                                 {"unbound_uint8 = NULL", "0"},
                                 {"unbound_uint8 = NULL + unbound_uint32", "0"},
                                 {"isNull(unbound_uint8 = NULL + unbound_uint32)", "1"},
                                 {"coalesce(unbound_uint8 = NULL + unbound_uint32, unbound_uint8)", "coalesce(NULL, unbound_uint8)"},

                                 /* function simplify */
                                 // AND
                                 {"unbound_uint8 AND 0", "0"},
                                 {"unbound_uint8 AND 1", "unbound_uint8"},
                                 {"unbound_uint8 AND NULL", "unbound_uint8 AND NULL"},
                                 {"unbound_uint8 AND 1 AND 0", "0"},
                                 {"unbound_uint8 AND 1 AND NULL", "unbound_uint8 AND NULL"},
                                 {"unbound_uint8 AND 0 AND NULL", "0"},
                                 {"unbound_uint8 AND unbound_uint16 AND 0", "0"},
                                 {"unbound_uint8 AND unbound_uint16 AND 1", "unbound_uint8 AND unbound_uint16"},
                                 {"unbound_uint8 AND unbound_uint16 AND NULL", "unbound_uint8 AND unbound_uint16 AND NULL"},
                                 {"unbound_uint32 AND 1", "cast(unbound_uint32, 'UInt8')"},
                                 {"unbound_nullable_uint32 AND 1", "cast(unbound_nullable_uint32, 'Nullable(UInt8)')"},
                                 // OR
                                 {"unbound_uint8 OR 0", "unbound_uint8"},
                                 {"unbound_uint8 OR 1", "1"},
                                 {"unbound_uint8 OR NULL", "unbound_uint8 OR NULL"},
                                 {"unbound_uint8 OR 1 OR 0", "1"},
                                 {"unbound_uint8 OR 1 OR NULL", "1"},
                                 {"unbound_uint8 OR 0 OR NULL", "unbound_uint8 OR NULL"},
                                 {"unbound_uint8 OR unbound_uint16 OR 0", "unbound_uint8 OR unbound_uint16"},
                                 {"unbound_uint8 OR unbound_uint16 OR 1", "1"},
                                 {"unbound_uint8 OR unbound_uint16 OR NULL", "unbound_uint8 OR unbound_uint16 OR NULL"},
                                 {"unbound_uint32 OR 0", "cast(unbound_uint32, 'UInt8')"},
                                 {"unbound_nullable_uint32 OR 0", "cast(unbound_nullable_uint32, 'Nullable(UInt8)')"},
                                 // isNull/isNotNull
                                 {"isNull(unbound_uint8)", "0"},
                                 {"isNotNull(unbound_uint8)", "1"},
                                 {"isNull(unbound_nullable_uint8)", "isNull(unbound_nullable_uint8)"},
                                 {"isNotNull(unbound_nullable_uint8)", "isNotNull(unbound_nullable_uint8)"},
                                 // trivial comparison
                                 {"unbound_uint8 = unbound_uint8", "1"},
                                 {"unbound_uint8 = unbound_uint16", "unbound_uint8 = unbound_uint16"},
                                 // if
//                                 {"if(isNull(unbound_nullable_uint8), NULL, cast(multiIf(`build_side_non_null_symbol` = 1, 1, NULL, 0, 0), 'UInt8'))", ""},
                                 {"if(unbound_nullable_uint8, NULL, 0)", "0"},
                                 {"if(isNull(unbound_nullable_uint8), NULL, 1)", "isNotNull(unbound_nullable_uint8)"},
                                 {"if(isNull(unbound_uint8), NULL, 1)", "1"},
                             }));

INSTANTIATE_TEST_SUITE_P(DISABLED_DebugExpressionInterpreter,
                         OptimizeExpressionTest,
                         ::testing::ValuesIn(
                             std::initializer_list<ExprTestCase> {
                                 {std::getenv("Q") == nullptr ? "1" : std::getenv("Q"), "1"},
                             }));

INSTANTIATE_TEST_SUITE_P(DISABLED_DebugExpressionInterpreter,
                         OptimizePredicateTest,
                         ::testing::ValuesIn(
                             std::initializer_list<ExprTestCase> {
                                 {std::getenv("Q") == nullptr ? "1" : std::getenv("Q"), "1"}
                             }));
