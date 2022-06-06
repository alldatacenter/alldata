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
package org.apache.ambari.server.api.query;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.predicate.AlwaysPredicate;
import org.apache.ambari.server.controller.predicate.ArrayPredicate;
import org.apache.ambari.server.controller.predicate.CategoryPredicate;
import org.apache.ambari.server.controller.predicate.ComparisonPredicate;
import org.apache.ambari.server.controller.predicate.PredicateVisitor;
import org.apache.ambari.server.controller.predicate.UnaryPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.utilities.PredicateHelper;

/**
 * A predicate visitor used to process a predicate to do the following :
 *
 * 1) Create a new predicate with all sub-resource elements contained in the
 *    visited predicate removed (converted to AlwaysPredicate).
 * 2) Create a set of any sub-resource subResourceCategories that were referenced in
 *    the predicate being visited (i.e. components, host_components, etc).
 * 3) Create a set of any sub-resource subResourceProperties that were referenced in
 *    the predicate being visited.
 *
 * For example, the service level query the predicate ...
 *
 *   ServiceInfo/service_name = HBASE AND
 *   components/ServiceComponentInfo/category = SLAVE AND
 *   components/host_components/metrics/cpu/cpu_num >= 1
 *
 * ... will produce ...
 *
 *   Predicate : ServiceInfo/service_name = HBASE
 *
 *   Sub-resource subResourceCategories : {components}
 *
 *   Sub-resource subResourceProperties : {components/ServiceComponentInfo/category,
 *                                         components/host_components/metrics/cpu/cpu_num}
 */
public class ProcessingPredicateVisitor implements PredicateVisitor {
  /**
   * Associated resource provider.
   */
  private final QueryImpl query;

  /**
   * The last visited predicate.
   */
  private Predicate lastVisited = null;

  /**
   * The set of sub-resource categories.
   */
  private final Set<String> subResourceCategories = new HashSet<>();

  /**
   * The set of sub-resource properties.
   */
  private final Set<String> subResourceProperties = new HashSet<>();


  // ----- Constructors ----------------------------------------------------

  /**
   * Constructor.
   *
   * @param query  associated query
   */
  public ProcessingPredicateVisitor(QueryImpl query) {
    this.query    = query;
  }


  // ----- PredicateVisitor --------------------------------------------------

  @Override
  public void acceptComparisonPredicate(ComparisonPredicate predicate) {

    String propertyId = predicate.getPropertyId();
    int    index      = propertyId.indexOf("/");
    String category   = index == -1 ? propertyId : propertyId.substring(0, index);

    Map<String, QueryImpl> subResources = query.ensureSubResources();

    if (subResources.containsKey(category)) {
      subResourceCategories.add(category);
      subResourceProperties.add(propertyId);
      lastVisited = AlwaysPredicate.INSTANCE;
    }
    else {
      lastVisited = predicate;
    }
  }

  @Override
  public void acceptArrayPredicate(ArrayPredicate arrayPredicate) {
    List<Predicate> predicateList = new LinkedList<>();

    Predicate[] predicates = arrayPredicate.getPredicates();
    if (predicates.length > 0) {
      for (Predicate predicate : predicates) {
        PredicateHelper.visit(predicate, this);
        predicateList.add(lastVisited);
      }
    }
    lastVisited = arrayPredicate.create(predicateList.toArray(new Predicate[predicateList.size()]));
  }

  @Override
  public void acceptUnaryPredicate(UnaryPredicate predicate) {
    String propertyId = predicate.getPropertyIds().iterator().next();
    int    index      = propertyId.indexOf("/");
    String category   = index == -1 ? propertyId : propertyId.substring(0, index);

    Map<String, QueryImpl> subResources = query.ensureSubResources();

    if (subResources.containsKey(category)) {
      subResourceCategories.add(category);
      subResourceProperties.add(propertyId);
      lastVisited = AlwaysPredicate.INSTANCE;
    }
    else {
      lastVisited = predicate;
    }
  }

  @Override
  public void acceptAlwaysPredicate(AlwaysPredicate predicate) {
    lastVisited = predicate;
  }

  @Override
  public void acceptCategoryPredicate(CategoryPredicate predicate) {
    lastVisited = predicate;
  }


  // ----- utility methods -------------------------------------------------

  /**
   * Get a new predicate with all sub-resource elements contained in the
   * visited predicate removed.
   *
   * @return the new predicate
   */
  public Predicate getProcessedPredicate() {
    return lastVisited;
  }

  /**
   * Get a set of any sub-resource subResourceCategories that were referenced
   * in the predicate that was visited.
   *
   * @return the set of sub-resource categories
   */
  public Set<String> getSubResourceCategories() {
    return subResourceCategories;
  }

  /**
   * Get a set of any sub-resource subResourceProperties that were referenced
   * in the predicate that was visited.
   *
   * @return the set of sub-resource properties
   */
  public Set<String> getSubResourceProperties() {
    return subResourceProperties;
  }
}
