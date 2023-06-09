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

#include <type_traits>
#include <Statistics/CollectStep.h>
#include <Statistics/ParseUtils.h>
#include <Statistics/StatsCpcSketch.h>
#include <Statistics/StatsNdvBuckets.h>
#include <Statistics/TableHandler.h>
#include <Statistics/TypeUtils.h>
#include <boost/noncopyable.hpp>
#include <fmt/format.h>

namespace DB::Statistics
{

class FirstFullColumnHandler : public ColumnHandlerBase
{
public:
    explicit FirstFullColumnHandler(HandlerContext & handler_context_, const NameAndTypePair & col_desc) : handler_context(handler_context_)
    {
        col_name = col_desc.name;
        data_type = decayDataType(col_desc.type);
        config = get_column_config(handler_context.settings, data_type);
    }

    std::vector<String> getSqls() override
    {
        auto quote_col_name = backQuoteIfNeed(col_name);
        auto wrapped_col_name = getWrappedColumnName(config, quote_col_name);

        auto count_sql = fmt::format(FMT_STRING("count({})"), quote_col_name);
        auto ndv_sql = fmt::format(FMT_STRING("cpc({})"), wrapped_col_name);
        auto histogram_sql = fmt::format(FMT_STRING("kll({})"), wrapped_col_name);
        // to estimate ndv
        LOG_INFO(
            &Poco::Logger::get("FirstFullColumnHandler"),
            fmt::format(
                FMT_STRING("col info: col={} && "
                           "sqls={},{},{}"),
                col_name,
                count_sql,
                ndv_sql,
                histogram_sql));

        return {count_sql, ndv_sql, histogram_sql};
    }

    size_t size() override { return 3; }

    void parse(const Block & block, size_t index_offset) override
    {
        // count(col)
        auto nonnull_count = static_cast<double>(getSingleValue<UInt64>(block, index_offset + 0));
        // cpc(col)
        auto ndv_b64 = getSingleValue<std::string_view>(block, index_offset + 1);
        // kll(col)
        auto histogram_b64 = getSingleValue<std::string_view>(block, index_offset + 2);

        // select count(*)
        double full_count = handler_context.full_count;

        HandlerColumnData result;

        // algorithm requires block_ndv as sample_nonnull
        if (nonnull_count != 0)
        {
            double ndv = getNdvFromBase64(ndv_b64);
            result.nonnull_count = nonnull_count;
            result.ndv_value_opt = std::min(ndv, nonnull_count);
            if (!histogram_b64.empty())
            {
                auto histogram = createStatisticsTyped<StatsKllSketch>(StatisticsTag::KllSketch, base64Decode(histogram_b64));
                result.min_as_double = histogram->minAsDouble().value_or(std::nan(""));
                result.max_as_double = histogram->maxAsDouble().value_or(std::nan(""));

                result.bucket_bounds = histogram->getBucketBounds();
            }
            LOG_INFO(
                &Poco::Logger::get("FirstFullColumnHandler"),
                fmt::format(
                    FMT_STRING("col info: col={} && "
                               "context raw data: full_count={} && "
                               "column raw data: nonnull_count={}, ndv=&& "
                               "cast data: result.nonnull_count={}, result.ndv_value={}"),
                    col_name,
                    full_count,
                    nonnull_count,
                    ndv,
                    result.nonnull_count,
                    result.ndv_value_opt.value_or(-1)));
        }
        else
        {
            result.nonnull_count = 0;
            result.ndv_value_opt = 0;
            // use NaN for min/max
            result.min_as_double = std::numeric_limits<double>::quiet_NaN();
            result.max_as_double = std::numeric_limits<double>::quiet_NaN();
        }

        // write result to context
        handler_context.columns_data[col_name] = std::move(result);
    }

private:
    HandlerContext & handler_context;
    String col_name;
    DataTypePtr data_type;
    ColumnCollectConfig config;
};

class SecondFullColumnHandler : public ColumnHandlerBase
{
public:
    explicit SecondFullColumnHandler(
        HandlerContext & handler_context_, std::shared_ptr<BucketBounds> bucket_bounds_, const NameAndTypePair & col_desc)
        : handler_context(handler_context_), bucket_bounds(bucket_bounds_)
    {
        col_name = col_desc.name;
        data_type = decayDataType(col_desc.type);
        config = get_column_config(handler_context.settings, data_type);
    }

