/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datart.data.provider;

import datart.core.data.provider.Dataframe;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class HttpDataFetcher {

    private static final HttpClient httpClient;

    private final HttpRequestParam param;

    static {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        try {
            // trust self-signed certificate and ignore hostname verification
            SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                    SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                    NoopHostnameVerifier.INSTANCE);
            httpClientBuilder.setSSLSocketFactory(scsf);
        } catch (Exception e) {
            log.warn("HttpClient config ssl failed, and used default config.");
        }
        httpClient = httpClientBuilder.build();
    }

    public HttpDataFetcher(HttpRequestParam param) {
        this.param = param;
    }

    public Dataframe fetchAndParse() throws IOException, URISyntaxException {

        HttpRequestBase httpRequest = createHttpRequest(param);

        HttpResponse response = httpClient.execute(httpRequest);

        HttpResponseParser parser;
        try {
            parser = param.getResponseParser().newInstance();
        } catch (Exception e) {
            parser = new ResponseJsonParser();
        }
        return parser.parseResponse(param.getTargetPropertyName(), response, param.getColumns());

    }

    private HttpRequestBase createHttpRequest(HttpRequestParam param) throws URISyntaxException {
        HttpRequestBase httpRequest;
        HttpEntity entity = createHttpEntity(param);
        switch (param.getMethod()) {
            case POST:
                HttpPost httpPost = new HttpPost();
                httpPost.setEntity(entity);
                httpRequest = httpPost;
                break;
            case PUT:
                HttpPut httpPut = new HttpPut();
                httpPut.setEntity(entity);
                httpRequest = httpPut;
                break;
            case DELETE:
                httpRequest = new HttpDelete();
                break;
            default:
                httpRequest = new HttpGet();
                break;
        }
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(param.getTimeout())
                .build();

        httpRequest.setConfig(config);

        httpRequest.setURI(createUri(param));

        withHeaders(param, httpRequest);

        if (StringUtils.isNotBlank(param.getUsername()) && StringUtils.isNotBlank(param.getPassword())) {
            String auth = param.getUsername() + ":" + param.getPassword();
            httpRequest.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.encodeBase64String(auth.getBytes(StandardCharsets.UTF_8)));
        }

        return httpRequest;
    }

    private HttpEntity createHttpEntity(HttpRequestParam param) {
        if (StringUtils.isEmpty(param.getBody())) {
            return null;
        }
        return new StringEntity(param.getBody(), ContentType.parse(param.getContentType()));
    }

    private URI createUri(HttpRequestParam param) throws URISyntaxException {

        URIBuilder uriBuilder = new URIBuilder(param.getUrl());

        if (!CollectionUtils.isEmpty(param.getQueryParam())) {
            for (Map.Entry<String, String> entry : param.getQueryParam().entrySet()) {
                uriBuilder.addParameter(entry.getKey(), entry.getValue());
            }
        }

        return uriBuilder.build();
    }

    private void withHeaders(HttpRequestParam param, HttpRequestBase httpRequest) {
        if (CollectionUtils.isEmpty(param.getHeaders())) return;
        for (Map.Entry<String, String> entry : param.getHeaders().entrySet()) {
            httpRequest.addHeader(entry.getKey(), entry.getValue());
        }
    }


}