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

package org.apache.inlong.sort.cdc.base.source.meta.split;

import io.debezium.relational.TableId;
import io.debezium.relational.history.TableChanges.TableChange;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.inlong.sort.cdc.base.source.meta.offset.Offset;

/** The state of split to describe the change log of table(s).
 * Copy from com.ververica:flink-cdc-base:2.3.0.
 * */
public class StreamSplitState extends SourceSplitState {

    @Nullable
    private Offset startingOffset;
    @Nullable
    private Offset endingOffset;
    private final Map<TableId, TableChange> tableSchemas;

    public StreamSplitState(StreamSplit split) {
        super(split);
        this.startingOffset = split.getStartingOffset();
        this.endingOffset = split.getEndingOffset();
        this.tableSchemas = split.getTableSchemas();
    }

    @Nullable
    public Offset getStartingOffset() {
        return startingOffset;
    }

    public void setStartingOffset(@Nullable Offset startingOffset) {
        this.startingOffset = startingOffset;
    }

    @Nullable
    public Offset getEndingOffset() {
        return endingOffset;
    }

    public void setEndingOffset(@Nullable Offset endingOffset) {
        this.endingOffset = endingOffset;
    }

    public Map<TableId, TableChange> getTableSchemas() {
        return tableSchemas;
    }

    public void recordSchema(TableId tableId, TableChange latestTableChange) {
        this.tableSchemas.put(tableId, latestTableChange);
    }

    @Override
    public StreamSplit toSourceSplit() {
        final StreamSplit streamSplit = split.asStreamSplit();
        return new StreamSplit(
                streamSplit.splitId(),
                getStartingOffset(),
                getEndingOffset(),
                streamSplit.asStreamSplit().getFinishedSnapshotSplitInfos(),
                getTableSchemas(),
                streamSplit.getTotalFinishedSplitSize());
    }

    @Override
    public String toString() {
        return "StreamSplitState{"
                + "startingOffset="
                + startingOffset
                + ", endingOffset="
                + endingOffset
                + ", split="
                + split
                + '}';
    }
}
