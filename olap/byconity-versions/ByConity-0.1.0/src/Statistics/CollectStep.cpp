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

#include <Statistics/CollectStep.h>
#include <Statistics/CommonErrorCodes.h>
#include <Statistics/StatisticsCollector.h>

namespace DB::Statistics
{

CollectStep::CollectStep(DB::Statistics::StatisticsCollector & core_)
    : core(core_), table_info(core_.table_info), catalog(core_.catalog), context(core_.context), handler_context(core_.settings)
{
}

std::unique_ptr<CollectStep> createStatisticsCollectorStepFull(StatisticsCollector & core);


void CollectStep::writeResult(TableStats & core_table_stats, ColumnStatsMap & core_columns_stats)
{
    // TODO
    auto table_basic = std::make_shared<StatsTableBasic>();
    table_basic->setRowCount(std::llround(handler_context.full_count));
    core_table_stats.basic = table_basic;

    for (auto & [col_name, col_data] : handler_context.columns_data)
    {
        ColumnStats column_stats;
        column_stats.basic = std::make_shared<StatsColumnBasic>();
        column_stats.basic->mutableProto().set_min_as_double(col_data.min_as_double);
        column_stats.basic->mutableProto().set_max_as_double(col_data.max_as_double);
        column_stats.basic->mutableProto().set_nonnull_count(std::llround(col_data.nonnull_count));
        if (!col_data.ndv_value_opt.has_value())
        {
            throw Exception("must has value", ErrorCodes::LOGICAL_ERROR);
        }
        auto ndv_value_regulated = std::min<double>(std::llround(col_data.nonnull_count), col_data.ndv_value_opt.value());
        column_stats.basic->mutableProto().set_ndv_value(ndv_value_regulated);
        column_stats.basic->mutableProto().set_total_length(0);

        if (col_data.ndv_buckets_result_opt.has_value())
        {
            column_stats.ndv_buckets_result = col_data.ndv_buckets_result_opt.value();
        }

        core_columns_stats.emplace(col_name, std::move(column_stats));
    }
}
void CollectStep::collectTable()
{
    TableHandler table_handler(table_info);
    table_handler.registerHandler(std::make_unique<RowCountHandler>(handler_context));
    //  select count(*) from <table>;
    auto sql = table_handler.getFullSql();
    auto helper = SubqueryHelper::create(context, sql);
    auto block = getOnlyRowFrom(helper);
    table_handler.parse(block);
    handler_context.full_count = handler_context.query_row_count.value();
    handler_context.query_row_count = std::nullopt;
}

}
