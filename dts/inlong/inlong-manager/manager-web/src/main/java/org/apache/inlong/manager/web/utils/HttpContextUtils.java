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

package org.apache.inlong.manager.web.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * HttpContextUtils
 */
@Slf4j
public class HttpContextUtils {

    /**
     * Get request params
     * @param request
     * @return
     */
    public static Map<String, String> getParameterMapAll(ServletRequest request) {
        Enumeration<String> parameters = request.getParameterNames();

        Map<String, String> params = new HashMap<>();
        while (parameters.hasMoreElements()) {
            String parameter = parameters.nextElement();
            String value = request.getParameter(parameter);
            params.put(parameter, value);
        }
        return params;
    }

    public static Map<String, String[]> getParameterMap(HttpServletRequest request) {
        Map<String, String[]> paramMap = new HashMap<>();
        String queryString = request.getQueryString();
        if (StringUtils.isNotBlank(queryString)) {
            String[] params = queryString.split("&");
            for (int i = 0; i < params.length; i++) {
                int splitIndex = params[i].indexOf("=");
                if (splitIndex == -1) {
                    continue;
                }
                String key = params[i].substring(0, splitIndex);
                if (!paramMap.containsKey(key)) {
                    if (splitIndex < params[i].length()) {
                        String value = params[i].substring(splitIndex + 1);
                        paramMap.put(key, new String[]{value});
                    }
                }
            }
        }
        return paramMap;
    }

    public static Map<String, String> getHeaderMapAll(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        Map<String, String> headers = new HashMap<>();
        while (headerNames.hasMoreElements()) {
            String parameter = headerNames.nextElement();
            String value = request.getHeader(parameter);
            headers.put(parameter, value);
        }
        return headers;
    }

    /**
     * Get request body
     * @param request
     * @return
     */
    public static String getBodyString(ServletRequest request) {
        StringBuilder builder = new StringBuilder();
        try (InputStream inputStream = request.getInputStream()) {
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line = "";
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            }
        } catch (IOException e) {
            log.error("failed to get body string of request={}", request, e);
        }
        if (builder.length() == 0) {
            return "{}";
        }
        return builder.toString();
    }

}