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

#include <Statistics/TableHandler.h>
#include <fmt/format.h>

namespace DB::Statistics
{

void TableHandler::registerHandler(std::unique_ptr<ColumnHandlerBase> handler)
{
    column_size += handler->size();
    handlers.emplace_back(std::move(handler));
}


String TableHandler::getFullSql()
{
    std::vector<String> sql_components;
    for (auto & handler : handlers)
    {
        auto sqls = handler->getSqls();
        sql_components.insert(sql_components.end(), sqls.begin(), sqls.end());
    }
    auto full_sql = fmt::format(FMT_STRING("select {} from {}"), fmt::join(sql_components, ", "), table_identifier.getDbTableName());
    LOG_INFO(&Poco::Logger::get("TableHandler"), "full_sql={}", full_sql);
    return full_sql;
}

void TableHandler::parse(const Block & block)
{
    LOG_INFO(&Poco::Logger::get("TableHandler"), "table={}", table_identifier.getDbTableName());
    if (block.columns() != column_size)
    {
        throw Exception("fetched block has wrong column size", ErrorCodes::LOGICAL_ERROR);
    }

    size_t column_offset = 0;
    for (auto & handler : handlers)
    {
        handler->parse(block, column_offset);
        column_offset += handler->size();
    }

    if (column_offset != column_size)
    {
        throw Exception("block column not match", ErrorCodes::LOGICAL_ERROR);
    }
}
}
