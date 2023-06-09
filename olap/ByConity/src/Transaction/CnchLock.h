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

#include <Core/BackgroundSchedulePool.h>
#include <Interpreters/StorageID.h>
#include <Transaction/LockRequest.h>
#include <boost/core/noncopyable.hpp>
#include <Transaction/TxnTimestamp.h>

namespace DB
{
class Context;

class CnchLockHolder : private boost::noncopyable
{
public:
    explicit CnchLockHolder(const Context & global_context_, std::vector<LockInfoPtr> && elems);
    ~CnchLockHolder();

    [[nodiscard]] bool tryLock();
    void unlock();
    void setLockExpireDuration(std::chrono::milliseconds expire_duration) { lock_expire_duration = expire_duration; }

private:
    void reportLockHeartBeatTask();
    void reportLockHeartBeat();

    const Context & global_context;
    TxnTimestamp txn_id;
    BackgroundSchedulePool::TaskHolder report_lock_heartbeat_task;

    class CnchLock;
    std::vector<std::unique_ptr<CnchLock>> cnch_locks;

    std::chrono::milliseconds lock_expire_duration{30000};
};

}
