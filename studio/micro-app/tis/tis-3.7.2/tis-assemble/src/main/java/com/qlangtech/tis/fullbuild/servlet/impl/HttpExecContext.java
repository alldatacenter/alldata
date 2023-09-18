/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.fullbuild.servlet.impl;

import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.order.center.IParamContext;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年12月11日 下午12:11:27
 */
public class HttpExecContext implements IParamContext {

    private static final Logger logger = LoggerFactory.getLogger(HttpExecContext.class);

    private final Map<String, String> params;

    @Override
    public ExecutePhaseRange getExecutePhaseRange() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("all")
    public HttpExecContext(HttpServletRequest request, Map<String, String> params, boolean parseHeaders) {
        super();
        this.params = params;
        String key = null;
        Enumeration en = request.getParameterNames();
        while (en.hasMoreElements()) {
            key = String.valueOf(en.nextElement());
            if (!params.containsKey(key)) {
                params.put(key, request.getParameter(key));
            }
        }
        if (parseHeaders) {
            String headKeyName = null;
            Enumeration headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                headKeyName = (String) headerNames.nextElement();
                params.put(headKeyName, request.getHeader(headKeyName));
            }
        }
    }

    @SuppressWarnings("all")
    public HttpExecContext(HttpServletRequest request) {
        this(request, new HashMap<String, String>(), false);
    }

    @Override
    public String getString(String key) {
        String value = params.get(key);
        // logger.info("httprequest key:" + key + ",value:" + value);
        return value;
    }

    @Override
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(this.getString(key));
    }

    @Override
    public int getInt(String key) {
        String val = this.getString(key);
        if (StringUtils.isEmpty(val)) {
            throw new IllegalArgumentException("key:" + key + " relevant val in request.params can not be find");
        }
        return Integer.parseInt(val);
    }

    @Override
    public long getLong(String key) {
        return Long.parseLong(this.getString(key));
    }

    @Override
    public long getPartitionTimestampWithMillis() {
        throw new UnsupportedOperationException();
    }
}
