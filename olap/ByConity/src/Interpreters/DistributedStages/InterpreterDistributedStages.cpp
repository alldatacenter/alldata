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

#include <chrono>
#include <memory>
#include <thread>
#include <Client/Connection.h>
#include <Interpreters/Cluster.h>
#include <Interpreters/Context_fwd.h>
#include <Interpreters/DistributedStages/ExchangeStepVisitor.h>
#include <Interpreters/DistributedStages/InterpreterDistributedStages.h>
#include <Interpreters/DistributedStages/InterpreterPlanSegment.h>
#include <Interpreters/DistributedStages/PlanSegmentSplitter.h>
#include <Interpreters/DistributedStages/executePlanSegment.h>
#include <Interpreters/InterpreterSelectWithUnionQuery.h>
#include <Interpreters/InterpreterSetQuery.h>
#include <Interpreters/RewriteDistributedQueryVisitor.h>
#include <Interpreters/SegmentScheduler.h>
#include <Parsers/ASTInsertQuery.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSelectWithUnionQuery.h>
#include <Parsers/ASTSubquery.h>
#include <Parsers/IAST.h>
#include <Storages/IStorage.h>
#include <Storages/SelectQueryInfo.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int UNKNOWN_PACKET_FROM_SERVER;
}

InterpreterDistributedStages::InterpreterDistributedStages(const ASTPtr & query_ptr_, ContextMutablePtr context_)
    : query_ptr(query_ptr_->clone())
    , context(std::move(context_))
    , log(&Poco::Logger::get("InterpreterDistributedStages"))
    , plan_segment_tree(std::make_unique<PlanSegmentTree>())
{
    initSettings();

    createPlanSegments();
}

void InterpreterDistributedStages::createPlanSegments()
{
    /**
     * we collect all distributed tables and try to rewrite distributed query into local query because
     * distributed query cannot generate a plan with ScanTable so that it hard to build distributed plan segment on this
     * kind of plan.
     *
     * if there is any local table or there is no tables, we treat the query as original query so that it will
     * not be splited to several segments, instead it only has one segment and we will execute this segment in local server
     * as it original worked.
     */
    bool add_exchange = false;
    auto query_data = RewriteDistributedQueryMatcher::collectTableInfos(query_ptr, context);
    if (query_data.all_distributed && !query_data.table_rewrite_info.empty())
    {
        RewriteDistributedQueryVisitor(query_data).visit(query_ptr);
        add_exchange = true;
    }

    QueryPlan query_plan;
    SelectQueryOptions options;
    if (add_exchange)
        options.distributedStages();

    InterpreterSelectWithUnionQuery(query_ptr, context, options).buildQueryPlan(query_plan);

    if (add_exchange)
    {
        ExchangeStepContext exchange_context{.context = context, .query_plan = query_plan};
        AddExchangeRewriter::rewrite(query_plan, exchange_context);
    }

    WriteBufferFromOwnString plan_str;
    query_plan.explainPlan(plan_str, {});
    LOG_DEBUG(log, "QUERY-PLAN-AFTER-EXCHANGE \n" + plan_str.str());

    PlanSegmentContext plan_segment_context{.context = context,
                                            .query_plan = query_plan,
                                            .query_id = context->getCurrentQueryId(),
                                            .shard_number = query_data.cluster ? query_data.cluster->getShardCount() : 1,
                                            .cluster_name = query_data.cluster_name,
                                            .plan_segment_tree = plan_segment_tree.get()};
    PlanSegmentSplitter::split(query_plan, plan_segment_context);

    LOG_DEBUG(log, "PLAN-SEGMENTS \n" + plan_segment_tree->toString());
}

BlockIO InterpreterDistributedStages::execute()
{
    return executePlanSegment();
}

PlanSegmentPtr MockPlanSegment(ContextPtr context)
{
    PlanSegmentPtr plan_segment = std::make_unique<PlanSegment>();

    PlanSegmentInputPtr left = std::make_shared<PlanSegmentInput>(PlanSegmentType::EXCHANGE);
    PlanSegmentInputPtr right = std::make_shared<PlanSegmentInput>(PlanSegmentType::EXCHANGE);
    PlanSegmentOutputPtr output = std::make_shared<PlanSegmentOutput>(PlanSegmentType::OUTPUT);

    plan_segment->appendPlanSegmentInput(left);
    plan_segment->appendPlanSegmentInput(right);
    plan_segment->setPlanSegmentOutput(output);

    /***
     * only read from system.one
     */
    StorageID table_id = StorageID("system", "one");
    StoragePtr storage = DatabaseCatalog::instance().getTable(table_id, context);


    QueryPlan query_plan;
    SelectQueryInfo select_query_info;
    storage->read(query_plan, {"dummy"}, storage->getInMemoryMetadataPtr(), select_query_info, context, {}, 0, 0);

    plan_segment->setQueryPlan(std::move(query_plan));

    return plan_segment;
}

void MockSendPlanSegment(ContextPtr query_context)
{
    auto plan_segment = MockPlanSegment(query_context);

    auto cluster = query_context->getCluster("test_shard_localhost");

    /**
     * only get the current node
     */
    auto node = cluster->getShardsAddresses().back()[0];

    auto connection = std::make_shared<Connection>(
                    node.host_name, node.port, node.default_database,
                    node.user, node.password, node.cluster, node.cluster_secret,
                    "MockSendPlanSegment", node.compression, node.secure);

    const auto & settings = query_context->getSettingsRef();
    auto connection_timeouts = ConnectionTimeouts::getTCPTimeoutsWithFailover(settings);
    connection->sendPlanSegment(connection_timeouts, plan_segment.get(), &settings, &query_context->getClientInfo());
    connection->poll(1000);
    Packet packet = connection->receivePacket();
    LOG_TRACE(&Poco::Logger::get("MockSendPlanSegment"), "sendPlanSegmentToLocal finish:" + std::to_string(packet.type));
    switch (packet.type)
    {
        case Protocol::Server::Exception:
            throw *packet.exception;
        case Protocol::Server::EndOfStream:
            break;
        default:
            throw Exception("Unknown packet from server", ErrorCodes::UNKNOWN_PACKET_FROM_SERVER);
    }
    connection->disconnect();
}

