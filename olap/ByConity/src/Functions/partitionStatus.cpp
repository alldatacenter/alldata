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
#include <Functions/IFunction.h>
#include <Functions/FunctionFactory.h>
#include <DataTypes/DataTypeString.h>
#include <Interpreters/Context.h>
#include <Storages/StorageMergeTree.h>
#include <Storages/StorageMaterializedView.h>
#include <Storages/MergeTree/MergeTreeData.h>


namespace DB
{
    namespace ErrorCodes
    {
        extern const int ILLEGAL_COLUMN;
        extern const int LOGICAL_ERROR;
    }

    class FunctionPartitionStatus : public IFunction
    {
    public:
        static constexpr auto name = "partitionStatus";

        FunctionPartitionStatus(const ContextPtr & context_) : context(context_) {}

        static FunctionPtr create(const ContextPtr & context)
        {
            return std::make_shared<FunctionPartitionStatus>(context);
        }

        String getName() const override
        {
            return name;
        }

        bool isDeterministic() const override { return false; }

        bool isDeterministicInScopeOfQuery() const override
        {
            return false;
        }

        size_t getNumberOfArguments() const override
        {
            return 3;
        }

        DataTypePtr getReturnTypeImpl(const DataTypes & /*arguments*/) const override
        {
            return std::make_shared<DataTypeString>();
        }

        ColumnNumbers getArgumentsThatAreAlwaysConstant() const override { return {0, 1, 2}; }

        inline String getColumnStringValue(const ColumnWithTypeAndName & argument) const { return argument.column->getDataAt(0).toString(); }

        ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr &, size_t input_rows_count) const override
        {
            String database_name = getColumnStringValue(arguments[0]);
            String table_name = getColumnStringValue(arguments[1]);
            String partition_id = getColumnStringValue(arguments[2]);
            if (database_name.empty() || table_name.empty() || partition_id.empty())
                throw Exception("Bad arguments: database/table/partition_id should not be empty", ErrorCodes::BAD_ARGUMENTS);

            auto table = DatabaseCatalog::instance().getTable({database_name, table_name}, context->getQueryContext());
            auto status = getPartitionStatus(table, partition_id);

            auto result_column = ColumnString::create();
            for (size_t j = 0; j < input_rows_count; ++j)
                result_column->insert(status);
            return result_column;
        }

    private:
        ContextPtr context;

        String getPartitionStatus(StoragePtr & storage, const String & partition_id) const
        {
            if (auto * merge_tree = dynamic_cast<StorageMergeTree *>(storage.get()))
            {
                auto parts = merge_tree->getDataPartsVectorInPartition(MergeTreeDataPartState::Committed, partition_id);
                return parts.empty() ? "None" : "Normal";
            }
            else if (auto * merge_tree_data = dynamic_cast<MergeTreeData *>(storage.get()))
            {
                auto parts = merge_tree_data->getDataPartsVectorInPartition(MergeTreeDataPartState::Committed, partition_id);
                return parts.empty() ? "None" : "Normal";
            }
            else if (auto * materialized_view = dynamic_cast<StorageMaterializedView *>(storage.get()))
            {
                auto target_table = materialized_view->getTargetTable();
                auto status = getPartitionStatus(target_table, partition_id);

                if (materialized_view->isRefreshing())
                    return materialized_view->getRefreshingPartition() == partition_id ? "Refreshing" : status;

                return status;
            }
            else
                throw Exception("Unknown engine: " + storage->getName(), ErrorCodes::LOGICAL_ERROR);
        }
    };

    void registerFunctionPartitionStatus(FunctionFactory & factory)
    {
        factory.registerFunction<FunctionPartitionStatus>();
    }
}
