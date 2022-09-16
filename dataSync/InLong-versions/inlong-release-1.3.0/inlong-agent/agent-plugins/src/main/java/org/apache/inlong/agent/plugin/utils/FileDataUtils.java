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

package org.apache.inlong.agent.plugin.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * File job utils
 */
public class FileDataUtils {

    public static final String KUBERNETES_LOG = "log";

    private static final Gson GSON = new Gson();

    /**
     * Get standard log for k8s
     */
    public static String getK8sJsonLog(String log, Boolean isJson) {
        if (!StringUtils.isNoneBlank(log)) {
            return null;
        }
        if (!isJson) {
            return log;
        }
        Type type = new TypeToken<HashMap<String, String>>() {
        }.getType();
        Map<String, String> logJson = GSON.fromJson(log, type);
        return logJson.getOrDefault(KUBERNETES_LOG, log);
    }

    /**
     * To judge json
     */
    public static boolean isJSON(String json) {
        boolean isJson;
        try {
            JsonObject convertedObject = new Gson().fromJson(json, JsonObject.class);
            isJson = convertedObject.isJsonObject();
        } catch (Exception exception) {
            return false;
        }
        return isJson;
    }

}
