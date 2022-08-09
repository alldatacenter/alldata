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

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.CLUSTER_NAME;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.COMPONENT_NAME;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.HOST_NAME;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.SERVICE_NAME;
import static org.apache.ambari.server.controller.predicate.Predicates.and;
import static org.apache.ambari.server.controller.predicate.Predicates.anyOf;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.topology.ProvisionStep;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Builds predicates to be used in the "update host component state" requests, one for each {@link ProvisionStep}.
 *
 * By default all components are both installed and started (except client components, but that exception is handled in
 * HostComponentResourceProvider, not here).  This can be customized at the request-, service-, and component levels in
 * the Add Service request.  Actions are inherited at the lower levels, so the request-level action applies to all
 * services that have no custom action given, and the effective service-level action applies to all components of the
 * given service with similar restriction.  The request and each service can specify exactly one {@code ProvisionAction},
 * but at the component level the same component may have different action for groups of hosts.
 *
 * The predicates need to filter by service name (to avoid affecting existing services), and apply any component-level
 * overrides by further checking for component name and host name.
 *
 * Example:
 * <pre>
 * cluster_name=TEST
 * AND
 * (
 *   (
 *     service_name=AMBARI_METRICS
 *     AND
 *     (
 *       component_name=METRICS_MONITOR
 *       OR
 *       component_name=METRICS_COLLECTOR
 *     )
 *   )
 *   OR
 *   (
 *     service_name=KAFKA
 *     AND
 *     (
 *       component_name=KAFKA_BROKER
 *       AND
 *       host_name=c7402
 *     )
 *   )
 * )
 * </pre>
 */
public class ProvisionActionPredicateBuilder {

  private final Map<ProvisionStep, Predicate> predicates = new EnumMap<>(ProvisionStep.class);
  private final AddServiceInfo request;
  private final Map<String, ProvisionAction> customServiceActions;
  private final Map<String, Map<String, Map<ProvisionAction, Set<String>>>> customComponentActions;
  private final Map<ProvisionStep, List<Predicate>> servicePredicatesByStep;

  public ProvisionActionPredicateBuilder(AddServiceInfo request) {
    this.request = request;

    customServiceActions = findServicesWithCustomAction();
    customComponentActions = findComponentsWithCustomAction();
    servicePredicatesByStep = createServicePredicates();
    createGlobalPredicates();
  }

  public Optional<Predicate> getPredicate(ProvisionStep action) {
    return Optional.ofNullable(predicates.get(action));
  }

  /**
   * Creates a "global" predicate for each {@link ProvisionStep} in the form of:
   * cluster_name=... AND (service1 predicate OR service2 predicate OR ...)
   */
  private void createGlobalPredicates() {
    Preconditions.checkState(servicePredicatesByStep != null);

    Function<Predicate, Predicate> andClusterNameMatches = and(clusterNameIs(request.clusterName()));
    for (Map.Entry<ProvisionStep, List<Predicate>> entry : servicePredicatesByStep.entrySet()) {
      ProvisionStep step = entry.getKey();
      List<Predicate> servicePredicates = entry.getValue();
      anyOf(servicePredicates).map(andClusterNameMatches).ifPresent(predicate -> predicates.put(step, predicate));
    }
  }

  /**
   * Creates predicates for each service for each {@link ProvisionStep} as necessary.
   *
   * @return step -> service predicates map
   */
  private Map<ProvisionStep, List<Predicate>> createServicePredicates() {
    Preconditions.checkState(customServiceActions != null);
    Preconditions.checkState(customComponentActions != null);

    ProvisionAction requestAction = request.getRequest().getProvisionAction();
    Map<ProvisionStep, List<Predicate>> servicePredicatesByStep = new EnumMap<>(ProvisionStep.class);

    for (Map.Entry<String, Map<String, Set<String>>> serviceEntry : request.newServices().entrySet()) {
      String serviceName = serviceEntry.getKey();
      Map<String, Set<String>> hostsByComponent = serviceEntry.getValue();

      ProvisionAction serviceAction = customServiceActions.getOrDefault(serviceName, requestAction);
      Predicate serviceNamePredicate = serviceNameIs(serviceName);

      Map<String, Map<ProvisionAction, Set<String>>> customActionByComponent = customComponentActions.get(serviceName);
      if (customActionByComponent == null) {
        classifyItem(serviceAction, serviceNamePredicate, servicePredicatesByStep);
      } else {
        Map<ProvisionStep, List<Predicate>> componentPredicatesByStep =
          createComponentPredicates(serviceAction, hostsByComponent, customActionByComponent);

        applyComponentOverrides(servicePredicatesByStep, serviceNamePredicate, componentPredicatesByStep);
      }
    }
    return servicePredicatesByStep;
  }

