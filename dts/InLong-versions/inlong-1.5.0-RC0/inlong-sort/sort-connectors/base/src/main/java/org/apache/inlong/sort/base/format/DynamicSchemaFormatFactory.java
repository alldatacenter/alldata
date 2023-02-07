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

package org.apache.inlong.sort.base.format;

import com.google.common.collect.ImmutableMap;
import org.apache.flink.util.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Dynamic schema format factory
 */
public class DynamicSchemaFormatFactory {

    public static Map<String, Function<Map<String, String>, AbstractDynamicSchemaFormat>> SUPPORT_FORMATS =
            ImmutableMap.of(
                    "canal-json", props -> new CanalJsonDynamicSchemaFormat(props),
                    "debezium-json", props -> new DebeziumJsonDynamicSchemaFormat(props));

    /**
     * Get format from the format name, it only supports [canal-json|debezium-json] for now
     *
     * @param identifier The identifier of this format
     * @return The dynamic format
     */
    @SuppressWarnings("rawtypes")
    public static AbstractDynamicSchemaFormat getFormat(String identifier) {
        return getFormat(identifier, new HashMap<>());
    }

    /**
     * Get format from the format name, it only supports [canal-json|debezium-json] for now
     *
     * @param identifier The identifier of this format
     * @return The dynamic format
     */
    @SuppressWarnings("rawtypes")
    public static AbstractDynamicSchemaFormat getFormat(String identifier, Map<String, String> properties) {
        Preconditions.checkNotNull(identifier, "The identifier is null");
        return Optional.ofNullable(SUPPORT_FORMATS.get(identifier))
                .orElseThrow(
                        () -> new UnsupportedOperationException("Unsupport dynamic schema format for:" + identifier))
                .apply(properties);
    }
}
