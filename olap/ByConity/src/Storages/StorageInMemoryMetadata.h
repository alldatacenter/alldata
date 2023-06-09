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

#pragma once

#include <DataTypes/DataTypeFactory.h>
#include <Parsers/IAST_fwd.h>
#include <Storages/ColumnDependency.h>
#include <Storages/ColumnsDescription.h>
#include <Storages/ConstraintsDescription.h>
#include <Storages/IndicesDescription.h>
#include <Storages/KeyDescription.h>
#include <Storages/ProjectionsDescription.h>
#include <Storages/SelectQueryDescription.h>
#include <Storages/TTLDescription.h>

#include <Common/MultiVersion.h>

namespace DB
{

/// Common metadata for all storages. Contains all possible parts of CREATE
/// query from all storages, but only some subset used.
struct StorageInMemoryMetadata
{
    /// Columns of table with their names, types,
    /// defaults, comments, etc. All table engines have columns.
    ColumnsDescription columns;
    /// Table indices. Currently supported for MergeTree only.
    IndicesDescription secondary_indices;
    /// Table constraints. Currently supported for MergeTree only.
    ConstraintsDescription constraints;
    /// Table projections. Currently supported for MergeTree only.
    ProjectionsDescription projections;
    mutable const ProjectionDescription * selected_projection{};
    /// PARTITION BY expression. Currently supported for MergeTree only.
    KeyDescription partition_key;
    /// CLUSTER BY expression. Currently supported for MergeTree only.
    KeyDescription cluster_by_key;
    /// PRIMARY KEY expression. If absent, than equal to order_by_ast.
    KeyDescription primary_key;
    /// ORDER BY expression. Required field for all MergeTree tables
    /// even in old syntax MergeTree(partition_key, order_by, ...)
    KeyDescription sorting_key;
    /// SAMPLE BY expression. Supported for MergeTree only.
    KeyDescription sampling_key;
    /// UNIQUE KEY expression. Supported for MergeTree only.
    KeyDescription unique_key;
    /// Separate ttl expressions for columns
    TTLColumnsDescription column_ttls_by_name;
    /// TTL expressions for table (Move and Rows)
    TTLTableDescription table_ttl;
    /// SETTINGS expression. Supported for MergeTree, Buffer and Kafka.
    ASTPtr settings_changes;
    /// SELECT QUERY. Supported for MaterializedView and View (have to support LiveView).
    SelectQueryDescription select;

    String comment;

    StorageInMemoryMetadata() = default;

    StorageInMemoryMetadata(const StorageInMemoryMetadata & other);
    StorageInMemoryMetadata & operator=(const StorageInMemoryMetadata & other);

    /// NOTE: Thread unsafe part. You should modify same StorageInMemoryMetadata
    /// structure from different threads. It should be used as MultiVersion
    /// object. See example in IStorage.

    /// Sets a user-defined comment for a table
    void setComment(const String & comment_);

    /// Sets only real columns, possibly overwrites virtual ones.
    void setColumns(ColumnsDescription columns_);

    /// Sets secondary indices
    void setSecondaryIndices(IndicesDescription secondary_indices_);

    /// Sets constraints
    void setConstraints(ConstraintsDescription constraints_);

    /// Sets projections
    void setProjections(ProjectionsDescription projections_);

    /// Set partition key for storage (methods below, are just wrappers for this struct).
    void setPartitionKey(const KeyDescription & partition_key_);
    /// Set sorting key for storage (methods below, are just wrappers for this struct).
    void setSortingKey(const KeyDescription & sorting_key_);
    /// Set primary key for storage (methods below, are just wrappers for this struct).
    void setPrimaryKey(const KeyDescription & primary_key_);
    /// Set sampling key for storage (methods below, are just wrappers for this struct).
    void setSamplingKey(const KeyDescription & sampling_key_);
    /// Set unique key for storage (methods below, are just wrappers for this struct).
    void setUniqueKey(const KeyDescription & unique_key_);

    /// Set common table TTLs
    void setTableTTLs(const TTLTableDescription & table_ttl_);

    /// TTLs for separate columns
    void setColumnTTLs(const TTLColumnsDescription & column_ttls_by_name_);

    /// Set settings changes in metadata (some settings exlicetely specified in
    /// CREATE query)
    void setSettingsChanges(const ASTPtr & settings_changes_);

    /// Set SELECT query for (Materialized)View
    void setSelectQuery(const SelectQueryDescription & select_);

    /// Returns combined set of columns
    const ColumnsDescription & getColumns() const;

    /// Returns secondary indices
    const IndicesDescription & getSecondaryIndices() const;

