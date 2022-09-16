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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Status of stream source
 */
public enum SourceStatus {

    // if deleted
    SOURCE_DISABLE(99, "disable"),
    SOURCE_NORMAL(101, "normal"),
    SOURCE_FAILED(102, "failed"),
    SOURCE_FROZEN(104, "frozen"),

    // if not approved
    SOURCE_NEW(110, "new created"),

    // ADD(0), DEL(1), RETRY(2), BACKTRACK(3), FROZEN(4), ACTIVE(5), CHECK(6), REDO_METRIC(7), MAKEUP(8);
    // [xxx] waiting to be issued
    TO_BE_ISSUED_ADD(200, "waiting to be issued add"),
    TO_BE_ISSUED_DELETE(201, "waiting to be issued delete"),
    TO_BE_ISSUED_RETRY(202, "waiting to be issued retry"),
    TO_BE_ISSUED_BACKTRACK(203, "waiting to be issued backtrack"),
    TO_BE_ISSUED_FROZEN(204, "waiting to be issued frozen"),
    TO_BE_ISSUED_ACTIVE(205, "waiting to be issued active"),
    TO_BE_ISSUED_CHECK(206, "waiting to be issued check"),
    TO_BE_ISSUED_REDO_METRIC(207, "waiting to be issued redo metric"),
    TO_BE_ISSUED_MAKEUP(208, "waiting to be issued makeup"),

    // [xxx] has been issued
    BEEN_ISSUED_ADD(300, "been issued add"),
    BEEN_ISSUED_DELETE(301, "been issued delete"),
    BEEN_ISSUED_RETRY(302, "been issued retry"),
    BEEN_ISSUED_BACKTRACK(303, "been issued backtrack"),
    BEEN_ISSUED_FROZEN(304, "been issued frozen"),
    BEEN_ISSUED_ACTIVE(305, "been issued active"),
    BEEN_ISSUED_CHECK(306, "been issued check"),
    BEEN_ISSUED_REDO_METRIC(307, "been issued redo metric"),
    BEEN_ISSUED_MAKEUP(308, "been issued makeup"),

    ;

    /**
     * The set of status from temporary to normal.
     */
    public static final Set<Integer> TEMP_TO_NORMAL = Sets.newHashSet(
            BEEN_ISSUED_ADD.getCode(), BEEN_ISSUED_RETRY.getCode(), BEEN_ISSUED_BACKTRACK.getCode(),
            BEEN_ISSUED_ACTIVE.getCode(), BEEN_ISSUED_CHECK.getCode(), BEEN_ISSUED_REDO_METRIC.getCode(),
            BEEN_ISSUED_MAKEUP.getCode());

    /**
     * The set of status allowed updating
     */
    public static final Set<Integer> ALLOWED_UPDATE = Sets.newHashSet(
            SOURCE_NEW.getCode(), SOURCE_FAILED.getCode(), SOURCE_FROZEN.getCode(),
            TO_BE_ISSUED_ADD.getCode(), TO_BE_ISSUED_DELETE.getCode(), TO_BE_ISSUED_RETRY.getCode(),
            TO_BE_ISSUED_BACKTRACK.getCode(), TO_BE_ISSUED_FROZEN.getCode(), TO_BE_ISSUED_ACTIVE.getCode(),
            TO_BE_ISSUED_CHECK.getCode(), TO_BE_ISSUED_REDO_METRIC.getCode(), TO_BE_ISSUED_MAKEUP.getCode());

    public static final Set<SourceStatus> TOBE_ISSUED_SET = Sets.newHashSet(
            TO_BE_ISSUED_ADD, TO_BE_ISSUED_DELETE, TO_BE_ISSUED_RETRY,
            TO_BE_ISSUED_BACKTRACK, TO_BE_ISSUED_FROZEN, TO_BE_ISSUED_ACTIVE,
            TO_BE_ISSUED_CHECK, TO_BE_ISSUED_REDO_METRIC, TO_BE_ISSUED_MAKEUP);

    private static final Map<SourceStatus, Set<SourceStatus>> SOURCE_STATE_AUTOMATON = Maps.newHashMap();

