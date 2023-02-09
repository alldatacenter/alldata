/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.

 * According to cos feature, we modify some class，comment, field name, etc.
 */
package com.qcloud.cos.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.util.Map;

/**
 * Connect the specified endpoint
 */
public class ConnectionUtils {
    private static final int CONNECT_TIMEOUT = 5 * 1000;
    private static final int READ_TIMEOUT = 10 * 1000;
    private static final String DEFAULT_HTTP_METHOD = "GET";

    private static ConnectionUtils instance;

    public static ConnectionUtils getInstance() {
        if (null == instance) {
            ConnectionUtils.instance = new ConnectionUtils();
        }

        return instance;
    }

    private ConnectionUtils() {
    }

    public HttpURLConnection connectToEndpoint(URI endpoint, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) endpoint.toURL().openConnection(Proxy.NO_PROXY);
        connection.setConnectTimeout(ConnectionUtils.CONNECT_TIMEOUT);
        connection.setReadTimeout(ConnectionUtils.READ_TIMEOUT);
        connection.setRequestMethod(ConnectionUtils.DEFAULT_HTTP_METHOD);
        connection.setDoOutput(true);

        for (Map.Entry<String, String> header : headers.entrySet()) {
            connection.addRequestProperty(header.getKey(), header.getValue());
        }

        connection.connect();

        return connection;
    }
}
