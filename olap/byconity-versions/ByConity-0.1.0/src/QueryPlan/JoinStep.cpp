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

#include <memory>
#include <Interpreters/HashJoin.h>
#include <Interpreters/IJoin.h>
#include <Interpreters/JoinSwitcher.h>
#include <Interpreters/MergeJoin.h>
#include <Interpreters/NestedLoopJoin.h>
#include <Optimizer/PredicateUtils.h>
#include <Parsers/ASTSerDerHelper.h>
#include <Processors/QueryPipeline.h>
#include <Processors/Transforms/ExpressionTransform.h>
#include <Processors/Transforms/FilterTransform.h>
#include <Processors/Transforms/JoiningTransform.h>
#include <QueryPlan/JoinStep.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

JoinPtr JoinStep::makeJoin(ContextPtr context)
{
    const auto & settings = context->getSettingsRef();
    auto table_join = std::make_shared<TableJoin>(settings, context->getTemporaryVolume());

    // todo support storage join
    //    if (table_to_join.database_and_table_name)
    //    {
    //        auto joined_table_id = context->resolveStorageID(table_to_join.database_and_table_name);
    //        StoragePtr table = DatabaseCatalog::instance().tryGetTable(joined_table_id, context);
    //        if (table)
    //        {
    //            if (dynamic_cast<StorageJoin *>(table.get()) ||
    //                dynamic_cast<StorageDictionary *>(table.get()))
    //                table_join->joined_storage = table;
    //        }
    //    }
    table_join->deduplicateAndQualifyColumnNames(input_streams[0].header.getNameSet(), "");

    auto using_ast = std::make_shared<ASTExpressionList>();
    for (size_t index = 0; index < left_keys.size(); ++index)
    {
        ASTPtr left = std::make_shared<ASTIdentifier>(left_keys[index]);
        ASTPtr right = std::make_shared<ASTIdentifier>(right_keys[index]);
        if (has_using)
        {
            table_join->renames[left_keys[index]] = right_keys[index];
            table_join->addUsingKey(left);
            using_ast->children.emplace_back(left);
        }
        else
        {
            table_join->addOnKeys(left, right);
        }
    }

    if (has_using)
    {
        table_join->table_join.using_expression_list = using_ast;
    }

    for (const auto & item : output_stream->header)
    {
        if (!input_streams[0].header.has(item.name))
        {
            NameAndTypePair joined_column{item.name, item.type};
            table_join->addJoinedColumn(joined_column);
        }
    }

    table_join->setAsofInequality(asof_inequality);
    if (context->getSettings().enforce_all_join_to_any_join)
    {
        strictness = ASTTableJoin::Strictness::RightAny;
    }

    table_join->table_join.strictness = strictness;
    table_join->table_join.kind = isCrossJoin() ? ASTTableJoin::Kind::Cross : kind;

    if (enforceNestLoopJoin())
    {
        if (!settings.enable_nested_loop_join)
            throw Exception("set enable_nested_loop_join=1 to enable outer join with filter", ErrorCodes::NOT_IMPLEMENTED);
        table_join->setJoinAlgorithm(JoinAlgorithm::NESTED_LOOP_JOIN);
        table_join->table_join.on_expression = filter->clone();
        table_join->table_join.kind = isCrossJoin() ? ASTTableJoin::Kind::Inner : kind;
    }

    bool allow_merge_join = table_join->allowMergeJoin();

    /// HashJoin with Dictionary optimisation
    auto sample_block = input_streams[1].header;
    String dict_name;
    String key_name;
    if (table_join->forceNestedLoopJoin())
        return std::make_shared<NestedLoopJoin>(table_join, sample_block, context);
    else if (table_join->forceHashJoin() || (table_join->preferMergeJoin() && !allow_merge_join))
        return std::make_shared<HashJoin>(table_join, sample_block);
    else if (table_join->forceMergeJoin() || (table_join->preferMergeJoin() && allow_merge_join))
        return std::make_shared<MergeJoin>(table_join, sample_block);
    return std::make_shared<JoinSwitcher>(table_join, sample_block);
}

