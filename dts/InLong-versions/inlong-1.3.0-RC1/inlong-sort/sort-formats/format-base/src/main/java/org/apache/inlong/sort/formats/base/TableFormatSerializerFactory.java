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

import java.util.Map;
import org.apache.flink.table.factories.TableFormatFactory;
import org.apache.flink.types.Row;

/**
 * Factory for creating configured instances of {@link TableFormatSerializer}.
 */
public interface TableFormatSerializerFactory extends TableFormatFactory<Row> {

    /**
     * Creates and configures a {@link TableFormatSerializer} using the given
     * properties.
     *
     * @param properties The normalized properties describing the format.
     * @return The configured serialization schema or null if the factory cannot
     *         provide an instance of the class.
     */
    TableFormatSerializer createFormatSerializer(
            final Map<String, String> properties
    );
}
