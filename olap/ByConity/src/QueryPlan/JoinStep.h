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
#include <QueryPlan/IQueryPlanStep.h>
#include <QueryPlan/ITransformingStep.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Interpreters/asof.h>
#include <Optimizer/PredicateConst.h>

namespace DB
{

enum class DistributionType : UInt8
{
    UNKNOWN = 0,
    REPARTITION,
    BROADCAST
};

class IJoin;
using JoinPtr = std::shared_ptr<IJoin>;

/// Join two data streams.
class JoinStep : public IQueryPlanStep
{
public:
    JoinStep(
        const DataStream & left_stream_,
        const DataStream & right_stream_,
        JoinPtr join_,
        size_t max_block_size_);

    JoinStep(
        DataStreams input_streams_,
        DataStream output_stream_,
        ASTTableJoin::Kind kind,
        ASTTableJoin::Strictness strictness_,
        Names left_keys_ = {},
        Names right_keys_ = {},
        ConstASTPtr filter_ = PredicateConst::TRUE_VALUE,
        bool has_using_ = false,
        std::optional<std::vector<bool>> require_right_keys_ = std::nullopt,
        ASOF::Inequality asof_inequality_ = ASOF::Inequality::GreaterOrEquals,
        DistributionType distribution_type_ = DistributionType::UNKNOWN,
        bool magic_set_ = false);


    String getName() const override { return "Join"; }

    Type getType() const override { return Type::Join; }

    QueryPipelinePtr updatePipeline(QueryPipelines pipelines, const BuildQueryPipelineSettings &) override;

    void describePipeline(FormatSettings & settings) const override;

    const JoinPtr & getJoin() const { return join; }

    ASTTableJoin::Kind getKind() const { return kind; }
    void setKind(ASTTableJoin::Kind kind_) { kind = kind_; }

    ASTTableJoin::Strictness getStrictness() const { return strictness; }
    const Names & getLeftKeys() const { return left_keys; }
    const Names & getRightKeys() const { return right_keys; }
    const ConstASTPtr & getFilter() const { return filter; }
    bool isHasUsing() const { return has_using; }
    std::optional<std::vector<bool>> getRequireRightKeys() const
    {
        return require_right_keys;
    }
    ASOF::Inequality getAsofInequality() const { return asof_inequality; }
    DistributionType getDistributionType() const { return distribution_type; }
    void setDistributionType(DistributionType distribution_type_) { distribution_type = distribution_type_; }

    bool isCrossJoin() const { return kind == ASTTableJoin::Kind::Cross || (kind == ASTTableJoin::Kind::Inner && left_keys.empty()); }

    bool isPhysical() const override { return distribution_type != DistributionType::UNKNOWN; }
    bool isLogical() const override { return !isPhysical(); }

    bool isMagic() const { return is_magic; }
    void setMagic(bool is_magic_) { is_magic = is_magic_; }

    bool supportReorder(bool support_filter, bool support_cross = false) const;

    bool supportSwap() const
    {
        if (getStrictness() != ASTTableJoin::Strictness::Unspecified && getStrictness() != ASTTableJoin::Strictness::All
            && getStrictness() != ASTTableJoin::Strictness::Any)
            return false;

        // todo can support swap
        if (require_right_keys || has_using)
            return false;

        return !isMagic() && !getLeftKeys().empty();
    }

    /**
     * Hash Join don't support non-equivalent filter yet, so we must use nest loop join.
     */
    bool enforceNestLoopJoin() const;

    JoinPtr makeJoin(ContextPtr context);

    void serialize(WriteBuffer & buf) const override;
    void serialize(WriteBuffer & buffer, bool with_output) const;

    static QueryPlanStepPtr deserialize(ReadBuffer & buf, ContextPtr);
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr ptr) const override;
    void setInputStreams(const DataStreams & input_streams_) override;
    String serializeToString() const override;

private:
    JoinPtr join;
    size_t max_block_size;

    ASTTableJoin::Kind kind;
    ASTTableJoin::Strictness strictness;

    Names left_keys;
    Names right_keys;

    /**
     * Non-equals predicate
     *
     * For example：
     *
     * LEFT JOIN orders ON (c_custkey = o_custkey) AND (o_comment NOT LIKE '%special%requests%')
     */
    ConstASTPtr filter;

    bool has_using;

    // A right join key which has its require_right_key = FALSE has below effects:
    // 1. It will be excluded of the output columns.
    // 2. For RIGHT/FULL JOIN, the counterpart left keys will carry the data of the right key
    // NB: If the require_right_keys is nullopt, it's {TRUE, TRUE...} equivalently.
    // NB: It's only be used in Clickhouse semantics currently.
    //
    // Examples:
    // For query "SELECT k FROM (SELECT 1 AS k) x RIGHT JOIN (SELECT 2 AS k) y USING k",
    //   if require_right_keys = FALSE, it outputs: [2]
    //   if require_right_keys = TRUE, it outputs: [NULL] (currently QueryPlanner does not generate this case)
    //
    // For query "SELECT k FROM (SELECT 1 AS k) x FULL JOIN (SELECT 2 AS k) y USING k",
    //   if require_right_keys = FALSE, it outputs: [1], [2]
    //   if require_right_keys = TRUE, it outputs: [1], [NULL] (currently QueryPlanner does not generate this case)
    std::optional<std::vector<bool>> require_right_keys;

    ASOF::Inequality asof_inequality;

    DistributionType distribution_type = DistributionType::UNKNOWN;

    bool is_magic;
    Processors processors;
};

/// Special step for the case when Join is already filled.
/// For StorageJoin and Dictionary.
class FilledJoinStep : public ITransformingStep
{
public:
    FilledJoinStep(const DataStream & input_stream_, JoinPtr join_, size_t max_block_size_);

    String getName() const override { return "FilledJoin"; }

    Type getType() const override { return Type::FilledJoin; }

    void transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &) override;

    void serialize(WriteBuffer & buf) const override;
    static QueryPlanStepPtr deserialize(ReadBuffer & buf, ContextPtr);
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr ptr) const override;
    void setInputStreams(const DataStreams & input_streams_) override;

private:
    JoinPtr join;
    size_t max_block_size;
};

}
