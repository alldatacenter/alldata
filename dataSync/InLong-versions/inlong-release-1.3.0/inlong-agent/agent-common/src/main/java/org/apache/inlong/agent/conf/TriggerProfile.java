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

package org.apache.inlong.agent.conf;

import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.pojo.JobProfileDto;
import org.apache.inlong.common.pojo.agent.DataConfig;

/**
 * profile used in trigger. Trigger profile is a special job profile
 */
public class TriggerProfile extends JobProfile {

    /**
     * Parse a given json string and get a TriggerProfile
     */
    public static TriggerProfile parseJsonStr(String jsonStr) {
        TriggerProfile conf = new TriggerProfile();
        conf.loadJsonStrResource(jsonStr);
        return conf;
    }

    /**
     * Parse a given JobProfile instance and get a TriggerProfile
     */
    public static TriggerProfile parseJobProfile(JobProfile jobProfile) {
        TriggerProfile conf = new TriggerProfile();
        conf.loadJsonStrResource(jobProfile.toJsonStr());
        return conf;
    }

    /**
     * Get a TriggerProfile from a DataConfig
     */
    public static TriggerProfile getTriggerProfiles(DataConfig dataConfig) {
        if (dataConfig == null) {
            return null;
        }
        return JobProfileDto.convertToTriggerProfile(dataConfig);
    }

    @Override
    public boolean allRequiredKeyExist() {
        return super.allRequiredKeyExist();
    }

    public String getTriggerId() {
        return get(JobConstants.JOB_ID);
    }

    public Integer getOpType() {
        return getInt(JobConstants.JOB_OP);
    }

    public String getDeliveryTime() {
        return get(JobConstants.JOB_DELIVERY_TIME);
    }

}
