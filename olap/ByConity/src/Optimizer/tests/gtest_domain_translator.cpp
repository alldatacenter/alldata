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
#include "Optimizer/value_sets.h"
#include "Optimizer/FunctionInvoker.h"
#include "Optimizer/DomainTranslator.h"
#include <DataTypes/DataTypesDecimal.h>
#include <Analyzers/ASTEquals.h>
#include <Optimizer/LiteralEncoder.h>
#include <Common/tests/gtest_global_register.h>
#include <Common/tests/gtest_global_context.h>
namespace DB::Predicate
{

static bool flag = false;

void initContextFlag()
{
    if (!flag)
    {
        flag = true;
        tryRegisterFunctions();
        tryRegisterFormats();
        tryRegisterStorages();
        tryRegisterAggregateFunctions();
    }
}

auto typeFromString(const std::string & str)
{
    auto & data_type_factory = DataTypeFactory::instance();
    return data_type_factory.get(str);
};

class DomainTranslatorTest : public testing::Test
{
public:
    DomainTranslatorTest();
    ~DomainTranslatorTest() { }
    void SetUp() { }
    void TearDown() { }

    ExtractionResult fromPredicate(ASTPtr original_predicate, ContextMutablePtr context);
    bool isEqual(const ConstASTPtr & a, const ConstASTPtr & b) { return ASTEquality::compareTree(a, b); }
    void assertPredicateTranslates(
        const ASTPtr & node, const TupleDomain & tupleDomain, const ASTPtr & remaining_expression, ContextMutablePtr context);
    void assertPredicateIsAlwaysFalse(const ASTPtr & node, ContextMutablePtr context);
    void assertPredicateIsAlwaysTrue(const ASTPtr & node, ContextMutablePtr context);
    void assertUnsupportedPredicate(const ASTPtr & node, ContextMutablePtr context);
    void testInPredicate(String symbol1, DataTypePtr type, const Field & one, const Field & two, ContextMutablePtr context);
    void testInPredicateWithFloatingPoint(
        String symbol_1, DataTypePtr type, const Field & one, const Field & two, const Field & nan, ContextMutablePtr context);
    void testSimpleComparison(
        ASTPtr node, const String & symbol, ASTPtr expect_remaining_expression, const Domain & expectedDomain, ContextMutablePtr context);
    static ASTPtr toPredicate(const TupleDomain & tuple_domain, DomainTranslator & domain_translator)
    {
        return domain_translator.toPredicate(tuple_domain);
    }

    ASTs appendTypesForLiterals(DataTypePtr type, ASTs asts);

    static ASTPtr notInF(const String & symbol, ASTPtr ast);
    static ASTPtr inF(const String & symbol, ASTPtr ast);
    static ASTPtr equalsF(const String & symbol, ASTPtr ast);
    static ASTPtr notEqualsF(const String & symbol, ASTPtr ast);
    static ASTPtr lessF(const String & symbol, ASTPtr ast);
    static ASTPtr lessOrEqualsF(const String & symbol, ASTPtr ast);
    static ASTPtr greaterF(const String & symbol, ASTPtr ast);
    static ASTPtr greaterOrEqualsF(const String & symbol, ASTPtr ast);
    static ASTPtr andF(const std::vector<ConstASTPtr> & predicates);
    static ASTPtr orF(const std::vector<ConstASTPtr> & predicates);
    static ASTPtr betweenF(const String & symbol, ASTPtr ast1, ASTPtr ast2);
    static ASTPtr isNotNullF(const String & symbol);
    static ASTPtr isNullF(const String & symbol);
    static ASTPtr notF(ASTPtr ast);
    static ASTPtr inF(ASTPtr ast, ASTs in_list);
    static ASTPtr notInF(ASTPtr ast, ASTs in_list);
    static ASTPtr likeF(const String & Symbol, const String & target);
    static ASTPtr startsWithF(const String & symbol, const String & target);

    ASTPtr getUnProcessableExpression1(const String & symbol);
    ASTPtr getUnProcessableExpression2(const String & symbol);

    Field field_0 = Int64(0);
    Field field_1 = Int64(1);
    Field field_2 = Int64(2);
    Field field_3 = Int64(3);
    Field field_4 = Int64(4);
    Field field_5 = Int64(5);
    Field field_6 = Int64(6);
    Field field_7 = Int64(7);
    Field field_9 = Int64(9);
    Field field_10 = Int64(10);
    Field field_11 = Int64(11);
    Field field_13 = Field(Int64(13));
    Field field_20 = Field(Int64(20));

    Field field_1_int8 = Int8(1);
    Field field_2_int8 = Int8(2);

    Field field_0_float64 = Field(Float64(0.0));
    Field field_1_float64 = Field(Float64(1.0));
    Field field_2_float64 = Field(Float64(2.0));
    Field field_3_float64 = Field(Float64(3.0));
    Field field_5_float64 = Field(Float64(5.0));

    Field field_0_float32 = Field(Float32(0.0));
    Field field_1_float32 = Field(Float32(1.0));
    Field field_2_float32 = Field(Float32(2.0));
    Field field_5_float32 = Field(Float32(5.0));

    // init data_type
    DataTypePtr int8_type = typeFromString("Int8");
    DataTypePtr int16_type = typeFromString("Int16");
    DataTypePtr int32_type = typeFromString("Int32");
    DataTypePtr int64_type = typeFromString("Int64");
    DataTypePtr uint8_type = typeFromString("UInt8");
    DataTypePtr uint16_type = typeFromString("UInt16");
    DataTypePtr uint32_type = typeFromString("UInt32");
    DataTypePtr uint64_type = typeFromString("UInt64");

    DataTypePtr float32_type = typeFromString("Float32");
    DataTypePtr float64_type = typeFromString("Float64");
    DataTypePtr decimal_12_2_type = createDecimal<DataTypeDecimal>(12, 2);
    DataTypePtr decimal_6_1_type = createDecimal<DataTypeDecimal>(6, 1);
    DataTypePtr decimal_38_3_type = createDecimal<DataTypeDecimal>(38, 3);
    DataTypePtr decimal_3_0_type = createDecimal<DataTypeDecimal>(3, 0);
    DataTypePtr decimal_2_0_type = createDecimal<DataTypeDecimal>(2, 0);

    DataTypePtr date_type = typeFromString("Date");
    DataTypePtr date_time_type = typeFromString("DateTime");
    DataTypePtr date_time64_type = typeFromString("DateTime64");
    //DataTypePtr createDecimal(UInt64 precision_value, UInt64 scale_value);

    DataTypePtr string_type = typeFromString("String");

    //init symbol
    String int8_sym = "int8_sym";
    String int16_sym = "int16_sym";
    String int32_sym = "int32_sym";
    String int64_sym = "int64_sym";
    String uint16_sym = "uint16_sym";
    String uint32_sym = "uint32_sym";
    String uint64_sym = "uint64_sym";
    String int64_sym_1 = "int64_sym_1";
    String int32_sym_1 = "int32_sym_1";

    String float32_sym = "float32_sym";
    String float64_sym = "float64_sym";
    String float64_sym_1 = "float64_sym_1";
    String decimal_12_2_sym = "decimal_12_2_sym";
    String decimal_6_1_sym = "decimal_6_1_sym";
    String decimal_38_3_sym = "decimal_38_3_sym";
    String decimal_3_0_sym = "decimal_3_0_sym";
    String decimal_2_0_sym = "decimal_2_0_sym";

    String date_sym = "date_sym";
    String date_time_sym = "date_time_sym";
    String date_time64_sym = "date_time64_sym";

    String string_sym = "string_sym";
    String string_sym_1 = "string_sym_1";
    String bool_sym = "bool_sym";

    NamesAndTypes names_and_types;

    ASTPtr liter_0 = std::make_shared<ASTLiteral>(0);
    ASTPtr liter_1 = std::make_shared<ASTLiteral>(1);
    ASTPtr liter_2 = std::make_shared<ASTLiteral>(2);
    ASTPtr liter_3 = std::make_shared<ASTLiteral>(3);
    ASTPtr liter_4 = std::make_shared<ASTLiteral>(4);
    ASTPtr liter_5 = std::make_shared<ASTLiteral>(5);
    ASTPtr liter_7 = std::make_shared<ASTLiteral>(7);
    ASTPtr liter_9 = std::make_shared<ASTLiteral>(9);
    ASTPtr liter_11 = std::make_shared<ASTLiteral>(11);
    ASTPtr liter_13 = std::make_shared<ASTLiteral>(13);
};

TEST_F(DomainTranslatorTest, testAllRoundTrip)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    DomainTranslator domain_translator = DomainTranslator(context);
    TupleDomain tuple_domain = TupleDomain::all();
    ExtractionResult result = fromPredicate(toPredicate(tuple_domain, domain_translator), context);
    ASSERT_TRUE(isEqual(result.remaining_expression, PredicateConst::TRUE_VALUE));
    ASSERT_EQ(result.tuple_domain, tuple_domain);
}

TEST_F(DomainTranslatorTest, testNoneRoundTrip)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    DomainTranslator domain_translator = DomainTranslator(context);

    TupleDomain tuple_domain = TupleDomain::none();
    ExtractionResult result = fromPredicate(toPredicate(tuple_domain, domain_translator), context);
    ASSERT_TRUE(isEqual(result.remaining_expression, PredicateConst::TRUE_VALUE));
    ASSERT_EQ(result.tuple_domain, tuple_domain);
}

TEST_F(DomainTranslatorTest, testRoundTrip)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    DomainTranslator domain_translator = DomainTranslator(context);

    TupleDomain tuple_domain = TupleDomain(DomainMap{
        {int64_sym, Domain::singleValue(int64_type, Int64(1))},
        {float64_sym, Domain::onlyNull(float64_type)},
        {string_sym, Domain::notNull(string_type)},
        {int64_sym_1, Domain::singleValue(int64_type, Int64(2))},
        {float64_sym_1,
         Domain(
             createValueSet(
                 {Range::lessThanOrEqualRange(float64_type, Float64(1.1)),
                  Range::equalRange(float64_type, Float64(2.0)),
                  Range(float64_type, false, Float64(3.0), true, Float64(3.5))}),
             true)},
        {string_sym_1,
         Domain(
             createValueSet({Range::lessThanOrEqualRange(string_type, "2013-01-01"), Range::greaterThanRange(string_type, "2013-10-01")}),
             false)},
        //TODO: datetime
    });

    assertPredicateTranslates(toPredicate(tuple_domain, domain_translator), tuple_domain, PredicateConst::TRUE_VALUE, context);
}

