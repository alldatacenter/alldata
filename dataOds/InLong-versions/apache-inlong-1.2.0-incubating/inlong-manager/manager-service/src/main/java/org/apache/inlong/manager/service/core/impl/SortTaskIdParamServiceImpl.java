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

import org.apache.inlong.manager.dao.entity.SortTaskIdParamEntity;
import org.apache.inlong.manager.dao.mapper.SortTaskIdParamEntityMapper;
import org.apache.inlong.manager.service.core.SortTaskIdParamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sort id params service implementation.
 */
@Service
public class SortTaskIdParamServiceImpl implements SortTaskIdParamService {

    private static final String KEY_GROUP_ID = "inlongGroupId";

    private static final String KEY_STREAM_ID = "inlongStreamId";

    @Autowired private SortTaskIdParamEntityMapper sortTaskIdParamEntityMapper;

    @Override
    public List<Map<String, String>> selectByTaskName(String taskName) {
        List<SortTaskIdParamEntity> taskIdParamEntityList =
                sortTaskIdParamEntityMapper.selectByTaskName(taskName);
        Map<String, Map<String, String>> idParams = new HashMap<>();
        taskIdParamEntityList.forEach(entity -> {
            Map<String, String> idParam =
                    idParams.computeIfAbsent(entity.getKey(), key -> new HashMap<>());
            idParam.put(entity.getParamKey(), entity.getParamValue());
            idParam.putIfAbsent(KEY_GROUP_ID, entity.getGroupId());
            idParam.putIfAbsent(KEY_STREAM_ID, entity.getStreamId());
        });
        return new ArrayList<>(idParams.values());
    }
}