    std::vector<String> getSqls() override
    {
        auto quote_col_name = backQuoteIfNeed(col_name);
        auto wrapped_col_name = getWrappedColumnName(config, quote_col_name);

        auto bounds_b64 = base64Encode(bucket_bounds->serialize());

        auto ndv_buckets_sql = fmt::format(FMT_STRING("ndv_buckets('{}')({})"), bounds_b64, wrapped_col_name);
        // to estimate ndv
        return {ndv_buckets_sql};
    }

    size_t size() override { return 1; }

    void parse(const Block & block, size_t index_offset) override
    {
        auto ndv_buckets_b64 = getSingleValue<std::string_view>(block, index_offset + 0);

        if (!handler_context.columns_data.count(col_name))
        {
            throw Exception("previous result not found", ErrorCodes::LOGICAL_ERROR);
        }

        // write result to context
        auto ndv_buckets = createStatisticsTyped<StatsNdvBuckets>(StatisticsTag::NdvBuckets, base64Decode(ndv_buckets_b64));

        auto result = ndv_buckets->asResult();

        handler_context.columns_data.at(col_name).ndv_buckets_result_opt = std::move(result);
    }

private:
    HandlerContext & handler_context;
    std::shared_ptr<BucketBounds> bucket_bounds;
    String col_name;
    DataTypePtr data_type;
    ColumnCollectConfig config;
};


class StatisticsCollectorStepFull : public CollectStep
{
public:
    explicit StatisticsCollectorStepFull(StatisticsCollector & core_) : CollectStep(core_) { }

    void collectFirstStep(const ColumnDescVector & cols_desc)
    {
        TableHandler table_handler(table_info);
        table_handler.registerHandler(std::make_unique<RowCountHandler>(handler_context));

        for (const auto & col_desc : cols_desc)
        {
            table_handler.registerHandler(std::make_unique<FirstFullColumnHandler>(handler_context, col_desc));
        }

        auto sql = table_handler.getFullSql();

        auto helper = SubqueryHelper::create(context, sql);
        auto block = getOnlyRowFrom(helper);
        table_handler.parse(block);
    }

    // judge info from step
    void collectSecondStep(const ColumnDescVector & cols_desc)
    {
        TableHandler table_handler(table_info);
        table_handler.registerHandler(std::make_unique<RowCountHandler>(handler_context));
        bool to_collect = false;
        for (const auto & col_desc : cols_desc)
        {
            auto & col_info = handler_context.columns_data.at(col_desc.name);
            if (std::llround(col_info.ndv_value_opt.value()) >= 2)
            {
                table_handler.registerHandler(std::make_unique<SecondFullColumnHandler>(handler_context, col_info.bucket_bounds, col_desc));
                to_collect = true;
            }
        }
        if (!to_collect)
        {
            // no need to collect the second
            return;
        }
        auto sql = table_handler.getFullSql();

        auto helper = SubqueryHelper::create(context, sql);
        auto block = getOnlyRowFrom(helper);
        table_handler.parse(block);
    }


    // exported symbol
    void collect(const ColumnDescVector & cols_desc) override
    {
        // TODO: split them into several columns step
        collectTable();
        collectFirstStep(cols_desc);
        collectSecondStep(cols_desc);
    }
};

std::unique_ptr<CollectStep> createStatisticsCollectorStepFull(StatisticsCollector & core)
{
    return std::make_unique<StatisticsCollectorStepFull>(core);
}

}