TEST_F(DomainTranslatorTest, testInOptimization)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    DomainTranslator domain_translator = DomainTranslator(context);

    ASTs literals = {liter_1, liter_2, liter_3};
    Domain test_domain = Domain(
        subtractValueSet(
            createAll(int64_type),
            createValueSet(
                {Range::equalRange(int64_type, field_1), Range::equalRange(int64_type, field_2), Range::equalRange(int64_type, field_3)})),
        false);

    TupleDomain tuple_domain = TupleDomain(DomainMap{{int64_sym, test_domain}});
    ASTPtr res = toPredicate(tuple_domain, domain_translator);
    ASSERT_TRUE(isEqual(res, notInF(int64_sym, makeASTFunction("tuple", literals))));

    //test2
    test_domain = Domain(
        createValueSet(
            {Range(int64_type, true, field_1, true, field_3),
             Range(int64_type, true, field_5, true, field_7),
             Range(int64_type, true, field_9, true, field_11)}),
        false);

    tuple_domain = TupleDomain(DomainMap{{int64_sym, test_domain}});
    ASSERT_TRUE(isEqual(
        toPredicate(tuple_domain, domain_translator),
        PredicateUtils::combineDisjuncts(ConstASTs{
            betweenF(int64_sym, liter_1, liter_3), betweenF(int64_sym, liter_5, liter_7), betweenF(int64_sym, liter_9, liter_11)})));

    //test3
    test_domain = Domain(
        unionValueSet(
            intersectValueSet(
                createValueSet({Range::lessThanRange(int64_type, field_4)}),
                subtractValueSet(
                    createAll(int64_type),
                    createValueSet(
                        {Range::equalRange(int64_type, field_1),
                         Range::equalRange(int64_type, field_2),
                         Range::equalRange(int64_type, field_3)}))),
            createValueSet({Range(int64_type, true, field_7, true, field_9)})),
        false);

    tuple_domain = TupleDomain(DomainMap{{int64_sym, test_domain}});
    ASTPtr to_predicate = toPredicate(tuple_domain, domain_translator);
    ASTPtr expected_res = orF(
        {andF({lessF(int64_sym, liter_4), notInF(int64_sym, makeASTFunction("tuple", literals))}), betweenF(int64_sym, liter_7, liter_9)});
    ASSERT_TRUE(isEqual(to_predicate, expected_res));

    //test4
    test_domain = Domain(
        unionValueSet(
            intersectValueSet(
                createValueSet({Range::lessThanRange(int64_type, field_4)}),
                subtractValueSet(
                    createAll(int64_type),
                    createValueSet(
                        {Range::equalRange(int64_type, field_1),
                         Range::equalRange(int64_type, field_2),
                         Range::equalRange(int64_type, field_3)}))),
            createValueSet({Range(int64_type, false, field_7, false, field_9), Range(int64_type, false, field_11, false, field_13)})),
        false);
    //Note: the order of asts in 'predicate compression' should be consistent
    tuple_domain = TupleDomain(DomainMap{{int64_sym, test_domain}});
    to_predicate = toPredicate(tuple_domain, domain_translator);
    expected_res = orF(
        {andF({lessF(int64_sym, liter_4), notInF(int64_sym, makeASTFunction("tuple", literals))}),
         andF({greaterF(int64_sym, liter_7), lessF(int64_sym, liter_9)}),
         andF({greaterF(int64_sym, liter_11), lessF(int64_sym, liter_13)})});
    ASSERT_TRUE(isEqual(to_predicate, expected_res));
}
TEST_F(DomainTranslatorTest, testToPredicateNone)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    DomainTranslator domain_translator = DomainTranslator(context);

    TupleDomain tuple_domain = TupleDomain(DomainMap{
        {int64_sym, Domain::singleValue(int64_type, field_1)},
        {float64_sym, Domain::onlyNull(float64_type)},
        {string_sym, Domain::notNull(string_type)},
        {string_sym_1, Domain::none(string_type)}});

    ASTPtr res = toPredicate(tuple_domain, domain_translator);
    ASSERT_TRUE(isEqual(res, DB::PredicateConst::FALSE_VALUE));
}

TEST_F(DomainTranslatorTest, testToPredicateAllIgnored)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    DomainTranslator domain_translator = DomainTranslator(context);

    TupleDomain tuple_domain = TupleDomain(DomainMap{
        {int64_sym, Domain::singleValue(int64_type, field_1)},
        {float64_sym, Domain::onlyNull(float64_type)},
        {string_sym, Domain::notNull(string_type)},
        {string_sym_1, Domain::all(string_type)}});

    TupleDomain expect_tuple_domain = TupleDomain(DomainMap{
        {int64_sym, Domain::singleValue(int64_type, field_1)},
        {float64_sym, Domain::onlyNull(float64_type)},
        {string_sym, Domain::notNull(string_type)}});


    ExtractionResult result = fromPredicate(toPredicate(tuple_domain, domain_translator), context);
    ASSERT_TRUE(isEqual(result.remaining_expression, PredicateConst::TRUE_VALUE));
    ASSERT_EQ(result.tuple_domain, expect_tuple_domain);
}


TEST_F(DomainTranslatorTest, testToPredicate)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    DomainTranslator domain_translator = DomainTranslator(context);

    TupleDomain tuple_domain = TupleDomain(DomainMap{{int64_sym, Domain::notNull(int64_type)}});
    ASSERT_TRUE(isEqual(toPredicate(tuple_domain, domain_translator), isNotNullF(int64_sym)));

    tuple_domain = TupleDomain(DomainMap{{int64_sym, Domain::onlyNull(int64_type)}});
    ASSERT_TRUE(isEqual(toPredicate(tuple_domain, domain_translator), isNullF(int64_sym)));

    tuple_domain = TupleDomain(DomainMap{{int64_sym, Domain::none(int64_type)}});
    ASSERT_TRUE(isEqual(toPredicate(tuple_domain, domain_translator), PredicateConst::FALSE_VALUE));

    tuple_domain = TupleDomain(DomainMap{{int64_sym, Domain::all(int64_type)}});
    ASSERT_TRUE(isEqual(toPredicate(tuple_domain, domain_translator), PredicateConst::TRUE_VALUE));

    tuple_domain = TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::greaterThanRange(int64_type, field_1)}), false)}});
    ASSERT_TRUE(isEqual(toPredicate(tuple_domain, domain_translator), greaterF(int64_sym, liter_1)));

    tuple_domain
        = TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::greaterThanOrEqualRange(int64_type, field_1)}), false)}});
    ASSERT_TRUE(isEqual(toPredicate(tuple_domain, domain_translator), greaterOrEqualsF(int64_sym, liter_1)));

    tuple_domain = TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::lessThanRange(int64_type, field_1)}), false)}});
    ASSERT_TRUE(isEqual(toPredicate(tuple_domain, domain_translator), lessF(int64_sym, liter_1)));

    tuple_domain = TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range(int64_type, false, field_0, true, field_1)}), false)}});
    ASSERT_TRUE(
        isEqual(toPredicate(tuple_domain, domain_translator), andF({greaterF(int64_sym, liter_0), lessOrEqualsF(int64_sym, liter_1)})));

    tuple_domain = TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::lessThanOrEqualRange(int64_type, field_1)}), false)}});
    ASSERT_TRUE(isEqual(toPredicate(tuple_domain, domain_translator), lessOrEqualsF(int64_sym, liter_1)));

    tuple_domain = TupleDomain(DomainMap{{int64_sym, Domain::singleValue(int64_type, field_1)}});
    ASSERT_TRUE(isEqual(toPredicate(tuple_domain, domain_translator), equalsF(int64_sym, liter_1)));

    ASTs temp = {liter_1, liter_2};
    tuple_domain = TupleDomain(DomainMap{
        {int64_sym, Domain(createValueSet({Range::equalRange(int64_type, field_1), Range::equalRange(int64_type, field_2)}), false)}});
    ASSERT_TRUE(isEqual(toPredicate(tuple_domain, domain_translator), inF(int64_sym, makeASTFunction("tuple", temp))));

    tuple_domain = TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::lessThanRange(int64_type, field_1)}), true)}});
    ASSERT_TRUE(isEqual(toPredicate(tuple_domain, domain_translator), orF({lessF(int64_sym, liter_1), isNullF(int64_sym)})));
}