    /// Has at least one non primary index
    bool hasSecondaryIndices() const;

    /// Return table constraints
    const ConstraintsDescription & getConstraints() const;

    const ProjectionsDescription & getProjections() const;
    /// Has at least one projection
    bool hasProjections() const;

    /// Returns true if there is set table TTL, any column TTL or any move TTL.
    bool hasAnyTTL() const { return hasAnyColumnTTL() || hasAnyTableTTL(); }

    /// Common tables TTLs (for rows and moves).
    TTLTableDescription getTableTTLs() const;
    bool hasAnyTableTTL() const;

    /// Separate TTLs for columns.
    TTLColumnsDescription getColumnTTLs() const;
    bool hasAnyColumnTTL() const;

    /// Just wrapper for table TTLs, return rows part of table TTLs.
    TTLDescription getRowsTTL() const;
    bool hasRowsTTL() const;

    bool hasPartitionLevelTTL() const;

    TTLDescriptions getRowsWhereTTLs() const;
    bool hasAnyRowsWhereTTL() const;

    /// Just wrapper for table TTLs, return moves (to disks or volumes) parts of
    /// table TTL.
    TTLDescriptions getMoveTTLs() const;
    bool hasAnyMoveTTL() const;

    // Just wrapper for table TTLs, return info about recompression ttl
    TTLDescriptions getRecompressionTTLs() const;
    bool hasAnyRecompressionTTL() const;

    // Just wrapper for table TTLs, return info about recompression ttl
    TTLDescriptions getGroupByTTLs() const;
    bool hasAnyGroupByTTL() const;

    /// Returns columns, which will be needed to calculate dependencies (skip
    /// indices, TTL expressions) if we update @updated_columns set of columns.
    ColumnDependencies getColumnDependencies(const NameSet & updated_columns) const;

    /// Block with ordinary + materialized columns + functional columns(if include_func_columns is true).
    Block getSampleBlock(bool include_func_columns = false) const;

    /// Block with ordinary columns + functional columns(if include_func_columns is true).
    Block getSampleBlockNonMaterialized(bool include_func_columns = false) const;

    /// Block with ordinary + materialized + virtuals. Virtuals have to be
    /// explicitly specified, because they are part of Storage type, not
    /// Storage metadata.
    Block getSampleBlockWithVirtuals(const NamesAndTypesList & virtuals) const;


    /// Unique table reserved names
    static constexpr auto DELETE_FLAG_COLUMN_NAME = "_delete_flag_";

    /// Functional columns can not be specified by create query and can not be queried, but can be contained in insert query, including INSERT and INSERT SELECT operations.
    NamesAndTypesList getFuncColumns() const
    {
        if (hasUniqueKey())
        {
            NamesAndTypesList res;
            /// When the user specifies this column in the "insert query", it will be considered as a delete operation based on the unique key if the value of this column is true(not zero).
            res.emplace_back(DELETE_FLAG_COLUMN_NAME, DataTypeFactory::instance().get("UInt8"));
            return res;
        }
        else
        {
            return {};
        }
    }

    /// Block with ordinary + materialized + aliases + virtuals. Virtuals have
    /// to be explicitly specified, because they are part of Storage type, not
    /// Storage metadata. StorageID required only for more clear exception
    /// message.
    Block getSampleBlockForColumns(
        const Names & column_names, const NamesAndTypesList & virtuals = {}, const StorageID & storage_id = StorageID::createEmpty()) const;

    /// Returns structure with partition key.
    const KeyDescription & getPartitionKey() const;
    /// Returns ASTExpressionList of partition key expression for storage or nullptr if there is none.
    ASTPtr getPartitionKeyAST() const { return partition_key.definition_ast; }
    /// Storage has user-defined (in CREATE query) partition key.
    bool isPartitionKeyDefined() const;
    /// Storage has partition key.
    bool hasPartitionKey() const;
    /// Returns column names that need to be read to calculate partition key.
    Names getColumnsRequiredForPartitionKey() const;

    /// Returns structure with cluster by key.
    const KeyDescription & getClusterByKey() const;
    /// Returns ASTExpressionList of cluster by key expression for storage or nullptr if there is none.
    ASTPtr getClusterByKeyAST() const { return cluster_by_key.definition_ast; }
    /// Storage has user-defined (in CREATE query) cluster by key.
    bool isClusterByKeyDefined() const;
    /// Storage has cluster by key.
    bool hasClusterByKey() const;
    /// Returns column names that need to be read to calculate cluster by key.
    Names getColumnsForClusterByKey() const;
    Int64 getBucketNumberFromClusterByKey() const;
    Int64 getSplitNumberFromClusterByKey() const;
    bool getWithRangeFromClusterByKey() const;

