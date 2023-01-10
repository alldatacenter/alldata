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

// #include <Core/UUIDHelpers.h>
// #include <Interpreters/Context.h>
// #include <Transaction/CnchLock.h>
// #include <Transaction/LockDefines.h>
// #include <Transaction/LockManager.h>
// #include <Transaction/LockRequest.h>
// #include <DaemonManager/tests/get_context_for_gtest.h>

// #pragma GCC diagnostic ignored "-Wsign-compare"
// #ifdef __clang__
// #    pragma clang diagnostic ignored "-Wzero-as-null-pointer-constant"
// #    pragma clang diagnostic ignored "-Wundef"
// #endif

// #include <chrono>
// #include <thread>
// #include <gtest/gtest.h>

// using namespace DB;

// class LockManagerTest : public ::testing::Test
// {
// protected:
//     virtual void SetUp() override
//     {
//         LockManager::instance().initialize();
//         LockManager::instance().setEvictExpiredLocks(false);
//         context = &DBGTestEnvironment::getContext();
//     }

//     Context* context;
// };

// TEST_F(LockManagerTest, lockModes)
// {
//     // None
//     UInt32 modes = modeMask(LockMode::NONE);
//     EXPECT_FALSE(conflicts(LockMode::NONE, modes));
//     EXPECT_FALSE(conflicts(LockMode::IS, modes));
//     EXPECT_FALSE(conflicts(LockMode::IX, modes));
//     EXPECT_FALSE(conflicts(LockMode::S, modes));
//     EXPECT_FALSE(conflicts(LockMode::X, modes));

//     // IS
//     modes = modeMask(LockMode::IS);
//     EXPECT_FALSE(conflicts(LockMode::NONE, modes));
//     EXPECT_FALSE(conflicts(LockMode::IS, modes));
//     EXPECT_FALSE(conflicts(LockMode::IX, modes));
//     EXPECT_FALSE(conflicts(LockMode::S, modes));
//     EXPECT_TRUE(conflicts(LockMode::X, modes));

//     // IX
//     modes = modeMask(LockMode::IX);
//     EXPECT_FALSE(conflicts(LockMode::NONE, modes));
//     EXPECT_FALSE(conflicts(LockMode::IS, modes));
//     EXPECT_FALSE(conflicts(LockMode::IX, modes));
//     EXPECT_TRUE(conflicts(LockMode::S, modes));
//     EXPECT_TRUE(conflicts(LockMode::X, modes));

//     // S
//     modes = modeMask(LockMode::S);
//     EXPECT_FALSE(conflicts(LockMode::NONE, modes));
//     EXPECT_FALSE(conflicts(LockMode::IS, modes));
//     EXPECT_TRUE(conflicts(LockMode::IX, modes));
//     EXPECT_FALSE(conflicts(LockMode::S, modes));
//     EXPECT_TRUE(conflicts(LockMode::X, modes));

//     // X
//     modes = modeMask(LockMode::X);
//     EXPECT_FALSE(conflicts(LockMode::NONE, modes));
//     EXPECT_TRUE(conflicts(LockMode::IS, modes));
//     EXPECT_TRUE(conflicts(LockMode::IX, modes));
//     EXPECT_TRUE(conflicts(LockMode::S, modes));
//     EXPECT_TRUE(conflicts(LockMode::X, modes));
// }

// static void checkConflict(const Context & context, LockMode existingMode, LockMode newMode, bool hasConflict)
// {
//     LockRequest request(1, "db.test", LockLevel::TABLE, existingMode, 2000);
//     EXPECT_EQ(true, request.lock(context));

//     LockRequest newRequest(2, "db.test", LockLevel::TABLE, newMode, 2000);
//     LockStatus status = LockManager::instance().lock(&newRequest, context);
//     if (hasConflict)
//         EXPECT_EQ(LockStatus::LOCK_WAITING, status);
//     else
//     {
//         EXPECT_EQ(LockStatus::LOCK_OK, status);
//     }
//     newRequest.unlock();
//     request.unlock();
// }

