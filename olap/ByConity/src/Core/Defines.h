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

#include <common/defines.h>

#define DBMS_DEFAULT_HOST "localhost"
#define DBMS_DEFAULT_PORT 9000
#define DBMS_DEFAULT_SECURE_PORT 9440
#define DBMS_DEFAULT_HTTP_PORT 8123
#define DBMS_DEFAULT_CONNECT_TIMEOUT_SEC 10
#define DBMS_DEFAULT_CONNECT_TIMEOUT_WITH_FAILOVER_MS 50
#define DBMS_DEFAULT_CONNECT_TIMEOUT_WITH_FAILOVER_SECURE_MS 100
#define DBMS_DEFAULT_SEND_TIMEOUT_SEC 300
#define DBMS_DEFAULT_RECEIVE_TIMEOUT_SEC 300
/// Timeouts for hedged requests.
#define DBMS_DEFAULT_HEDGED_CONNECTION_TIMEOUT_MS 100
#define DBMS_DEFAULT_RECEIVE_DATA_TIMEOUT_MS 2000
/// Timeout for synchronous request-result protocol call (like Ping or TablesStatus).
#define DBMS_DEFAULT_SYNC_REQUEST_TIMEOUT_SEC 5
#define DBMS_DEFAULT_POLL_INTERVAL 10

/// The size of the I/O buffer by default.
#define DBMS_DEFAULT_BUFFER_SIZE 1048576ULL

/** Which blocks by default read the data (by number of rows).
  * Smaller values give better cache locality, less consumption of RAM, but more overhead to process the query.
  */
#define DEFAULT_BLOCK_SIZE 65505    /// 65536 minus 16 + 15 bytes padding that we usually have in arrays

/** Which blocks should be formed for insertion into the table, if we control the formation of blocks.
  * (Sometimes the blocks are inserted exactly such blocks that have been read / transmitted from the outside, and this parameter does not affect their size.)
  * More than DEFAULT_BLOCK_SIZE, because in some tables a block of data on the disk is created for each block (quite a big thing),
  *  and if the parts were small, then it would be costly then to combine them.
  */
#define DEFAULT_INSERT_BLOCK_SIZE 1048545   /// 1048576 minus 16 + 15 bytes padding that we usually have in arrays
#define DEFAULT_BLOCK_SIZE_BYTES 0
/** The same, but for merge operations. Less DEFAULT_BLOCK_SIZE for saving RAM (since all the columns are read).
  * Significantly less, since there are 10-way mergers.
  */
#define DEFAULT_MERGE_BLOCK_SIZE 8192

#define DEFAULT_TEMPORARY_LIVE_VIEW_TIMEOUT_SEC 5
#define DEFAULT_PERIODIC_LIVE_VIEW_REFRESH_SEC 60
#define SHOW_CHARS_ON_SYNTAX_ERROR ptrdiff_t(160)
#define DEFAULT_LIVE_VIEW_HEARTBEAT_INTERVAL_SEC 15
#define DBMS_DEFAULT_DISTRIBUTED_CONNECTIONS_POOL_SIZE 1024
#define DBMS_CONNECTION_POOL_WITH_FAILOVER_DEFAULT_MAX_TRIES 3
/// each period reduces the error counter by 2 times
/// too short a period can cause errors to disappear immediately after creation.
#define DBMS_CONNECTION_POOL_WITH_FAILOVER_DEFAULT_DECREASE_ERROR_PERIOD 60
/// replica error max cap, this is to prevent replica from accumulating too many errors and taking to long to recover.
#define DBMS_CONNECTION_POOL_WITH_FAILOVER_MAX_ERROR_COUNT 1000

/// The boundary on which the blocks for asynchronous file operations should be aligned.
#define DEFAULT_AIO_FILE_BLOCK_SIZE 4096

#define DEFAULT_HTTP_READ_BUFFER_TIMEOUT 1800
#define DEFAULT_HTTP_READ_BUFFER_CONNECTION_TIMEOUT 1
/// Maximum namber of http-connections between two endpoints
/// the number is unmotivated
#define DEFAULT_COUNT_OF_HTTP_CONNECTIONS_PER_ENDPOINT 15

#define DBMS_DEFAULT_PATH "/var/lib/clickhouse/"

#define KEEPER_DEFAULT_PATH "/var/lib/clickhouse-keeper/"

// more aliases: https://mailman.videolan.org/pipermail/x264-devel/2014-May/010660.html

/// Marks that extra information is sent to a shard. It could be any magic numbers.
#define DBMS_DISTRIBUTED_SIGNATURE_HEADER 0xCAFEDACEull
#define DBMS_DISTRIBUTED_SIGNATURE_HEADER_OLD_FORMAT 0xCAFECABEull

#if !__has_include(<sanitizer/asan_interface.h>) || !defined(ADDRESS_SANITIZER)
#   define ASAN_UNPOISON_MEMORY_REGION(a, b)
#   define ASAN_POISON_MEMORY_REGION(a, b)
#endif

/// Actually, there may be multiple acquisitions of different locks for a given table within one query.
/// Check with IStorage class for the list of possible locks
#define DBMS_DEFAULT_LOCK_ACQUIRE_TIMEOUT_SEC 120

/// Default limit on recursion depth of recursive descend parser.
#define DBMS_DEFAULT_MAX_PARSER_DEPTH 1000

/// Max depth of hierarchical dictionary
#define DBMS_HIERARCHICAL_DICTIONARY_MAX_DEPTH 1000

// The file name in ext4 system has a maximum of 255 characters
// 255 - strlen(".null.bin") - 1
#define DBMS_MAX_FILE_NAME_LENGTH 245

/// Version of ClickHouse inter server BRPC protocol.
/// It's not necessary to increase this version number in most cases
/// unless the serialization of plan segment has changed.
#define DBMS_BRPC_PROTOCOL_VERSION 1

#define TEST_KNOB_FORCE_META_REBUILD 0x08ull
