/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal.utils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * PropertyManager class to get localized resources.
 */
public class PropertyManager {
    private ResourceBundle bundle;

    PropertyManager(String baseName, Locale locale) {
        this.bundle = ResourceBundle.getBundle(baseName, locale);
    }

    public static PropertyManager getInstance(String baseName) {
        return new PropertyManager(baseName, Locale.getDefault());
    }

    public static PropertyManager getInstance(String baseName, Locale locale) {
        return new PropertyManager(baseName, locale);
    }

    public String getString(String key) {
        return bundle.getString(key);
    }

    public String getFormattedString(String key, Object... args) {
        return MessageFormat.format(getString(key), args);
    }
}
