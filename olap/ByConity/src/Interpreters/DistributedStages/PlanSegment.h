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

#include <optional>
#include <Core/Types.h>
#include <Core/Block.h>
#include <QueryPlan/QueryPlan.h>
#include <Interpreters/Context_fwd.h>
#include <Interpreters/StorageID.h>
#include <Interpreters/DistributedStages/AddressInfo.h>
#include <Interpreters/DistributedStages/ExchangeMode.h>

namespace DB
{
using DynamicFilterId = UInt32;

/**
 * SOURCE means the plan is the leaf of a plan segment tree, i.g. TableScan Node.
 * EXCHANGE always marking the plan that need to repartiton the data.
 * OUTPUT is only used in PlanSegmentOutput and its output is client, which means we should output the results.
 */
enum class PlanSegmentType : UInt8
{
    UNKNOWN = 0,
    SOURCE,
    EXCHANGE,
    OUTPUT
};

String planSegmentTypeToString(const PlanSegmentType & type);

/***
 * Base class for input && output of PlanSegment.
 * We have several types of inputs and outpus, they are distinguished by PlanSegmentType,
 * hence if we get inputs and want to determine the next operation on it, we should check the PlanSegmentType first.
 * For example, exchange_parallel_size and exchange_mode always used in Type of PlanSegmentType::EXCHANGE.
 */

class IPlanSegment
{
public:
    IPlanSegment() = default;

    IPlanSegment(const PlanSegmentType & type_)
    : type(type_) {}

    IPlanSegment(const Block & header_, const PlanSegmentType & type_)
    : header(header_), type(type_) {}

    virtual ~IPlanSegment() = default;

    Block getHeader() const { return header; }

    void setHeader(const Block & header_) { header = header_; }

    PlanSegmentType getPlanSegmentType() const { return type; }

    void setPlanSegmentType(const PlanSegmentType & type_) { type = type_; }

    ExchangeMode getExchangeMode() const { return exchange_mode; }

    void setExchangeMode(const ExchangeMode & mode_) { exchange_mode = mode_; }

    size_t getExchangeParallelSize() const { return exchange_parallel_size; }

    void setExchangeParallelSize(size_t exchange_parallel_size_) { exchange_parallel_size = exchange_parallel_size_;}

    String getPlanSegmentName() const { return name; }

    void setPlanSegmentName(const String & name_) { name = name_; }

    size_t getPlanSegmentId() const { return segment_id; }

    void setPlanSegmentId(size_t segment_id_) { segment_id = segment_id_; }

    Names getShufflekeys() const { return shuffle_keys; }

    void setShufflekeys(const Names & keys) { shuffle_keys = keys; }

    virtual void serialize(WriteBuffer & buf) const;

    virtual void deserialize(ReadBuffer & buf, ContextPtr);

    virtual String toString(size_t indent = 0) const;

protected:
    Block header;
    PlanSegmentType type = PlanSegmentType::UNKNOWN;
    ExchangeMode exchange_mode = ExchangeMode::UNKNOWN;
    size_t exchange_parallel_size = 0;
    String name;
    size_t segment_id = std::numeric_limits<size_t>::max();
    Names shuffle_keys;
};

class PlanSegmentInput : public IPlanSegment
{
public:
    PlanSegmentInput(const Block & header_, const PlanSegmentType & type_)
    : IPlanSegment(header_, type_) {}

    PlanSegmentInput(const PlanSegmentType & type_)
    : IPlanSegment(type_) {}

    PlanSegmentInput() = default;

    AddressInfos & getSourceAddresses() { return source_addresses; }

    void clearSourceAddresses() { source_addresses.clear(); }

    void insertSourceAddress(const AddressInfo & address_info) { source_addresses.push_back(address_info); }

    void insertSourceAddress(const AddressInfos & address_infos) { source_addresses.insert(source_addresses.end(), address_infos.begin(), address_infos.end()); }

    const AddressInfos & getSourceAddress() const { return source_addresses; }

    size_t getParallelIndex() const { return parallel_index; }

    void setParallelIndex(size_t parallel_index_) { parallel_index = parallel_index_;}

    bool needKeepOrder() const { return keep_order; }

    void setKeepOrder(bool keep_order_) { keep_order = keep_order_; }

    void serialize(WriteBuffer & buf) const override;

    void deserialize(ReadBuffer & buf, ContextPtr context) override;

    String toString(size_t indent = 0) const override;

    std::optional<StorageID> getStorageID() const { return storage_id; }

    void setStorageID(const StorageID & storage_id_) { storage_id = storage_id_;}

private:
    size_t parallel_index = 0;
    bool keep_order = false;
    AddressInfos source_addresses;
    std::optional<StorageID> storage_id;
};

using PlanSegmentInputPtr = std::shared_ptr<PlanSegmentInput>;
using PlanSegmentInputs = std::vector<PlanSegmentInputPtr>;

class PlanSegmentOutput : public IPlanSegment
{
public:
    PlanSegmentOutput(const Block & header_, const PlanSegmentType & type_)
    : IPlanSegment(header_, type_) {}

    PlanSegmentOutput(const PlanSegmentType & type_)
    : IPlanSegment(type_) {}

    PlanSegmentOutput() = default;

    size_t getParallelSize() const { return parallel_size; }

    void setParallelSize(size_t parallel_size_) { parallel_size = parallel_size_; }

