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

package org.apache.inlong.sort.doris.table;

import org.apache.commons.lang3.StringUtils;
import org.apache.doris.flink.exception.StreamLoadException;
import org.apache.doris.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.doris.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.doris.shaded.org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.inlong.sort.doris.model.RespContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * DorisStreamLoad copy from {@link org.apache.doris.flink.table.DorisStreamLoad}
 **/
public class DorisStreamLoad implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(org.apache.doris.flink.table.DorisStreamLoad.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final List<String> DORIS_SUCCESS_STATUS = new ArrayList<>(
            Arrays.asList("Success", "Publish Timeout"));
    private static final String LOAD_URL_PATTERN = "http://%s/api/%s/%s/_stream_load";
    private final String authEncoding;
    private final Properties streamLoadProp;
    private final CloseableHttpClient httpClient;
    private String hostPort;

    public DorisStreamLoad(String hostPort, String user, String passwd,
            Properties streamLoadProp) {
        this.hostPort = hostPort;
        this.authEncoding = basicAuthHeader(user, passwd);
        this.streamLoadProp = streamLoadProp;
        HttpClientBuilder httpClientBuilder = HttpClients
                .custom()
                .setRedirectStrategy(new DefaultRedirectStrategy() {

                    @Override
                    protected boolean isRedirectable(String method) {
                        return true;
                    }
                });
        this.httpClient = httpClientBuilder.build();
    }

    public RespContent load(String db, String tbl, String value) throws StreamLoadException {
        LoadResponse loadResponse = loadBatch(db, tbl, value);
        LOG.info("Streamload Response:{}", loadResponse);
        if (loadResponse.status != 200) {
            throw new StreamLoadException("stream load error: " + loadResponse.respContent);
        } else {
            try {
                RespContent respContent = OBJECT_MAPPER.readValue(loadResponse.respContent, RespContent.class);
                if (!DORIS_SUCCESS_STATUS.contains(respContent.getStatus())) {
                    String errMsg = String.format("stream load error: %s, see more in %s", respContent.getMessage(),
                            respContent.getErrorURL());
                    throw new StreamLoadException(errMsg);
                }
                return respContent;
            } catch (IOException e) {
                throw new StreamLoadException(e);
            }
        }
    }

    public String getLoadUrlStr(String db, String tbl) {
        return String.format(LOAD_URL_PATTERN, hostPort, db, tbl);
    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }

    private LoadResponse loadBatch(String db, String tbl, String value) {
        String label = streamLoadProp.getProperty("label");
        if (StringUtils.isBlank(label)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String formatDate = sdf.format(new Date());
            label = String.format("flink_connector_%s_%s", formatDate,
                    UUID.randomUUID().toString().replaceAll("-", ""));
        }

        try {
            final String loadUrlStr = String.format(LOAD_URL_PATTERN, hostPort, db, tbl);
            LOG.info("Streamload Url:{}", loadUrlStr);
            HttpPut put = new HttpPut(loadUrlStr);
            put.setHeader(HttpHeaders.EXPECT, "100-continue");
            put.setHeader(HttpHeaders.AUTHORIZATION, this.authEncoding);
            put.setHeader("label", label);
            for (Map.Entry<Object, Object> entry : streamLoadProp.entrySet()) {
                if (entry.getValue() != null) {
                    put.setHeader(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
            if (!put.containsHeader("format")) {
                put.setHeader("format", "json");
            }
            put.setHeader("strip_outer_array", "true");
            StringEntity entity = new StringEntity(value, "UTF-8");
            put.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(put)) {
                final int statusCode = response.getStatusLine().getStatusCode();
                final String reasonPhrase = response.getStatusLine().getReasonPhrase();
                String loadResult = "";
                if (response.getEntity() != null) {
                    loadResult = EntityUtils.toString(response.getEntity());
                }
                return new LoadResponse(statusCode, reasonPhrase, loadResult);
            }
        } catch (Exception e) {
            String err = "failed to stream load data with label: " + label;
            LOG.warn(err, e);
            return new LoadResponse(-1, e.getMessage(), err);
        }
    }

    private String basicAuthHeader(String username, String password) {
        final String tobeEncode = username + ":" + password;
        byte[] encoded = Base64.encodeBase64(tobeEncode.getBytes(StandardCharsets.UTF_8));
        return "Basic " + new String(encoded);
    }

    public void close() throws IOException {
        if (null != httpClient) {
            try {
                httpClient.close();
            } catch (IOException e) {
                LOG.error("Closing httpClient failed.", e);
                throw new RuntimeException("Closing httpClient failed.", e);
            }
        }
    }

    public static class LoadResponse {

        public int status;
        public String respMsg;
        public String respContent;

        public LoadResponse(int status, String respMsg, String respContent) {
            this.status = status;
            this.respMsg = respMsg;
            this.respContent = respContent;
        }

        @Override
        public String toString() {
            try {
                return OBJECT_MAPPER.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "";
            }
        }
    }
}
