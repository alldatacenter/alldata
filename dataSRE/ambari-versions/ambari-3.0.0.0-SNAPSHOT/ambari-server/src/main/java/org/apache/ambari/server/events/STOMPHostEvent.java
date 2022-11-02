/**
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
package org.apache.ambari.server.events;

import java.beans.Transient;

/**
 * Update data from server side, will be sent as STOMP message only to specified recipient.
 */
public abstract class STOMPHostEvent extends STOMPEvent {

  /**
   * Host id message will sent to.
   * @return host id.
   */
  @Transient
  public abstract Long getHostId();

  public STOMPHostEvent(Type type) {
    super(type);
  }
}
