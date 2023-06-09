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

#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <DataStreams/IBlockInputStream.h>
#include <DataTypes/DataTypeNullable.h>
#include <Interpreters/RuntimeFilter/RuntimeFilter.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ExpressionListParsers.h>
#include <QueryPlan/PlanSerDerHelper.h>
#include <Common/assert_cast.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

RuntimeFilter::RuntimeFilter() : log(&Poco::Logger::get("RuntimeFilter"))
{
}

void RuntimeFilter::init(
    ContextPtr context_,
    const Block & build_header_,
    const Block & stream_header,
    std::unordered_map<String, String> & join_key_map_,
    bool enable_bloom_filter_,
    bool enable_range_filter_,
    std::unordered_map<String, std::vector<String>> & multiple_alias_)
{
    Stopwatch timer{CLOCK_MONOTONIC_COARSE};
    timer.start();
    settings = context_->getSettingsRef();
    enable_bloom_filter = enable_bloom_filter_;
    enable_range_filter = enable_range_filter_;
    build_join_keys = build_header_.cloneEmpty();
    join_column_map = join_key_map_;
    multiple_alias = multiple_alias_;
    if (enable_bloom_filter)
    {
        for (const auto & name : build_join_keys.getNames())
            col_to_bf[name] = std::make_shared<BloomFilterV2>(
                context_->getSettingsRef().dynamic_filter_default_bytes, context_->getSettingsRef().dynamic_filter_default_hashes);
    }

    if (enable_range_filter)
        initAggregator(stream_header);
    build_time += timer.elapsedMilliseconds();
}

void RuntimeFilter::init(const Settings & context_settings)
{
    Stopwatch timer{CLOCK_MONOTONIC_COARSE};
    timer.start();
    if (enable_range_filter)
    {
        Block stream_header = build_join_keys.cloneEmpty();
        settings = context_settings;
        initAggregator(stream_header);
    }
    build_time += timer.elapsedMilliseconds();
}

void RuntimeFilter::initAggregator(const Block & stream_header)
{
    std::vector<String> aggregates = {"min", "max"};
    AggregateDescriptions aggregate_descriptions;
    for (const auto & column_type : build_join_keys)
    {
        ASTPtr column_identifier = std::make_shared<ASTIdentifier>(column_type.name);
        for (const String & aggregate : aggregates)
        {
            std::shared_ptr<ASTFunction> agg_function = makeASTFunction(aggregate, column_identifier);
            AggregateDescription aggregate_desc;
            aggregate_desc.column_name = agg_function->getColumnName();
            const ASTs & arguments = agg_function->arguments->children;
            aggregate_desc.argument_names.resize(arguments.size());
            aggregate_desc.arguments.resize(arguments.size());
            DataTypes types(arguments.size());
            for (size_t i = 0; i < arguments.size(); ++i)
            {
                aggregate_desc.argument_names[i] = column_type.name;
                aggregate_desc.arguments[i] = stream_header.getPositionByName(column_type.name);
                types[i] = column_type.type;
            }
            AggregateFunctionProperties properties;
            aggregate_desc.function
                = AggregateFunctionFactory::instance().get(agg_function->name, types, aggregate_desc.parameters, properties);
            aggregate_descriptions.push_back(aggregate_desc);
        }
    }

    bool allow_to_use_two_level_group_by = settings.max_bytes_before_external_group_by != 0;
    Aggregator::Params params(
        stream_header,
        {},
        aggregate_descriptions,
        true,
        settings.max_rows_to_group_by,
        settings.group_by_overflow_mode,
        allow_to_use_two_level_group_by ? settings.group_by_two_level_threshold : 0,
        allow_to_use_two_level_group_by ? settings.group_by_two_level_threshold_bytes : 0,
        settings.max_bytes_before_external_group_by,
        settings.empty_result_for_aggregation_by_empty_set,
        nullptr,
        settings.max_threads,
        settings.min_free_disk_space_for_temporary_data,
        settings.compile_aggregate_expressions,
        settings.min_count_to_compile_aggregate_expression);

    /* The order must be maintained since aggregator will be accessed in
     * data_variants' destructor */
    if (data_variants)
        data_variants.reset();
    data_variants = std::make_shared<AggregatedDataVariants>();

    if (aggregator)
        aggregator.reset();
    aggregator = std::make_shared<Aggregator>(params);
    if (!aggregate_columns.empty())
        aggregate_columns.clear();
    aggregate_columns.resize(params.aggregates_size);

    if (!key.empty())
        key.clear();
    key.resize(params.keys_size);

    if (!key_columns.empty())
        key_columns.clear();
    key_columns.resize(params.keys_size);
}

