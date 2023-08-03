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

import org.apache.inlong.manager.common.exceptions.BusinessException;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Inlong group related status
 */
public enum GroupStatus {

    TO_BE_SUBMIT(100, "waiting for submit"),
    TO_BE_APPROVAL(101, "waiting for approval"),

    APPROVE_REJECTED(102, "approval rejected"),
    APPROVE_PASSED(103, "approval passed"),

    CONFIG_ING(110, "in configure"),
    CONFIG_FAILED(120, "configuration failed"),
    CONFIG_SUCCESSFUL(130, "configuration successful"),

    SUSPENDING(141, "suspending"),
    SUSPENDED(140, "suspended"),

    RESTARTING(151, "restarting"),
    RESTARTED(150, "restarted"),

    DELETING(41, "deleting"),
    DELETED(40, "deleted"),

    // FINISH is used for batch task.
    FINISH(131, "finish");

    private static final Map<GroupStatus, Set<GroupStatus>> GROUP_STATE_AUTOMATON = Maps.newHashMap();

    /*
     * Init group finite status automaton
     */
    static {
        GROUP_STATE_AUTOMATON.put(TO_BE_SUBMIT, Sets.newHashSet(TO_BE_SUBMIT, TO_BE_APPROVAL, DELETING));
        GROUP_STATE_AUTOMATON.put(TO_BE_APPROVAL, Sets.newHashSet(APPROVE_REJECTED, APPROVE_PASSED));

        GROUP_STATE_AUTOMATON.put(APPROVE_REJECTED, Sets.newHashSet(APPROVE_REJECTED, TO_BE_APPROVAL, DELETING));
        GROUP_STATE_AUTOMATON.put(APPROVE_PASSED, Sets.newHashSet(APPROVE_PASSED, CONFIG_ING, DELETING));

        GROUP_STATE_AUTOMATON.put(CONFIG_ING, Sets.newHashSet(CONFIG_ING, CONFIG_FAILED, CONFIG_SUCCESSFUL));
        GROUP_STATE_AUTOMATON.put(CONFIG_FAILED,
                Sets.newHashSet(CONFIG_FAILED, CONFIG_ING, CONFIG_SUCCESSFUL, TO_BE_APPROVAL, DELETING));
        GROUP_STATE_AUTOMATON.put(CONFIG_SUCCESSFUL,
                Sets.newHashSet(CONFIG_SUCCESSFUL, TO_BE_APPROVAL, CONFIG_ING, SUSPENDING, DELETING, FINISH));

        GROUP_STATE_AUTOMATON.put(SUSPENDING, Sets.newHashSet(SUSPENDING, SUSPENDED, CONFIG_FAILED));
        GROUP_STATE_AUTOMATON.put(SUSPENDED, Sets.newHashSet(SUSPENDED, RESTARTING, DELETING));

        GROUP_STATE_AUTOMATON.put(RESTARTING, Sets.newHashSet(RESTARTING, RESTARTED, CONFIG_FAILED));
        GROUP_STATE_AUTOMATON.put(RESTARTED, Sets.newHashSet(RESTARTED, SUSPENDING, TO_BE_APPROVAL, DELETING));

        GROUP_STATE_AUTOMATON.put(DELETING, Sets.newHashSet(DELETING, DELETED, CONFIG_FAILED));
        GROUP_STATE_AUTOMATON.put(DELETED, Sets.newHashSet(DELETED));

        GROUP_STATE_AUTOMATON.put(FINISH, Sets.newHashSet(FINISH, DELETING));
    }

    private final Integer code;
    private final String description;

    GroupStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static GroupStatus forCode(int code) {
        for (GroupStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new BusinessException(ErrorCodeEnum.ILLEGAL_RECORD_FIELD_VALUE,
                String.format("Illegal code=%s for GroupStatus", code));
    }

    public static boolean notAllowedTransition(GroupStatus pre, GroupStatus now) {
        Set<GroupStatus> nextStates = GROUP_STATE_AUTOMATON.get(pre);
        return nextStates == null || !nextStates.contains(now);
    }

    /**
     * Checks whether the given status allows updating operate.
     */
    public static boolean notAllowedUpdate(GroupStatus status) {
        return status == GroupStatus.TO_BE_APPROVAL
                || status == GroupStatus.CONFIG_ING
                || status == GroupStatus.SUSPENDING
                || status == GroupStatus.RESTARTING
                || status == GroupStatus.DELETING;
    }

    /**
     * Checks whether the given status allows updating the MQ type of inlong group.
     */
    public static boolean allowedUpdateMQ(GroupStatus status) {
        return status == GroupStatus.TO_BE_SUBMIT
                || status == GroupStatus.TO_BE_APPROVAL
                || status == GroupStatus.APPROVE_REJECTED
                || status == GroupStatus.CONFIG_FAILED;
    }

    /**
     * Checks whether the given status allows updating stream source.
     */
    public static boolean allowedUpdateSource(GroupStatus status) {
        return status == GroupStatus.CONFIG_SUCCESSFUL
                || status == GroupStatus.CONFIG_FAILED;
    }

    /**
     * Checks whether the given status needs to delete the inlong stream first.
     */
    public static boolean deleteStreamFirst(GroupStatus status) {
        return status == GroupStatus.APPROVE_PASSED
                || status == GroupStatus.CONFIG_FAILED
                || status == GroupStatus.CONFIG_SUCCESSFUL
                || status == GroupStatus.SUSPENDED
                || status == GroupStatus.RESTARTED
                || status == GroupStatus.FINISH;
    }

    /**
     * Checks whether the given status allows deleting other infos,
     * <p/>
     * If true, will logically delete all related infos, including streams, sources, sinks, etc.
     */
    public static boolean allowedDeleteSubInfos(GroupStatus status) {
        return status == GroupStatus.TO_BE_SUBMIT
                || status == GroupStatus.APPROVE_REJECTED
                || status == GroupStatus.DELETED;
    }

    /**
     * Checks whether the given status allows suspending operate.
     */
    public static boolean allowedSuspend(GroupStatus status) {
        return status == GroupStatus.CONFIG_SUCCESSFUL
                || status == GroupStatus.RESTARTED
                || status == GroupStatus.SUSPENDED
                || status == GroupStatus.FINISH;
    }

    /**
     * Temporary group status, adding, deleting and modifying operations are not allowed
     */
    public static boolean isTempStatus(GroupStatus status) {
        return status == TO_BE_APPROVAL
                || status == CONFIG_ING
                || status == SUSPENDING
                || status == RESTARTING
                || status == DELETING;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }

}