JoinStep::JoinStep(const DataStream & left_stream_, const DataStream & right_stream_, JoinPtr join_, size_t max_block_size_)
    : join(std::move(join_)), max_block_size(max_block_size_)
{
    input_streams = {left_stream_, right_stream_};
    output_stream = DataStream{
        .header = JoiningTransform::transformHeader(left_stream_.header, join),
    };
}

JoinStep::JoinStep(
    DataStreams input_streams_,
    DataStream output_stream_,
    ASTTableJoin::Kind kind_,
    ASTTableJoin::Strictness strictness_,
    Names left_keys_,
    Names right_keys_,
    ConstASTPtr filter_,
    bool has_using_,
    std::optional<std::vector<bool>> require_right_keys_,
    ASOF::Inequality asof_inequality_,
    DistributionType distribution_type_,
    bool is_magic_)
    : kind(kind_)
    , strictness(strictness_)
    , left_keys(std::move(left_keys_))
    , right_keys(std::move(right_keys_))
    , filter(std::move(filter_))
    , has_using(has_using_)
    , require_right_keys(std::move(require_right_keys_))
    , asof_inequality(asof_inequality_)
    , distribution_type(distribution_type_)
    , is_magic(is_magic_)
{
    input_streams = std::move(input_streams_);
    output_stream = std::move(output_stream_);
}

void JoinStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
}

QueryPipelinePtr JoinStep::updatePipeline(QueryPipelines pipelines, const BuildQueryPipelineSettings & settings)
{
    if (pipelines.size() != 2)
        throw Exception(ErrorCodes::LOGICAL_ERROR, "JoinStep expect two input steps");

    if (!join)
    {
        join = makeJoin(settings.context);
        max_block_size = settings.context->getSettingsRef().max_block_size;
    }

    auto pipeline = QueryPipeline::joinPipelines(std::move(pipelines[0]), std::move(pipelines[1]), join, max_block_size, &processors);

    // if NestLoopJoin is choose, no need to add filter stream.
    if (filter && !PredicateUtils::isTruePredicate(filter) && join->getType() != JoinType::NestedLoop)
    {
        Names output;
        auto header = pipeline->getHeader();
        for (const auto & item : header)
            output.emplace_back(item.name);
        output.emplace_back(filter->getColumnName());

        auto actions_dag = createExpressionActions(settings.context, header.getNamesAndTypesList(), output, filter->clone());
        auto expression = std::make_shared<ExpressionActions>(actions_dag, settings.getActionsSettings());

        pipeline->addSimpleTransform([&](const Block & input_header, QueryPipeline::StreamType stream_type) {
            bool on_totals = stream_type == QueryPipeline::StreamType::Totals;
            return std::make_shared<FilterTransform>(input_header, expression, filter->getColumnName(), true, on_totals);
        });
    }

    projection(*pipeline, output_stream->header, settings);
    return pipeline;
}

bool JoinStep::enforceNestLoopJoin() const
{
    if (filter && !PredicateUtils::isTruePredicate(filter))
    {
        bool strictness_join = strictness != ASTTableJoin::Strictness::Unspecified && strictness != ASTTableJoin::Strictness::All;
        bool outer_join = kind != ASTTableJoin::Kind::Inner && kind != ASTTableJoin::Kind::Cross;
        return strictness_join || outer_join;
    }
    return false;
}

bool JoinStep::supportReorder(bool support_filter, bool support_cross) const
{
    if (!support_filter && !PredicateUtils::isTruePredicate(filter))
        return false;

    if (require_right_keys || has_using)
        return false;

    if (strictness != ASTTableJoin::Strictness::Unspecified && strictness != ASTTableJoin::Strictness::All)
        return false;

    bool cross_join = isCrossJoin();
    if (!support_cross && cross_join)
        return false;

    if (support_cross && cross_join)
        return !is_magic;

    return kind == ASTTableJoin::Kind::Inner && !left_keys.empty() && !is_magic;
}

