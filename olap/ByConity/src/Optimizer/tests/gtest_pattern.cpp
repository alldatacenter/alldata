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


#include <gtest/gtest.h>

using namespace DB;
using namespace DB::Patterns;

std::unordered_set<PlanNodePtr> getCapturesAsSet(const Match & match, const Capture & capture)
{
    std::unordered_set<PlanNodePtr> res;
    auto its = match.captures.equal_range(capture);
    for (auto it = its.first; it != its.second; ++it)
        res.insert(std::any_cast<PlanNodePtr>(it->second));
    return res;
}

TEST(OptimizerPatternTest, TypeOfPattern)
{
    PlanNodePtr readNode = createTableScanNode("db1", "t1", {});
    PlanNodePtr exchangeNode = createExchangeNode(ExchangeMode::BROADCAST, {});
    PatternPtr readPattern = typeOf(IQueryPlanStep::Type::TableScan);
    PatternPtr anyPattern = DB::Patterns::any();

    ASSERT_TRUE(readPattern->matches(readNode));
    ASSERT_TRUE(!readPattern->matches(exchangeNode));

    ASSERT_TRUE(anyPattern->matches(readNode));
    ASSERT_TRUE(anyPattern->matches(exchangeNode));
}

TEST(OptimizerPatternTest, CapturePattern)
{
    Capture readNodeCap;
    Capture idCap;
    Capture dbCap;
    Capture tableCap;
    PatternPtr pattern = tableScan()
                             ->capturedAs(readNodeCap)
                             ->capturedAs(idCap, [](const auto & node) { return node->getId(); })
                             ->capturedStepAs<MockedTableScanStep>(dbCap, [](const auto & step) { return step.database; })
                             ->capturedStepAs<MockedTableScanStep>(tableCap, &MockedTableScanStep::table);

    PlanNodePtr readNode = createTableScanNode("db1", "t1", {});

    std::optional<Match> match = pattern->match(readNode);
    ASSERT_TRUE(match.has_value());
    const Captures & captures = match->captures;
    ASSERT_TRUE(captures.size() == 4);
    ASSERT_TRUE(captures.at<PlanNodePtr>(readNodeCap) == readNode);
    ASSERT_TRUE(captures.at<PlanNodeId>(idCap) == readNode->getId());
    ASSERT_TRUE(captures.at<std::string>(dbCap) == "db1");
    ASSERT_TRUE(captures.at<std::string>(tableCap) == "t1");
}

// Capture step property of wrong step type will lead to a runtime exception
TEST(OptimizerPatternTest, DISABLED_CapturePattern_unexpectedStepProvided)
{
    Capture dbCap;
    PatternPtr pattern = exchange()->capturedStepAs<MockedTableScanStep>(dbCap, [](const auto & step) { return step.database; });

    PlanNodePtr exchangeNode = createExchangeNode(ExchangeMode::BROADCAST, {});
    ASSERT_THROW_DB_EXCEPTION_WITH_MESSAGE_PREFIX(pattern->match(exchangeNode), "Unexpected plan step found in pattern matching");
}

TEST(OptimizerPatternTest, FilterPattern)
{
    PlanNodePtr readNodeT1 = createTableScanNode("db1", "t1", {});
    PlanNodePtr readNodeT2 = createTableScanNode("db1", "t2", {createTableScanNode("db1", "t3", {})});

    PatternPtr t1Pattern
        = typeOf(IQueryPlanStep::Type::TableScan)->matchingStep<MockedTableScanStep>([](const auto & s) { return s.table == "t1"; });

    PatternPtr t2Pattern
        = typeOf(IQueryPlanStep::Type::TableScan)->matching([](const auto & node, const auto &) { return !node->getChildren().empty(); });

    ASSERT_TRUE(t1Pattern->matches(readNodeT1));
    ASSERT_TRUE(!t1Pattern->matches(readNodeT2));
    ASSERT_TRUE(!t2Pattern->matches(readNodeT1));
    ASSERT_TRUE(t2Pattern->matches(readNodeT2));
}

