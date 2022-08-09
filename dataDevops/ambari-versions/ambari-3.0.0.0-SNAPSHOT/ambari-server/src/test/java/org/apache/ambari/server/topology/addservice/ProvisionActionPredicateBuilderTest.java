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
package org.apache.ambari.server.topology.addservice;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.topology.ProvisionStep;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ProvisionActionPredicateBuilderTest {

  private static final String CLUSTER_NAME = "TEST";
  private static final Map<String, Map<String, Set<String>>> NEW_SERVICES = ImmutableMap.of(
    "AMBARI_METRICS", ImmutableMap.of(
      "METRICS_COLLECTOR", ImmutableSet.of("c7401"),
      "METRICS_GRAFANA", ImmutableSet.of("c7403"),
      "METRICS_MONITOR", ImmutableSet.of("c7401", "c7402", "c7403", "c7404", "c7405")
    ),
    "KAFKA", ImmutableMap.of(
      "KAFKA_BROKER", ImmutableSet.of("c7402", "c7404")
    ),
    "ZOOKEEPER", ImmutableMap.of(
      "ZOOKEEPER_SERVER", ImmutableSet.of("c7401", "c7402", "c7403"),
      "ZOOKEEPER_CLIENT", ImmutableSet.of("c7404")
    )
  );
  private static final RequestStageContainer STAGES = new RequestStageContainer(42L, null, null, null, null);
  private static final AddServiceInfo.Builder ADD_SERVICE_INFO_BUILDER = new AddServiceInfo.Builder()
    .setClusterName(CLUSTER_NAME).setStages(STAGES).setNewServices(NEW_SERVICES);

  @Test
  public void noCustomProvisionAction() {
    AddServiceRequest request = createRequest(null, null, null);
    AddServiceInfo info = ADD_SERVICE_INFO_BUILDER.setRequest(request).build();
    ProvisionActionPredicateBuilder builder = new ProvisionActionPredicateBuilder(info);

    assertTrue(builder.getPredicate(ProvisionStep.INSTALL).isPresent());
    assertFalse(builder.getPredicate(ProvisionStep.SKIP_INSTALL).isPresent());
    assertTrue(builder.getPredicate(ProvisionStep.START).isPresent());

    Predicate installPredicate = builder.getPredicate(ProvisionStep.INSTALL).get();
    Predicate startPredicate = builder.getPredicate(ProvisionStep.START).get();

    Set<Resource> allNewHostComponents = allHostComponents(NEW_SERVICES);
    assertMatchesAll(installPredicate, allNewHostComponents);
    assertMatchesAll(startPredicate, allNewHostComponents);

    assertNoMatchForExistingComponents(installPredicate, startPredicate);
  }

  @Test
  public void requestLevelStartOnly() {
    AddServiceRequest request = createRequest(ProvisionAction.START_ONLY, null, null);
    AddServiceInfo info = ADD_SERVICE_INFO_BUILDER.setRequest(request).build();
    ProvisionActionPredicateBuilder builder = new ProvisionActionPredicateBuilder(info);

    assertEquals(Optional.empty(), builder.getPredicate(ProvisionStep.INSTALL));
    assertTrue(builder.getPredicate(ProvisionStep.SKIP_INSTALL).isPresent());
    assertTrue(builder.getPredicate(ProvisionStep.START).isPresent());

    Predicate skipInstallPredicate = builder.getPredicate(ProvisionStep.SKIP_INSTALL).get();
    Predicate startPredicate = builder.getPredicate(ProvisionStep.START).get();

    Set<Resource> allNewHostComponents = allHostComponents(NEW_SERVICES);
    assertMatchesAll(skipInstallPredicate, allNewHostComponents);
    assertMatchesAll(startPredicate, allNewHostComponents);
    assertNoMatchForExistingComponents(skipInstallPredicate, startPredicate);
  }

  @Test
  public void customAtAllLevels() {
    AddServiceRequest request = createRequest(ProvisionAction.START_ONLY,
      ImmutableSet.of(
        Service.of("AMBARI_METRICS", ProvisionAction.INSTALL_AND_START),
        Service.of("KAFKA"),
        Service.of("ZOOKEEPER", ProvisionAction.INSTALL_ONLY)
      ),
      ImmutableSet.of(
        Component.of("KAFKA_BROKER", ProvisionAction.INSTALL_AND_START, "c7404"), // overrides request-level
        // KAFKA_BROKER on c7402 added by layout recommendation inherits request-level
        Component.of("METRICS_GRAFANA", ProvisionAction.START_ONLY, "c7403"), // overrides service-level
        Component.of("METRICS_MONITOR", "c7401"), // inherit from service
        Component.of("METRICS_MONITOR", ProvisionAction.INSTALL_AND_START, "c7402"), // matches service-level
        // METRICS_MONITOR on c7403 added by layout recommendation, inherits service-level
        Component.of("METRICS_MONITOR", ProvisionAction.INSTALL_ONLY, "c7404", "c7405") // overrides service-level
      )
    );
    AddServiceInfo info = ADD_SERVICE_INFO_BUILDER.setRequest(request).build();
    ProvisionActionPredicateBuilder builder = new ProvisionActionPredicateBuilder(info);

    assertTrue(builder.getPredicate(ProvisionStep.INSTALL).isPresent());
    assertTrue(builder.getPredicate(ProvisionStep.SKIP_INSTALL).isPresent());
    assertTrue(builder.getPredicate(ProvisionStep.START).isPresent());

    Predicate installPredicate = builder.getPredicate(ProvisionStep.INSTALL).get();
    Predicate skipInstallPredicate = builder.getPredicate(ProvisionStep.SKIP_INSTALL).get();
    Predicate startPredicate = builder.getPredicate(ProvisionStep.START).get();

    Map<String, Map<String, Set<String>>> installComponents = ImmutableMap.of(
      "AMBARI_METRICS", ImmutableMap.of(
        "METRICS_COLLECTOR", ImmutableSet.of("c7401"),
        "METRICS_MONITOR", ImmutableSet.of("c7401", "c7402", "c7404", "c7405")
      ),
      "KAFKA", ImmutableMap.of(
        "KAFKA_BROKER", ImmutableSet.of("c7404")
      ),
      "ZOOKEEPER", ImmutableMap.of(
        "ZOOKEEPER_SERVER", ImmutableSet.of("c7401", "c7402", "c7403"),
        "ZOOKEEPER_CLIENT", ImmutableSet.of("c7404")
      )
    );
    Map<String, Map<String, Set<String>>> skipInstallComponents = ImmutableMap.of(
      "AMBARI_METRICS", ImmutableMap.of(
        "METRICS_GRAFANA", ImmutableSet.of("c7403")
      ),
      "KAFKA", ImmutableMap.of(
        "KAFKA_BROKER", ImmutableSet.of("c7402")
      )
    );
    Map<String, Map<String, Set<String>>> startComponents = ImmutableMap.of(
      "AMBARI_METRICS", ImmutableMap.of(
        "METRICS_COLLECTOR", ImmutableSet.of("c7401"),
        "METRICS_GRAFANA", ImmutableSet.of("c7403"),
        "METRICS_MONITOR", ImmutableSet.of("c7401", "c7402", "c7403")
      ),
      "KAFKA", ImmutableMap.of(
        "KAFKA_BROKER", ImmutableSet.of("c7402", "c7404")
      )
    );
    assertMatchesAll(installPredicate, allHostComponents(installComponents));
    assertMatchesAll(skipInstallPredicate, allHostComponents(skipInstallComponents));
    assertMatchesAll(startPredicate, allHostComponents(startComponents));
    assertNoMatchForExistingComponents(skipInstallPredicate, startPredicate);
  }

  private static void assertNoMatchForExistingComponents(Predicate... predicates) {
    Resource existingComponent = existingComponent();
    for (Predicate predicate : predicates) {
      assertFalse(predicate.evaluate(existingComponent));
    }
  }

  private static Resource existingComponent() {
    return hostComponent("HDFS", "NAMENODE", "c7401");
  }

  private static void assertMatchesAll(Predicate predicates, Collection<? extends Resource> resources) {
    assertEquals(ImmutableSet.of(), resources.stream().filter(each -> !predicates.evaluate(each)).collect(toSet()));
  }

  private static AddServiceRequest createRequest(ProvisionAction provisionAction, Set<Service> services, Set<Component> components) {
    return new AddServiceRequest(AddServiceRequest.OperationType.ADD_SERVICE, null, provisionAction, AddServiceRequest.ValidationType.DEFAULT, "HDP", "3.0", services, components, null, null, null);
  }

  private static Set<Resource> allHostComponents(Map<String, Map<String, Set<String>>> services) {
    return services.entrySet().stream()
      .flatMap(componentsOfService -> componentsOfService.getValue().entrySet().stream()
        .flatMap(hostsOfComponent -> hostsOfComponent.getValue().stream()
          .map(host -> hostComponent(componentsOfService.getKey(), hostsOfComponent.getKey(), host))))
      .collect(toSet());
  }

  private static Resource hostComponent(String service, String component, String hostname) {
    Resource hostComponent = new ResourceImpl(Resource.Type.HostComponent);
    hostComponent.setProperty(HostComponentResourceProvider.CLUSTER_NAME, CLUSTER_NAME);
    hostComponent.setProperty(HostComponentResourceProvider.SERVICE_NAME, service);
    hostComponent.setProperty(HostComponentResourceProvider.COMPONENT_NAME, component);
    hostComponent.setProperty(HostComponentResourceProvider.HOST_NAME, hostname);
    return hostComponent;
  }

}
