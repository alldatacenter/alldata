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

package org.apache.inlong.sort.cdc.mongodb.source.offset;

import static com.ververica.cdc.connectors.mongodb.source.utils.MongoRecordUtils.bsonTimestampFromEpochMillis;
import static com.ververica.cdc.connectors.mongodb.source.utils.MongoRecordUtils.currentBsonTimestamp;

import java.util.Map;
import org.apache.inlong.sort.cdc.base.source.meta.offset.OffsetFactory;

/** An change stream offset factory class create {@link ChangeStreamOffset} instance.
 * Copy from com.ververica:flink-connector-mongodb-cdc:2.3.0.
 */
public class ChangeStreamOffsetFactory extends OffsetFactory {

    @Override
    public ChangeStreamOffset newOffset(Map<String, String> offset) {
        return new ChangeStreamOffset(offset);
    }

    @Override
    public ChangeStreamOffset newOffset(String filename, Long position) {
        throw new UnsupportedOperationException(
                "not supported create new Offset by filename and position.");
    }

    @Override
    public ChangeStreamOffset newOffset(Long position) {
        return new ChangeStreamOffset(bsonTimestampFromEpochMillis(position));
    }

    @Override
    public ChangeStreamOffset createInitialOffset() {
        return new ChangeStreamOffset(currentBsonTimestamp());
    }

    @Override
    public ChangeStreamOffset createNoStoppingOffset() {
        return ChangeStreamOffset.NO_STOPPING_OFFSET;
    }
}