TEST(OptimizerPatternTest, FilterPatternFilterByCaptures)
{
    Capture dbCap;
    PatternPtr pattern = tableScan()
                             ->capturedStepAs<MockedTableScanStep>(dbCap, [](const auto & step) { return step.database; })
                             ->matchingCapture([&dbCap](const Captures & caps) { return caps.at<std::string>(dbCap) == "db1"; });

    PlanNodePtr readNode = createTableScanNode("db1", "t1", {});

    ASSERT_TRUE(pattern->matches(readNode));
    ASSERT_TRUE(pattern->match(readNode)->captures.size() == 1);
    ASSERT_TRUE(pattern->match(readNode)->captures.at<std::string>(dbCap) == "db1");
}

// Predicate of wrong step type will lead to a runtime exception
TEST(OptimizerPatternTest, DISABLED_FilterPatternUnexpectedStepProvided)
{
    PatternPtr pattern = exchange()->matchingStep<MockedTableScanStep>([](const auto & step) { return step.database == "db1"; });

    PlanNodePtr exchangeNode = createExchangeNode(ExchangeMode::BROADCAST, {});
    ASSERT_THROW_DB_EXCEPTION_WITH_MESSAGE_PREFIX(pattern->match(exchangeNode), "Unexpected plan step found in pattern matching");
}

TEST(OptimizerPatternTest, WithPatterns)
{
    PlanNodePtr readNode1 = createTableScanNode("db1", "t1", {});
    PlanNodePtr readNode2 = createTableScanNode("db1", "t2", {});
    PlanNodePtr filterNode = createFilterNode("col1", "equals", {});
    PlanNodePtr joinNode1 = createJoinNode({}, {}, {});
    PlanNodePtr joinNode2 = createJoinNode({}, {}, {readNode1});
    PlanNodePtr joinNode3 = createJoinNode({}, {}, {filterNode});
    PlanNodePtr joinNode4 = createJoinNode({}, {}, {readNode1, filterNode});
    PlanNodePtr joinNode5 = createJoinNode({}, {}, {readNode1, readNode2});

    PatternPtr emptyPattern = join()->withEmpty();
    PatternPtr singlePattern = join()->withSingle(Patterns::tableScan());
    PatternPtr anyPattern = join()->withAny(Patterns::tableScan());
    PatternPtr allPattern = join()->withAll(Patterns::tableScan());

    ASSERT_TRUE(emptyPattern->matches(joinNode1));
    ASSERT_TRUE(!singlePattern->matches(joinNode1));
    ASSERT_TRUE(!anyPattern->matches(joinNode1));
    ASSERT_TRUE(!allPattern->matches(joinNode1));
    ASSERT_TRUE(!emptyPattern->matches(joinNode2));
    ASSERT_TRUE(singlePattern->matches(joinNode2));
    ASSERT_TRUE(anyPattern->matches(joinNode2));
    ASSERT_TRUE(allPattern->matches(joinNode2));
    ASSERT_TRUE(!emptyPattern->matches(joinNode3));
    ASSERT_TRUE(!singlePattern->matches(joinNode3));
    ASSERT_TRUE(!anyPattern->matches(joinNode3));
    ASSERT_TRUE(!allPattern->matches(joinNode3));
    ASSERT_TRUE(!emptyPattern->matches(joinNode4));
    ASSERT_TRUE(!singlePattern->matches(joinNode4));
    ASSERT_TRUE(anyPattern->matches(joinNode4));
    ASSERT_TRUE(!allPattern->matches(joinNode4));
    ASSERT_TRUE(!emptyPattern->matches(joinNode5));
    ASSERT_TRUE(!singlePattern->matches(joinNode5));
    ASSERT_TRUE(anyPattern->matches(joinNode5));
    ASSERT_TRUE(allPattern->matches(joinNode5));
}

TEST(OptimizerPatternTest, WithPatternsSubPatternCanUseOuterCaptures)
{
    Capture read1DbCap;
    Capture read1TableCap;
    Capture read2DbCap;
    Capture read2TableCap;

    PlanNodePtr node = createTableScanNode("db1", "t1", {createTableScanNode("db1", "t2", {})});

    std::function<std::string(const MockedTableScanStep &)> dbProperty = [](const auto & step) { return step.database; };
    std::function<std::string(const MockedTableScanStep &)> tableProperty = [](const auto & step) { return step.table; };

    PatternPtr pattern = tableScan()
                             ->capturedStepAs<MockedTableScanStep>(read1DbCap, dbProperty)
                             ->capturedStepAs<MockedTableScanStep>(read1TableCap, tableProperty)
                             ->withSingle(tableScan()
                                              ->capturedStepAs<MockedTableScanStep>(read2DbCap, dbProperty)
                                              ->capturedStepAs<MockedTableScanStep>(read2TableCap, tableProperty)
                                              ->matchingCapture([&](const Captures & caps) {
                                                  return caps.at<std::string>(read1DbCap) == caps.at<std::string>(read2DbCap)
                                                      && caps.at<std::string>(read1TableCap) != caps.at<std::string>(read2TableCap);
                                              }));

    ASSERT_TRUE(pattern->matches(node));
}

