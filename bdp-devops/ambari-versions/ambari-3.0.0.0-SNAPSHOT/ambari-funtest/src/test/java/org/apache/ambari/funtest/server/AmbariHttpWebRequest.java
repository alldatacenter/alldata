/*
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

package org.apache.ambari.funtest.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.RequestLine;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.lang.StringBuilder;

import org.apache.commons.lang.StringUtils;

/**
 * REST API Client that performs various operations on the Ambari Server
 * using Http.
 */
public class AmbariHttpWebRequest extends WebRequest {
    private static final Log LOG = LogFactory.getLog(AmbariHttpWebRequest.class);
    private static String SERVER_URL_FORMAT = "http://%s:%d";
    private static String SERVER_SSL_URL_FORMAT = "https://%s:%d";

    private String content;

    private String serverName;
    private int serverApiPort;
    private int serverAgentPort;
    private WebResponse response;
    private String curlApi;

    /**
     *  Constructor to set the REST API URL, Server API port, Server Agent API port, user id and password.
     *
     * @param params
     */
    public AmbariHttpWebRequest(ConnectionParams params) {
        setServerName(params.getServerName());
        setServerApiPort(params.getServerApiPort());
        setServerAgentPort(params.getServerAgentPort());
        setUserName(params.getUserName());
        setPassword(params.getPassword());
        addHeader("X-Requested-By", "ambari");
        if (getUserName() != null) {
            addHeader("Authorization", getBasicAuthentication());
        }
    }

    /**
     * Sends the request to the Ambari server and returns the response.
     *
     * @return - Response from the Ambari server.
     * @throws IOException
     */
    @Override
    public WebResponse getResponse() throws IOException {
        if (response == null) {
            LOG.info(getCurlApi());
            response = executeRequest();
        }

        return response;
    }

    /**
     * Gets the full URI
     *
     * @return - Full path to the URI
     */
    @Override
    public String getUrl() {
        return getServerApiUrl() + getApiPath();
    }

    /**
     * Gets the JSON content (request data).
     *
     * @return - JSON content.
     */
    @Override
    public String getContent() {
        if (content == null) {
            content = getRequestData();
        }

        return content;
    }

    /**
     * Gets the content encoding.
     *
     * @return - Content encoding.
     */
    @Override
    public String getContentEncoding() { return "UTF-8"; }

    /**
     * Gets the content type, like application/json, application/text, etc.
     *
     * @return - Content type.
     */
    @Override
    public String getContentType() { return "application/json"; }

    /**
     * Gets the curl command line call for this request. Useful for
     * debugging.
     *
     * @return - Curl command line call.
     */
    public String getCurlApi() {
        if (curlApi == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("curl");
            if (getUserName() != null) {
                sb.append(String.format(" -u %s", getUserName()));
                if (getPassword() != null) {
                    sb.append(String.format(":%s", getPassword()));
                }
            }
            sb.append(String.format(" -H \"%s\"", "X-Requested-By: ambari"));
            sb.append(String.format(" -X %s", getHttpMethod()));
            if (getHttpMethod().equals("PUT") || getHttpMethod().equals("POST")) {
                if (getContent() != null) {
                    sb.append(String.format(" -d '%s'", getContent()));
                }
            }
            sb.append(String.format(" %s", getUrl()));
            curlApi = sb.toString();
        }

        return curlApi;
    }

    /**
     * Sets the REST API URL for the Ambari Server
     *
     * @param serverName - REST API URL
     */
    public void setServerName(String serverName) { this.serverName = serverName; }

    /**
     * Gets the REST API URL for the Ambari Server
     *
     * @return - REST API URL
     */
    public String getServerName() { return this.serverName; }

    /**
     * Gets the port number for the REST API used by the web clients.
     *
     * @return - Server API port number.
     */
    public int getServerApiPort() { return this.serverApiPort; }

    /**
     * Sets the port number for the REST API used by the web clients.
     *
     * @param serverApiPort - Server API port.
     */
    public void setServerApiPort(int serverApiPort) { this.serverApiPort = serverApiPort; }

    /**
     * Gets the port number for the REST API used by the agent.
     *
     * @return - Agent API port number.
     */
    public int getServerAgentPort() { return this.serverAgentPort; }

