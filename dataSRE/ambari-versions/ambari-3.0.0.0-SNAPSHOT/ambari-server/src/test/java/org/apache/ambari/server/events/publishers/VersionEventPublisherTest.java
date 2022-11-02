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

package org.apache.ambari.server.events.publishers;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import org.apache.ambari.server.events.HostComponentVersionAdvertisedEvent;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * VersionEventPublisher tests.
 */
public class VersionEventPublisherTest {

  private Injector injector;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector();
  }

  @Test
  public void testPublish() throws Exception {

    Cluster cluster = createNiceMock(Cluster.class);
    ServiceComponentHost sch = createNiceMock(ServiceComponentHost.class);

    expect(cluster.getClusterId()).andReturn(99L);

    replay(cluster, sch);

    VersionEventPublisher publisher = injector.getInstance(VersionEventPublisher.class);

    Listener listener = injector.getInstance(Listener.class);

    HostComponentVersionAdvertisedEvent event = new HostComponentVersionAdvertisedEvent(cluster, sch, "1.2.3.4-5678");

    publisher.publish(event);

    assertEquals(event, listener.getLastEvent());

    verify(cluster, sch);
  }

  private static class Listener {

    private HostComponentVersionAdvertisedEvent lastEvent = null;

    @Inject
    private Listener(VersionEventPublisher eventPublisher) {
      eventPublisher.register(this);
    }

    @Subscribe
    public void onEvent(HostComponentVersionAdvertisedEvent event) {
      lastEvent = event;
    }

    public HostComponentVersionAdvertisedEvent getLastEvent() {
      return lastEvent;
    }
  }
}
