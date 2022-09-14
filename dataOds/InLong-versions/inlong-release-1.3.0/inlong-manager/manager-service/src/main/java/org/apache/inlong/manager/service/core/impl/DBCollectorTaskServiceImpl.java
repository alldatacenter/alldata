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

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.pojo.dbcollector.DBCollectorReportTaskRequest;
import org.apache.inlong.manager.pojo.dbcollector.DBCollectorTaskInfo;
import org.apache.inlong.manager.pojo.dbcollector.DBCollectorTaskRequest;
import org.apache.inlong.manager.dao.entity.DBCollectorDetailTaskEntity;
import org.apache.inlong.manager.dao.mapper.DBCollectorDetailTaskMapper;
import org.apache.inlong.manager.service.core.DBCollectorTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
@Deprecated
public class DBCollectorTaskServiceImpl implements DBCollectorTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBCollectorTaskServiceImpl.class);
    private static final String INTERFACE_VERSION = "1.0";

    private static final int INIT = 0;
    private static final int DISPATCHED = 1;
    private static final int DONE = 2;
    private static final int FAILED = 3;

    private static final int RETURN_SUCC = 0;
    private static final int RETURN_EMPTY = 1;
    private static final int RETURN_INVALID_VERSION = 2;
    private static final int RETURN_INVALID_STATE = 3;

    @Autowired
    private DBCollectorDetailTaskMapper detailTaskMapper;

    @Override
    public DBCollectorTaskInfo getTask(DBCollectorTaskRequest req) {
        LOGGER.debug("db collector task request: {}", req);
        if (!INTERFACE_VERSION.equals(req.getVersion())) {
            return DBCollectorTaskInfo.builder().version(INTERFACE_VERSION).returnCode(RETURN_INVALID_VERSION).build();
        }
        DBCollectorDetailTaskEntity entity = detailTaskMapper.selectOneByState(INIT);
        if (entity == null) {
            return DBCollectorTaskInfo.builder().version(INTERFACE_VERSION).returnCode(RETURN_EMPTY).build();
        }
        int ret = detailTaskMapper.changeState(entity.getId(), 0, INIT, DISPATCHED);
        if (ret == 0) {
            return DBCollectorTaskInfo.builder().version(INTERFACE_VERSION).returnCode(RETURN_EMPTY).build();
        } else {
            DBCollectorTaskInfo.TaskInfo task = new DBCollectorTaskInfo.TaskInfo();
            task.setId(entity.getId());
            task.setType(entity.getType());
            task.setDBType(entity.getDbType());
            task.setIp(entity.getIp());
            task.setDBPort(entity.getPort());
            task.setDBName(entity.getDbName());
            task.setUser(entity.getUser());
            task.setPassword(entity.getPassword());
            task.setSqlStatement(entity.getSqlStatement());
            task.setTotalLimit(entity.getTotalLimit());
            task.setOnceLimit(entity.getOnceLimit());
            task.setTimeLimit(entity.getTimeLimit());
            task.setRetryTimes(entity.getRetryTimes());
            task.setInlongGroupId(entity.getGroupId());
            task.setInlongStreamId(entity.getStreamId());
            return DBCollectorTaskInfo.builder().version(INTERFACE_VERSION).returnCode(RETURN_SUCC).data(task).build();
        }
    }

    @Override
    public Integer reportTask(DBCollectorReportTaskRequest req) {
        if (!Objects.equals(req.getVersion(), INTERFACE_VERSION)) {
            return RETURN_INVALID_VERSION;
        }
        DBCollectorDetailTaskEntity entity = detailTaskMapper.selectByTaskId(req.getId());
        if (entity == null) {
            return RETURN_EMPTY;
        }
        if (req.getState() != DISPATCHED
                && req.getState() != DONE
                && req.getState() != FAILED) {
            return RETURN_INVALID_STATE;
        }
        int ret = detailTaskMapper.changeState(entity.getId(), req.getOffset(), DISPATCHED, req.getState());
        if (ret == 0) {
            return RETURN_EMPTY;
        }
        return RETURN_SUCC;
    }
}
