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

#include <Storages/SelectQueryInfo.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <Parsers/ASTSerDerHelper.h>

namespace DB
{

void PrewhereInfo::serialize(WriteBuffer & buf) const
{
    if (alias_actions)
    {
        writeBinary(true, buf);
        alias_actions->serialize(buf);
    }
    else
        writeBinary(false, buf);

    if (row_level_filter)
    {
        writeBinary(true, buf);
        row_level_filter->serialize(buf);
    }
    else
        writeBinary(false, buf);

    if (prewhere_actions)
    {
        writeBinary(true, buf);
        prewhere_actions->serialize(buf);
    }
    else
        writeBinary(false, buf);

    writeBinary(row_level_column_name, buf);
    writeBinary(prewhere_column_name, buf);
    writeBinary(remove_prewhere_column, buf);
    writeBinary(need_filter, buf);
}

PrewhereInfoPtr PrewhereInfo::deserialize(ReadBuffer & buf, ContextPtr context)
{
    ActionsDAGPtr alias_actions;
    ActionsDAGPtr row_level_filter;
    ActionsDAGPtr prewhere_actions;

    bool has_alias_actions;
    readBinary(has_alias_actions, buf);
    if (has_alias_actions)
        alias_actions = ActionsDAG::deserialize(buf, context);

    bool has_row_level_filter;
    readBinary(has_row_level_filter, buf);
    if (has_row_level_filter)
        row_level_filter = ActionsDAG::deserialize(buf, context);

    bool has_prewhere_actions;
    readBinary(has_prewhere_actions, buf);
    if (has_prewhere_actions)
        prewhere_actions = ActionsDAG::deserialize(buf, context);

    String row_level_column_name;
    String prewhere_column_name;
    bool remove_prewhere_column;
    bool need_filter;

    readBinary(row_level_column_name, buf);
    readBinary(prewhere_column_name, buf);
    readBinary(remove_prewhere_column, buf);
    readBinary(need_filter, buf);

    auto prewhere_info_ptr = std::make_shared<PrewhereInfo>(prewhere_actions, prewhere_column_name);
    prewhere_info_ptr->alias_actions = alias_actions;
    prewhere_info_ptr->row_level_filter = row_level_filter;
    prewhere_info_ptr->row_level_column_name = row_level_column_name;
    prewhere_info_ptr->remove_prewhere_column = remove_prewhere_column;
    prewhere_info_ptr->need_filter = need_filter;

    return prewhere_info_ptr;
}

void InputOrderInfo::serialize(WriteBuffer & buf) const
{
    serializeSortDescription(order_key_prefix_descr, buf);
    writeBinary(direction, buf);
}

void InputOrderInfo::deserialize(ReadBuffer & buf)
{
    deserializeSortDescription(order_key_prefix_descr, buf);
    readBinary(direction, buf);
}

void SelectQueryInfo::serialize(WriteBuffer & buf) const
{
    serializeAST(query, buf);
    serializeAST(view_query, buf);
}

void SelectQueryInfo::deserialize(ReadBuffer & buf)
{
    query = deserializeAST(buf);
    view_query = deserializeAST(buf);
}

}
