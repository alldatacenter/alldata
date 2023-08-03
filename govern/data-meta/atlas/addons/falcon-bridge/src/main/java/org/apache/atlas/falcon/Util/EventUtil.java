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

package org.apache.atlas.falcon.Util;

import org.apache.commons.lang3.StringUtils;
import org.apache.falcon.FalconException;
import org.apache.falcon.security.CurrentUser;

import java.util.HashMap;
import java.util.Map;

/**
 * Falcon event util
 */
public final class EventUtil {

    private EventUtil() {}


    public static Map<String, String> convertKeyValueStringToMap(final String keyValueString) {
        if (StringUtils.isBlank(keyValueString)) {
            return null;
        }

        Map<String, String> keyValueMap = new HashMap<>();

        String[] tags = keyValueString.split(",");
        for (String tag : tags) {
            int index = tag.indexOf("=");
            String tagKey = tag.substring(0, index).trim();
            String tagValue = tag.substring(index + 1, tag.length()).trim();
            keyValueMap.put(tagKey, tagValue);
        }
        return keyValueMap;
    }

    public static String getUser() throws FalconException {
        try {
            return CurrentUser.getAuthenticatedUGI().getShortUserName();
        } catch (Exception ioe) {
            //Ignore is failed to get user, uses login user
        }
        return null;
    }
}