  /**
   * Creates a service-level predicate for each step in the form of:
   * <pre>service_name=... AND (component1 predicate OR component2 predicate OR ...)</pre>
   * The result is appended to the list of predicates corresponding to each step in {@code servicePredicatesByStep}.
   *
   * @param servicePredicatesByStep step -> service predicates
   * @param serviceNamePredicate predicate for service_name=...
   * @param componentPredicatesByStep step -> component predicates
   */
  private static void applyComponentOverrides(
    Map<ProvisionStep, List<Predicate>> servicePredicatesByStep,
    Predicate serviceNamePredicate,
    Map<ProvisionStep, List<Predicate>> componentPredicatesByStep
  ) {
    Function<Predicate, Predicate> andServiceNameMatches = and(serviceNamePredicate);
    for (Map.Entry<ProvisionStep, List<Predicate>> entry : componentPredicatesByStep.entrySet()) {
      ProvisionStep step = entry.getKey();
      List<Predicate> componentPredicates = entry.getValue();
      anyOf(componentPredicates).map(andServiceNameMatches).ifPresent(predicate ->
        servicePredicatesByStep.computeIfAbsent(step, __ -> new LinkedList<>()).add(predicate)
      );
    }
  }

  /**
   * Creates predicates for each component with custom action (one that does not match its parent service's action).
   *
   * @param serviceAction service-level action
   * @param hostsByComponent component -> hosts map (all hosts, including ones for which no custom action was specified)
   * @param customActionByComponent component -> action -> hosts mapping;
   *   only contains components whose action does not match the upper-level (service or request) action
   * @return step -> component predicates map
   */
  private static Map<ProvisionStep, List<Predicate>> createComponentPredicates(
    ProvisionAction serviceAction,
    Map<String, Set<String>> hostsByComponent,
    Map<String, Map<ProvisionAction, Set<String>>> customActionByComponent
  ) {
    Map<ProvisionStep, List<Predicate>> componentPredicatesByStep = new EnumMap<>(ProvisionStep.class);

    for (Map.Entry<String, Set<String>> componentEntry : hostsByComponent.entrySet()) {
      String componentName = componentEntry.getKey();
      Set<String> allHosts = componentEntry.getValue();
      Map<ProvisionAction, Set<String>> hostsByAction = customActionByComponent.getOrDefault(componentName, ImmutableMap.of());

      if (!hostsByAction.isEmpty()) {
        Set<String> customActionHosts = new HashSet<>();
        for (Map.Entry<ProvisionAction, Set<String>> e : hostsByAction.entrySet()) {
          ProvisionAction componentAction = e.getKey();
          Set<String> hosts = e.getValue();

          Predicate componentPredicate = predicateForComponentHosts(componentName, hosts);
          classifyItem(componentAction, componentPredicate, componentPredicatesByStep);

          customActionHosts.addAll(hosts);
        }

        // apply service-level action to any hosts for which no explicit action was specified
        Set<String> leftoverHosts = ImmutableSet.copyOf(Sets.difference(allHosts, customActionHosts));
        if (!leftoverHosts.isEmpty()) {
          Predicate componentPredicate = predicateForComponentHosts(componentName, leftoverHosts);
          classifyItem(serviceAction, componentPredicate, componentPredicatesByStep);
        }
      } else {
        Predicate componentPredicate = componentNameIs(componentName);
        classifyItem(serviceAction, componentPredicate, componentPredicatesByStep);
      }
    }
    return componentPredicatesByStep;
  }

