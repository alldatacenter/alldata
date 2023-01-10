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

#include <Common/ProfileEvents.h>
#include <Common/CurrentThread.h>
#include <Common/typeid_cast.h>
#include <Columns/ColumnArray.h>

/// Available events. Add something here as you wish.
#define APPLY_FOR_EVENTS(M) \
    M(Query, "Number of queries to be interpreted and potentially executed. Does not include queries that failed to parse or were rejected due to AST size limits, quota limits or limits on the number of simultaneously running queries. May include internal queries initiated by ClickHouse itself. Does not count subqueries.") \
    M(VwQuery, "Number of queries started to be interpreted and maybe executed that belongs to a virtual warehouse.") \
    M(SelectQuery, "Same as Query, but only for SELECT queries.") \
    M(InsertQuery, "Same as Query, but only for INSERT queries.") \
    M(FailedQuery, "Number of failed queries.") \
    M(FailedSelectQuery, "Same as FailedQuery, but only for SELECT queries.") \
    M(FailedInsertQuery, "Same as FailedQuery, but only for INSERT queries.") \
    M(InsufficientConcurrencyQuery, "Number of queries that are cancelled due to insufficient concurrency") \
    M(QueryTimeMicroseconds, "Total time of all queries.") \
    M(SelectQueryTimeMicroseconds, "Total time of SELECT queries.") \
    M(InsertQueryTimeMicroseconds, "Total time of INSERT queries.") \
    M(FileOpen, "Number of files opened.") \
    M(Seek, "Number of times the 'lseek' function was called.") \
    M(ReadBufferFromFileDescriptorRead, "Number of reads (read/pread) from a file descriptor. Does not include sockets.") \
    M(ReadBufferFromFileDescriptorReadFailed, "Number of times the read (read/pread) from a file descriptor have failed.") \
    M(ReadBufferFromFileDescriptorReadBytes, "Number of bytes read from file descriptors. If the file is compressed, this will show the compressed data size.") \
    M(WriteBufferFromFileDescriptorWrite, "Number of writes (write/pwrite) to a file descriptor. Does not include sockets.") \
    M(WriteBufferFromFileDescriptorWriteFailed, "Number of times the write (write/pwrite) to a file descriptor have failed.") \
    M(WriteBufferFromFileDescriptorWriteBytes, "Number of bytes written to file descriptors. If the file is compressed, this will show compressed data size.") \
    M(ReadBufferAIORead, "") \
    M(ReadBufferAIOReadBytes, "") \
    M(WriteBufferAIOWrite, "") \
    M(WriteBufferAIOWriteBytes, "") \
    M(ReadCompressedBytes, "Number of bytes (the number of bytes before decompression) read from compressed sources (files, network).") \
    M(CompressedReadBufferBlocks, "Number of compressed blocks (the blocks of data that are compressed independent of each other) read from compressed sources (files, network).") \
    M(CompressedReadBufferBytes, "Number of uncompressed bytes (the number of bytes after decompression) read from compressed sources (files, network).") \
    M(UncompressedCacheHits, "") \
    M(UncompressedCacheMisses, "") \
    M(UncompressedCacheWeightLost, "") \
    M(MMappedFileCacheHits, "") \
    M(MMappedFileCacheMisses, "") \
    M(IOBufferAllocs, "") \
    M(IOBufferAllocBytes, "") \
    M(ArenaAllocChunks, "") \
    M(ArenaAllocBytes, "") \
    M(FunctionExecute, "") \
    M(TableFunctionExecute, "") \
    M(MarkCacheHits, "") \
    M(MarkCacheMisses, "") \
    M(QueryCacheHits, "") \
    M(QueryCacheMisses, "") \
    M(ChecksumsCacheHits, "") \
    M(ChecksumsCacheMisses, "") \
    M(CreatedReadBufferOrdinary, "") \
    M(CreatedReadBufferAIO, "") \
    M(CreatedReadBufferAIOFailed, "") \
    M(CreatedReadBufferMMap, "") \
    M(CreatedReadBufferMMapFailed, "") \
    M(DiskReadElapsedMicroseconds, "Total time spent waiting for read syscall. This include reads from page cache.") \
    M(DiskWriteElapsedMicroseconds, "Total time spent waiting for write syscall. This include writes to page cache.") \
    M(NetworkReceiveElapsedMicroseconds, "Total time spent waiting for data to receive or receiving data from network. Only ClickHouse-related network interaction is included, not by 3rd party libraries.") \
    M(NetworkSendElapsedMicroseconds, "Total time spent waiting for data to send to network or sending data to network. Only ClickHouse-related network interaction is included, not by 3rd party libraries..") \
    M(NetworkReceiveBytes, "Total number of bytes received from network. Only ClickHouse-related network interaction is included, not by 3rd party libraries.") \
    M(NetworkSendBytes, "Total number of bytes send to network. Only ClickHouse-related network interaction is included, not by 3rd party libraries.") \
    M(ThrottlerSleepMicroseconds, "Total time a query was sleeping to conform the 'max_network_bandwidth' setting.") \
    \
    M(QueryMaskingRulesMatch, "Number of times query masking rules was successfully matched.") \
    \
    M(ReplicatedPartFetches, "Number of times a data part was downloaded from replica of a ReplicatedMergeTree table.") \
    M(ReplicatedPartFailedFetches, "Number of times a data part was failed to download from replica of a ReplicatedMergeTree table.") \
    M(ObsoleteReplicatedParts, "") \
    M(ReplicatedPartMerges, "Number of times data parts of ReplicatedMergeTree tables were successfully merged.") \
    M(ReplicatedPartFetchesOfMerged, "Number of times we prefer to download already merged part from replica of ReplicatedMergeTree table instead of performing a merge ourself (usually we prefer doing a merge ourself to save network traffic). This happens when we have not all source parts to perform a merge or when the data part is old enough.") \
    M(ReplicatedPartMutations, "") \
    M(ReplicatedPartChecks, "") \
    M(ReplicatedPartChecksFailed, "") \
    M(ReplicatedDataLoss, "Number of times a data part that we wanted doesn't exist on any replica (even on replicas that are offline right now). That data parts are definitely lost. This is normal due to asynchronous replication (if quorum inserts were not enabled), when the replica on which the data part was written was failed and when it became online after fail it doesn't contain that data part.") \
    \
    M(InsertedRows, "Number of rows INSERTed to all tables.") \
    M(InsertedBytes, "Number of bytes (uncompressed; for columns as they stored in memory) INSERTed to all tables.") \
    M(DelayedInserts, "Number of times the INSERT of a block to a MergeTree table was throttled due to high number of active data parts for partition.") \
    M(RejectedInserts, "Number of times the INSERT of a block to a MergeTree table was rejected with 'Too many parts' exception due to high number of active data parts for partition.") \
    M(DelayedInsertsMilliseconds, "Total number of milliseconds spent while the INSERT of a block to a MergeTree table was throttled due to high number of active data parts for partition.") \
    M(DistributedDelayedInserts, "Number of times the INSERT of a block to a Distributed table was throttled due to high number of pending bytes.") \
    M(DistributedRejectedInserts, "Number of times the INSERT of a block to a Distributed table was rejected with 'Too many bytes' exception due to high number of pending bytes.") \
    M(DistributedDelayedInsertsMilliseconds, "Total number of milliseconds spent while the INSERT of a block to a Distributed table was throttled due to high number of pending bytes.") \
    M(DuplicatedInsertedBlocks, "Number of times the INSERTed block to a ReplicatedMergeTree table was deduplicated.") \
    \
    M(ZooKeeperInit, "") \
    M(ZooKeeperTransactions, "") \
    M(ZooKeeperList, "") \
    M(ZooKeeperCreate, "") \
    M(ZooKeeperRemove, "") \
    M(ZooKeeperExists, "") \
    M(ZooKeeperGet, "") \
    M(ZooKeeperSet, "") \
    M(ZooKeeperMulti, "") \
    M(ZooKeeperCheck, "") \
    M(ZooKeeperClose, "") \
    M(ZooKeeperWatchResponse, "") \
    M(ZooKeeperUserExceptions, "") \
    M(ZooKeeperHardwareExceptions, "") \
    M(ZooKeeperOtherExceptions, "") \
    M(ZooKeeperWaitMicroseconds, "") \
    M(ZooKeeperBytesSent, "") \
    M(ZooKeeperBytesReceived, "") \
    \
    M(StorageMemoryFlush, "") \
    M(StorageMemoryErrorOnFlush, "") \
    M(StorageMemoryPassedAllMinThresholds, "") \
    M(StorageMemoryPassedTimeMaxThreshold, "") \
    M(StorageMemoryPassedRowsMaxThreshold, "") \
    M(StorageMemoryPassedBytesMaxThreshold, "") \
    \
    M(DistributedConnectionFailTry, "Total count when distributed connection fails with retry") \
    M(DistributedConnectionMissingTable, "") \
    M(DistributedConnectionStaleReplica, "") \
    M(DistributedConnectionFailAtAll, "Total count when distributed connection fails after all retries finished") \
    \
    M(HedgedRequestsChangeReplica, "Total count when timeout for changing replica expired in hedged requests.") \
    \
    M(CompileFunction, "Number of times a compilation of generated LLVM code (to create fused function for complex expressions) was initiated.") \
    M(CompiledFunctionExecute, "Number of times a compiled function was executed.") \
    M(CompileExpressionsMicroseconds, "Total time spent for compilation of expressions to LLVM code.") \
    M(CompileExpressionsBytes, "Number of bytes used for expressions compilation.") \
    \
    M(ExternalSortWritePart, "") \
    M(ExternalSortMerge, "") \
    M(ExternalAggregationWritePart, "") \
    M(ExternalAggregationMerge, "") \
    M(ExternalAggregationCompressedBytes, "") \
    M(ExternalAggregationUncompressedBytes, "") \
    \
    M(SlowRead, "Number of reads from a file that were slow. This indicate system overload. Thresholds are controlled by read_backoff_* settings.") \
    M(ReadBackoff, "Number of times the number of query processing threads was lowered due to slow reads.") \
    \
    M(ReplicaYieldLeadership, "Number of times Replicated table was yielded its leadership due to large replication lag relative to other replicas.") \
    M(ReplicaPartialShutdown, "How many times Replicated table has to deinitialize its state due to session expiration in ZooKeeper. The state is reinitialized every time when ZooKeeper is available again.") \
    \
    M(SelectedParts, "Number of data parts selected to read from a MergeTree table.") \
    M(SelectedRanges, "Number of (non-adjacent) ranges in all data parts selected to read from a MergeTree table.") \
    M(SelectedMarks, "Number of marks (index granules) selected to read from a MergeTree table.") \
    M(SelectedRows, "Number of rows SELECTed from all tables.") \
    M(SelectedBytes, "Number of bytes (uncompressed; for columns as they stored in memory) SELECTed from all tables.") \
    \
    M(Merge, "Number of launched background merges.") \
    M(MergedRows, "Rows read for background merges. This is the number of rows before merge.") \
    M(MergedUncompressedBytes, "Uncompressed bytes (for columns as they stored in memory) that was read for background merges. This is the number before merge.") \
    M(MergesTimeMilliseconds, "Total time spent for background merges.")\
    \
    M(MergeTreeDataWriterRows, "Number of rows INSERTed to MergeTree tables.") \
    M(MergeTreeDataWriterUncompressedBytes, "Uncompressed bytes (for columns as they stored in memory) INSERTed to MergeTree tables.") \
    M(MergeTreeDataWriterCompressedBytes, "Bytes written to filesystem for data INSERTed to MergeTree tables.") \
    M(MergeTreeDataWriterBlocks, "Number of blocks INSERTed to MergeTree tables. Each block forms a data part of level zero.") \
    M(MergeTreeDataWriterBlocksAlreadySorted, "Number of blocks INSERTed to MergeTree tables that appeared to be already sorted.") \
    \
    M(MergeTreeDataProjectionWriterRows, "Number of rows INSERTed to MergeTree tables projection.") \
    M(MergeTreeDataProjectionWriterUncompressedBytes, "Uncompressed bytes (for columns as they stored in memory) INSERTed to MergeTree tables projection.") \
    M(MergeTreeDataProjectionWriterCompressedBytes, "Bytes written to filesystem for data INSERTed to MergeTree tables projection.") \
    M(MergeTreeDataProjectionWriterBlocks, "Number of blocks INSERTed to MergeTree tables projection. Each block forms a data part of level zero.") \
    M(MergeTreeDataProjectionWriterBlocksAlreadySorted, "Number of blocks INSERTed to MergeTree tables projection that appeared to be already sorted.") \
    \
    M(CannotRemoveEphemeralNode, "Number of times an error happened while trying to remove ephemeral node. This is not an issue, because our implementation of ZooKeeper library guarantee that the session will expire and the node will be removed.") \
    \
    M(RegexpCreated, "Compiled regular expressions. Identical regular expressions compiled just once and cached forever.") \
    M(ContextLock, "Number of times the lock of Context was acquired or tried to acquire. This is global lock.") \
    \
    M(StorageBufferFlush, "") \
    M(StorageBufferErrorOnFlush, "") \
    M(StorageBufferPassedAllMinThresholds, "") \
    M(StorageBufferPassedTimeMaxThreshold, "") \
    M(StorageBufferPassedRowsMaxThreshold, "") \
    M(StorageBufferPassedBytesMaxThreshold, "") \
    M(StorageBufferPassedTimeFlushThreshold, "") \
    M(StorageBufferPassedRowsFlushThreshold, "") \
    M(StorageBufferPassedBytesFlushThreshold, "") \
    M(StorageBufferLayerLockReadersWaitMilliseconds, "Time for waiting for Buffer layer during reading") \
    M(StorageBufferLayerLockWritersWaitMilliseconds, "Time for waiting free Buffer layer to write to (can be used to tune Buffer layers)") \
    \
    M(DictCacheKeysRequested, "") \
    M(DictCacheKeysRequestedMiss, "") \
    M(DictCacheKeysRequestedFound, "") \
    M(DictCacheKeysExpired, "") \
    M(DictCacheKeysNotFound, "") \
    M(DictCacheKeysHit, "") \
    M(DictCacheRequestTimeNs, "") \
    M(DictCacheRequests, "") \
    M(DictCacheLockWriteNs, "") \
    M(DictCacheLockReadNs, "") \
    \
    M(DistributedSyncInsertionTimeoutExceeded, "") \
    M(DataAfterMergeDiffersFromReplica, "") \
    M(DataAfterMutationDiffersFromReplica, "") \
    M(PolygonsAddedToPool, "") \
    M(PolygonsInPoolAllocatedBytes, "") \
    M(RWLockAcquiredReadLocks, "") \
    M(RWLockAcquiredWriteLocks, "") \
    M(RWLockReadersWaitMilliseconds, "") \
    M(RWLockWritersWaitMilliseconds, "") \
    M(DNSError, "Total count of errors in DNS resolution") \
    \
    M(RealTimeMicroseconds, "Total (wall clock) time spent in processing (queries and other tasks) threads (not that this is a sum).") \
    M(UserTimeMicroseconds, "Total time spent in processing (queries and other tasks) threads executing CPU instructions in user space. This include time CPU pipeline was stalled due to cache misses, branch mispredictions, hyper-threading, etc.") \
    M(SystemTimeMicroseconds, "Total time spent in processing (queries and other tasks) threads executing CPU instructions in OS kernel space. This include time CPU pipeline was stalled due to cache misses, branch mispredictions, hyper-threading, etc.") \
    M(SoftPageFaults, "") \
    M(HardPageFaults, "") \
    M(VoluntaryContextSwitches, "") \
    M(InvoluntaryContextSwitches, "") \
    \
    M(OSIOWaitMicroseconds, "Total time a thread spent waiting for a result of IO operation, from the OS point of view. This is real IO that doesn't include page cache.") \
    M(OSCPUWaitMicroseconds, "Total time a thread was ready for execution but waiting to be scheduled by OS, from the OS point of view.") \
    M(OSCPUVirtualTimeMicroseconds, "CPU time spent seen by OS. Does not include involuntary waits due to virtualization.") \
    M(OSReadBytes, "Number of bytes read from disks or block devices. Doesn't include bytes read from page cache. May include excessive data due to block size, readahead, etc.") \
    M(OSWriteBytes, "Number of bytes written to disks or block devices. Doesn't include bytes that are in page cache dirty pages. May not include data that was written by OS asynchronously.") \
    M(OSReadChars, "Number of bytes read from filesystem, including page cache.") \
    M(OSWriteChars, "Number of bytes written to filesystem, including page cache.") \
    \
    M(PerfCpuCycles, "Total cycles. Be wary of what happens during CPU frequency scaling.")  \
    M(PerfInstructions, "Retired instructions. Be careful, these can be affected by various issues, most notably hardware interrupt counts.") \
    M(PerfCacheReferences, "Cache accesses. Usually this indicates Last Level Cache accesses but this may vary depending on your CPU. This may include prefetches and coherency messages; again this depends on the design of your CPU.") \
    M(PerfCacheMisses, "Cache misses. Usually this indicates Last Level Cache misses; this is intended to be used in con‐junction with the PERFCOUNTHWCACHEREFERENCES event to calculate cache miss rates.") \
    M(PerfBranchInstructions, "Retired branch instructions. Prior to Linux 2.6.35, this used the wrong event on AMD processors.") \
    M(PerfBranchMisses, "Mispredicted branch instructions.") \
    M(PerfBusCycles, "Bus cycles, which can be different from total cycles.") \
    M(PerfStalledCyclesFrontend, "Stalled cycles during issue.") \
    M(PerfStalledCyclesBackend, "Stalled cycles during retirement.") \
    M(PerfRefCpuCycles, "Total cycles; not affected by CPU frequency scaling.") \
    \
    M(PerfCpuClock, "The CPU clock, a high-resolution per-CPU timer") \
    M(PerfTaskClock, "A clock count specific to the task that is running") \
    M(PerfContextSwitches, "Number of context switches") \
    M(PerfCpuMigrations, "Number of times the process has migrated to a new CPU") \
    M(PerfAlignmentFaults, "Number of alignment faults. These happen when unaligned memory accesses happen; the kernel can handle these but it reduces performance. This happens only on some architectures (never on x86).") \
    M(PerfEmulationFaults, "Number of emulation faults. The kernel sometimes traps on unimplemented instructions and emulates them for user space. This can negatively impact performance.") \
    M(PerfMinEnabledTime, "For all events, minimum time that an event was enabled. Used to track event multiplexing influence") \
    M(PerfMinEnabledRunningTime, "Running time for event with minimum enabled time. Used to track the amount of event multiplexing") \
    M(PerfDataTLBReferences, "Data TLB references") \
    M(PerfDataTLBMisses, "Data TLB misses") \
    M(PerfInstructionTLBReferences, "Instruction TLB references") \
    M(PerfInstructionTLBMisses, "Instruction TLB misses") \
    M(PerfLocalMemoryReferences, "Local NUMA node memory reads") \
    M(PerfLocalMemoryMisses, "Local NUMA node memory read misses") \
    \
    M(CreatedHTTPConnections, "Total amount of created HTTP connections (closed or opened).") \
    \
    M(CannotWriteToWriteBufferDiscard, "Number of stack traces dropped by query profiler or signal handler because pipe is full or cannot write to pipe.") \
    M(QueryProfilerSignalOverruns, "Number of times we drop processing of a signal due to overrun plus the number of signals that OS has not delivered due to overrun.") \
    \
    M(CreatedLogEntryForMerge, "Successfully created log entry to merge parts in ReplicatedMergeTree.") \
    M(NotCreatedLogEntryForMerge, "Log entry to merge parts in ReplicatedMergeTree is not created due to concurrent log update by another replica.") \
    M(CreatedLogEntryForMutation, "Successfully created log entry to mutate parts in ReplicatedMergeTree.") \
    M(NotCreatedLogEntryForMutation, "Log entry to mutate parts in ReplicatedMergeTree is not created due to concurrent log update by another replica.") \
    \
    M(S3ReadMicroseconds, "Time of GET and HEAD requests to S3 storage.") \
    M(S3ReadBytes, "Read bytes (incoming) in GET and HEAD requests to S3 storage.") \
    M(S3ReadRequestsCount, "Number of GET and HEAD requests to S3 storage.") \
    M(S3ReadRequestsErrors, "Number of non-throttling errors in GET and HEAD requests to S3 storage.") \
    M(S3ReadRequestsThrottling, "Number of 429 and 503 errors in GET and HEAD requests to S3 storage.") \
    M(S3ReadRequestsRedirects, "Number of redirects in GET and HEAD requests to S3 storage.") \
    \
    M(S3WriteMicroseconds, "Time of POST, DELETE, PUT and PATCH requests to S3 storage.") \
    M(S3WriteBytes, "Write bytes (outgoing) in POST, DELETE, PUT and PATCH requests to S3 storage.") \
    M(S3WriteRequestsCount, "Number of POST, DELETE, PUT and PATCH requests to S3 storage.") \
    M(S3WriteRequestsErrors, "Number of non-throttling errors in POST, DELETE, PUT and PATCH requests to S3 storage.") \
    M(S3WriteRequestsThrottling, "Number of 429 and 503 errors in POST, DELETE, PUT and PATCH requests to S3 storage.") \
    M(S3WriteRequestsRedirects, "Number of redirects in POST, DELETE, PUT and PATCH requests to S3 storage.") \
    M(QueryMemoryLimitExceeded, "Number of times when memory limit exceeded for query.") \
    M(MarkBitmapIndexReadMicroseconds, "Total time spent in reading mark bitmap index.") \
    \
    M(SDRequest, "Number requests sent to SD") \
    M(SDRequestFailed, "Number requests sent to SD that failed") \
    M(SDRequestUpstream, "Number requests sent to SD upstream") \
    M(SDRequestUpstreamFailed, "Number requests sent to SD upstream that failed") \
    \
    M(WriteBufferFromHdfsWriteBytes, "")\
    M(HDFSWriteElapsedMilliseconds, "")\
    M(WriteBufferFromHdfsWrite, "")\
    M(WriteBufferFromHdfsWriteFailed, "")\
    M(HdfsFileOpen, "")\
    M(ReadBufferFromHdfsRead, "")\
    M(ReadBufferFromHdfsReadFailed, "")\
    M(ReadBufferFromHdfsReadBytes, "")\
    M(HDFSReadElapsedMilliseconds, "")\
    M(HDFSSeek, "")\
    M(HDFSSeekElapsedMicroseconds, "")\
    M(HdfsGetBlkLocMicroseconds, "Total number of millisecons spent to call getBlockLocations") \
    M(HdfsSlowNodeCount, "Total number of millisecons spent to call getBlockLocations") \
    M(HdfsFailedNodeCount, "Total number of millisecons spent to call getBlockLocations")     \
    \
    M(DiskCacheGetMicroSeconds, "Total time for disk cache get operation") \
    M(DiskCacheAcquireStatsLock, "Total time for acquire table stats lock") \
    M(DiskCacheScheduleCacheTaskMicroSeconds, "Total time for schedule disk cache task") \
    M(DiskCacheUpdateStatsMicroSeconds, "Total time for update disk cache statistics") \
    M(DiskCacheGetMetaMicroSeconds, "Total time for disk cache get operations") \
    M(DiskCacheGetTotalOps, "Total count of disk cache get operations") \
    M(DiskCacheSetTotalOps, "Total count of disk cache set operations") \
    \
    M(CnchTxnAborted, "Total number of aborted transactions (excludes preempting transactions)") \
    M(CnchTxnCommitted, "Total number of committed transactions") \
    M(CnchTxnExpired, "Total number of expired transactions") \
    M(CnchTxnReadTxnCreated, "Total number of read only transaction created") \
    M(CnchTxnWriteTxnCreated, "Total number of write transaction created") \
    M(CnchTxnCommitV1Failed, "Number of commitV1 failures") \
    M(CnchTxnCommitV2Failed, "Number of commitV2 failures") \
    M(CnchTxnCommitV1ElapsedMilliseconds, "Total number of milliseconds spent to commitV1") \
    M(CnchTxnCommitV2ElapsedMilliseconds, "Total number of milliseconds spent to commitV2") \
    M(CnchTxnPrecommitElapsedMilliseconds, "Total number of milliseconds spent to preempt tranasctions") \
    M(CnchTxnCommitKVElapsedMilliseconds, "Total number of milliseconds spent to commit transaction in catalog") \
    M(CnchTxnCleanFailed, "Number of times clean a transaction was failed") \
    M(CnchTxnCleanElapsedMilliseconds, "Total number of milliseconds spent to clean transactions") \
    M(CnchTxnAllTransactionRecord, "Total number of transaction records") \
    M(CnchTxnFinishedTransactionRecord, "Total number of finished transaction records") \
    \
    M(IntentLockElapsedMilliseconds, "Total time spent to acquire intent locks") \
    M(IntentLockWriteIntentElapsedMilliseconds, "Total time spent to write intents") \
    M(IntentLockPreemptionElapsedMilliseconds, "Total time spent to preempt conflict locks") \
    M(TsCacheCheckElapsedMilliseconds, "Total number of milliseconds spent to check in Timestamp cache") \
    M(TsCacheUpdateElapsedMilliseconds, "Total number of milliseconds spent to update in Timestamp cache") \
    M(CatalogConstructorSuccess, "") \
    M(CatalogConstructorFailed, "") \
    M(CatalogTime, "Total time spent getting data parts from Catalog") \
    M(TotalPartitions, "Number of total partitions") \
    M(PrunedPartitions, "Number of pruned partitions") \
    M(UpdateTableStatisticsSuccess, "") \
    M(UpdateTableStatisticsFailed, "") \
    M(GetTableStatisticsSuccess, "") \
    M(GetTableStatisticsFailed, "") \
    M(GetAvailableTableStatisticsTagsSuccess, "") \
    M(GetAvailableTableStatisticsTagsFailed, "") \
    M(RemoveTableStatisticsSuccess, "") \
    M(RemoveTableStatisticsFailed, "") \
    M(UpdateColumnStatisticsSuccess, "") \
    M(UpdateColumnStatisticsFailed, "") \
    M(GetColumnStatisticsSuccess, "") \
    M(GetColumnStatisticsFailed, "") \
    M(GetAvailableColumnStatisticsTagsSuccess, "") \
    M(GetAvailableColumnStatisticsTagsFailed, "") \
    M(RemoveColumnStatisticsSuccess, "") \
    M(RemoveColumnStatisticsFailed, "") \
    M(CreateDatabaseSuccess, "") \
    M(CreateDatabaseFailed, "") \
    M(GetDatabaseSuccess, "") \
    M(GetDatabaseFailed, "") \
    M(IsDatabaseExistsSuccess, "") \
    M(IsDatabaseExistsFailed, "") \
    M(DropDatabaseSuccess, "") \
    M(DropDatabaseFailed, "") \
    M(RenameDatabaseSuccess, "") \
    M(RenameDatabaseFailed, "") \
    M(CreateTableSuccess, "") \
    M(CreateTableFailed, "") \
    M(DropTableSuccess, "") \
    M(DropTableFailed, "") \
    M(CreateUDFSuccess, "") \
    M(CreateUDFFailed, "") \
    M(DropUDFSuccess, "") \
    M(DropUDFFailed, "") \
    M(DetachTableSuccess, "") \
    M(DetachTableFailed, "") \
    M(AttachTableSuccess, "") \
    M(AttachTableFailed, "") \
    M(IsTableExistsSuccess, "") \
    M(IsTableExistsFailed, "") \
    M(AlterTableSuccess, "") \
    M(AlterTableFailed, "") \
    M(RenameTableSuccess, "") \
    M(RenameTableFailed, "") \
    M(SetWorkerGroupForTableSuccess, "") \
    M(SetWorkerGroupForTableFailed, "") \
    M(GetTableSuccess, "") \
    M(GetTableFailed, "") \
    M(TryGetTableSuccess, "") \
    M(TryGetTableFailed, "") \
    M(TryGetTableByUUIDSuccess, "") \
    M(TryGetTableByUUIDFailed, "") \
    M(GetTableByUUIDSuccess, "") \
    M(GetTableByUUIDFailed, "") \
    M(GetTablesInDBSuccess, "") \
    M(GetTablesInDBFailed, "") \
    M(GetAllViewsOnSuccess, "") \
    M(GetAllViewsOnFailed, "") \
    M(SetTableActivenessSuccess, "") \
    M(SetTableActivenessFailed, "") \
    M(GetTableActivenessSuccess, "") \
    M(GetTableActivenessFailed, "") \
    M(GetServerDataPartsInPartitionsSuccess, "") \
    M(GetServerDataPartsInPartitionsFailed, "") \
    M(GetAllServerDataPartsSuccess, "") \
    M(GetAllServerDataPartsFailed, "") \
    M(GetDataPartsByNamesSuccess, "") \
    M(GetDataPartsByNamesFailed, "") \
    M(GetStagedDataPartsByNamesSuccess, "") \
    M(GetStagedDataPartsByNamesFailed, "") \
    M(GetAllDeleteBitmapsSuccess, "") \
    M(GetAllDeleteBitmapsFailed, "") \
    M(GetStagedPartsSuccess, "") \
    M(GetStagedPartsFailed, "") \
    M(GetDeleteBitmapsInPartitionsSuccess, "") \
    M(GetDeleteBitmapsInPartitionsFailed, "") \
    M(GetDeleteBitmapByKeysSuccess, "") \
    M(GetDeleteBitmapByKeysFailed, "") \
    M(AddDeleteBitmapsSuccess, "") \
    M(AddDeleteBitmapsFailed, "") \
    M(RemoveDeleteBitmapsSuccess, "") \
    M(RemoveDeleteBitmapsFailed, "") \
    M(FinishCommitSuccess, "") \
    M(FinishCommitFailed, "") \
    M(GetKafkaOffsetsVoidSuccess, "") \
    M(GetKafkaOffsetsVoidFailed, "") \
    M(GetKafkaOffsetsTopicPartitionListSuccess, "") \
    M(GetKafkaOffsetsTopicPartitionListFailed, "") \
    M(ClearOffsetsForWholeTopicSuccess, "") \
    M(ClearOffsetsForWholeTopicFailed, "") \
    M(DropAllPartSuccess, "") \
    M(DropAllPartFailed, "") \
    M(GetPartitionListSuccess, "") \
    M(GetPartitionListFailed, "") \
    M(GetPartitionsFromMetastoreSuccess, "") \
    M(GetPartitionsFromMetastoreFailed, "") \
    M(GetPartitionIDsSuccess, "") \
    M(GetPartitionIDsFailed, "") \
    M(CreateDictionarySuccess, "") \
    M(CreateDictionaryFailed, "") \
    M(GetCreateDictionarySuccess, "") \
    M(GetCreateDictionaryFailed, "") \
    M(DropDictionarySuccess, "") \
    M(DropDictionaryFailed, "") \
    M(DetachDictionarySuccess, "") \
    M(DetachDictionaryFailed, "") \
    M(AttachDictionarySuccess, "") \
    M(AttachDictionaryFailed, "") \
    M(GetDictionariesInDBSuccess, "") \
    M(GetDictionariesInDBFailed, "") \
    M(GetDictionarySuccess, "") \
    M(GetDictionaryFailed, "") \
    M(IsDictionaryExistsSuccess, "") \
    M(IsDictionaryExistsFailed, "") \
    M(TryLockPartInKVSuccess, "") \
    M(TryLockPartInKVFailed, "") \
    M(UnLockPartInKVSuccess, "") \
    M(UnLockPartInKVFailed, "") \
    M(TryResetAndLockConflictPartsInKVSuccess, "") \
    M(TryResetAndLockConflictPartsInKVFailed, "") \
    M(CreateTransactionRecordSuccess, "") \
    M(CreateTransactionRecordFailed, "") \
    M(RemoveTransactionRecordSuccess, "") \
    M(RemoveTransactionRecordFailed, "") \
    M(RemoveTransactionRecordsSuccess, "") \
    M(RemoveTransactionRecordsFailed, "") \
    M(GetTransactionRecordSuccess, "") \
    M(GetTransactionRecordFailed, "") \
    M(TryGetTransactionRecordSuccess, "") \
    M(TryGetTransactionRecordFailed, "") \
    M(SetTransactionRecordSuccess, "") \
    M(SetTransactionRecordFailed, "") \
    M(SetTransactionRecordWithRequestsSuccess, "") \
    M(SetTransactionRecordWithRequestsFailed, "") \
    M(SetTransactionRecordCleanTimeSuccess, "") \
    M(SetTransactionRecordCleanTimeFailed, "") \
    M(SetTransactionRecordStatusWithOffsetsSuccess, "") \
    M(SetTransactionRecordStatusWithOffsetsFailed, "") \
    M(RollbackTransactionSuccess, "") \
    M(RollbackTransactionFailed, "") \
    M(WriteIntentsSuccess, "") \
    M(WriteIntentsFailed, "") \
    M(TryResetIntentsIntentsToResetSuccess, "") \
    M(TryResetIntentsIntentsToResetFailed, "") \
    M(TryResetIntentsOldIntentsSuccess, "") \
    M(TryResetIntentsOldIntentsFailed, "") \
    M(ClearIntentsSuccess, "") \
    M(ClearIntentsFailed, "") \
    M(WritePartsSuccess, "") \
    M(WritePartsFailed, "") \
    M(SetCommitTimeSuccess, "") \
    M(SetCommitTimeFailed, "") \
    M(ClearPartsSuccess, "") \
    M(ClearPartsFailed, "") \
    M(WriteUndoBufferConstResourceSuccess, "") \
    M(WriteUndoBufferConstResourceFailed, "") \
    M(WriteUndoBufferNoConstResourceSuccess, "") \
    M(WriteUndoBufferNoConstResourceFailed, "") \
    M(ClearUndoBufferSuccess, "") \
    M(ClearUndoBufferFailed, "") \
    M(GetUndoBufferSuccess, "") \
    M(GetUndoBufferFailed, "") \
    M(GetAllUndoBufferSuccess, "") \
    M(GetAllUndoBufferFailed, "") \
    M(GetTransactionRecordsSuccess, "") \
    M(GetTransactionRecordsFailed, "") \
    M(GetTransactionRecordsTxnIdsSuccess, "") \
    M(GetTransactionRecordsTxnIdsFailed, "") \
    M(GetTransactionRecordsForGCSuccess, "") \
    M(GetTransactionRecordsForGCFailed, "") \
    M(ClearZombieIntentSuccess, "") \
    M(ClearZombieIntentFailed, "") \
    M(WriteFilesysLockSuccess, "") \
    M(WriteFilesysLockFailed, "") \
    M(GetFilesysLockSuccess, "") \
    M(GetFilesysLockFailed, "") \
    M(ClearFilesysLockDirSuccess, "") \
    M(ClearFilesysLockDirFailed, "") \
    M(ClearFilesysLockTxnIdSuccess, "") \
    M(ClearFilesysLockTxnIdFailed, "") \
    M(GetAllFilesysLockSuccess, "") \
    M(GetAllFilesysLockFailed, "") \
    M(InsertTransactionSuccess, "") \
    M(InsertTransactionFailed, "") \
    M(RemoveTransactionSuccess, "") \
    M(RemoveTransactionFailed, "") \
    M(GetActiveTransactionsSuccess, "") \
    M(GetActiveTransactionsFailed, "") \
    M(UpdateServerWorkerGroupSuccess, "") \
    M(UpdateServerWorkerGroupFailed, "") \
    M(GetWorkersInWorkerGroupSuccess, "") \
    M(GetWorkersInWorkerGroupFailed, "") \
    M(GetTableByIDSuccess, "") \
    M(GetTableByIDFailed, "") \
    M(GetTablesByIDSuccess, "") \
    M(GetTablesByIDFailed, "") \
    M(GetAllDataBasesSuccess, "") \
    M(GetAllDataBasesFailed, "") \
    M(GetAllTablesSuccess, "") \
    M(GetAllTablesFailed, "") \
    M(GetTrashTableIDIteratorSuccess, "") \
    M(GetTrashTableIDIteratorFailed, "") \
    M(GetAllUDFsSuccess, "") \
    M(GetAllUDFsFailed, "") \
    M(GetUDFByNameSuccess, "") \
    M(GetUDFByNameFailed, "") \
    M(GetTrashTableIDSuccess, "") \
    M(GetTrashTableIDFailed, "") \
    M(GetTablesInTrashSuccess, "") \
    M(GetTablesInTrashFailed, "") \
    M(GetDatabaseInTrashSuccess, "") \
    M(GetDatabaseInTrashFailed, "") \
    M(GetAllTablesIDSuccess, "") \
    M(GetAllTablesIDFailed, "") \
    M(GetTableIDByNameSuccess, "") \
    M(GetTableIDByNameFailed, "") \
    M(GetAllWorkerGroupsSuccess, "") \
    M(GetAllWorkerGroupsFailed, "") \
    M(GetAllDictionariesSuccess, "") \
    M(GetAllDictionariesFailed, "") \
    M(ClearDatabaseMetaSuccess, "") \
    M(ClearDatabaseMetaFailed, "") \
    M(ClearTableMetaForGCSuccess, "") \
    M(ClearTableMetaForGCFailed, "") \
    M(ClearDataPartsMetaSuccess, "") \
    M(ClearDataPartsMetaFailed, "") \
    M(ClearStagePartsMetaSuccess, "") \
    M(ClearStagePartsMetaFailed, "") \
    M(ClearDataPartsMetaForTableSuccess, "") \
    M(ClearDataPartsMetaForTableFailed, "") \
    M(GetSyncListSuccess, "") \
    M(GetSyncListFailed, "") \
    M(ClearSyncListSuccess, "") \
    M(ClearSyncListFailed, "") \
    M(GetServerPartsByCommitTimeSuccess, "") \
    M(GetServerPartsByCommitTimeFailed, "") \
    M(CreateRootPathSuccess, "") \
    M(CreateRootPathFailed, "") \
    M(DeleteRootPathSuccess, "") \
    M(DeleteRootPathFailed, "") \
    M(GetAllRootPathSuccess, "") \
    M(GetAllRootPathFailed, "") \
    M(CreateMutationSuccess, "") \
    M(CreateMutationFailed, "") \
    M(RemoveMutationSuccess, "") \
    M(RemoveMutationFailed, "") \
    M(GetAllMutationsStorageIdSuccess, "") \
    M(GetAllMutationsStorageIdFailed, "") \
    M(GetAllMutationsSuccess, "") \
    M(GetAllMutationsFailed, "") \
    M(SetTableClusterStatusSuccess, "") \
    M(SetTableClusterStatusFailed, "") \
    M(GetTableClusterStatusSuccess, "") \
    M(GetTableClusterStatusFailed, "") \
    M(IsTableClusteredSuccess, "") \
    M(IsTableClusteredFailed, "") \
    M(SetTablePreallocateVWSuccess, "") \
    M(SetTablePreallocateVWFailed, "") \
    M(GetTablePreallocateVWSuccess, "") \
    M(GetTablePreallocateVWFailed, "") \
    M(GetTablePartitionMetricsSuccess, "") \
    M(GetTablePartitionMetricsFailed, "") \
    M(GetTablePartitionMetricsFromMetastoreSuccess, "") \
    M(GetTablePartitionMetricsFromMetastoreFailed, "") \
    M(UpdateTopologiesSuccess, "") \
    M(UpdateTopologiesFailed, "") \
    M(GetTopologiesSuccess, "") \
    M(GetTopologiesFailed, "") \
    M(GetTrashDBVersionsSuccess, "") \
    M(GetTrashDBVersionsFailed, "") \
    M(UndropDatabaseSuccess, "") \
    M(UndropDatabaseFailed, "") \
    M(GetTrashTableVersionsSuccess, "") \
    M(GetTrashTableVersionsFailed, "") \
    M(UndropTableSuccess, "") \
    M(UndropTableFailed, "") \
    M(UpdateResourceGroupSuccess, "") \
    M(UpdateResourceGroupFailed, "") \
    M(GetResourceGroupSuccess, "") \
    M(GetResourceGroupFailed, "") \
    M(RemoveResourceGroupSuccess, "") \
    M(RemoveResourceGroupFailed, "") \
    M(GetInsertionLabelKeySuccess, "") \
    M(GetInsertionLabelKeyFailed, "") \
    M(PrecommitInsertionLabelSuccess, "") \
    M(PrecommitInsertionLabelFailed, "") \
    M(CommitInsertionLabelSuccess, "") \
    M(CommitInsertionLabelFailed, "") \
    M(TryCommitInsertionLabelSuccess, "") \
    M(TryCommitInsertionLabelFailed, "") \
    M(AbortInsertionLabelSuccess, "") \
    M(AbortInsertionLabelFailed, "") \
    M(GetInsertionLabelSuccess, "") \
    M(GetInsertionLabelFailed, "") \
    M(RemoveInsertionLabelSuccess, "") \
    M(RemoveInsertionLabelFailed, "") \
    M(RemoveInsertionLabelsSuccess, "") \
    M(RemoveInsertionLabelsFailed, "") \
    M(ScanInsertionLabelsSuccess, "") \
    M(ScanInsertionLabelsFailed, "") \
    M(ClearInsertionLabelsSuccess, "") \
    M(ClearInsertionLabelsFailed, "") \
    M(CreateVirtualWarehouseSuccess, "") \
    M(CreateVirtualWarehouseFailed, "") \
    M(AlterVirtualWarehouseSuccess, "") \
    M(AlterVirtualWarehouseFailed, "") \
    M(TryGetVirtualWarehouseSuccess, "") \
    M(TryGetVirtualWarehouseFailed, "") \
    M(ScanVirtualWarehousesSuccess, "") \
    M(ScanVirtualWarehousesFailed, "") \
    M(DropVirtualWarehouseSuccess, "") \
    M(DropVirtualWarehouseFailed, "") \
    M(CreateWorkerGroupSuccess, "") \
    M(CreateWorkerGroupFailed, "") \
    M(UpdateWorkerGroupSuccess, "") \
    M(UpdateWorkerGroupFailed, "") \
    M(TryGetWorkerGroupSuccess, "") \
    M(TryGetWorkerGroupFailed, "") \
    M(ScanWorkerGroupsSuccess, "") \
    M(ScanWorkerGroupsFailed, "") \
    M(DropWorkerGroupSuccess, "") \
    M(DropWorkerGroupFailed, "") \
    M(GetNonHostUpdateTimestampFromByteKVSuccess, "") \
    M(GetNonHostUpdateTimestampFromByteKVFailed, "") \
    M(MaskingPolicyExistsSuccess, "") \
    M(MaskingPolicyExistsFailed, "") \
    M(GetMaskingPoliciesSuccess, "") \
    M(GetMaskingPoliciesFailed, "") \
    M(PutMaskingPolicySuccess, "") \
    M(PutMaskingPolicyFailed, "") \
    M(TryGetMaskingPolicySuccess, "") \
    M(TryGetMaskingPolicyFailed, "") \
    M(GetMaskingPolicySuccess, "") \
    M(GetMaskingPolicyFailed, "") \
    M(GetAllMaskingPolicySuccess, "") \
    M(GetAllMaskingPolicyFailed, "") \
    M(GetMaskingPolicyAppliedTablesSuccess, "") \
    M(GetMaskingPolicyAppliedTablesFailed, "") \
    M(GetAllMaskingPolicyAppliedTablesSuccess, "") \
    M(GetAllMaskingPolicyAppliedTablesFailed, "") \
    M(DropMaskingPoliciesSuccess, "") \
    M(DropMaskingPoliciesFailed, "") \
    M(IsHostServerSuccess, "") \
    M(IsHostServerFailed, "") \
    M(CnchReadSizeFromDiskCache, "") \
    M(CnchReadSizeFromRemote, "") \
    M(CnchReadDataMicroSeconds,"") \
    M(CnchAddStreamsElapsedMilliseconds,"") \
    M(CnchAddStreamsParallelTasks,"") \
    M(CnchAddStreamsParallelElapsedMilliseconds,"") \
    M(CnchAddStreamsSequentialTasks,"") \
    M(CnchAddStreamsSequentialElapsedMilliseconds,"") \
    M(SetBGJobStatusSuccess, "") \
    M(SetBGJobStatusFailed, "") \
    M(GetBGJobStatusSuccess, "") \
    M(GetBGJobStatusFailed, "") \
    M(GetBGJobStatusesSuccess, "") \
    M(GetBGJobStatusesFailed, "") \
    M(DropBGJobStatusSuccess, "") \
    M(DropBGJobStatusFailed, "") \