TEST(OptimizerPatternTest, WithPatternsOuterPatternCanUseSubCaptures)
{
    Capture read1DbCap;
    Capture read1TableCap;
    Capture read2DbCap;
    Capture read2TableCap;

    PlanNodePtr node = createTableScanNode("db1", "t1", {createTableScanNode("db1", "t2", {})});

    std::function<std::string(const MockedTableScanStep &)> dbProperty = [](const auto & step) { return step.database; };
    std::function<std::string(const MockedTableScanStep &)> tableProperty = [](const auto & step) { return step.table; };

    PatternPtr pattern = tableScan()
                             ->capturedStepAs<MockedTableScanStep>(read1DbCap, dbProperty)
                             ->capturedStepAs<MockedTableScanStep>(read1TableCap, tableProperty)
                             ->withSingle(tableScan()
                                              ->capturedStepAs<MockedTableScanStep>(read2DbCap, dbProperty)
                                              ->capturedStepAs<MockedTableScanStep>(read2TableCap, tableProperty))
                             ->matchingCapture([&](const Captures & caps) {
                                 return caps.at<std::string>(read1DbCap) == caps.at<std::string>(read2DbCap)
                                     && caps.at<std::string>(read1TableCap) != caps.at<std::string>(read2TableCap);
                             });

    ASSERT_TRUE(pattern->matches(node));
}

// Test Case 1
// Plan 1: Filter("col1", "gt")
// Plan 2: Filter("col1", "gt") -> Filter("col2", "equals")
// Plan 3: Filter("col1", "gt") -> Filter("col1", "lt") -> Filter("col2", "equals")
// Pattern 1:
//        typeOf: Filter
//        capturedAs: filter1@1
//        filter: filterIsGt
//        withSingle:
//            typeOf: Filter
//            capturedAs: filter2@2
// Pattern 2:
//        typeOf: Filter
//        capturedAs: filter1@1
//        filter: filterIsGt
//        withSingle:
//            typeOf: Filter
//            capturedAs: filter2@2
//            filter: filterIsLt
TEST(OptimizerPatternTest, Case1)
{
    PlanNodePtr plan1 = createFilterNode("col1", "gt", {});
    PlanNodePtr plan2 = createFilterNode("col1", "gt", {createFilterNode("col2", "equals", {})});
    PlanNodePtr plan3 = createFilterNode("col1", "gt", {createFilterNode("col1", "lt", {createFilterNode("col2", "equals", {})})});

    auto filterIsGtPred = [](const MockedFilterStep & s) { return s.filter == "gt"; };

    auto filterIsLtPred = [](const MockedFilterStep & s) { return s.filter == "lt"; };

    Capture filter1Cap{"filter1"};
    Capture filter2Cap{"filter2"};

    PatternPtr pattern1 = filter()
                              ->capturedAs(filter1Cap)
                              ->matchingStep<MockedFilterStep>(filterIsGtPred, "filterIsGt")
                              ->withSingle(filter()->capturedAs(filter2Cap));

    PatternPtr pattern2 = filter()
                              ->capturedAs(filter1Cap)
                              ->matchingStep<MockedFilterStep>(filterIsGtPred, "filterIsGt")
                              ->withSingle(filter()->capturedAs(filter2Cap)->matchingStep<MockedFilterStep>(filterIsLtPred, "filterIsLt"));

    ASSERT_TRUE(!pattern1->matches(plan1));

    ASSERT_TRUE(pattern1->matches(plan2));
    ASSERT_TRUE(pattern1->match(plan2)->captures.size() == 2);
    ASSERT_TRUE(pattern1->match(plan2)->captures.at<PlanNodePtr>(filter1Cap) == plan2);
    ASSERT_TRUE(pattern1->match(plan2)->captures.at<PlanNodePtr>(filter2Cap) == plan2->getChildren()[0]);

    ASSERT_TRUE(pattern1->matches(plan3));
    ASSERT_TRUE(pattern1->match(plan3)->captures.size() == 2);
    ASSERT_TRUE(pattern1->match(plan3)->captures.at<PlanNodePtr>(filter1Cap) == plan3);
    ASSERT_TRUE(pattern1->match(plan3)->captures.at<PlanNodePtr>(filter2Cap) == plan3->getChildren()[0]);

    ASSERT_TRUE(!pattern2->matches(plan1));

    ASSERT_TRUE(!pattern2->matches(plan2));

    ASSERT_TRUE(pattern2->matches(plan3));
    ASSERT_TRUE(pattern2->match(plan3)->captures.size() == 2);
    ASSERT_TRUE(pattern2->match(plan3)->captures.at<PlanNodePtr>(filter1Cap) == plan3);
    ASSERT_TRUE(pattern2->match(plan3)->captures.at<PlanNodePtr>(filter2Cap) == plan3->getChildren()[0]);
}

