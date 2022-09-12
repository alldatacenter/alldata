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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Inlong group related status
 */
public enum GroupStatus {

    DRAFT(0, "draft"),
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

    // GROUP_FINISH is used for batch task.
    FINISH(131, "finish");

    private static final Map<GroupStatus, Set<GroupStatus>> GROUP_STATE_AUTOMATON = Maps.newHashMap();

    /*
     * Init group finite status automaton
     */
    static {
        GROUP_STATE_AUTOMATON.put(DRAFT, Sets.newHashSet(DRAFT, TO_BE_SUBMIT, DELETING));
        GROUP_STATE_AUTOMATON.put(TO_BE_SUBMIT, Sets.newHashSet(TO_BE_SUBMIT, TO_BE_APPROVAL, DELETING));
        GROUP_STATE_AUTOMATON.put(TO_BE_APPROVAL, Sets.newHashSet(TO_BE_APPROVAL, APPROVE_REJECTED, APPROVE_PASSED));

        GROUP_STATE_AUTOMATON.put(APPROVE_REJECTED, Sets.newHashSet(APPROVE_REJECTED, TO_BE_APPROVAL, DELETING));
        GROUP_STATE_AUTOMATON.put(APPROVE_PASSED, Sets.newHashSet(APPROVE_PASSED, CONFIG_ING, DELETING));

        GROUP_STATE_AUTOMATON.put(CONFIG_ING, Sets.newHashSet(CONFIG_ING, CONFIG_FAILED, CONFIG_SUCCESSFUL));
        GROUP_STATE_AUTOMATON.put(CONFIG_FAILED,
                Sets.newHashSet(CONFIG_FAILED, CONFIG_SUCCESSFUL, TO_BE_APPROVAL, DELETING));
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
        throw new IllegalStateException(String.format("Illegal code=%s for GroupStatus", code));
    }

    public static boolean notAllowedTransition(GroupStatus pre, GroupStatus now) {
        Set<GroupStatus> nextStates = GROUP_STATE_AUTOMATON.get(pre);
        return nextStates == null || !nextStates.contains(now);
    }

    /**
     * Checks whether the given status allows the update.
     */
    public static boolean notAllowedUpdate(GroupStatus status) {
        return status == GroupStatus.CONFIG_ING || status == GroupStatus.TO_BE_APPROVAL;
    }

    /**
     * Checks whether the given status allows the logical delete
     */
    public static boolean allowedLogicDelete(GroupStatus status) {
        return status == GroupStatus.DRAFT || status == GroupStatus.TO_BE_SUBMIT
                || status == GroupStatus.DELETED || status == GroupStatus.FINISH;
    }

    /**
     * Only the {@link GroupStatus#DRAFT} and {@link GroupStatus#TO_BE_SUBMIT} status
     * allows change the MQ type of inlong group.
     */
    public static boolean notAllowedUpdateMQ(GroupStatus status) {
        return status == GroupStatus.DRAFT || status == GroupStatus.TO_BE_SUBMIT;
    }

    /**
     * Temporary group status, adding, deleting and modifying operations are not allowed
     */
    public static boolean isTempStatus(GroupStatus status) {
        return status == TO_BE_APPROVAL || status == CONFIG_ING;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT).replace("group_", "");
    }
}