TEST_F(DomainTranslatorTest, testToPredicateWithRangeOptimisation)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    DomainTranslator domain_translator = DomainTranslator(context);
    ASTPtr liter_0_float64 = std::make_shared<ASTLiteral>(field_0_float64);
    ASTPtr liter_1_float64 = std::make_shared<ASTLiteral>(field_1_float64);
    ASTPtr liter_2_float64 = std::make_shared<ASTLiteral>(field_2_float64);
    ASTPtr liter_3_float64 = std::make_shared<ASTLiteral>(field_3_float64);

    //test1
    TupleDomain tuple_domain = TupleDomain(DomainMap{
        {int64_sym,
         Domain(createValueSet({Range::greaterThanRange(int64_type, field_1), Range::lessThanRange(int64_type, field_1)}), false)}});
    ASSERT_TRUE(isEqual(toPredicate(tuple_domain, domain_translator), notEqualsF(int64_sym, liter_1)));

    //test2
    tuple_domain = TupleDomain(DomainMap{
        {int64_sym,
         Domain(
             createValueSet(
                 {Range::lessThanRange(int64_type, field_0),
                  Range(int64_type, false, field_0, false, field_1),
                  Range::greaterThanRange(int64_type, field_1)}),
             false)}});
    ASSERT_TRUE(isEqual(toPredicate(tuple_domain, domain_translator), notInF(int64_sym, makeASTFunction("tuple", ASTs{liter_0, liter_1}))));

    //test3
    tuple_domain = TupleDomain(DomainMap{
        {int64_sym,
         Domain(
             createValueSet(
                 {Range::lessThanRange(int64_type, field_0),
                  Range(int64_type, false, field_0, false, field_1),
                  Range::greaterThanRange(int64_type, field_2)}),
             false)}});
    ASSERT_TRUE(isEqual(
        toPredicate(tuple_domain, domain_translator),
        orF({andF({lessF(int64_sym, liter_1), notEqualsF(int64_sym, liter_0)}), greaterF(int64_sym, liter_2)})));

    /*
    test4 todo: not support
        // floating point types: do not coalesce ranges when range "all" would be introduced
        ASTPtr liter_0_float32 = std::make_shared<ASTLiteral>(Float32(0.0));
        ASTPtr liter_1_float32 = std::make_shared<ASTLiteral>(Float32(1.0));
        ASTPtr liter_2_float32 = std::make_shared<ASTLiteral>(Float32(2.0));

        tuple_domain = TupleDomain(DomainMap{{
            float32_sym,
            Domain(
                createValueSet({
                    Range::greaterThanRange(float32_type, field_0_float32),
                    Range::lessThanRange(float32_type, field_0_float32)}),
                false)}});

        ASSERT_TRUE(isEqual(
            toPredicate(tuple_domain, domain_translator),
            orF({lessF(float32_sym, liter_0_float32), greaterF(float32_sym, liter_0_float32)})));

        tuple_domain = TupleDomain(DomainMap{{
            float32_sym,
            Domain(
                createValueSet({
                    Range::lessThanRange(float32_type, field_0_float32),
                    Range(float32_type, false, field_0_float32, false, field_1_float32),
                    Range::greaterThanRange(float32_type, field_1_float32)}),
                false)}});

        ASSERT_TRUE(isEqual(
            toPredicate(tuple_domain, domain_translator),
            orF({
                lessF(float32_sym, liter_0_float32),
                andF({greaterF(float32_sym, liter_0_float32), lessF(float32_sym, liter_1_float32)}),
                greaterF(float32_sym, liter_1_float32)})));

        tuple_domain = TupleDomain(DomainMap{{
            float32_sym,
            Domain(
                createValueSet({
                    Range::lessThanRange(float32_type, field_0_float32),
                    Range(float32_type, false, field_0_float32, false, field_1_float32),
                    Range::greaterThanRange(float32_type, field_2_float32)}),
                false)}});

        ASSERT_TRUE(isEqual(
            toPredicate(tuple_domain, domain_translator),
            orF({
                andF({lessF(float32_sym, liter_1_float32), notEqualsF(float32_sym, liter_0_float32)}),
                greaterF(float32_sym, liter_2_float32)})));

        tuple_domain = TupleDomain(DomainMap{{
            float64_sym,
            Domain(
                createValueSet(
                    {Range::lessThanRange(float64_type, field_0_float64),
                     Range(float64_type, false, field_0_float64, false, field_1_float64),
                     Range(float64_type, false, field_2_float64, false, field_3_float64),
                     Range::greaterThanRange(float64_type, field_3_float64)}),
                false)}});

        ASSERT_TRUE(isEqual(
            toPredicate(tuple_domain, domain_translator),
            orF(
                {andF({lessF(float64_sym, liter_1_float64), notEqualsF(float64_sym, liter_0_float64)}),
                 andF({greaterF(float64_sym, liter_2_float64), notEqualsF(float64_sym, liter_3_float64)})})));*/
}

TEST_F(DomainTranslatorTest, testFromUnknownPredicate)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    assertPredicateTranslates(getUnProcessableExpression1(int64_sym), TupleDomain::all(), getUnProcessableExpression1(int64_sym), context);
    assertPredicateTranslates(
        notF(getUnProcessableExpression1(int64_sym)), TupleDomain::all(), notF(getUnProcessableExpression1(int64_sym)), context);
}

TEST_F(DomainTranslatorTest, testFromAndPredicate)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    ASTPtr liter_1_int64 = std::make_shared<ASTLiteral>(field_1);
    ASTPtr liter_5_int64 = std::make_shared<ASTLiteral>(field_5);

    ASTPtr original_predicate = andF(
        {andF({greaterF(int64_sym, liter_1_int64), getUnProcessableExpression1(int64_sym)}),
         andF({lessF(int64_sym, liter_5_int64), getUnProcessableExpression2(int64_sym)})});
    ASTPtr expect_remaining_expression = andF({getUnProcessableExpression1(int64_sym), getUnProcessableExpression2(int64_sym)});
    TupleDomain expect_tuple_domain
        = TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range(int64_type, false, field_1, false, field_5)}), false)}});
    assertPredicateTranslates(original_predicate, expect_tuple_domain, expect_remaining_expression, context);

    // Test complements
    original_predicate = notF(andF(
        {andF({greaterF(int64_sym, liter_1_int64), getUnProcessableExpression1(int64_sym)}),
         andF({lessF(int64_sym, liter_5_int64), getUnProcessableExpression2(int64_sym)})}));
    assertPredicateTranslates(original_predicate, TupleDomain::all(), original_predicate, context);
    //
    //    original_predicate = notF(andF(
    //        {notF(andF({greaterF(int64_sym, liter_1_int64), getUnProcessableExpression1(int64_sym)})),
    //         notF(andF({lessF(int64_sym, liter_5_int64), getUnProcessableExpression2(int64_sym)}))}));
    //    expect_tuple_domain = TupleDomain(DomainMap{{int64_sym, Domain::notNull(int64_type)}});
    //    assertPredicateTranslates(original_predicate, expect_tuple_domain, original_predicate, context);
    original_predicate = notF(andF(
        {notF(andF({greaterF(int64_sym, liter_1_int64), getUnProcessableExpression1(int64_sym)})),
         notF(andF({lessF(int64_sym, liter_5_int64), getUnProcessableExpression2(int64_sym)}))}));
    ExtractionResult result = fromPredicate(original_predicate, context);
    ASSERT_TRUE(isEqual(result.remaining_expression, original_predicate));
    ASSERT_EQ(result.tuple_domain, TupleDomain(DomainMap{{int64_sym, Domain::notNull(int64_type)}}));
}


