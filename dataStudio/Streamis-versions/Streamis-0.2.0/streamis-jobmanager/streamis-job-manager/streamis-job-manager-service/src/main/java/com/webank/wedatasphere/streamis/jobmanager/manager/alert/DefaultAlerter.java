/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.manager.alert;

import com.webank.wedatasphere.streamis.jobmanager.manager.dao.StreamAlertMapper;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamAlertRecord;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamJob;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamTask;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class DefaultAlerter implements Alerter{

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAlerter.class);

    @Autowired
    private StreamAlertMapper streamAlertMapper;

    @Override
    public void alert(AlertLevel alertLevel, String alertMessage, List<String> alertUsers, StreamTask streamTask) {
        LOG.info("Alert info: Level={}, alertMessage={}, alertUsers={}", alertLevel.name(), alertMessage,
                StringUtils.join(alertUsers, ","));
        for (String alertUser : alertUsers) {
            StreamAlertRecord streamAlertRecord = new StreamAlertRecord();
            streamAlertRecord.setAlertLevel(alertLevel.name());
            streamAlertRecord.setAlertMsg(alertMessage);
            streamAlertRecord.setAlertUser(alertUser);
            streamAlertRecord.setJobVersionId(streamTask.getJobVersionId());
            streamAlertRecord.setJobId(streamTask.getJobId());
            streamAlertRecord.setCreateTime(new Date());
            streamAlertRecord.setStatus(0);
            streamAlertRecord.setTaskId(streamTask.getId());
            streamAlertMapper.insert(streamAlertRecord);
        }
    }
}
