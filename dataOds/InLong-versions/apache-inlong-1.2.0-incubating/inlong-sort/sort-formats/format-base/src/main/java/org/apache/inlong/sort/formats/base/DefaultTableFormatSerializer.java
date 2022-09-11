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

import static org.apache.inlong.sort.formats.base.TableFormatConstants.DEFAULT_IGNORE_ERRORS;

import java.util.Objects;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of {@link TableFormatSerializer}.
 */
public class DefaultTableFormatSerializer implements TableFormatSerializer {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(DefaultTableFormatSerializer.class);

    /**
     * The delegated serialization schema for rows.
     */
    private final SerializationSchema<Row> serializationSchema;

    /**
     * True if ignore errors in the serialization.
     */
    private final boolean ignoreErrors;

    public DefaultTableFormatSerializer(
            SerializationSchema<Row> serializationSchema,
            boolean ignoreErrors
    ) {
        this.serializationSchema = serializationSchema;
        this.ignoreErrors = ignoreErrors;
    }

    public DefaultTableFormatSerializer(
            SerializationSchema<Row> serializationSchema
    ) {
        this(serializationSchema, DEFAULT_IGNORE_ERRORS);
    }

    @Override
    public void flatMap(
            Row row,
            Collector<byte[]> collector
    ) throws Exception {
        byte[] bytes;

        try {
            bytes = serializationSchema.serialize(row);
        } catch (Exception e) {
            if (ignoreErrors) {
                LOG.warn("Could not properly serialize the row {}.", row, e);
                return;
            } else {
                throw e;
            }
        }

        collector.collect(bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultTableFormatSerializer that = (DefaultTableFormatSerializer) o;
        return ignoreErrors == that.ignoreErrors
                       && Objects.equals(serializationSchema, that.serializationSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serializationSchema, ignoreErrors);
    }

    @Override
    public String toString() {
        return "DefaultTableFormatSerializer{"
                + "serializationSchema=" + serializationSchema
                + ", ignoreErrors=" + ignoreErrors
                + '}';
    }
}
