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

#pragma once
#include <Core/QueryProcessingStage.h>
#include <QueryPlan/ISourceStep.h>
#include <Storages/IStorage.h>
#include <Storages/SelectQueryInfo.h>

namespace DB
{

class TableScanStep : public ISourceStep
{
public:
    TableScanStep(
        ContextPtr context,
        StorageID storage_id_,
        const NamesWithAliases & column_alias_,
        const SelectQueryInfo & query_info_,
        QueryProcessingStage::Enum processing_stage_,
        size_t max_block_size_);

    TableScanStep(
        DataStream output,
        StoragePtr storage_,
        StorageID storage_id_,
        String original_table_,
        Names column_names_,
        NamesWithAliases column_alias_,
        SelectQueryInfo query_info_,
        QueryProcessingStage::Enum processing_stage_,
        size_t max_block_size_)
        : ISourceStep(std::move(output))
        , storage(storage_)
        , storage_id(storage_id_)
        , original_table(std::move(original_table_))
        , column_names(std::move(column_names_))
        , column_alias(std::move(column_alias_))
        , query_info(std::move(query_info_))
        , processing_stage(processing_stage_)
        , max_block_size(max_block_size_)
        , log(&Poco::Logger::get("TableScanStep"))
    {
    }

    String getName() const override { return "TableScan"; }
    Type getType() const override { return Type::TableScan; }

    void initializePipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &) override;
    void serialize(WriteBuffer & buffer) const override;
    static QueryPlanStepPtr deserialize(ReadBuffer & buf, ContextPtr context);

    const String & getDatabase() const { return storage_id.database_name; }
    const String & getTable() const { return storage_id.table_name; }
//    void setTable(const String & table_);
    void setOriginalTable(const String & original_table_) { original_table = original_table_; }
    const Names & getColumnNames() const { return column_names; }
    const NamesWithAliases & getColumnAlias() const { return column_alias; }
    QueryProcessingStage::Enum getProcessedStage() const;
    size_t getMaxBlockSize() const;
    bool setFilter(const std::vector<ConstASTPtr> & filters) const;
    bool hasFilter() const;
    bool setLimit(size_t limit, const ContextMutablePtr & context);
    bool hasLimit() const;

    void optimizeWhereIntoPrewhre(ContextPtr context);

    SelectQueryInfo fillQueryInfo(ContextPtr context);
    void fillPrewhereInfo(ContextPtr context);
    void makeSetsForIndex(const ASTPtr & node, ContextPtr context, PreparedSets & prepared_sets) const;

    void allocate(ContextPtr context);
    Int32 getUniqueId() const { return unique_id; }
    void setUniqueId(Int32 unique_id_) { unique_id = unique_id_; }

    std::shared_ptr<IStorage> getStorage() const;
    const SelectQueryInfo & getQueryInfo() const { return query_info; }
    const StorageID & getStorageID() const { return storage_id; }
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr context) const override;

private:
    StoragePtr storage;
    StorageID storage_id;
    String original_table;
    Names column_names;
    NamesWithAliases column_alias;
    SelectQueryInfo query_info;
    QueryProcessingStage::Enum processing_stage;
    size_t max_block_size;

    // just for cascades, in order to distinguish between the same tables.
    Int32 unique_id{0};
    Poco::Logger * log;

    // Optimises the where clauses for a bucket table by rewriting the IN clause and hence reducing the IN set size
    void rewriteInForBucketTable(ContextPtr context) const;
    static ASTPtr rewriteDynamicFilter(const ASTPtr & filter, QueryPipeline & pipeline, const BuildQueryPipelineSettings & build_context);
};

}
