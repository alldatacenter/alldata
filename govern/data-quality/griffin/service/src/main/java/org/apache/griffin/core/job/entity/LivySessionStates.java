/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.job.entity;

import static org.apache.griffin.core.job.entity.LivySessionStates.State.DEAD;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.FINDING;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.FOUND;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.NOT_FOUND;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.NOT_STARTED;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.RUNNING;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.SHUTTING_DOWN;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.STARTING;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.STOPPED;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.SUCCESS;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.UNKNOWN;

import com.cloudera.livy.sessions.SessionState;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class LivySessionStates {

    /**
     * UNKNOWN is used to represent the state that server get null from Livy.
     * the other state is just same as com.cloudera.livy.sessions.SessionState.
     */
    public enum State {
        NOT_STARTED,
        STARTING,
        RECOVERING,
        IDLE,
        RUNNING,
        BUSY,
        SHUTTING_DOWN,
        ERROR,
        DEAD,
        SUCCESS,
        UNKNOWN,
        STOPPED,
        FINDING,
        NOT_FOUND,
        FOUND
    }

    private static SessionState toSessionState(State state) {
        if (state == null) {
            return null;
        }
        switch (state) {
            case NOT_STARTED:
                return new SessionState.NotStarted();
            case STARTING:
                return new SessionState.Starting();
            case RECOVERING:
                return new SessionState.Recovering();
            case IDLE:
                return new SessionState.Idle();
            case RUNNING:
                return new SessionState.Running();
            case BUSY:
                return new SessionState.Busy();
            case SHUTTING_DOWN:
                return new SessionState.ShuttingDown();
            case ERROR:
                return new SessionState.Error(System.nanoTime());
            case DEAD:
                return new SessionState.Dead(System.nanoTime());
            case SUCCESS:
                return new SessionState.Success(System.nanoTime());
            default:
                return null;
        }
    }

    public static State toLivyState(JsonObject object) {
        if (object != null) {
            JsonElement state = object.get("state");
            JsonElement finalStatus = object.get("finalStatus");
            State finalState = parseState(state);
            return finalState != null ? finalState : parseState(finalStatus);
        }
        return UNKNOWN;
    }

    private static State parseState(JsonElement state) {
        if (state == null) {
            return null;
        }
        switch (state.getAsString()) {
            case "NEW":
            case "NEW_SAVING":
            case "SUBMITTED":
                return NOT_STARTED;
            case "ACCEPTED":
                return STARTING;
            case "RUNNING":
                return RUNNING;
            case "SUCCEEDED":
                return SUCCESS;
            case "FAILED":
                return DEAD;
            case "KILLED":
                return SHUTTING_DOWN;
            case "FINISHED":
                return null;
            default:
                return UNKNOWN;
        }
    }

    public static boolean isActive(State state) {
        if (UNKNOWN.equals(state) || STOPPED.equals(state) || NOT_FOUND.equals
            (state) || FOUND.equals(state)) {
            // set UNKNOWN isActive() as false.
            return false;
        } else if (FINDING.equals(state)) {
            return true;
        }
        SessionState sessionState = toSessionState(state);
        return sessionState != null && sessionState.isActive();
    }

    public static String convert2QuartzState(State state) {
        SessionState sessionState = toSessionState(state);
        if (STOPPED.equals(state) || SUCCESS.equals(state)) {
            return "COMPLETE";
        }
        if (UNKNOWN.equals(state) || NOT_FOUND.equals(state)
            || FOUND.equals(state) || sessionState == null
            || !sessionState.isActive()) {
            return "ERROR";
        }
        return "NORMAL";

    }

    public static boolean isHealthy(State state) {
        return !(State.ERROR.equals(state) || State.DEAD.equals(state)
            || State.SHUTTING_DOWN.equals(state)
            || State.FINDING.equals(state)
            || State.NOT_FOUND.equals(state)
            || State.FOUND.equals(state));
    }
}
