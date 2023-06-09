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
package com.qlangtech.tis.manage.common;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-9-6
 */
public class TISHttpServletRequestWrapper extends HttpServletRequestWrapper {

    public TISHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    private AppDomainInfo domain;

    @Override
    public void setAttribute(String name, Object o) {
        try {
            if (ActionTool.REQUEST_DOMAIN_KEY.equals(name) && (domain == null)) {
                this.domain = (AppDomainInfo) o;
            } else {
                super.setAttribute(name, o);
            }
        } catch (Exception e) {
            throw new RuntimeException("name:" + name + ",value:" + o, e);
        }
    }

    private Map<String, Cookie> cookieMap;

    public Cookie getCookie(String name) {
        return getCookieMap().get(name);
    // return cookieMap.get(name);
    }

    private Map<String, Cookie> getCookieMap() {
        if (cookieMap == null) {
            synchronized (this) {
                if (cookieMap == null) {
                    cookieMap = new HashMap<String, Cookie>();
                    Cookie[] cs = this.getCookies();
                    if (cs != null) {
                        for (Cookie c : cs) {
                            cookieMap.put(c.getName(), c);
                        }
                    }
                }
            }
        }
        return cookieMap;
    }

    private byte[] bodyContent;

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (bodyContent != null) {
            return new TisServletInputStream(bodyContent);
        }
        ServletInputStream inputStream = super.getInputStream();
        if (inputStream.isFinished()) {
            return inputStream;
        }
        try (ByteArrayOutputStream array = new ByteArrayOutputStream()) {
            IOUtils.copy(inputStream, array);
            this.bodyContent = array.toByteArray();
        }
        return new TisServletInputStream(bodyContent);
    }

    private static class TisServletInputStream extends ServletInputStream {

        private final InputStream input;

        boolean finished = false;

        public TisServletInputStream(byte[] content) {
            this.input = new ByteArrayInputStream(content);
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
        }

        @Override
        public int read() throws IOException {
            int result;
            if ((result = this.input.read()) < 0) {
                finished = true;
            }
            return result;
        }

        @Override
        public void close() throws IOException {
            this.input.close();
        }
    }

    public void removeCookie(String cookiekey) {
        getCookieMap().remove(cookiekey);
    }

    // @Override
    // public ServletInputStream getInputStream() throws IOException {
    //
    // return super.getInputStream();
    // }
    //
    // @Override
    // public BufferedReader getReader() throws IOException {
    //
    // return super.getReader();
    // }
    @Override
    public String[] getParameterValues(String name) {
        String[] params = super.getParameterValues(name);
        if (params != null) {
            return params;
        }
        List<String> result = new ArrayList<String>();
        int i = 0;
        String value = null;
        while (StringUtils.isNotBlank(value = this.getParameter(name + '[' + (i++) + ']'))) {
            result.add(value);
        }
        return result.toArray(new String[] {});
    }

    @Override
    public Object getAttribute(String name) {
        if (ActionTool.REQUEST_DOMAIN_KEY.equals(name)) {
            return this.domain;
        } else {
            return super.getAttribute(name);
        }
    }
}
