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

#include <DataStreams/CountingBlockOutputStream.h>
#include <Common/ProfileEvents.h>


namespace ProfileEvents
{
    extern const Event InsertedRows;
    extern const Event InsertedBytes;
}


namespace DB
{

void CountingBlockOutputStream::write(const Block & block)
{
    stopwatch.start();

    stream->write(block);

    Progress local_progress(block.rows(), block.bytes(), 0, stopwatch.elapsedMilliseconds());
    progress.incrementPiecewiseAtomically(local_progress);

    ProfileEvents::increment(ProfileEvents::InsertedRows, local_progress.read_rows);
    ProfileEvents::increment(ProfileEvents::InsertedBytes, local_progress.read_bytes);

    if (process_elem)
        process_elem->updateProgressOut(local_progress);

    if (progress_callback)
        progress_callback(local_progress);
}

void CountingBlockOutputStream::writePrefix()
{
    stopwatch.start();

    stream->writePrefix();

    Progress local_progress(0, 0, 0, stopwatch.elapsedMilliseconds());
    progress.incrementPiecewiseAtomically(local_progress);

    /// Update the write duration to progress
    if (process_elem)
        process_elem->updateProgressOut(local_progress);

    if (progress_callback)
        progress_callback(local_progress);
}

void CountingBlockOutputStream::writeSuffix()
{
    stopwatch.start();

    stream->writeSuffix();

    Progress local_progress(0, 0, 0, stopwatch.elapsedMilliseconds());
    progress.incrementPiecewiseAtomically(local_progress);

    /// Update the write duration to progress
    if (process_elem)
        process_elem->updateProgressOut(local_progress);

    if (progress_callback)
        progress_callback(local_progress);
}


}
