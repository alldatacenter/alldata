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

package org.apache.inlong.manager.service.core;

import com.github.pagehelper.PageInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamConfigLogListResponse;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamConfigLogPageRequest;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamConfigLogRequest;

/**
 * Stream config log service.
 */
public interface StreamConfigLogService {

    /**
     * Report config log.
     *
     * @param request request condition.
     * @return whether succeed info.
     */
    String reportConfigLog(InlongStreamConfigLogRequest request);

    /**
     * Query inlong stream config log list based on conditions
     *
     * @param request Inlong stream config log paging query request
     * @return Inlong stream config log paging list
     */
    PageInfo<InlongStreamConfigLogListResponse> listByCondition(
            InlongStreamConfigLogPageRequest request);

}
