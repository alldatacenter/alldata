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

#include <MergeTreeCommon/CnchBucketTableCommon.h>
#include <DataTypes/DataTypesNumber.h>
#include <Columns/ColumnsNumber.h>
#include <Common/SipHash.h>
#include <Functions/FunctionFactory.h>
#include <Columns/ColumnConst.h>
#include <iostream>
#include <Common/assert_cast.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int TOO_MANY_PARTS;
}

/**
 * Receives a block with CLUSTER BY columns and appends a bucket_number column to it. Each row in the block is hashed and a bucket_number
 * is generated. This is then inserted to the bucket_number column for that corresponding row.
 * NOTE: block passed in here is the block_copy, not the original block.
 * NOTE: total_shard_num is TOTAL_BUCKET_NUMBER
 * NOTE: context here is a required arg for FunctionPtr::execute but DTSPartition hash does not use it. It can be provided an empty context.
 * TODO: Add test cases
 **/
void prepareBucketColumn(
    Block & block, const Names bucket_columns, const Int64 split_number, const bool is_with_range, const Int64 total_shard_num, const ContextPtr & context)
{
    if (split_number <= 0 && is_with_range)
        throw Exception("Unexpected operation. SPLIT_NUMBER is required for WITH_RANGE ", ErrorCodes::LOGICAL_ERROR);

    ColumnPtr bucket_number_column;
    if (split_number > 0)
    {
        if (bucket_columns.size() == 1)
        {
            createColumnWithDtsPartitionHash(block, bucket_columns, split_number, context);
        }
        else
        {
            auto split_value_column = createColumnWithSipHash(block, bucket_columns, split_number);
            block.insert(ColumnWithTypeAndName{std::move(split_value_column), std::make_shared<DataTypeInt64>(), "split_value"});
        }
        bucket_number_column = createBucketNumberColumn(block, split_number, is_with_range, total_shard_num);
    }
    else
    {
        bucket_number_column = createColumnWithSipHash(block, bucket_columns, total_shard_num);
    }
    block.insert(ColumnWithTypeAndName{std::move(bucket_number_column), std::make_shared<DataTypeUInt64>(), COLUMN_BUCKET_NUMBER});
}

void buildBucketScatterSelector(
        const ColumnRawPtrs & columns,
        PODArray<size_t> & partition_num_to_first_row,
        IColumn::Selector & selector,
        size_t max_parts)
{
    std::unordered_map<Int64, Int64> partitions_map;

    auto bucket_column = columns[0]; // There is only one column in the columns variable which is the bucket number column
    size_t num_rows = bucket_column->size();
    size_t partitions_count = 0;
    for (size_t i = 0; i < num_rows; ++i)
    {
        Int64 key = bucket_column->getInt(i);
        auto iterator_inserted_pair = partitions_map.insert({key, partitions_count});
        auto it = iterator_inserted_pair.first;
        auto inserted = iterator_inserted_pair.second;

        if (inserted)
        {
            if (max_parts && partitions_count >= max_parts)
                throw Exception("Too many partitions for single INSERT block (more than " + std::to_string(max_parts) + "). The limit is controlled by 'max_partitions_per_insert_block' setting. Large number of partitions is a common misconception. It will lead to severe negative performance impact, including slow server startup, slow INSERT queries and slow SELECT queries. Recommended total number of partitions for a table is under 1000..10000. Please note, that partitioning is not intended to speed up SELECT queries (ORDER BY key is sufficient to make range queries fast). Partitions are intended for data manipulation (DROP PARTITION, etc).", ErrorCodes::TOO_MANY_PARTS);

            partition_num_to_first_row.push_back(i);
            it->second = partitions_count;

            ++partitions_count;

            /// Optimization for common case when there is only one partition - defer selector initialization.
            if (partitions_count == 2)
            {
                selector = IColumn::Selector(num_rows);
                std::fill(selector.begin(), selector.begin() + i, 0);
            }
        }
        if (partitions_count > 1)
            selector[i] = it->second;
    }
}


