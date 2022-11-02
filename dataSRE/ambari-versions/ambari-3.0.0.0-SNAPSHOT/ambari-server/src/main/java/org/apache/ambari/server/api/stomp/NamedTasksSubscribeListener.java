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
package org.apache.ambari.server.api.stomp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
public class NamedTasksSubscribeListener {
  private static Logger LOG = LoggerFactory.getLogger(NamedTasksSubscribeListener.class);

  @Autowired
  private NamedTasksSubscriptions namedTasksSubscriptions;

  @EventListener
  public void subscribe(SessionSubscribeEvent sse)
  {
    MessageHeaders msgHeaders = sse.getMessage().getHeaders();
    String sessionId  = (String) msgHeaders.get("simpSessionId");
    String destination  = (String) msgHeaders.get("simpDestination");
    String id  = (String) msgHeaders.get("simpSubscriptionId");
    if (sessionId != null && destination != null && id != null) {
      namedTasksSubscriptions.addDestination(sessionId, destination, id);
    }
    LOG.info(String.format("API subscribe was arrived with sessionId = %s, destination = %s and id = %s",
        sessionId, destination, id));
  }

  @EventListener
  public void unsubscribe(SessionUnsubscribeEvent suse)
  {
    MessageHeaders msgHeaders = suse.getMessage().getHeaders();
    String sessionId  = (String) msgHeaders.get("simpSessionId");
    String id  = (String) msgHeaders.get("simpSubscriptionId");
    if (sessionId != null && id != null) {
      namedTasksSubscriptions.removeId(sessionId, id);
    }
    LOG.info(String.format("API unsubscribe was arrived with sessionId = %s and id = %s",
        sessionId, id));
  }

  @EventListener
  public void disconnect(SessionDisconnectEvent sde)
  {
    MessageHeaders msgHeaders = sde.getMessage().getHeaders();
    String sessionId  = (String) msgHeaders.get("simpSessionId");
    if (sessionId != null) {
      namedTasksSubscriptions.removeSession(sessionId);
    }
    LOG.info(String.format("API disconnect was arrived with sessionId = %s",
        sessionId));
  }
}
