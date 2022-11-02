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
package org.apache.ambari.server.notifications;

import org.apache.ambari.server.state.alert.AlertTarget;

/**
 * The {@link TargetConfigurationResult} is used to validate if an
 * {@link AlertTarget} 's configuration is valid and the dispatcher for its type
 * can successfully deliver a {@link Notification}.
 */
public class TargetConfigurationResult {

  public enum Status {
    VALID, INVALID
  }

  private String message;
  private Status status;

  private TargetConfigurationResult(Status status, String message) {
    this.message = message;
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public Status getStatus() {
    return status;
  }

  public static TargetConfigurationResult valid() {
    return new TargetConfigurationResult(Status.VALID, "Configuration is valid");
  }

  public static TargetConfigurationResult invalid(String message) {
    return new TargetConfigurationResult(Status.INVALID, message);
  }
}
