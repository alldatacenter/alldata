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

package org.apache.inlong.agent.state;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractStateWrapper implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStateWrapper.class);

    private final Map<Pair<State, State>, StateCallback> callBacks = new HashMap<>();

    private volatile State currentState = State.ACCEPTED;

    public AbstractStateWrapper() {
        addCallbacks();
    }

    /**
     * add callback for state change
     */
    public abstract void addCallbacks();

    public AbstractStateWrapper addCallback(State begin, State end, StateCallback callback) {
        callBacks.put(new ImmutablePair<>(begin, end), callback);
        return this;
    }

    /**
     * change state and execute callback functions
     *
     * @param nextState next state
     */
    public synchronized void doChangeState(State nextState) {
        LOGGER.debug("state change, current state is {}, next state is {}", currentState, nextState);
        Pair<State, State> statePair = new ImmutablePair<>(currentState, nextState);
        StateCallback callback = callBacks.get(statePair);
        // change state before callback.
        currentState = nextState;
        if (callback != null) {
            callback.call(currentState, nextState);
        }
    }

    /**
     * determine the exception
     */
    public boolean isException() {
        State tmpState = currentState;
        return State.KILLED.equals(tmpState) || State.FAILED.equals(tmpState) || State.FATAL.equals(tmpState);
    }

    public boolean isFinished() {
        State tmpState = currentState;
        return State.FATAL.equals(tmpState) || State.SUCCEEDED.equals(tmpState) || State.KILLED.equals(tmpState);
    }

    public boolean isSuccess() {
        return State.SUCCEEDED.equals(currentState);
    }

    public boolean isFailed() {
        return State.FAILED.equals(currentState);
    }

    public boolean isFatal() {
        State tmpState = currentState;
        return State.FATAL.equals(tmpState) || State.KILLED.equals(tmpState);
    }

    public State getCurrentState() {
        return currentState;
    }
}
