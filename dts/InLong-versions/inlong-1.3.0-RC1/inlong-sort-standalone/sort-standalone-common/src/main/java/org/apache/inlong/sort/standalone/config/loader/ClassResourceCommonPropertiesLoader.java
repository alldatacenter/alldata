/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.standalone.config.loader;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;

/**
 * 
 * FileCommonPropertiesLoader
 */
public class ClassResourceCommonPropertiesLoader implements CommonPropertiesLoader {

    public static final Logger LOG = InlongLoggerFactory.getLogger(ClassResourceCommonPropertiesLoader.class);
    public static final String FILENAME = "common.properties";

    /**
     * load
     * 
     * @return
     */
    @Override
    public Map<String, String> load() {
        return this.loadProperties(FILENAME);
    }

    /**
     * loadProperties
     * 
     * @param  fileName
     * @return
     */
    protected Map<String, String> loadProperties(String fileName) {
        Map<String, String> result = new ConcurrentHashMap<>();
        try (InputStream inStream = getClass().getClassLoader().getResource(fileName).openStream()) {
            Properties props = new Properties();
            props.load(inStream);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                result.put((String) entry.getKey(), (String) entry.getValue());
            }
        } catch (UnsupportedEncodingException e) {
            LOG.error("fail to load properties, file ={}, and e= {}", fileName, e);
        } catch (Exception e) {
            LOG.error("fail to load properties, file ={}, and e= {}", fileName, e);
        }
        return result;
    }
}
