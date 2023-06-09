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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Describes a finite state machine in terms of a mapping of tokens to
 * characters, a regular expression describing the valid transitions
 * using the characters, and an end state. Used to validate state
 * transitions. One simple example is the implementation of the
 * Check*VisitorFsm classes used to validate call sequences in ASM
 * visitors (classes like CheckClassAdapter only validate per-call
 * arguments, not the entire sequence of calls, according to
 * http://asm.ow2.org/asm50/javadoc/user/org/objectweb/asm/util/CheckClassAdapter.html
 *
 * <p>The current implementation is very simple, and requires some user setup.
 * Basically, we use Java's {@link java.util.regex.Pattern} and
 * {@link java.util.regex.Matcher} to verify a regular expression on demand.
 * The user must map state transitions' string names onto characters, and
 * specify a regex that describes the state machine. Using this technique, we
 * can only validate when we transition to an end state; we just check to see
 * if accumulated characters comprise an allowed regular expression.
 *
 * <p>In this simple implementation, the tokens and characters may represent
 * states or transitions, depending on what is most convenient. In either case,
 * we just check that a sequence of them matches the regular expression governing
 * this state machine.
 */
public class FsmDescriptor {
  private final Map<String, Character> tokenMap; // state/transition -> character
  private final Pattern fsmPattern; // compiled regex representing the FSM
  private final char lastTransition; // the character that represents the last state/transition

  /**
   * Create a finite state machine descriptor. The descriptor is immutable, and may
   * be shared across many cursors that are executing the FSM.
   *
   * @param tokenMap mapping of transitions/states to characters
   * @param fsmRegex the regular expression, defined using the characters from the tokenMap
   * @param lastTransition the name of the final transition/state
   */
  public FsmDescriptor(final Map<String, Character> tokenMap,
      final String fsmRegex, final String lastTransition) {
    Preconditions.checkNotNull(tokenMap);
    Preconditions.checkNotNull(fsmRegex);
    Preconditions.checkNotNull(lastTransition);
    Preconditions.checkArgument(tokenMap.containsKey(lastTransition));

    // make sure the characters in the tokenMap are unique
    final HashSet<Character> charSet = new HashSet<>();
    for(Map.Entry<String, Character> me : tokenMap.entrySet()) {
      final Character character = me.getValue();
      if (charSet.contains(character)) {
        throw new IllegalArgumentException("Duplicate tokenMap char: '" + character + "'");
      }
      charSet.add(character);
    }

    this.tokenMap = Collections.unmodifiableMap(tokenMap);
    this.fsmPattern = Pattern.compile(fsmRegex);
    this.lastTransition = this.tokenMap.get(lastTransition).charValue();
  }

  /**
   * Create a cursor for performing and validating transitions according to this
   * state machine.
   *
   * @return the new cursor
   */
  public FsmCursor createCursor() {
    return new FsmCursor(this);
  }

  /**
   * Validate the given transitions against this state machine.
   *
   * @param transitions a character sequence indicating a set of transitions or
   *   states as defined by the tokenMap used at construction time.
   * @throws IllegalStateException if the set of transitions is not allowed
   */
  void validateTransitions(final CharSequence transitions) {
    final long length = transitions.length();
    if (length == 0) {
      return; // assume we haven't started yet
    }

    final Matcher matcher = fsmPattern.matcher(transitions);
    if (!matcher.find() || (matcher.start() != 0) || (matcher.end() != length)) {
      throw new IllegalStateException("Illegal state transition(s): " + transitions);
    }
  }

  /**
   * Look up the character used to represent the transition or state denoted by
   * the given token.
   *
   * @param token the transition or state to look up
   * @return the character used to represent that transition or state
   */
  char getChar(final String token) {
    Preconditions.checkNotNull(token);
    final Character character = tokenMap.get(token);
    Preconditions.checkNotNull(character);
    return character.charValue();
  }

  /**
   * Determine whether the indicated character represents the final transition or state.
   *
   * @param transitionChar the character to look up
   * @return true if the character represents the final transition or state
   */
  boolean isLastTransition(final char transitionChar) {
    return transitionChar == lastTransition;
  }
}
