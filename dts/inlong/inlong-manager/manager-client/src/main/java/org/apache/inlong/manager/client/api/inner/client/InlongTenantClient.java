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

package org.apache.inlong.manager.client.api.inner.client;

import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.service.InlongTenantApi;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.tenant.InlongTenantInfo;
import org.apache.inlong.manager.pojo.tenant.InlongTenantPageRequest;
import org.apache.inlong.manager.pojo.tenant.InlongTenantRequest;

/**
 * Client for {@link InlongTenantApi}.
 */
public class InlongTenantClient {

    private final InlongTenantApi inlongTenantApi;

    public InlongTenantClient(ClientConfiguration configuration) {
        inlongTenantApi = ClientUtils.createRetrofit(configuration).create(InlongTenantApi.class);
    }

    /**
     * Get inlong tenant by tenant name
     *
     * @param name tenant name
     * @return {@link InlongTenantInfo}
     */
    public InlongTenantInfo getTenantByName(String name) {
        Response<InlongTenantInfo> inlongTenantInfoResponse = ClientUtils.executeHttpCall(inlongTenantApi.get(name));
        ClientUtils.assertRespSuccess(inlongTenantInfoResponse);
        return inlongTenantInfoResponse.getData();
    }

    /**
     * Create inlong tenant
     *
     * @param inlongTenantRequest tenant info
     * @return inlong tenant id
     */
    public Integer save(InlongTenantRequest inlongTenantRequest) {
        Response<Integer> saveInlongTenantResult = ClientUtils.executeHttpCall(
                inlongTenantApi.createInLongTenant(inlongTenantRequest));
        ClientUtils.assertRespSuccess(saveInlongTenantResult);
        return saveInlongTenantResult.getData();
    }

    /**
     * Paging query tenant info based on conditions.
     *
     * @param inlongTenantPageRequest paging request
     * @return tenant page list
     */
    public PageResult<InlongTenantInfo> listByCondition(InlongTenantPageRequest inlongTenantPageRequest) {
        Response<PageResult<InlongTenantInfo>> pageResultResponse = ClientUtils.executeHttpCall(
                inlongTenantApi.listByCondition(inlongTenantPageRequest));
        ClientUtils.assertRespSuccess(pageResultResponse);
        return pageResultResponse.getData();
    }

    /**
     * Update one tenant
     *
     * @param request tenant request that needs to be modified
     * @return whether succeed
     */
    public Boolean update(InlongTenantRequest request) {
        Response<Boolean> updateResult = ClientUtils.executeHttpCall(inlongTenantApi.update(request));
        ClientUtils.assertRespSuccess(updateResult);
        return updateResult.getData();
    }
}
