/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.util;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonUtilsV2 {

    static private final ThreadLocal<ObjectMapper> mapper = new ThreadLocal<ObjectMapper>() {
        @Override
        protected ObjectMapper initialValue() {
            return new ObjectMapper();
        }
    };

    static public ObjectMapper getMapper() {
        return mapper.get();
    }

    static public Map<String, String> jsonToMap(String jsonStr) throws Exception {
        final Map<String, String> ret;

        if (jsonStr == null || jsonStr.isEmpty()) {
            ret = new HashMap<>();
        } else {
            ret = getMapper().readValue(jsonStr, new TypeReference<Map<String, String>>() {});
        }

        return ret;
    }

    static public String mapToJson(Map<?, ?> map) throws Exception {
        return getMapper().writeValueAsString(map);
    }

    static public String listToJson(List<?> list) throws Exception {
        return getMapper().writeValueAsString(list);
    }

    static public String objToJson(Serializable obj) throws Exception {
        return getMapper().writeValueAsString(obj);
    }

    static public <T> T jsonToObj(String json, Class<T> tClass) throws Exception {
        return getMapper().readValue(json, tClass);
    }

}
