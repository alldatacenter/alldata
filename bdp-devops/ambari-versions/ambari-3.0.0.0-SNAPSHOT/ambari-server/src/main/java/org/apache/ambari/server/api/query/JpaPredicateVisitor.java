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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.ambari.server.controller.predicate.AlwaysPredicate;
import org.apache.ambari.server.controller.predicate.ArrayPredicate;
import org.apache.ambari.server.controller.predicate.CategoryPredicate;
import org.apache.ambari.server.controller.predicate.ComparisonPredicate;
import org.apache.ambari.server.controller.predicate.PredicateVisitor;
import org.apache.ambari.server.controller.predicate.UnaryPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.utilities.PredicateHelper;

/**
 * The {@link JpaPredicateVisitor} is used to convert an Ambari
 * {@link Predicate} into a JPA {@link javax.persistence.criteria.Predicate}.
 */
public abstract class JpaPredicateVisitor<T> implements PredicateVisitor {
  /**
   * JPA entity manager
   */
  private EntityManager m_entityManager;

  /**
   * Builds the {@link CriteriaQuery} from the {@link Predicate}.
   */
  private CriteriaBuilder m_builder;

  /**
   * The root that the {@code from} clause requests from.
   */
  final private Root<T> m_root;

  /**
   * The query to submit to JPA.
   */
  final private CriteriaQuery<T> m_query;

  /**
   * The last calculated predicate.
   */
  private javax.persistence.criteria.Predicate m_lastPredicate = null;

  /**
   * A queue of lists of {@link javax.persistence.criteria.Predicate}. Every
   * time an {@code OR} or {@code AND} is encountered, a new chain (list) is
   * created and the prior list is enqeued. When the logical statement is
   * closed, the chain is completed and added to the prior chain's list.
   */
  private ArrayDeque<List<javax.persistence.criteria.Predicate>> m_queue =
    new ArrayDeque<>();

  /**
   * Constructor.
   *
   * @param entityManager
   *          the EM used to get a {@link CriteriaBuilder}.
   * @param entityClass
   *          the entity class being queried from.
   */
  public JpaPredicateVisitor(EntityManager entityManager, Class<T> entityClass) {
    m_entityManager = entityManager;
    m_builder = m_entityManager.getCriteriaBuilder();
    m_query = m_builder.createQuery(entityClass);
    m_root = m_query.from(entityClass);
  }

  /**
   * Gets the entity class that is the root type in the JPA {@code from} clause.
   *
   * @return the entity class (not {@code null}).
   */
  public abstract Class<T> getEntityClass();

  /**
   * Gets the {@link SingularAttribute}s mapped to the specified Ambari-style
   * property.
   *
   * @param propertyId
   *          the Ambari-style property (not {@code null}).
   * @return the {@link SingularAttribute}, or {@code null} if no mapping
   *         exists.
   */
  public abstract List<? extends SingularAttribute<?, ?>> getPredicateMapping(
      String propertyId);

  /**
   * Gets the final JPA {@link javax.persistence.criteria.Predicate} after the
   * visitor is done traversing the Ambari {@link Predicate}.
   *
   * @return the predicate, or {@code null} if none.
   */
  public javax.persistence.criteria.Predicate getJpaPredicate() {
    return m_lastPredicate;
  }

  /**
   * Gets the query to use along with {@link #getJpaPredicate()}.
   *
   * @return the query (not {@code null}).
   */
  public CriteriaQuery<T> getCriteriaQuery() {
    return m_query;
  }

  /**
   * Gets the criteria builder used to construct the query and predicates.
   *
   * @return the builder (never {@code null}).
   */
  public CriteriaBuilder getCriteriaBuilder() {
    return m_builder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void acceptComparisonPredicate(ComparisonPredicate predicate) {
    String propertyId = predicate.getPropertyId();

    List<? extends SingularAttribute<?, ?>> singularAttributes = getPredicateMapping(propertyId);

    if (null == singularAttributes || singularAttributes.size() == 0) {
      return;
    }

    SingularAttribute<?, ?> lastSingularAttribute = null;
    Path<Comparable> path = null;

    for (SingularAttribute<?, ?> singularAttribute : singularAttributes) {
      lastSingularAttribute = singularAttribute;

      if (singularAttribute != null) {
        if (null == path) {
          path = m_root.get(singularAttribute.getName());
        } else {
          path = path.get(singularAttribute.getName());
        }
      }
    }

    if (null == path) {
      return;
    }

    String operator = predicate.getOperator();
    Comparable value = predicate.getValue();

    // convert string to enum for proper JPA comparisons
    if (lastSingularAttribute != null) {
      Class<?> clazz = lastSingularAttribute.getJavaType();
      if (clazz.isEnum()) {
        Class<? extends Enum> enumClass = (Class<? extends Enum>) clazz;
        value = Enum.valueOf(enumClass, value.toString());
      }
    }

    javax.persistence.criteria.Predicate jpaPredicate = null;
    if ("=".equals(operator)) {
      jpaPredicate = m_builder.equal(path, value);
    } else if ("<".equals(operator)) {
      jpaPredicate = m_builder.lessThan(path, value);
    } else if ("<=".equals(operator)) {
      jpaPredicate = m_builder.lessThanOrEqualTo(path, value);
    } else if (">".equals(operator)) {
      jpaPredicate = m_builder.greaterThan(path, value);
    } else if (">=".equals(operator)) {
      jpaPredicate = m_builder.greaterThanOrEqualTo(path, value);
    }

    if (null == jpaPredicate) {
      return;
    }

    if (null == m_queue.peekLast()) {
      m_lastPredicate = jpaPredicate;
    } else {
      m_queue.peekLast().add(jpaPredicate);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acceptArrayPredicate(ArrayPredicate predicate) {
    // no predicates, no work
    Predicate[] predicates = predicate.getPredicates();
    if (predicates.length == 0) {
      return;
    }

    // create a new list for all of the predicates in this chain
    List<javax.persistence.criteria.Predicate> predicateList = new ArrayList<>();
    m_queue.add(predicateList);

    // visit every child predicate so it can be added to the list
    String operator = predicate.getOperator();
    for (int i = 0; i < predicates.length; i++) {
      PredicateHelper.visit(predicates[i], this);
    }
    javax.persistence.criteria.Predicate jpaPredicate = null;
    // the list is done; deque and apply logical AND or OR
    predicateList = m_queue.pollLast();
    if (predicateList != null) {
      javax.persistence.criteria.Predicate[] array = new javax.persistence.criteria.Predicate[predicateList.size()];
      array = predicateList.toArray(array);

      if ("AND".equals(operator)) {
        jpaPredicate = m_builder.and(array);
      } else {
        jpaPredicate = m_builder.or(array);
      }

      if (null == m_queue.peekLast()) {
        m_lastPredicate = jpaPredicate;
      } else {
        m_queue.peekLast().add(jpaPredicate);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acceptUnaryPredicate(UnaryPredicate predicate) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acceptAlwaysPredicate(AlwaysPredicate predicate) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acceptCategoryPredicate(CategoryPredicate predicate) {
  }
}