void createColumnWithDtsPartitionHash(Block & block, const Names & bucket_columns, const Int64 & split_number, const ContextPtr & context)
{
    // use columnConst instead? Perf diff between int column with one value and column const
    // create split_number column of type ColumnConst since this column has one constant value in it
    auto split_number_column = ColumnInt64::create();
    split_number_column->insertValue(split_number);
    auto split_number_column_const = ColumnConst::create(std::move(split_number_column), 1);
    auto split_number_column_with_type = ColumnWithTypeAndName{std::move(split_number_column_const), std::make_shared<DataTypeInt64>(), "split_number"};

    // create split_value column
    auto split_value_column = ColumnInt64::create();
    auto split_value_column_with_type = ColumnWithTypeAndName{std::move(split_value_column), std::make_shared<DataTypeInt64>(), "split_value"};

    // insert new columns to block
    block.insert(split_number_column_with_type);
    block.insert(split_value_column_with_type);

    // generate arguments for DTSPartition. This is the index of the CLUSTER BY column and the split_value column in the block
    ColumnsWithTypeAndName args = { block.getByPosition(block.getPositionByName(bucket_columns[0])), block.getByPosition(block.columns() - 2) };

    // Create DTSPartition hasher and execute hashing on the block to populate split_value column
    auto dts_hasher = FunctionFactory::instance().get("dtspartition", context)->build({ block.getByName(bucket_columns[0]), split_number_column_with_type });
    auto col_to = dts_hasher->execute(args, {}, block.rows(), 0);
    block.getByPosition(block.columns() - 1).column = std::move(col_to);
}

// Util functions for prepareBucketColumn
ColumnPtr createColumnWithSipHash(Block & block, const Names & bucket_columns, const Int64 & divisor)
{
    auto result_column = ColumnUInt64::create();
    auto num_rows = block.rows();
    for (size_t i = 0; i < num_rows; i++)
    {
        SipHash hash;
        for (auto column_name : bucket_columns)
        {
            block.getByName(column_name).column->updateHashWithValue(i, hash);
        }
        result_column->insertValue(hash.get64() % divisor);
    }
    return result_column;
}

ColumnPtr createBucketNumberColumn(Block & block, const Int64 & split_number, const bool is_with_range, const Int64 total_shard_num)
{
    auto bucket_number_column = ColumnUInt64::create();
    auto split_value_column = block.getByPosition(block.columns() - 1).column;
    auto num_rows = block.rows();
    Int64 bucket_number = -1;
    Int64 shard_ratio = split_number / total_shard_num;
    shard_ratio = shard_ratio == 0 ? 1 : shard_ratio;
    for(size_t i = 0; i < num_rows; i++)
    {
        auto current_split_value = split_value_column->getInt(i);
        if (is_with_range)      /// TODO: Perform vectorization
        {
            // implicit floor for shard ratio.
            // split_number has no constraint to match user requirement, so a shard_ratio(0), when split_number < total_shard_num , is ok for customer.
            bucket_number = current_split_value / shard_ratio;
            bucket_number = bucket_number >= total_shard_num ? total_shard_num - 1 : bucket_number;
        }
        else
        {
            bucket_number = current_split_value % total_shard_num;
        }
        bucket_number_column->insertValue(bucket_number);
    }
    return bucket_number_column;
}

bool ReplacingConstantExpressionsMatcher::needChildVisit(ASTPtr &, const ASTPtr &)
{
    return true;
}

void ReplacingConstantExpressionsMatcher::visit(ASTPtr & node, Block & block_with_constants)
{
    if (!node->as<ASTFunction>())
        return;

    std::string name = node->getColumnName();
    if (block_with_constants.has(name))
    {
        auto result = block_with_constants.getByName(name);
        if (!isColumnConst(*result.column))
            return;

        node = std::make_shared<ASTLiteral>(assert_cast<const ColumnConst &>(*result.column).getField());
    }
}

void RewriteInQueryMatcher::Data::replaceExpressionListChildren(const ASTFunction * fn)
{
    if (auto * tuple_elements = fn->children.front()->as<ASTExpressionList>(); tuple_elements) {
        ASTPtr replacement_ast;
        if (ast_children_replacement.size() == 1)
        {
            replacement_ast = ast_children_replacement[0];
        }
        else
        {
            Tuple tuple;
            tuple.reserve(ast_children_replacement.size());
            for (const auto literal : ast_children_replacement)
                tuple.push_back(literal->as<ASTLiteral>()->value);
            replacement_ast = std::make_shared<ASTLiteral>(std::move(tuple));
        }
        tuple_elements->setOrReplaceAST(tuple_elements->children.back(), replacement_ast);
    }
}

bool RewriteInQueryMatcher::needChildVisit(ASTPtr & node, const ASTPtr & child)
{
    // parent node must be an ASTFunction of name "in"
    // Do not go into subqueries
    const auto * fn = node->as<ASTFunction>();
    if (!fn || fn->name != "in" || child->as<ASTSelectWithUnionQuery>())
        return false; // NOLINT
    return true;
}

void RewriteInQueryMatcher::visit(ASTPtr & node, Data & data)
{
    if (!node)
        return;

    const auto * fn = node->as<ASTFunction>();
    if (!fn || fn->name != "in")
        return;

    data.replaceExpressionListChildren(fn);
}

}
