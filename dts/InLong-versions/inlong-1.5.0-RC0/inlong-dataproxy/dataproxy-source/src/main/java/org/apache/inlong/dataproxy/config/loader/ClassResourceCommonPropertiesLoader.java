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

package org.apache.inlong.dataproxy.config.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class resource common properties loader
 */
public class ClassResourceCommonPropertiesLoader implements CommonPropertiesLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ClassResourceCommonPropertiesLoader.class);
    private static final String FILE_NAME = "common.properties";

    /**
     * load properties
     */
    @Override
    public Map<String, String> load() {
        return this.loadProperties();
    }

    protected Map<String, String> loadProperties() {
        Map<String, String> result = new ConcurrentHashMap<>();
        URL resource = getClass().getClassLoader().getResource(FILE_NAME);
        try (InputStream inStream = Objects.requireNonNull(resource).openStream()) {
            Properties props = new Properties();
            props.load(inStream);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                result.put((String) entry.getKey(), (String) entry.getValue());
            }
        } catch (Exception e) {
            LOG.error("fail to load properties from file ={}", FILE_NAME, e);
        }
        return result;
    }
}
