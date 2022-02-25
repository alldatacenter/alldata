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

import org.apache.ambari.server.controller.predicate.AlwaysPredicate;
import org.apache.ambari.server.controller.predicate.ArrayPredicate;
import org.apache.ambari.server.controller.predicate.CategoryPredicate;
import org.apache.ambari.server.controller.predicate.ComparisonPredicate;
import org.apache.ambari.server.controller.predicate.NotPredicate;
import org.apache.ambari.server.controller.predicate.PredicateVisitor;
import org.apache.ambari.server.controller.predicate.UnaryPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.utilities.PredicateHelper;

/**
 * A predicate visitor used to extract the sub resource elements from a given predicate
 * and produce a new predicate that can be evaluated with a sub resource of the
 * given category.  For example, given the category 'components' the predicate ...
 *
 *   ServiceInfo/service_name = HBASE AND
 *   components/ServiceComponentInfo/category = SLAVE AND
 *   components/host_components/metrics/cpu/cpu_num >= 1
 *
 * ... will produce the new predicate ...
 *
 *   ServiceComponentInfo/category = SLAVE AND
 *   host_components/metrics/cpu/cpu_num >= 1
 */
public class SubResourcePredicateVisitor implements PredicateVisitor {
  /**
   * The last visited predicate.
   */
  private Predicate lastVisited = null;

  /**
   * The sub resource category (i.e components, host_components, etc).
   */
  private final String category;


  //----- Constructors -------------------------------------------------------

  /**
   * Constructor.
   */
  public SubResourcePredicateVisitor(String category) {
    this.category = category;
  }


  // ----- PredicateVisitor --------------------------------------------------

  @Override
  public void acceptComparisonPredicate(ComparisonPredicate predicate) {

    String propertyId = predicate.getPropertyId();

    int    index    = propertyId.indexOf("/");
    String category = index == -1 ? propertyId : propertyId.substring(0, index);

    if(index > -1 && category.equals(this.category)) {
      // copy and strip off category
      lastVisited = predicate.copy(propertyId.substring(index + 1));
    } else {
      lastVisited = AlwaysPredicate.INSTANCE;
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
    //TODO implement subresource parsing not only for ComparisonPredicate
    if (predicate.getPredicate() instanceof ComparisonPredicate) {
      ComparisonPredicate innerPredicate = (ComparisonPredicate) predicate.getPredicate();
      String propertyId = innerPredicate.getPropertyId();

      int    index    = propertyId.indexOf("/");
      String category = index == -1 ? propertyId : propertyId.substring(0, index);

      if(index > -1 && category.equals(this.category)) {
        // copy and strip off category
        lastVisited = new NotPredicate(innerPredicate.copy(propertyId.substring(index + 1)));
      } else {
        lastVisited = AlwaysPredicate.INSTANCE;
      }
    } else {
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
   * Obtain a predicate that can be evaluated with sub resources
   * belonging to the associated category.
   *
   * @return a sub resource predicate
   */
  public Predicate getSubResourcePredicate() {
    return lastVisited;
  }
}