    bool needKeepOrder() const { return keep_order; }

    void setKeepOrder(bool keep_order_) { keep_order = keep_order_; }

    void serialize(WriteBuffer & buf) const override;

    void deserialize(ReadBuffer & buf, ContextPtr) override;

    String toString(size_t indent = 0) const override;

private:
    String shuffle_function_name = "cityHash64";
    size_t parallel_size;
    bool keep_order = false;
};

using PlanSegmentOutputPtr = std::shared_ptr<PlanSegmentOutput>;

/**
 * PlanSegment is a object that cannot be copy since queryPlan is only move-able.
 * This is reasonable since we should guarantee the uniqueness of the PlanSegment.
 * Hence, be careful of the modification of PlanSegment. If we want to create a new PlanSegment, use clone Function instead.
 */
class PlanSegment;
using PlanSegmentPtr = std::unique_ptr<PlanSegment>;

class PlanSegment
{
public:

    PlanSegment() = default;
    PlanSegment(const ContextPtr & context_);
    PlanSegment(PlanSegment && ) = default;
    PlanSegment & operator=(PlanSegment &&) = default;

    PlanSegment(size_t segment_id_,
                const String & query_id_,
                const String & cluster_name_)
                : segment_id(segment_id_)
                , query_id(query_id_)
                , cluster_name(cluster_name_) {}

    ~PlanSegment() = default;

    QueryPlan & getQueryPlan() { return query_plan; }

    const QueryPlan & getQueryPlan() const { return query_plan; }

    void setQueryPlan(QueryPlan && query_plan_) { query_plan = std::move(query_plan_); }

    void serialize(WriteBuffer & buf) const;

    void deserialize(ReadBuffer & buf);

    static PlanSegmentPtr deserializePlanSegment(ReadBuffer & buf, ContextPtr context);

    size_t getPlanSegmentId() const { return segment_id; }

    void setPlanSegmentId(size_t segment_id_) { segment_id = segment_id_; }

    String getQueryId() const { return query_id; }

    void setQueryId(const String & query_id_) { query_id = query_id_; }

    PlanSegmentInputs getPlanSegmentInputs() const { return inputs; }

    void appendPlanSegmentInput(const PlanSegmentInputPtr & input) { inputs.push_back(input); }

    void appendPlanSegmentInputs(const PlanSegmentInputs & inputs_) { inputs.insert(inputs.end(), inputs_.begin(), inputs_.end()); }

    void setPlanSegmentOutput(const PlanSegmentOutputPtr & output_) { output = output_; }

    PlanSegmentOutputPtr getPlanSegmentOutput() const { return output; }

    AddressInfo getCoordinatorAddress() const { return coordinator_address; }

    AddressInfo getCurrentAddress() const { return current_address; }

    void setCoordinatorAddress(const AddressInfo & coordinator_address_) { coordinator_address = coordinator_address_; }

    void setCurrentAddress(const AddressInfo & current_address_) { current_address = current_address_; }

    void setPlanSegmentToQueryPlan(QueryPlan::Node * node);

    PlanSegmentPtr clone();

    void setContext(const ContextPtr & context_);

    ContextMutablePtr getContext() const { return context;}

    String toString() const;

    String getClusterName() const { return cluster_name; }

    size_t getExchangeParallelSize() const { return exchange_parallel_size; }

    void setExchangeParallelSize(size_t exchange_parallel_size_) { exchange_parallel_size = exchange_parallel_size_;}

    size_t getParallelSize() const { return parallel; }

    void setParallelSize(size_t parallel_size_) { parallel = parallel_size_; }

    void update();

    void addRuntimeFilter(DynamicFilterId id) { runtime_filters.emplace_back(id); }

    std::vector<DynamicFilterId> & getRuntimeFilters() { return runtime_filters; }

private:
    size_t segment_id;
    String query_id;
    QueryPlan query_plan;

    PlanSegmentInputs inputs;
    PlanSegmentOutputPtr output;

    AddressInfo coordinator_address;
    AddressInfo current_address;
    String cluster_name;
    size_t parallel;
    size_t exchange_parallel_size;

    std::vector<DynamicFilterId> runtime_filters;

    ContextMutablePtr context;
};

class PlanSegmentTree
{
public:

    PlanSegmentTree() = default;
    PlanSegmentTree(PlanSegmentTree && ) = default;
    PlanSegmentTree & operator=(PlanSegmentTree &&) = default;

    struct Node
    {
        PlanSegmentPtr plan_segment;
        std::vector<Node *> children = {};

        PlanSegment * getPlanSegment() {
            if (plan_segment)
                return plan_segment.get();
            return nullptr;
        }
    };

    using Nodes = std::list<Node>;

    void addNode(Node && node_) {
        nodes.push_back(std::move(node_));
        if (nodes.size() == 1)
            root = &nodes.back();
    }

    void setRoot(Node && node_) {
        nodes.push_front(std::move(node_));
        root = &nodes.front();
    }

    void setRoot(Node * root_) { root = root_; }

    Node * getRoot() { return root; }

    Node * getLastNode() { return &nodes.back(); }

    Nodes & getNodes() { return nodes; }

    std::unordered_map<size_t, PlanSegmentPtr &> getPlanSegmentsMap();

    String toString() const;

private:
    Nodes nodes;
    Node * root = nullptr;
};

using PlanSegmentTreePtr = std::unique_ptr<PlanSegmentTree>;

}
