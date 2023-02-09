/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal.security;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.obs.services.internal.Constants;
import com.obs.services.internal.utils.PropertyManager;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EcsSecurityUtils {
    /**
     * Default root url for the openstack metadata apis.
     */
    private static final String OPENSTACK_METADATA_ROOT = "/openstack/latest";

    /**
     * Default endpoint for the ECS Instance Metadata Service.
     */
    private static final String ECS_METADATA_SERVICE_URL = PropertyManager.getInstance(Constants.PROPERTY_NAME_OBS)
            .getFormattedString("ecs.metadata.service.url");

    private static final String EC2_METADATA_SERVICE_OVERRIDE_URL = "ecsMetadataServiceOverrideEndpoint";

    private static final long HTTP_CONNECT_TIMEOUT_VALUE = 30 * 1000;

    private static OkHttpClient httpClient = new OkHttpClient.Builder().followRedirects(false)
            .retryOnConnectionFailure(true).cache(null)
            .connectTimeout(HTTP_CONNECT_TIMEOUT_VALUE, TimeUnit.MILLISECONDS)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_VALUE, TimeUnit.MILLISECONDS)
            .readTimeout(HTTP_CONNECT_TIMEOUT_VALUE, TimeUnit.MILLISECONDS).build();

    /**
     * Returns the temporary security credentials (access, secret,
     * securitytoken, and expires_at) associated with the IAM roles on the
     * instance.
     */
    public static String getSecurityKeyInfoWithDetail() throws IOException {
        return getResourceWithDetail(getEndpointForECSMetadataService() + OPENSTACK_METADATA_ROOT + "/securitykey");
    }

    /**
     * Returns the host address of the ECS Instance Metadata Service.
     */
    public static String getEndpointForECSMetadataService() {
        String overridUrl = System.getProperty(EC2_METADATA_SERVICE_OVERRIDE_URL);
        return overridUrl != null ? overridUrl : ECS_METADATA_SERVICE_URL;
    }

    /**
     * Get resource and return contents from metadata service with the specify
     * path.
     */
    private static String getResourceWithDetail(String endpoint) throws IOException {
        Request.Builder builder = new Request.Builder();
        builder.header("Accept", "*/*");
        Request request = builder.url(endpoint).get().build();
        Call c = httpClient.newCall(request);
        Response res = null;
        String content = "";
        try {
            res = c.execute();
            String header = "";
            if (res.headers() != null) {
                header = res.headers().toString();
            }
            if (res.body() != null) {
                content = res.body().string();
            }

            if (!(res.code() >= 200 && res.code() < 300)) {
                String errorMessage = "Get securityKey form ECS failed, Code : " + res.code() + "; Headers : " + header
                        + "; Content : " + content;
                throw new IllegalArgumentException(errorMessage);
            }

            return content;
        } finally {
            if (res != null) {
                res.close();
            }
        }
    }
}
