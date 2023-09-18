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
package io.datavines.common.config;

import java.util.Properties;

public class Configurations {

    private final Properties configuration;

    public Configurations() {
        configuration = new Properties();
    }

    public Configurations(Properties configuration) {
        this.configuration = configuration;
    }

    public String getString(String key){
        return configuration.getProperty(key);
    }

    public String getString(String key,String defaultValue){
        return configuration.getProperty(key,defaultValue);
    }

    public int getInt(String key){
        return Integer.parseInt(configuration.getProperty(key));
    }

    public int getInt(String key,String defaultValue) {
        return Integer.parseInt(configuration.getProperty(key,defaultValue));
    }

    public int getInt(String key,Integer defaultValue) {
        return Integer.parseInt(configuration.getProperty(key,String.valueOf(defaultValue)));
    }

    public Float getFloat(String key) {
        return Float.valueOf(configuration.getProperty(key));
    }

    public Float getFloat(String key,String defaultValue) {
        return Float.valueOf(configuration.getProperty(key,defaultValue));
    }
}
