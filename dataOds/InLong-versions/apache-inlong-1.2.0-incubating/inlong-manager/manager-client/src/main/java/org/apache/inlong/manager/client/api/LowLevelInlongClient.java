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

package org.apache.inlong.manager.client.api;

import com.github.pagehelper.PageInfo;
import org.apache.inlong.manager.client.api.impl.LowLevelInlongClientImpl;
import org.apache.inlong.manager.common.pojo.group.InlongGroupListResponse;
import org.apache.inlong.manager.common.pojo.group.InlongGroupPageRequest;

/**
 *  An interface to provide original Inlong Apis.
 *  Not recommend to use
 */
public interface LowLevelInlongClient {

    /**
     * Create inlong client.
     *
     * @param serviceUrl the service url
     * @param configuration the configuration
     * @return the inlong client
     */
    static LowLevelInlongClient create(String serviceUrl, ClientConfiguration configuration) {
        return new LowLevelInlongClientImpl(serviceUrl, configuration);
    }

    /**
     * List group
     *
     * @param request The request
     * @return PageInfo of group
     *
     * @throws Exception The exception may throws
     */
    PageInfo<InlongGroupListResponse> listGroup(InlongGroupPageRequest request) throws Exception;

}
