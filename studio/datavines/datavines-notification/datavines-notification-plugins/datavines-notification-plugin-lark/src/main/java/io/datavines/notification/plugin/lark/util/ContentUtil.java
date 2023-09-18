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
package io.datavines.notification.plugin.lark.util;

import java.util.HashMap;
import java.util.Map;

public class ContentUtil {

    public static Map<String, String> createParamMap(Object... elements) {
        Map<String, String> paramMap = new HashMap<>(32);
        if (elements.length % 2 == 1) {
            throw new RuntimeException("params length must be even!");
        }
        for (int i = 0; i < elements.length / 2; i++) {
            Object key = elements[2 * i];
            Object value = elements[2 * i + 1];
            if (key == null) {
                continue;
            }
            paramMap.put(key.toString(), value == null ? "" : value.toString());
        }
        return paramMap;
    }
}
