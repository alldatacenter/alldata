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

package org.apache.inlong.agent.conf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.utils.AgentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Base configuration, store and parse config params.
 */
public abstract class AbstractConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfiguration.class);
    private static final JsonParser JSON_PARSER = new JsonParser();

    private final Map<String, JsonPrimitive> configStorage = new HashMap<>();

    /**
     * get config file by class loader
     **/
    private ClassLoader classLoader;

    public AbstractConfiguration() {
        classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = AgentConfiguration.class.getClassLoader();
        }
    }

    /**
     * Check whether all required keys exist.
     *
     * @return true if all key exist else false.
     */
    public abstract boolean allRequiredKeyExist();

    /**
     * support load config file from json/properties file.
     *
     * @param fileName file name
     * @param isJson whether is json file
     */
    private void loadResource(String fileName, boolean isJson) {
        Reader reader = null;
        try {
            InputStream inputStream = classLoader.getResourceAsStream(fileName);
            if (inputStream != null) {
                reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                if (isJson) {
                    JsonElement tmpElement = JSON_PARSER.parse(reader).getAsJsonObject();
                    updateConfig(new HashMap<>(10), 0, tmpElement);
                } else {
                    Properties properties = new Properties();
                    properties.load(reader);
                    properties.forEach((key, value) -> configStorage.put((String) key,
                            new JsonPrimitive((String) value)));
                }
            }
        } catch (Exception ioe) {
            LOGGER.error("error init {}", fileName, ioe);
        } finally {
            AgentUtils.finallyClose(reader);
        }
    }

    /**
     * load config from json string.
     *
     * @param jsonStr json string
     */
    public void loadJsonStrResource(String jsonStr) {
        JsonElement tmpElement = JSON_PARSER.parse(jsonStr);
        updateConfig(new HashMap<>(10), 0, tmpElement);
    }

    /**
     * load config file from CLASS_PATH. config file is json file.
     *
     * @param fileName file name
     */
    void loadJsonResource(String fileName) {
        loadResource(fileName, true);
    }

    void loadPropertiesResource(String fileName) {
        loadResource(fileName, false);
    }

    /**
     * Convert json string to map
     *
     * @param keyDeptPath map
     * @param dept json dept
     * @param tmpElement json element
     */
    void updateConfig(HashMap<Integer, String> keyDeptPath, int dept, JsonElement tmpElement) {
        if (tmpElement instanceof JsonObject) {
            JsonObject tmpJsonObject = tmpElement.getAsJsonObject();
            for (String key : tmpJsonObject.keySet()) {
                keyDeptPath.put(dept, key);
                updateConfig(keyDeptPath, dept + 1, tmpJsonObject.get(key));
            }
        } else if (tmpElement instanceof JsonArray) {
            JsonArray tmpJsonArray = tmpElement.getAsJsonArray();
            String lastKey = keyDeptPath.getOrDefault(dept - 1, "");
            for (int index = 0; index < tmpJsonArray.size(); index++) {
                keyDeptPath.put(dept - 1, lastKey + "[" + index + "]");
                updateConfig(keyDeptPath, dept, tmpJsonArray.get(index));
            }
        } else if (tmpElement instanceof JsonPrimitive) {
            List<String> builder = new ArrayList<>();
            for (int index = 0; index < dept; index++) {
                builder.add(keyDeptPath.getOrDefault(index, ""));
            }
            String keyChain = StringUtils.join(builder, ".");
            if (!StringUtils.isBlank(keyChain)) {
                configStorage.put(keyChain, tmpElement.getAsJsonPrimitive());
            }
        }
    }

    /**
     * get int from config
     *
     * @param key key
     * @param defaultValue default value
     * @return value
     */
    public int getInt(String key, int defaultValue) {
        JsonElement value = configStorage.get(key);
        return value == null ? defaultValue : value.getAsInt();
    }

    /**
     * get int from config
     *
     * @param key key
     * @return value
     * @throws NullPointerException npe
     */
    public int getInt(String key) {
        JsonElement value = configStorage.get(key);
        if (value == null) {
            throw new NullPointerException("null value for key " + key);
        }
        return value.getAsInt();
    }

    /**
     * get long
     *
     * @param key key
     * @param defaultValue default value
     * @return long
     */
    public long getLong(String key, long defaultValue) {
        JsonElement value = configStorage.get(key);
        return value == null ? defaultValue : value.getAsLong();
    }

    /**
     * get boolean
     *
     * @param key key
     * @param defaultValue default value
     * @return boolean
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        JsonElement value = configStorage.get(key);
        return value == null ? defaultValue : value.getAsBoolean();
    }

    public float getFloat(String key, float defaultValue) {
        JsonElement value = configStorage.get(key);
        return value == null ? defaultValue : value.getAsFloat();
    }

    /**
     * get string
     *
     * @param key key
     * @param defaultValue default value
     * @return string
     */
    public String get(String key, String defaultValue) {
        JsonElement value = configStorage.get(key);
        return value == null ? defaultValue : value.getAsString();
    }

    /**
     * get string or throw npe
     *
     * @param key key
     * @return string
     * @throws NullPointerException if value is null, throw npe
     */
    public String get(String key) {
        JsonElement value = configStorage.get(key);
        if (value == null) {
            throw new NullPointerException("null value for key " + key);
        }
        return value.getAsString();
    }

    /**
     * whether key exists
     *
     * @param key key
     * @return true if key exists else not
     */
    public boolean hasKey(String key) {
        return configStorage.containsKey(key);
    }

    /**
     * set key/value
     *
     * @param key key
     * @param value value
     */
    public void set(String key, String value) {
        if (value != null) {
            configStorage.put(key, new JsonPrimitive(value));
            return;
        }

        // del
        if (configStorage.containsKey(key)) {
            configStorage.remove(key);
        }
    }

    public void setInt(String key, int value) {
        configStorage.put(key, new JsonPrimitive(value));
    }

    public void setLong(String key, long value) {
        configStorage.put(key, new JsonPrimitive(value));
    }

    public void setBoolean(String key, boolean value) {
        configStorage.put(key, new JsonPrimitive(value));
    }

    /**
     * get key-value map properties from config file
     */
    public Map<String, String> getConfigProperties() {
        Map<String, String> properties = new HashMap<>();
        for (Map.Entry<String, JsonPrimitive> entry : configStorage.entrySet()) {
            properties.put(entry.getKey(), entry.getValue().getAsString());
        }
        return properties;
    }

    Map<String, JsonPrimitive> getConfigStorage() {
        return configStorage;
    }

    public URL getConfigLocation(String fileName) {
        return classLoader.getResource(fileName);
    }

    /**
     * get configStorage list, item format: "key=value"
     */
    List<String> getStorageList() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, JsonPrimitive> entry : configStorage.entrySet()) {
            result.add(entry.getKey() + "=" + entry.getValue().getAsString());
        }
        return result;
    }
}
