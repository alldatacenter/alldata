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

import org.apache.inlong.manager.dao.entity.SortTaskSinkParamEntity;
import org.apache.inlong.manager.dao.mapper.SortTaskSinkParamEntityMapper;
import org.apache.inlong.manager.service.core.SortTaskSinkParamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sort sink params service implementation.
 */
@Service
public class SortTaskSinkParamServiceImpl implements SortTaskSinkParamService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SortTaskSinkParamServiceImpl.class);

    @Autowired
    private SortTaskSinkParamEntityMapper sortTaskSinkParamEntityMapper;

    @Override
    public Map<String, String> selectByTaskNameAndType(String taskName, String sinkType) {
        LOGGER.info("task name is {}, sink type is {}", taskName, sinkType);
        List<SortTaskSinkParamEntity> taskSinkParamEntityList =
                sortTaskSinkParamEntityMapper.selectByTaskNameAndType(taskName);
        Map<String, String> sinkParams = new HashMap<>();
        taskSinkParamEntityList.forEach(entity -> sinkParams.put(entity.getParamKey(), entity.getParamValue()));
        return sinkParams;
    }

}
