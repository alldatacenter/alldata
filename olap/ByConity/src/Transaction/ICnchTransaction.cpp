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

#include <Transaction/ICnchTransaction.h>

#include <Catalog/DataModelPartWrapper.h>
// #include <MergeTreeCommon/CnchPartsHelper.h>
#include <CloudServices/CnchServerClient.h>
#include <CloudServices/CnchServerClientPool.h>
#include <ResourceGroup/IResourceGroupManager.h>
#include <Transaction/LockManager.h>
#include <cppkafka/topic_partition_list.h>
#include <Common/serverLocality.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int SUPPORT_IS_DISABLED;
    extern const int CNCH_LOCK_ACQUIRE_FAILED;
}

bool isReadOnlyTransaction(const DB::IAST * ast)
{
    ResourceSelectCase::QueryType query_type = ResourceSelectCase::getQueryType(ast);
    return query_type == ResourceSelectCase::QueryType::SELECT || query_type == ResourceSelectCase::QueryType::OTHER;
}

CnchTransactionStatus ICnchTransaction::getStatus() const
{
    auto lock = getLock();
    return txn_record.status();
}

void ICnchTransaction::setStatus(CnchTransactionStatus status)
{
    auto lock = getLock();
    txn_record.setStatus(status);
}

void ICnchTransaction::setTransactionRecord(TransactionRecord record)
{
    auto lock = getLock();
    txn_record = std::move(record);
}

// IntentLockPtr ICnchTransaction::createIntentLock(const LockEntity & entity, const Strings & intent_names)
// {
//     return std::make_unique<IntentLock>(context, getTransactionRecord(), entity, intent_names);
// }

void ICnchTransaction::setKafkaTpl(const String & consumer_group_, const cppkafka::TopicPartitionList & tpl_)
{
    this->consumer_group = consumer_group_;
    this->tpl = tpl_;
}

void ICnchTransaction::getKafkaTpl(String & consumer_group_, cppkafka::TopicPartitionList & tpl_) const
{
    consumer_group_ = this->consumer_group;
    tpl_ = this->tpl;
}

DatabasePtr ICnchTransaction::tryGetDatabaseViaCache(const String & database_name)
{
    std::lock_guard lock(database_cache_mutex);
    auto it = database_cache.find(database_name);
    if (database_cache.end() != it)
        return it->second;
    else
        return nullptr;
}

void ICnchTransaction::addDatabaseIntoCache(DatabasePtr db)
{
    std::lock_guard lock(database_cache_mutex);
    database_cache.insert(std::make_pair(db->getDatabaseName(), std::move(db)));
}

}
