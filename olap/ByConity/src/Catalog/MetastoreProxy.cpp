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

#include <Catalog/MetastoreProxy.h>
#include <Protos/DataModelHelpers.h>
#include <cstddef>
#include <random>
#include <sstream>
#include <vector>
#include <string.h>
#include <DaemonManager/BGJobStatusInCatalog.h>
#include <IO/ReadHelpers.h>

namespace DB::ErrorCodes
{
extern const int METASTORE_DB_UUID_CAS_ERROR;
extern const int METASTORE_TABLE_UUID_CAS_ERROR;
extern const int METASTORE_TABLE_NAME_CAS_ERROR;
extern const int METASTORE_ROOT_PATH_ALREADY_EXISTS;
extern const int METASTORE_ROOT_PATH_ID_NOT_UNIQUE;
extern const int METASTORE_CLEAR_INTENT_CAS_FAILURE;
extern const int VIRTUAL_WAREHOUSE_NOT_FOUND;
extern const int FUNCTION_ALREADY_EXISTS;
extern const int METASTORE_COMMIT_CAS_FAILURE;
}

namespace DB::Catalog
{

void MetastoreProxy::updateServerWorkerGroup(const DB::String & worker_group_name, const DB::String & worker_group_info)
{
    metastore_ptr->put(WORKER_GROUP_STORE_PREFIX + worker_group_name, worker_group_info);
}

void MetastoreProxy::getServerWorkerGroup(const String & worker_group_name, DB::String & worker_group_info)
{
    metastore_ptr->get(WORKER_GROUP_STORE_PREFIX + worker_group_name, worker_group_info);
}

void MetastoreProxy::dropServerWorkerGroup(const DB::String & worker_group_name)
{
    metastore_ptr->drop(WORKER_GROUP_STORE_PREFIX + worker_group_name);
}

IMetaStore::IteratorPtr MetastoreProxy::getAllWorkerGroupMeta()
{
    return metastore_ptr->getByPrefix(WORKER_GROUP_STORE_PREFIX);
}

void MetastoreProxy::addDatabase(const String & name_space, const Protos::DataModelDB & db_model)
{
    String db_meta;
    db_model.SerializeToString(&db_meta);

    BatchCommitRequest batch_write;
    if (db_model.has_uuid())
        batch_write.AddPut(SinglePutRequest(dbUUIDUniqueKey(name_space, UUIDHelpers::UUIDToString(RPCHelpers::createUUID(db_model.uuid()))), "", true));
    batch_write.AddPut(SinglePutRequest(dbKey(name_space, db_model.name(), db_model.commit_time()), db_meta));

    BatchCommitResponse resp;
    try
    {
        metastore_ptr->batchWrite(batch_write, resp);
    }
    catch (Exception & e)
    {
        if (e.code() == ErrorCodes::METASTORE_COMMIT_CAS_FAILURE)
        {
             /// check if db uuid has conflict with current metainfo
            if (resp.puts.count(0))
                throw Exception(
                        "Database with the same uuid(" + UUIDHelpers::UUIDToString(RPCHelpers::createUUID(db_model.uuid())) + ") already exists in catalog. Please use another uuid or "
                        "clear old version of this database and try again.",
                        ErrorCodes::METASTORE_DB_UUID_CAS_ERROR);
        }
        else
            throw e;
    }
}

void MetastoreProxy::getDatabase(const DB::String & name_space, const DB::String & name, DB::Strings & db_info)
{
    auto it = metastore_ptr->getByPrefix(dbKeyPrefix(name_space, name));
    while(it->next())
    {
        db_info.emplace_back(it->value());
    }
}

IMetaStore::IteratorPtr MetastoreProxy::getAllDatabaseMeta(const DB::String & name_space)
{
    return metastore_ptr->getByPrefix(allDbPrefix(name_space));
}

std::vector<Protos::DataModelDB> MetastoreProxy::getTrashDBs(const String & name_space)
{
    std::vector<Protos::DataModelDB> res;
    auto it = metastore_ptr->getByPrefix(dbTrashPrefix(name_space));
    while(it->next())
    {
        Protos::DataModelDB db_model;
        db_model.ParseFromString(it->value());
        res.emplace_back(db_model);
    }
    return res;
}

std::vector<UInt64> MetastoreProxy::getTrashDBVersions(const String & name_space, const String & database)
{
    std::vector<UInt64> res;
    auto it = metastore_ptr->getByPrefix(dbTrashPrefix(name_space) + escapeString(database));
    while(it->next())
    {
        const auto & key = it->key();
        auto pos = key.find_last_of('_');
        UInt64 drop_ts = std::stoull(key.substr(pos + 1, String::npos), nullptr);
        res.emplace_back(drop_ts);
    }
    return res;
}

void MetastoreProxy::dropDatabase(const String & name_space, const Protos::DataModelDB & db_model)
{
    String name = db_model.name();
    UInt64 ts = db_model.commit_time();

    BatchCommitRequest batch_write;

    /// get all trashed dictionaries of current db and remove them with db metadata
    auto dic_ptrs = getDictionariesFromTrash(name_space, name + "_" + toString(ts));
    for (auto & dic_ptr : dic_ptrs)
        batch_write.AddDelete(SingleDeleteRequest(dictionaryTrashKey(name_space, dic_ptr->database(), dic_ptr->name())));

    batch_write.AddDelete(SingleDeleteRequest(dbKey(name_space, name, ts)));
    batch_write.AddDelete(SingleDeleteRequest(dbTrashKey(name_space, name, ts)));
    if (db_model.has_uuid())
        batch_write.AddDelete(SingleDeleteRequest(dbUUIDUniqueKey(name_space, UUIDHelpers::UUIDToString(RPCHelpers::createUUID(db_model.uuid())))));

    BatchCommitResponse resp;
    metastore_ptr->batchWrite(batch_write, resp);
}

String MetastoreProxy::getTableUUID(const String & name_space, const String & database, const String & name)
{
    String identifier_meta;
    metastore_ptr->get(tableUUIDMappingKey(name_space, database, name), identifier_meta);
    if (identifier_meta.empty())
        return "";
    Protos::TableIdentifier identifier;
    identifier.ParseFromString(identifier_meta);
    return identifier.uuid();
}

void MetastoreProxy::dropMaskingPolicies(const String & name_space, const Strings & names)
{
    if (names.empty())
        return;

    Strings keys;
    keys.reserve(names.size());
    std::transform(names.begin(), names.end(), std::back_inserter(keys),
        [&name_space](const auto & name) {
            return maskingPolicyKey(name_space, name);
        }
    );

    multiDrop(keys);
}

std::shared_ptr<Protos::TableIdentifier> MetastoreProxy::getTableID(const String & name_space, const String & database, const String & name)
{
    String identifier_meta;
    metastore_ptr->get(tableUUIDMappingKey(name_space, database, name), identifier_meta);
    std::shared_ptr<Protos::TableIdentifier> res = nullptr;
    if (!identifier_meta.empty())
    {
        res = std::make_shared<Protos::TableIdentifier>();
        res->ParseFromString(identifier_meta);
    }
    return res;
}

String MetastoreProxy::getTrashTableUUID(const String & name_space, const String & database, const String & name, const UInt64 & ts)
{
    String identifier_meta;
    metastore_ptr->get(tableTrashKey(name_space, database, name, ts), identifier_meta);
    if (identifier_meta.empty())
        return "";
    Protos::TableIdentifier identifier;
    identifier.ParseFromString(identifier_meta);
    return identifier.uuid();
}

void MetastoreProxy::createTable(const String & name_space, const DB::Protos::DataModelTable & table_data, const Strings & dependencies, const Strings & masking_policy_mapping)
{
    const String & database = table_data.database();
    const String & name = table_data.name();
    const String & uuid = UUIDHelpers::UUIDToString(RPCHelpers::createUUID(table_data.uuid()));
    String serialized_meta;
    table_data.SerializeToString(&serialized_meta);

    BatchCommitRequest batch_write;
    batch_write.AddPut(SinglePutRequest(nonHostUpdateKey(name_space, uuid), "0", true));
    // insert table meta
    batch_write.AddPut(SinglePutRequest(tableStoreKey(name_space, uuid, table_data.commit_time()), serialized_meta, true));
    /// add dependency mapping if need
    for (const String & dependency : dependencies)
        batch_write.AddPut(SinglePutRequest(viewDependencyKey(name_space, dependency, uuid), uuid));

    for (const String & mask : masking_policy_mapping)
        batch_write.AddPut(SinglePutRequest(maskingPolicyTableMappingKey(name_space, mask, uuid), uuid));

    /// add `table name` ->`uuid` mapping
    Protos::TableIdentifier identifier;
    identifier.set_database(database);
    identifier.set_name(name);
    identifier.set_uuid(uuid);
    batch_write.AddPut(SinglePutRequest(tableUUIDMappingKey(name_space, database, name), identifier.SerializeAsString(), true));
    batch_write.AddPut(SinglePutRequest(tableUUIDUniqueKey(name_space, uuid), "", true));

    BatchCommitResponse resp;
    try
    {
        metastore_ptr->batchWrite(batch_write, resp);
    }
    catch (Exception & e)
    {
        if (e.code() == ErrorCodes::METASTORE_COMMIT_CAS_FAILURE)
        {
            auto putssize = batch_write.puts.size();
            if (resp.puts.count(putssize-1))
            {
                throw Exception(
                    "Table with the same uuid already exists in catalog. Please use another uuid or "
                    "clear old version of this table and try again.",
                    ErrorCodes::METASTORE_TABLE_UUID_CAS_ERROR);
            }
            else if (resp.puts.count(putssize-2))
            {
                throw Exception(
                    "Table with the same name already exists in catalog. Please use another name and try again.",
                    ErrorCodes::METASTORE_TABLE_NAME_CAS_ERROR);
            }
        }
        throw e;
    }
}

void MetastoreProxy::createUDF(const String & name_space, const DB::Protos::DataModelUDF & udf_data)
{
    const String & database = udf_data.database();
    const String & name = udf_data.function_name();
    String serialized_meta;
    udf_data.SerializeToString(&serialized_meta);

    try
    {
        metastore_ptr->put(udfStoreKey(name_space, database, name), serialized_meta, true);
    }
    catch (Exception & e)
    {
        if (e.code() == ErrorCodes::METASTORE_COMMIT_CAS_FAILURE) {
            throw Exception("UDF with function name - " + name + " in database - " +  database + " already exists.", ErrorCodes::FUNCTION_ALREADY_EXISTS);
        }
        throw e;
    }
}

void MetastoreProxy::dropUDF(const String & name_space, const String &db_name, const String &function_name)
{
    metastore_ptr->drop(udfStoreKey(name_space, db_name, function_name));
}

void MetastoreProxy::updateTable(const String & name_space, const String & table_uuid, const String & table_info_new, const UInt64 & ts)
{
    metastore_ptr->put(tableStoreKey(name_space, table_uuid, ts), table_info_new);
}

void MetastoreProxy::getTableByUUID(const String & name_space, const String & table_uuid, Strings & tables_info)
{
    auto it = metastore_ptr->getByPrefix(tableStorePrefix(name_space, table_uuid));
    while(it->next())
    {
        tables_info.emplace_back(it->value());
    }
}

IMetaStore::IteratorPtr MetastoreProxy::getAllTablesMeta(const DB::String &name_space)
{
    return metastore_ptr->getByPrefix(tableMetaPrefix(name_space));
}

IMetaStore::IteratorPtr MetastoreProxy::getAllUDFsMeta(const DB::String &name_space, const DB::String & database_name)
{
    return metastore_ptr->getByPrefix(udfStoreKey(name_space, database_name));
}

Strings MetastoreProxy::getUDFsMetaByName(const String & name_space, const std::unordered_set<String> &function_names)
{
    Strings keys;
    for (const auto & function_name: function_names)
        keys.push_back(udfStoreKey(name_space, function_name));

    Strings udf_info;
    auto values = metastore_ptr->multiGet(keys);
    for (const auto & ele : values)
        udf_info.emplace_back(std::move(ele.first));
    return udf_info;
}

std::vector<std::shared_ptr<Protos::TableIdentifier>> MetastoreProxy::getAllTablesId(const String & name_space, const String & db)
{
    std::vector<std::shared_ptr<Protos::TableIdentifier>> res;
    auto it = metastore_ptr->getByPrefix(tableUUIDMappingPrefix(name_space, db));
    while(it->next())
    {
        std::shared_ptr<Protos::TableIdentifier> identifier_ptr(new Protos::TableIdentifier());
        identifier_ptr->ParseFromString(it->value());
        res.push_back(identifier_ptr);
    }
    return res;
}

Strings MetastoreProxy::getAllDependence(const String & name_space, const String & uuid)
{
    Strings res;
    auto it = metastore_ptr->getByPrefix(viewDependencyPrefix(name_space, uuid));

    while(it->next())
        res.push_back(it->value());

    return res;
}


IMetaStore::IteratorPtr MetastoreProxy::getTrashTableIDIterator(const String & name_space, uint32_t iterator_internal_batch_size)
{
    return metastore_ptr->getByPrefix(tableTrashPrefix(name_space), 0, iterator_internal_batch_size);
}

std::vector<std::shared_ptr<Protos::TableIdentifier>> MetastoreProxy::getTrashTableID(const String & name_space)
{
    std::vector<std::shared_ptr<Protos::TableIdentifier>> res;
    auto it = metastore_ptr->getByPrefix(tableTrashPrefix(name_space));
    while(it->next())
    {
        std::shared_ptr<Protos::TableIdentifier> identifier_ptr(new Protos::TableIdentifier());
        identifier_ptr->ParseFromString(it->value());
        res.push_back(identifier_ptr);
    }
    return res;
}

void MetastoreProxy::createDictionary(const String & name_space, const String & db, const String & name, const String & dic_meta)
{
    metastore_ptr->put(dictionaryStoreKey(name_space, db, name), dic_meta);
}

void MetastoreProxy::getDictionary(const String & name_space, const String & db, const String & name, String & dic_meta)
{
    metastore_ptr->get(dictionaryStoreKey(name_space, db, name), dic_meta);
}

void MetastoreProxy::dropDictionary(const String & name_space, const String & db, const String & name)
{
    metastore_ptr->drop(dictionaryStoreKey(name_space, db, name));
}

std::vector<std::shared_ptr<Protos::DataModelDictionary>> MetastoreProxy::getDictionariesInDB(const String & name_space, const String & database)
{
    std::vector<std::shared_ptr<Protos::DataModelDictionary>> res;
    auto it = metastore_ptr->getByPrefix(dictionaryPrefix(name_space, database));
    while(it->next())
    {
        std::shared_ptr<Protos::DataModelDictionary> dic_ptr(new Protos::DataModelDictionary());
        dic_ptr->ParseFromString(it->value());
        res.push_back(dic_ptr);
    }
    return res;
}

std::shared_ptr<Protos::TableIdentifier> MetastoreProxy::getTrashTableID(const String & name_space, const String & database, const String & table, const UInt64 & ts)
{
    std::shared_ptr<Protos::TableIdentifier> res = nullptr;
    String meta;
    metastore_ptr->get(tableTrashKey(name_space, database, table, ts), meta);
    if (!meta.empty())
    {
        res = std::make_shared<Protos::TableIdentifier>();
        res->ParseFromString(meta);
    }
    return res;
}

std::vector<std::shared_ptr<Protos::TableIdentifier>> MetastoreProxy::getTablesFromTrash(const String & name_space, const String & database)
{
    std::vector<std::shared_ptr<Protos::TableIdentifier>> res;
    auto it = metastore_ptr->getByPrefix(tableTrashPrefix(name_space, database));
    while(it->next())
    {
        std::shared_ptr<Protos::TableIdentifier> identifier_ptr(new Protos::TableIdentifier());
        identifier_ptr->ParseFromString(it->value());
        res.push_back(identifier_ptr);
    }
    return res;
}

std::unordered_map<String, UInt64> MetastoreProxy::getTrashTableVersions(const String & name_space, const String & database, const String & table)
{
    std::unordered_map<String, UInt64> res;
    auto it = metastore_ptr->getByPrefix(tableTrashPrefix(name_space) + escapeString(database) + "_" + escapeString(table));
    while(it->next())
    {
        const auto & key = it->key();
        auto pos = key.find_last_of('_');
        UInt64 drop_ts = std::stoull(key.substr(pos + 1, String::npos), nullptr);
        Protos::TableIdentifier data_model;
        data_model.ParseFromString(it->value());
        res.emplace(data_model.uuid(), drop_ts);
    }
    return res;
}

IMetaStore::IteratorPtr MetastoreProxy::getAllDictionaryMeta(const DB::String & name_space)
{
    return metastore_ptr->getByPrefix(allDictionaryPrefix(name_space));
}

std::vector<std::shared_ptr<DB::Protos::DataModelDictionary>> MetastoreProxy::getDictionariesFromTrash(const String & name_space, const String & database)
{
    std::vector<std::shared_ptr<Protos::DataModelDictionary>> res;
    auto it = metastore_ptr->getByPrefix(dictionaryTrashPrefix(name_space, database));
    while(it->next())
    {
        std::shared_ptr<Protos::DataModelDictionary> dic_ptr(new Protos::DataModelDictionary());
        dic_ptr->ParseFromString(it->value());
        res.push_back(dic_ptr);
    }
    return res;
}

void MetastoreProxy::clearTableMeta(const String & name_space, const String & database, const String & table, const String & uuid, const Strings & dependencies, const UInt64 & ts)
{
    /// uuid should not be empty
    if (uuid.empty())
        return;

    BatchCommitRequest batch_write;

    /// remove all versions of table meta info.
    auto it_t = metastore_ptr->getByPrefix(tableStorePrefix(name_space, uuid));
    while(it_t->next())
    {
        batch_write.AddDelete(SingleDeleteRequest(it_t->key()));
    }

    /// remove table partition list;
    String partition_list_prefix = tablePartitionInfoPrefix(name_space, uuid);
    auto it_p = metastore_ptr->getByPrefix(partition_list_prefix);
    while(it_p->next())
    {
        batch_write.AddDelete(SingleDeleteRequest(it_p->key()));
    }
    /// remove dependency
    for (const String & dependency : dependencies)
        batch_write.AddDelete(SingleDeleteRequest(viewDependencyKey(name_space, dependency, uuid)));

    /// remove trash record if the table marked as deleted before be cleared
    batch_write.AddDelete(SingleDeleteRequest(tableTrashKey(name_space, database, table, ts)));

    /// remove MergeMutateThread meta
    batch_write.AddDelete(SingleDeleteRequest(mergeMutateThreadStartTimeKey(name_space, uuid)));
    /// remove table uuid unique key
    batch_write.AddDelete(SingleDeleteRequest(tableUUIDUniqueKey(name_space, uuid)));
    batch_write.AddDelete(SingleDeleteRequest(nonHostUpdateKey(name_space, uuid)));

    /// remove all statistics
    auto table_statistics_prefix = tableStatisticPrefix(name_space, uuid);
    for (auto it = metastore_ptr->getByPrefix(table_statistics_prefix); it->next(); )
    {
        batch_write.AddDelete(SingleDeleteRequest(it->key()));
    }
    auto table_statistics_tag_prefix = tableStatisticTagPrefix(name_space, uuid);
    for (auto it = metastore_ptr->getByPrefix(table_statistics_tag_prefix); it->next(); )
    {
        batch_write.AddDelete(SingleDeleteRequest(it->key()));
    }
    auto column_statistics_prefix = columnStatisticPrefix(name_space, uuid);
    for (auto it = metastore_ptr->getByPrefix(column_statistics_prefix); it->next(); )
    {
        batch_write.AddDelete(SingleDeleteRequest(it->key()));
    }
    auto column_statistics_tag_prefix = columnStatisticTagPrefixWithoutColumn(name_space, uuid);
    for (auto it = metastore_ptr->getByPrefix(column_statistics_tag_prefix); it->next(); )
    {
        batch_write.AddDelete(SingleDeleteRequest(it->key()));
    }

    BatchCommitResponse resp;
    metastore_ptr->batchWrite(batch_write, resp);
}

String MetastoreProxy::getMaskingPolicy(const String & name_space, const String & masking_policy_name) const
{
    String data;
    metastore_ptr->get(maskingPolicyKey(name_space, masking_policy_name), data);
    return data;
}

Strings MetastoreProxy::getMaskingPolicies(const String & name_space, const Strings & masking_policy_names) const
{
    if (masking_policy_names.empty())
        return {};

    Strings keys;
    keys.reserve(masking_policy_names.size());
    for (const auto & name : masking_policy_names)
        keys.push_back(maskingPolicyKey(name_space, name));

    Strings res;
    auto values = metastore_ptr->multiGet(keys);
    for (auto & ele : values)
        res.push_back(std::move(ele.first));

    return res;
}

Strings MetastoreProxy::getAllMaskingPolicy(const String & name_space) const
{
    Strings models;
    auto it = metastore_ptr->getByPrefix(maskingPolicyMetaPrefix(name_space));
    models.reserve(models.size());
    while (it->next())
    {
        models.push_back(it->value());
    }

    return models;
}

void MetastoreProxy::putMaskingPolicy(const String & name_space, const Protos::DataModelMaskingPolicy & new_masking_policy) const
{
    metastore_ptr->put(maskingPolicyKey(name_space, new_masking_policy.name()), new_masking_policy.SerializeAsString());
}

IMetaStore::IteratorPtr MetastoreProxy::getMaskingPolicyAppliedTables(const String & name_space, const String & masking_policy_name) const
{
    return metastore_ptr->getByPrefix(maskingPolicyTableMappingPrefix(name_space, masking_policy_name));
}

void MetastoreProxy::renameTable(const String & name_space,
                                 Protos::DataModelTable & table,
                                 const String & old_db_name,
                                 const String & old_table_name,
                                 const String & uuid,
                                 BatchCommitRequest & batch_write)
{
    /// update `table`->`uuid` mapping.
    batch_write.AddDelete(SingleDeleteRequest(tableUUIDMappingKey(name_space, old_db_name, old_table_name)));
    Protos::TableIdentifier identifier;
    identifier.set_database(table.database());
    identifier.set_name(table.name());
    identifier.set_uuid(uuid);
    batch_write.AddPut(SinglePutRequest(tableUUIDMappingKey(name_space, table.database(), table.name()), identifier.SerializeAsString(), true));

    String meta_data;
    table.SerializeToString(&meta_data);
    /// add new table meta data with new name
    batch_write.AddPut(SinglePutRequest(tableStoreKey(name_space, uuid, table.commit_time()), meta_data, true));
}

bool MetastoreProxy::alterTable(const String & name_space, const Protos::DataModelTable & table, const Strings & masks_to_remove, const Strings & masks_to_add)
{
    BatchCommitRequest batch_write;

    String table_uuid = UUIDHelpers::UUIDToString(RPCHelpers::createUUID(table.uuid()));
    batch_write.AddPut(SinglePutRequest(tableStoreKey(name_space, table_uuid, table.commit_time()), table.SerializeAsString(), true));
    for (const auto & name : masks_to_remove)
        batch_write.AddDelete(SingleDeleteRequest(maskingPolicyTableMappingKey(name_space, name, table_uuid)));

    for (const auto & name : masks_to_add)
        batch_write.AddPut(SinglePutRequest(maskingPolicyTableMappingKey(name_space, name, table_uuid), table_uuid));

    BatchCommitResponse resp;
    return metastore_ptr->batchWrite(batch_write, resp);
}

Strings MetastoreProxy::getAllTablesInDB(const String & name_space, const String & database)
{
    Strings res;
    auto it = metastore_ptr->getByPrefix(tableUUIDMappingPrefix(name_space, database));
    Protos::TableIdentifier identifier;
    while(it->next())
    {
        identifier.ParseFromString(it->value());
        res.push_back(identifier.name());
    }
    return res;
}

void MetastoreProxy::dropDataPart(const String & name_space, const String & uuid, const String & part_name, const String & part_info)
{
    metastore_ptr->put(dataPartKey(name_space, uuid, part_name), part_info);
}

Strings MetastoreProxy::getPartsByName(const String & name_space, const String & uuid, RepeatedFields & parts_name)
{
    Strings keys;
    for (const auto & part_name : parts_name)
        keys.push_back(dataPartKey(name_space, uuid, part_name));

    Strings parts_meta;
    auto values = metastore_ptr->multiGet(keys);
    for (auto & ele : values)
        parts_meta.emplace_back(std::move(ele.first));
    return parts_meta;
}

IMetaStore::IteratorPtr MetastoreProxy::getPartsInRange(const String & name_space, const String & uuid, const String & partition_id)
{
    if (partition_id.empty())
        /// when partition_id is empty, it should return all data parts in current table.
        return metastore_ptr->getByPrefix(dataPartPrefix(name_space, uuid));

    std::stringstream ss;
    ss << dataPartPrefix(name_space, uuid) << partition_id << '_';
    return metastore_ptr->getByPrefix(ss.str());
}

IMetaStore::IteratorPtr MetastoreProxy::getPartsInRange(const String & name_space, const String & table_uuid, const String & range_start, const String & range_end, bool include_start, bool include_end)
{
    auto prefix = dataPartPrefix(name_space, table_uuid);
    return metastore_ptr->getByRange(prefix + range_start, prefix + range_end, include_start, include_end);
}

UInt64 MetastoreProxy::getNonHostUpdateTimeStamp(const String & name_space, const String & table_uuid)
{
    String nhut = "";
    metastore_ptr->get(nonHostUpdateKey(name_space, table_uuid), nhut);
    return nhut.empty() ? 0 : std::stoul(nhut);
}

void MetastoreProxy::setNonHostUpdateTimeStamp(const String & name_space, const String & table_uuid, const UInt64 pts)
{
    metastore_ptr->put(nonHostUpdateKey(name_space, table_uuid), toString(pts));
}

void MetastoreProxy::prepareAddDataParts(const String & name_space, const String & table_uuid, const Strings & current_partitions,
        const google::protobuf::RepeatedPtrField<Protos::DataModelPart> & parts, BatchCommitRequest & batch_write,
        const std::vector<String> & expected_parts, bool update_sync_list)
{
    if (parts.empty())
        return;

    std::unordered_set<String> existing_partitions{current_partitions.begin(), current_partitions.end()};
    std::unordered_map<String, String > partition_map;

    UInt64 commit_time = 0;
    size_t expected_parts_size = expected_parts.size();

    if (expected_parts_size != static_cast<size_t>(parts.size()))
        throw Exception("Part size wants to write does not match the expected part size.", ErrorCodes::LOGICAL_ERROR);

    for (auto it = parts.begin(); it != parts.end(); it++)
    {
        auto info_ptr = createPartInfoFromModel(it->part_info());
        /// TODO: replace to part commit time?
        if (!commit_time)
            commit_time = info_ptr->mutation;
        String part_meta = it->SerializeAsString();

        batch_write.AddPut(SinglePutRequest(dataPartKey(name_space, table_uuid, info_ptr->getPartName()), part_meta, expected_parts[it - parts.begin()]));

        if (!existing_partitions.count(info_ptr->partition_id) && !partition_map.count(info_ptr->partition_id))
            partition_map.emplace(info_ptr->partition_id, it->partition_minmax());
    }

    if (update_sync_list)
        batch_write.AddPut(SinglePutRequest(syncListKey(name_space, table_uuid, commit_time), std::to_string(commit_time)));

    Protos::PartitionMeta partition_model;
    for (auto it = partition_map.begin(); it != partition_map.end(); it++)
    {
        std::stringstream ss;
        /// To keep the partitions have the same order as data parts in bytekv, we add an extra "_" in the key of partition meta
        ss << tablePartitionInfoPrefix(name_space, table_uuid) << it->first << '_';

        partition_model.set_id(it->first);
        partition_model.set_partition_minmax(it->second);

        batch_write.AddPut(SinglePutRequest(ss.str(), partition_model.SerializeAsString()));
    }
}

void MetastoreProxy::prepareAddStagedParts(
    const String & name_space, const String & table_uuid,
    const google::protobuf::RepeatedPtrField<Protos::DataModelPart> & parts,
    BatchCommitRequest & batch_write,
    const std::vector<String> & expected_staged_parts)
{
    if (parts.empty())
        return;

    size_t expected_staged_part_size = expected_staged_parts.size();
    if (expected_staged_part_size != static_cast<size_t>(parts.size()))
        throw Exception("Staged part size wants to write does not match the expected staged part size.", ErrorCodes::LOGICAL_ERROR);

    for (auto it = parts.begin(); it != parts.end(); it++)
    {
        auto info_ptr = createPartInfoFromModel(it->part_info());
        String part_meta = it->SerializeAsString();
        batch_write.AddPut(SinglePutRequest(stagedDataPartKey(name_space, table_uuid, info_ptr->getPartName()), part_meta, expected_staged_parts[it - parts.begin()]));
    }
}

void MetastoreProxy::dropDataPart(const String & name_space, const String & uuid, const String & part_name)
{
    metastore_ptr->drop(dataPartKey(name_space, uuid, part_name));
}

void MetastoreProxy::dropAllPartInTable(const String & name_space, const String & uuid)
{
    /// clear data parts metadata as well as partition metadata
    metastore_ptr->clean(dataPartPrefix(name_space, uuid));
    metastore_ptr->clean(tablePartitionInfoPrefix(name_space, uuid));
}

IMetaStore::IteratorPtr MetastoreProxy::getStagedParts(const String & name_space, const String & uuid)
{
    return metastore_ptr->getByPrefix(stagedDataPartPrefix(name_space, uuid));
}

IMetaStore::IteratorPtr MetastoreProxy::getStagedPartsInPartition(const String & name_space, const String & uuid, const String & partition)
{
    return metastore_ptr->getByPrefix(stagedDataPartPrefix(name_space, uuid) + partition + "_");
}

void MetastoreProxy::createRootPath(const String & root_path)
{
    std::mt19937 gen(std::random_device{}());
    BatchCommitRequest batch_write;

    String path_id = std::to_string(gen());
    /// avoid generate reserved path id
    while (path_id == "0")
        path_id = std::to_string(gen());

    batch_write.AddPut(SinglePutRequest(ROOT_PATH_PREFIX + root_path, path_id, true));
    batch_write.AddPut(SinglePutRequest(ROOT_PATH_ID_UNIQUE_PREFIX + path_id, "", true));

    BatchCommitResponse resp;
    try
    {
        metastore_ptr->batchWrite(batch_write, resp);
    }
    catch (Exception & e)
    {
        if (e.code() == ErrorCodes::METASTORE_COMMIT_CAS_FAILURE)
        {
            if (resp.puts.count(0))
            {
                throw Exception("Root path " + root_path + " already exists.", ErrorCodes::METASTORE_ROOT_PATH_ALREADY_EXISTS);
            }
            else if (resp.puts.count(1))
            {
                throw Exception("Failed to create new root path because of path id collision, please try again.", ErrorCodes::METASTORE_ROOT_PATH_ID_NOT_UNIQUE);
            }
        }
        throw e;
    }
}

void MetastoreProxy::deleteRootPath(const String & root_path)
{
    String path_id;
    metastore_ptr->get(ROOT_PATH_PREFIX + root_path, path_id);
    if (!path_id.empty())
    {
        BatchCommitRequest batch_write;
        BatchCommitResponse resp;
        batch_write.AddDelete(SingleDeleteRequest(ROOT_PATH_PREFIX + root_path));
        batch_write.AddDelete(SingleDeleteRequest(ROOT_PATH_ID_UNIQUE_PREFIX + path_id));
        metastore_ptr->batchWrite(batch_write, resp);
    }
}

std::vector<std::pair<String, UInt32>> MetastoreProxy::getAllRootPath()
{
    std::vector<std::pair<String, UInt32>> res;
    auto it = metastore_ptr->getByPrefix(ROOT_PATH_PREFIX);
    while(it->next())
    {
        String path = it->key().substr(String(ROOT_PATH_PREFIX).size(), std::string::npos);
        UInt32 path_id = std::stoul(it->value(), nullptr);
        res.emplace_back(path, path_id);
    }
    return res;
}

void MetastoreProxy::createMutation(const String & name_space, const String & uuid, const String & mutation_name, const String & mutation_text)
{
    metastore_ptr->put(tableMutationKey(name_space, uuid, mutation_name), mutation_text);
}

void MetastoreProxy::removeMutation(const String & name_space, const String & uuid, const String & mutation_name)
{
    metastore_ptr->drop(tableMutationKey(name_space, uuid, mutation_name));
}

Strings MetastoreProxy::getAllMutations(const String & name_space, const String & uuid)
{
    Strings res;
    auto it = metastore_ptr->getByPrefix(tableMutationPrefix(name_space, uuid));
    while(it->next())
        res.push_back(it->value());
    return res;
}

void MetastoreProxy::createTransactionRecord(const String & name_space, const UInt64 & txn_id, const String & txn_data)
{
    metastore_ptr->put(transactionRecordKey(name_space, txn_id), txn_data);
}

void MetastoreProxy::removeTransactionRecord(const String & name_space, const UInt64 & txn_id)
{
    metastore_ptr->drop(transactionRecordKey(name_space, txn_id));
}

void MetastoreProxy::removeTransactionRecords(const String & name_space, const std::vector<TxnTimestamp> & txn_ids) {
    if (txn_ids.empty())
        return;

    BatchCommitRequest batch_write;
    BatchCommitResponse resp;

    for (const auto & txn_id : txn_ids)
        batch_write.AddDelete(SingleDeleteRequest(transactionRecordKey(name_space, txn_id.toUInt64())));

    metastore_ptr->batchWrite(batch_write, resp);
}

String MetastoreProxy::getTransactionRecord(const String & name_space, const UInt64 & txn_id)
{
    String txn_data;
    metastore_ptr->get(transactionRecordKey(name_space, txn_id), txn_data);
    return txn_data;
}

std::vector<std::pair<String, UInt64>> MetastoreProxy::getTransactionRecords(const String & name_space, const std::vector<TxnTimestamp> & txn_ids)
{
    if (txn_ids.empty())
        return {};

    Strings txn_keys;
    txn_keys.reserve(txn_ids.size());
    for (const auto & txn_id : txn_ids)
        txn_keys.push_back(transactionRecordKey(name_space, txn_id.toUInt64()));

    return metastore_ptr->multiGet(txn_keys);
}

IMetaStore::IteratorPtr MetastoreProxy::getAllTransactionRecord(const String & name_space, const size_t & max_result_number)
{
    return metastore_ptr->getByPrefix(escapeString(name_space) + "_" + TRANSACTION_RECORD_PREFIX, max_result_number);
}

std::pair<bool, String> MetastoreProxy::updateTransactionRecord(const String & name_space, const UInt64 & txn_id, const String & txn_data_old, const String & txn_data_new)
{
    return metastore_ptr->putCAS(transactionRecordKey(name_space, txn_id), txn_data_new, txn_data_old, true);
}

bool MetastoreProxy::updateTransactionRecordWithOffsets(const String &name_space, const UInt64 &txn_id,
                                                        const String &txn_data_old, const String &txn_data_new,
                                                        const String & consumer_group,
                                                        const cppkafka::TopicPartitionList & tpl)
{
    BatchCommitRequest batch_write;
    BatchCommitResponse resp;

    batch_write.AddPut(SinglePutRequest(transactionRecordKey(name_space, txn_id), txn_data_new, txn_data_old));
    for (auto & tp : tpl)
        batch_write.AddPut(SinglePutRequest(kafkaOffsetsKey(name_space, consumer_group, tp.get_topic(), tp.get_partition()), std::to_string(tp.get_offset())));

    return metastore_ptr->batchWrite(batch_write, resp);

    /*** offsets committing may don't need CAS now
    Strings keys, old_values, new_values;

    /// Pack txn data
    keys.emplace_back(transactionRecordKey(name_space,txn_id));
    old_values.emplace_back(txn_data_old);
    new_values.emplace_back(txn_data_new);

    /// Pack offsets
    for (size_t i = 0; i < tpl.size(); ++i)
    {
        keys.emplace_back(kafkaOffsetsKey(name_space, consumer_group, tpl[i].get_topic(), tpl[i].get_partition()));
        old_values.emplace_back(std::to_string(undo_tpl[i].get_offset()));
        new_values.emplace_back(std::to_string(tpl[i].get_offset()));
    }

    return metastore_ptr->multiWriteCAS(keys, new_values, old_values);
     **/
}

std::pair<bool, String> MetastoreProxy::MetastoreProxy::updateTransactionRecordWithRequests(
    SinglePutRequest & txn_request, BatchCommitRequest & requests, BatchCommitResponse & response)
{
    if (requests.puts.empty())
    {
        return metastore_ptr->putCAS(
            String(txn_request.key), String(txn_request.value), String(*txn_request.expected_value), true);
    }
    else
    {
        requests.AddPut(txn_request);
        metastore_ptr->batchWrite(requests, response);
        return {response.puts.empty(), String{}};
        /// TODO: set txn_result
    }
}

void MetastoreProxy::setTransactionRecord(const String & name_space, const UInt64 & txn_id, const String & txn_data, UInt64 /*ttl*/)
{
    return metastore_ptr->put(transactionRecordKey(name_space, txn_id), txn_data);
}

bool MetastoreProxy::writeIntent(const String & name_space, const String & intent_prefix, const std::vector<WriteIntent> & intents, std::vector<String> & cas_failed_list)
{
    BatchCommitRequest batch_write;
    for (auto  & intent : intents)
    {
        batch_write.AddPut(SinglePutRequest(writeIntentKey(name_space, intent_prefix, intent.intent()), intent.serialize(), true));
    }

    BatchCommitResponse resp;
    try
    {
        metastore_ptr->batchWrite(batch_write, resp);
        return true;
    }
    catch (Exception & e)
    {
        if (e.code() == ErrorCodes::METASTORE_COMMIT_CAS_FAILURE)
        {
            for (auto & [index, new_value]: resp.puts)
            {
                std::cout << "cas failed key: " << new_value << std::endl;
                cas_failed_list.push_back(new_value);
            }
        }
        else
            throw e;
    }
    return false;
}

bool MetastoreProxy::resetIntent(const String & name_space, const String & intent_prefix, const std::vector<WriteIntent> & intents, const UInt64 & new_txn_id, const String & new_location)
{
    BatchCommitRequest batch_write;
    BatchCommitResponse resp;
    for (const auto & intent : intents)
    {
        WriteIntent new_intent(new_txn_id, new_location, intent.intent());
        batch_write.AddPut(SinglePutRequest(writeIntentKey(name_space, intent_prefix, intent.intent()), new_intent.serialize(), intent.serialize()));
    }

    try
    {
        metastore_ptr->batchWrite(batch_write, resp);
        return true;
    }
    catch (const Exception & e)
    {
        if (e.code() == ErrorCodes::METASTORE_COMMIT_CAS_FAILURE)
            return false;
        else
            throw;
    }
}

void MetastoreProxy::clearIntents(const String & name_space, const String & intent_prefix, const std::vector<WriteIntent> & intents)
{
    /// TODO: Because bytekv does not support `CAS delete`, now we firstly get all intent from bytekv and compare with expected value, then
    /// we delete them with version info in one transaction. we may optimize this if bytekv could support `CAS delete` later.

    Strings intent_names;
    for (const auto & intent : intents)
        intent_names.emplace_back(writeIntentKey(name_space, intent_prefix, intent.intent()));

    auto snapshot = metastore_ptr->multiGet(intent_names);

    Poco::Logger * log = &Poco::Logger::get(__func__);
    std::vector<size_t> matched_intent_index;

    for (size_t i = 0; i < intents.size(); i++)
    {
        WriteIntent actual_intent = WriteIntent::deserialize(snapshot[i].first);
        if (intents[i] == actual_intent)
            matched_intent_index.push_back(i);
        else
            LOG_WARNING(
                log, "Clear WriteIntent got CAS_FAILED. expected: " + intents[i].toString() + ", actual: " + actual_intent.toString());
    }

    if (matched_intent_index.empty())
        return;

    /// then remove intents from metastore.
    BatchCommitRequest batch_write;
    BatchCommitResponse resp;

    /// CAS delete is needed becuase the intent could be overwrite by other transactions
    for (auto idx : matched_intent_index)
    {
        batch_write.AddDelete(SingleDeleteRequest(intent_names[idx], {}, intents[idx].serialize()));
    }

    bool cas_success = metastore_ptr->batchWrite(batch_write, resp);

    if (!cas_success && intent_names.size() > 1)
    {
        LOG_WARNING(log, "Clear WriteIntent got CAS_FAILED. This happens because other tasks concurrently reset the WriteIntent.");

        // try clean for each intent
        for (auto idx : matched_intent_index)
        {
            try
            {
                metastore_ptr->drop(intent_names[idx], snapshot[idx].first);
            }
            catch (const Exception & e)
            {
                if (e.code() == ErrorCodes::METASTORE_COMMIT_CAS_FAILURE)
                    LOG_WARNING(log, "CAS fail when cleaning the intent: " + intent_names[idx]);
                else
                    throw e;
            }
        }
    }
}

void MetastoreProxy::clearZombieIntent(const String & name_space, const UInt64 & txn_id)
{
    auto it = metastore_ptr->getByPrefix(escapeString(name_space) + "_" + WRITE_INTENT_PREFIX);
    BatchCommitRequest batch_write;
    BatchCommitResponse resp;
    while(it->next())
    {
        Protos::DataModelWriteIntent intent_model;
        intent_model.ParseFromString(it->value());
        if (intent_model.txn_id() == txn_id)
        {
            batch_write.AddDelete(SingleDeleteRequest(it->value()));
        }
    }

    if (!batch_write.isEmpty())
    {
        metastore_ptr->batchWrite(batch_write, resp);
    }
}

std::multimap<String, String> MetastoreProxy::getAllMutations(const String & name_space)
{
    std::multimap<String, String> res;
    auto it = metastore_ptr->getByPrefix(tableMutationPrefix(name_space));
    while (it->next())
    {
        auto prefix = escapeString(name_space) + "_" + TABLE_MUTATION_PREFIX;
        auto uuid_len = it->key().find('_', prefix.size()) - prefix.size();
        auto uuid = it->key().substr(prefix.size(), uuid_len);
        res.emplace(uuid, it->value());
    }
    return res;
}

void MetastoreProxy::insertTransaction(UInt64 txn)
{
    metastore_ptr->put(transactionKey(txn), "");
}

void MetastoreProxy::removeTransaction(UInt64 txn)
{
    metastore_ptr->drop(transactionKey(txn));
}

IMetaStore::IteratorPtr MetastoreProxy::getActiveTransactions()
{
    return metastore_ptr->getByPrefix(TRANSACTION_STORE_PREFIX);
}

std::unordered_set<UInt64> MetastoreProxy::getActiveTransactionsSet()
{
    IMetaStore::IteratorPtr it = getActiveTransactions();

    std::unordered_set<UInt64> res;

    while(it->next())
    {
        UInt64 txnID = std::stoull(it->key().substr(String(TRANSACTION_STORE_PREFIX).size(), std::string::npos), nullptr);
        res.insert(txnID);
    }

    return res;
}

void MetastoreProxy::writeUndoBuffer(const String & name_space, const UInt64 & txnID, const String & uuid, UndoResources & resources)
{
    if (resources.empty())
        return;

    BatchCommitRequest batch_write;
    BatchCommitResponse resp;
    for (auto & resource : resources)
    {
        resource.setUUID(uuid);
        batch_write.AddPut(SinglePutRequest(undoBufferStoreKey(name_space, txnID, resource), resource.serialize()));
    }
    metastore_ptr->batchWrite(batch_write, resp);
}

void MetastoreProxy::clearUndoBuffer(const String & name_space, const UInt64 & txnID)
{
    metastore_ptr->clean(undoBufferKey(name_space, txnID));
}

IMetaStore::IteratorPtr MetastoreProxy::getUndoBuffer(const String & name_space, UInt64 txnID)
{
    return metastore_ptr->getByPrefix(undoBufferKey(name_space, txnID));
}

IMetaStore::IteratorPtr MetastoreProxy::getAllUndoBuffer(const String & name_space)
{
    return metastore_ptr->getByPrefix(escapeString(name_space) + '_' + UNDO_BUFFER_PREFIX);
}

void MetastoreProxy::multiDrop(const Strings & keys)
{
    BatchCommitRequest batch_write;
    BatchCommitResponse resp;
    for (const auto & key : keys)
    {
        batch_write.AddDelete(SingleDeleteRequest(key));
    }
    metastore_ptr->batchWrite(batch_write, resp);
}

bool MetastoreProxy::batchWrite(const BatchCommitRequest & request, BatchCommitResponse response)
{
    return metastore_ptr->batchWrite(request, response);
}

void MetastoreProxy::writeFilesysLock(const String & name_space, UInt64 txn_id, const String & dir, const String & db, const String & table)
{
    Protos::DataModelFileSystemLock data;
    data.set_txn_id(txn_id);
    data.set_directory(dir);
    data.set_database(db);
    data.set_table(table);

    metastore_ptr->put(filesysLockKey(name_space, dir), data.SerializeAsString());
}

String MetastoreProxy::getFilesysLock(const String & name_space, const String & dir)
{
    String data;
    metastore_ptr->get(filesysLockKey(name_space, dir), data);
    return data;
}

void MetastoreProxy::clearFilesysLock(const String & name_space, const String & dir)
{
    metastore_ptr->drop(filesysLockKey(name_space, dir));
}

IMetaStore::IteratorPtr MetastoreProxy::getAllFilesysLock(const String & name_space)
{
    return metastore_ptr->getByPrefix(escapeString(name_space) + '_' + FILESYS_LOCK_PREFIX);
}

std::vector<String> MetastoreProxy::multiDropAndCheck(const Strings & keys)
{
    multiDrop(keys);
    /// check if all keys have been deleted, return drop failed keys if any;
    std::vector<String> keys_drop_failed{};
    auto values = metastore_ptr->multiGet(keys);
    for (auto & pair : values)
    {
        if (!pair.first.empty())
        {
            keys_drop_failed.emplace_back(pair.first);
        }
    }
    return keys_drop_failed;
}

IMetaStore::IteratorPtr MetastoreProxy::getPartitionList(const String & name_space, const String & uuid)
{
    return metastore_ptr->getByPrefix(tablePartitionInfoPrefix(name_space, uuid));
}

void MetastoreProxy::updateTopologyMeta(const String & name_space, const String & topology)
{
    metastore_ptr->put(escapeString(name_space) + "_" + SERVERS_TOPOLOGY_KEY, topology);
}

String MetastoreProxy::getTopologyMeta(const String & name_space)
{
    String topology_meta;
    metastore_ptr->get(escapeString(name_space) + "_" + SERVERS_TOPOLOGY_KEY, topology_meta);
    return topology_meta;
}

IMetaStore::IteratorPtr MetastoreProxy::getSyncList(const String & name_space, const String & uuid)
{
    return metastore_ptr->getByPrefix(syncListPrefix(name_space, uuid));
}

void MetastoreProxy::clearSyncList(const String & name_space, const String & uuid, const std::vector<TxnTimestamp> & sync_list)
{
    BatchCommitRequest batch_write;
    BatchCommitResponse resp;
    for (auto & ts : sync_list)
        batch_write.AddDelete(SingleDeleteRequest(syncListKey(name_space, uuid, ts)));

    metastore_ptr->batchWrite(batch_write, resp);
}

void MetastoreProxy::clearOffsetsForWholeTopic(const String &name_space, const String &topic, const String &consumer_group)
{
    auto prefix = escapeString(name_space) + "_" + KAFKA_OFFSETS_PREFIX + escapeString(consumer_group) + "_" + escapeString(topic) + "_";
    metastore_ptr->clean(prefix);
}

cppkafka::TopicPartitionList MetastoreProxy::getKafkaTpl(const String & name_space, const String & consumer_group,
                                                         const String & topic_name)
{
    cppkafka::TopicPartitionList res;

    for (int partition = 0; ; ++partition)
    {
        String key = kafkaOffsetsKey(name_space, consumer_group, topic_name, partition);
        String value;
        metastore_ptr->get(key, value);
        /// If cannot get value, we think it get max-partition which maybe optimized later
        if (value.empty())
            break;
        auto tp = cppkafka::TopicPartition(topic_name, partition, std::stoll(value));
        res.emplace_back(std::move(tp));
    }

    std::sort(res.begin(), res.end());
    return res;
}

/// TODO: performance?
void MetastoreProxy::getKafkaTpl(const String & name_space, const String & consumer_group, cppkafka::TopicPartitionList & tpl)
{
    if (tpl.empty())
        return;

    Strings keys;
    for (auto & tp : tpl)
    {
        auto key = kafkaOffsetsKey(name_space, consumer_group, tp.get_topic(), tp.get_partition());
        keys.emplace_back(std::move(key));
    }

    auto values = metastore_ptr->multiGet(keys);
    if (tpl.size() != values.size())
        throw Exception("Got wrong size of offsets while getting tpl", ErrorCodes::LOGICAL_ERROR);

    size_t idx = 0;
    for (auto & ele : values)
    {
        if (unlikely(ele.first.empty()))
            tpl[idx].set_offset(RD_KAFKA_OFFSET_INVALID);
        else
            tpl[idx].set_offset(std::stoll(ele.first));
        ++idx;
    }
}

void MetastoreProxy::setTableClusterStatus(const String & name_space, const String & uuid, const bool & already_clustered)
{
    metastore_ptr->put(clusterStatusKey(name_space, uuid), already_clustered ? "true" : "false");
}

void MetastoreProxy::getTableClusterStatus(const String & name_space, const String & uuid, bool & is_clustered)
{
    String meta;
    metastore_ptr->get(clusterStatusKey(name_space, uuid), meta);
    if (meta == "true")
        is_clustered = true;
    else
        is_clustered = false;
}

/// BackgroundJob related API
void MetastoreProxy::setBGJobStatus(const String & name_space, const String & uuid, CnchBGThreadType type, CnchBGThreadStatus status)
{
    if (type == CnchBGThreadType::Clustering)
        metastore_ptr->put(
            clusterBGJobStatusKey(name_space, uuid),
            String{BGJobStatusInCatalog::serializeToChar(status)}
        );
    else if (type == CnchBGThreadType::MergeMutate)
        metastore_ptr->put(
            mergeBGJobStatusKey(name_space, uuid),
            String{BGJobStatusInCatalog::serializeToChar(status)}
        );
    else if (type == CnchBGThreadType::PartGC)
        metastore_ptr->put(
            partGCBGJobStatusKey(name_space, uuid),
            String{BGJobStatusInCatalog::serializeToChar(status)}
        );
    else if (type == CnchBGThreadType::Consumer)
        metastore_ptr->put(
            consumerBGJobStatusKey(name_space, uuid),
            String{BGJobStatusInCatalog::serializeToChar(status)}
        );
    else if (type == CnchBGThreadType::DedupWorker)
        metastore_ptr->put(
            dedupWorkerBGJobStatusKey(name_space, uuid),
            String{BGJobStatusInCatalog::serializeToChar(status)}
        );
    else
        throw Exception(String{"persistent status is not support for "} + toString(type), ErrorCodes::LOGICAL_ERROR);
}

std::optional<CnchBGThreadStatus> MetastoreProxy::getBGJobStatus(const String & name_space, const String & uuid, CnchBGThreadType type)
{
    String status_store_data;
    if (type == CnchBGThreadType::Clustering)
        metastore_ptr->get(clusterBGJobStatusKey(name_space, uuid), status_store_data);
    else if (type == CnchBGThreadType::MergeMutate)
        metastore_ptr->get(mergeBGJobStatusKey(name_space, uuid), status_store_data);
    else if (type == CnchBGThreadType::PartGC)
        metastore_ptr->get(partGCBGJobStatusKey(name_space, uuid), status_store_data);
    else if (type == CnchBGThreadType::Consumer)
        metastore_ptr->get(consumerBGJobStatusKey(name_space, uuid), status_store_data);
    else if (type == CnchBGThreadType::DedupWorker)
        metastore_ptr->get(dedupWorkerBGJobStatusKey(name_space, uuid), status_store_data);
    else
        throw Exception(String{"persistent status is not support for "} + toString(type), ErrorCodes::LOGICAL_ERROR);

    if (status_store_data.empty())
        return {};

    return BGJobStatusInCatalog::deserializeFromString(status_store_data);
}

UUID MetastoreProxy::parseUUIDFromBGJobStatusKey(const std::string & key)
{
    auto pos = key.rfind("_");
    if (pos == std::string::npos || pos == (key.size() -1))
        throw Exception("invalid BGJobStatusKey", ErrorCodes::LOGICAL_ERROR);
    std::string uuid = key.substr(pos + 1);
    return UUIDHelpers::toUUID(uuid);
}

std::unordered_map<UUID, CnchBGThreadStatus> MetastoreProxy::getBGJobStatuses(const String & name_space, CnchBGThreadType type)
{
    auto get_iter_lambda = [&] ()
        {
            if (type == CnchBGThreadType::Clustering)
                return metastore_ptr->getByPrefix(allClusterBGJobStatusKeyPrefix(name_space));
            else if (type == CnchBGThreadType::MergeMutate)
                return metastore_ptr->getByPrefix(allMergeBGJobStatusKeyPrefix(name_space));
            else if (type == CnchBGThreadType::PartGC)
                return metastore_ptr->getByPrefix(allPartGCBGJobStatusKeyPrefix(name_space));
            else if (type == CnchBGThreadType::Consumer)
                return metastore_ptr->getByPrefix(allConsumerBGJobStatusKeyPrefix(name_space));
            else if (type == CnchBGThreadType::DedupWorker)
                return metastore_ptr->getByPrefix(allDedupWorkerBGJobStatusKeyPrefix(name_space));
            else
                throw Exception(String{"persistent status is not support for "} + toString(type), ErrorCodes::LOGICAL_ERROR);
        };

    std::unordered_map<UUID, CnchBGThreadStatus> res;
    IMetaStore::IteratorPtr it = get_iter_lambda();
    while (it->next())
    {
        res.insert(
            std::make_pair(
                parseUUIDFromBGJobStatusKey(it->key()),
                BGJobStatusInCatalog::deserializeFromString(it->value())
                )
        );
    }

    return res;
}

void MetastoreProxy::dropBGJobStatus(const String & name_space, const String & uuid, CnchBGThreadType type)
{
    switch (type)
    {
        case CnchBGThreadType::Clustering:
            metastore_ptr->drop(clusterBGJobStatusKey(name_space, uuid));
            break;
        case CnchBGThreadType::MergeMutate:
            metastore_ptr->drop(mergeBGJobStatusKey(name_space, uuid));
            break;
        case CnchBGThreadType::PartGC:
            metastore_ptr->drop(partGCBGJobStatusKey(name_space, uuid));
            break;
        case CnchBGThreadType::Consumer:
            metastore_ptr->drop(consumerBGJobStatusKey(name_space, uuid));
            break;
        case CnchBGThreadType::DedupWorker:
            metastore_ptr->drop(dedupWorkerBGJobStatusKey(name_space, uuid));
            break;
        default:
            throw Exception(String{"persistent status is not support for "} + toString(type), ErrorCodes::LOGICAL_ERROR);
    }
}

void MetastoreProxy::setTablePreallocateVW(const String & name_space, const String & uuid, const String & vw)
{
    metastore_ptr->put(preallocateVW(name_space, uuid), vw);
}

void MetastoreProxy::getTablePreallocateVW(const String & name_space, const String & uuid, String & vw)
{
    metastore_ptr->get(preallocateVW(name_space, uuid), vw);
}

IMetaStore::IteratorPtr MetastoreProxy::getMetaInRange(const String & prefix, const String & range_start, const String & range_end, bool include_start, bool include_end)
{
    return metastore_ptr->getByRange(prefix + range_start, prefix + range_end, include_start, include_end);
}

void MetastoreProxy::prepareAddDeleteBitmaps(const String & name_space, const String & table_uuid,
                                             const DeleteBitmapMetaPtrVector & bitmaps,
                                             BatchCommitRequest & batch_write,
                                             const std::vector<String> & expected_bitmaps)
{
    size_t expected_bitmaps_size = expected_bitmaps.size();
    if (expected_bitmaps_size > 0 && expected_bitmaps_size != static_cast<size_t>(bitmaps.size()))
        throw Exception("The size of deleted bitmaps wants to write does not match the actual size in catalog", ErrorCodes::LOGICAL_ERROR);

    size_t idx {0};
    for (const auto & dlb_ptr : bitmaps)
    {
        const Protos::DataModelDeleteBitmap & model = *(dlb_ptr->getModel());
        if (expected_bitmaps_size == 0)
            batch_write.AddPut(SinglePutRequest(deleteBitmapKey(name_space, table_uuid, model), model.SerializeAsString()));
        else
            batch_write.AddPut(SinglePutRequest(deleteBitmapKey(name_space, table_uuid, model), model.SerializeAsString(), expected_bitmaps[idx]));

        ++idx;
    }
}

Strings MetastoreProxy::getDeleteBitmapByKeys(const Strings & keys)
{
    Strings parts_meta;
    auto values = metastore_ptr->multiGet(keys);
    for (auto & ele : values)
        parts_meta.emplace_back(std::move(ele.first));
    return parts_meta;
}

void MetastoreProxy::precommitInsertionLabel(const String & name_space, const InsertionLabelPtr & label)
{
    auto label_key = insertionLabelKey(name_space, toString(label->table_uuid), label->name);
    /// TODO: catch exception here
    metastore_ptr->put(label_key, label->serializeValue(), true /* if_not_exists */);
}

void MetastoreProxy::commitInsertionLabel(const String & name_space, InsertionLabelPtr & label)
{
    auto label_key = insertionLabelKey(name_space, toString(label->table_uuid), label->name);

    String old_value = label->serializeValue();
    label->status = InsertionLabel::Committed;
    String new_value = label->serializeValue();

    try
    {
        metastore_ptr->putCAS(label_key, new_value, old_value);
    }
    catch (...)
    {
        /// TODO: handle exception here
        label->status = InsertionLabel::Precommitted;
        throw;
    }
}

std::pair<uint64_t, String> MetastoreProxy::getInsertionLabel(const String & name_space, const String & uuid, const String & name)
{
    String value;
    auto label_key = insertionLabelKey(name_space, uuid, name);
    auto version = metastore_ptr->get(label_key, value);
    // the version return here is not correct, not suppported
    return {version, value};
}

void MetastoreProxy::removeInsertionLabel(const String & name_space, const String & uuid, const String & name, [[maybe_unused]] uint64_t expected_version)
{
    auto label_key = insertionLabelKey(name_space, uuid, name);
    // the version is not supported
    metastore_ptr->drop(label_key);
}

void MetastoreProxy::removeInsertionLabels(const String & name_space, const std::vector<InsertionLabel> & labels)
{
    BatchCommitRequest batch_write;
    BatchCommitResponse resp;
    for (auto & label : labels)
        batch_write.AddDelete(SingleDeleteRequest(insertionLabelKey(name_space, toString(label.table_uuid), label.name)));
    metastore_ptr->batchWrite(batch_write, resp);
}

IMetaStore::IteratorPtr MetastoreProxy::scanInsertionLabels(const String & name_space, const String & uuid)
{
    auto label_key_prefix = insertionLabelKey(name_space, uuid, {});
    return metastore_ptr->getByPrefix(label_key_prefix);
}

void MetastoreProxy::clearInsertionLabels(const String & name_space, const String & uuid)
{
    auto label_key = insertionLabelKey(name_space, uuid, {});
    metastore_ptr->clean(label_key);
}

void MetastoreProxy::updateTableStatistics(
    const String & name_space, const String & uuid, const std::unordered_map<StatisticsTag, StatisticsBasePtr> & data)
{

    BatchCommitRequest batch_write;
    for (const auto & [tag, statisticPtr] : data)
    {
        Protos::TableStatistic table_statistic;
        table_statistic.set_tag(static_cast<UInt64>(tag));
        table_statistic.set_timestamp(0); // currently this is deprecated
        table_statistic.set_blob(statisticPtr->serialize());
        batch_write.AddPut(SinglePutRequest(tableStatisticKey(name_space, uuid, tag), table_statistic.SerializeAsString()));
        batch_write.AddPut(SinglePutRequest(tableStatisticTagKey(name_space, uuid, tag), std::to_string(static_cast<UInt64>(tag))));
    }

    BatchCommitResponse resp;
    metastore_ptr->batchWrite(batch_write, resp);
}

std::unordered_map<StatisticsTag, StatisticsBasePtr>
MetastoreProxy::getTableStatistics(const String & name_space, const String & uuid, const std::unordered_set<StatisticsTag> & tags)
{
    Strings keys;
    keys.reserve(tags.size());
    for (const auto & tag : tags)
    {
        keys.push_back(tableStatisticKey(name_space, uuid, tag));
    }
    auto values = metastore_ptr->multiGet(keys);
    std::unordered_map<StatisticsTag, StatisticsBasePtr> res;
    for (const auto & value : values)
    {
        if (value.first.empty())
            continue;
        Protos::TableStatistic table_statistic;
        table_statistic.ParseFromString(value.first);
        StatisticsTag tag = static_cast<StatisticsTag>(table_statistic.tag());
        TxnTimestamp ts(table_statistic.timestamp());
        auto statisticPtr = createStatisticsBase(tag, table_statistic.blob());
        res.emplace(tag, statisticPtr);
    }
    return res;
}

std::unordered_set<StatisticsTag> MetastoreProxy::getAvailableTableStatisticsTags(const String & name_space, const String & uuid)
{
    std::unordered_set<StatisticsTag> res;
    auto it = metastore_ptr->getByPrefix(tableStatisticTagPrefix(name_space, uuid));
    while (it->next())
    {
        res.emplace(static_cast<StatisticsTag>(std::stoull(it->value())));
    }
    return res;
}

void MetastoreProxy::removeTableStatistics(const String & name_space, const String & uuid, const std::unordered_set<StatisticsTag> & tags)
{
    BatchCommitRequest batch_write;
    for (const auto & tag : tags)
    {
        batch_write.AddDelete(SingleDeleteRequest(tableStatisticKey(name_space, uuid, tag)));
        batch_write.AddDelete(SingleDeleteRequest(tableStatisticTagKey(name_space, uuid, tag)));
    }
    BatchCommitResponse resp;
    metastore_ptr->batchWrite(batch_write, resp);
}

void MetastoreProxy::updateColumnStatistics(
    const String & name_space,
    const String & uuid,
    const String & column,
    const std::unordered_map<StatisticsTag, StatisticsBasePtr> & data)
{

    BatchCommitRequest batch_write;
    for (const auto & [tag, statisticPtr] : data)
    {
        Protos::ColumnStatistic column_statistic;
        column_statistic.set_tag(static_cast<UInt64>(tag));
        column_statistic.set_timestamp(0);
        column_statistic.set_column(column);
        column_statistic.set_blob(statisticPtr->serialize());
        batch_write.AddPut(SinglePutRequest(columnStatisticKey(name_space, uuid, column, tag), column_statistic.SerializeAsString()));
        batch_write.AddPut(SinglePutRequest(columnStatisticTagKey(name_space, uuid, column, tag), std::to_string(static_cast<UInt64>(tag))));
    }

    BatchCommitResponse resp;
    metastore_ptr->batchWrite(batch_write, resp);
}

std::unordered_map<StatisticsTag, StatisticsBasePtr> MetastoreProxy::getColumnStatistics(
    const String & name_space, const String & uuid, const String & column, const std::unordered_set<StatisticsTag> & tags)
{
    Strings keys;
    keys.reserve(tags.size());
    for (const auto & tag : tags)
    {
        keys.push_back(columnStatisticKey(name_space, uuid, column, tag));
    }
    auto values = metastore_ptr->multiGet(keys);
    std::unordered_map<StatisticsTag, StatisticsBasePtr> res;
    for (const auto & value : values)
    {
        if (value.first.empty())
            continue;
        Protos::ColumnStatistic column_statistic;
        column_statistic.ParseFromString(value.first);
        StatisticsTag tag = static_cast<StatisticsTag>(column_statistic.tag());
        TxnTimestamp ts(column_statistic.timestamp());
        auto statisticPtr = createStatisticsBase(tag, column_statistic.blob());
        res.emplace(tag, statisticPtr);
    }

    return res;
}

std::unordered_set<StatisticsTag>
MetastoreProxy::getAvailableColumnStatisticsTags(const String & name_space, const String & uuid, const String & column)
{
    std::unordered_set<StatisticsTag> res;
    auto it = metastore_ptr->getByPrefix(columnStatisticTagPrefix(name_space, uuid, column));
    while (it->next())
    {
        res.emplace(static_cast<StatisticsTag>(std::stoull(it->value())));
    }
    return res;
}

void MetastoreProxy::removeColumnStatistics(
    const String & name_space, const String & uuid, const String & column, const std::unordered_set<StatisticsTag> & tags)
{
    BatchCommitRequest batch_write;
    for (const auto & tag : tags)
    {
        batch_write.AddDelete(SingleDeleteRequest(columnStatisticKey(name_space, uuid, column, tag)));
        batch_write.AddDelete(SingleDeleteRequest(columnStatisticTagKey(name_space, uuid, column, tag)));
    }
    BatchCommitResponse resp;
    metastore_ptr->batchWrite(batch_write, resp);
}

void MetastoreProxy::createVirtualWarehouse(const String & name_space, const String & vw_name, const VirtualWarehouseData & data)
{
    auto vw_key = VWKey(name_space, vw_name);
    metastore_ptr->put(vw_key, data.serializeAsString(), /*if_not_exists*/ true);
}

void MetastoreProxy::alterVirtualWarehouse(const String & name_space, const String & vw_name, const VirtualWarehouseData & data)
{
    auto vw_key = VWKey(name_space, vw_name);
    metastore_ptr->put(vw_key, data.serializeAsString());
}

bool MetastoreProxy::tryGetVirtualWarehouse(const String & name_space, const String & vw_name, VirtualWarehouseData & data)
{
    auto vw_key = VWKey(name_space, vw_name);
    String value;
    metastore_ptr->get(vw_key, value);

    if (value.empty())
        return false;
    data.parseFromString(value);
    return true;
}

std::vector<VirtualWarehouseData> MetastoreProxy::scanVirtualWarehouses(const String & name_space)
{
    std::vector<VirtualWarehouseData> res;

    auto vw_prefix = VWKey(name_space, String{});
    auto it = metastore_ptr->getByPrefix(vw_prefix);
    while (it->next())
    {
        res.emplace_back();
        res.back().parseFromString(it->value());
    }
    return res;
}

void MetastoreProxy::dropVirtualWarehouse(const String & name_space, const String & vw_name)
{
    VirtualWarehouseData vw_data;
    if (!tryGetVirtualWarehouse(name_space, vw_name, vw_data))
        return;

    auto vw_key = VWKey(name_space, vw_name);
    metastore_ptr->drop(vw_key);
}

void MetastoreProxy::createWorkerGroup(const String & name_space, const String & worker_group_id, const WorkerGroupData & data)
{
    auto worker_group_key = WorkerGroupKey(name_space, worker_group_id);
    metastore_ptr->put(worker_group_key, data.serializeAsString());
}

void MetastoreProxy::updateWorkerGroup(const String & name_space, const String & worker_group_id, const WorkerGroupData & data)
{
    auto worker_group_key = WorkerGroupKey(name_space, worker_group_id);
    metastore_ptr->put(worker_group_key, data.serializeAsString());
}

bool MetastoreProxy::tryGetWorkerGroup(const String & name_space, const String & worker_group_id, WorkerGroupData & data)
{
    auto worker_group_key = WorkerGroupKey(name_space, worker_group_id);
    String value;
    metastore_ptr->get(worker_group_key, value);

    if (value.empty())
        return false;
    data.parseFromString(value);
    return true;
}

std::vector<WorkerGroupData> MetastoreProxy::scanWorkerGroups(const String & name_space)
{
    std::vector<WorkerGroupData> res;

    auto worker_group_prefix = WorkerGroupKey(name_space, String{});
    auto it = metastore_ptr->getByPrefix(worker_group_prefix);
    while (it->next())
    {
        res.emplace_back();
        res.back().parseFromString(it->value());
    }

    return res;
}

void MetastoreProxy::dropWorkerGroup(const String & name_space, const String & worker_group_id)
{
    auto worker_group_key = WorkerGroupKey(name_space, worker_group_id);
    metastore_ptr->drop(worker_group_key);
}

void MetastoreProxy::setMergeMutateThreadStartTime(const String & name_space, const String & uuid, const UInt64 & start_time)
{
    metastore_ptr->put(mergeMutateThreadStartTimeKey(name_space, uuid), toString(start_time));
}

UInt64 MetastoreProxy::getMergeMutateThreadStartTime(const String & name_space, const String & uuid)
{
    String meta_str;
    metastore_ptr->get(mergeMutateThreadStartTimeKey(name_space, uuid), meta_str);
    if (meta_str.empty())
        return 0;
    else
        return std::stoull(meta_str);
}

} /// end of namespace DB::Catalog