TEST_F(DomainTranslatorTest, testFromOrPredicate)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    ASTPtr liter_1_int64 = std::make_shared<ASTLiteral>(field_1);
    ASTPtr liter_2_int64 = std::make_shared<ASTLiteral>(field_2);
    ASTPtr liter_5_int64 = std::make_shared<ASTLiteral>(field_5);
    ASTPtr liter_10_int64 = std::make_shared<ASTLiteral>(field_10);
    ASTPtr liter_20_int64 = std::make_shared<ASTLiteral>(field_20);
    ASTPtr liter_1_float64 = std::make_shared<ASTLiteral>(field_1_float64);
    ASTPtr liter_2_float64 = std::make_shared<ASTLiteral>(field_2_float64);
    ASTPtr liter_5_float64 = std::make_shared<ASTLiteral>(field_5_float64);
    ASTPtr liter_2_float32 = std::make_shared<ASTLiteral>(field_2_float32);
    ASTPtr liter_5_float32 = std::make_shared<ASTLiteral>(field_5_float32);
    ASTPtr expect_remaining_expression = nullptr;

    ASTPtr original_predicate = orF(
        {andF({greaterF(int64_sym, liter_1_int64), getUnProcessableExpression1(int64_sym)}),
         andF({lessF(int64_sym, liter_5_int64), getUnProcessableExpression2(int64_sym)})});
    TupleDomain expect_tuple_domain = TupleDomain(DomainMap{{int64_sym, Domain::notNull(int64_type)}});
    assertPredicateTranslates(original_predicate, expect_tuple_domain, original_predicate, context);

    original_predicate = orF(
        {andF({equalsF(string_sym, std::make_shared<ASTLiteral>("abc"))}),
         andF({equalsF(string_sym, std::make_shared<ASTLiteral>("def"))})});
    expect_tuple_domain = TupleDomain(DomainMap{
        {string_sym, Domain(createValueSet({Range::equalRange(string_type, "abc"), Range::equalRange(string_type, "def")}), false)}});
    assertPredicateTranslates(original_predicate, expect_tuple_domain, PredicateConst::TRUE_VALUE, context);

    original_predicate = orF(
        {andF({equalsF(int64_sym, liter_1_int64), getUnProcessableExpression1(int64_sym)}),
         andF({equalsF(int64_sym, liter_2_int64), getUnProcessableExpression2(int64_sym)})});
    expect_tuple_domain = TupleDomain(DomainMap{
        {int64_sym, Domain(createValueSet({Range::equalRange(int64_type, field_1), Range::equalRange(int64_type, field_2)}), false)}});
    assertPredicateTranslates(original_predicate, expect_tuple_domain, original_predicate, context);

    original_predicate = orF(
        {andF({lessF(int64_sym, liter_20_int64), getUnProcessableExpression1(int64_sym)}),
         andF({greaterF(int64_sym, liter_10_int64), getUnProcessableExpression2(int64_sym)})});
    expect_tuple_domain = TupleDomain(DomainMap{{int64_sym, Domain(createAll(int64_type), false)}});
    assertPredicateTranslates(original_predicate, expect_tuple_domain, original_predicate, context);

    // Same unprocessableExpression means that we can do more extraction
    // If both sides are operating on the same single symbol
    original_predicate = orF(
        {andF({equalsF(int64_sym, liter_1_int64), getUnProcessableExpression1(int64_sym)}),
         andF({equalsF(int64_sym, liter_2_int64), getUnProcessableExpression1(int64_sym)})});
    expect_tuple_domain = TupleDomain(DomainMap{
        {int64_sym, Domain(createValueSet({Range::equalRange(int64_type, field_1), Range::equalRange(int64_type, field_2)}), false)}});
    assertPredicateTranslates(original_predicate, expect_tuple_domain, getUnProcessableExpression1(int64_sym), context);

    // And notF if they have different symbols
    original_predicate = orF(
        {andF({equalsF(int64_sym, liter_1_int64), getUnProcessableExpression1(int64_sym)}),
         andF({equalsF(float64_sym, liter_2_float64), getUnProcessableExpression1(int64_sym)})});
    assertPredicateTranslates(original_predicate, TupleDomain::all(), original_predicate, context);

    // Domain union implicitly adds NaN as an accepted value
    // The original predicate is returned as the RemainingExpression
    // (even if left andF {right unprocessableExpressions are the same)
    original_predicate = orF({greaterF(float64_sym, liter_2_float64), lessF(float64_sym, liter_5_float64)});
    expect_tuple_domain = TupleDomain(DomainMap{{float64_sym, Domain::notNull(float64_type)}});
    assertPredicateTranslates(original_predicate, expect_tuple_domain, original_predicate, context);

    /*TODO: false
    original_predicate = orF({
        greaterF(float32_sym, liter_2_float32),
        lessF(float32_sym, liter_5_float32),
        isNullF(float32_sym)});
    assertPredicateTranslates(
        original_predicate,
        TupleDomain::all(),
        original_predicate,
        context);*/

    original_predicate = orF(
        {andF({greaterF(float64_sym, liter_2_float64), getUnProcessableExpression1(float64_sym)}),
         andF({lessF(float64_sym, liter_5_float64), getUnProcessableExpression1(float64_sym)})});
    expect_tuple_domain = TupleDomain(DomainMap{{float64_sym, Domain::notNull(float64_type)}});
    assertPredicateTranslates(original_predicate, expect_tuple_domain, original_predicate, context);

    /*todo:
    original_predicate = orF({
        andF({greaterF(float32_sym, liter_2_float32), getUnProcessableExpression1(float32_sym)}),
        andF({lessF(float32_sym, liter_5_float32), getUnProcessableExpression1(float32_sym)})});
    assertPredicateTranslates(
        original_predicate,
        TupleDomain(DomainMap{{float32_sym, Domain::notNull(float32_type)}}),
        original_predicate,
        context);*/

    // if both side's tuple domain are "all"
    original_predicate = orF({andF({getUnProcessableExpression1(int64_sym)}), andF({getUnProcessableExpression1(int64_sym)})});
    expect_tuple_domain = TupleDomain::all();
    assertPredicateTranslates(original_predicate, expect_tuple_domain, getUnProcessableExpression1(int64_sym), context);

    original_predicate = orF({andF({getUnProcessableExpression1(int64_sym)}), andF({getUnProcessableExpression2(int64_sym)})});
    expect_tuple_domain = TupleDomain::all();
    assertPredicateTranslates(original_predicate, expect_tuple_domain, original_predicate, context);

    // We can make another optimization if one side is the super set of the other side
    original_predicate = orF(
        {andF({greaterF(int64_sym, liter_1_int64), greaterF(float64_sym, liter_1_float64), getUnProcessableExpression1(int64_sym)}),
         andF({greaterF(int64_sym, liter_2_int64), greaterF(float64_sym, liter_2_float64), getUnProcessableExpression1(int64_sym)})});
    expect_tuple_domain = TupleDomain(DomainMap{
        {int64_sym, Domain(createValueSet({Range::greaterThanRange(int64_type, field_1)}), false)},
        {float64_sym, Domain(createValueSet({Range::greaterThanRange(float64_type, field_1_float64)}), false)}});
    assertPredicateTranslates(original_predicate, expect_tuple_domain, getUnProcessableExpression1(int64_sym), context);

    // We can't make those inferences if the unprocessableExpressions are non-deterministic
    /*TODO:
    original_predicate = orF({
        andF({equalsF(int64_sym, liter_1_int64), randPredicate(int64_sym, int64_type)}),
        andF({equalsF(int64_sym, liter_2_int64), randPredicate(int64_sym, int64_type)})});
    result = fromPredicate(original_predicate, context);
    ASSERT_TRUE(isEqual(result.remaining_expression, original_predicate));
    ASSERT_EQ(result.tuple_domain, TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::equalRange(int64_type, field_1), Range::equalRange(int64_type, field_2)}), false)}}));*/

    // Test complements
    original_predicate = notF(orF(
        {andF({greaterF(int64_sym, liter_1_int64), getUnProcessableExpression1(int64_sym)}),
         andF({lessF(int64_sym, liter_5_int64), getUnProcessableExpression2(int64_sym)})}));
    expect_remaining_expression = andF(
        {notF(andF({greaterF(int64_sym, liter_1_int64), getUnProcessableExpression1(int64_sym)})),
         notF(andF({lessF(int64_sym, liter_5_int64), getUnProcessableExpression2(int64_sym)}))});
    expect_tuple_domain = TupleDomain::all();
    assertPredicateTranslates(original_predicate, expect_tuple_domain, expect_remaining_expression, context);

    original_predicate = notF(orF(
        {notF(andF({greaterF(int64_sym, liter_1_int64), getUnProcessableExpression1(int64_sym)})),
         notF(andF({lessF(int64_sym, liter_5_int64), getUnProcessableExpression2(int64_sym)}))}));
    expect_tuple_domain
        = TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range(int64_type, false, field_1, false, field_5)}), false)}});
    expect_remaining_expression = andF({getUnProcessableExpression1(int64_sym), getUnProcessableExpression2(int64_sym)});
    assertPredicateTranslates(original_predicate, expect_tuple_domain, expect_remaining_expression, context);

    original_predicate
        = andF({greaterF(float64_sym, std::make_shared<ASTLiteral>(0)), greaterF(float64_sym, std::make_shared<ASTLiteral>(1.5))});
    expect_tuple_domain
        = TupleDomain(DomainMap{{float64_sym, Domain(createValueSet({Range(float64_type, false, Float64(1.5), false, Field())}), false)}});
    expect_remaining_expression = andF({getUnProcessableExpression1(int64_sym), getUnProcessableExpression2(int64_sym)});
    assertPredicateTranslates(original_predicate, expect_tuple_domain, PredicateConst::TRUE_VALUE, context);


    original_predicate
        = andF({greaterF(float64_sym, std::make_shared<ASTLiteral>(3)), greaterF(float64_sym, std::make_shared<ASTLiteral>(0.5))});
    expect_tuple_domain
        = TupleDomain(DomainMap{{float64_sym, Domain(createValueSet({Range(float64_type, false, Float64(3.0), false, Field())}), false)}});
    expect_remaining_expression = andF({getUnProcessableExpression1(int64_sym), getUnProcessableExpression2(int64_sym)});
    assertPredicateTranslates(original_predicate, expect_tuple_domain, PredicateConst::TRUE_VALUE, context);
}