void JoinStep::describePipeline(FormatSettings & settings) const
{
    IQueryPlanStep::describePipeline(processors, settings);
}

void JoinStep::serialize(WriteBuffer & buf) const
{
    serialize(buf, true);
}

void JoinStep::serialize(WriteBuffer & buf, bool with_output) const
{
    writeBinary(step_description, buf);

    if (input_streams.size() < 2)
        throw Exception(ErrorCodes::LOGICAL_ERROR, "JoinStep expect two input streams");

    serializeDataStream(input_streams[0], buf);
    serializeDataStream(input_streams[1], buf);

    if (join)
    {
        writeBinary(true, buf);
        serializeEnum(join->getType(), buf);
        join->serialize(buf);
        writeBinary(max_block_size, buf);
    }
    else
    {
        writeBinary(false, buf);
        if (with_output)
            serializeDataStream(output_stream.value(), buf);
        SERIALIZE_ENUM(kind, buf)
        SERIALIZE_ENUM(strictness, buf)

        writeVectorBinary(left_keys, buf);
        writeVectorBinary(right_keys, buf);

        serializeAST(filter->clone(), buf);
        writeBinary(has_using, buf);

        writeBinary(require_right_keys.has_value(), buf);
        if (require_right_keys)
        {
            std::vector<UInt8> uint8_vec;
            std::transform(require_right_keys->begin(), require_right_keys->end(), std::back_inserter(uint8_vec), [](bool x) {
                return static_cast<UInt8>(x);
            });
            writeVectorBinary(uint8_vec, buf);
        }
        SERIALIZE_ENUM(asof_inequality, buf)
        SERIALIZE_ENUM(distribution_type, buf)

        writeBinary(is_magic, buf);
    }
}

String JoinStep::serializeToString() const
{
    WriteBufferFromOwnString buffer;
    serialize(buffer, false);
    return buffer.str();
}

QueryPlanStepPtr JoinStep::deserialize(ReadBuffer & buf, ContextPtr context)
{
    String step_description;
    readBinary(step_description, buf);

    DataStream left_stream = deserializeDataStream(buf);
    DataStream right_stream = deserializeDataStream(buf);

    JoinPtr join = nullptr;
    bool has_join;
    readBinary(has_join, buf);
    QueryPlanStepPtr step;
    if (has_join)
    {
        JoinType type;
        deserializeEnum(type, buf);
        switch (type)
        {
            case JoinType::Hash:
                join = HashJoin::deserialize(buf, context);
                break;
            case JoinType::Merge:
                join = MergeJoin::deserialize(buf, context);
                break;
            case JoinType::NestedLoop:
                join = NestedLoopJoin::deserialize(buf, context);
                break;
            case JoinType::Switcher:
                join = JoinSwitcher::deserialize(buf, context);
                break;
        }

        size_t max_block_size;
        readBinary(max_block_size, buf);

        step = std::make_unique<JoinStep>(left_stream, right_stream, std::move(join), max_block_size);
    }
    else
    {
        // todo output diff
        DataStream output = deserializeDataStream(buf);

        DESERIALIZE_ENUM(ASTTableJoin::Kind, kind, buf)
        DESERIALIZE_ENUM(ASTTableJoin::Strictness, strictness, buf)

        Names left_keys;
        readVectorBinary(left_keys, buf);
        Names right_keys;
        readVectorBinary(right_keys, buf);


        auto filter = deserializeAST(buf);

        bool has_using;
        readBinary(has_using, buf);

        bool has_require_right_keys;
        std::vector<bool> require_right_keys;
        readBinary(has_require_right_keys, buf);
        if (has_require_right_keys)
        {
            std::vector<UInt8> uint8_vec;
            readVectorBinary(uint8_vec, buf);
            std::transform(
                uint8_vec.begin(), uint8_vec.end(), std::back_inserter(require_right_keys), [](UInt8 x) { return static_cast<bool>(x); });
        }

        DESERIALIZE_ENUM(ASOF::Inequality, asof_inequality, buf)
        DESERIALIZE_ENUM(DistributionType, distribution_type, buf)

        bool is_magic;
        readBinary(is_magic, buf);

        DataStreams inputs = {left_stream, right_stream};

        step = std::make_shared<JoinStep>(
            inputs,
            output,
            kind,
            strictness,
            left_keys,
            right_keys,
            filter,
            has_using,
            require_right_keys,
            asof_inequality,
            distribution_type,
            is_magic);
    }

    step->setStepDescription(step_description);
    return step;
}

