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

#include <variant>
#include <Columns/ColumnsNumber.h>
#include <DataStreams/IBlockInputStream.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/InterpreterCreateStatsQuery.h>
#include <Parsers/ASTStatsQuery.h>
#include <Protos/optimizer_statistics.pb.h>
#include <Statistics/StatisticsCollector.h>
#include <Statistics/StatsTableBasic.h>
#include <Statistics/TypeUtils.h>
#include <Common/Stopwatch.h>


namespace DB
{
namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
    extern const int LOGICAL_ERROR;
    extern const int UNKNOWN_TABLE;
    extern const int INCORRECT_DATA;
    extern const int PARAMETER_OUT_OF_BOUND;
}
using namespace Statistics;

template <typename QueryType>
static auto getTableIdentifier(ContextPtr context, const QueryType * query)
{
    std::vector<StatsTableIdentifier> tables;
    auto catalog = createCatalogAdaptor(context);
    auto db = context->resolveDatabase(query->database);
    if (query->target_all)
    {
        tables = catalog->getAllTablesID(db);
        if (tables.empty())
        {
            auto err_msg = fmt::format(FMT_STRING("current database `{}` has no tables"), db);
            LOG_WARNING(&Poco::Logger::get("CreateStats"), err_msg);
        }
    }
    else
    {
        auto table_info_opt = catalog->getTableIdByName(db, query->table);
        if (!table_info_opt)
        {
            auto msg = "Unknown Table (" + query->table + ") in database (" + db + ")";
            throw Exception(msg, ErrorCodes::UNKNOWN_TABLE);
        }
        tables.emplace_back(table_info_opt.value());
    }
    return tables;
}


struct CollectTarget
{
    StatsTableIdentifier table_identifier;
    ColumnDescVector columns_desc;
};

static Block constructInfoBlock(ContextPtr context, const CollectTarget & target, String row_count_or_error, double time)
{
    Block block;
    auto append_str_column = [&](String header, String value) {
        ColumnWithTypeAndName tuple;
        tuple.name = header;
        tuple.type = std::make_shared<DataTypeString>();
        auto col = tuple.type->createColumn();
        col->insertData(value.data(), value.size());
        tuple.column = std::move(col);
        block.insert(std::move(tuple));
    };

    auto append_num_column = [&]<typename T>(String header, T value) {
        static_assert(std::is_trivial_v<T>);
        ColumnWithTypeAndName tuple;
        tuple.name = header;
        tuple.type = std::make_shared<DataTypeNumber<T>>();
        auto col = ColumnVector<T>::create();
        col->insertValue(value);
        tuple.column = std::move(col);
        block.insert(std::move(tuple));
    };

    append_str_column("table_name", target.table_identifier.getTableName());
    append_num_column("column_count", target.columns_desc.size());
    append_str_column("row_count_or_error", row_count_or_error);
    if (context->getSettingsRef().create_stats_time_output)
    {
        append_num_column("elapsed_time", time);
    }
    return block;
}

// slice CollectTarget to multiple if column_size is larger than max_columns
static std::vector<CollectTarget> sliceTargets(UInt64 max_columns, std::vector<CollectTarget> old_targets)
{
    if (max_columns < 1)
    {
        throw Exception("Incorrect max columns, should be at least 1", ErrorCodes::PARAMETER_OUT_OF_BOUND);
    }

    std::vector<CollectTarget> result;
    for (auto & target : old_targets)
    {
        if (target.columns_desc.size() <= max_columns)
        {
            result.emplace_back(std::move(target));
            continue;
        }

        auto target_size = target.columns_desc.size();
        for (size_t col = 0; col < target_size; col += max_columns)
        {
            auto col_next = std::min(col + max_columns, target_size);
            auto beg_iter = std::make_move_iterator(target.columns_desc.begin() + col);
            auto end_iter = std::make_move_iterator(target.columns_desc.begin() + col_next);
            auto sub_columns = ColumnDescVector(beg_iter, end_iter);
            result.emplace_back(CollectTarget{target.table_identifier, std::move(sub_columns)});
        }
    }
    return result;
}