// Test Case 2
// Plan 1: Aggregating("col2", "sum") -> Filter("col1", "gt") -> Read("db1", "t1")
// Plan 2: Filter("col1", "gt") -> Aggregating("col2", "sum") -> Read("db1", "t1")
// Pattern 1:
//         typeOf: Aggregating
//         capturedAs: aggregating@3
//         withSingle:
//              typeOf: Filter
//              capturedAs: filter@4
//              withSingle:
//                 typeOf: TableScan
//                 capturedAs: read@5
//
// Pattern 2:
//         typeOf: Any
//         capturedAs: any@6
//         withSingle:
//              typeOf: Any
//              withSingle:
//                  typeOf: TableScan
//
// Pattern 3:
//         typeOf: Any
//         capturedAs: any@6
//         withSingle:
//             typeOf: Aggregating
//             capturedAs: aggregating@3
//
// Pattern 4:
//         typeOf: Any
//         capturedAs: any@6
//         withSingle:
//              typeOf: Aggregating
//              filter: notFinalAgg
//              capturedAs: aggregating@3
TEST(OptimizerPatternTest, Case2)
{
    PlanNodePtr plan1
        = createAggregatingNode("col2", "sum", true, {createFilterNode("col1", "gt", {createTableScanNode("db1", "t1", {})})});

    PlanNodePtr plan2
        = createFilterNode("col1", "gt", {createAggregatingNode("col2", "sum", true, {createTableScanNode("db1", "t1", {})})});

    Capture aggCap{"aggregating"};
    Capture filterCap{"filter"};
    Capture readCap{"read"};
    Capture anyCap{"any"};

    PatternPtr pattern1
        = aggregating()->capturedAs(aggCap)->withSingle(filter()->capturedAs(filterCap)->withSingle(tableScan()->capturedAs(readCap)));

    PatternPtr pattern2 = DB::Patterns::any()->capturedAs(anyCap)->withSingle(DB::Patterns::any()->withSingle(tableScan()));

    PatternPtr pattern3 = DB::Patterns::any()->capturedAs(anyCap)->withSingle(aggregating()->capturedAs(aggCap));

    PatternPtr pattern4 = DB::Patterns::any()->capturedAs(anyCap)->withSingle(
        aggregating()->matchingStep<MockedAggregatingStep>([](const auto & s) { return !s.final; }, "notFinalAgg")->capturedAs(aggCap));

    ASSERT_TRUE(pattern1->matches(plan1));
    ASSERT_TRUE(pattern1->match(plan1)->captures.size() == 3);
    ASSERT_TRUE(pattern1->match(plan1)->captures.at<PlanNodePtr>(aggCap) == plan1);
    ASSERT_TRUE(pattern1->match(plan1)->captures.at<PlanNodePtr>(filterCap) == plan1->getChildren()[0]);
    ASSERT_TRUE(pattern1->match(plan1)->captures.at<PlanNodePtr>(readCap) == plan1->getChildren()[0]->getChildren()[0]);

    ASSERT_TRUE(!pattern1->matches(plan2));

    ASSERT_TRUE(pattern2->matches(plan1));
    ASSERT_TRUE(pattern2->match(plan1)->captures.size() == 1);
    ASSERT_TRUE(pattern2->match(plan1)->captures.at<PlanNodePtr>(anyCap) == plan1);

    ASSERT_TRUE(pattern2->matches(plan2));
    ASSERT_TRUE(pattern2->match(plan2)->captures.size() == 1);
    ASSERT_TRUE(pattern2->match(plan2)->captures.at<PlanNodePtr>(anyCap) == plan2);

    ASSERT_TRUE(!pattern3->matches(plan1));

    ASSERT_TRUE(pattern3->matches(plan2));
    ASSERT_TRUE(pattern3->match(plan2)->captures.size() == 2);
    ASSERT_TRUE(pattern3->match(plan2)->captures.at<PlanNodePtr>(anyCap) == plan2);
    ASSERT_TRUE(pattern3->match(plan2)->captures.at<PlanNodePtr>(aggCap) == plan2->getChildren()[0]);

    ASSERT_TRUE(!pattern4->matches(plan1));

    ASSERT_TRUE(!pattern4->matches(plan2));
}

