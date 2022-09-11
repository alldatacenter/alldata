/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.formats.base;

import java.util.Objects;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;
import org.apache.flink.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of {@link TableFormatDeserializer}.
 */
public class DefaultTableFormatDeserializer implements TableFormatDeserializer {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(DefaultTableFormatDeserializer.class);

    /**
     * The delegated deserialization schema for rows.
     */
    private final DeserializationSchema<Row> deserializationSchema;

    /**
     * True if ignore errors in the deserialization.
     */
    private final boolean ignoreErrors;

    public DefaultTableFormatDeserializer(
            DeserializationSchema<Row> deserializationSchema,
            boolean ignoreErrors
    ) {
        this.deserializationSchema = deserializationSchema;
        this.ignoreErrors = ignoreErrors;
    }

    public DefaultTableFormatDeserializer(
            DeserializationSchema<Row> deserializationSchema
    ) {
        this(deserializationSchema, TableFormatConstants.DEFAULT_IGNORE_ERRORS);
    }

    @Override
    public TypeInformation<Row> getProducedType() {
        return deserializationSchema.getProducedType();
    }

    @Override
    public void flatMap(
            byte[] bytes,
            Collector<Row> collector
    ) throws Exception {
        Row row;

        try {
            row = deserializationSchema.deserialize(bytes);
        } catch (Exception e) {
            if (ignoreErrors) {
                LOG.warn("Could not properly deserialize the data {}.",
                        StringUtils.byteToHexString(bytes), e);
                return;
            } else {
                throw e;
            }
        }

        if (row != null) {
            collector.collect(row);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultTableFormatDeserializer that = (DefaultTableFormatDeserializer) o;
        return ignoreErrors == that.ignoreErrors
               && Objects.equals(deserializationSchema, that.deserializationSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deserializationSchema, ignoreErrors);
    }

    @Override
    public String toString() {
        return "DefaultTableFormatDeserializer{"
               + "deserializationSchema=" + deserializationSchema
               + ", ignoreErrors=" + ignoreErrors
               + '}';
    }
}
