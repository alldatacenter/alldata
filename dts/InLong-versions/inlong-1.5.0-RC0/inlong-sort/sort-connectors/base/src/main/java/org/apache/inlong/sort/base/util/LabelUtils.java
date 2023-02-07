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

package org.apache.inlong.sort.base.util;

import java.util.LinkedHashMap;
import java.util.Map;
import static org.apache.inlong.sort.base.Constants.DELIMITER;
import static org.apache.inlong.sort.base.Constants.KEY_VALUE_DELIMITER;

/**
 * The lable utils class, it is used for parse the lables to a label map
 */
public final class LabelUtils {

    private LabelUtils() {
    }

    /**
     * Parse the labels to label map
     *
     * @param labels The labels format by 'key1=value1&key2=value2...'
     * @return The label map of labels
     */
    public static Map<String, String> parseLabels(String labels) {
        return parseLabels(labels, new LinkedHashMap<>());
    }

    /**
     * Parse the labels to label map
     *
     * @param labels The labels format by 'key1=value1&key2=value2...'
     * @return The label map of labels
     */
    public static Map<String, String> parseLabels(String labels, Map<String, String> labelMap) {
        if (labelMap == null) {
            labelMap = new LinkedHashMap<>();
        }
        if (labels == null || labels.length() == 0) {
            return labelMap;
        }
        String[] labelArray = labels.split(DELIMITER);
        for (String label : labelArray) {
            int index = label.indexOf(KEY_VALUE_DELIMITER);
            if (index < 1 || index == label.length() - 1) {
                throw new IllegalArgumentException("The format of labels must be like 'key1=value1&key2=value2...'");
            }
            labelMap.put(label.substring(0, index), label.substring(index + 1));
        }
        return labelMap;
    }
}
