/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include <DataStreams/materializeBlock.h>
#include <DataTypes/DataTypeAggregateFunction.h>
#include <AggregateFunctions/AggregateFunctionMerge.h>
#include <AggregateFunctions/AggregateFunctionNull.h>
#include <AggregateFunctions/IAggregateFunction.h>
#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <DataTypes/DataTypeNullable.h>
#include <Columns/ColumnAggregateFunction.h>

namespace DB
{

Block materializeBlock(const Block & block)
{
    if (!block)
        return block;

    Block res = block;
    size_t columns = res.columns();
    for (size_t i = 0; i < columns; ++i)
    {
        auto & element = res.getByPosition(i);
        element.column = element.column->convertToFullColumnIfConst();
    }

    return res;
}

void materializeBlockInplace(Block & block)
{
    for (size_t i = 0; i < block.columns(); ++i)
        block.getByPosition(i).column = block.getByPosition(i).column->convertToFullColumnIfConst();
}

void substituteBlock(Block & block, const std::unordered_map<String, String> & name_substitution_info)
{
    Block new_block;
    new_block.info = block.info;

    for (auto it = block.begin(); it != block.end(); ++it)
    {
        /// Substitute the column's name
        const auto & substitution_name = name_substitution_info.at(it->name);
        if (!substitution_name.empty())
            it->name = substitution_name;

        if (const auto * data_type = typeid_cast<const DataTypeAggregateFunction *>(it->type.get()))
        {
            /// We can just use the `nested_func` to change the column info,
            /// because this stream is only called after AggregatingBlockInputStream, so the block is already aggregated,
            /// we only need to merge the aggregate functions' states and
            /// won't call the `AggregateFunctionMerge::add` again (only this function is not directly call it's nested function's add method).
            /// So strip the outer merge aggregate function is fine.
            if (const auto * merge_agg_func = typeid_cast<const AggregateFunctionMerge *>(data_type->getFunction().get()))
            {
                auto nested_func = merge_agg_func->getNestedFunction();
                it->type = std::make_shared<DataTypeAggregateFunction>(nested_func,
                                                                       nested_func->getArgumentTypes(),
                                                                       nested_func->getParameters());
                if (it->column)
                {
                    auto * aggr_column = const_cast<ColumnAggregateFunction *>(typeid_cast<const ColumnAggregateFunction *>(
                        it->column.get()));
                    aggr_column->set(nested_func);
                }
            }
            else
            {
                /// The merge aggregate function maybe inside a null aggregate function
                const auto *null_true_agg_func = typeid_cast<const AggregateFunctionNullUnary<true, true> *>(data_type->getFunction().get());
                const auto *null_false_agg_func = typeid_cast<const AggregateFunctionNullUnary<false, true> *>(data_type->getFunction().get());
                if (null_true_agg_func || null_false_agg_func)
                {
                    if (const auto * nested_merge_agg_func = typeid_cast<const AggregateFunctionMerge *>(
                            null_true_agg_func ? null_true_agg_func->getNestedFunction().get()
                                               : null_false_agg_func->getNestedFunction().get()))
                    {
                        const auto & merge_func_arg_type = typeid_cast<const DataTypeAggregateFunction &>(*(nested_merge_agg_func->getArgumentTypes()[0]));
                        auto nested_func = nested_merge_agg_func->getNestedFunction();

                        /// Since this merge aggregate function inside a null aggregate function, we generate a new aggregate function
                        /// that can handle nullable arguments instead of using the nested function directly.
                        AggregateFunctionProperties properties;
                        auto function = AggregateFunctionFactory::instance().get(nested_func->getName(),
                                                                                 merge_func_arg_type.getArgumentsDataTypes(),
                                                                                 nested_func->getParameters(), properties);
                        it->type = std::make_shared<DataTypeAggregateFunction>(function,
                                                                               function->getArgumentTypes(),
                                                                               function->getParameters());
                        if (it->column)
                        {
                            auto * aggr_column = const_cast<ColumnAggregateFunction *>(typeid_cast<const ColumnAggregateFunction *>(
                                it->column.get()));
                            aggr_column->set(function);
                        }
                    }
                }
            }
        }

        new_block.insert(std::move(*it));
    }

    block.swap(new_block);
}

}

