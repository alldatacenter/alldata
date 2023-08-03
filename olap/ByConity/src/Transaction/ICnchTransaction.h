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

#include <Catalog/DataModelPartWrapper_fwd.h>
#include <Core/Types.h>
#include <MergeTreeCommon/InsertionLabel.h>
#include <Parsers/IAST_fwd.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>
#include <Transaction/Actions/IAction.h>
#include <bthread/mutex.h>
#include <cppkafka/cppkafka.h>
#include <Transaction/IntentLock.h>
#include <Core/BackgroundSchedulePool.h>
#include <Interpreters/Context.h>
#include <MergeTreeCommon/InsertionLabel.h>
#include <Transaction/TransactionCommon.h>
#include <Transaction/TxnTimestamp.h>
#include <Databases/IDatabase.h>
#include <cppkafka/topic_partition_list.h>
#include <Common/TypePromotion.h>
#include <Common/serverLocality.h>
#include <common/logger_useful.h>

#include <memory>
#include <string>

namespace DB
{
struct TxnCleanTask;

namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
}

bool isReadOnlyTransaction(const DB::IAST * ast);

class ICnchTransaction : public TypePromotion<ICnchTransaction>, public WithContext
{
public:
    explicit ICnchTransaction(const ContextPtr & context_) : WithContext(context_), global_context(context_->getGlobalContext()) { }
    explicit ICnchTransaction(const ContextPtr & context_, TransactionRecord record)
        : WithContext(context_), global_context(context_->getGlobalContext()), txn_record(std::move(record))
    {
    }

    virtual ~ICnchTransaction() = default;

    ICnchTransaction(const ICnchTransaction &) = delete;
    ICnchTransaction & operator=(const ICnchTransaction &) = delete;

    TxnTimestamp getTransactionID() const { return txn_record.txnID(); }
    TxnTimestamp getPrimaryTransactionID() const { return txn_record.primaryTxnID(); }
    TxnTimestamp getStartTime() const { return txn_record.txnID(); }
    TxnTimestamp getCommitTime() const { return txn_record.commitTs(); }
    void setCommitTime(const TxnTimestamp & commit_ts) { txn_record.setCommitTs(commit_ts); }
    TransactionRecord getTransactionRecord() const { return txn_record; }

    std::unique_lock<bthread::RecursiveMutex> getLock() const { return std::unique_lock(mutex); }

    String getInitiator() const { return txn_record.initiator(); }

    CnchTransactionStatus getStatus() const;

    bool isReadOnly() const { return txn_record.isReadOnly(); }
    void setReadOnly(bool read_only) { txn_record.read_only = read_only; }

    bool isPrepared() { return txn_record.isPrepared(); }

    bool isPrimary() { return txn_record.isPrimary(); }

    bool isSecondary() { return txn_record.isSecondary(); }

    void setMainTableUUID(const UUID & uuid) { main_table_uuid = uuid; }
    UUID getMainTableUUID() const { return main_table_uuid; }

    void setKafkaTpl(const String & consumer_group, const cppkafka::TopicPartitionList & tpl);
    void getKafkaTpl(String & consumer_group, cppkafka::TopicPartitionList & tpl) const;

    template <typename TAction, typename... Args>
    ActionPtr createAction(Args &&... args) const
    {
        return std::make_shared<TAction>(global_context, txn_record.txnID(), std::forward<Args>(args)...);
    }
    template <typename... Args>
    IntentLockPtr createIntentLock(const String & lock_prefix, Args &&... args) const
    {
        String intent = fmt::format("{}", fmt::join(Strings{std::forward<Args>(args)...}, "-"));
        return std::make_unique<IntentLock>(global_context, getTransactionRecord(), lock_prefix, Strings{intent});
    }

    // IntentLockPtr createIntentLock(const LockEntity & entity, const Strings & intent_names = {});

    // If transaction is initiated by worker, record the worker's host and port
    void setCreator(String creator_) { creator = std::move(creator_); }
    const String & getCreator() const { return creator; }

    virtual String getTxnType() const = 0;

    virtual void appendAction(ActionPtr)
    {
        throw Exception("appendAction is not supported for " + getTxnType(), ErrorCodes::NOT_IMPLEMENTED);
    }

