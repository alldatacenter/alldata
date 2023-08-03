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

#include <cstddef>
#include <Core/Names.h>
#include <Core/UUID.h>
// #include <Interpreters/Context.h>
#include <Protos/RPCHelpers.h>
#include <Protos/cnch_common.pb.h>
#include <Protos/data_models.pb.h>
#include <Transaction/TxnTimestamp.h>
#include <Poco/Logger.h>
#include <common/logger_useful.h>
#include <Core/Types.h>

namespace DB
{
namespace Catalog
{
    class Catalog;
}
class MergeTreeMetaBase;

/// TODO: @ducle.canh - use protobuf records
using CnchTransactionStatus = Protos::TransactionStatus;
using CnchTransactionType = Protos::TransactionType;
using CnchTransactionPriority = Protos::TransactionPriority;

const char * txnStatusToString(CnchTransactionStatus status);

const char * txnTypeToString(CnchTransactionType type);

const char * txnPriorityToString(CnchTransactionPriority priority);

enum class CnchTransactionInitiator
{
    Server = 0, /// Transaction initiated by server
    Worker = 1, /// Transaction initiated by worker
    Kafka = 2, /// Transaction initiated by kafka engine
    Merge = 3, /// Transaction initiated by merge task
    GC = 4, /// Transaction initiated by garbage collection task
    Txn = 5, /// Transaction initiated by interactive transaction session
};

const char * txnInitiatorToString(CnchTransactionInitiator initiator);

struct WriteIntent
{
    using Container = Protos::DataModelWriteIntent;
    Container pb_model;

    WriteIntent() = default;
    WriteIntent(TxnTimestamp txn_id, String location, String intent)
    {
        if (txn_id)
            pb_model.set_txn_id(txn_id);
        if (!location.empty())
            pb_model.set_location(std::move(location));
        if (!intent.empty())
            pb_model.set_intent(std::move(intent));
    }

    bool operator==(const WriteIntent & other) const
    {
        return pb_model.txn_id() == other.pb_model.txn_id() && pb_model.location() == other.pb_model.location()
            && pb_model.intent() == other.pb_model.intent();
    }

    bool operator!=(const WriteIntent & other) const { return !(*this == other); }

    /// Getters
    TxnTimestamp txnId() const { return pb_model.txn_id(); }
    String location() const { return pb_model.location(); }
    String intent() const { return pb_model.intent(); }

    /// Setters
    WriteIntent & setTxnId(TxnTimestamp txn_id)
    {
        if (txn_id)
            pb_model.set_txn_id(txn_id.toUInt64());
        return *this;
    }
    WriteIntent & setLocation(String location)
    {
        if (!location.empty())
            pb_model.set_location(std::move(location));
        return *this;
    }
    WriteIntent & setIntent(String intent)
    {
        if (!intent.empty())
            pb_model.set_intent(std::move(intent));
        return *this;
    }

    /// Serialization and deserialization
    String serialize() const { return pb_model.SerializeAsString(); }
    static WriteIntent deserialize(const String & serialized)
    {
        WriteIntent res;
        res.pb_model.ParseFromString(serialized);
        return res;
    }
    static WriteIntent deserializeFromArray(const char * data, size_t sz)
    {
        WriteIntent res;
        res.pb_model.ParseFromArray(data, sz);
        return res;
    }

    /// Helpers and debug methods
    String toString() const { return pb_model.ShortDebugString(); }
};

using UndoResourceType = Protos::UndoResourceType;
inline bool UndoResourceTypeIsValid(UndoResourceType type)
{
    return ::DB::Protos::UndoResourceType_IsValid(type);
}


struct UndoResource
{
    using Container = Protos::DataModelUndoBuffer;
    UInt64 txn_id;
    /// each undo resource object will have a unique id; this is safe enough as even if we create
    /// 1 million undo resources per sec, it will take ~30 years to overflow the counter
    UInt64 id;
    Container pb_model;

    Poco::Logger * log = &Poco::Logger::get("UndoResource");

    /// pb_model.placeholders is a repeated field that hold resource info. Depending on the type,
    /// we have different interpretations of the resource info. Note that disk name is not a
    /// placeholder because it can be empty.
    /// type        | placeholder[0] | placeholder[1] | placeholder[2] | ...
    /// ---------------------------------------------------------------|---
    /// Part        | name           | relative_path  | null           |
    /// DeleteBitmap| name           | relative_path  | null           |