  /**
   * Maps services in the request by component.
   *
   * @return component -> service map
   */
  private Map<String, String> mapServicesByComponent() {
    Map<String, String> serviceByComponent = new HashMap<>();
    for (Map.Entry<String, Map<String, Set<String>>> e : request.newServices().entrySet()) {
      String service = e.getKey();
      for (String component : e.getValue().keySet()) {
        serviceByComponent.put(component, service);
      }
    }
    return serviceByComponent;
  }

  /**
   * Finds all services for which custom provision action was specified in the request.
   *
   * @return service -> action map; only contains services whose action does not match the request-level action
   */
  private Map<String, ProvisionAction> findServicesWithCustomAction() {
    ProvisionAction requestAction = request.getRequest().getProvisionAction();
    return request.getRequest().getServices().stream()
      .filter(service -> service.getProvisionAction().isPresent())
      .filter(service -> !Objects.equals(requestAction, service.getProvisionAction().get()))
      .collect(toMap(Service::getName, service -> service.getProvisionAction().get()));
  }

  /**
   * Finds all host components for which custom provision action was specified in the request.
   *
   * @return service -> component -> action -> hosts mapping; only contains components whose action does not match the upper-level (service or request) action
   */
  private Map<String, Map<String, Map<ProvisionAction, Set<String>>>> findComponentsWithCustomAction() {
    Preconditions.checkState(customServiceActions != null);

    Map<String, String> serviceByComponent = mapServicesByComponent();

    Map<String, Map<String, Map<ProvisionAction, Set<String>>>> result = new HashMap<>();
    for (Component component : request.getRequest().getComponents()) {
      component.getProvisionAction().ifPresent(componentAction -> {
        String componentName = component.getName();
        String serviceName = serviceByComponent.get(componentName);
        ProvisionAction serviceAction = customServiceActions.getOrDefault(serviceName, request.getRequest().getProvisionAction());
        if (!Objects.equals(serviceAction, componentAction)) {
          result
            .computeIfAbsent(serviceName, __ -> new HashMap<>())
            .computeIfAbsent(componentName, __ -> new EnumMap<>(ProvisionAction.class))
            .computeIfAbsent(componentAction, __ -> new HashSet<>())
            .addAll(component.getHosts().stream().map(Host::getFqdn).collect(toSet()));
        }
      });
    }
    return result;
  }

  /**
   * Adds {@code item} to the list(s) it belongs to depending on {@code action}'s steps.
   * For example if {@code action} is {@link ProvisionAction#INSTALL_AND_START}, then the
   * {@code item} is added to both {@link ProvisionStep#INSTALL} and {@link ProvisionStep#START}
   * lists, but not to the list for {@link ProvisionStep#SKIP_INSTALL}.
   *
   * @param action provision action
   * @param item the item to add
   * @param itemsByStep step -> list of items
   */
  private static <T> void classifyItem(ProvisionAction action, T item, Map<ProvisionStep, List<T>> itemsByStep) {
    for (ProvisionStep step : action.getSteps()) {
      itemsByStep.computeIfAbsent(step, __ -> new LinkedList<>()).add(item);
    }
  }

  /**
   * Creates a predicate in the form of:
   * <pre>component_name=... AND (host_name=... OR host_name=... OR ...)</pre>
   *
   * @param componentName component name
   * @param hosts set of host names
   */
  private static Predicate predicateForComponentHosts(String componentName, Set<String> hosts) {
    Preconditions.checkNotNull(hosts);
    Preconditions.checkArgument(!hosts.isEmpty());
    Set<Predicate> hostPredicates = hosts.stream().map(ProvisionActionPredicateBuilder::hostnameIs).collect(toSet());
    return anyOf(hostPredicates).map(and(componentNameIs(componentName))).get();
  }

  private static Predicate clusterNameIs(String clusterName) {
    return new EqualsPredicate<>(CLUSTER_NAME, clusterName);
  }

  private static Predicate serviceNameIs(String serviceName) {
    return new EqualsPredicate<>(SERVICE_NAME, serviceName);
  }

  private static Predicate componentNameIs(String componentName) {
    return new EqualsPredicate<>(COMPONENT_NAME, componentName);
  }

  private static Predicate hostnameIs(String hostname) {
    return new EqualsPredicate<>(HOST_NAME, hostname);
  }
}