void RuntimeFilter::merge(RuntimeFilter & other_filter)
{
    Stopwatch timer{CLOCK_MONOTONIC_COARSE};
    timer.start();

    if (enable_bloom_filter)
    {
        for (auto & iter : other_filter.col_to_bf)
        {
            if (col_to_bf.find(iter.first) == col_to_bf.end())
                col_to_bf[iter.first] = iter.second;
            else
            {
                auto & rf = col_to_bf[iter.first];
                rf->mergeInplace(*iter.second);
            }
        }
    }

    if (enable_range_filter && other_filter.getRangeValid())
    {
        for (auto & column_type : build_join_keys)
        {
            MutableColumnPtr col_to = column_type.column->assumeMutable();
            col_to->insertRangeFrom(
                *other_filter.build_join_keys.getByName(column_type.name).column.get(), 0, other_filter.build_join_keys.rows());
            column_type.column = std::move(col_to);
        }

        Block stream_header = build_join_keys.cloneEmpty();
        initAggregator(stream_header);
        bool no_more_keys = false;
        aggregator->executeOnBlock(build_join_keys, *data_variants, key_columns, aggregate_columns, /*key,*/ no_more_keys);
        range_valid = true;
        finalize();
    }
    build_bytes += other_filter.getBuildBytes();
    build_time += other_filter.getBuildTime() + timer.elapsedMilliseconds();
}

void RuntimeFilter::mergeBatchFilers(std::vector<std::shared_ptr<RuntimeFilter>> other_filters)
{
    Stopwatch timer{CLOCK_MONOTONIC_COARSE};
    timer.start();

    if (other_filters.empty())
        return;

    for (auto & other_filter : other_filters)
    {
        if (enable_bloom_filter)
        {
            for (auto & iter : other_filter->col_to_bf)
            {
                if (col_to_bf.find(iter.first) == col_to_bf.end())
                    col_to_bf[iter.first] = iter.second;
                else
                {
                    auto & rf = col_to_bf[iter.first];
                    rf->mergeInplace(*iter.second);
                }
            }
        }

        build_bytes += other_filter->getBuildBytes();
        build_time += other_filter->getBuildTime();
    }

    if (enable_range_filter)
    {
        for (auto & column_type : build_join_keys)
        {
            MutableColumnPtr col_to = column_type.column->assumeMutable();
            for (auto & other_filter : other_filters)
            {
                if (other_filter->getRangeValid())
                    col_to->insertRangeFrom(
                        *other_filter->build_join_keys.getByName(column_type.name).column.get(), 0, other_filter->build_join_keys.rows());
                range_valid = true;
            }
            column_type.column = std::move(col_to);
        }

        Block stream_header = build_join_keys.cloneEmpty();
        initAggregator(stream_header);
        bool no_more_keys = false;
        aggregator->executeOnBlock(build_join_keys, *data_variants, key_columns, aggregate_columns, /*key,*/ no_more_keys);
        finalize();
    }
    build_time += timer.elapsedMilliseconds();
}

void RuntimeFilter::add(Block & block)
{
    Stopwatch timer{CLOCK_MONOTONIC_COARSE};
    timer.start();

    if (enable_bloom_filter)
    {
        for (const auto & col : block)
        {
            if (build_join_keys.has(col.name))
            {
                const auto & bf_ptr = col_to_bf[col.name];
                build_bytes += col.column->byteSize();
                for (size_t i = 0; i < block.rows(); ++i)
                {
                    if (const auto * nullable = checkAndGetColumn<ColumnNullable>(col.column.get()))
                    {
                        if (nullable->isNullAt(i))
                            bf_ptr->addKey(StringRef("NULL"));
                        else
                            bf_ptr->addKey(nullable->getNestedColumn().getDataAt(i));
                    }
                    else
                    {
                        bf_ptr->addKey(col.column->getDataAt(i));
                    }
                }
            }
        }
    }

    if (enable_range_filter)
    {
        bool no_more_keys = false;
        aggregator->executeOnBlock(block, *data_variants, key_columns, aggregate_columns, /* key,*/ no_more_keys);
        range_valid = true;
    }
    build_time += timer.elapsedMilliseconds();
}

