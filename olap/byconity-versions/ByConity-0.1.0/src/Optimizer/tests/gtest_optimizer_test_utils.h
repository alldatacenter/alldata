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

#pragma once

#include <Core/Names.h>
#include <Interpreters/DistributedStages/ExchangeMode.h>
#include <Optimizer/Iterative/IterativeRewriter.h>
#include <Optimizer/Rule/Patterns.h>
#include <QueryPlan/IQueryPlanStep.h>

#include <optional>
#include <stdexcept>

#define ASSERT_THROW_DB_EXCEPTION_WITH_ERROR_CODE(stat, errcode) \
    do \
    { \
        try \
        { \
            stat; \
        } \
        catch (const DB::Exception & e) \
        { \
            if (e.code() == errcode) \
                break; \
        } \
        catch (...) \
        { \
        } \
        GTEST_FAIL() << "Expected exception with error code " << errcode; \
    } while (0)

#define ASSERT_THROW_DB_EXCEPTION_WITH_MESSAGE_PREFIX(stat, msgPrefix) \
    do \
    { \
        try \
        { \
            stat; \
        } \
        catch (const DB::Exception & e) \
        { \
            if (strncmp(e.message().data(), msgPrefix, strlen(msgPrefix)) == 0) \
                break; \
        } \
        catch (...) \
        { \
        } \
        GTEST_FAIL() << "Expected exception with error msg " << msgPrefix; \
    } while (0)

namespace DB
{
struct MockedTableScanStep : public IQueryPlanStep
{
    std::string database;
    std::string table;

    MockedTableScanStep(std::string database_, std::string table_, std::optional<DataStream> output_stream_)
        : database(database_), table(table_)
    {
        output_stream = std::move(output_stream_);
    }
    String getName() const override { return "TableScan"; }
    Type getType() const override { return IQueryPlanStep::Type::TableScan; }
    QueryPipelinePtr updatePipeline(QueryPipelines, const BuildQueryPipelineSettings &) override { return {}; }
    void setInputStreams(const DataStreams &) override { }
    void serialize(WriteBuffer &) const override { }
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr) const override
    {
        return std::make_shared<MockedTableScanStep>(database, table, output_stream);
    }
};

struct MockedFilterStep : public IQueryPlanStep
{
    std::string column;
    std::string filter;

    MockedFilterStep(std::string column_, std::string filter_, std::optional<DataStream> output_stream_) : column(column_), filter(filter_)
    {
        output_stream = std::move(output_stream_);
    }
    String getName() const override { return "Filter"; }
    IQueryPlanStep::Type getType() const override { return IQueryPlanStep::Type::Filter; }
    QueryPipelinePtr updatePipeline(QueryPipelines, const BuildQueryPipelineSettings &) override { return {}; }
    void setInputStreams(const DataStreams &) override { }
    void serialize(WriteBuffer &) const override { }
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr) const override
    {
        return std::make_shared<MockedFilterStep>(column, filter, output_stream);
    }
};

struct MockedExchangeStep : public IQueryPlanStep
{
    ExchangeMode mode;

    MockedExchangeStep(ExchangeMode mode_, std::optional<DataStream> output_stream_) : mode(mode_)
    {
        output_stream = std::move(output_stream_);
    }
    String getName() const override { return "Exchange"; }
    IQueryPlanStep::Type getType() const override { return IQueryPlanStep::Type::Exchange; }
    QueryPipelinePtr updatePipeline(QueryPipelines, const BuildQueryPipelineSettings &) override { return {}; }
    void setInputStreams(const DataStreams &) override { }
    void serialize(WriteBuffer &) const override { }
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr) const override { return std::make_shared<MockedExchangeStep>(mode, output_stream); }
};

struct MockedJoinStep : public IQueryPlanStep
{
    ASTTableJoin::Kind kind;
    Names keys;

    MockedJoinStep(ASTTableJoin::Kind kind_, Names keys_, std::optional<DataStream> output_stream_) : kind(kind_), keys(keys_)
    {
        output_stream = std::move(output_stream_);
    }
    String getName() const override { return "Join"; }
    IQueryPlanStep::Type getType() const override { return IQueryPlanStep::Type::Join; }
    QueryPipelinePtr updatePipeline(QueryPipelines, const BuildQueryPipelineSettings &) override { return {}; }
    void setInputStreams(const DataStreams &) override { }
    void serialize(WriteBuffer &) const override { }
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr) const override { return std::make_shared<MockedJoinStep>(kind, keys, output_stream); }
};

struct MockedAggregatingStep : public IQueryPlanStep
{
    std::string key;
    std::string aggregator;
    bool final;

