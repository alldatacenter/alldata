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
import org.apache.inlong.manager.client.api.service.AuditApi;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.audit.AuditRequest;
import org.apache.inlong.manager.pojo.audit.AuditVO;
import org.apache.inlong.manager.pojo.common.Response;

import java.util.List;

/**
 * Client for {@link AuditApi}.
 */
public class AuditClient {

    private final AuditApi auditApi;

    public AuditClient(ClientConfiguration configuration) {
        auditApi = ClientUtils.createRetrofit(configuration).create(AuditApi.class);
    }

    /**
     * Query audit data for list by condition
     *
     * @param request The audit request of query condition
     * @return The result of query
     */
    public List<AuditVO> list(AuditRequest request) {
        Preconditions.expectNotNull(request, "request cannot be null");
        Preconditions.expectNotBlank(request.getInlongGroupId(), ErrorCodeEnum.INVALID_PARAMETER,
                "inlong group id cannot be empty");
        Preconditions.expectNotBlank(request.getInlongStreamId(), ErrorCodeEnum.INVALID_PARAMETER,
                "inlong stream id cannot be empty");
        Response<List<AuditVO>> response = ClientUtils.executeHttpCall(auditApi.list(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Refresh the base item of audit cache.
     *
     * @return true if not exception, or false if it has exception
     */
    public Boolean refreshCache(AuditRequest request) {
        Response<Boolean> response = ClientUtils.executeHttpCall(auditApi.refreshCache());
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }
}
