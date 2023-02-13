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

package org.apache.inlong.common.util;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Basic authentication utility
 */
public class BasicAuth {

    public static final String BASIC_AUTH_HEADER = "Authorization";
    public static final String BASIC_AUTH_PREFIX = "Basic";
    public static final String BASIC_AUTH_SEPARATOR = " ";
    public static final String BASIC_AUTH_JOINER = ":";
    public static final String BASIC_AUTH_EMPTY = "";

    /**
     * Generate http basic auth credential from configured secretId and secretKey
     */
    public static String genBasicAuthCredential(String secretId, String secretKey) {
        if (StringUtils.isBlank(secretId) || StringUtils.isBlank(secretKey)) {
            return BASIC_AUTH_EMPTY;
        }
        String credential = String.join(BASIC_AUTH_JOINER, secretId, secretKey);
        return BASIC_AUTH_PREFIX + BASIC_AUTH_SEPARATOR + Base64.getEncoder()
                .encodeToString(credential.getBytes(StandardCharsets.UTF_8));
    }
}