// Test Case 3
// Plan 1: Aggregating("col2", "sum") -> Join(INNER, {"col1"}) -> Read("db1", "t1")
//                                                             -> Exchange(BROADCAST) -> Read("db1", "t2")
// Pattern 1:
//         typeOf: Aggregating
//         capturedAs: aggregating@7
//         withSingle:
//             typeOf: Join
//             capturedAs: join@8
//
// Pattern 2:
//         typeOf: Aggregating
//         capturedAs: aggregating@7
//         withSingle:
//             typeOf: Join
//             capturedAs: join@8
//             withSingle:
//                 typeOf: TableScan
//
// Pattern 3:
//         typeOf: Aggregating
//         capturedAs: aggregating@7
//         withSingle:
//             typeOf: Join
//             capturedAs: join@8
//             withAll:
//                 typeOf: TableScan
//
// Pattern 4:
//         typeOf: Aggregating
//         capturedAs: aggregating@7
//         withSingle:
//             typeOf: Join
//             filter: isInner
//             capturedAs: join@8
//             withAny:
//                 typeOf: TableScan
//                 capturedAs: read1@9
//             withAny:
//                 typeOf: Exchange
//                 capturedAs: exchange@10
//                 withSingle:
//                     typeOf: TableScan
//                     capturedAs: read2@11
TEST(OptimizerPatternTest, Case3)
{
    PlanNodePtr plan = createAggregatingNode(
        "col2",
        "sum",
        true,
        {createJoinNode(
            ASTTableJoin::Kind::Inner,
            {"col1"},
            {createTableScanNode("db1", "t1", {}), createExchangeNode(ExchangeMode::BROADCAST, {createTableScanNode("db1", "t2", {})})})});

    Capture aggCap{"aggregating"};
    Capture joinCap{"join"};
    Capture read1Cap{"read1"};
    Capture exchangeCap{"exchange"};
    Capture read2Cap{"read2"};

    PatternPtr pattern1 = aggregating()->capturedAs(aggCap)->withSingle(join()->capturedAs(joinCap));

    PatternPtr pattern2 = aggregating()->capturedAs(aggCap)->withSingle(join()->capturedAs(joinCap)->withSingle(tableScan()));

    PatternPtr pattern3 = aggregating()->capturedAs(aggCap)->withSingle(join()->capturedAs(joinCap)->withAll(tableScan()));

    PatternPtr pattern4 = aggregating()->capturedAs(aggCap)->withSingle(
        join()
            ->matchingStep<MockedJoinStep>([](const auto & s) { return isInner(s.kind); }, "isInner")
            ->capturedAs(joinCap)
            ->withAny(tableScan()->capturedAs(read1Cap))
            ->withAny(exchange()->capturedAs(exchangeCap)->withSingle(tableScan()->capturedAs(read2Cap))));

    ASSERT_TRUE(pattern1->matches(plan));
    ASSERT_TRUE(pattern1->match(plan)->captures.size() == 2);
    ASSERT_TRUE(pattern1->match(plan)->captures.at<PlanNodePtr>(aggCap) == plan);
    ASSERT_TRUE(pattern1->match(plan)->captures.at<PlanNodePtr>(joinCap) == plan->getChildren()[0]);

    ASSERT_TRUE(!pattern2->matches(plan));

    ASSERT_TRUE(!pattern3->matches(plan));

    ASSERT_TRUE(pattern4->matches(plan));
    ASSERT_TRUE(pattern4->match(plan)->captures.size() == 5);
    ASSERT_TRUE(pattern4->match(plan)->captures.at<PlanNodePtr>(aggCap) == plan);
    ASSERT_TRUE(pattern4->match(plan)->captures.at<PlanNodePtr>(joinCap) == plan->getChildren()[0]);
    ASSERT_TRUE(pattern4->match(plan)->captures.at<PlanNodePtr>(read1Cap) == plan->getChildren()[0]->getChildren()[0]);
    ASSERT_TRUE(pattern4->match(plan)->captures.at<PlanNodePtr>(exchangeCap) == plan->getChildren()[0]->getChildren()[1]);
    ASSERT_TRUE(pattern4->match(plan)->captures.at<PlanNodePtr>(read2Cap) == plan->getChildren()[0]->getChildren()[1]->getChildren()[0]);
}

