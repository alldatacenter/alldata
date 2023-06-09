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

#include <stack>
#include <IO/Operators.h>
#include <IO/WriteBuffer.h>
#include <Interpreters/ActionsDAG.h>
#include <Interpreters/ArrayJoinAction.h>
#include <Processors/QueryPipeline.h>
#include <QueryPlan/BuildQueryPipelineSettings.h>
#include <QueryPlan/IQueryPlanStep.h>
#include <QueryPlan/Optimizations/Optimizations.h>
#include <QueryPlan/Optimizations/QueryPlanOptimizationSettings.h>
#include <QueryPlan/PlanNode.h>
#include <QueryPlan/PlanSerDerHelper.h>
#include <QueryPlan/QueryPlan.h>
#include <QueryPlan/TableScanStep.h>
#include <common/logger_useful.h>
#include <Common/JSONBuilder.h>
#include <Common/Stopwatch.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

QueryPlan::QueryPlan() = default;
QueryPlan::~QueryPlan() = default;
QueryPlan::QueryPlan(QueryPlan &&) = default;
QueryPlan & QueryPlan::operator=(QueryPlan &&) = default;

QueryPlan::QueryPlan(PlanNodePtr root_, PlanNodeIdAllocatorPtr id_allocator_)
    : plan_node(std::move(root_)), id_allocator(std::move(id_allocator_))
{
}

QueryPlan::QueryPlan(PlanNodePtr root_, CTEInfo cte_info_, PlanNodeIdAllocatorPtr id_allocator_)
    : plan_node(std::move(root_)), cte_info(std::move(cte_info_)), id_allocator(std::move(id_allocator_))
{
}

void QueryPlan::checkInitialized() const
{
    if (!isInitialized())
        throw Exception("QueryPlan was not initialized", ErrorCodes::LOGICAL_ERROR);
}

void QueryPlan::checkNotCompleted() const
{
    if (isCompleted())
        throw Exception("QueryPlan was already completed", ErrorCodes::LOGICAL_ERROR);
}

bool QueryPlan::isCompleted() const
{
    return isInitialized() && !root->step->hasOutputStream();
}

const DataStream & QueryPlan::getCurrentDataStream() const
{
    checkInitialized();
    checkNotCompleted();
    return root->step->getOutputStream();
}

void QueryPlan::unitePlans(QueryPlanStepPtr step, std::vector<std::unique_ptr<QueryPlan>> plans)
{
    if (isInitialized())
        throw Exception("Cannot unite plans because current QueryPlan is already initialized",
                        ErrorCodes::LOGICAL_ERROR);

    const auto & inputs = step->getInputStreams();
    size_t num_inputs = step->getInputStreams().size();
    if (num_inputs != plans.size())
    {
        throw Exception("Cannot unite QueryPlans using " + step->getName() +
                        " because step has different number of inputs. "
                        "Has " + std::to_string(plans.size()) + " plans "
                        "and " + std::to_string(num_inputs) + " inputs", ErrorCodes::LOGICAL_ERROR);
    }

    for (size_t i = 0; i < num_inputs; ++i)
    {
        const auto & step_header = inputs[i].header;
        const auto & plan_header = plans[i]->getCurrentDataStream().header;
        if (!blocksHaveEqualStructure(step_header, plan_header))
            throw Exception("Cannot unite QueryPlans using " + step->getName() + " because "
                            "it has incompatible header with plan " + root->step->getName() + " "
                            "plan header: " + plan_header.dumpStructure() +
                            "step header: " + step_header.dumpStructure(), ErrorCodes::LOGICAL_ERROR);
    }

    for (auto & plan : plans)
        nodes.splice(nodes.end(), std::move(plan->nodes));

    nodes.emplace_back(Node{.step = std::move(step)});
    root = &nodes.back();

    for (auto & plan : plans)
        root->children.emplace_back(plan->root);

    for (auto & plan : plans)
    {
        max_threads = std::max(max_threads, plan->max_threads);
        interpreter_context.insert(interpreter_context.end(),
                                   plan->interpreter_context.begin(), plan->interpreter_context.end());
    }
}