void checkPlan(PlanSegment * lhs, PlanSegment * rhs)
{
    auto lhs_str = lhs->toString();
    auto rhs_str = rhs->toString();

    if(lhs_str != rhs_str)
        throw Exception("checkPlan failed", ErrorCodes::LOGICAL_ERROR);
}

void MockTestQuery(PlanSegmentTree * plan_segment_tree, ContextMutablePtr context)
{
    if (plan_segment_tree->getNodes().size() < 2)
        return;
    /**
     * serialize to buffer
     */
    WriteBufferFromOwnString write_buffer;
    writeBinary(plan_segment_tree->getNodes().size(), write_buffer);
    for (auto & node : plan_segment_tree->getNodes())
        node.plan_segment->serialize(write_buffer);

    ReadBufferFromString read_buffer(write_buffer.str());
    size_t plan_size;
    readBinary(plan_size, read_buffer);
    std::vector<std::shared_ptr<PlanSegment>> plansegments;

    for (size_t i = 0; i < plan_size; ++i)
    {
        auto plan = std::make_shared<PlanSegment>(context);
        plan->deserialize(read_buffer);
        plansegments.push_back(plan);
    }

    /**
     * check results
     */
    std::vector<PlanSegment *> old_plans;
    for (auto & node : plan_segment_tree->getNodes())
        old_plans.push_back(node.plan_segment.get());

    for (size_t i = 0; i < plan_size; ++i)
    {
        auto lhs = old_plans[i];
        auto rhs = plansegments[i].get();
        checkPlan(lhs, rhs);
    }
}

BlockIO InterpreterDistributedStages::executePlanSegment()
{
    return executePlanSegmentTree(plan_segment_tree, context);
}

void InterpreterDistributedStages::initSettings()
{
    auto query = getQuery();
    const auto * select_with_union = query->as<ASTSelectWithUnionQuery>();
    const auto * select_query = query->as<ASTSelectQuery>();

    if (!select_with_union && !select_query)
        return;

    if (select_with_union)
    {
        size_t num_selects = select_with_union->list_of_selects->children.size();
        if (num_selects == 1)
            select_query = typeid_cast<ASTSelectQuery *>(select_with_union->list_of_selects->children.at(0).get());
    }

    if (select_query && select_query->settings())
        InterpreterSetQuery(select_query->settings(), context).executeForCurrentContext();
}

bool InterpreterDistributedStages::isDistributedStages(const ASTPtr & query, ContextPtr context_)
{
    const auto * select_with_union = query->as<ASTSelectWithUnionQuery>();
    const auto * select_query = query->as<ASTSelectQuery>();

    if (!select_with_union && !select_query)
        return false;

    if (select_with_union)
    {
        size_t num_selects = select_with_union->list_of_selects->children.size();
        if (num_selects == 1)
            select_query = typeid_cast<ASTSelectQuery *>(select_with_union->list_of_selects->children.at(0).get());
    }

    if (select_query && select_query->settings())
    {
        ContextMutablePtr context_clone = Context::createCopy(context_);
        InterpreterSetQuery(select_query->settings(), context_clone).executeForCurrentContext();
        return context_clone->getSettingsRef().enable_distributed_stages;
    }

    return context_->getSettingsRef().enable_distributed_stages;
}

DistributedStagesSettings InterpreterDistributedStages::extractDistributedStagesSettingsImpl(const ASTPtr & query, ContextPtr context_)
{
    const auto * select_with_union = query->as<ASTSelectWithUnionQuery>();
    const auto * select_query = query->as<ASTSelectQuery>();

    if (!select_with_union && !select_query)
        return DistributedStagesSettings(false, false);

    const Settings & settings_ = context_->getSettingsRef();
    DistributedStagesSettings distributed_stages_settings(settings_.enable_distributed_stages && !settings_.enable_optimizer, settings_.fallback_to_simple_query);

    if (select_with_union)
    {
        size_t num_selects = select_with_union->list_of_selects->children.size();
        if (num_selects == 1)
            select_query = typeid_cast<ASTSelectQuery *>(select_with_union->list_of_selects->children.at(0).get());
    }

    if (select_query && select_query->settings())
    {
        ContextMutablePtr context_clone = Context::createCopy(context_);
        InterpreterSetQuery(select_query->settings(), context_clone).executeForCurrentContext();
        const Settings & clone_settings = context_clone->getSettingsRef();
        distributed_stages_settings.enable_distributed_stages = clone_settings.enable_distributed_stages;
        distributed_stages_settings.fallback_to_simple_query = clone_settings.fallback_to_simple_query;
        return distributed_stages_settings;
    }

    return distributed_stages_settings;
}

DistributedStagesSettings InterpreterDistributedStages::extractDistributedStagesSettings(const ASTPtr & query, ContextPtr context_)
{
    const auto * insert_query = query->as<ASTInsertQuery>();
    if (insert_query && insert_query->select)
        return extractDistributedStagesSettingsImpl(insert_query->select, context_);
    else
        return extractDistributedStagesSettingsImpl(query, context_);
}

}