    /**
     * Sets the port number for the REST API used by the agent.
     *
     * @param serverAgentPort - Agent API port number.
     */
    public void setServerAgentPort(int serverAgentPort) { this.serverAgentPort = serverAgentPort; }

    /**
     * Gets the REST API path fragment.
     *
     * @return - REST API path.
     */
    protected String getApiPath() { return ""; }

    /**
     * Gets the request data used in POST and PUT requests.
     *
     * @return - Request data.
     */
    protected String getRequestData() { return ""; }

    /**
     * Gets the basic authentication string to be used for the Authorization header.
     *
     * @return - Base-64 encoded authentication string.
     */
    protected String getBasicAuthentication() {
        String authString = getUserName() + ":" + getPassword();
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        String authStringEnc = new String(authEncBytes);

        return "Basic " + authStringEnc;
    }

    /**
     * Gets URL for the Ambari Server (without the API path)
     *
     * @return - Ambari server URL.
     */
    protected String getServerApiUrl() {
        return String.format(SERVER_URL_FORMAT, getServerName(), getServerApiPort());
    }

    /**
     * Gets URL for the Agent Server (without the API path)
     *
     * @return - Agent server URL.
     */
    protected String getServerAgentUrl() {
        return String.format(SERVER_URL_FORMAT, getServerName(), getServerAgentPort());
    }

    /**
     * Helper method to create simple Json objects.
     *
     * @param name - Name
     * @param value - Value
     * @return - A JsonObject {name: value}
     */
    protected static JsonObject createJsonObject(String name, String value) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(name, value);
        return jsonObject;
    }

    /**
     * Helper method to create simple Json objects.
     *
     * @param name - Name
     * @param jsonElement - Json object.
     * @return - A JsonObject {name: jsonElement }
     */
    protected static JsonObject createJsonObject(String name, JsonElement jsonElement) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(name, jsonElement);
        return jsonObject;
    }

    /**
     * Executes the current request by using HttpClient methods and returns the response.
     *
     * @return - Response from the Ambari server/Agent server.
     * @throws IOException
     */
    private WebResponse executeRequest() throws IOException {
        final WebResponse response = new WebResponse();
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            HttpRequestBase requestBase = null;
            String httpMethod = getHttpMethod();

            if (httpMethod.equals("GET")) {
                requestBase = new HttpGet(getRequestUrl());
            } else if (httpMethod.equals("POST")) {
                HttpPost httpPost = new HttpPost(getRequestUrl());
                if (StringUtils.isNotEmpty(getContent())) {
                    httpPost.setHeader("Content-Type", getContentType());
                    httpPost.setEntity(new StringEntity(getContent(), getContentEncoding()));
                }
                requestBase = httpPost;
            } else if (httpMethod.equals("PUT")) {
                HttpPut httpPut = new HttpPut(getRequestUrl());
                if (StringUtils.isNotEmpty(getContent())) {
                    httpPut.setHeader("Content-Type", getContentType());
                    httpPut.setEntity(new StringEntity(getContent(), getContentEncoding()));
                }
                requestBase = httpPut;
            } else if (httpMethod.equals("DELETE")) {
                requestBase = new HttpDelete(getRequestUrl());
            } else {
                throw new RuntimeException(String.format("Unsupported HTTP method: %s", httpMethod));
            }

            Map<String, String> headers = getHeaders();

            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBase.addHeader(header.getKey(), header.getValue());
            }

            RequestLine requestLine = requestBase.getRequestLine();
            LOG.debug(requestLine);

            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                @Override
                public String handleResponse(final HttpResponse httpResponse)
                        throws ClientProtocolException, IOException {
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    response.setStatusCode(statusCode);
                    HttpEntity entity = httpResponse.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                }
            };

            String responseBody = httpClient.execute(requestBase, responseHandler);
            response.setContent(responseBody);
        } finally {
            httpClient.close();
        }

        return response;
    }

    private String getRequestUrl() {
        StringBuilder requestUrl = new StringBuilder(getUrl());

        if (StringUtils.isNotEmpty(getQueryString())) {
            requestUrl.append("?");
            requestUrl.append(getQueryString());
        }

        return requestUrl.toString();
    }
}