// TEST_F(LockManagerTest, ValidateConflictMatrix)
// {
//     checkConflict(*context, LockMode::IS, LockMode::IS, false);
//     checkConflict(*context, LockMode::IS, LockMode::IX, false);
//     checkConflict(*context, LockMode::IS, LockMode::S, false);
//     checkConflict(*context, LockMode::IS, LockMode::X, true);

//     checkConflict(*context, LockMode::IX, LockMode::IS, false);
//     checkConflict(*context, LockMode::IX, LockMode::IX, false);
//     checkConflict(*context, LockMode::IX, LockMode::S, true);
//     checkConflict(*context, LockMode::IX, LockMode::X, true);

//     checkConflict(*context, LockMode::S, LockMode::IS, false);
//     checkConflict(*context, LockMode::S, LockMode::IX, true);
//     checkConflict(*context, LockMode::S, LockMode::S, false);
//     checkConflict(*context, LockMode::S, LockMode::X, true);

//     checkConflict(*context, LockMode::X, LockMode::IS, true);
//     checkConflict(*context, LockMode::X, LockMode::IX, true);
//     checkConflict(*context, LockMode::X, LockMode::S, true);
//     checkConflict(*context, LockMode::X, LockMode::X, true);
// }

// TEST_F(LockManagerTest, FIFO)
// {
//     const UInt64 timeout = 2000;
//     LockRequest request1(1, "db.test", LockLevel::TABLE, LockMode::X, timeout);
//     EXPECT_EQ(true, request1.lock(*context));

//     // The subsequent request will be block
//     LockRequest request2(2, "db.test", LockLevel::TABLE, LockMode::X, timeout);
//     EXPECT_EQ(LockStatus::LOCK_WAITING, LockManager::instance().lock(&request2, *context));

//     LockRequest request3(3, "db.test", LockLevel::TABLE, LockMode::X, timeout);
//     EXPECT_EQ(LockStatus::LOCK_WAITING, LockManager::instance().lock(&request3, *context));

//     EXPECT_EQ(LockStatus::LOCK_OK, request1.getStatus());
//     // Once request1 unlocks, request2 should be granted
//     request1.unlock();
//     EXPECT_EQ(LockStatus::LOCK_OK, request2.getStatus());
//     EXPECT_EQ(LockStatus::LOCK_WAITING, request3.getStatus());

//     // Once request2 unlocks, request3 should be granted
//     request2.unlock();
//     EXPECT_EQ(LockStatus::LOCK_OK, request3.getStatus());

//     request3.unlock();
// }

// TEST_F(LockManagerTest, WaitLockTimeout)
// {
//     const UInt64 timeout = 1000;
//     LockRequest request1(1, "db.test", LockLevel::TABLE, LockMode::X, timeout);
//     EXPECT_EQ(true, request1.lock(*context));

//     LockRequest request2(2, "db.test", LockLevel::TABLE, LockMode::X, timeout);
//     EXPECT_EQ(false, request2.lock(*context));

//     request1.unlock();
//     request2.unlock();
// }

// TEST_F(LockManagerTest, LockedByMe)
// {
//     const UInt64 timeout = 1000;
//     LockRequest txn1_request1(1, "db.test", LockLevel::TABLE, LockMode::X, timeout);
//     EXPECT_EQ(true, txn1_request1.lock(*context));

//     LockRequest txn1_request2(1, "db.test", LockLevel::TABLE, LockMode::X, timeout);
//     EXPECT_EQ(true, txn1_request2.lock(*context));

//     LockRequest txn2_request1(2, "db.test", LockLevel::TABLE, LockMode::X, timeout);
//     EXPECT_EQ(LockStatus::LOCK_WAITING, LockManager::instance().lock(&txn2_request1, *context));

