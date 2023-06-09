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

#include <Protos/data_models.pb.h>
#include <Transaction/LockManager.h>
#include <Transaction/LockRequest.h>
#include <Common/Exception.h>
#include <Core/UUID.h>
#include <IO/WriteHelpers.h>
#include <IO/Operators.h>
#include <Transaction/LockDefines.h>
#include <Protos/RPCHelpers.h>

#include <cassert>
#include <ctime>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

LockRequest::LockRequest(TxnTimestamp id, String lock_entity, LockLevel lock_level, LockMode lock_mode, UInt64 timeout_)
    : txn_id(std::move(id)), entity(std::move(lock_entity)), level(lock_level), mode(lock_mode), timeout(timeout_)
{
}

bool LockRequest::lock(const Context & context)
{
    if (getStatus() != LockStatus::LOCK_INIT)
    {
        throw Exception("Lock status should be LOCK_INIT", ErrorCodes::LOGICAL_ERROR);
    }

    auto lock_status = LockManager::instance().lock(this, context);
    if (lock_status == LockStatus::LOCK_OK)
    {
        return true;
    }
    else if (lock_status == LockStatus::LOCK_TIMEOUT)
    {
        return false;
    }
    else if (lock_status == LockStatus::LOCK_WAITING)
    {
        if (wait())
        {
            // status has changed to OK before timeout
            return true;
        }
        else
        {
            // remove from LockContext's waiting list
            LockManager::instance().unlock(this);
            setStatus(LockStatus::LOCK_TIMEOUT);
            return false;
        }
    }
    else
    {
        throw Exception("Invalid lock status " + toString(to_underlying(status)), ErrorCodes::LOGICAL_ERROR);
    }
}

void LockRequest::unlock()
{
    // TODO: remove from waiting list or granted list
    auto lock_status = getStatus();
    if (lock_status == LockStatus::LOCK_WAITING || lock_status == LockStatus::LOCK_OK)
    {
        LockManager::instance().unlock(this);
    }
    else if (lock_status == LockStatus::LOCK_TIMEOUT)
    {
        // no need to deal with timeout case
    }
    else
    {
        throw Exception("Invalid unlock operator " + toString(to_underlying(status)), ErrorCodes::LOGICAL_ERROR);
    }
}

LockRequest::LockRequestItor LockRequest::getRequestItor() const
{
    std::lock_guard lg(mutex);
    return request_itor;
}

LockStatus LockRequest::getStatus() const
{
    std::lock_guard lg(mutex);
    return status;
}

String LockRequest::toDebugString() const
{
    Protos::DataModelLockField field_model;
    field_model.ParseFromString(entity);
    WriteBufferFromOwnString buf;
    buf << "LockRequest{txn_id: " << txn_id.toString() << ", entity: " << field_model.ShortDebugString() << ", level: " << toString(level)
        << ", mode: " << toString(mode) << ", timeout: " << timeout << '}';
    return buf.str();
}

void LockRequest::setLockResult(LockStatus lockStatus, LockRequestItor it)
{
    {
        std::lock_guard lg(mutex);
        status = lockStatus;
        request_itor = it;
    }
}

void LockRequest::setStatus(LockStatus status_)
{
    std::lock_guard lg(mutex);
    status = status_;
}

void LockRequest::notify()
{
    cv.notify_one();
}

bool LockRequest::wait()
{
    std::unique_lock lk(mutex);
    return cv.wait_for(lk, std::chrono::milliseconds(timeout), [this]() { return status == LockStatus::LOCK_OK; });
}

LockLevel LockInfo::getLockLevel() const {
    if (hasPartition())
    {
        return LockLevel::PARTITION;
    }
    else if (hasBucket())
    {
        return LockLevel::BUCKET;
    }
    else
    {
        return LockLevel::TABLE;
    }
}

const LockRequestPtrs & LockInfo::getLockRequests()
{
    if (!requests.empty())
    {
        return requests;
    }

    if (lock_mode != LockMode::S && lock_mode != LockMode::X)
    {
        throw Exception("Only support lockmode S or X", ErrorCodes::LOGICAL_ERROR);
    }

    LockLevel level = getLockLevel();
    LockMode intent_mode = (lock_mode == LockMode::S) ? LockMode::IS : LockMode::IX;

    Strings entities(LockLevelSize);
    Protos::DataModelLockField field_model;
    {
        RPCHelpers::fillUUID(table_uuid, *(field_model.mutable_uuid()));
        field_model.SerializeToString(&entities[to_underlying(LockLevel::TABLE)]);
    }
    if (hasBucket())
    {
        field_model.set_bucket(bucket);
        field_model.SerializeToString(&entities[to_underlying(LockLevel::BUCKET)]);
    }
    if (hasPartition())
    {
        field_model.set_partition(partition);
        field_model.SerializeToString(&entities[to_underlying(LockLevel::PARTITION)]);
    }

    for (size_t i = 0; i <= to_underlying(level); i++)
    {
        if (entities[i].empty())
            continue;

        LockMode mode = (i == to_underlying(level)) ? lock_mode : intent_mode;
        requests.emplace_back(std::make_unique<LockRequest>(txn_id, std::move(entities[i]), static_cast<LockLevel>(i), mode, timeout));
    }

    return requests;
}

String LockInfo::toDebugString() const
{
    WriteBufferFromOwnString buf;
    buf << "LockInfo{txn_id: " << txn_id.toString() << ", lock_id: " << lock_id << ", level: " << toString(getLockLevel())
        << ", mode: " << toString(lock_mode) << ", timeout: " << timeout << ", uuid: " << table_uuid;
    if (hasBucket())
    {
        buf << ", bucket: " << bucket;
    }

    if (hasPartition())
    {
        buf << ", partition: " << partition;
    }
    buf << '}';
    return buf.str();
}
}