void RuntimeFilter::finalize()
{
    if (enable_range_filter)
    {
        if (!range_valid)
        {
            Block empty_block = build_join_keys.cloneEmpty();
            build_join_keys.swap(empty_block);
            return;
        }
        Stopwatch timer{CLOCK_MONOTONIC_COARSE};
        timer.start();
        auto blocks = aggregator->convertToBlocks(*data_variants, true, settings.max_threads);
        for (auto & block : blocks)
        {
            if (!block)
                break;

            Block result_block = build_join_keys.cloneEmpty();
            for (size_t i = 0; i < block.columns(); i = i + 2)
            {
                Field min_value = (*block.getByPosition(i).column.get())[0];
                Field max_value = (*block.getByPosition(i + 1).column.get())[0];
                MutableColumnPtr filter_column_ptr = result_block.getByPosition(i / 2).column->assumeMutable();
                filter_column_ptr->insert(min_value);
                filter_column_ptr->insert(max_value);
                result_block.getByPosition(i / 2).column = std::move(filter_column_ptr);
            }
            build_join_keys.swap(result_block);
        }
        build_time += timer.elapsedMilliseconds();
    }
}

void RuntimeFilter::serialize(WriteBuffer & write_buf, bool transform)
{
    writeBinary(enable_range_filter, write_buf);
    if (transform)
    {
        Block transformed_block;
        for (auto const & build_column : build_join_keys)
        {
            auto iter = join_column_map.find(build_column.name);
            if (iter == join_column_map.end())
                throw Exception("RuntimeFilter can not find join map key with" + build_column.name, ErrorCodes::LOGICAL_ERROR);
            transformed_block.insert(ColumnWithTypeAndName(build_column.column, build_column.type, iter->second));
        }
        serializeBlock(transformed_block, write_buf);
    }
    else
        serializeBlock(build_join_keys, write_buf);
    writeBinary(join_column_map.size(), write_buf);
    for (const auto & join_key : join_column_map)
    {
        writeBinary(join_key.first, write_buf);
        writeBinary(join_key.second, write_buf);
    }

    writeBinary(enable_bloom_filter, write_buf);
    if (enable_bloom_filter)
    {
        writeBinary(col_to_bf.size(), write_buf);
        for (const auto & bf_filter : col_to_bf)
        {
            if (transform)
            {
                auto iter = join_column_map.find(bf_filter.first);
                if (iter == join_column_map.end())
                    throw Exception("RuntimeFilter can not find join map key with" + bf_filter.first, ErrorCodes::LOGICAL_ERROR);
                writeBinary(iter->second, write_buf);
            }
            else
                writeBinary(bf_filter.first, write_buf);

            bf_filter.second->serializeToBuffer(write_buf);
        }

        /// serialize multiple alias
        writeBinary(multiple_alias.size(), write_buf);
        for (const auto & aliases : multiple_alias)
        {
            writeBinary(aliases.first, write_buf);
            writeBinary(aliases.second.size(), write_buf);
            for (const auto & alias_original : aliases.second)
                writeBinary(alias_original, write_buf);
        }
    }

    writeBinary(build_time, write_buf);
    writeBinary(build_bytes, write_buf);
    writeBinary(range_valid, write_buf);
}

void RuntimeFilter::deserialize(ReadBuffer & read_buf, bool transform)
{
    readBinary(enable_range_filter, read_buf);
    build_join_keys = deserializeBlock(read_buf);
    size_t join_key_map_size;
    readBinary(join_key_map_size, read_buf);
    for (size_t i = 0; i < join_key_map_size; i++)
    {
        String build_column;
        String execute_column;
        readBinary(build_column, read_buf);
        readBinary(execute_column, read_buf);
        join_column_map.emplace(build_column, execute_column);
    }
    readBinary(enable_bloom_filter, read_buf);
    if (enable_bloom_filter)
    {
        size_t br_filter_size;
        readBinary(br_filter_size, read_buf);
        for (size_t i = 0; i < br_filter_size; i++)
        {
            String column_name;
            readBinary(column_name, read_buf);
            std::shared_ptr<BloomFilterV2> bf = std::make_shared<BloomFilterV2>();
            bf->deserialize(read_buf);
            col_to_bf.emplace(column_name, bf);
        }

        size_t multiple_alias_size;
        readBinary(multiple_alias_size, read_buf);
        for (size_t i = 0; i < multiple_alias_size; i++)
        {
            String alias;
            readBinary(alias, read_buf);
            size_t alias_size;
            readBinary(alias_size, read_buf);
            for (size_t j = 0; j < alias_size; j++)
            {
                String alias_original;
                readBinary(alias_original, read_buf);
                multiple_alias[alias].emplace_back(alias_original);
            }
        }

        /// when deserialize need transform put all alias name to bloom
        /// filters to share same runtime filter with shuffle key
        if (transform)
        {
            for (const auto & bf : col_to_bf)
            {
                if (multiple_alias.count(bf.first))
                {
                    for (const auto & original_alias : multiple_alias.find(bf.first)->second)
                        col_to_bf[original_alias] = bf.second;
                }
            }
        }
    }
    readBinary(build_time, read_buf);
    readBinary(build_bytes, read_buf);
    readBinary(range_valid, read_buf);
}

