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

package org.apache.ambari.server.state.svccomphost;

import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.ServiceComponentHostEventType;

/**
 * Base class for all events that represent server-side actions.
 */
public class ServiceComponentHostServerActionEvent extends
    ServiceComponentHostEvent {

  /**
   * Constructs a new ServiceComponentHostServerActionEvent.
   * <p/>
   * This method is expected to be called by ether a ServiceComponentHostServerActionEvent or a
   * class that extends it.
   *
   * @param type                 the ServiceComponentHostEventType - expected to be
   *                             ServiceComponentHostEventType.HOST_SVCCOMP_SERVER_ACTION
   * @param serviceComponentName a String declaring the component for which this action is to be
   *                             routed - expected to be "AMBARI_SERVER"
   * @param hostName             a String declaring the host on which the action should be executed -
   *                             expected to be the hostname of the Ambari server
   * @param opTimestamp          the time in which this event was created
   * @param stackId              the relevant stackid
   */
  protected ServiceComponentHostServerActionEvent(ServiceComponentHostEventType type,
                                                  String serviceComponentName, String hostName,
                                                  long opTimestamp, String stackId) {
    super(type, serviceComponentName, hostName, opTimestamp, stackId);
  }

  /**
   * Constructs a new ServiceComponentHostServerActionEvent where the component name is set to
   * "AMBARI_SERVER" and the type is set to ServiceComponentHostEventType.HOST_SVCCOMP_SERVER_ACTION.
   *
   * @param hostName    a String declaring the host on which the action should be executed -
   *                    expected to be the hostname of the Ambari server
   * @param opTimestamp the time in which this event was created
   */
  public ServiceComponentHostServerActionEvent(String hostName, long opTimestamp) {
    this("AMBARI_SERVER", hostName, opTimestamp);
  }

  /**
   * Constructs a new ServiceComponentHostServerActionEvent
   *
   * @param serviceComponentName a String declaring the name of component
   * @param hostName             a String declaring the host on which the action should be executed -
   *                             expected to be the hostname of the Ambari server
   * @param opTimestamp          the time in which this event was created
   */
  public ServiceComponentHostServerActionEvent(String serviceComponentName, String hostName,
                                               long opTimestamp) {
    this(ServiceComponentHostEventType.HOST_SVCCOMP_SERVER_ACTION, serviceComponentName, hostName,
        opTimestamp, "");
  }
}