void QueryPlan::addStep(QueryPlanStepPtr step, PlanNodes children)
{
    checkNotCompleted();

    size_t num_input_streams = step->getInputStreams().size();

    if (num_input_streams == 0)
    {
        if (isInitialized())
            throw Exception("Cannot add step " + step->getName() + " to QueryPlan because "
                            "step has no inputs, but QueryPlan is already initialized", ErrorCodes::LOGICAL_ERROR);

        nodes.emplace_back(Node{.step = std::move(step)});
        root = &nodes.back();
        return;
    }

    if (num_input_streams >= 1)
    {
        if (!isInitialized())
            throw Exception("Cannot add step " + step->getName() + " to QueryPlan because "
                            "step has input, but QueryPlan is not initialized", ErrorCodes::LOGICAL_ERROR);

        const auto & root_header = root->step->getOutputStream().header;
        const auto & step_header = step->getInputStreams().front().header;
        if (!blocksHaveEqualStructure(root_header, step_header))
            throw Exception("Cannot add step " + step->getName() + " to QueryPlan because "
                            "it has incompatible header with root step " + root->step->getName() + " "
                            "root header: " + root_header.dumpStructure() +
                            "step header: " + step_header.dumpStructure(), ErrorCodes::LOGICAL_ERROR);

        nodes.emplace_back(Node{.step = std::move(step), .children = {root}});
        root = &nodes.back();
        return;
    }
    plan_node = plan_node->addStep(id_allocator->nextId(), std::move(step), std::move(children));

    throw Exception("Cannot add step " + step->getName() + " to QueryPlan because it has " +
                    std::to_string(num_input_streams) + " inputs but " + std::to_string(isInitialized() ? 1 : 0) +
                    " input expected", ErrorCodes::LOGICAL_ERROR);
}

void QueryPlan::addNode(Node && node_)
{
    nodes.emplace_back(std::move(node_));
}

void QueryPlan::addRoot(Node && node_)
{
    nodes.emplace_back(std::move(node_));
    root = &nodes.back();
}

/**
 * Remove nodes that have no step and children.
 * Refresh children of each node since this childen maybe removed.
 */
void QueryPlan::freshPlan()
{
    for (auto it = nodes.begin(); it != nodes.end();)
    {
        if (!it->step && it->children.empty())
            it = nodes.erase(it);
        else
            ++it;
    }

    std::unordered_set<Node *> exists_nodes;

    for (auto & node : nodes)
        exists_nodes.insert(&node);

    for (auto & node : nodes)
    {
        std::vector<Node *> freshed_children;
        for (auto & child : node.children)
        {
            if (exists_nodes.count(child))
                freshed_children.push_back(child);
        }
        node.children.swap(freshed_children);
    }
}

/**
 * Be careful, after we create a sub_plan, some nodes in the original plan have been deleted and deconstructed.
 * More precisely， nodes that moved to sub_plan are deleted.
 */
QueryPlan QueryPlan::getSubPlan(QueryPlan::Node * node_)
{
    QueryPlan sub_plan;

    std::stack<QueryPlan::Node *> plan_nodes;
    sub_plan.addRoot(Node{.step = std::move(node_->step), .children = std::move(node_->children), .id = node_->id});
    plan_nodes.push(sub_plan.getRoot());

    while (!plan_nodes.empty())
    {
        auto current = plan_nodes.top();
        plan_nodes.pop();

        std::vector<Node *> result_children;
        for (auto & child : current->children)
        {
            sub_plan.addNode(Node{.step = std::move(child->step), .children = std::move(child->children), .id = child->id});
            result_children.push_back(sub_plan.getLastNode());
            plan_nodes.push(sub_plan.getLastNode());
        }
        current->children.swap(result_children);
    }

    freshPlan();

    return sub_plan;
}

