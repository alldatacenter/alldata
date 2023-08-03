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

// #include <iostream>

// #include <Core/Types.h>
// #include <Transaction/TxnTimestamp.h>
// #include <Transaction/TimestampCacheManager.h>
// #include <Transaction/TimestampCache.h>

// #pragma GCC diagnostic ignored "-Wsign-compare"
// #ifdef __clang__
// #pragma clang diagnostic ignored "-Wzero-as-null-pointer-constant"
// #pragma clang diagnostic ignored "-Wundef"
// #endif

// #include <gtest/gtest.h>

// using namespace DB;

// TEST(TimestampCacheInsert, AddParts)
// {
//     const size_t max_size = 10;
//     TimestampCache tsCache(max_size);

//     Strings parts{"p1", "p2", "p3"};
//     TxnTimestamp ts(1);

//     tsCache.insertOrAssign(parts, ts);

//     // tsCache: {p1, p2, p3}
//     EXPECT_EQ(tsCache.size(), 3);
//     EXPECT_EQ(tsCache["p1"], ts);
//     EXPECT_EQ(tsCache["p2"], ts);
//     EXPECT_EQ(tsCache["p3"], ts);

//     tsCache.clear();
//     TxnTimestamp ts2(2);
//     tsCache.insertOrAssign("p1", ts);
//     tsCache.insertOrAssign("p2", ts2);
//     EXPECT_EQ(tsCache["p1"], ts);
//     EXPECT_EQ(tsCache["p2"], ts2);
// }

// TEST(TimestampCacheInsert, AddPartsOverFlow)
// {
//     const size_t max_size = 3;
//     TimestampCache tsCache(max_size);

//     Strings parts{"p1", "p2"};
//     TxnTimestamp t1(1);
//     tsCache.insertOrAssign(parts, t1);

//     parts = {"p3", "p4"};
//     TxnTimestamp t2(2);
//     tsCache.insertOrAssign(parts, t2);

//     // tsCache: {p3, p4, p1}
//     EXPECT_EQ(tsCache.size(), max_size);
//     EXPECT_EQ(tsCache.find("p2"), tsCache.end());
//     EXPECT_EQ(tsCache["p1"], t1);
//     EXPECT_EQ(tsCache["p3"], t2);
//     EXPECT_EQ(tsCache["p4"], t2);
// }

// TEST(TimestampCacheInsert, UpdateParts)
// {
//     const size_t max_size = 3;
//     TimestampCache tsCache(max_size);

//     Strings parts{"p1", "p2"};
//     TxnTimestamp t1(1);
//     tsCache.insertOrAssign(parts, t1);

//     TxnTimestamp t2(2);
//     tsCache.insertOrAssign(parts, t2);

//     EXPECT_EQ(tsCache["p1"], t2);
//     EXPECT_EQ(tsCache["p2"], t2);
// }

// TEST(TimestampCacheInsert, UpdateAndInsertParts)
// {
//     const size_t max_size = 4;
//     TimestampCache tsCache(max_size);

//     Strings parts1{"p1", "p2"};
//     TxnTimestamp t1(1);
//     tsCache.insertOrAssign(parts1, t1);

//     Strings parts2{"p2", "p3"};
//     TxnTimestamp t2(2);
//     tsCache.insertOrAssign(parts2, t2);

//     EXPECT_EQ(tsCache["p1"], t1);
//     EXPECT_EQ(tsCache["p2"], t2);
//     EXPECT_EQ(tsCache["p3"], t2);
// }

// TEST(TimestampCacheInsert, UpdateAndInsertPartsOverflow)
// {
//     const size_t max_size = 4;
//     TimestampCache tsCache(max_size);

//     Strings parts1{"p1", "p2", "p3"};
//     TxnTimestamp t1(1);
//     tsCache.insertOrAssign(parts1, t1);

//     Strings parts2{"p3", "p4", "p5"};
//     TxnTimestamp t2(2);
//     tsCache.insertOrAssign(parts2, t2);

//     // tsCache: {p3, p4, p5, p1}
//     EXPECT_EQ(tsCache.size(), max_size);
//     EXPECT_EQ(tsCache["p1"], t1);
//     EXPECT_EQ(tsCache.find("p2"), tsCache.end());
//     EXPECT_EQ(tsCache["p3"], t2);
//     EXPECT_EQ(tsCache["p4"], t2);
// }

// TEST(TimestampCacheLoopUps, LookupParts)
// {
//     const size_t max_size = 4;
//     TimestampCache tsCache(max_size);

//     TxnTimestamp t1(1);
//     tsCache.insertOrAssign(Strings{"p1", "p2"}, t1);

//     TxnTimestamp t2(2);
//     tsCache.insertOrAssign(Strings{"p3", "p4"}, t2);

//     EXPECT_EQ(tsCache.lookup(Strings{"p1"}), t1);
//     EXPECT_EQ(tsCache.lookup(Strings{"p3", "p4"}), t2);
//     EXPECT_EQ(tsCache.lookup(Strings{"p1", "p3", "p4"}), t2);

//     // p10, p11 not exists
//     EXPECT_EQ(tsCache.lookup(Strings{"p1", "p10"}), t1);
//     EXPECT_EQ(tsCache.lookup(Strings{"p10", "p11"}), tsCache.low_water());
// }

// TEST(TimestampCacheManager, AcquireLock)
// {
//     const size_t max_size = 10;
//     std::unique_ptr<TimestampCacheManager> tsCacheManager = std::make_unique<TimestampCacheManager>(max_size);
//     UUID table_uuid = UUIDHelpers::generateV4();
//     TxnTimestamp t1(1);
//     {
//         auto lock = tsCacheManager->getTimestampCacheTableGuard(table_uuid);
//         auto & tsCachePtr = tsCacheManager->getTimestampCacheUnlocked(table_uuid);
//         tsCachePtr->insertOrAssign(Strings{"p1", "p2"}, t1);
//     }

//     {
//         auto lock = tsCacheManager->getTimestampCacheTableGuard(table_uuid);
//         auto & tsCachePtr = tsCacheManager->getTimestampCacheUnlocked(table_uuid);
//         EXPECT_EQ(tsCachePtr->lookup("p1"), t1);
//         EXPECT_EQ(tsCachePtr->lookup("p2"), t1);
//     }
// }