TEST_F(DomainTranslatorTest, testFromNotPredicate)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    ASTPtr liter_1_int64 = std::make_shared<ASTLiteral>(field_1);

    ASTPtr test_ast = notF(andF({equalsF(int64_sym, liter_1_int64), getUnProcessableExpression1(int64_sym)}));
    assertPredicateTranslates(test_ast, TupleDomain::all(), test_ast, context);

    test_ast = notF(getUnProcessableExpression1(int64_sym));
    assertPredicateTranslates(test_ast, TupleDomain::all(), test_ast, context);

    assertPredicateIsAlwaysFalse(notF(PredicateConst::TRUE_VALUE), context);

    assertPredicateTranslates(
        notF(equalsF(int64_sym, liter_1_int64)),
        TupleDomain(DomainMap{
            {int64_sym,
             Domain(createValueSet({Range::lessThanRange(int64_type, field_1), Range::greaterThanRange(int64_type, field_1)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);
}

TEST_F(DomainTranslatorTest, testFromUnprocessableComparison)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    ASTPtr test_ast = makeASTFunction("greater", getUnProcessableExpression1(int64_sym), getUnProcessableExpression2(int64_sym));
    assertPredicateTranslates(test_ast, TupleDomain::all(), test_ast, context);

    test_ast = notF(makeASTFunction("greater", getUnProcessableExpression1(int64_sym), getUnProcessableExpression2(int64_sym)));
    assertPredicateTranslates(test_ast, TupleDomain::all(), test_ast, context);
}

TEST_F(DomainTranslatorTest, testFromBasicComparisons)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    ASTPtr liter_1_int64 = std::make_shared<ASTLiteral>(field_1);
    ASTPtr liter_2_int64 = std::make_shared<ASTLiteral>(field_2);

    // Test out the extraction of all basic comparisons
    assertPredicateTranslates(
        greaterF(int64_sym, liter_2_int64),
        TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::greaterThanRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateTranslates(
        greaterOrEqualsF(int64_sym, liter_2_int64),
        TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::greaterThanOrEqualRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateTranslates(
        lessF(int64_sym, liter_2_int64),
        TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::lessThanRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateTranslates(
        lessOrEqualsF(int64_sym, liter_2_int64),
        TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::lessThanOrEqualRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateTranslates(
        equalsF(int64_sym, liter_2_int64),
        TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::equalRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateTranslates(
        notEqualsF(int64_sym, liter_2_int64),
        TupleDomain(DomainMap{
            {int64_sym,
             Domain(createValueSet({Range::lessThanRange(int64_type, field_2), Range::greaterThanRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    //TODO test color

    // Test complement
    assertPredicateTranslates(
        notF(greaterF(int64_sym, liter_2_int64)),
        TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::lessThanOrEqualRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);


    assertPredicateTranslates(
        notF(greaterOrEqualsF(int64_sym, liter_2_int64)),
        TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::lessThanRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);


    assertPredicateTranslates(
        notF(lessF(int64_sym, liter_2_int64)),
        TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::greaterThanOrEqualRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateTranslates(
        notF(lessOrEqualsF(int64_sym, liter_2_int64)),
        TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::greaterThanRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateTranslates(
        notF(equalsF(int64_sym, liter_2_int64)),
        TupleDomain(DomainMap{
            {int64_sym,
             Domain(createValueSet({Range::lessThanRange(int64_type, field_2), Range::greaterThanRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateTranslates(
        notF(notEqualsF(int64_sym, liter_2_int64)),
        TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::equalRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);


    //TODO:test color
}

TEST_F(DomainTranslatorTest, testFromFlippedBasicComparisons)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    ASTPtr liter_1_int64 = std::make_shared<ASTLiteral>(field_1);
    ASTPtr liter_2_int64 = std::make_shared<ASTLiteral>(field_2);
    ASTPtr int64_sym_ident = std::make_shared<ASTIdentifier>(int64_sym);

    // Test out the extraction of all basic comparisons where the reference literal ordering is flipped
    assertPredicateTranslates(
        makeASTFunction("greater", liter_2_int64, int64_sym_ident),
        TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::lessThanRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateTranslates(
        makeASTFunction("greaterOrEquals", liter_2_int64, int64_sym_ident),
        TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::lessThanOrEqualRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateTranslates(
        makeASTFunction("less", liter_2_int64, int64_sym_ident),
        TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::greaterThanRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateTranslates(
        makeASTFunction("lessOrEquals", liter_2_int64, int64_sym_ident),
        TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::greaterThanOrEqualRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);


    assertPredicateTranslates(
        makeASTFunction("equals", liter_2_int64, int64_sym_ident),
        TupleDomain(DomainMap{{int64_sym, Domain(createValueSet({Range::equalRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);


    assertPredicateTranslates(
        makeASTFunction("notEquals", liter_2_int64, int64_sym_ident),
        TupleDomain(DomainMap{
            {int64_sym,
             Domain(createValueSet({Range::lessThanRange(int64_type, field_2), Range::greaterThanRange(int64_type, field_2)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);
}

TEST_F(DomainTranslatorTest, testFromBasicComparisonsWithNulls)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    ASTPtr liter_null_int64 = std::make_shared<ASTLiteral>(Field());
    // Test out the extraction of all basic comparisons with null literals
    assertPredicateIsAlwaysFalse(greaterF(int64_sym, liter_null_int64), context);

    assertPredicateTranslates(
        greaterF(string_sym, std::make_shared<ASTLiteral>(Field())),
        TupleDomain(DomainMap{{string_sym, Domain(createNone(string_type), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateIsAlwaysFalse(greaterOrEqualsF(int64_sym, liter_null_int64), context);
    assertPredicateIsAlwaysFalse(lessF(int64_sym, liter_null_int64), context);
    assertPredicateIsAlwaysFalse(lessOrEqualsF(int64_sym, liter_null_int64), context);
    assertPredicateIsAlwaysFalse(equalsF(int64_sym, liter_null_int64), context);
    assertPredicateIsAlwaysFalse(notEqualsF(int64_sym, liter_null_int64), context);

    // Test complements
    assertPredicateIsAlwaysFalse(notF(greaterF(int64_sym, liter_null_int64)), context);
    assertPredicateIsAlwaysFalse(notF(greaterOrEqualsF(int64_sym, liter_null_int64)), context);
    assertPredicateIsAlwaysFalse(notF(lessF(int64_sym, liter_null_int64)), context);
    assertPredicateIsAlwaysFalse(notF(lessOrEqualsF(int64_sym, liter_null_int64)), context);
    assertPredicateIsAlwaysFalse(notF(equalsF(int64_sym, liter_null_int64)), context);
    assertPredicateIsAlwaysFalse(notF(notEqualsF(int64_sym, liter_null_int64)), context);
}

TEST_F(DomainTranslatorTest, testFromBasicComparisonsWithNaN)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    Float64 not_a_num_float64 = std::sqrt(-1.0);
    Float32 not_a_num_float32 = std::sqrt(-1.0);
    ASTPtr nan_float64 = std::make_shared<ASTLiteral>(Field(not_a_num_float64));
    ASTPtr nan_float32 = std::make_shared<ASTLiteral>(Field(not_a_num_float32));

    //Test float64
    assertPredicateIsAlwaysFalse(equalsF(float64_sym, nan_float64), context);
    assertPredicateIsAlwaysFalse(greaterF(float64_sym, nan_float64), context);
    assertPredicateIsAlwaysFalse(greaterOrEqualsF(float64_sym, nan_float64), context);
    assertPredicateIsAlwaysFalse(lessF(float64_sym, nan_float64), context);
    assertPredicateIsAlwaysFalse(lessOrEqualsF(float64_sym, nan_float64), context);
    assertPredicateTranslates(
        notEqualsF(float64_sym, nan_float64),
        TupleDomain(DomainMap{{float64_sym, Domain::notNull(float64_type)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateTranslates(
        notF(equalsF(float64_sym, nan_float64)),
        TupleDomain(DomainMap{{float64_sym, Domain::notNull(float64_type)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateTranslates(
        notF(greaterF(float64_sym, nan_float64)),
        TupleDomain(DomainMap{{float64_sym, Domain::notNull(float64_type)}}),
        PredicateConst::TRUE_VALUE,
        context);
    assertPredicateTranslates(
        notF(greaterOrEqualsF(float64_sym, nan_float64)),
        TupleDomain(DomainMap{{float64_sym, Domain::notNull(float64_type)}}),
        PredicateConst::TRUE_VALUE,
        context);
    assertPredicateTranslates(
        notF(lessF(float64_sym, nan_float64)),
        TupleDomain(DomainMap{{float64_sym, Domain::notNull(float64_type)}}),
        PredicateConst::TRUE_VALUE,
        context);
    assertPredicateTranslates(
        notF(lessOrEqualsF(float64_sym, nan_float64)),
        TupleDomain(DomainMap{{float64_sym, Domain::notNull(float64_type)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateIsAlwaysFalse(notF(notEqualsF(float64_sym, nan_float64)), context);
    /*TODO: Float32 cannot be represented by field. ast of an expression which has float32 will convert Filed(float64),
   it seems that "symbol(float32) >  value(float64)" which can not be represented by tuple domain, because the value would
   be coarse float32, which may cause an overflow.

    //Test float32
    assertPredicateIsAlwaysFalse(equalsF(float32_sym, nan_float32), context);
    assertPredicateIsAlwaysFalse(greaterF(float32_sym, nan_float32), context);
    assertPredicateIsAlwaysFalse(greaterOrEqualsF(float32_sym, nan_float32), context);
    assertPredicateIsAlwaysFalse(lessF(float32_sym, nan_float32), context);
    assertPredicateIsAlwaysFalse(lessOrEqualsF(float32_sym, nan_float32), context);
    assertPredicateTranslates(
        notEqualsF(float32_sym, nan_float32),
        TupleDomain(DomainMap{{float32_sym, Domain::notNull(float32_type)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateTranslates(
        notF(equalsF(float32_sym, nan_float32)),
        TupleDomain(DomainMap{{float32_sym, Domain::notNull(float32_type)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateTranslates(
        notF(greaterF(float32_sym, nan_float32)),
        TupleDomain(DomainMap{{float32_sym, Domain::notNull(float32_type)}}),
        PredicateConst::TRUE_VALUE,
        context);
    assertPredicateTranslates(
        notF(greaterOrEqualsF(float32_sym, nan_float32)),
        TupleDomain(DomainMap{{float32_sym, Domain::notNull(float32_type)}}),
        PredicateConst::TRUE_VALUE,
        context);
    assertPredicateTranslates(
        notF(lessF(float32_sym, nan_float32)),
        TupleDomain(DomainMap{{float32_sym, Domain::notNull(float32_type)}}),
        PredicateConst::TRUE_VALUE,
        context);
    assertPredicateTranslates(
        notF(lessOrEqualsF(float32_sym, nan_float32)),
        TupleDomain(DomainMap{{float32_sym, Domain::notNull(float32_type)}}),
        PredicateConst::TRUE_VALUE,
        context);

    assertPredicateIsAlwaysFalse(notF(notEqualsF(float32_sym, nan_float32)), context);
    */
}
/*TODO:
TEST_F(DomainTranslatorTest, testNonImplicitCastOnSymbolSide)
{
    // we expect Tuple_domain.all here().
    // see comment in DomainTranslator.Visitor.visitComparisonExpression()
}

 TEST_F(DomainTranslatorTest, testNoSaturatedFloorCastFromUnsupportedApproximateDomain)
 {
 }

 testFromComparisonsWithCoercions
*/

TEST_F(DomainTranslatorTest, testNarrowTypeCompareWithWideType)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    ASTPtr ast_fun = greaterF(int8_sym, std::make_shared<ASTLiteral>(Int64(256)));
    assertUnsupportedPredicate(ast_fun, context);

    ast_fun = greaterF(int32_sym, std::make_shared<ASTLiteral>(Int8(1)));
    assertPredicateTranslates(
        ast_fun,
        TupleDomain(DomainMap{{int32_sym, Domain(createValueSet({Range::greaterThanRange(int32_type, Int64(1))}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);
}

TEST_F(DomainTranslatorTest, testNoSuperTypeForComparisonType)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    ASTPtr ast_fun = greaterF(int64_sym, std::make_shared<ASTLiteral>(Float64(0.5)));
    assertUnsupportedPredicate(ast_fun, context);
}

TEST_F(DomainTranslatorTest, testNotSupportFloat32ComapareWithFloat32)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    ASTPtr ast_fun = greaterF(float32_sym, std::make_shared<ASTLiteral>(Float32(1.0)));
    assertUnsupportedPredicate(ast_fun, context);
}

TEST_F(DomainTranslatorTest, testFromUnprocessableInPredicate)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    ASTPtr bool_sym_identi = std::make_shared<ASTIdentifier>("bool_sym");
    assertUnsupportedPredicate(inF(getUnProcessableExpression1(bool_sym), {PredicateConst::TRUE_VALUE}), context);
}

TEST_F(DomainTranslatorTest, testUnsupportedFunctions)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    DomainTranslator domain_translator(context);

    ASTPtr if_fun_temp = makeASTFunction(
        "if",
        makeASTFunction("isNull", std::make_shared<ASTIdentifier>(int64_sym)),
        std::make_shared<ASTLiteral>(Null{}),
        std::make_shared<ASTLiteral>(Null{}));
    assertUnsupportedPredicate(if_fun_temp, context);
}

TEST_F(DomainTranslatorTest, testIsIgnore)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    DomainTranslator domain_translator(context);
    ASTPtr liter_0_int64 = std::make_shared<ASTLiteral>(field_0);

    ASTPtr expression = orF({greaterF(int64_sym, liter_0_int64), getUnProcessableExpression1(int64_sym)});
    domain_translator.getExtractionResult(expression, names_and_types);
    ASSERT_TRUE(domain_translator.isIgnored());
}

TEST_F(DomainTranslatorTest, testConjunctExpression)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();
    DomainTranslator domain_translator(context);

    ASTPtr liter_0_float64 = std::make_shared<ASTLiteral>(field_0_float64);
    ASTPtr liter_0_int64 = std::make_shared<ASTLiteral>(field_0);

    ASTPtr expression = andF({greaterF(float64_sym, liter_0_float64), greaterF(int64_sym, liter_0_int64)});

    assertPredicateTranslates(
        expression,
        TupleDomain(DomainMap{
            {float64_sym, Domain(createValueSet({Range::greaterThanRange(float64_type, field_0_float64)}), false)},
            {int64_sym, Domain(createValueSet({Range::greaterThanRange(int64_type, field_0)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    ASTPtr to_predicate = toPredicate(fromPredicate(expression, context).tuple_domain, domain_translator);
    ASTPtr expected_ast = andF({greaterF(float64_sym, liter_0_float64), greaterF(int64_sym, liter_0_int64)});
    ASSERT_TRUE(isEqual(
        toPredicate(fromPredicate(expression, context).tuple_domain, domain_translator),
        andF({greaterF(float64_sym, liter_0_float64), greaterF(int64_sym, liter_0_int64)})));
}

TEST_F(DomainTranslatorTest, testFromIsNullPredicate)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    assertPredicateTranslates(
        isNullF(int64_sym), TupleDomain(DomainMap{{int64_sym, Domain::onlyNull(int64_type)}}), PredicateConst::TRUE_VALUE, context);
    assertPredicateTranslates(
        notF(isNullF(int64_sym)), TupleDomain(DomainMap{{int64_sym, Domain::notNull(int64_type)}}), PredicateConst::TRUE_VALUE, context);
}

TEST_F(DomainTranslatorTest, testFromIsNotNullPredicate)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    assertPredicateTranslates(
        isNotNullF(int64_sym), TupleDomain(DomainMap{{int64_sym, Domain::notNull(int64_type)}}), PredicateConst::TRUE_VALUE, context);
    assertPredicateTranslates(
        notF(isNotNullF(int64_sym)),
        TupleDomain(DomainMap{{int64_sym, Domain::onlyNull(int64_type)}}),
        PredicateConst::TRUE_VALUE,
        context);
}

TEST_F(DomainTranslatorTest, testFromBooleanLiteralPredicate)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    assertPredicateIsAlwaysTrue(PredicateConst::TRUE_VALUE, context);
    assertPredicateIsAlwaysFalse(notF(PredicateConst::TRUE_VALUE), context);
    assertPredicateIsAlwaysFalse(PredicateConst::FALSE_VALUE, context);
    assertPredicateIsAlwaysTrue(notF(PredicateConst::FALSE_VALUE), context);
}

TEST_F(DomainTranslatorTest, testFromNullLiteralPredicate)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    assertPredicateIsAlwaysFalse(std::make_shared<ASTLiteral>(Field()), context);
    assertPredicateIsAlwaysFalse(notF(std::make_shared<ASTLiteral>(Field())), context);
}

TEST_F(DomainTranslatorTest, testImplicitCastOnValueSide)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    ASTPtr liter_1_int64 = std::make_shared<ASTLiteral>(field_1);
    ASTPtr liter_2_int64 = std::make_shared<ASTLiteral>(field_2);
    ASTPtr liter_1_uint64 = std::make_shared<ASTLiteral>(Field(UInt64(1)));
    ASTPtr liter_2_uint64 = std::make_shared<ASTLiteral>(Field(UInt64(2)));
    ASTPtr liter_1_string = std::make_shared<ASTLiteral>(Field("2022-10-20"));
    ASTPtr liter_2_string = std::make_shared<ASTLiteral>(Field("2022-11-20"));


    // symbol(float64) compare with value(int64)
    assertPredicateTranslates(
        betweenF(float64_sym, liter_1, liter_2),
        TupleDomain(DomainMap{{float64_sym, Domain(createValueSet({Range(float64_type, true, Float64(1.0), true, Float64(2.0))}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    // symbol(float64) compare with value(uint64)
    assertPredicateTranslates(
        betweenF(float64_sym, liter_1_uint64, liter_2_uint64),
        TupleDomain(DomainMap{{float64_sym, Domain(createValueSet({Range(float64_type, true, Float64(1.0), true, Float64(2.0))}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    // symbol(Date) compare with value(string)
    assertPredicateTranslates(
        betweenF(date_sym, liter_1_string, liter_2_string),
        TupleDomain(DomainMap{
            {date_sym,
             Domain(
                 createValueSet({Range(
                     date_type,
                     true,
                     convertFieldToType(Field("2022-10-20"), *date_type),
                     true,
                     convertFieldToType(Field("2022-11-20"), *date_type))}),
                 false)}}),
        PredicateConst::TRUE_VALUE,
        context);
}

/*
TEST_F(DomainTranslatorTest, testInPredicateWithInteger)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    testInPredicate(int8_sym, int8_type, field_1_int8, field_2_int8, context);
}

TEST_F(DomainTranslatorTest, testInPredicateWithBigint)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    testInPredicate(int64_sym, int64_type, field_1, field_2, context);
}
 */
/* TODO: can not support the expression which is 'symbol(narrow type) operator value(wide type)' unless use 'cast' function;
TEST_F(DomainTranslatorTest, testInPredicateWithReal)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    //TODO: can not support the expression which is 'symbol(narrow type) operator value(wide type)'

    testInPredicateWithFloatingPoint(float32_sym, float32_type, Field(Float32(1.0)),  Field(Float32(2.0)),  Field(Float32(std::sqrt(-1.0))), context);
}

TEST_F(DomainTranslatorTest, testInPredicateWithDouble)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    //TODO: can not support the expression which is 'symbol(narrow type) operator value(wide type)'

    testInPredicateWithFloatingPoint(float64_sym, float64_type, Field(Float64(1.0)),  Field(Float64(2.0)),  Field(Float64(std::sqrt(-1.0))), context);
}

TEST_F(DomainTranslatorTest, testInPredicateWithShortDecimal)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    //TODO: can not support the expression which is 'symbol(narrow type) operator value(wide type)'

    testInPredicate(decimal_6_1_sym, decimal_6_1_type, DecimalField<Decimal32>(10, 1), DecimalField<Decimal32>(20, 1), context);
}

TEST_F(DomainTranslatorTest, testInPredicateWithLongDecimal)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    testInPredicate(
        decimal_12_2_sym,
        decimal_12_2_type,
        DecimalField<Decimal64>(10, 2),
        DecimalField<Decimal64>(20, 2), context);

    testInPredicate(
        decimal_38_3_sym,
        decimal_38_3_type,
        DecimalField<Decimal128>(10, 3),
        DecimalField<Decimal128>(20, 3), context);
}

TEST_F(DomainTranslatorTest, testInPredicateWithVarchar)
{
    ContextMutablePtr context = Context::createCopy(getContext().context);
    initContextFlag();

    testInPredicate(
        string_sym,
        string_type,
        "first",
        "second",
        context);
}
 */
/*
TEST_F(DomainTranslatorTest, testNonImplicitCastOnSymbolSide)
    public void testInPredicateWithEquitableType()
{
    assertPredicateTranslates(
        in(C_COLOR, ImmutableList.of(colorLiteral(COLOR_VALUE_1))),
        tupleDomain(C_COLOR, Domain.singleValue(COLOR, COLOR_VALUE_1)));

    assertPredicateTranslates(
        in(C_COLOR, ImmutableList.of(colorLiteral(COLOR_VALUE_1), colorLiteral(COLOR_VALUE_2))),
        tupleDomain(C_COLOR, Domain.create(ValueSet.of(COLOR, COLOR_VALUE_1, COLOR_VALUE_2), false)));

    assertPredicateTranslates(
        not(in(C_COLOR, ImmutableList.of(colorLiteral(COLOR_VALUE_1), colorLiteral(COLOR_VALUE_2)))),
        tupleDomain(C_COLOR, Domain.create(ValueSet.of(COLOR, COLOR_VALUE_1, COLOR_VALUE_2).complement(), false)));
}

TEST_F(DomainTranslatorTest, testNonImplicitCastOnSymbolSide)
    public void testInPredicateWithCasts()
{
    assertPredicateTranslates(
        new InPredicate(
            C_BIGINT.toSymbolReference(),
            new InListExpression(ImmutableList.of(cast(toExpression(1L, SMALLINT), BIGINT)))),
        tupleDomain(C_BIGINT, Domain.singleValue(BIGINT, 1L)));

    assertPredicateTranslates(
        new InPredicate(
            cast(C_SMALLINT, BIGINT),
            new InListExpression(ImmutableList.of(toExpression(1L, BIGINT)))),
        tupleDomain(C_SMALLINT, Domain.singleValue(SMALLINT, 1L)));

    assertUnsupportedPredicate(new InPredicate(
        cast(C_BIGINT, INTEGER),
        new InListExpression(ImmutableList.of(toExpression(1L, INTEGER)))));
}

TEST_F(DomainTranslatorTest, testNonImplicitCastOnSymbolSide)
    public void testFromInPredicateWithCastsAndNulls()
{
    assertPredicateIsAlwaysFalse(new InPredicate(
        C_BIGINT.toSymbolReference(),
        new InListExpression(ImmutableList.of(cast(toExpression(null, SMALLINT), BIGINT)))));

    assertUnsupportedPredicate(not(new InPredicate(
        cast(C_SMALLINT, BIGINT),
        new InListExpression(ImmutableList.of(toExpression(null, BIGINT))))));

    assertPredicateTranslates(
        new InPredicate(
            C_BIGINT.toSymbolReference(),
            new InListExpression(ImmutableList.of(cast(toExpression(null, SMALLINT), BIGINT), toExpression(1L, BIGINT)))),
        tupleDomain(C_BIGINT, Domain.create(ValueSet.ofRanges(Range.equal(BIGINT, 1L)), false)));

    assertPredicateIsAlwaysFalse(not(new InPredicate(
        C_BIGINT.toSymbolReference(),
        new InListExpression(ImmutableList.of(cast(toExpression(null, SMALLINT), BIGINT), toExpression(1L, SMALLINT))))));
}

TEST_F(DomainTranslatorTest, testNonImplicitCastOnSymbolSide)
    public void testFromBetweenPredicate()
{
    assertPredicateTranslates(
        between(C_BIGINT, bigintLiteral(1L), bigintLiteral(2L)),
        tupleDomain(C_BIGINT, Domain.create(ValueSet.ofRanges(Range.range(BIGINT, 1L, true, 2L, true)), false)));

    assertPredicateTranslates(
        between(cast(C_INTEGER, DOUBLE), cast(bigintLiteral(1L), DOUBLE), doubleLiteral(2.1)),
        tupleDomain(C_INTEGER, Domain.create(ValueSet.ofRanges(Range.range(INTEGER, 1L, true, 2L, true)), false)));

    assertPredicateIsAlwaysFalse(between(C_BIGINT, bigintLiteral(1L), nullLiteral(BIGINT)));

    // Test complements
    assertPredicateTranslates(
        not(between(C_BIGINT, bigintLiteral(1L), bigintLiteral(2L))),
        tupleDomain(C_BIGINT, Domain.create(ValueSet.ofRanges(Range.lessThan(BIGINT, 1L), Range.greaterThan(BIGINT, 2L)), false)));

    assertPredicateTranslates(
        not(between(cast(C_INTEGER, DOUBLE), cast(bigintLiteral(1L), DOUBLE), doubleLiteral(2.1))),
        tupleDomain(C_INTEGER, Domain.create(ValueSet.ofRanges(Range.lessThan(INTEGER, 1L), Range.greaterThan(INTEGER, 2L)), false)));

    assertPredicateTranslates(
        not(between(C_BIGINT, bigintLiteral(1L), nullLiteral(BIGINT))),
        tupleDomain(C_BIGINT, Domain.create(ValueSet.ofRanges(Range.lessThan(BIGINT, 1L)), false)));
}
*/

/*
TEST_F(DomainTranslatorTest, testExpressionConstantFolding)
{

}
*/

/*
TEST_F(DomainTranslatorTest, testMultipleCoercionsOnSymbolSide)
    public void testMultipleCoercionsOnSymbolSide()
{
}

TEST_F(DomainTranslatorTest, testNumericTypeTranslation)
{

}

private void testNumericTypeTranslationChain(NumericValues<?>... translationChain)
{

}

private void testNumericTypeTranslation(NumericValues<?> columnValues, NumericValues<?> literalValues)
{

}
*/

//TEST_F(DomainTranslatorTest, testLikePredicate)
//{
//    ContextMutablePtr global_context = DBGTestEnvironment::getContext();
//    Context context(global_context);
//    initContextFlag();
//
//    // constant
//    testSimpleComparison(
//        likeF(string_sym, "abc"),
//        string_sym,
//        PredicateConst::TRUE_VALUE,
//        Domain::multipleValues(string_type, {Field("abc")}),
//        context);
//
//    // starts with pattern
//    assertUnsupportedPredicate(likeF(string_sym, "_def"), context);
//    assertUnsupportedPredicate(likeF(string_sym, "%def"), context);
//
//    // _ pattern (unless escaped)
//    testSimpleComparison(
//        likeF(string_sym, "abc_def"),
//        string_sym,
//        likeF(string_sym, "abc_def"),
//        Domain(createValueSet({Range(string_type, true, "abc", false, "abd")}), false),
//        context);
//
//    testSimpleComparison(
//        likeF(string_sym, "abc\\_def"),
//        string_sym,
//        PredicateConst::TRUE_VALUE,
//        Domain::multipleValues(string_type, {Field("abc_def")}),
//        context);
//
//    testSimpleComparison(
//        likeF(string_sym, "abc\\_def_"),
//        string_sym,
//        likeF(string_sym, "abc\\_def_"),
//        Domain(createValueSet({Range(string_type, true, "abc_def", false, "abc_deg")}), false),
//        context);
//
//    testSimpleComparison(
//        likeF(string_sym, "abc\\_def"),
//        string_sym,
//        PredicateConst::TRUE_VALUE,
//        Domain::multipleValues(string_type, {Field("abc_def")}),
//        context);
//
//    testSimpleComparison(
//        likeF(string_sym, "abc\u007f\u0123"),
//        string_sym,
//        PredicateConst::TRUE_VALUE,
//        Domain::multipleValues(string_type, {"abc\u007f\u0123"}),
//        context);
//
//    // non-ASCII prefix
//    testSimpleComparison(
//        likeF(string_sym, "abc\u0123def\u007e\u007f\u00ff\u0123\uccf0%"),
//        string_sym,
//        likeF(string_sym, "abc\u0123def\u007e\u007f\u00ff\u0123\uccf0%"),
//        Domain(createValueSet({
//            Range(string_type, true, "abc\u0123def\u007e\u007f\u00ff\u0123\uccf0", false, "abc\u0123def\u007f")}), false),
//        context);
//
//    // negation with literal
//    testSimpleComparison(
//        notF(likeF(string_sym, "abcdef")),
//        string_sym,
//        PredicateConst::TRUE_VALUE,
//        Domain(createValueSet({
//                   Range::lessThanRange(string_type, "abcdef"),
//                   Range::greaterThanRange(string_type, "abcdef")}),false),
//        context);
//
//    testSimpleComparison(
//        notF(likeF(string_sym, "abc\\_def")),
//        string_sym,
//        PredicateConst::TRUE_VALUE,
//        Domain(createValueSet({
//                   Range::lessThanRange(string_type, "abc_def"),
//                   Range::greaterThanRange(string_type, "abc_def")}), false),
//        context);
//
//    // negation with pattern
//    assertUnsupportedPredicate(notF(likeF(string_sym, "abc\\_def_")), context);
//}

//TEST_F(DomainTranslatorTest, testStartsWithFunction)
//{
//    ContextMutablePtr global_context = DBGTestEnvironment::getContext();
//    Context context(global_context);
//    initContextFlag();
//    DomainTranslator domain_translator(context);
//
//    // constant
//    testSimpleComparison(
//        startsWithF(string_sym, "abc"),
//        string_sym,
//        startsWithF(string_sym, "abc"),
//        Domain(createValueSet({Range(string_type, true, "abc", false, "abd")}), false),
//        context);
//
//    testSimpleComparison(
//        startsWithF(string_sym, "_abc"),
//        string_sym,
//        startsWithF(string_sym, "_abc"),
//        Domain(createValueSet({Range(string_type, true, "_abc", false, "_abd")}), false),
//        context);
//
//    // empty
//    assertUnsupportedPredicate(startsWithF(string_sym, ""), context);
//    // complement
//    assertUnsupportedPredicate(notF(startsWithF(string_sym, "abc")), context);
//
//    //"abc\u0123def\u007e\u00ff\u00ff\u0123\uccf0"
//    // non-ASCII
//    testSimpleComparison(
//        startsWithF(string_sym, "abc\u0123def\u007e\u007f\u00ff\u0123\uccf0"),
//        string_sym,
//        startsWithF(string_sym, "abc\u0123def\u007e\u007f\u00ff\u0123\uccf0"),
//        Domain(
//            createValueSet({Range(string_type,true, "abc\u0123def\u007e\u007f\u00ff\u0123\uccf0",false, "abc\u0123def\u007f")}),
//            false),
//        context);
//}

DomainTranslatorTest::DomainTranslatorTest()
{
    //init names_and_types
    names_and_types.emplace_back(int8_sym, int8_type);
    names_and_types.emplace_back(int16_sym, int16_type);
    names_and_types.emplace_back(int32_sym, int32_type);
    names_and_types.emplace_back(int64_sym, int64_type);
    names_and_types.emplace_back(uint16_sym, uint16_type);
    names_and_types.emplace_back(uint32_sym, uint32_type);
    names_and_types.emplace_back(uint64_sym, uint64_type);
    names_and_types.emplace_back(int64_sym_1, int64_type);
    names_and_types.emplace_back(int32_sym_1, int32_type);
    names_and_types.emplace_back(float32_sym, float32_type);
    names_and_types.emplace_back(float64_sym, float64_type);
    names_and_types.emplace_back(float64_sym_1, float64_type);
    //names_and_types.emplace_back(decimal_sym, decimal_type);
    names_and_types.emplace_back(date_sym, date_type);
    names_and_types.emplace_back(date_time_sym, date_time_type);
    names_and_types.emplace_back(date_time64_sym, date_time64_type);
    names_and_types.emplace_back(string_sym, string_type);
    names_and_types.emplace_back(string_sym_1, string_type);
    names_and_types.emplace_back(bool_sym, uint8_type);
    names_and_types.emplace_back(decimal_12_2_sym, decimal_12_2_type);
    names_and_types.emplace_back(decimal_6_1_sym, decimal_6_1_type);
    names_and_types.emplace_back(decimal_38_3_sym, decimal_38_3_type);
    names_and_types.emplace_back(decimal_3_0_sym, decimal_3_0_type);
    names_and_types.emplace_back(decimal_2_0_sym, decimal_2_0_type);
}

ASTPtr DomainTranslatorTest::notInF(const String & symbol, ASTPtr ast)
{
    auto * ast_fun = ast->as<ASTFunction>();
    if (ast_fun && ast_fun->name == "tuple")
    {
        return makeASTFunction("notIn", std::make_shared<ASTIdentifier>(symbol), ast);
    }
    throw Exception("logic error when make astFunction for 'in'.", DB::ErrorCodes::LOGICAL_ERROR);
}
ASTPtr DomainTranslatorTest::inF(const String & symbol, ASTPtr ast)
{
    auto * ast_fun = ast->as<ASTFunction>();
    if (ast_fun && ast_fun->name == "tuple")
    {
        return makeASTFunction("in", std::make_shared<ASTIdentifier>(symbol), ast);
    }
    throw Exception("logic error when make astFunction for 'in'.", DB::ErrorCodes::LOGICAL_ERROR);
}

ASTPtr DomainTranslatorTest::equalsF(const String & symbol, ASTPtr ast)
{
    return makeASTFunction("equals", std::make_shared<ASTIdentifier>(symbol), ast);
}

ASTPtr DomainTranslatorTest::notEqualsF(const String & symbol, ASTPtr ast)
{
    return makeASTFunction("notEquals", std::make_shared<ASTIdentifier>(symbol), ast);
}

ASTPtr DomainTranslatorTest::lessF(const String & symbol, ASTPtr ast)
{
    return makeASTFunction("less", std::make_shared<ASTIdentifier>(symbol), ast);
}

ASTPtr DomainTranslatorTest::lessOrEqualsF(const String & symbol, ASTPtr ast)
{
    return makeASTFunction("lessOrEquals", std::make_shared<ASTIdentifier>(symbol), ast);
}

ASTPtr DomainTranslatorTest::greaterF(const String & symbol, ASTPtr ast)
{
    return makeASTFunction("greater", std::make_shared<ASTIdentifier>(symbol), ast);
}

ASTPtr DomainTranslatorTest::greaterOrEqualsF(const String & symbol, ASTPtr ast)
{
    return makeASTFunction("greaterOrEquals", std::make_shared<ASTIdentifier>(symbol), ast);
}

ASTPtr DomainTranslatorTest::andF(const std::vector<ConstASTPtr> & predicates)
{
    return PredicateUtils::combineConjuncts(predicates);
}

ASTPtr DomainTranslatorTest::orF(const std::vector<ConstASTPtr> & predicates)
{
    return PredicateUtils::combineDisjuncts(predicates);
}

ASTPtr DomainTranslatorTest::betweenF(const String & symbol, ASTPtr ast1, ASTPtr ast2)
{
    ASTPtr identifier_sym = std::make_shared<ASTIdentifier>(symbol);
    return PredicateUtils::combineConjuncts(
        {makeASTFunction("greaterOrEquals", identifier_sym, ast1), makeASTFunction("lessOrEquals", identifier_sym, ast2)});
}

ASTPtr DomainTranslatorTest::isNotNullF(const String & symbol)
{
    return makeASTFunction("isNotNull", std::make_shared<ASTIdentifier>(symbol));
}

ASTPtr DomainTranslatorTest::isNullF(const String & symbol)
{
    return makeASTFunction("isNull", std::make_shared<ASTIdentifier>(symbol));
}

ASTPtr DomainTranslatorTest::notF(ASTPtr ast)
{
    return makeASTFunction("not", ast);
}

ASTPtr DomainTranslatorTest::inF(ASTPtr ast, ASTs in_list)
{
    return makeASTFunction("in", ast, makeASTFunction("tuple", std::move(in_list)));
}

ASTPtr DomainTranslatorTest::notInF(ASTPtr ast, ASTs in_list)
{
    return makeASTFunction("notIn", ast, makeASTFunction("tuple", std::move(in_list)));
}

ASTPtr DomainTranslatorTest::likeF(const String & symbol, const String & target)
{
    return makeASTFunction("like", std::make_shared<ASTIdentifier>(symbol), std::make_shared<ASTLiteral>(target));
}

ASTPtr DomainTranslatorTest::startsWithF(const String & symbol, const String & target)
{
    return makeASTFunction("startsWith", std::make_shared<ASTIdentifier>(symbol), std::make_shared<ASTLiteral>(target));
}

ExtractionResult DomainTranslatorTest::fromPredicate(ASTPtr original_predicate, ContextMutablePtr context)
{
    DomainTranslator domain_translator = DomainTranslator(context);
    //return DomainTranslator::getExtractionResult(original_predicate, names_and_types);
    return domain_translator.getExtractionResult(original_predicate, names_and_types);
}

void DomainTranslatorTest::assertPredicateTranslates(
    const ASTPtr & node, const TupleDomain & tuple_domain, const ASTPtr & remaining_expression, ContextMutablePtr context)
{
    ExtractionResult result = fromPredicate(node, context);
    ASSERT_TRUE(isEqual(result.remaining_expression, remaining_expression));
    ASSERT_EQ(result.tuple_domain, tuple_domain);
}

void DomainTranslatorTest::assertUnsupportedPredicate(const ASTPtr & node, ContextMutablePtr context)
{
    assertPredicateTranslates(node, TupleDomain::all(), node, context);
}

void DomainTranslatorTest::assertPredicateIsAlwaysFalse(const ASTPtr & node, ContextMutablePtr context)
{
    assertPredicateTranslates(node, TupleDomain::none(), PredicateConst::TRUE_VALUE, context);
}

void DomainTranslatorTest::assertPredicateIsAlwaysTrue(const ASTPtr & node, ContextMutablePtr context)
{
    assertPredicateTranslates(node, TupleDomain::all(), PredicateConst::TRUE_VALUE, context);
}

void DomainTranslatorTest::testInPredicate(
    String symbol_1, DataTypePtr type, const Field & one, const Field & two, ContextMutablePtr context)
{
    ASTPtr one_expression = std::make_shared<ASTLiteral>(one);
    ASTPtr two_expression = std::make_shared<ASTLiteral>(two);
    ASTPtr null_expression = std::make_shared<ASTLiteral>(Field());
    ASTPtr sym1_ident = std::make_shared<ASTIdentifier>(symbol_1);

    // IN, single value
    assertPredicateTranslates(
        inF(sym1_ident, {one_expression}),
        TupleDomain(DomainMap{{symbol_1, Domain::singleValue(type, one)}}),
        PredicateConst::TRUE_VALUE,
        context);

    // IN, two values
    assertPredicateTranslates(
        inF(sym1_ident, {one_expression, two_expression}),
        TupleDomain(DomainMap{{symbol_1, Domain::multipleValues(type, {one, two})}}),
        PredicateConst::TRUE_VALUE,
        context);

    // IN, with null
    assertPredicateIsAlwaysFalse(inF(sym1_ident, {null_expression}), context);

    assertPredicateTranslates(
        inF(sym1_ident, {one_expression, null_expression, two_expression}),
        TupleDomain(DomainMap{{symbol_1, Domain::multipleValues(type, {one, two})}}),
        PredicateConst::TRUE_VALUE,
        context);

    // NOT IN, single value
    assertPredicateTranslates(
        notF(inF(sym1_ident, {one_expression})),
        TupleDomain(
            DomainMap{{symbol_1, Domain(createValueSet({Range::lessThanRange(type, one), Range::greaterThanRange(type, one)}), false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    // NOT IN, two values
    assertPredicateTranslates(
        notF(inF(sym1_ident, {one_expression, two_expression})),
        TupleDomain(DomainMap{
            {symbol_1,
             Domain(
                 createValueSet({Range::lessThanRange(type, one), Range(type, false, one, false, two), Range::greaterThanRange(type, two)}),
                 false)}}),
        PredicateConst::TRUE_VALUE,
        context);

    // NOT IN, with null
    assertPredicateIsAlwaysFalse(notF(inF(sym1_ident, {null_expression})), context);
    assertPredicateTranslates(
        notF(inF(sym1_ident, {one_expression, null_expression, two_expression})), TupleDomain::none(), PredicateConst::TRUE_VALUE, context);

    // NOT IN, with expression
    assertPredicateTranslates(
        notF(inF(sym1_ident, {one_expression, two_expression})),
        TupleDomain(DomainMap{
            {symbol_1,
             Domain(
                 createValueSet({Range::lessThanRange(type, one), Range(type, false, one, false, two), Range::greaterThanRange(type, two)}),
                 false)}}),
        PredicateConst::TRUE_VALUE,
        context);
}

void DomainTranslatorTest::testInPredicateWithFloatingPoint(
    String symbol_1, DataTypePtr type, const Field & one, const Field & two, const Field & nan, ContextMutablePtr context)
{
    ASTPtr one_expression = std::make_shared<ASTLiteral>(one);
    ASTPtr two_expression = std::make_shared<ASTLiteral>(two);
    ASTPtr nan_expression = std::make_shared<ASTLiteral>(nan);
    ASTPtr null_expression = std::make_shared<ASTLiteral>(Field());
    ASTPtr sym1_ident = std::make_shared<ASTIdentifier>(symbol_1);

    // IN, single value
    assertPredicateTranslates(
        inF(sym1_ident, {one_expression}),
        TupleDomain(DomainMap{{symbol_1, Domain::singleValue(type, one)}}),
        PredicateConst::TRUE_VALUE,
        context);

    // IN, two values
    assertPredicateTranslates(
        inF(sym1_ident, {one_expression, two_expression}),
        TupleDomain(DomainMap{{symbol_1, Domain::multipleValues(type, {one, two})}}),
        PredicateConst::TRUE_VALUE,
        context);

    // IN, with null
    assertPredicateIsAlwaysFalse(inF(sym1_ident, {null_expression}), context);
    assertPredicateTranslates(
        inF(sym1_ident, {one_expression, null_expression, two_expression}),
        TupleDomain(DomainMap{{symbol_1, Domain::multipleValues(type, {one, two})}}),
        PredicateConst::TRUE_VALUE,
        context);

    // IN, with NaN
    assertPredicateIsAlwaysFalse(inF(sym1_ident, {nan_expression}), context);
    assertPredicateTranslates(
        inF(sym1_ident, {one_expression, nan_expression, two_expression}),
        TupleDomain(DomainMap{{symbol_1, Domain::multipleValues(type, {one, two})}}),
        PredicateConst::TRUE_VALUE,
        context);

    // IN, with null and NaN
    assertPredicateIsAlwaysFalse(inF(sym1_ident, {nan_expression, null_expression}), context);
    assertPredicateTranslates(
        inF(sym1_ident, {one_expression, nan_expression, two_expression, null_expression}),
        TupleDomain(DomainMap{{symbol_1, Domain::multipleValues(type, {one, two})}}),
        PredicateConst::TRUE_VALUE,
        context);

    // NOT IN, single value
    assertPredicateTranslates(
        notF(inF(sym1_ident, {one_expression})),
        TupleDomain(DomainMap{{symbol_1, Domain::notNull(type)}}),
        notEqualsF(symbol_1, one_expression),
        context);

    // NOT IN, two values
    assertPredicateTranslates(
        notF(inF(sym1_ident, {one_expression, two_expression})),
        TupleDomain(DomainMap{{symbol_1, Domain::notNull(type)}}),
        notInF(sym1_ident, {one_expression, two_expression}),
        context);

    // NOT IN, with null
    assertPredicateIsAlwaysFalse(notF(inF(sym1_ident, {null_expression})), context);
    assertPredicateIsAlwaysFalse(notF(inF(sym1_ident, {one_expression, null_expression, two_expression})), context);

    // NOT IN, with NaN
    assertPredicateTranslates(
        notF(inF(sym1_ident, {nan_expression})),
        TupleDomain(DomainMap{{symbol_1, Domain::notNull(type)}}),
        PredicateConst::TRUE_VALUE,
        context);
    assertPredicateTranslates(
        notF(inF(sym1_ident, {one_expression, nan_expression, two_expression})),
        TupleDomain(DomainMap{{symbol_1, Domain::notNull(type)}}),
        notInF(sym1_ident, {one_expression, two_expression}),
        context);

    // NOT IN, with null and NaN
    assertPredicateIsAlwaysFalse(notF(inF(sym1_ident, {nan_expression, null_expression})), context);
    assertPredicateIsAlwaysFalse(notF(inF(sym1_ident, {one_expression, nan_expression, two_expression, null_expression})), context);
}

void DomainTranslatorTest::testSimpleComparison(
    ASTPtr node, const String & symbol, ASTPtr expect_remaining_expression, const Domain & expected_domain, ContextMutablePtr context)
{
    ExtractionResult result = fromPredicate(node, context);
    ASSERT_TRUE(isEqual(result.remaining_expression, expect_remaining_expression));
    ASSERT_EQ(result.tuple_domain, TupleDomain(DomainMap{{symbol, expected_domain}}));
}

ASTs DomainTranslatorTest::appendTypesForLiterals(DataTypePtr type, ASTs asts)
{
    ASTs res;
    for (auto & ast : asts)
    {
        auto * liter = ast->as<ASTLiteral>();
        if (!liter)
            throw Exception("ast must be literals", DB::ErrorCodes::LOGICAL_ERROR);
        else
            res.emplace_back(makeASTFunction("cast", ast, std::make_shared<ASTLiteral>(type->getName())));
    }
    return res;
}

ASTPtr DomainTranslatorTest::getUnProcessableExpression1(const String & symbol)
{
    ASTPtr temp = std::make_shared<ASTIdentifier>(symbol);
    return makeASTFunction("greater", temp, temp);
}

ASTPtr DomainTranslatorTest::getUnProcessableExpression2(const String & symbol)
{
    ASTPtr temp = std::make_shared<ASTIdentifier>(symbol);
    return makeASTFunction("less", temp, temp);
}

//int main(int argc, char ** argv)
//{
//    ::testing::InitGoogleTest(&argc, argv);
//    return RUN_ALL_TESTS();
//}

}

