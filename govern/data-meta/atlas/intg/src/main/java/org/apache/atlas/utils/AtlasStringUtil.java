/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.utils;

import java.util.Map;
import java.util.Objects;

public class AtlasStringUtil {
    public static boolean hasOption(Map<String, String> options, String optionName) {
        return options != null && options.containsKey(optionName);
    }

    public static String getOption(Map<String, String> options, String optionName) {
        return options != null ? options.get(optionName) : null;
    }

    public static boolean getBooleanOption(Map<String, String> options, String optionName) {
        return options != null && Boolean.parseBoolean(options.get(optionName));
    }

    public static boolean getBooleanOption(Map<String, String> options, String optionName, boolean defaultValue) {
        return (options != null && options.containsKey(optionName)) ? Boolean.parseBoolean(options.get(optionName)) : defaultValue;
    }

    public static boolean optionEquals(Map<String, String> options, String optionName, String value) {
        return Objects.equals(options != null ? options.get(optionName) : null, value);
    }
}