QueryPipelinePtr QueryPlan::buildQueryPipeline(
    const QueryPlanOptimizationSettings & optimization_settings,
    const BuildQueryPipelineSettings & build_pipeline_settings)
{
    checkInitialized();

    if (!optimization_settings.enable_optimizer)
    {
        optimize(optimization_settings);
    }

    struct Frame
    {
        Node * node = {};
        QueryPipelines pipelines = {};
    };

    QueryPipelinePtr last_pipeline;

    std::stack<Frame> stack;
    stack.push(Frame{.node = root});
    Stopwatch watch;
    while (!stack.empty())
    {
        auto & frame = stack.top();

        if (last_pipeline)
        {
            frame.pipelines.emplace_back(std::move(last_pipeline));
            last_pipeline = nullptr; //-V1048
        }

        size_t next_child = frame.pipelines.size();
        if (next_child == frame.node->children.size())
        {
            bool limit_max_threads = frame.pipelines.empty();
            try
            {
                last_pipeline = frame.node->step->updatePipeline(std::move(frame.pipelines), build_pipeline_settings);
            }
            catch (const Exception & e) /// Typical for an incorrect username, password, or address.
            {
                LOG_ERROR(log, "Build pipeline error {}", e.what());
                throw;
            }
// #ifndef NDEBUG
//             if (optimization_settings.enable_optimizer)
//             {
//                 const auto & output_header = frame.node->step->getOutputStream().header;
//                 const auto & pipeline_header = last_pipeline->getHeader();
//                 assertBlocksHaveEqualStructure(
//                     output_header,
//                     pipeline_header,
//                     "QueryPlan::buildQueryPipeline for " + frame.node->step->getName() + " (output header, pipeline header)");
//             }
// #endif

            if (limit_max_threads && max_threads)
                last_pipeline->limitMaxThreads(max_threads);

            stack.pop();
        }
        else
            stack.push(Frame{.node = frame.node->children[next_child]});
    }

    for (auto & context : interpreter_context)
        last_pipeline->addInterpreterContext(std::move(context));

    LOG_DEBUG(log, "Build pipeline takes:{}", watch.elapsedMilliseconds());
    return last_pipeline;
}

Pipe QueryPlan::convertToPipe(
    const QueryPlanOptimizationSettings & optimization_settings,
    const BuildQueryPipelineSettings & build_pipeline_settings)
{
    if (!isInitialized())
        return {};

    if (isCompleted())
        throw Exception("Cannot convert completed QueryPlan to Pipe", ErrorCodes::LOGICAL_ERROR);

    return QueryPipeline::getPipe(std::move(*buildQueryPipeline(optimization_settings, build_pipeline_settings)));
}

void QueryPlan::addInterpreterContext(std::shared_ptr<Context> context)
{
    interpreter_context.emplace_back(std::move(context));
}


static void explainStep(const IQueryPlanStep & step, JSONBuilder::JSONMap & map, const QueryPlan::ExplainPlanOptions & options)
{
    map.add("Node Type", step.getName());

    if (options.description)
    {
        const auto & description = step.getStepDescription();
        if (!description.empty())
            map.add("Description", description);
    }

    if (options.header && step.hasOutputStream())
    {
        auto header_array = std::make_unique<JSONBuilder::JSONArray>();

        for (const auto & output_column : step.getOutputStream().header)
        {
            auto column_map = std::make_unique<JSONBuilder::JSONMap>();
            column_map->add("Name", output_column.name);
            if (output_column.type)
                column_map->add("Type", output_column.type->getName());

            header_array->add(std::move(column_map));
        }

        map.add("Header", std::move(header_array));
    }

    if (options.actions)
        step.describeActions(map);

    if (options.indexes)
        step.describeIndexes(map);
}