//     txn1_request1.unlock();
//     EXPECT_EQ(LockStatus::LOCK_WAITING, txn2_request1.getStatus());
//     txn1_request2.unlock();
//     EXPECT_EQ(LockStatus::LOCK_OK, txn2_request1.getStatus());

//     txn2_request1.unlock();
// }

// TEST_F(LockManagerTest, TableLock)
// {
//     const UInt64 timeout = 1000;
//     UUID uuid = UUIDHelpers::Nil;
//     LockInfoPtr info = std::make_shared<LockInfo>(0);
//     info->setLockID(1).setMode(LockMode::X).setTimeout(timeout).setUUID(uuid);
//     LockManager::instance().lock(info, *context);
//     EXPECT_EQ(LockStatus::LOCK_OK, info->status);
//     auto & lock_reqs = info->getLockRequests();
//     EXPECT_EQ(1, lock_reqs.size());

//     // Request to lock the same table will block
//     LockRequest request(1, lock_reqs[0]->getEntity(), LockLevel::TABLE, LockMode::X, timeout);
//     EXPECT_EQ(LockStatus::LOCK_WAITING, LockManager::instance().lock(&request, *context));
//     LockManager::instance().unlock(info);
//     EXPECT_EQ(LockStatus::LOCK_OK, request.getStatus());

//     // Request to lock another table is ok
//     info = std::make_shared<LockInfo>(1);
//     info->setLockID(2).setMode(LockMode::X).setTimeout(timeout).setUUID(UUIDHelpers::generateV4());
//     LockManager::instance().lock(info, *context);
//     EXPECT_EQ(LockStatus::LOCK_OK, info->status);
// }

// TEST_F(LockManagerTest, BucketLock)
// {
//     const UInt64 timeout = 1000;
//     UUID uuid = UUIDHelpers::Nil;
//     Int64 bucket = 0;
//     LockInfoPtr info = std::make_shared<LockInfo>(0);
//     info->setLockID(1).setMode(LockMode::X).setTimeout(timeout).setUUID(uuid).setBucket(bucket);
//     LockManager::instance().lock(info, *context);
//     EXPECT_EQ(LockStatus::LOCK_OK, info->status);
//     auto & lock_reqs = info->getLockRequests();
//     EXPECT_EQ(2, lock_reqs.size());

//     // Table level should be locked
//     LockRequest request1(1, lock_reqs[0]->getEntity(), LockLevel::TABLE, LockMode::X, timeout);
//     EXPECT_EQ(LockStatus::LOCK_WAITING, LockManager::instance().lock(&request1, *context));

//     // Bucket level should be locked
//     LockRequest request2(1, lock_reqs[1]->getEntity(), LockLevel::BUCKET, LockMode::X, timeout);
//     EXPECT_EQ(LockStatus::LOCK_WAITING, LockManager::instance().lock(&request2, *context));

//     LockManager::instance().unlock(info);
//     EXPECT_EQ(LockStatus::LOCK_OK, request1.getStatus());
//     EXPECT_EQ(LockStatus::LOCK_OK, request2.getStatus());

//     // Lock another table is ok
//     info = std::make_shared<LockInfo>(1);
//     info->setLockID(2).setMode(LockMode::X).setTimeout(timeout).setUUID(UUIDHelpers::generateV4()).setBucket(bucket);
//     LockManager::instance().lock(info, *context);
//     EXPECT_EQ(LockStatus::LOCK_OK, info->status);
// }

// TEST_F(LockManagerTest, PartitionLock)
// {
//     const UInt64 timeout = 1000;
//     UUID uuid = UUIDHelpers::Nil;
//     Int64 bucket = 1;
//     String partition = "all";
//     LockInfoPtr info = std::make_shared<LockInfo>(0);
//     info->setLockID(1).setMode(LockMode::X).setTimeout(timeout).setUUID(uuid).setBucket(bucket).setPartition(partition);
//     LockManager::instance().lock(info, *context);
//     EXPECT_EQ(LockStatus::LOCK_OK, info->status);
//     auto & lock_reqs = info->getLockRequests();

