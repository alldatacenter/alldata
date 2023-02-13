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

package org.apache.inlong.agent.plugin.utils;

import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.db.Db;
import org.apache.inlong.agent.db.RocksDbImp;
import org.apache.inlong.agent.db.TriggerProfileDb;
import org.apache.inlong.agent.utils.AgentUtils;

import java.util.List;

import static org.apache.inlong.agent.constant.JobConstants.JOB_ID_PREFIX;

public class RocksDBUtils {

    public static void main(String[] args) {
        Db db = new RocksDbImp();
        upgrade(db);
    }

    public static void upgrade(Db db) {
        TriggerProfileDb triggerProfileDb = new TriggerProfileDb(db);
        List<TriggerProfile> allTriggerProfiles = triggerProfileDb.getTriggers();
        allTriggerProfiles.forEach(triggerProfile -> {
            if (triggerProfile.hasKey(JobConstants.JOB_DIR_FILTER_PATTERN)) {
                triggerProfile.set(JobConstants.JOB_DIR_FILTER_PATTERNS,
                        triggerProfile.get(JobConstants.JOB_DIR_FILTER_PATTERN));
                triggerProfile.set(JobConstants.JOB_DIR_FILTER_PATTERN, null);
            }

            triggerProfile.set(JobConstants.JOB_INSTANCE_ID,
                    AgentUtils.getSingleJobId(JOB_ID_PREFIX, triggerProfile.getTriggerId()));

            triggerProfileDb.storeTrigger(triggerProfile);
        });
    }

    public static void printTrigger(Db db) {
    }
}