    MockedAggregatingStep(std::string key_, std::string aggregator_, bool final_, std::optional<DataStream> output_stream_)
        : key(key_), aggregator(aggregator_), final(final_)
    {
        output_stream = std::move(output_stream_);
    }
    String getName() const override { return "Aggregating"; }
    IQueryPlanStep::Type getType() const override { return IQueryPlanStep::Type::Aggregating; }
    QueryPipelinePtr updatePipeline(QueryPipelines, const BuildQueryPipelineSettings &) override { return {}; }
    void setInputStreams(const DataStreams &) override { }
    void serialize(WriteBuffer &) const override { }
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr) const override
    {
        return std::make_shared<MockedAggregatingStep>(key, aggregator, final, output_stream);
    }
};

struct MockedStepForRewriterTest : public IQueryPlanStep
{
    int i;

    MockedStepForRewriterTest(int ii, std::optional<DataStream> output_stream_) : i(ii) { output_stream = std::move(output_stream_); }
    String getName() const override { return "MockedStepForRewriterTest"; }
    IQueryPlanStep::Type getType() const override { return IQueryPlanStep::Type::Filter; }
    QueryPipelinePtr updatePipeline(QueryPipelines, const BuildQueryPipelineSettings &) override { return {}; }
    void setInputStreams(const DataStreams &) override { }
    void serialize(WriteBuffer &) const override { }
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr) const override
    {
        return std::make_shared<MockedStepForRewriterTest>(i, output_stream);
    }
};

QueryPlan createQueryPlan(const PlanNodePtr & plan);
std::shared_ptr<PlanNode<MockedTableScanStep>> createTableScanNode(std::string database, std::string table, PlanNodes children);
std::shared_ptr<PlanNode<MockedFilterStep>> createFilterNode(std::string column, std::string filter, PlanNodes children);
std::shared_ptr<PlanNode<MockedExchangeStep>> createExchangeNode(ExchangeMode mode, PlanNodes children);
std::shared_ptr<PlanNode<MockedJoinStep>> createJoinNode(ASTTableJoin::Kind kind, Names keys, PlanNodes children);
std::shared_ptr<PlanNode<MockedAggregatingStep>>
createAggregatingNode(std::string key, std::string aggregator, bool final, PlanNodes children);
std::shared_ptr<UnionNode> createUnionNode(PlanNodes children);
std::shared_ptr<IntersectNode> createIntersectNode(bool distinct, PlanNodes children);
std::shared_ptr<ExceptNode> createExceptNode(bool distinct, PlanNodes children);
std::shared_ptr<PlanNode<MockedStepForRewriterTest>> createRewriteTestNode(int ii, PlanNodes children);

std::shared_ptr<PlanNode<MockedTableScanStep>>
createTableScanNode(std::string database, std::string table, std::optional<DataStream> output_stream_, PlanNodes children);
std::shared_ptr<PlanNode<MockedFilterStep>>
createFilterNode(std::string column, std::string filter, std::optional<DataStream> output_stream_, PlanNodes children);
std::shared_ptr<PlanNode<MockedExchangeStep>>
createExchangeNode(ExchangeMode mode, std::optional<DataStream> output_stream_, PlanNodes children);
std::shared_ptr<PlanNode<MockedJoinStep>>
createJoinNode(ASTTableJoin::Kind kind, Names keys, std::optional<DataStream> output_stream_, PlanNodes children);
std::shared_ptr<PlanNode<MockedAggregatingStep>>
createAggregatingNode(std::string key, std::string aggregator, bool final, std::optional<DataStream> output_stream_, PlanNodes children);
std::shared_ptr<UnionNode> createUnionNode(std::optional<DataStream> output_stream_, PlanNodes children);
std::shared_ptr<IntersectNode> createIntersectNode(bool distinct, std::optional<DataStream> output_stream_, PlanNodes children);
std::shared_ptr<ExceptNode> createExceptNode(bool distinct, std::optional<DataStream> output_stream_, PlanNodes children);
std::shared_ptr<PlanNode<MockedStepForRewriterTest>>
createRewriteTestNode(int ii, std::optional<DataStream> output_stream_, PlanNodes children);

DataStream createDataStream(Names names);
DataStreams collectInputStreams(const PlanNodes & nodes);
void check_data_stream(const DataStream & ds1, const DataStream & ds2);
void check_input_stream(PlanNodePtr node);
void replaceChildrenAndInputStream(PlanNodePtr node, PlanNodes & children_);

void initTestDB(DB::ContextMutablePtr context);
std::string executeTestDBQuery(const std::string & query, DB::ContextMutablePtr context, bool log_query = false);
}
