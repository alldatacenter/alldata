/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.utils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Manager class to get localized resources.
 */
public class ResourceManager {
    private ResourceBundle bundle;

    ResourceManager(String baseName, Locale locale) {
        this.bundle = ResourceBundle.getBundle(baseName, locale);
    }

    public static ResourceManager getInstance(String baseName) {
        return new ResourceManager(baseName, Locale.getDefault());
    }

    public static ResourceManager getInstance(String baseName, Locale locale) {
        return new ResourceManager(baseName, locale);
    }

    public String getString(String key) {
        return bundle.getString(key);
    }

    public String getFormattedString(String key, Object... args) {
        return MessageFormat.format(getString(key), args);
    }
}