    virtual std::vector<ActionPtr> & getPendingActions()
    {
        throw Exception("getPendingActions is not supported for " + getTxnType(), ErrorCodes::NOT_IMPLEMENTED);
    }


    virtual void setKafkaStorageID(StorageID)
    {
        throw Exception("setKafkaStorageID is not supported for " + getTxnType(), ErrorCodes::NOT_IMPLEMENTED);
    }

    virtual StorageID getKafkaTableID() const
    {
        throw Exception("getKafkaTableID is not supported for " + getTxnType(), ErrorCodes::NOT_IMPLEMENTED);
    }

    virtual void setKafkaConsumerIndex(size_t)
    {
        throw Exception("setKafkaConsumerIndex is not supported for " + getTxnType(), ErrorCodes::NOT_IMPLEMENTED);
    }
    virtual size_t getKafkaConsumerIndex() const
    {
        throw Exception("getKafkaConsumerIndex is not supported for " + getTxnType(), ErrorCodes::NOT_IMPLEMENTED);
    }

    void setInsertionLabel(InsertionLabelPtr label) { insertion_label = std::move(label); }
    const InsertionLabelPtr & getInsertionLabel() const { return insertion_label; }

public:
    // Commit API for 2PC, internally calls precommit() and commit()
    // Returns commit_ts on success.
    // throws exceptions and rollback the transaction if commitV2 fails
    virtual TxnTimestamp commitV2() = 0;

    // Precommit transaction, which is the first phase of 2PC
    virtual void precommit() = 0;

    // Commit phase of 2PC
    virtual TxnTimestamp commit() = 0;

    // Rollback transaction, discard all writes made by transaction. Set transaction status to ABORTED
    virtual TxnTimestamp rollback() = 0;

    // Abort transaction, CAS operation.
    virtual TxnTimestamp abort() = 0;

    // Commit API for one-phase commit
    // DOES NOT support rollback if fails
    // DDL statements only supports commitV1 now
    virtual TxnTimestamp commitV1() { throw Exception("commitV1 is not supported for " + getTxnType(), ErrorCodes::NOT_IMPLEMENTED); }

    // Set transaction status to aborted
    // WILL NOT rollback
    virtual void rollbackV1(const TxnTimestamp & /*ts*/)
    {
        throw Exception("commitV1 is not supported for " + getTxnType(), ErrorCodes::NOT_IMPLEMENTED);
    }

    // clean intermediate parts, locks and undobuffer
    virtual void clean(TxnCleanTask &) { }

    // Clean intermediate parts synchronously
    virtual void removeIntermediateData() { }

    bool force_clean_by_dm = false;

    DatabasePtr tryGetDatabaseViaCache(const String & database_name);
    void addDatabaseIntoCache(DatabasePtr db);

    bool async_post_commit = false;
protected:
    void setStatus(CnchTransactionStatus status);
    void setTransactionRecord(TransactionRecord record);

protected:
    /// Transaction still needs global context because the query context will expired after query is finished, but
    /// the transaction still running even query is finished.
    ContextPtr global_context;
    TransactionRecord txn_record;
    UUID main_table_uuid{UUIDHelpers::Nil};

    /// for committing offsets
    String consumer_group;
    cppkafka::TopicPartitionList tpl;

    InsertionLabelPtr insertion_label;

private:
    String creator;
    mutable bthread::RecursiveMutex mutex;

    Poco::Logger * log{&Poco::Logger::get("ICnchTransaction")};
    mutable std::mutex database_cache_mutex;
    std::map<String, DatabasePtr> database_cache;
};

using TransactionCnchPtr = std::shared_ptr<ICnchTransaction>;

class TransactionCnchHolder
{
public:
    explicit TransactionCnchHolder(TransactionCnchPtr txn_ = nullptr) : txn(txn_) { }

    TransactionCnchHolder(TransactionCnchHolder &&) = default;
    TransactionCnchHolder & operator=(TransactionCnchHolder &&) = default;

    void release() { }

    ~TransactionCnchHolder() { release(); }

private:
    TransactionCnchPtr txn;
};

}