ASTPtr RuntimeFilter::parseJoinKey(const String & bloom_key)
{
    ASTPtr join_key;
    const char * begin = bloom_key.data();
    const char * end = bloom_key.data() + bloom_key.size();
    size_t max_query_size = settings.max_query_size;
    Tokens tokens(begin, end, max_query_size);
    IParser::Pos pos(tokens, settings.max_parser_depth);
    Expected expected;
    bool parse_res = ParserExpressionWithOptionalAlias(true, ParserSettings::CLICKHOUSE).parse(pos, join_key, expected);
    if (!parse_res)
        throw Exception("RuntimeFilter can parse join key as expression -" + bloom_key, ErrorCodes::LOGICAL_ERROR);
    return join_key;
}

ASTPtr RuntimeFilter::parseRangeKey(const ColumnWithTypeAndName & range_key, size_t row)
{
    DataTypePtr range_key_type;
    if (range_key.type->isNullable())
    {
        const auto * nullable_type = typeid_cast<const DataTypeNullable *>(range_key.type.get());
        range_key_type = nullable_type->getNestedType();
    }
    else
        range_key_type = range_key.type;
    WhichDataType which(range_key_type);
    if (which.idx == TypeIndex::Date || which.idx == TypeIndex::DateTime || which.idx == TypeIndex::DateTime64)
    {
        WriteBufferFromOwnString write_buffer;
        serializeColumn(range_key.column, range_key.type, write_buffer);
        // range_key.type->serializeAsText(*range_key.column, row, write_buffer, FormatSettings());
        return std::make_shared<ASTLiteral>(write_buffer.str());
    }
    return std::make_shared<ASTLiteral>((*range_key.column.get())[row]);
}

std::unordered_map<std::string, std::pair<ASTPtr, ASTPtr>> RuntimeFilter::getMinMax()
{
    std::unordered_map<std::string, std::pair<ASTPtr, ASTPtr>> min_max;
    if (!enable_range_filter || build_join_keys.rows() != 2)
        return min_max;

    for (const auto & column_type : build_join_keys)
    {
        min_max.emplace(column_type.name, std::make_pair(parseRangeKey(column_type, 0), parseRangeKey(column_type, 1)));
    }
    return min_max;
}

ASTs RuntimeFilter::getPredicate(const String & seg_key)
{
    ASTs conditions;
    for (const auto & column_type : build_join_keys)
    {
        ASTPtr join_key = std::make_shared<ASTIdentifier>(column_type.name);
        if (enable_range_filter && build_join_keys.rows() == 2 && column_type.type->isComparable())
        {
            /// construct greater function
            const auto greater_function = std::make_shared<ASTFunction>();
            greater_function->name = "greaterOrEquals";
            greater_function->arguments = std::make_shared<ASTExpressionList>();
            greater_function->children.push_back(greater_function->arguments);
            ASTPtr min_value = parseRangeKey(column_type, 0);
            greater_function->arguments->children.push_back(join_key);
            greater_function->arguments->children.push_back(min_value);

            /// construct less function
            const auto less_function = std::make_shared<ASTFunction>();
            less_function->name = "lessOrEquals";
            less_function->arguments = std::make_shared<ASTExpressionList>();
            less_function->children.push_back(less_function->arguments);
            ASTPtr max_value = parseRangeKey(column_type, 1);
            less_function->arguments->children.push_back(join_key);
            less_function->arguments->children.push_back(max_value);

            conditions.push_back(greater_function);
            conditions.push_back(less_function);
        }

        /// construct bloom filter existence function
        if (enable_bloom_filter)
        {
            const auto bloom_function = std::make_shared<ASTFunction>();
            bloom_function->name = "bloomFilterExist";
            bloom_function->arguments = std::make_shared<ASTExpressionList>();
            bloom_function->children.push_back(bloom_function->arguments);
            ASTPtr seg_key_ast = std::make_shared<ASTLiteral>(seg_key);
            bloom_function->arguments->children.push_back(seg_key_ast);
            bloom_function->arguments->children.push_back(join_key);
            conditions.push_back(bloom_function);
        }
    }

    return conditions;
}

BloomFilterV2Ptr RuntimeFilter::getBloomFilterByColumn(const String & col_name)
{
    if (enable_bloom_filter)
    {
        if (col_to_bf.find(col_name) == col_to_bf.end())
            return nullptr;
        else
            return col_to_bf[col_name];
    }
    else
        return nullptr;
}

size_t RuntimeFilter::size()
{
    size_t total_size = 0;
    if (enable_bloom_filter)
    {
        for (const auto & bf : col_to_bf)
            total_size += bf.second->size();
    }
    total_size += build_join_keys.bytes();
    return total_size;
}

}