    /// Becareful with the order of the `args`, don't change it unless you know what you are doing
    template <typename... Args>
    UndoResource(TxnTimestamp txn_id_, UndoResourceType type_, Args&&... args) : txn_id(txn_id_.toUInt64())
    {
        if (!UndoResourceTypeIsValid(type_))
            throw Exception("UndoResource: invalid type " + toString(static_cast<int>(type_)), ErrorCodes::LOGICAL_ERROR);
        pb_model.set_type(type_);
        std::array<String, sizeof...(Args)> placeholders{std::forward<Args>(args)...};
        for (size_t i = 0; i < placeholders.size(); ++i)
        {
            if (placeholders[i].empty())
                throw Exception("UndoResource: empty placeholder, this is a bug", ErrorCodes::LOGICAL_ERROR);

            /// Only for backward and forward compatibility with cnch-1.0 and cnch-1.1,
            /// can completely remove them after verify the dev version
            if (i == 0) pb_model.set_placeholder_0(placeholders[i]);
            if (i == 1) pb_model.set_placeholder_1(placeholders[i]);

            pb_model.add_placeholders(std::move(placeholders[i]));
        }

        pb_model.set_txn_id(txn_id);
        id = counter.fetch_add(1);
    }

    /// Getters
    const String & diskName() const { return pb_model.disk_name(); }
    bool metadataOnly() const { return pb_model.metadata_only(); }
    UndoResourceType type() const { return pb_model.type(); }
    const String & uuid() const { return pb_model.uuid(); }
    size_t numPlaceholders() const { return pb_model.placeholders_size(); }
    const String & placeholders(size_t i) const
    {
        if (i > 1)
        {
            /// old format has only 2 placeholders, so if i > 1 it must from new format
            /// remove this check for better performance later
            return pb_model.placeholders(i);
        }
        if (i == 0) return isLegacy() ? pb_model.placeholder_0() : pb_model.placeholders(0);
        if (i == 1) return isLegacy() ? pb_model.placeholder_1() : pb_model.placeholders(1);
        __builtin_unreachable();
    }

    /// Setters -> only for uuid and disk name
    UndoResource & setUUID(String uuid)
    {
        if (!uuid.empty())
            pb_model.set_uuid(std::move(uuid));
        return *this;
    }
    UndoResource & setDiskName(String disk_name)
    {
        if (!disk_name.empty())
            pb_model.set_disk_name(std::move(disk_name));
        return *this;
    }
    UndoResource & setMetadataOnly(bool metadata_only)
    {
        pb_model.set_metadata_only(metadata_only);
        return *this;
    }

    /// Serialization and deserialization
    String serialize() const { return pb_model.SerializeAsString(); }
    static UndoResource deserialize(const String & serialized)
    {
        UndoResource res;
        res.pb_model.ParseFromString(serialized);
        return res;
    }
    static UndoResource deserializeFromArray(const char * data, size_t sz)
    {
        UndoResource res;
        res.pb_model.ParseFromArray(data, sz);
        return res;
    }

    /// domain level logics
    bool isLegacy() const { return pb_model.placeholders_size() == 0; }
    void clean(Catalog::Catalog & catalog, MergeTreeMetaBase * storage) const;

private:
    static inline std::atomic<UInt64> counter{0};
    UndoResource() = default;
};

using UndoResources = std::vector<UndoResource>;

struct UndoResourceNames
{
    NameSet parts;
    NameSet bitmaps;
    NameSet staged_parts;
};

UndoResourceNames integrateResources(const UndoResources & resources);

/// filesys lock structure, use pb_model directly
struct FilesysLock
{
    using FilesysLockContainer = Protos::DataModelFileSystemLock;
    FilesysLockContainer pb_model;
    UInt64 txn_id() const { return pb_model.txn_id(); }
    const String & database() const { return pb_model.database(); }
    const String & table() const { return pb_model.table(); }
    const String & directory() const { return pb_model.directory(); }
};

struct CommitExtraInfo
{
};


/// Transaction Record provide an abstract layer on top of Protos::DataModelTransactionRecord
/// User getters and setters, DO NOT create custom constructors
struct TransactionRecord
{
    using Container = Protos::DataModelTransactionRecord;

    Container pb_model;
    bool read_only{false}; // read-only transaction
    bool prepared{false}; // transaction is prepared and ready to commit

