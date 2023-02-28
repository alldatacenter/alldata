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
package io.datavines.connector.plugin;

import io.datavines.common.utils.StringUtils;
import io.datavines.connector.api.ConnectorParameterConverter;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.HashMap;
import java.util.Map;

public class PrestoConnectorParameterConverter implements ConnectorParameterConverter {

    @Override
    public Map<String, Object> converter(Map<String, Object> parameter) {
        Map<String,Object> config = new HashMap<>();
        config.put("table", parameter.get("table"));
        config.put("user", parameter.get("user"));
        config.put("password", parameter.get("password"));
        String database = (String)parameter.get("database");
        if (StringUtils.isNotEmpty(database)) {
            config.put("url", String.format("jdbc:presto://%s:%s/%s/%s",
                    parameter.get("host"),
                    parameter.get("port"),
                    parameter.get("catalog"),
                    parameter.get("database")));
        } else {
            config.put("url", String.format("jdbc:presto://%s:%s/%s",
                    parameter.get("host"),
                    parameter.get("port"),
                    parameter.get("catalog")));
        }

        return config;
    }
}