JSONBuilder::ItemPtr QueryPlan::explainPlan(const ExplainPlanOptions & options)
{
    checkInitialized();

    struct Frame
    {
        Node * node = {};
        size_t next_child = 0;
        std::unique_ptr<JSONBuilder::JSONMap> node_map = {};
        std::unique_ptr<JSONBuilder::JSONArray> children_array = {};
    };

    std::stack<Frame> stack;
    stack.push(Frame{.node = root});

    std::unique_ptr<JSONBuilder::JSONMap> tree;

    while (!stack.empty())
    {
        auto & frame = stack.top();

        if (frame.next_child == 0)
        {
            if (!frame.node->children.empty())
                frame.children_array = std::make_unique<JSONBuilder::JSONArray>();

            frame.node_map = std::make_unique<JSONBuilder::JSONMap>();
            explainStep(*frame.node->step, *frame.node_map, options);
        }

        if (frame.next_child < frame.node->children.size())
        {
            stack.push(Frame{frame.node->children[frame.next_child]});
            ++frame.next_child;
        }
        else
        {
            if (frame.children_array)
                frame.node_map->add("Plans", std::move(frame.children_array));

            tree.swap(frame.node_map);
            stack.pop();

            if (!stack.empty())
                stack.top().children_array->add(std::move(tree));
        }
    }

    return tree;
}

static void explainStep(
    const IQueryPlanStep & step,
    IQueryPlanStep::FormatSettings & settings,
    const QueryPlan::ExplainPlanOptions & options)
{
    std::string prefix(settings.offset, ' ');
    settings.out << prefix;
    settings.out << step.getName();

    const auto & description = step.getStepDescription();
    if (options.description && !description.empty())
        settings.out << " (" << description << ')';

    settings.out.write('\n');

    if (options.header)
    {
        settings.out << prefix;

        if (!step.hasOutputStream())
            settings.out << "No header";
        else if (!step.getOutputStream().header)
            settings.out << "Empty header";
        else
        {
            settings.out << "Header: ";
            bool first = true;

            for (const auto & elem : step.getOutputStream().header)
            {
                if (!first)
                    settings.out << "\n" << prefix << "        ";

                first = false;
                elem.dumpNameAndType(settings.out);
            }
        }

        settings.out.write('\n');
    }

    if (options.actions)
        step.describeActions(settings);

    if (options.indexes)
        step.describeIndexes(settings);
}

std::string debugExplainStep(const IQueryPlanStep & step)
{
    WriteBufferFromOwnString out;
    IQueryPlanStep::FormatSettings settings{.out = out};
    QueryPlan::ExplainPlanOptions options{.actions = true};
    explainStep(step, settings, options);
    return out.str();
}

void QueryPlan::explainPlan(WriteBuffer & buffer, const ExplainPlanOptions & options) const
{
    checkInitialized();

    IQueryPlanStep::FormatSettings settings{.out = buffer, .write_header = options.header};

    struct Frame
    {
        Node * node = {};
        bool is_description_printed = false;
        size_t next_child = 0;
    };

    std::stack<Frame> stack;
    stack.push(Frame{.node = root});

    while (!stack.empty())
    {
        auto & frame = stack.top();

        if (!frame.is_description_printed)
        {
            settings.offset = (stack.size() - 1) * settings.indent;
            explainStep(*frame.node->step, settings, options);
            frame.is_description_printed = true;
        }

        if (frame.next_child < frame.node->children.size())
        {
            stack.push(Frame{frame.node->children[frame.next_child]});
            ++frame.next_child;
        }
        else
            stack.pop();
    }
}

static void explainPipelineStep(IQueryPlanStep & step, IQueryPlanStep::FormatSettings & settings)
{
    settings.out << String(settings.offset, settings.indent_char) << "(" << step.getName() << ")\n";
    size_t current_offset = settings.offset;
    step.describePipeline(settings);
    if (current_offset == settings.offset)
        settings.offset += settings.indent;
}

