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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.kafka.connect.source.SourceRecord;

/** Data structure to describe a set of {@link SourceRecord}.
 * Copy from com.ververica:flink-cdc-base:2.3.0.
 * */
public final class SourceRecords {

    private final List<SourceRecord> sourceRecords;

    public SourceRecords(List<SourceRecord> sourceRecords) {
        this.sourceRecords = sourceRecords;
    }

    public List<SourceRecord> getSourceRecordList() {
        return sourceRecords;
    }

    public Iterator<SourceRecord> iterator() {
        return sourceRecords.iterator();
    }

    public static com.ververica.cdc.connectors.base.source.meta.split.SourceRecords fromSingleRecord(
            SourceRecord record) {
        final List<SourceRecord> records = new ArrayList<>();
        records.add(record);
        return new com.ververica.cdc.connectors.base.source.meta.split.SourceRecords(records);
    }
}
