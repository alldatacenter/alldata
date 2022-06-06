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

package org.apache.ambari.server.orm.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.google.inject.Singleton;

@Singleton
public class DaoUtils {

  public <T> List<T> selectAll(EntityManager entityManager, Class<T> entityClass) {
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<T> query = criteriaBuilder.createQuery(entityClass);
    Root<T> root = query.from(entityClass);
    query.select(root);
    TypedQuery<T> typedQuery = entityManager.createQuery(query);
    try {
      return typedQuery.getResultList();
    } catch (NoResultException ignored) {
      return Collections.emptyList();
    }
  }

  public <T> List<T> selectList(TypedQuery<T> query, Object... parameters) {
    setParameters(query, parameters);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
      return Collections.emptyList();
    }
  }

  public <T> T selectSingle(TypedQuery<T> query, Object... parameters) {
    setParameters(query, parameters);
    try {
      return query.getSingleResult();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  public <T> T selectOne(TypedQuery<T> query, Object... parameters) {
    setParameters(query, parameters);
    try {
      return query.setMaxResults(1).getSingleResult();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  public int executeUpdate(Query query, Object... parameters) {
    setParameters(query, parameters);
    return query.executeUpdate();
  }

  public void setParameters(Query query, Object... parameters) {
    for (int i = 0; i < parameters.length; i++) {
      query.setParameter(i + 1, parameters[i]);
    }
  }
}
