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

#include <atomic>
#include <map>
#include <optional>
#include <boost/noncopyable.hpp>

#include <Core/UUID.h>
#include <Interpreters/StorageID.h>
#include <Transaction/TransactionCommon.h>
#include <Transaction/TxnTimestamp.h>
#include <Common/Exception.h>
#include <common/logger_useful.h>
#include "Interpreters/Context_fwd.h"

namespace DB
{
class Context;
class IntentLock;
using IntentLockPtr = std::unique_ptr<IntentLock>;

/// Provide kv intent lock for a single object, sastify Lockable concept https://en.cppreference.com/w/cpp/named_req/Lockable
class IntentLock : boost::noncopyable, WithContext
{
public:
    static constexpr auto TB_LOCK_PREFIX = "TB_LOCK";
    static constexpr auto DB_LOCK_PREFIX = "DB_LOCK";

    IntentLock(ContextPtr context_, TransactionRecord txn_record_, String lock_prefix_, Strings intent_names_ = {})
        : WithContext(context_)
        , txn_record(std::move(txn_record_))
        , lock_prefix(lock_prefix_)
        , intent_names(std::move(intent_names_))
        , log(&Poco::Logger::get("IntentLock"))
    {
    }

    ~IntentLock()
    {
        try
        {
            unlock();
        }
        catch (...)
        {
            DB::tryLogCurrentException(__PRETTY_FUNCTION__);
        }
    }
    bool tryLock();
    bool isLocked() const { return locked; }
    void lock();
    void unlock();
    bool try_lock() { return tryLock(); } /// To provide std::lock compatible interface

private:
    static constexpr size_t lock_retry = 3;

    TransactionRecord txn_record;
    String lock_prefix;
    Strings intent_names;

    bool locked{false};
    bool valid{true};
    Poco::Logger * log;

    void lockImpl();
    void writeIntents();
    void removeIntents();
    std::optional<TransactionRecord> tryGetTransactionRecord(const TxnTimestamp & txnID);
    std::vector<WriteIntent> createWriteIntents();
};

}
