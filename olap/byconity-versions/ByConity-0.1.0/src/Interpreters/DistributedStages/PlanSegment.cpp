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

#include <Interpreters/DistributedStages/PlanSegment.h>
#include <IO/WriteBufferFromString.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>
#include <Interpreters/Context.h>
#include <DataTypes/DataTypesNumber.h>
#include <Functions/FunctionFactory.h>
#include <Core/ColumnWithTypeAndName.h>
#include <Core/ColumnNumbers.h>
#include <Parsers/IAST.h>
#include <DataStreams/NativeBlockOutputStream.h>
#include <DataStreams/NativeBlockInputStream.h>
#include <Parsers/queryToString.h>
#include <QueryPlan/PlanSerDerHelper.h>
#include <QueryPlan/RemoteExchangeSourceStep.h>

#include <sstream>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

String planSegmentTypeToString(const PlanSegmentType & type)
{
    std::ostringstream ostr;

    switch(type)
    {
        case PlanSegmentType::UNKNOWN:
            ostr << "UNKNOWN";
            break;
        case PlanSegmentType::SOURCE:
            ostr << "SOURCE";
            break;
        case PlanSegmentType::EXCHANGE:
            ostr << "EXCHANGE";
            break;
        case PlanSegmentType::OUTPUT:
            ostr << "OUTPUT";
            break;
    }

    return ostr.str();
}

void IPlanSegment::serialize(WriteBuffer & buf) const
{
    serializeBlock(header, buf);
    writeBinary(UInt8(type), buf);
    writeBinary(UInt8(exchange_mode), buf);
    writeBinary(exchange_parallel_size, buf);
    writeBinary(name, buf);
    writeBinary(segment_id, buf);

    writeBinary(shuffle_keys.size(), buf);
    for (auto & key : shuffle_keys)
        writeBinary(key, buf);
}

void IPlanSegment::deserialize(ReadBuffer & buf, ContextPtr)
{
    header = deserializeBlock(buf);

    UInt8 read_type;
    readBinary(read_type, buf);
    type = PlanSegmentType(read_type);

    UInt8 read_mode;
    readBinary(read_mode, buf);
    exchange_mode = ExchangeMode(read_mode);

    readBinary(exchange_parallel_size, buf);
    readBinary(name, buf);
    readBinary(segment_id, buf);

    size_t key_size;
    readBinary(key_size, buf);
    shuffle_keys.resize(key_size);
    for (size_t i = 0; i < key_size; ++i)
        readBinary(shuffle_keys[i], buf);
}

String IPlanSegment::toString(size_t indent) const
{
    std::ostringstream ostr;
    String indent_str(indent, ' ');

    ostr << indent_str << "segment_id: " << segment_id << "\n";
    ostr << indent_str << "name: " << name << "\n";
    ostr << indent_str << "header: " << header.dumpStructure() << "\n";
    ostr << indent_str << "type: " << planSegmentTypeToString(type) << "\n";
    ostr << indent_str << "exchange_mode: " << exchangeModeToString(exchange_mode) << "\n";
    ostr << indent_str << "exchange_parallel_size: " << exchange_parallel_size << "\n";
    ostr << indent_str << "shuffle_keys: " << "\n";
    ostr << indent_str;
    for (auto & key : shuffle_keys)
        ostr << key << ", ";

    return ostr.str();
}

void PlanSegmentInput::serialize(WriteBuffer & buf) const
{
    IPlanSegment::serialize(buf);

    writeBinary(parallel_index, buf);

    writeBinary(keep_order, buf);

    writeBinary(source_addresses.size(), buf);
    for (auto & source_address : source_addresses)
        source_address.serialize(buf);

    if (type == PlanSegmentType::SOURCE)
    {
        if (storage_id)
        {
            writeBinary(true, buf);
            storage_id->serialize(buf);
        }
        else
        {
            writeBinary(false, buf);
        }
    }
}

void PlanSegmentInput::deserialize(ReadBuffer & buf, ContextPtr context)
{
    IPlanSegment::deserialize(buf, context);

    readBinary(parallel_index, buf);

    readBinary(keep_order, buf);

    size_t addresses_size;
    readBinary(addresses_size, buf);
    for (size_t i = 0; i < addresses_size; ++i)
    {
        AddressInfo address;
        address.deserialize(buf);
        source_addresses.push_back(address);
    }

    if (type == PlanSegmentType::SOURCE)
    {
        bool has;
        readBinary(has, buf);
        if (has)
            storage_id = StorageID::deserialize(buf, context);
    }
}

String PlanSegmentInput::toString(size_t indent) const
{
    std::ostringstream ostr;
    String indent_str(indent, ' ');

    ostr << IPlanSegment::toString(indent) << "\n";
    ostr << indent_str << "parallel_index: " << parallel_index << "\n";
    ostr << indent_str << "keep_order: " << keep_order << "\n";
    ostr << indent_str << "storage_id: " << (type == PlanSegmentType::SOURCE && storage_id.has_value() ? storage_id->getNameForLogs() : "") << "\n";
    ostr << indent_str << "source_addresses: " << "\n";
    for (auto & address : source_addresses)
        ostr << indent_str << indent_str << address.toString() << "\n";

    return ostr.str();
}

