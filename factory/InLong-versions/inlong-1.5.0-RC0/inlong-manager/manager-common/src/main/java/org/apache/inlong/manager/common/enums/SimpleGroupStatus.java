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

package org.apache.inlong.manager.common.enums;

import java.util.ArrayList;
import java.util.List;

/**
 * The simple group status, more readable for users
 */
public enum SimpleGroupStatus {

    CREATE, REJECTED, INITIALIZING, OPERATING, STARTED, FAILED, STOPPED, FINISHED, DELETED;

    /**
     * Parse SimpleGroupStatus from the status code
     *
     * @param code of status
     * @see org.apache.inlong.manager.common.enums.GroupStatus
     */
    public static SimpleGroupStatus parseStatusByCode(int code) {
        GroupStatus groupStatus = GroupStatus.forCode(code);
        switch (groupStatus) {
            case TO_BE_SUBMIT:
            case TO_BE_APPROVAL:
                return CREATE;
            case DELETING:
            case SUSPENDING:
            case RESTARTING:
                return OPERATING;
            case APPROVE_REJECTED:
                return REJECTED;
            case APPROVE_PASSED:
            case CONFIG_ING:
                return INITIALIZING;
            case CONFIG_FAILED:
                return FAILED;
            case CONFIG_SUCCESSFUL:
            case RESTARTED:
                return STARTED;
            case SUSPENDED:
                return STOPPED;
            case FINISH:
                return FINISHED;
            case DELETED:
                return DELETED;
            default:
                throw new IllegalArgumentException(String.format("Unsupported status %s for group", code));
        }
    }

    /**
     * Parse group status code by the status string
     *
     * @see org.apache.inlong.manager.common.enums.GroupStatus
     */
    public static List<Integer> parseStatusCodeByStr(String status) {
        SimpleGroupStatus groupStatus;
        try {
            groupStatus = SimpleGroupStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Unsupported status %s for group", status));
        }

        List<Integer> statusList = new ArrayList<>();
        switch (groupStatus) {
            case CREATE:
                statusList.add(GroupStatus.TO_BE_SUBMIT.getCode());
                return statusList;
            case OPERATING:
                statusList.add(GroupStatus.DELETING.getCode());
                statusList.add(GroupStatus.SUSPENDING.getCode());
                statusList.add(GroupStatus.RESTARTING.getCode());
                return statusList;
            case REJECTED:
                statusList.add(GroupStatus.APPROVE_REJECTED.getCode());
                return statusList;
            case INITIALIZING:
                statusList.add(GroupStatus.TO_BE_APPROVAL.getCode());
                statusList.add(GroupStatus.APPROVE_PASSED.getCode());
                statusList.add(GroupStatus.CONFIG_ING.getCode());
                return statusList;
            case FAILED:
                statusList.add(GroupStatus.CONFIG_FAILED.getCode());
                return statusList;
            case STARTED:
                statusList.add(GroupStatus.RESTARTED.getCode());
                statusList.add(GroupStatus.CONFIG_SUCCESSFUL.getCode());
                return statusList;
            case STOPPED:
                statusList.add(GroupStatus.SUSPENDED.getCode());
                return statusList;
            case FINISHED:
                statusList.add(GroupStatus.FINISH.getCode());
                return statusList;
            case DELETED:
                statusList.add(GroupStatus.DELETED.getCode());
                return statusList;
            default:
                throw new IllegalArgumentException(String.format("Unsupported status %s for inlong group", status));
        }
    }

}
