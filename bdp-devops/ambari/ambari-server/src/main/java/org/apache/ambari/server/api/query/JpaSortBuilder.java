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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.ambari.server.controller.spi.SortRequest;
import org.apache.ambari.server.controller.spi.SortRequestProperty;
import org.apache.commons.lang.ObjectUtils;

/**
 * The {@link JpaSortBuilder} class is used to convert and Ambari
 * {@link SortRequest} and list of {@link SortRequestProperty} into a JPA
 * {@link Order} list. This can then be given to a {@link CriteriaBuilder} to
 * provide sorting in the JPA layer during a query.
 * <p/>
 * This classes uses the {@link JpaPredicateVisitor} for the entity being
 * queried in order to convert the entity fields into {@link Path} expressions.
 */
public class JpaSortBuilder<T> {

  /**
   * Constructor.
   *
   */
  public JpaSortBuilder() {
  }

  /**
   * Builds the list of sort orders based on the supplied request and JPA
   * predicate visitor.
   *
   * @param sortRequest
   *          the Ambari sort request properties to turn into a JPA sort
   *          request. If {@code null} or the {@link SortRequestProperty} list
   *          is null, an empty list is returned.
   * @param visitor
   *          a visitor that knows how to convert the Ambari properties into
   *          {@link SingularAttribute} (not {@code null}).
   * @return a list of sorts or an empty list if none (never {@code null}).
   */
  public List<Order> buildSortOrders(SortRequest sortRequest,
      JpaPredicateVisitor<T> visitor) {

    if (null == sortRequest || null == sortRequest.getProperties()) {
      return Collections.emptyList();
    }

    CriteriaBuilder builder = visitor.getCriteriaBuilder();
    List<SortRequestProperty> sortProperties = sortRequest.getProperties();
    List<Order> sortOrders = new ArrayList<>(sortProperties.size());

    for (SortRequestProperty sort : sortProperties) {
      String propertyId = sort.getPropertyId();

      List<? extends SingularAttribute<?, ?>> singularAttributes = visitor.getPredicateMapping(propertyId);

      if (null == singularAttributes || singularAttributes.size() == 0) {
        continue;
      }

      Path<?> path = null;
      for (SingularAttribute<?, ?> singularAttribute : singularAttributes) {
        if (null == path) {

          CriteriaQuery<T> query = visitor.getCriteriaQuery();
          Set<Root<?>> roots = query.getRoots();

          // if there are existing roots; use the existing roots to prevent more
          // roots from being added potentially causing a cartesian product
          // where we don't want one
          if (null != roots && !roots.isEmpty()) {
            Iterator<Root<?>> iterator = roots.iterator();
            while (iterator.hasNext()) {
              Root<?> root = iterator.next();

              Class<?> visitorEntityClass = visitor.getEntityClass();
              if (ObjectUtils.equals(visitorEntityClass, root.getJavaType())
                  || ObjectUtils.equals(visitorEntityClass, root.getModel().getJavaType())) {
                path = root.get(singularAttribute.getName());
                break;
              }
            }
          }

          // no roots exist already which match this entity class, create a new
          // path
          if (null == path) {
            path = query.from(visitor.getEntityClass()).get(singularAttribute.getName());
          }
        } else {
          path = path.get(singularAttribute.getName());
        }
      }

      Order sortOrder = null;
      if (sort.getOrder() == org.apache.ambari.server.controller.spi.SortRequest.Order.ASC) {
        sortOrder = builder.asc(path);
      } else {
        sortOrder = builder.desc(path);
      }

      sortOrders.add(sortOrder);
    }

    return sortOrders;
  }
}
