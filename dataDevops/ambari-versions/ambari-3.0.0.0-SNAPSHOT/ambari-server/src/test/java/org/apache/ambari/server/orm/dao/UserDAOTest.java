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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authorization.UserName;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * UserDAO unit tests.
 */
public class UserDAOTest {

  private static String SERVICEOP_USER_NAME = "serviceopuser";
  private UserDAO userDAO;

  public void init(UserEntity userInDB) {
    final EntityManager entityManager = createStrictMock(EntityManager.class);
    final DaoUtils daoUtils = createNiceMock(DaoUtils.class);
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityManagerProvider.class);
        bind(EntityManager.class).toInstance(entityManager);
        bind(DBAccessor.class).toInstance(dbAccessor);
        bind(DaoUtils.class).toInstance(daoUtils);
      }
    });
    userDAO = mockInjector.getInstance(UserDAO.class);

    TypedQuery<UserEntity> userQuery = createNiceMock(TypedQuery.class);
    expect(userQuery.getSingleResult()).andReturn(userInDB);
    expect(entityManager.createNamedQuery(anyString(), anyObject(Class.class))).andReturn(userQuery);
    replay(entityManager, daoUtils, dbAccessor, userQuery);
  }

  @Test
  public void testUserByName() {
    init(user());
    assertEquals(SERVICEOP_USER_NAME, userDAO.findUserByName(SERVICEOP_USER_NAME).getUserName());
  }

  private static final UserEntity user() {
    return user(SERVICEOP_USER_NAME);
  }

  private static final UserEntity user(String name) {
    UserEntity userEntity = new UserEntity();
    userEntity.setUserName(UserName.fromString(name).toString());
    return userEntity;
  }

  static class EntityManagerProvider implements Provider<EntityManager> {
    private final EntityManager entityManager;

    @Inject
    public EntityManagerProvider(EntityManager entityManager) {
      this.entityManager = entityManager;
    }

    @Override
    public EntityManager get() {
      return entityManager;
    }
  }

}