namespace ProfileEvents
{

#define M(NAME, DOCUMENTATION) extern const Event NAME = __COUNTER__;
    APPLY_FOR_EVENTS(M)
#undef M
constexpr Event END = __COUNTER__;

/// Global variable, initialized by zeros.
Counter global_counters_array[END] {};
/// Initialize global counters statically
Counters global_counters(global_counters_array);

const Event Counters::num_counters = END;


Counters::Counters(VariableContext level_, Counters * parent_)
    : counters_holder(new Counter[num_counters] {}),
      parent(parent_),
      level(level_)
{
    counters = counters_holder.get();
}

void Counters::resetCounters()
{
    if (counters)
    {
        for (Event i = 0; i < num_counters; ++i)
            counters[i].store(0, std::memory_order_relaxed);
    }
}

void Counters::reset()
{
    parent = nullptr;
    resetCounters();
}

Counters Counters::getPartiallyAtomicSnapshot() const
{
    Counters res(VariableContext::Snapshot, nullptr);
    for (Event i = 0; i < num_counters; ++i)
        res.counters[i].store(counters[i].load(std::memory_order_relaxed), std::memory_order_relaxed);
    return res;
}

const char * getName(Event event)
{
    static const char * strings[] =
    {
    #define M(NAME, DOCUMENTATION) #NAME,
        APPLY_FOR_EVENTS(M)
    #undef M
    };

    return strings[event];
}

const char * getDocumentation(Event event)
{
    static const char * strings[] =
    {
    #define M(NAME, DOCUMENTATION) DOCUMENTATION,
        APPLY_FOR_EVENTS(M)
    #undef M
    };

    return strings[event];
}


Event end() { return END; }


void increment(Event event, Count amount)
{
    DB::CurrentThread::getProfileEvents().increment(event, amount);
}

}

#undef APPLY_FOR_EVENTS