    static {
        // new
        SOURCE_STATE_AUTOMATON.put(SOURCE_NEW, Sets.newHashSet(SOURCE_DISABLE, SOURCE_NEW, TO_BE_ISSUED_ADD));

        // normal
        SOURCE_STATE_AUTOMATON.put(SOURCE_NORMAL,
                Sets.newHashSet(SOURCE_DISABLE, SOURCE_NORMAL, SOURCE_FAILED, TO_BE_ISSUED_DELETE,
                        TO_BE_ISSUED_RETRY, TO_BE_ISSUED_BACKTRACK, TO_BE_ISSUED_FROZEN, TO_BE_ISSUED_ACTIVE,
                        TO_BE_ISSUED_CHECK, TO_BE_ISSUED_REDO_METRIC, TO_BE_ISSUED_MAKEUP));

        // failed
        SOURCE_STATE_AUTOMATON.put(SOURCE_FAILED, Sets.newHashSet(SOURCE_DISABLE, SOURCE_FAILED, TO_BE_ISSUED_RETRY));

        // frozen
        SOURCE_STATE_AUTOMATON.put(SOURCE_FROZEN, Sets.newHashSet(SOURCE_DISABLE, SOURCE_FROZEN, TO_BE_ISSUED_ACTIVE));

        // [xxx] bo be issued
        HashSet<SourceStatus> tobeAddNext = Sets.newHashSet(BEEN_ISSUED_ADD, SOURCE_DISABLE);
        tobeAddNext.addAll(TOBE_ISSUED_SET);
        SOURCE_STATE_AUTOMATON.put(TO_BE_ISSUED_ADD, tobeAddNext);
        HashSet<SourceStatus> tobeDeleteNext = Sets.newHashSet(BEEN_ISSUED_DELETE);
        tobeDeleteNext.addAll(TOBE_ISSUED_SET);
        SOURCE_STATE_AUTOMATON.put(TO_BE_ISSUED_DELETE, Sets.newHashSet(tobeDeleteNext));
        HashSet<SourceStatus> tobeRetryNext = Sets.newHashSet(BEEN_ISSUED_RETRY);
        tobeRetryNext.addAll(TOBE_ISSUED_SET);
        SOURCE_STATE_AUTOMATON.put(TO_BE_ISSUED_RETRY, Sets.newHashSet(tobeRetryNext));
        HashSet<SourceStatus> tobeBacktrackNext = Sets.newHashSet(BEEN_ISSUED_BACKTRACK);
        tobeBacktrackNext.addAll(TOBE_ISSUED_SET);
        SOURCE_STATE_AUTOMATON.put(TO_BE_ISSUED_BACKTRACK, Sets.newHashSet(tobeBacktrackNext));
        HashSet<SourceStatus> tobeFrozenNext = Sets.newHashSet(BEEN_ISSUED_FROZEN);
        tobeFrozenNext.addAll(TOBE_ISSUED_SET);
        SOURCE_STATE_AUTOMATON.put(TO_BE_ISSUED_FROZEN, Sets.newHashSet(tobeFrozenNext));
        HashSet<SourceStatus> tobeActiveNext = Sets.newHashSet(BEEN_ISSUED_ACTIVE);
        tobeActiveNext.addAll(TOBE_ISSUED_SET);
        SOURCE_STATE_AUTOMATON.put(TO_BE_ISSUED_ACTIVE, Sets.newHashSet(tobeActiveNext));
        HashSet<SourceStatus> tobeCheckNext = Sets.newHashSet(BEEN_ISSUED_CHECK);
        tobeCheckNext.addAll(TOBE_ISSUED_SET);
        SOURCE_STATE_AUTOMATON.put(TO_BE_ISSUED_CHECK, Sets.newHashSet(tobeCheckNext));
        HashSet<SourceStatus> tobeRedoMetricNext = Sets.newHashSet(BEEN_ISSUED_REDO_METRIC);
        tobeRedoMetricNext.addAll(TOBE_ISSUED_SET);
        SOURCE_STATE_AUTOMATON.put(TO_BE_ISSUED_REDO_METRIC, Sets.newHashSet(tobeRedoMetricNext));
        HashSet<SourceStatus> tobeMakeupNext = Sets.newHashSet(BEEN_ISSUED_MAKEUP);
        tobeMakeupNext.addAll(TOBE_ISSUED_SET);
        SOURCE_STATE_AUTOMATON.put(TO_BE_ISSUED_MAKEUP, Sets.newHashSet(tobeMakeupNext));

        // [xxx] been issued
        SOURCE_STATE_AUTOMATON.put(BEEN_ISSUED_ADD, Sets.newHashSet(SOURCE_NORMAL, SOURCE_FAILED));
        SOURCE_STATE_AUTOMATON.put(BEEN_ISSUED_DELETE, Sets.newHashSet(SOURCE_NORMAL, SOURCE_FAILED));
        SOURCE_STATE_AUTOMATON.put(BEEN_ISSUED_RETRY, Sets.newHashSet(SOURCE_NORMAL, SOURCE_FAILED));
        SOURCE_STATE_AUTOMATON.put(BEEN_ISSUED_BACKTRACK, Sets.newHashSet(SOURCE_NORMAL, SOURCE_FAILED));
        SOURCE_STATE_AUTOMATON.put(BEEN_ISSUED_FROZEN, Sets.newHashSet(SOURCE_NORMAL, SOURCE_FAILED));
        SOURCE_STATE_AUTOMATON.put(BEEN_ISSUED_ACTIVE, Sets.newHashSet(SOURCE_NORMAL, SOURCE_FAILED));
        SOURCE_STATE_AUTOMATON.put(BEEN_ISSUED_CHECK, Sets.newHashSet(SOURCE_NORMAL, SOURCE_FAILED));
        SOURCE_STATE_AUTOMATON.put(BEEN_ISSUED_REDO_METRIC, Sets.newHashSet(SOURCE_NORMAL, SOURCE_FAILED));
        SOURCE_STATE_AUTOMATON.put(BEEN_ISSUED_MAKEUP, Sets.newHashSet(SOURCE_NORMAL, SOURCE_FAILED));
    }

    private final Integer code;
    private final String description;

    SourceStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Get state from the given code
     */
    public static SourceStatus forCode(int code) {
        for (SourceStatus state : values()) {
            if (state.getCode() == code) {
                return state;
            }
        }
        throw new IllegalStateException(String.format("Illegal code=%s for SourceState", code));
    }

    /**
     * Whether the `next` state is valid according to the `current` state.
     */
    public static boolean isAllowedTransition(SourceStatus current, SourceStatus next) {
        Set<SourceStatus> nextStates = SOURCE_STATE_AUTOMATON.get(current);
        return nextStates != null && nextStates.contains(next);
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
