/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corebase.utils;

import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster;

public class OpsSyncInfo {

    private boolean updated = false;
    private long groupFlowChkId = TBaseConstants.META_VALUE_UNDEFINED;
    private long defFlowChkId = TBaseConstants.META_VALUE_UNDEFINED;
    private int qryPriorityId = TBaseConstants.META_VALUE_UNDEFINED;
    private long csmFrmMaxOffsetCtrlId = TBaseConstants.META_VALUE_UNDEFINED;
    private boolean requireAuth = false;
    private String defFlowControlInfo = "";
    private String groupFlowControlInfo = "";

    public OpsSyncInfo() {

    }

    /**
     * Update Ops task information
     *
     * @param opsTaskInfo   the ops task information sent from Master
     */
    public void updOpsSyncInfo(ClientMaster.OpsTaskInfo opsTaskInfo) {
        if (opsTaskInfo == null) {
            return;
        }
        if (opsTaskInfo.hasDefFlowCheckId()) {
            this.defFlowChkId = opsTaskInfo.getDefFlowCheckId();
            this.updated = true;
        }
        if (opsTaskInfo.hasGroupFlowCheckId()) {
            this.groupFlowChkId = opsTaskInfo.getGroupFlowCheckId();
            this.updated = true;
        }
        if (opsTaskInfo.hasQryPriorityId()) {
            this.qryPriorityId = opsTaskInfo.getQryPriorityId();
            this.updated = true;
        }
        if (opsTaskInfo.hasCsmFrmMaxOffsetCtrlId()
                && opsTaskInfo.getCsmFrmMaxOffsetCtrlId() >= 0) {
            this.csmFrmMaxOffsetCtrlId = opsTaskInfo.getCsmFrmMaxOffsetCtrlId();
            this.updated = true;
        }
        if (opsTaskInfo.hasRequireAuth() && opsTaskInfo.getRequireAuth()) {
            this.requireAuth = true;
            this.updated = true;
        }
        if (opsTaskInfo.hasDefFlowControlInfo()
                && TStringUtils.isNotBlank(opsTaskInfo.getDefFlowControlInfo())) {
            this.defFlowControlInfo = opsTaskInfo.getDefFlowControlInfo();
            this.updated = true;
        }
        if (opsTaskInfo.hasGroupFlowControlInfo()
                && TStringUtils.isNotBlank(opsTaskInfo.getGroupFlowControlInfo())) {
            this.groupFlowControlInfo = opsTaskInfo.getGroupFlowControlInfo();
            this.updated = true;
        }
    }

    public boolean isUpdated() {
        return updated;
    }

    public long getGroupFlowChkId() {
        return groupFlowChkId;
    }

    public long getDefFlowChkId() {
        return defFlowChkId;
    }

    public int getQryPriorityId() {
        return qryPriorityId;
    }

    public long getCsmFromMaxOffsetCtrlId() {
        return csmFrmMaxOffsetCtrlId;
    }

    public boolean isRequireAuth() {
        return requireAuth;
    }

    public String getDefFlowControlInfo() {
        return defFlowControlInfo;
    }

    public String getGroupFlowControlInfo() {
        return groupFlowControlInfo;
    }

    public void clear() {
        this.updated = false;
        this.groupFlowChkId = TBaseConstants.META_VALUE_UNDEFINED;
        this.defFlowChkId = TBaseConstants.META_VALUE_UNDEFINED;
        this.qryPriorityId = TBaseConstants.META_VALUE_UNDEFINED;
        this.csmFrmMaxOffsetCtrlId = TBaseConstants.META_VALUE_UNDEFINED;
        this.requireAuth = false;
        this.defFlowControlInfo = "";
        this.groupFlowControlInfo = "";
    }
}
