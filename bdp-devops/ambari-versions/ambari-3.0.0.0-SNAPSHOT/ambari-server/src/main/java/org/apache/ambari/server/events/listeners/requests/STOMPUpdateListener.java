/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.events.listeners.requests;

import java.util.Collections;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.events.DefaultMessageEmitter;
import org.apache.ambari.server.events.STOMPEvent;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Injector;

public class STOMPUpdateListener {

  @Autowired
  private DefaultMessageEmitter defaultMessageEmitter;

  private final Set<STOMPEvent.Type> typesToProcess;

  public STOMPUpdateListener(Injector injector, Set<STOMPEvent.Type> typesToProcess) {
    STOMPUpdatePublisher STOMPUpdatePublisher =
      injector.getInstance(STOMPUpdatePublisher.class);
    STOMPUpdatePublisher.registerAgent(this);
    STOMPUpdatePublisher.registerAPI(this);
    this.typesToProcess = typesToProcess == null ? Collections.emptySet() : typesToProcess;
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onUpdateEvent(STOMPEvent event) throws AmbariException, InterruptedException {
    if (typesToProcess.contains(event.getType())) {
      defaultMessageEmitter.emitMessage(event);
    }
  }
}
