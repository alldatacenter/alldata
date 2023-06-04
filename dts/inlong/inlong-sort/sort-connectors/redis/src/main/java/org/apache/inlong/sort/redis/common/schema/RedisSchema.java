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

package org.apache.inlong.sort.redis.common.schema;

import java.io.Serializable;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink.Context;
import org.apache.flink.table.data.RowData;

public interface RedisSchema<T> extends Serializable {

    /**
     * Validate the schema.
     */
    void validate(ResolvedSchema resolvedSchema);

    /**
     * The position of redis rowKey.
     */
    int getKeyIndex();

    /**
     * The start position of redis value.
     */
    int getValueIndex();

    /**
     * The serialization schema for parsing rowData to
     */
    SerializationSchema<RowData> getSerializationSchema(Context context,
            EncodingFormat<SerializationSchema<RowData>> format);

    /**
     * The {@link StateEncoder} for encoding row to stateType.
     */
    StateEncoder<T> getStateEncoder();
}
