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

package org.apache.drill.exec.store.pcap.decoder;

/**
 * This class is used to record the status of the TCP Handshake. Initially this is used just to determine whether a session is open or closed, but
 * future functionality could include SYN flood identification, or other hackery with TCP flags.
 */
public class TcpHandshake {
  private boolean isConnected = false;

  private State currentSessionState = State.NONE;

  private long sessionID;

  /**
   * Returns true for a correct TCP handshake: SYN|SYNACK|ACK, False if not.
   *
   * @return boolean true if the session is open, false if not.
   */
  public boolean isConnected() {
    return isConnected;
  }

  /**
   * This method returns true if the session is closed properly via FIN -> FIN ACK, false if not.
   *
   * @return boolean true if the session is closed, false if not.
   */
  public boolean isClosed() {
    if (currentSessionState == State.CLOSE_WAIT ||
      currentSessionState == State.FORCED_CLOSED ||
      currentSessionState == State.CLOSED ||
      currentSessionState == State.TIME_WAIT ||
      currentSessionState == State.FIN_WAIT) {
      return true;
    } else {
      return false;
    }
  }

  public State getCurrentSessionState() {
    return currentSessionState;
  }

  public void setConnected(long sessionID) {
    this.sessionID = sessionID;
    currentSessionState = State.OPEN;
    isConnected = true;
  }

  public void setRst() {
    isConnected = false;
    currentSessionState = State.FORCED_CLOSED;
  }

  public void setFin() {
    if (currentSessionState == State.OPEN) {
      currentSessionState = State.CLOSE_WAIT;   // The next packet should be another FIN packet
    } else if (currentSessionState == State.CLOSE_WAIT) {
      currentSessionState = State.TIME_WAIT;
    }
  }

  public void setAck() {
    if (currentSessionState == State.SYN) {
      currentSessionState = State.SYNACK;
    } else if (currentSessionState == State.SYNACK) {
      currentSessionState = State.OPEN;
      isConnected = true;
    } else if (currentSessionState == State.CLOSE_WAIT) {
      currentSessionState = State.FIN_WAIT;
    }
  }

  public void setSyn() {
    if (currentSessionState == State.NONE) {
      currentSessionState = State.SYN;
    }
  }

  /**
   * This enum variable represents the various states of the TCP Handshake
   */
  enum State {
    /**
     * The NONE state is the initialization state. No session has be established
     */
    NONE,
    /**
     * The OPEN state represents a successfully opened TCP session. It is established in the final step in the TCP
     * handshake.
     */
    OPEN,
    /**
     * The CLOSED session represents a closed TCP session. This state occurs after the final ACK of the 4 way close process
     */
    CLOSED,
    /**
     * The CLOSE_WAIT state represents a session in which one party has sent a frame with a FIN flag set.
     * At this point resources can be released, however, to fully close the session the other party needs to send a frame
     * with an ACK packet.
    */
    CLOSE_WAIT,
    /**
     * This state occurs after receiving the first FIN/ACK frame.  The recipient will then follow with a FIN frame, closing the session.
     */
    TIME_WAIT,
    /**
     * The SYN state represents the first step in the TCP handshake. The originator has sent a frame with the SYN flag set.
     * The next step would be the SYN/ACK stage.
     */
    SYN,
    /**
     * This step represents the second step of the TCP handshake in which the recipient acknowledges the originator's SYN
     * flag.
     */
    SYNACK,
    /**
     * The FORCED_CLOSED state represents a session which was closed forcefully by a RST frame.
     */
    FORCED_CLOSED,
    /**
     * The FIN_WAIT state occurs after receiving the initial FIN flag.
     */
    FIN_WAIT
  }
}
