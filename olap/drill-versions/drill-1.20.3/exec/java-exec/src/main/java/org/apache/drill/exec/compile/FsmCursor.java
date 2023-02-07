/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.compile;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Maintains state while traversing a finite state machine described by
 * an FsmDescriptor. To begin, use {@link FsmDescriptor#createCursor() createCursor}.
 */
public class FsmCursor {
  private final FsmDescriptor fsmDescriptor;
  private final StringBuilder stringBuilder;

  /**
   * Constructor.
   *
   * @param fsmDescriptor the descriptor for the FSM this cursor will track
   */
  FsmCursor(final FsmDescriptor fsmDescriptor) {
    this.fsmDescriptor = fsmDescriptor;
    stringBuilder = new StringBuilder();
  }

  /**
   * Record a transition in the state machine.
   *
   * <p>Validates the transition using {@link FsmDescriptor#validateTransitions(CharSequence)}.
   *
   * @param token the name of the transition
   */
  public void transition(final String token) {
    Preconditions.checkNotNull(token);
    final char c = fsmDescriptor.getChar(token);
    stringBuilder.append(c);

    if (fsmDescriptor.isLastTransition(c)) {
      fsmDescriptor.validateTransitions(stringBuilder);
    }
  }

  /**
   * Resets the state machine, setting it back to the start state.
   */
  public void reset() {
    stringBuilder.setLength(0);
  }
}