    /// Constructors for compability with cnch-1.0 and cnch-1.1, can completely remove them after verify the dev version
    TransactionRecord()
    {
        /// txn_id, commit_ts, status, priority, and location is required in cnch-1.0 and cnch-1.1
        setID(0).setCommitTs(0).setStatus(CnchTransactionStatus::Inactive).setPriority(CnchTransactionPriority::low).setLocation("");
    }
    TransactionRecord(const Container & pb_model_) : pb_model(pb_model_) {}
    TransactionRecord(Container && pb_model_) : pb_model(std::move(pb_model_)) {}

    /// Getters
    TxnTimestamp txnID() const { return pb_model.txn_id(); } // 0 if not set
    CnchTransactionType type() const { return pb_model.type(); } // Implicit if not set
    TxnTimestamp primaryTxnID() const { return pb_model.has_primary_txn_id() ? pb_model.primary_txn_id() : pb_model.txn_id(); }
    TxnTimestamp commitTs() const { return pb_model.commit_ts(); } // 0 if not set
    CnchTransactionStatus status() const { return pb_model.status(); } // Running if not set
    CnchTransactionPriority priority() const { return pb_model.has_priority() ? pb_model.priority() : CnchTransactionPriority::high; }
    const String & initiator() const { return pb_model.initiator(); } // empty string if not set
    const String & location() const { return pb_model.location(); } // empty string if not set
    TxnTimestamp cleanTs() const { return pb_model.clean_ts(); } // 0 if not set
    UUID mainTableUUID() const
    {
        return pb_model.has_main_table_uuid() ? RPCHelpers::createUUID(pb_model.main_table_uuid()) : UUIDHelpers::Nil;
    }


    /// Setters, using chain-style
    TransactionRecord & setID(const TxnTimestamp & txn_id_)
    {
        pb_model.set_txn_id(txn_id_.toUInt64());
        return *this;
    }
    TransactionRecord & setType(const CnchTransactionType type_)
    {
        pb_model.set_type(type_);
        return *this;
    }
    TransactionRecord & setStatus(const CnchTransactionStatus status_)
    {
        pb_model.set_status(status_);
        return *this;
    }
    TransactionRecord & setCommitTs(const TxnTimestamp & commit_ts_)
    {
        pb_model.set_commit_ts(commit_ts_.toUInt64());
        return *this;
    }
    TransactionRecord & setPriority(const CnchTransactionPriority priority_)
    {
        pb_model.set_priority(priority_);
        return *this;
    }
    TransactionRecord & setInitiator(const String & initiator_)
    {
        pb_model.set_initiator(initiator_);
        return *this;
    }
    TransactionRecord & setLocation(const String & location_)
    {
        pb_model.set_location(location_);
        return *this;
    }
    TransactionRecord & setPrimaryID(const TxnTimestamp & primary_txn_id_)
    {
        if (primary_txn_id_)
            pb_model.set_primary_txn_id(primary_txn_id_.toUInt64());
        return *this;
    }
    TransactionRecord & setCleanTs(const TxnTimestamp & clean_ts_)
    {
        if (clean_ts_)
            pb_model.set_clean_ts(clean_ts_.toUInt64());
        return *this;
    }
    TransactionRecord & setMainTableUUID(const UUID & uuid_)
    {
        if (uuid_ != UUIDHelpers::Nil)
        {
            Protos::UUID uuid;
            RPCHelpers::fillUUID(uuid_, uuid);
            pb_model.mutable_main_table_uuid()->CopyFrom(uuid);
        }
        return *this;
    }

    TransactionRecord & setReadOnly(bool read_only_)
    {
        read_only = read_only_;
        return *this;
    }

    /// Serialization and deserialization
    String serialize() const { return pb_model.SerializeAsString(); }
    static TransactionRecord deserialize(const String & serialized)
    {
        TransactionRecord res;
        res.pb_model.ParseFromString(serialized);
        return res;
    }
    static TransactionRecord deserializeFromArray(const char * data, size_t sz)
    {
        TransactionRecord res;
        res.pb_model.ParseFromArray(data, sz);
        return res;
    }

    /// helpers & debug methods
    bool isReadOnly() const { return read_only; }
    bool isPrepared() const { return pb_model.status() == CnchTransactionStatus::Running && prepared; }
    bool isSecondary() const { return pb_model.has_primary_txn_id() && pb_model.type() == CnchTransactionType::Implicit; }
    bool isPrimary() const { return !isSecondary(); }
    bool ended() const
    {
        return pb_model.status() == CnchTransactionStatus::Finished || pb_model.status() == CnchTransactionStatus::Aborted;
    }
    bool hasMainTableUUID() const { return pb_model.has_main_table_uuid(); }
    String toString() const { return pb_model.ShortDebugString(); }
};

}
