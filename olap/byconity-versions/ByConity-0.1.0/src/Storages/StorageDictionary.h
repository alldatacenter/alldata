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

#include <atomic>
#include <common/shared_ptr_helper.h>

#include <Storages/IStorage.h>
#include <Interpreters/IExternalLoaderConfigRepository.h>


namespace DB
{
struct DictionaryStructure;
class TableFunctionDictionary;

class StorageDictionary final : public shared_ptr_helper<StorageDictionary>, public IStorage, public WithContext
{
    friend struct shared_ptr_helper<StorageDictionary>;
    friend class TableFunctionDictionary;
public:
    std::string getName() const override { return "Dictionary"; }

    ~StorageDictionary() override;

    void checkTableCanBeDropped() const override;
    void checkTableCanBeDetached() const override;

    Pipe read(
        const Names & column_names,
        const StorageMetadataPtr & /*metadata_snapshot*/,
        SelectQueryInfo & query_info,
        ContextPtr context,
        QueryProcessingStage::Enum processed_stage,
        size_t max_block_size,
        unsigned threads) override;

    static NamesAndTypesList getNamesAndTypes(const DictionaryStructure & dictionary_structure);
    static String generateNamesAndTypesDescription(const NamesAndTypesList & list);

    bool isDictionary() const override { return true; }
    void shutdown() override;
    void startup() override;

    void renameInMemory(const StorageID & new_table_id) override;

    Poco::Timestamp getUpdateTime() const;
    LoadablesConfigurationPtr getConfiguration() const;

    String getDictionaryName() const { return dictionary_name; }

    /// Specifies where the table is located relative to the dictionary.
    enum class Location
    {
        /// Table was created automatically as an element of a database with the Dictionary engine.
        DictionaryDatabase,

        /// Table was created automatically along with a dictionary
        /// and has the same database and name as the dictionary.
        /// It provides table-like access to the dictionary.
        /// User cannot drop that table.
        SameDatabaseAndNameAsDictionary,

        /// Table was created explicitly by a statement like
        /// CREATE TABLE ... ENGINE=Dictionary
        /// User chose the table's database and name and can drop that table.
        Custom,
    };

private:
    String dictionary_name;
    const Location location;

    mutable std::mutex dictionary_config_mutex;
    Poco::Timestamp update_time;
    LoadablesConfigurationPtr configuration;

    std::atomic<bool> remove_repository_callback_executed = false;
    scope_guard remove_repository_callback;

    void removeDictionaryConfigurationFromRepository();

    StorageDictionary(
        const StorageID & table_id_,
        const String & dictionary_name_,
        const ColumnsDescription & columns_,
        const String & comment,
        Location location_,
        ContextPtr context_);

    StorageDictionary(
        const StorageID & table_id_,
        const String & dictionary_name_,
        const DictionaryStructure & dictionary_structure,
        Location location_,
        ContextPtr context_);

    StorageDictionary(
        const StorageID & table_id_,
        LoadablesConfigurationPtr dictionary_configuration_,
        ContextPtr context_,
        bool is_cnch_dictionary);
};

}