void QueryPlan::explainPipeline(WriteBuffer & buffer, const ExplainPipelineOptions & options) const
{
    checkInitialized();

    IQueryPlanStep::FormatSettings settings{.out = buffer, .write_header = options.header};

    struct Frame
    {
        Node * node = {};
        size_t offset = 0;
        bool is_description_printed = false;
        size_t next_child = 0;
    };

    std::stack<Frame> stack;
    stack.push(Frame{.node = root});

    while (!stack.empty())
    {
        auto & frame = stack.top();

        if (!frame.is_description_printed)
        {
            settings.offset = frame.offset;
            explainPipelineStep(*frame.node->step, settings);
            frame.offset = settings.offset;
            frame.is_description_printed = true;
        }

        if (frame.next_child < frame.node->children.size())
        {
            stack.push(Frame{frame.node->children[frame.next_child], frame.offset});
            ++frame.next_child;
        }
        else
            stack.pop();
    }
}

void QueryPlan::optimize(const QueryPlanOptimizationSettings & optimization_settings)
{
    QueryPlanOptimizations::optimizeTree(optimization_settings, *root, nodes);
}

void QueryPlan::serialize(WriteBuffer & buffer) const
{
    writeBinary(nodes.size(), buffer);
    /**
     * we first encode the query plan node for serialize / deserialize
     */
    size_t id = 0;
    for (auto it = nodes.begin(); it != nodes.end(); ++it)
        it->id = id++;

    for (auto it = nodes.begin(); it != nodes.end(); ++it)
    {
        serializePlanStep(it->step, buffer);
        /**
         * serialize its children ids
         */
        writeBinary(it->children.size(), buffer);
        for (auto jt = it->children.begin(); jt != it->children.end(); ++jt)
            writeBinary((*jt)->id, buffer);

        writeBinary(it->id, buffer);
    }

    if (root)
    {
        writeBinary(true, buffer);
        writeBinary(root->id, buffer);
    }
    else
        writeBinary(false, buffer);
}

void QueryPlan::deserialize(ReadBuffer & buffer)
{
    size_t nodes_size;
    readBinary(nodes_size, buffer);

    std::unordered_map<size_t, Node *> map_to_node;
    std::unordered_map<size_t, std::vector<size_t>> map_to_children;

    for (size_t i = 0; i < nodes_size; ++i)
    {
        QueryPlanStepPtr step = deserializePlanStep(buffer, interpreter_context.empty() ? nullptr : interpreter_context.back());
        /**
         * deserialize children ids
         */
        size_t children_size;
        readBinary(children_size, buffer);
        if (children_size > 100)
        {
            std::cout << "bug";
        }
        std::vector<size_t> children(children_size);
        for (size_t j = 0; j < children_size; ++j)
            readBinary(children[j], buffer);

        /**
         * construct node, node-id mapping and id-children mapping
         */
        size_t id;
        readBinary(id, buffer);
        nodes.emplace_back(Node{.step = std::move(step), .id = id});
        map_to_node[id] = &nodes.back();
        map_to_children[id] = std::move(children);
    }

    bool has_root;
    readBinary(has_root, buffer);
    size_t root_id;
    if (has_root)
        readBinary(root_id, buffer);

    /**
     * After we have node-id mapping and id-children mapping,
     * fill the children infomation to each node.
     */
    for (auto it = nodes.begin(); it != nodes.end(); ++it)
    {
        size_t id = it->id;
        auto & children = map_to_children[id];
        for (auto & child_id : children)
            it->children.push_back(map_to_node[child_id]);
    }

    if (has_root)
        root = map_to_node[root_id];
}

void QueryPlan::allocateLocalTable(ContextPtr context)
{
    for (const auto & node : nodes)
    {
        if (node.step->getType() == IQueryPlanStep::Type::TableScan)
        {
            auto * table_scan = dynamic_cast<TableScanStep *>(node.step.get());
            table_scan->allocate(context);
        }
    }
}

}