static ITransformingStep::Traits getStorageJoinTraits()
{
    return ITransformingStep::Traits{
        {
            .preserves_distinct_columns = false,
            .returns_single_stream = false,
            .preserves_number_of_streams = true,
            .preserves_sorting = false,
        },
        {
            .preserves_number_of_rows = false,
        }};
}

FilledJoinStep::FilledJoinStep(const DataStream & input_stream_, JoinPtr join_, size_t max_block_size_)
    : ITransformingStep(input_stream_, JoiningTransform::transformHeader(input_stream_.header, join_), getStorageJoinTraits())
    , join(std::move(join_))
    , max_block_size(max_block_size_)
{
    if (!join->isFilled())
        throw Exception(ErrorCodes::LOGICAL_ERROR, "FilledJoinStep expects Join to be filled");
}

void FilledJoinStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
    output_stream->header = JoiningTransform::transformHeader(input_streams_[0].header, join);
}

void FilledJoinStep::transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &)
{
    bool default_totals = false;
    if (!pipeline.hasTotals() && join->getTotals())
    {
        pipeline.addDefaultTotals();
        default_totals = true;
    }

    auto finish_counter = std::make_shared<JoiningTransform::FinishCounter>(pipeline.getNumStreams());

    pipeline.addSimpleTransform([&](const Block & header, QueryPipeline::StreamType stream_type) {
        bool on_totals = stream_type == QueryPipeline::StreamType::Totals;
        auto counter = on_totals ? nullptr : finish_counter;
        return std::make_shared<JoiningTransform>(header, join, max_block_size, on_totals, default_totals, counter);
    });
}

void FilledJoinStep::serialize(WriteBuffer & buf) const
{
    IQueryPlanStep::serializeImpl(buf);

    if (join)
    {
        writeBinary(true, buf);
        serializeEnum(join->getType(), buf);
        join->serialize(buf);
    }
    else
        writeBinary(false, buf);

    writeBinary(max_block_size, buf);
}

QueryPlanStepPtr FilledJoinStep::deserialize(ReadBuffer & buf, ContextPtr context)
{
    String step_description;
    readBinary(step_description, buf);

    DataStream input_stream = deserializeDataStream(buf);

    JoinPtr join = nullptr;
    bool has_join;
    readBinary(has_join, buf);
    if (has_join)
    {
        JoinType type;
        deserializeEnum(type, buf);
        switch (type)
        {
            case JoinType::Hash:
                join = HashJoin::deserialize(buf, context);
                break;
            case JoinType::Merge:
                join = MergeJoin::deserialize(buf, context);
                break;
            case JoinType::NestedLoop:
                join = NestedLoopJoin::deserialize(buf, context);
                break;
            case JoinType::Switcher:
                join = JoinSwitcher::deserialize(buf, context);
                break;
        }
    }

    bool max_block_size;
    readBinary(max_block_size, buf);

    auto step = std::make_unique<FilledJoinStep>(input_stream, std::move(join), max_block_size);

    step->setStepDescription(step_description);
    return step;
}

std::shared_ptr<IQueryPlanStep> JoinStep::copy(ContextPtr) const
{
    return std::make_shared<JoinStep>(
        input_streams,
        output_stream.value(),
        kind,
        strictness,
        left_keys,
        right_keys,
        filter,
        has_using,
        require_right_keys,
        asof_inequality,
        distribution_type,
        is_magic);
}

std::shared_ptr<IQueryPlanStep> FilledJoinStep::copy(ContextPtr) const
{
    return std::make_shared<FilledJoinStep>(input_streams[0], join, max_block_size);
}

}
