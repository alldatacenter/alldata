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

package org.apache.inlong.agent.db;

import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.constant.CommonConstants;
import org.apache.inlong.agent.constant.JobConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * db interface for trigger profile.
 */
public class TriggerProfileDb {

    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerProfileDb.class);

    private final Db db;

    public TriggerProfileDb(Db db) {
        this.db = db;
    }

    /**
     * get trigger list from db.
     *
     * @return list of trigger
     */
    public List<TriggerProfile> getTriggers() {
        // potential performance issue, needs to find out the speed.
        List<KeyValueEntity> result = this.db.findAll(CommonConstants.TRIGGER_ID_PREFIX);
        List<TriggerProfile> triggerList = new ArrayList<>();
        for (KeyValueEntity entity : result) {
            triggerList.add(entity.getAsTriggerProfile());
        }
        return triggerList;
    }

    /**
     * store trigger profile.
     *
     * @param trigger trigger
     */
    public void storeTrigger(TriggerProfile trigger) {
        if (trigger.allRequiredKeyExist()) {
            String keyName = CommonConstants.TRIGGER_ID_PREFIX + trigger.get(JobConstants.JOB_ID);
            KeyValueEntity entity = new KeyValueEntity(keyName,
                    trigger.toJsonStr(), trigger.get(JobConstants.JOB_DIR_FILTER_PATTERNS));
            KeyValueEntity oldEntity = db.put(entity);
            if (oldEntity != null) {
                LOGGER.warn("trigger profile {} has been replaced", oldEntity.getKey());
            }
        }
    }

    /**
     * delete trigger by id.
     */
    public void deleteTrigger(String id) {
        db.remove(CommonConstants.TRIGGER_ID_PREFIX + id);
    }
}
