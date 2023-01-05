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
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class MockHttpURLConnection extends HttpURLConnection {

    private final InputStream inputStream;

    private final Map<String, List<String>> headerFields;

    public MockHttpURLConnection(InputStream inputStream) {
        this(inputStream, Collections.emptyMap());
    }

    public MockHttpURLConnection(InputStream inputStream, Map<String, List<String>> headerFields) {
        super(null);
        if (inputStream == null) {
            throw new IllegalStateException("param inputStream can not be null");
        }
        this.inputStream = inputStream;
        this.headerFields = headerFields;
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        return this.headerFields;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return this.inputStream;
    }

    @Override
    public void disconnect() {
        IOUtils.closeQuietly(this.inputStream);
    }

    @Override
    public boolean usingProxy() {
        return false;
    }

    @Override
    public void connect() throws IOException {
    }
}
