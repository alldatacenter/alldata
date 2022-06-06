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


import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.predicate.AlwaysPredicate;
import org.apache.ambari.server.controller.predicate.ArrayPredicate;
import org.apache.ambari.server.controller.predicate.CategoryPredicate;
import org.apache.ambari.server.controller.predicate.ComparisonPredicate;
import org.apache.ambari.server.controller.predicate.PredicateVisitor;
import org.apache.ambari.server.controller.predicate.UnaryPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateHelper;

/**
 * A predicate visitor used to generate a new predicate so that the resources
 * referenced by the predicate will be extended to include the joined properties
 * of their sub-resources.
 */
public class ExtendedResourcePredicateVisitor implements PredicateVisitor {
  /**
   * The last visited predicate.
   */
  private Predicate lastVisited = null;

  /**
   * The joined resource map.
   */
  private final Map<Resource, Set<Map<String, Object>>> joinedResources;


  // ----- Constructors ------------------------------------------------------

  /**
   * Constructor.
   *
   * @param extendedProperties the map of sets of extended properties
   */
  public ExtendedResourcePredicateVisitor(Map<Resource,
      Set<Map<String, Object>>> extendedProperties) {
    this.joinedResources = extendedProperties;
  }

  // ----- PredicateVisitor --------------------------------------------------

  @Override
  public void acceptComparisonPredicate(ComparisonPredicate predicate) {
    lastVisited = new ExtendedResourcePredicate(predicate, joinedResources);
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
    lastVisited = new ExtendedResourcePredicate(predicate, joinedResources);
  }

  @Override
  public void acceptAlwaysPredicate(AlwaysPredicate predicate) {
    lastVisited = predicate;
  }

  @Override
  public void acceptCategoryPredicate(CategoryPredicate predicate) {
    lastVisited = new ExtendedResourcePredicate(predicate, joinedResources);
  }


  // ----- utility methods -------------------------------------------------

  /**
   * Get the extended predicate.
   *
   * @return the predicate
   */
  public Predicate getExtendedPredicate() {
    return lastVisited;
  }


  // ----- inner classes -----------------------------------------------------

  // ----- ExtendedResourcePredicate -----------------------------------------

  /**
   * Predicate implementation used to replace any existing predicate and
   * extend the resource being evaluated with an extended set of property
   * values.
   */
  private static class ExtendedResourcePredicate implements Predicate {

    /**
     * The predicate being extended.
     */
    private final Predicate predicate;

    /**
     * The map of extended property sets keyed by resource.
     */
    private final Map<Resource, Set<Map<String, Object>>> joinedResources;


    // ----- Constructors ----------------------------------------------------

    /**
     * Constructor
     *
     * @param predicate        the predicate being extended
     * @param joinedResources  the map of extended sets of property values
     */
    public ExtendedResourcePredicate(Predicate predicate,
                                     Map<Resource, Set<Map<String, Object>>> joinedResources) {
      this.predicate       = predicate;
      this.joinedResources = joinedResources;
    }

    // ----- Predicate -------------------------------------------------------

    @Override
    public boolean evaluate(Resource resource) {

      Set<Map<String, Object>> extendedPropertySet = joinedResources.get(resource);

      if (extendedPropertySet == null) {
        return predicate.evaluate(resource);
      }

      for (Map<String, Object> extendedProperties : extendedPropertySet) {
        Resource extendedResource = new ExtendedResourceImpl(resource, extendedProperties);

        if (predicate.evaluate(extendedResource)) {
          return true;
        }
      }
      return false;
    }
  }

  // ----- ExtendedResourceImpl ----------------------------------------------

  /**
   * A resource that extends a given resource by copying it and adding additional
   * properties.
   */
  private static class ExtendedResourceImpl extends ResourceImpl {

    // ----- Constructors ----------------------------------------------------

    /**
     * Constructor
     *
     * @param resource            the resource to copy
     * @param extendedProperties  the map of extended properties
     */
    public ExtendedResourceImpl(Resource resource, Map<String, Object> extendedProperties) {
      super(resource);
      initProperties(extendedProperties);
    }

    // ----- utility methods -------------------------------------------------

    /**
     *  Initialize this resource by setting the extended properties.
     */
    private void initProperties(Map<String, Object> extendedProperties) {
      for (Map.Entry<String, Object> entry : extendedProperties.entrySet()) {
        setProperty(entry.getKey(), entry.getValue());
      }
    }
  }
}
