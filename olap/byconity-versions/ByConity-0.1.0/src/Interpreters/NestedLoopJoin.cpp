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

#include <limits>

#include <unordered_set>
#include <Columns/ColumnNullable.h>
#include <Columns/ColumnsCommon.h>
#include <Columns/FilterDescription.h>
#include <Core/NamesAndTypes.h>
#include <DataStreams/MergingSortedBlockInputStream.h>
#include <DataStreams/OneBlockInputStream.h>
#include <DataStreams/TemporaryFileStream.h>
#include <DataStreams/materializeBlock.h>
#include <Interpreters/TableJoin.h>
#include <Interpreters/ExpressionAnalyzer.h>
#include <Interpreters/NestedLoopJoin.h>
#include <Interpreters/join_common.h>
#include <QueryPlan/PlanSerDerHelper.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTIdentifier.h>
#include <QueryPlan/PlanSerDerHelper.h>


namespace DB
{
namespace ErrorCodes
{
    extern const int SET_SIZE_LIMIT_EXCEEDED;
    extern const int NOT_IMPLEMENTED;
    extern const int PARAMETER_OUT_OF_BOUND;
    extern const int NOT_ENOUGH_SPACE;
    extern const int LOGICAL_ERROR;
}

NestedLoopJoin::NestedLoopJoin(std::shared_ptr<TableJoin> table_join_, const Block & right_sample_block_, const ContextPtr & context_)
    : table_join(table_join_)
    , right_sample_block(right_sample_block_)
    , size_limits(table_join->sizeLimits())
    , nullable_right_side(table_join->forceNullableRight())
    , is_any_join(isAny(table_join->strictness()))
    , is_left(isLeft(table_join->kind()))
    , max_rows_in_right_block(table_join->maxRowsInRightBlock())
    , context(context_)
{
    if (!isLeft(table_join->kind()) && !isInner(table_join->kind()))
        throw Exception("NestedLoop join supported for LEFT and INNER JOINs only", ErrorCodes::NOT_IMPLEMENTED);

    if (!max_rows_in_right_block)
        throw Exception("partial_nested_loop_join_rows_in_right_blocks cannot be zero", ErrorCodes::PARAMETER_OUT_OF_BOUND);

    if (!size_limits.hasLimits())
    {
        size_limits.max_bytes = table_join->defaultMaxBytes();
        if (!size_limits.max_bytes)
            throw Exception(
                "No limit for NestedLoop join (max_rows_in_join, max_bytes_in_join or default_max_bytes_in_join have to be set)",
                ErrorCodes::PARAMETER_OUT_OF_BOUND);
    }
    table_join->splitAdditionalColumns(right_sample_block, right_table_keys, right_columns_to_add);
    JoinCommon::removeLowCardinalityInplace(right_columns_to_add);
    JoinCommon::createMissedColumns(right_columns_to_add);

    const NameSet required_right_keys = table_join->requiredRightKeys();
    for (const auto & column : right_table_keys)
        if (required_right_keys.count(column.name))
            right_columns_to_add.insert(ColumnWithTypeAndName{nullptr, column.type, column.name});
    JoinCommon::createMissedColumns(right_columns_to_add);

    if (nullable_right_side)
        JoinCommon::convertColumnsToNullable(right_columns_to_add);
}

void NestedLoopJoin::setTotals(const Block & totals_block)
{
    totals = totals_block;
}

const Block & NestedLoopJoin::getTotals() const
{
    return totals;
}

size_t NestedLoopJoin::getTotalRowCount() const
{
    size_t right_blocks_row_count = 0;
    for (Block block : right_blocks)
    {
        right_blocks_row_count += block.rows();
    }
    return right_blocks_row_count;
}

size_t NestedLoopJoin::getTotalByteCount() const
{
    size_t right_blocks_bytes = 0;
    for (Block block : right_blocks)
    {
        right_blocks_bytes += block.bytes();
    }
    return right_blocks_bytes;
}

bool NestedLoopJoin::saveRightBlock(Block && block)
{
    std::unique_lock lock(rwlock);
    right_blocks.emplace_back(std::move(block));
    return table_join->sizeLimits().check(getTotalRowCount(), getTotalByteCount(), "JOIN", ErrorCodes::SET_SIZE_LIMIT_EXCEEDED);
}

bool NestedLoopJoin::isConstFromLeftTable(const ColumnWithTypeAndName & rightCol, const std::unordered_set<std::string> & left_column_names)
{
    return isColumnConst(*rightCol.column) && left_column_names.find(rightCol.name) != left_column_names.end();
}

void NestedLoopJoin::paddingRightBlockWithConstColumn(Block & left_block, size_t left_row_index, Block & right_block) const
{
    auto right_block_rows = right_block.rows();

    // convert left column to const to avoid copy
    for (size_t i = 0; i < left_block.columns(); i++)
    {
        auto & left_col = left_block.getByPosition(i);
        Field field;
        left_col.column->get(left_row_index, field);
        right_block.insert({left_col.type->createColumnConst(right_block_rows, field), left_col.type, left_col.name});
    }
}

bool NestedLoopJoin::addJoinedBlock(const Block & src_block, bool)
{
    Block block = materializeBlock(src_block);
    JoinCommon::removeLowCardinalityInplace(block);
    if (nullable_right_side)
        JoinCommon::convertColumnsToNullable(block);
    return saveRightBlock(std::move(block));
}

void NestedLoopJoin::joinBlock(Block & left_block, ExtraBlockPtr &)
{
    auto left_rows = left_block.rows();
    materializeBlockInplace(left_block);
    JoinCommon::removeLowCardinalityInplace(left_block);

    NamesAndTypesList total_columns = left_block.cloneEmpty().getNamesAndTypesList();
    completeColumnsAfterJoin(total_columns);

    if (table_join->hasOn())
    {
        // generate actions for on expression
        ASTPtr expression_list = table_join->getOnExpression();
        auto syntax_result
                = TreeRewriter(context).analyze(expression_list, total_columns);

        auto actions = ExpressionAnalyzer(expression_list, syntax_result, context).getActions(true, false);

        String filter_name = expression_list->getColumnName();
        if (left_rows == 0)
        {
            completeHeader(left_block);
            return;
        }

        joinImpl(actions, filter_name, left_block);
    }
}

void NestedLoopJoin::completeColumnsAfterJoin(NamesAndTypesList & total_columns)
{
    auto it = total_columns.begin();
    // empty when analyse sql
    if (right_blocks.empty())
    {
        if (table_join->columnsFromJoinedTable().empty())
        {
            auto name_type_list = right_columns_to_add.getNamesAndTypesList();
            total_columns.insert(it, name_type_list.begin(), name_type_list.end());
        }
        else
            total_columns.insert(it, table_join->columnsFromJoinedTable().begin(), table_join->columnsFromJoinedTable().end());
    }
    else
    {
        auto name_type_list = right_blocks.front().cloneEmpty().getNamesAndTypesList();
        total_columns.insert(it, name_type_list.begin(), name_type_list.end());
    }
}

void NestedLoopJoin::completeHeader(Block & left_block)
{
    for (size_t i = 0; i < right_columns_to_add.columns(); ++i)
    {
        const auto & column = right_columns_to_add.getByPosition(i);
        left_block.insert(column);
    }
}

void NestedLoopJoin::joinImpl(
    const ExpressionActionsPtr & actions,
    const String & filter_name,
    Block & left_block)
{
    Block new_block;
    std::unordered_set<std::string> left_column_names(left_block.columns());
    for (size_t i = 0; i < left_block.columns(); i++)
    {
        left_column_names.emplace(left_block.getByPosition(i).name);
    }

    for (size_t left_row_index = 0; left_row_index < left_block.rows(); left_row_index++)
    {
        for (Block right_block : right_blocks)
        {
            paddingRightBlockWithConstColumn(left_block, left_row_index, right_block);
            actions->execute(right_block, false);
            auto filter_column = right_block.getByName(filter_name).column;
            FilterDescription filter_and_holder(*filter_column);
            auto filtered_size = countBytesInFilter(*filter_and_holder.data);

            if (new_block.columns() == 0)
            {
                for (size_t i = 0; i < right_block.columns(); ++i)
                {
                    auto & right_col = right_block.getByPosition(i);
                    // if column is const and from left table, should convert to right type
                    if (isConstFromLeftTable(right_col, left_column_names))
                    {
                        auto & left_col = left_block.getByName(right_col.name);
                        auto new_column = left_col.type->createColumn();
                        if ((is_left && filtered_size == 0) || (is_any_join && filtered_size != 0))
                            new_column->insertFrom(*left_col.column, left_row_index);
                        else
                            new_column->insertManyFrom(*left_col.column, left_row_index, filtered_size);

                        new_block.insert({std::move(new_column), left_col.type, left_col.name});
                    }
                    // column from right table
                    else
                    {
                        if (is_left && filtered_size == 0)
                        {
                            auto new_column = right_col.type->createColumn();
                            new_column->insertDefault();
                            new_block.insert({std::move(new_column), right_col.type, right_col.name});
                        }
                        else if (is_any_join && filtered_size != 0)
                        {
                            right_col.column = right_col.column->filter(*filter_and_holder.data, 1);
                            new_block.insert({right_col.column->cut(0, 1), right_col.type, right_col.name});
                        }
                        else
                            new_block.insert({right_col.column->filter(*filter_and_holder.data, 1), right_col.type, right_col.name});
                    }
                }
            }
            else
            {
                if (!is_left && filtered_size == 0)
                    continue;

                for (size_t i = 0; i < new_block.columns(); ++i)
                {
                    auto & right_col = right_block.getByPosition(i);
                    if (isConstFromLeftTable(right_col, left_column_names))
                    {
                        auto & col = left_block.getByName(right_col.name);
                        auto mutable_column = IColumn::mutate(std::move(new_block.getByPosition(i).column));
                        if ((is_left && filtered_size == 0) || (is_any_join && filtered_size != 0))
                            mutable_column->insertFrom(*col.column, left_row_index);
                        else
                            mutable_column->insertManyFrom(*col.column, left_row_index, filtered_size);

                        new_block.getByPosition(i).column = std::move(mutable_column);
                    }
                    else
                    {
                        auto mutable_column = IColumn::mutate(std::move(new_block.getByPosition(i).column));
                        if (is_left && filtered_size == 0)
                            mutable_column->insertDefault();
                        else if (is_any_join && filtered_size != 0)
                        {
                            right_col.column = right_col.column->filter(*filter_and_holder.data, 1);
                            const auto source_column = right_col.column->cut(0, 1);
                            mutable_column->insertRangeFrom(*source_column, 0, source_column->size());
                        }
                        else
                        {
                            const auto source_column = right_col.column->filter(*filter_and_holder.data, 1);
                            mutable_column->insertRangeFrom(*source_column, 0, source_column->size());
                        }
                        new_block.getByPosition(i).column = std::move(mutable_column);
                    }
                }
            }
        }
    }
    completeHeader(left_block);
    for (size_t i = 0; i < left_block.columns(); ++i)
    {
        const auto & column = left_block.getByPosition(i);
        if (new_block)
            left_block.setColumn(i, std::move(*new_block.findByName(column.name)));
        else if (is_left)
        {
            auto rows = left_block.rows();
            if (column.column->size() != rows)
            {
                auto new_column = column.type->createColumn();
                new_column->insertManyDefaults(rows);
                left_block.setColumn(i, ColumnWithTypeAndName{std::move(new_column), column.type, column.name});
            }
        }
        else if (isInner(table_join->kind()))
            left_block.setColumn(i, column.cloneEmpty());
    }
}

void NestedLoopJoin::serialize(WriteBuffer & buf) const
{
    table_join->serialize(buf);
    serializeBlock(right_sample_block, buf);
}

JoinPtr NestedLoopJoin::deserialize(ReadBuffer & buf, ContextPtr context)
{
    auto table_join = TableJoin::deserialize(buf, context);
    auto right_sample_block = deserializeBlock(buf);

    return std::make_shared<NestedLoopJoin>(table_join, right_sample_block, context);
}

}