// Test Case 4
// Plan 1: Join(INNER, {"col1"}) -> Read("db1", "t1")
//                               -> Join(CROSS, {"col2"}) -> Read("db1", "t2")
//                                                        -> Read("db1", "t3")
// Pattern 1:
//         typeOf: Join
//         capturedAs: join1@12
//         withAll:
//             typeOf: Any
//             capturedAs: any@16
//
// Pattern 2:
//         typeOf: Join
//         capturedAs: join1@12
//         withAny:
//             typeOf: Any
//             capturedAs: any@16
//             withAny:
//                 typeOf: TableScan
//                 capturedAs: read1@14
//
// Pattern 3:
//         typeOf: Join
//         capturedAs: join1@12
//         withAny:
//             typeOf: TableScan
//             filter: isDb1
//             capturedAs: read1@14
//         withAny:
//             typeOf: Join
//             capturedAs: join2@13
//             withAll:
//                 typeOf: TableScan
//                 filter: isDb1
//                 capturedAs: read2@15
TEST(OptimizerPatternTest, Case4)
{
    PlanNodePtr plan = createJoinNode(
        ASTTableJoin::Kind::Inner,
        {"col1"},
        {createTableScanNode("db1", "t1", {}),
         createJoinNode(
             ASTTableJoin::Kind::Cross, {"col2"}, {createTableScanNode("db1", "t2", {}), createTableScanNode("db1", "t3", {})})});

    Capture join1Cap{"join1"};
    Capture join2Cap{"join2"};
    Capture read1Cap{"read1"};
    Capture read2Cap{"read2"};
    Capture anyCap{"any"};

    std::function<bool(const MockedTableScanStep &)> isDb1Pred = [](const auto & s) { return s.database == "db1"; };

    PatternPtr pattern1 = join()->capturedAs(join1Cap)->withAll(DB::Patterns::any()->capturedAs(anyCap));

    PatternPtr pattern2
        = join()->capturedAs(join1Cap)->withAny(DB::Patterns::any()->capturedAs(anyCap)->withAny(tableScan()->capturedAs(read1Cap)));

    PatternPtr pattern3
        = join()
              ->capturedAs(join1Cap)
              ->withAny(tableScan()->matchingStep(isDb1Pred, "isDb1")->capturedAs(read1Cap))
              ->withAny(join()->capturedAs(join2Cap)->withAll(tableScan()->matchingStep(isDb1Pred, "isDb1")->capturedAs(read2Cap)));

    ASSERT_TRUE(pattern1->matches(plan));
    ASSERT_TRUE(pattern1->match(plan)->captures.size() == 3);
    ASSERT_TRUE(pattern1->match(plan)->captures.at<PlanNodePtr>(join1Cap) == plan);
    auto pattern1AnyCaps = getCapturesAsSet(*pattern1->match(plan), anyCap);
    std::unordered_set<PlanNodePtr> expectedPattern1AnyCaps = {plan->getChildren()[0], plan->getChildren()[1]};
    ASSERT_TRUE(pattern1AnyCaps == expectedPattern1AnyCaps);

    ASSERT_TRUE(pattern2->matches(plan));
    ASSERT_TRUE(pattern2->match(plan)->captures.size() == 4);
    ASSERT_TRUE(pattern2->match(plan)->captures.at<PlanNodePtr>(join1Cap) == plan);
    ASSERT_TRUE(pattern2->match(plan)->captures.at<PlanNodePtr>(anyCap) == plan->getChildren()[1]);
    auto pattern2ReadCaps = getCapturesAsSet(*pattern2->match(plan), read1Cap);
    std::unordered_set<PlanNodePtr> expectedPattern2ReadCaps
        = {plan->getChildren()[1]->getChildren()[0], plan->getChildren()[1]->getChildren()[1]};
    ASSERT_TRUE(pattern2ReadCaps == expectedPattern2ReadCaps);

    ASSERT_TRUE(pattern3->matches(plan));
    ASSERT_TRUE(pattern3->match(plan)->captures.size() == 5);
    ASSERT_TRUE(pattern3->match(plan)->captures.at<PlanNodePtr>(join1Cap) == plan);
    ASSERT_TRUE(pattern3->match(plan)->captures.at<PlanNodePtr>(read1Cap) == plan->getChildren()[0]);
    ASSERT_TRUE(pattern3->match(plan)->captures.at<PlanNodePtr>(join2Cap) == plan->getChildren()[1]);
    auto pattern3ReadCaps = getCapturesAsSet(*pattern3->match(plan), read2Cap);
    std::unordered_set<PlanNodePtr> expectedPattern3ReadCaps
        = {plan->getChildren()[1]->getChildren()[0], plan->getChildren()[1]->getChildren()[1]};
    ASSERT_TRUE(pattern3ReadCaps == expectedPattern3ReadCaps);
}