// return row_count
Int64 collectStatsOnTarget(ContextPtr context, const CollectorSettings & settings, const CollectTarget & collect_target)
{
    auto catalog = createCatalogAdaptor(context);
    StatisticsCollector impl(context, catalog, collect_target.table_identifier, settings);
    impl.collect(collect_target.columns_desc);

    impl.writeToCatalog();
    auto row_count = impl.getTableStats().basic->getRowCount();

    return row_count;
}

namespace
{
    class CreateStatsBlockInputStream : public IBlockInputStream, WithContext
    {
    public:
        CreateStatsBlockInputStream(ContextPtr context_, const CollectorSettings & settings_, std::vector<CollectTarget> collect_targets_)
            : WithContext(context_), settings(settings_), collect_targets(std::move(collect_targets_))
        {
        }
        String getName() const override { return "Statistics"; }
        Block getHeader() const override { return {}; }

    private:
        Block readImpl() override
        {
            auto context = getContext();
            Stopwatch watch;
            if (counter >= collect_targets.size())
            {
                if (error_infos.empty())
                {
                    // succeed
                    return {};
                }
                // handle errors
                String total_error;
                for (const auto & [k, v] : error_infos)
                {
                    total_error += fmt::format(FMT_STRING("when collecting table {} having the following error: {}\n"), k, v);
                }
                throw Exception(total_error, ErrorCodes::INCORRECT_DATA);
            }

            auto collect_target = collect_targets.at(counter++);

            try
            {
                auto row_count = collectStatsOnTarget(context, settings, collect_target);
                auto elapsed_time = watch.elapsedSeconds();
                return constructInfoBlock(context, collect_target, std::to_string(row_count), elapsed_time);
            }
            catch (...)
            {
                auto logger = &Poco::Logger::get("CreateStats");
                auto elapsed_time = watch.elapsedSeconds();
                auto err_info_with_stack = getCurrentExceptionMessage(true);
                LOG_ERROR(logger, err_info_with_stack);

                auto err_info = getCurrentExceptionMessage(false);
                error_infos.emplace(collect_target.table_identifier.getDbTableName(), err_info_with_stack);

                return constructInfoBlock(context, collect_target, err_info, elapsed_time);
            }
        }

    private:
        std::map<String, String> error_infos;
        CollectorSettings settings;
        std::vector<CollectTarget> collect_targets;
        size_t counter = 0;
    };
}

BlockIO InterpreterCreateStatsQuery::execute()
{
    auto context = getContext();
    auto query = query_ptr->as<const ASTCreateStatsQuery>();
    if (!query)
    {
        throw Exception("Create stats query logical error", ErrorCodes::LOGICAL_ERROR);
    }

    auto tables = getTableIdentifier(context, query);
    std::vector<CollectTarget> valid_targets;
    auto catalog = createCatalogAdaptor(context);
    // TODO: check more for support
    for (const auto & table : tables)
    {
        if (catalog->isTableCollectable(table))
        {
            if (query->if_not_exists && catalog->hasStatsData(table))
            {
                // skip when if_not_exists is on
                continue;
            }
            CollectTarget target{.table_identifier = table};
            if (query->columns.empty())
                target.columns_desc = catalog->getCollectableColumns(table);
            else
            {
                target.columns_desc = filterCollectableColumns(catalog->getCollectableColumns(table), query->columns);
            }

            valid_targets.emplace_back(std::move(target));
        }
    }

    if (valid_targets.empty())
    {
        return {};
    }

    valid_targets = sliceTargets(context->getSettingsRef().statistics_batch_max_columns, std::move(valid_targets));

    catalog->checkHealth(/*is_write=*/true);
    CollectorSettings settings(context->getSettingsRef());
    using SampleType = ASTCreateStatsQuery::SampleType;

    if (query->sample_type == SampleType::FullScan)
    {
        settings.enable_sample = false;
    }
    else if (query->sample_type == SampleType::Sample)
    {
        settings.enable_sample = true;
        if (query->sample_rows)
        {
            settings.sample_row_count = *query->sample_rows;
        }

        if (query->sample_ratio)
        {
            settings.sample_ratio = *query->sample_ratio;
        }
    }

    BlockIO io;
    io.in = std::make_shared<CreateStatsBlockInputStream>(context, settings, std::move(valid_targets));
    return io;
}

}
