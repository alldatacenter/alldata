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

package org.apache.inlong.manager.client.api.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.inner.client.ClientFactory;
import org.apache.inlong.manager.client.api.service.AuthInterceptor;
import org.apache.inlong.manager.common.auth.Authentication;
import org.apache.inlong.manager.common.auth.DefaultAuthentication;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Utils for client
 */
@Slf4j
@UtilityClass
public class ClientUtils {

    private static final String REQUEST_FAILED_MSG = "Request to Inlong %s failed: %s";

    private static ClientFactory clientFactory;

    /**
     * Get factory for {@link org.apache.inlong.manager.client.api.inner.client}.
     *
     * @param configuration client configuration
     * @return ClientFactory
     */
    public static ClientFactory getClientFactory(ClientConfiguration configuration) {
        return Optional.ofNullable(clientFactory).orElse(new ClientFactory(configuration));
    }

    /**
     * Get retrofit to instantiate Client API.
     *
     * @param configuration client configuration
     * @return Retrofit
     */
    public static Retrofit createRetrofit(ClientConfiguration configuration) {
        String host = configuration.getBindHost();
        int port = configuration.getBindPort();

        Authentication authentication = configuration.getAuthentication();
        Preconditions.expectNotNull(authentication, "inlong should be authenticated");
        Preconditions.expectTrue(authentication instanceof DefaultAuthentication,
                "inlong only support default authentication");
        DefaultAuthentication defaultAuthentication = (DefaultAuthentication) authentication;

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(
                        new AuthInterceptor(defaultAuthentication.getUsername(), defaultAuthentication.getPassword()))
                .connectTimeout(configuration.getConnectTimeout(), configuration.getTimeUnit())
                .readTimeout(configuration.getReadTimeout(), configuration.getTimeUnit())
                .writeTimeout(configuration.getWriteTimeout(), configuration.getTimeUnit())
                .retryOnConnectionFailure(true)
                .build();

        return new Retrofit.Builder()
                .baseUrl("http://" + host + ":" + port + "/inlong/manager/api/")
                .addConverterFactory(JacksonConverterFactory.create(JsonUtils.OBJECT_MAPPER))
                .client(okHttpClient)
                .build();
    }

    /**
     * Send http request.
     *
     * @param call http request
     * @param <T> T
     * @return T
     */
    public static <T> T executeHttpCall(Call<T> call) {
        Request request = call.request();
        String url = request.url().encodedPath();
        try {
            retrofit2.Response<T> response = call.execute();
            Preconditions.expectTrue(response.isSuccessful(),
                    String.format(REQUEST_FAILED_MSG, url, response.message()));
            return response.body();
        } catch (IOException e) {
            log.error(String.format(REQUEST_FAILED_MSG, url, e.getMessage()), e);
            throw new RuntimeException(String.format(REQUEST_FAILED_MSG, url, e.getMessage()), e);
        }
    }

    /**
     * Assert if the response is successful.
     *
     * @param response response
     */
    public static void assertRespSuccess(Response<?> response) {
        Preconditions.expectTrue(response.isSuccess(), String.format(REQUEST_FAILED_MSG, response.getErrMsg(), null));
    }
}
