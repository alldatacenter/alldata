/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.cdc.base.source.reader.external;

import java.util.Iterator;
import javax.annotation.Nullable;
import org.apache.flink.annotation.Experimental;
import org.apache.inlong.sort.cdc.base.source.meta.split.SnapshotSplit;
import org.apache.inlong.sort.cdc.base.source.meta.split.StreamSplit;

/**
 * Fetcher to fetch data of a table split, the split is either snapshot split {@link SnapshotSplit}
 * or stream split {@link StreamSplit}.
 * Copy from com.ververica:flink-cdc-base:2.3.0.
 */
@Experimental
public interface Fetcher<T, Split> {

    /** Add to task to fetch, this should call only when the reader is idle. */
    void submitTask(FetchTask<Split> fetchTask);

    /**
     * Fetched records from data source. The method should return null when reaching the end of the
     * split, the empty {@link Iterator} will be returned if the data of split is on pulling.
     */
    @Nullable
    Iterator<T> pollSplitRecords() throws InterruptedException;

    /** Return the current fetch task is finished or not. */
    boolean isFinished();

    /** Close the client and releases all resources. */
    void close();
}
