/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.serveraction;

import java.util.Date;

import org.apache.commons.lang.time.FastDateFormat;

/**
 * ActionLog is a class for logging progress of ServerAction execution.
 */
public class ActionLog {
  /**
   * The StringBuffer to hold the messages logged to STDERR
   */
  private StringBuffer stdErr = new StringBuffer();

  /**
   * The StringBuffer to hold the messages logged to STDOUT
   */
  private StringBuffer stdOut = new StringBuffer();

  /**
   * A date formatter to use to format timestamps
   * <p/>
   * This is a thread-safe version of a date formatter
   */
  private FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss,SSS");

  /**
   * Append message to stdErr of action log.
   *
   * @param message text to append
   */
  public void writeStdErr(String message) {
    write(stdErr, message);
  }

  /**
   * Append message to stdOut of action log.
   *
   * @param message text to append
   */
  public void writeStdOut(String message) {
    write(stdOut, message);
  }

  /**
   * Return all text from stdErr.
   *
   * @return text of stdErr
   */
  public String getStdErr() {
    return stdErr.toString();
  }

  /**
   * Return all text from stdOut.
   *
   * @return text of stdOut
   */
  public String getStdOut() {
    return stdOut.toString();
  }


  /**
   * Appends a message to the specified buffer
   *
   * @param buffer  the StringBuffer to use to append the formatted message
   * @param message a String containing the message to log
   */
  private void write(StringBuffer buffer, String message) {
    if (message != null) {
      Date date = new Date();
      buffer.append(dateFormat.format(date));
      buffer.append(" - ");
      buffer.append(message);
      buffer.append("\n");
    }
  }

}
