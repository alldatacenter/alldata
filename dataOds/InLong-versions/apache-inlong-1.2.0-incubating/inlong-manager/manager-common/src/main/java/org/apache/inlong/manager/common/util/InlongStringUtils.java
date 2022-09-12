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

package org.apache.inlong.manager.common.util;

import org.apache.commons.lang3.StringUtils;

/**
 * String utils for inlong.
 */
public class InlongStringUtils {

    /**
     * Trim the first and last char if its equals to the given element.
     */
    public static String trimFirstAndLastChar(String origin, String replacement) {
        origin = origin.replaceAll(" ", "");
        if (StringUtils.isBlank(origin)) {
            return origin;
        }

        int length;
        boolean beginFlag;
        boolean endFlag;
        do {
            int first = origin.indexOf(replacement);
            int last = origin.lastIndexOf(replacement);
            length = origin.length();
            int beginIndex = (first == 0) ? 1 : 0;
            int endIndex = (last + 1 == length) ? last : length;
            origin = origin.substring(beginIndex, endIndex);

            beginFlag = (first == 0);
            endFlag = (last + 1 == length);
        } while (beginFlag || endFlag);

        return origin;
    }

}