//     // Table level should be locked
//     LockRequest request1(1, lock_reqs[0]->getEntity(), LockLevel::TABLE, LockMode::X, timeout);
//     EXPECT_EQ(LockStatus::LOCK_WAITING, LockManager::instance().lock(&request1, *context));

//     // Bucket level should be locked
//     LockRequest request2(1, lock_reqs[1]->getEntity(), LockLevel::BUCKET, LockMode::X, timeout);
//     EXPECT_EQ(LockStatus::LOCK_WAITING, LockManager::instance().lock(&request2, *context));

//     // Partition level should be locked
//     LockRequest request3(1, lock_reqs[2]->getEntity(), LockLevel::PARTITION, LockMode::X, timeout);
//     EXPECT_EQ(LockStatus::LOCK_WAITING, LockManager::instance().lock(&request3, *context));

//     LockManager::instance().unlock(info);
//     EXPECT_EQ(LockStatus::LOCK_OK, request1.getStatus());
//     EXPECT_EQ(LockStatus::LOCK_OK, request2.getStatus());
//     EXPECT_EQ(LockStatus::LOCK_OK, request3.getStatus());

//     // Lock another table is ok
//     info = std::make_shared<LockInfo>(0);
//     info->setLockID(2).setMode(LockMode::X).setTimeout(timeout).setUUID(UUIDHelpers::generateV4()).setBucket(bucket).setPartition(partition);
//     LockManager::instance().lock(info, *context);
//     EXPECT_EQ(LockStatus::LOCK_OK, info->status);
// }

// TEST_F(LockManagerTest, EvictExpiredTxnLocks)
// {
//     LockManager::instance().setEvictExpiredLocks(true);
//     TxnTimestamp txn_id = 10;
//     UUID uuid = UUIDHelpers::generateV4();
//     Int64 bucket = 1;
//     String partition = "all";
//     LockInfoPtr info = std::make_shared<LockInfo>(txn_id);
//     info->setLockID(1).setMode(LockMode::X).setTimeout(1000).setUUID(uuid).setBucket(bucket).setPartition(partition);
//     LockManager::instance().lock(info, *context);
//     EXPECT_EQ(LockStatus::LOCK_OK, info->status);
//     const auto & lock_reqs = info->getLockRequests();

//     // make the transaction expired
//     LockManager::instance().updateExpireTime(
//         txn_id, LockManager::Clock::now() - std::chrono::milliseconds(1000));

//     // should be able to acquire locks
//     LockRequest request0(1, lock_reqs[0]->getEntity(), LockLevel::TABLE, LockMode::X, 1000);
//     LockRequest request1(1, lock_reqs[1]->getEntity(), LockLevel::BUCKET, LockMode::X, 1000);
//     LockRequest request2(1, lock_reqs[2]->getEntity(), LockLevel::PARTITION, LockMode::X, 1000);

//     EXPECT_EQ(LockStatus::LOCK_OK, LockManager::instance().lock(&request0, *context));
//     EXPECT_EQ(LockStatus::LOCK_OK, LockManager::instance().lock(&request1, *context));
//     EXPECT_EQ(LockStatus::LOCK_OK, LockManager::instance().lock(&request2, *context));

//     LockManager::instance().unlock(&request0);
//     LockManager::instance().unlock(&request1);
//     LockManager::instance().unlock(&request2);

//     // If updated txn expire time, new lock requests should fail
//     LockManager::instance().lock(info, *context);
//     LockManager::instance().updateExpireTime(txn_id, LockManager::Clock::now() + std::chrono::milliseconds(3000));
//     EXPECT_EQ(LockStatus::LOCK_WAITING, LockManager::instance().lock(&request0, *context));
//     EXPECT_EQ(LockStatus::LOCK_WAITING, LockManager::instance().lock(&request1, *context));
//     EXPECT_EQ(LockStatus::LOCK_WAITING, LockManager::instance().lock(&request1, *context));
// }
