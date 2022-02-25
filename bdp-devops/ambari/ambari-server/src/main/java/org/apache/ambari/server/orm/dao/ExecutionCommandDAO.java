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

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.ExecutionCommandEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class ExecutionCommandDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public ExecutionCommandEntity findByPK(long taskId) {
    return entityManagerProvider.get().find(ExecutionCommandEntity.class, taskId);
  }

  @RequiresSession
  public List<ExecutionCommandEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), ExecutionCommandEntity.class);
  }

  @Transactional
  public void create(ExecutionCommandEntity executionCommand) {
    entityManagerProvider.get().persist(executionCommand);
  }

  @Transactional
  public ExecutionCommandEntity merge(ExecutionCommandEntity executionCommand) {
    return entityManagerProvider.get().merge(executionCommand);
  }

  @Transactional
  public void remove(ExecutionCommandEntity executionCommand) {
    entityManagerProvider.get().remove(merge(executionCommand));
  }

  @Transactional
  public void removeByPK(long taskId) {
    remove(findByPK(taskId));
  }
}