void PlanSegmentOutput::serialize(WriteBuffer & buf) const
{
    IPlanSegment::serialize(buf);
    writeBinary(shuffle_function_name, buf);
    writeBinary(parallel_size, buf);
    writeBinary(keep_order, buf);
}

void PlanSegmentOutput::deserialize(ReadBuffer & buf, ContextPtr context)
{
    IPlanSegment::deserialize(buf, context);
    readBinary(shuffle_function_name, buf);
    readBinary(parallel_size, buf);
    readBinary(keep_order, buf);
}

String PlanSegmentOutput::toString(size_t indent) const
{
    std::ostringstream ostr;
    String indent_str(indent, ' ');

    ostr << IPlanSegment::toString(indent) << "\n";
    ostr << indent_str << "shuffle_function_name: " << shuffle_function_name << "\n";
    ostr << indent_str << "parallel_size: " << parallel_size << "\n";
    ostr << indent_str << "keep_order: " << keep_order;

    return ostr.str();
}

PlanSegment::PlanSegment(const ContextPtr & context_) : context(Context::createCopy(context_)) {

}

void PlanSegment::setContext(const ContextPtr & context_) { context = Context::createCopy(context_); }

void PlanSegment::setPlanSegmentToQueryPlan(QueryPlan::Node * node)
{
    if (!node)
        return;

    if (auto * remote_step = dynamic_cast<RemoteExchangeSourceStep *>(node->step.get()))
        remote_step->setPlanSegment(this);
    else
    {
        for (auto & child : node->children)
        {
            setPlanSegmentToQueryPlan(child);
        }
    }
}

void PlanSegment::serialize(WriteBuffer & buf) const
{
    writeBinary(segment_id, buf);
    writeBinary(query_id, buf);

    query_plan.serialize(buf);

    writeBinary(inputs.size(), buf);
    for (auto & input : inputs)
        input->serialize(buf);

    if (output)
        output->serialize(buf);
    else
        throw Exception("Cannot find output when serialize PlanSegment", ErrorCodes::LOGICAL_ERROR);

    coordinator_address.serialize(buf);
    current_address.serialize(buf);

    writeBinary(cluster_name, buf);
    writeBinary(parallel, buf);
    writeBinary(exchange_parallel_size, buf);
}

void PlanSegment::deserialize(ReadBuffer & buf)
{
    readBinary(segment_id, buf);
    readBinary(query_id, buf);

    query_plan.addInterpreterContext(context);
    query_plan.deserialize(buf);

    size_t input_size;
    readBinary(input_size, buf);
    for (size_t i = 0; i < input_size; ++i)
    {
        auto input = std::make_shared<PlanSegmentInput>();
        input->deserialize(buf, context);
        inputs.push_back(input);
    }

    output = std::make_shared<PlanSegmentOutput>();
    output->deserialize(buf, context);
    coordinator_address.deserialize(buf);
    current_address.deserialize(buf);

    readBinary(cluster_name, buf);
    readBinary(parallel, buf);
    readBinary(exchange_parallel_size, buf);
}

/**
 * update plansegemnt if
 * 1. a segment is deserialized
 * 2. before final segment executed on coordinator
 */
void PlanSegment::update()
{
    setPlanSegmentToQueryPlan(query_plan.getRoot());
}

PlanSegmentPtr PlanSegment::deserializePlanSegment(ReadBuffer & buf, ContextPtr context_)
{
    auto plan_segment = std::make_unique<PlanSegment>(context_);
    plan_segment->deserialize(buf);
    plan_segment->update();
    return plan_segment;
}

String PlanSegment::toString() const
{
    std::ostringstream ostr;

    ostr << "segment_id: " << segment_id << "\n";
    ostr << "query_id: " << query_id << "\n";

    WriteBufferFromOwnString plan_str;
    query_plan.explainPlan(plan_str, {});
    ostr << plan_str.str() << "\n";

    ostr << "inputs: " << "\n";
    for (auto & input : inputs)
        ostr << input->toString(4) << "\n";
    ostr << "output: " << "\n";
    if (output)
        ostr << output->toString(4) << "\n";

    ostr << "coordinator_address: " << coordinator_address.toString() << "\n";
    ostr << "current_address: " << current_address.toString() << "\n";
    ostr << "cluster_name: " << cluster_name << "\n";
    ostr << "parallel: " << parallel << ", exchange_parallel_size: " << exchange_parallel_size;

    return ostr.str();
}

std::unordered_map<size_t, PlanSegmentPtr &> PlanSegmentTree::getPlanSegmentsMap()
{
    std::unordered_map<size_t, PlanSegmentPtr &> all_segments;
    Nodes & all_nodes = getNodes();
    for(auto & node : all_nodes)
    {
        all_segments.emplace(node.plan_segment->getPlanSegmentId(), node.plan_segment);
    }
    return all_segments;
}

String PlanSegmentTree::toString() const
{
    std::ostringstream ostr;

    std::queue<Node *> print_queue;
    print_queue.push(root);

    while (!print_queue.empty())
    {
        auto current = print_queue.front();
        print_queue.pop();

        for (auto & child : current->children)
            print_queue.push(child);

        ostr << current->plan_segment->toString() << "\n";
        ostr << " ------------------ " << "\n";
    }

    return ostr.str();
}

}
