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
package org.apache.drill.common.exceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.drill.exec.proto.CoordinationProtos;

/**
 * Holds context information about a DrillUserException. We can add structured context information that will added
 * to the error message displayed to the client.
 */
class UserExceptionContext {

  private static final String NEW_LINE = System.lineSeparator();

  private final String errorId;
  private final List<String> contextList;

  private CoordinationProtos.DrillbitEndpoint endpoint;

  UserExceptionContext() {
    errorId = UUID.randomUUID().toString();
    contextList = new ArrayList<>();
  }

  /**
   * adds a string to the bottom of the context list
   * @param context context string
   */
  void add(String context) {
    contextList.add(context);
  }

  /**
   * add DrillbitEndpoint identity to the context.
   * <p>if the context already has a drillbitEndpoint identity, the new identity will be ignored
   *
   * @param endpoint drillbit endpoint identity
   */
  void add(CoordinationProtos.DrillbitEndpoint endpoint) {
    if (this.endpoint == null) {
      this.endpoint = endpoint;
    }
  }

  /**
   * adds a sring value to the bottom of the context list
   * @param context context name
   * @param value context value
   */
  void add(String context, String value) {
    add(deColon(context) + ": " + value);
  }

  /**
   * adds a long value to the bottom of the context list
   * @param context context name
   * @param value context value
   */
  void add(String context, long value) {
    add(deColon(context) + ": " + value);
  }

  /**
   * adds a double to the bottom of the context list
   * @param context context name
   * @param value context value
   */
  void add(String context, double value) {
    add(deColon(context) + ": " + value);
  }

  private String deColon(String context) {
    context = context.trim();
    if (context.endsWith(":")) {
      return context.substring(0, context.length() - 1);
    } else {
      return context;
    }
  }

  /**
   * pushes a string to the top of the context list
   * @param context context string
   */
  void push(String context) {
    contextList.add(0, context);
  }

  /**
   * pushes a string value to the top of the context list
   * @param context context name
   * @param value context value
   */
  void push(String context, String value) {
    push(deColon(context) + ": " + value);
  }

  /**
   * pushes a long value to the top of the context list
   * @param context context name
   * @param value context value
   */
  void push(String context, long value) {
    push(deColon(context) + ": " + value);
  }

  /**
   * adds a double at the top of the context list
   * @param context context name
   * @param value context value
   */
  void push(String context, double value) {
    push(deColon(context) + ": " + value);
  }

  String getErrorId() {
    return errorId;
  }

  CoordinationProtos.DrillbitEndpoint getEndpoint() {
    return endpoint;
  }

  /**
   * generate a context message
   * @return string containing all context information concatenated
   */
  String generateContextMessage(boolean includeErrorIdAndIdentity, boolean includeSeeLogsMessage) {
    StringBuilder sb = new StringBuilder();

    for (String context : contextList) {
      sb.append(context)
          .append(NEW_LINE);
    }

    if (includeSeeLogsMessage) {
      sb.append(NEW_LINE)
          .append("Please, refer to logs for more information.")
          .append(NEW_LINE);
    }

    if (includeErrorIdAndIdentity) {
      // add identification infos
      sb.append(NEW_LINE)
          .append("[Error Id: ")
          .append(errorId)
          .append(" ");
      if (endpoint != null) {
        sb.append("on ")
            .append(endpoint.getAddress())
            .append(":")
            .append(endpoint.getUserPort());
      }
      sb.append("]");
    }
    return sb.toString();
  }
}
