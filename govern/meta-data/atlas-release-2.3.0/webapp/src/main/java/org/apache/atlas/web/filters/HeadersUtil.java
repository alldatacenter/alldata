/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.web.filters;

import org.apache.atlas.AtlasConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
@Component
public class HeadersUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HeadersUtil.class);

    public static final Map<String, String> headerMap = new HashMap<>();

    public static final String X_FRAME_OPTIONS_KEY = "X-Frame-Options";
    public static final String X_CONTENT_TYPE_OPTIONS_KEY = "X-Content-Type-Options";
    public static final String X_XSS_PROTECTION_KEY = "X-XSS-Protection";
    public static final String STRICT_TRANSPORT_SEC_KEY = "Strict-Transport-Security";
    public static final String CONTENT_SEC_POLICY_KEY = "Content-Security-Policy";

    public static final String X_FRAME_OPTIONS_VAL = "DENY";
    public static final String X_CONTENT_TYPE_OPTIONS_VAL = "nosniff";
    public static final String X_XSS_PROTECTION_VAL = "1; mode=block";
    public static final String STRICT_TRANSPORT_SEC_VAL = "max-age=31536000; includeSubDomains";
    public static final String CONTENT_SEC_POLICY_VAL = "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' blob: data:; connect-src 'self'; img-src 'self' blob: data:; style-src 'self' 'unsafe-inline';font-src 'self' data:";
    public static final String SERVER_KEY = "Server";
    public static final String USER_AGENT_KEY = "User-Agent";
    public static final String USER_AGENT_VALUE = "Mozilla";
    public static final String X_REQUESTED_WITH_KEY = "X-REQUESTED-WITH";
    public static final String X_REQUESTED_WITH_VALUE = "XMLHttpRequest";
    public static final int SC_AUTHENTICATION_TIMEOUT = 419;

    HeadersUtil() {
        headerMap.put(X_FRAME_OPTIONS_KEY, X_FRAME_OPTIONS_VAL);
        headerMap.put(X_CONTENT_TYPE_OPTIONS_KEY, X_CONTENT_TYPE_OPTIONS_VAL);
        headerMap.put(X_XSS_PROTECTION_KEY, X_XSS_PROTECTION_VAL);
        headerMap.put(STRICT_TRANSPORT_SEC_KEY, STRICT_TRANSPORT_SEC_VAL);
        headerMap.put(CONTENT_SEC_POLICY_KEY, CONTENT_SEC_POLICY_VAL);
        headerMap.put(SERVER_KEY, AtlasConfiguration.HTTP_HEADER_SERVER_VALUE.getString());
    }

    public static void setHeaderMapAttributes(AtlasResponseRequestWrapper responseWrapper, String headerKey) {
        responseWrapper.setHeader(headerKey, headerMap.get(headerKey));

    }

    public static void setSecurityHeaders(AtlasResponseRequestWrapper responseWrapper) {
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            responseWrapper.setHeader(entry.getKey(), entry.getValue());
        }
    }
}
