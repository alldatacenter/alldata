/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.app.common;

import com.baomidou.mybatisplus.annotation.EnumValue;

import java.util.HashMap;

/** running status for workflow and task nodes */
public enum ExecutionStatus {

    /**
     * status： 0 submit success 1 running 2 ready pause 3 pause 4 ready stop 5 stop 6 failure 7
     * success 8 need fault tolerance 9 kill 10 waiting thread 11 waiting depend node complete 12
     * delay execution 13 forced success 14 serial wait 15 ready block 16 block 17 dispatch
     */
    SUBMITTED_SUCCESS(0, "submit success"),
    RUNNING_EXECUTION(1, "running"),
    READY_PAUSE(2, "ready pause"),
    PAUSE(3, "pause"),
    READY_STOP(4, "ready stop"),
    STOP(5, "stop"),
    FAILURE(6, "failure"),
    SUCCESS(7, "success"),
    NEED_FAULT_TOLERANCE(8, "need fault tolerance"),
    KILL(9, "kill"),
    WAITING_THREAD(10, "waiting thread"),
    WAITING_DEPEND(11, "waiting depend node complete"),
    DELAY_EXECUTION(12, "delay execution"),
    FORCED_SUCCESS(13, "forced success"),
    SERIAL_WAIT(14, "serial wait"),
    READY_BLOCK(15, "ready block"),
    BLOCK(16, "block"),
    DISPATCH(17, "dispatch"),
    PAUSE_BY_ISOLATION(18, "paused by isolation"),
    KILL_BY_ISOLATION(19, "killed by isolation"),

    PAUSE_BY_CORONATION(20, "paused by coronation"),
    FORBIDDEN_BY_CORONATION(21, "forbidden by coronation"),
    ;

    ExecutionStatus(int code, String descp) {
        this.code = code;
        this.descp = descp;
    }

    @EnumValue private final int code;
    private final String descp;

    private static HashMap<Integer, ExecutionStatus> EXECUTION_STATUS_MAP = new HashMap<>();

    private static final int[] NEED_FAILOVER_STATES =
            new int[] {
                ExecutionStatus.SUBMITTED_SUCCESS.ordinal(),
                ExecutionStatus.DISPATCH.ordinal(),
                ExecutionStatus.RUNNING_EXECUTION.ordinal(),
                ExecutionStatus.DELAY_EXECUTION.ordinal(),
                ExecutionStatus.READY_PAUSE.ordinal(),
                ExecutionStatus.READY_STOP.ordinal()
            };

    static {
        for (ExecutionStatus executionStatus : ExecutionStatus.values()) {
            EXECUTION_STATUS_MAP.put(executionStatus.code, executionStatus);
        }
    }

    /**
     * status is success
     *
     * @return status
     */
    public boolean typeIsSuccess() {
        return this == SUCCESS || this == FORCED_SUCCESS || this == FORBIDDEN_BY_CORONATION;
    }

    /**
     * status is failure
     *
     * @return status
     */
    public boolean typeIsFailure() {
        return this == FAILURE || this == NEED_FAULT_TOLERANCE;
    }

    /**
     * status is finished
     *
     * @return status
     */
    public boolean typeIsFinished() {
        return typeIsSuccess()
                || typeIsFailure()
                || typeIsCancel()
                || typeIsPause()
                || typeIsPauseByIsolation()
                || typeIsStop()
                || typeIsBlock()
                || typeIsPauseByCoronation()
                || typeIsForbiddenByCoronation();
    }

    public boolean typeIsReady() {
        return this == READY_PAUSE || this == READY_STOP || this == READY_BLOCK;
    }

    /**
     * status is waiting thread
     *
     * @return status
     */
    public boolean typeIsWaitingThread() {
        return this == WAITING_THREAD;
    }

    /**
     * status is pause
     *
     * @return status
     */
    public boolean typeIsPause() {
        return this == PAUSE;
    }

    public boolean typeIsPauseByIsolation() {
        return this == PAUSE_BY_ISOLATION;
    }

    public boolean typeIsPauseByCoronation() {
        return this == PAUSE_BY_CORONATION;
    }

    public boolean typeIsForbiddenByCoronation() {
        return this == FORBIDDEN_BY_CORONATION;
    }

    public boolean typeIsKilledByIsolation() {
        return this == KILL_BY_ISOLATION;
    }

    public boolean typeIsIsolated() {
        return this == PAUSE_BY_ISOLATION || this == KILL_BY_ISOLATION;
    }

    /**
     * status is pause
     *
     * @return status
     */
    public boolean typeIsStop() {
        return this == STOP;
    }

    /**
     * status is running
     *
     * @return status
     */
    public boolean typeIsRunning() {
        return this == RUNNING_EXECUTION || this == WAITING_DEPEND || this == DELAY_EXECUTION;
    }

    /**
     * status is block
     *
     * @return status
     */
    public boolean typeIsBlock() {
        return this == BLOCK;
    }

    /**
     * status is cancel
     *
     * @return status
     */
    public boolean typeIsCancel() {
        return this == KILL || this == STOP || this == KILL_BY_ISOLATION;
    }

    public int getCode() {
        return code;
    }

    public String getDescp() {
        return descp;
    }

    public static ExecutionStatus of(int status) {
        if (EXECUTION_STATUS_MAP.containsKey(status)) {
            return EXECUTION_STATUS_MAP.get(status);
        }
        throw new IllegalArgumentException("invalid status : " + status);
    }

    public static boolean isNeedFailoverWorkflowInstanceState(ExecutionStatus executionStatus) {
        return ExecutionStatus.SUBMITTED_SUCCESS == executionStatus
                || ExecutionStatus.DISPATCH == executionStatus
                || ExecutionStatus.RUNNING_EXECUTION == executionStatus
                || ExecutionStatus.DELAY_EXECUTION == executionStatus
                || ExecutionStatus.READY_PAUSE == executionStatus
                || ExecutionStatus.READY_STOP == executionStatus;
    }

    public static int[] getNeedFailoverWorkflowInstanceState() {
        return NEED_FAILOVER_STATES;
    }

    public static int[] getRunningProcessState() {
        return new int[] {
            ExecutionStatus.RUNNING_EXECUTION.getCode(),
            ExecutionStatus.SUBMITTED_SUCCESS.getCode(),
            ExecutionStatus.DISPATCH.getCode(),
            ExecutionStatus.SERIAL_WAIT.getCode()
        };
    }

    public static int[] getNotTerminatedStates() {
        return new int[] {
            ExecutionStatus.SUBMITTED_SUCCESS.getCode(),
            ExecutionStatus.DISPATCH.getCode(),
            ExecutionStatus.RUNNING_EXECUTION.getCode(),
            ExecutionStatus.DELAY_EXECUTION.getCode(),
            ExecutionStatus.READY_PAUSE.getCode(),
            ExecutionStatus.READY_STOP.getCode(),
            ExecutionStatus.NEED_FAULT_TOLERANCE.getCode(),
            ExecutionStatus.WAITING_THREAD.getCode(),
            ExecutionStatus.WAITING_DEPEND.getCode()
        };
    }

    public boolean canStop() {
        return typeIsRunning() || this == READY_PAUSE;
    }
}
