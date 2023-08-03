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

package org.apache.inlong.manager.pojo.sort.node.base;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Interface of the node provider
 */
public interface NodeProvider {

    /**
     * Determines whether the current instance matches the specified type.
     *
     * @param streamType the specified type
     * @return whether the current instance matches the specified type
     */
    Boolean accept(String streamType);

    /**
     * Parse properties
     *
     * @param properties The properties with string key and object value
     * @return The properties with string key and string value
     */
    default Map<String, String> parseProperties(Map<String, Object> properties) {
        return properties.entrySet().stream()
                .filter(v -> Objects.nonNull(v.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    }
}
