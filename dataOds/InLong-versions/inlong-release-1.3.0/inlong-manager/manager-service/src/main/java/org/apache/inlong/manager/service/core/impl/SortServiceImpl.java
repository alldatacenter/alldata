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

package org.apache.inlong.manager.service.core.impl;

import org.apache.inlong.common.pojo.sortstandalone.SortClusterResponse;
import org.apache.inlong.common.pojo.sdk.SortSourceConfigResponse;
import org.apache.inlong.manager.service.core.SortClusterService;
import org.apache.inlong.manager.service.core.SortSourceService;
import org.apache.inlong.manager.service.core.SortService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Sort service implementation.
 */
@Lazy
@Service
public class SortServiceImpl implements SortService {

    @Lazy
    @Autowired
    private SortSourceService sortSourceService;

    @Lazy
    @Autowired
    private SortClusterService sortClusterService;

    @Override
    public SortClusterResponse getClusterConfig(String clusterName, String md5) {

        return sortClusterService.getClusterConfig(clusterName, md5);
    }

    @Override
    public SortSourceConfigResponse getSourceConfig(String clusterName, String sortTaskId, String md5) {
        return sortSourceService.getSourceConfig(clusterName, sortTaskId, md5);
    }
}
