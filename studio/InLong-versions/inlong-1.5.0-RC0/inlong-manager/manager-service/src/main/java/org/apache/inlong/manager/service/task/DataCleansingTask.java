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

package org.apache.inlong.manager.service.task;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupExtEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamExtEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamFieldEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSinkEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSinkFieldEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSourceEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSourceFieldEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamTransformEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamTransformFieldEntityMapper;
import org.apache.inlong.manager.dao.mapper.WorkflowEventLogEntityMapper;
import org.apache.inlong.manager.dao.mapper.WorkflowProcessEntityMapper;
import org.apache.inlong.manager.dao.mapper.WorkflowTaskEntityMapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Data cleansing task, it will be executed periodically if turned on.
 */
@Slf4j
@Service
public class DataCleansingTask extends TimerTask implements InitializingBean {

    /**
     * The execution starts after this delay in seconds.
     */
    private static final int INITIAL_DELAY = 60;

    @Value("${data.cleansing.enabled:false}")
    private Boolean enabled;
    @Value("${data.cleansing.interval.seconds:1800}")
    private Integer interval;
    @Value("${data.cleansing.before.days:10}")
    private Integer before;
    @Value("${data.cleansing.batchSize:100}")
    private Integer batchSize;

    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private InlongGroupExtEntityMapper groupExtMapper;
    @Autowired
    private InlongStreamEntityMapper streamMapper;
    @Autowired
    private InlongStreamExtEntityMapper streamExtMapper;
    @Autowired
    private InlongStreamFieldEntityMapper streamFieldMapper;
    @Autowired
    private StreamSinkEntityMapper streamSinkMapper;
    @Autowired
    private StreamSinkFieldEntityMapper streamSinkFieldMapper;
    @Autowired
    private StreamSourceEntityMapper streamSourceMapper;
    @Autowired
    private StreamSourceFieldEntityMapper streamSourceFieldMapper;
    @Autowired
    private StreamTransformEntityMapper streamTransformMapper;
    @Autowired
    private StreamTransformFieldEntityMapper streamTransformFieldMapper;

    @Autowired
    private WorkflowProcessEntityMapper workflowProcessMapper;
    @Autowired
    private WorkflowTaskEntityMapper workflowTaskMapper;
    @Autowired
    private WorkflowEventLogEntityMapper workflowEventLogMapper;

    @Override
    public void afterPropertiesSet() {
        if (enabled) {
            log.info("start data cleansing timer task");
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleWithFixedDelay(this, INITIAL_DELAY, interval, TimeUnit.SECONDS);
        }
    }

    @Override
    public void run() {
        try {
            // get the date before `before` days
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.add(Calendar.DAY_OF_MONTH, -before);

            Date daysBefore = calendar.getTime();
            List<String> groupIds = groupMapper.selectDeletedGroupIds(daysBefore, batchSize);
            if (CollectionUtils.isEmpty(groupIds)) {
                return;
            }

            log.info("begin to delete data before {}, group ids {}", daysBefore, groupIds);

            // cleanse configuration data
            groupMapper.deleteByInlongGroupIds(groupIds);
            groupExtMapper.deleteByInlongGroupIds(groupIds);
            streamMapper.deleteByInlongGroupIds(groupIds);
            streamExtMapper.deleteByInlongGroupIds(groupIds);
            streamFieldMapper.deleteByInlongGroupIds(groupIds);

            streamSinkMapper.deleteByInlongGroupIds(groupIds);
            streamSinkFieldMapper.deleteByInlongGroupIds(groupIds);
            streamSourceMapper.deleteByInlongGroupIds(groupIds);
            streamSourceFieldMapper.deleteByInlongGroupIds(groupIds);
            streamTransformMapper.deleteByInlongGroupIds(groupIds);
            streamTransformFieldMapper.deleteByInlongGroupIds(groupIds);

            // cleanse workflow data
            List<Integer> processIds = workflowProcessMapper.selectByInlongGroupIds(groupIds);
            workflowProcessMapper.deleteByProcessIds(processIds);
            workflowTaskMapper.deleteByProcessIds(processIds);
            workflowEventLogMapper.deleteByProcessIds(processIds);

            log.info("success to delete data before {}, group ids size: {}", daysBefore, groupIds.size());
        } catch (Exception e) {
            log.error("exception while cleansing data from db: ", e);
        }
    }
}
