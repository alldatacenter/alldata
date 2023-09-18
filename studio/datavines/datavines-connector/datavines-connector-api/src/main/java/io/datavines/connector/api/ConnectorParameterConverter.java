/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.connector.api;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Map;

public interface ConnectorParameterConverter {

    Map<String,Object> converter(Map<String,Object> parameter);

    default String getConnectorUUID(Map<String,Object> parameter) {
        Map<String, Object> convertResult = converter(parameter);
        return DigestUtils.md5Hex(
                String.valueOf(convertResult.get("url")) +
                convertResult.get("table") +
                convertResult.get("user") +
                convertResult.get("password"));
    }
}
