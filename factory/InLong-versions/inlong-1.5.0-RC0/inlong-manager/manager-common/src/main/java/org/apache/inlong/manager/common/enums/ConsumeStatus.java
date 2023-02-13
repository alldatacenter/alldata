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
import org.apache.inlong.manager.common.exceptions.BusinessException;

import java.util.Map;
import java.util.Set;

/**
 * Inlong consume status
 */
public enum ConsumeStatus {

    TO_BE_SUBMIT(100, "waiting for submit"),
    TO_BE_APPROVAL(101, "waiting for approval"),

    APPROVE_REJECTED(102, "approval rejected"),
    APPROVE_PASSED(103, "approval passed"),
    APPROVE_CANCELED(104, "approval canceled"),

    DELETING(41, "deleting"),
    DELETED(40, "deleted"),

    ;

    /**
     * State automaton for InlongConsume
     */
    private static final Map<ConsumeStatus, Set<ConsumeStatus>> CONSUME_STATE_AUTOMATON = Maps.newHashMap();

    /*
     * Init consume finite status automaton
     */
    static {
        CONSUME_STATE_AUTOMATON.put(TO_BE_SUBMIT, Sets.newHashSet(TO_BE_SUBMIT, TO_BE_APPROVAL, DELETING));
        CONSUME_STATE_AUTOMATON.put(TO_BE_APPROVAL,
                Sets.newHashSet(TO_BE_APPROVAL, APPROVE_REJECTED, APPROVE_PASSED, APPROVE_CANCELED, DELETING));

        CONSUME_STATE_AUTOMATON.put(APPROVE_REJECTED, Sets.newHashSet(APPROVE_REJECTED, TO_BE_APPROVAL, DELETING));
        CONSUME_STATE_AUTOMATON.put(APPROVE_CANCELED, Sets.newHashSet(APPROVE_CANCELED, TO_BE_APPROVAL, DELETING));
        CONSUME_STATE_AUTOMATON.put(APPROVE_PASSED, Sets.newHashSet(APPROVE_PASSED, TO_BE_APPROVAL, DELETING));

        CONSUME_STATE_AUTOMATON.put(DELETING, Sets.newHashSet(DELETING, DELETED));
        CONSUME_STATE_AUTOMATON.put(DELETED, Sets.newHashSet(DELETED));
    }

    private final Integer code;
    private final String description;

    ConsumeStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Get the ConsumeStatus instance from the given code
     *
     * @param code status code
     * @return instance of ConsumeStatus
     */
    public static ConsumeStatus forCode(int code) {
        for (ConsumeStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new BusinessException(String.format("Illegal code=%s for ConsumeStatus", code));
    }

    /**
     * Check whether the current status can be transferred to the next status
     *
     * @param cur current status
     * @param next next status
     * @return true if transferred, false if not
     */
    public static boolean notAllowedTransfer(ConsumeStatus cur, ConsumeStatus next) {
        Set<ConsumeStatus> nextSet = CONSUME_STATE_AUTOMATON.get(cur);
        return nextSet == null || !nextSet.contains(next);
    }

    /**
     * Checks whether the given status allows the update.
     */
    public static boolean allowedUpdate(ConsumeStatus status) {
        return status == ConsumeStatus.TO_BE_SUBMIT
                || status == ConsumeStatus.APPROVE_REJECTED
                || status == ConsumeStatus.APPROVE_CANCELED;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

}