    /// Returns structure with sorting key.
    const KeyDescription & getSortingKey() const;
    /// Returns ASTExpressionList of sorting key expression for storage or nullptr if there is none.
    ASTPtr getSortingKeyAST() const { return sorting_key.definition_ast; }
    /// Storage has user-defined (in CREATE query) sorting key.
    bool isSortingKeyDefined() const;
    /// Storage has sorting key. It means, that it contains at least one column.
    bool hasSortingKey() const;
    /// Returns column names that need to be read to calculate sorting key.
    Names getColumnsRequiredForSortingKey() const;
    /// Returns columns names in sorting key specified by user in ORDER BY
    /// expression. For example: 'a', 'x * y', 'toStartOfMonth(date)', etc.
    Names getSortingKeyColumns() const;

    /// Returns column names that need to be read for FINAL to work.
    Names getColumnsRequiredForFinal() const { return getColumnsRequiredForSortingKey(); }

    /// Returns structure with sampling key.
    const KeyDescription & getSamplingKey() const;
    /// Returns sampling expression AST for storage or nullptr if there is none.
    ASTPtr getSamplingKeyAST() const { return sampling_key.definition_ast; }
    /// Storage has user-defined (in CREATE query) sampling key.
    bool isSamplingKeyDefined() const;
    /// Storage has sampling key.
    bool hasSamplingKey() const;
    /// Returns column names that need to be read to calculate sampling key.
    Names getColumnsRequiredForSampling() const;

    /// Returns structure with primary key.
    const KeyDescription & getPrimaryKey() const;
    /// Returns ASTExpressionList of primary key expression for storage or nullptr if there is none.
    ASTPtr getPrimaryKeyAST() const { return primary_key.definition_ast; }
    /// Storage has user-defined (in CREATE query) sorting key.
    bool isPrimaryKeyDefined() const;
    /// Storage has primary key (maybe part of some other key). It means, that
    /// it contains at least one column.
    bool hasPrimaryKey() const;
    /// Returns column names that need to be read to calculate primary key.
    Names getColumnsRequiredForPrimaryKey() const;
    /// Returns columns names in sorting key specified by. For example: 'a', 'x
    /// * y', 'toStartOfMonth(date)', etc.
    Names getPrimaryKeyColumns() const;

    /// Returns structure with unique key.
    const KeyDescription & getUniqueKey() const;
    /// Returns ASTExpressionList of unique key expression for storage or nullptr if there is none.
    ASTPtr getUniqueKeyAST() const { return primary_key.definition_ast; }
    /// Storage has user-defined (in CREATE query) sorting key.
    bool isUniqueKeyDefined() const;
    /// Storage has unique key (maybe part of some other key). It means, that
    /// it contains at least one column.
    bool hasUniqueKey() const;
    /// Returns column names that need to be read to calculate unique key.
    Names getColumnsRequiredForUniqueKey() const;
    /// Returns columns names in unique key specified by. For example: 'a', 'x
    /// * y', 'toStartOfMonth(date)', etc.
    Names getUniqueKeyColumns() const;
    /// Returns the unique key expression
    ExpressionActionsPtr getUniqueKeyExpression() const;

    /// Storage settings
    ASTPtr getSettingsChanges() const;
    bool hasSettingsChanges() const { return settings_changes != nullptr; }

    /// Select query for *View storages.
    const SelectQueryDescription & getSelectQuery() const;
    bool hasSelectQuery() const;

    /// Verify that all the requested names are in the table and are set correctly:
    /// list of names is not empty and the names do not repeat.
    void check(const Names & column_names, const NamesAndTypesList & virtuals, const StorageID & storage_id) const;

    /// Check that all the requested names are in the table and have the correct types.
    void check(const NamesAndTypesList & columns) const;

    /// Check that all names from the intersection of `names` and `columns` are in the table and have the same types.
    void check(const NamesAndTypesList & columns, const Names & column_names) const;

    /// Check that the data block contains all the columns of the table with the correct types,
    /// contains only the columns of the table, and all the columns are different.
    /// If |need_all| is set, then checks that all the columns of the table are in the block.
    void check(const Block & block, bool need_all = false) const;

    /// check if there exist map column
    bool hasMapColumn() const;
};

using StorageMetadataPtr = std::shared_ptr<const StorageInMemoryMetadata>;
using MultiVersionStorageMetadataPtr = MultiVersion<StorageInMemoryMetadata>;

}
