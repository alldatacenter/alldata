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

#include <DataTypes/DataTypeString.h>
#include <Databases/DatabaseMemory.h>
#include <IO/ReadBufferFromString.h>
#include <IO/WriteBufferFromString.h>
#include <Interpreters/executeQuery.h>
#include <Optimizer/tests/gtest_optimizer_test_utils.h>
#include <QueryPlan/ExceptStep.h>
#include <QueryPlan/IntersectStep.h>
#include <QueryPlan/UnionStep.h>
#include <Common/tests/gtest_global_register.h>

#include <gtest/gtest.h>

using namespace DB;
using namespace DB::Patterns;

namespace DB
{
namespace
{
    auto id_allocator = std::make_shared<PlanNodeIdAllocator>();
}

QueryPlan createQueryPlan(const PlanNodePtr & plan)
{
    return QueryPlan{plan, id_allocator};
}

std::shared_ptr<PlanNode<MockedTableScanStep>> createTableScanNode(std::string database, std::string table, DB::PlanNodes children)
{
    return createTableScanNode(database, table, {DataStream{}}, children);
}

std::shared_ptr<PlanNode<MockedFilterStep>> createFilterNode(std::string column, std::string filter, DB::PlanNodes children)
{
    return createFilterNode(column, filter, {DataStream{}}, children);
}

std::shared_ptr<PlanNode<MockedExchangeStep>> createExchangeNode(DB::ExchangeMode mode, DB::PlanNodes children)
{
    return createExchangeNode(mode, {DataStream{}}, children);
}

std::shared_ptr<PlanNode<MockedJoinStep>> createJoinNode(DB::ASTTableJoin::Kind kind, DB::Names keys, DB::PlanNodes children)
{
    return createJoinNode(kind, keys, {DataStream{}}, children);
}

std::shared_ptr<PlanNode<MockedAggregatingStep>>
createAggregatingNode(std::string key, std::string aggregator, bool final, DB::PlanNodes children)
{
    return createAggregatingNode(key, aggregator, final, {DataStream{}}, children);
}

std::shared_ptr<PlanNode<UnionStep>> createUnionNode(DB::PlanNodes children)
{
    return createUnionNode({DataStream{}}, children);
}

std::shared_ptr<PlanNode<IntersectStep>> createIntersectNode(bool distinct, DB::PlanNodes children)
{
    return createIntersectNode(distinct, {DataStream{}}, children);
}

std::shared_ptr<PlanNode<ExceptStep>> createExceptNode(bool distinct, DB::PlanNodes children)
{
    return createExceptNode(distinct, {DataStream{}}, children);
}

std::shared_ptr<PlanNode<MockedStepForRewriterTest>> createRewriteTestNode(int ii, DB::PlanNodes children)
{
    return createRewriteTestNode(ii, {DataStream{}}, children);
}

std::shared_ptr<PlanNode<MockedTableScanStep>>
createTableScanNode(std::string database, std::string table, std::optional<DataStream> output_stream_, PlanNodes children)
{
    auto step = std::make_unique<MockedTableScanStep>(database, table, output_stream_);
    auto node = std::make_shared<PlanNode<MockedTableScanStep>>(id_allocator->nextId(), std::move(step));
    replaceChildrenAndInputStream(node, children);
    return node;
}

std::shared_ptr<PlanNode<MockedFilterStep>>
createFilterNode(std::string column, std::string filter, std::optional<DataStream> output_stream_, PlanNodes children)
{
    auto step = std::make_unique<const MockedFilterStep>(column, filter, output_stream_);
    auto node = std::make_shared<PlanNode<MockedFilterStep>>(id_allocator->nextId(), std::move(step));
    replaceChildrenAndInputStream(node, children);
    return node;
}

std::shared_ptr<PlanNode<MockedExchangeStep>>
createExchangeNode(ExchangeMode mode, std::optional<DataStream> output_stream_, PlanNodes children)
{
    auto step = std::make_unique<MockedExchangeStep>(mode, output_stream_);
    auto node = std::make_shared<PlanNode<MockedExchangeStep>>(id_allocator->nextId(), std::move(step));
    replaceChildrenAndInputStream(node, children);
    return node;
}

std::shared_ptr<PlanNode<MockedJoinStep>>
createJoinNode(ASTTableJoin::Kind kind, Names keys, std::optional<DataStream> output_stream_, PlanNodes children)
{
    auto step = std::make_unique<MockedJoinStep>(kind, keys, output_stream_);
    auto node = std::make_shared<PlanNode<MockedJoinStep>>(id_allocator->nextId(), std::move(step));
    replaceChildrenAndInputStream(node, children);
    return node;
}

std::shared_ptr<PlanNode<MockedAggregatingStep>>
createAggregatingNode(std::string key, std::string aggregator, bool final, std::optional<DataStream> output_stream_, PlanNodes children)
{
    auto step = std::make_unique<MockedAggregatingStep>(key, aggregator, final, output_stream_);
    auto node = std::make_shared<PlanNode<MockedAggregatingStep>>(id_allocator->nextId(), std::move(step));
    replaceChildrenAndInputStream(node, children);
    return node;
}

std::shared_ptr<PlanNode<UnionStep>> createUnionNode(std::optional<DataStream> output_stream_, DB::PlanNodes children)
{
    DataStreams input_stream_ = collectInputStreams(children);
    auto step = std::make_unique<UnionStep>(input_stream_, output_stream_ ? *output_stream_ : DataStream{}, false);
    return std::make_shared<PlanNode<UnionStep>>(id_allocator->nextId(), std::move(step), children);
}

std::shared_ptr<PlanNode<IntersectStep>>
createIntersectNode(bool distinct, std::optional<DataStream> output_stream_, DB::PlanNodes children)
{
    DataStreams input_stream_ = collectInputStreams(children);
    auto step = std::make_unique<IntersectStep>(input_stream_, output_stream_ ? *output_stream_ : DataStream{}, distinct);
    return std::make_shared<PlanNode<IntersectStep>>(id_allocator->nextId(), std::move(step), children);
}

std::shared_ptr<PlanNode<ExceptStep>> createExceptNode(bool distinct, std::optional<DataStream> output_stream_, DB::PlanNodes children)
{
    DataStreams input_stream_ = collectInputStreams(children);
    auto step = std::make_unique<ExceptStep>(input_stream_, output_stream_ ? *output_stream_ : DataStream{}, distinct);
    return std::make_shared<PlanNode<ExceptStep>>(id_allocator->nextId(), std::move(step), children);
}

std::shared_ptr<PlanNode<MockedStepForRewriterTest>>
createRewriteTestNode(int ii, std::optional<DataStream> output_stream_, DB::PlanNodes children)
{
    auto step = std::make_unique<MockedStepForRewriterTest>(ii, output_stream_);
    auto node = std::make_shared<PlanNode<MockedStepForRewriterTest>>(id_allocator->nextId(), std::move(step));
    replaceChildrenAndInputStream(node, children);
    return node;
}

DataStream createDataStream(Names names)
{
    NamesAndTypes ds;
    std::transform(names.begin(), names.end(), std::back_inserter(ds), [](std::string & name) -> NameAndTypePair {
        return {name, std::make_shared<DataTypeString>()};
    });

    return {ds};
}

DataStreams collectInputStreams(const PlanNodes & nodes)
{
    DataStreams res;
    std::transform(
        nodes.begin(), nodes.end(), std::back_inserter(res), [](auto & node_ptr) { return node_ptr->getStep()->getOutputStream(); });
    return res;
}

void check_data_stream(const DataStream & ds1, const DataStream & ds2)
{
    ASSERT_TRUE(ds1.header.columns() == ds2.header.columns());

    for (size_t j = 0; j < ds1.header.columns(); ++j)
    {
        ASSERT_TRUE(ds1.header.getByPosition(j).name == ds2.header.getByPosition(j).name);
        ASSERT_TRUE(ds1.header.getByPosition(j).type->getName() == ds2.header.getByPosition(j).type->getName());
    }
}

void check_input_stream(PlanNodePtr node)
{
    const auto & inputs = node->getStep()->getInputStreams();
    //    ASSERT_TRUE(inputs.size() == node->getChildren().size());

    for (size_t i = 0; i < inputs.size(); ++i)
    {
        auto & in = inputs[i];
        auto & out = node->getChildren()[i]->getStep()->getOutputStream();

        check_data_stream(in, out);
    }
}

void replaceChildrenAndInputStream(PlanNodePtr node, PlanNodes & children_)
{
    DataStreams input_streams_;

    std::transform(children_.begin(), children_.end(), std::back_inserter(input_streams_), [](PlanNodePtr & plan_node) {
        return plan_node->getStep()->getOutputStream();
    });

    node->replaceChildren(children_);
}

void initTestDB(ContextMutablePtr context)
{
    context->makeGlobalContext();
    context->setPath("./");

    tryRegisterFunctions();
    tryRegisterFormats();
    tryRegisterStorages();
    tryRegisterAggregateFunctions();

    context->setCurrentQueryId("");
    context->setSetting("enable_optimizer", false);

    auto testdb = std::make_shared<DatabaseMemory>("testdb", context);

    DB::DatabasePtr database = std::make_shared<DB::DatabaseMemory>("testdb", context);
    DB::DatabaseCatalog::instance().attachDatabase("testdb", database);
    context->setCurrentDatabase("testdb");
}

std::string executeTestDBQuery(const std::string & query, ContextMutablePtr context, bool log_query)
{
    String res;
    ReadBufferFromString is1(query);
    WriteBufferFromString os1(res);

    if (log_query)
        std::cout << "Execute test DB query: "
                  << "\033[1;34m" << query << "\033[0m" << std::endl
                  << std::flush;

    executeQuery(is1, os1, false, context, {}, {});

    if (log_query)
        std::cout << "Finish test DB query, got result: " << std::endl << "\033[1;32m" << res << "\033[0m" << std::flush;

    return res;
}
}