// Test Case 5
// Plan 1: Aggregating("ak1", "sum", true) -> Join(Cross, {"jk1"}) -> Exchange(REPARTITION) -> Read("db1", "t1")
//                                                                 -> Exchange(REPARTITION) -> Read("db2", "t2")
//
// Pattern 1:
//       typeOf: Aggregating
//       filter by: isFinalAgg
//       capture aggOp as: @30
//       with single:
//           typeOf: Join
//           capture joinType as: @31
//           with any:
//               typeOf: Exchange
//               capture `this` as: @32
//               with single:
//                   typeOf: TableScan
//                   filter by: isDb1
//                   capture tableName as: @33
TEST(OptimizerPatternTest, Case5)
{
    Capture aggOpCap;
    Capture joinTypeCap;
    Capture excgCap;
    Capture tableCap;

    PlanNodePtr plan = createAggregatingNode(
        "ak1",
        "sum",
        true,
        {createJoinNode(
            ASTTableJoin::Kind::Cross,
            {"jk1"},
            {createExchangeNode(ExchangeMode::REPARTITION, {createTableScanNode("db1", "t1", {})}),
             createExchangeNode(ExchangeMode::REPARTITION, {createTableScanNode("db2", "t2", {})})})});

    PatternPtr pattern1
        = aggregating()
              ->matchingStep<MockedAggregatingStep>(&MockedAggregatingStep::final, "isFinalAgg")
              ->capturedStepAs<MockedAggregatingStep>(aggOpCap, &MockedAggregatingStep::aggregator, "aggOp")
              ->withSingle(join()
                               ->capturedStepAs<MockedJoinStep>(joinTypeCap, &MockedJoinStep::kind, "joinType")
                               ->withAny(exchange()->capturedAs(excgCap)->withSingle(
                                   tableScan()
                                       ->matchingStep<MockedTableScanStep>([](const auto & st) { return st.database == "db1"; }, "isDb1")
                                       ->capturedStepAs<MockedTableScanStep>(tableCap, &MockedTableScanStep::table, "tableName"))));

    ASSERT_TRUE(pattern1->matches(plan));
    ASSERT_TRUE(pattern1->match(plan)->captures.size() == 4);
    ASSERT_TRUE(pattern1->match(plan)->captures.at<std::string>(aggOpCap) == "sum");
    ASSERT_TRUE(pattern1->match(plan)->captures.at<ASTTableJoin::Kind>(joinTypeCap) == ASTTableJoin::Kind::Cross);
    ASSERT_TRUE(pattern1->match(plan)->captures.at<PlanNodePtr>(excgCap) == plan->getChildren()[0]->getChildren()[0]);
    ASSERT_TRUE(pattern1->match(plan)->captures.at<std::string>(tableCap) == "t1");
}
