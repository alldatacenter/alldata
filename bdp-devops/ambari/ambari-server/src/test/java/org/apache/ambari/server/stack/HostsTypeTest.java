/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.stack;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.LinkedHashSet;

import org.junit.Test;


public class HostsTypeTest {
  @Test
  public void testGuessMasterFrom1() {
    HostsType hosts = HostsType.guessHighAvailability(newLinkedHashSet(asList("c6401")));
    assertThat(hosts.getMasters(), is(singleton("c6401")));
    assertThat(hosts.getSecondaries(), hasSize(0));
  }

  @Test
  public void testGuessMasterFrom3() {
    HostsType hosts = HostsType.guessHighAvailability(newLinkedHashSet(asList("c6401", "c6402", "c6403")));
    assertThat(hosts.getMasters(), is(singleton("c6401")));
    assertThat(hosts.getSecondaries(), is(newLinkedHashSet(asList("c6402", "c6403"))));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGuessMasterFromEmptyList() {
    HostsType.guessHighAvailability(new LinkedHashSet<>(emptySet()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMasterIsMandatory() {
    new HostsType.HighAvailabilityHosts(null, emptyList());
  }

  @Test
  public void testFederatedMastersAndSecondaries() {
    HostsType federated = HostsType.federated(asList(
      new HostsType.HighAvailabilityHosts("master1", asList("sec1", "sec2")),
      new HostsType.HighAvailabilityHosts("master2", asList("sec3", "sec4"))),
      new LinkedHashSet<>(emptySet()));
    assertThat(federated.getMasters(), is(newHashSet("master1", "master2")));
    assertThat(federated.getSecondaries(), is(newHashSet("sec1", "sec2", "sec3", "sec4")));
  }

  @Test
  public void testArrangeHosts() {
    HostsType federated = HostsType.federated(asList(
      new HostsType.HighAvailabilityHosts("master1", asList("sec1", "sec2")),
      new HostsType.HighAvailabilityHosts("master2", asList("sec3", "sec4"))),
      new LinkedHashSet<>(emptySet()));
    federated.arrangeHostSecondariesFirst();
    assertThat(federated.getHosts(), is(newLinkedHashSet(asList("sec1", "sec2", "master1", "sec3", "sec4", "master2"))));
  }
}