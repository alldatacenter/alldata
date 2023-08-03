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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;

/**
 * Inlong http request wrapper
 */
@Slf4j
public class InlongRequestWrapper extends HttpServletRequestWrapper {

    private static final ObjectMapper mapper = new ObjectMapper();

    private String bodyParams;

    private Map<String, String[]> params;

    private Map<String, String> headers;

    public InlongRequestWrapper(HttpServletRequest request) {
        super(request);
        this.bodyParams = HttpContextUtils.getBodyString(request);
        this.params = HttpContextUtils.getParameterMap(request);
        this.headers = HttpContextUtils.getHeaderMapAll(request);
    }

    @Override
    public String getParameter(String name) {
        String result;
        Object v = params.get(name);
        if (v == null) {
            result = null;
        } else if (v instanceof String[]) {
            String[] strArr = (String[]) v;
            if (strArr.length > 0) {
                result = strArr[0];
            } else {
                result = null;
            }
        } else if (v instanceof String) {
            result = (String) v;
        } else {
            result = v.toString();
        }
        return result;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return params;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] result;
        Object v = params.get(name);
        if (v == null) {
            result = null;
        } else if (v instanceof String[]) {
            result = (String[]) v;
        } else if (v instanceof String) {
            result = new String[]{(String) v};
        } else {
            result = new String[]{v.toString()};
        }
        return result;
    }

    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(bodyParams.getBytes(StandardCharsets.UTF_8));
        return new ServletInputStream() {

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // no op
            }

            @Override
            public int read() {
                return inputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(this.getInputStream()));
    }

    @Override
    public String getHeader(String name) {
        return this.headers.get(name.toLowerCase());
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return new Vector<>(params.keySet()).elements();
    }

    public void addHeader(String name, String value) {
        headers.put(name.toLowerCase(), value);
    }

    public void addBodyParam(String key, String value) throws JsonProcessingException {
        ObjectNode objectNode = (ObjectNode) mapper.readTree(bodyParams);
        objectNode.put(key, value);
        bodyParams = objectNode.toString();
    }

    public void addParameter(String name, String value) {
        params.put(name, new String[]{value});
    }

}
