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

package org.apache.ambari.server.view.persistence;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.orm.PersistenceType;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.dynamic.DynamicClassLoader;
import org.eclipse.persistence.jpa.dynamic.JPADynamicHelper;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.tools.schemaframework.SchemaManager;

import com.google.common.base.Optional;
import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * Module used for data store creation and injection.
 */
public class DataStoreModule implements Module, SchemaManagerFactory {

  /**
   * The view instance.
   */
  private final ViewInstanceEntity viewInstanceEntity;

  /**
   * The class loader.
   */
  private final DynamicClassLoader classLoader;

  /**
   * The entity manager factory.
   */
  private final EntityManagerFactory entityManagerFactory;

  /**
   * The dynamic JPA helper.
   */
  private final JPADynamicHelper jpaDynamicHelper;

  /**
   * View persistence unit name.
   */
  private static final String VIEWS_PERSISTENCE_UNIT_NAME = "ambari-views";
  private Optional<String> puName = Optional.absent();


  // ----- Constructors ------------------------------------------------------

  public DataStoreModule(ViewInstanceEntity viewInstanceEntity) {
    ViewEntity view = viewInstanceEntity.getViewEntity();

    this.viewInstanceEntity   = viewInstanceEntity;
    this.classLoader          = new DynamicClassLoader(view.getClassLoader());
    this.entityManagerFactory = getEntityManagerFactory(view.getAmbariConfiguration());
    this.jpaDynamicHelper     = new JPADynamicHelper(entityManagerFactory.createEntityManager());
  }

  public DataStoreModule(ViewInstanceEntity viewInstanceEntity,String puName) {
    this.puName = Optional.of(puName);
    ViewEntity view = viewInstanceEntity.getViewEntity();

    this.viewInstanceEntity   = viewInstanceEntity;
    this.classLoader          = new DynamicClassLoader(view.getClassLoader());
    this.entityManagerFactory = getEntityManagerFactory(view.getAmbariConfiguration());
    this.jpaDynamicHelper     = new JPADynamicHelper(entityManagerFactory.createEntityManager());
  }


  // ----- Module ------------------------------------------------------------

  @Override
  public void configure(Binder binder) {
    binder.bind(ViewInstanceEntity.class).toInstance(viewInstanceEntity);
    binder.bind(DynamicClassLoader.class).toInstance(classLoader);
    binder.bind(EntityManagerFactory.class).toInstance(entityManagerFactory);
    binder.bind(JPADynamicHelper.class).toInstance(jpaDynamicHelper);
    binder.bind(SchemaManagerFactory.class).toInstance(this);
  }


  public void close() {
    entityManagerFactory.close();
  }

  // ----- SchemaManagerFactory ----------------------------------------------


  @Override
  public SchemaManager getSchemaManager(DatabaseSession session) {
    return new SchemaManager(session);
  }

  // ----- helper methods ----------------------------------------------------

  // get an entity manager factory for the given class loader and configuration
  private EntityManagerFactory getEntityManagerFactory(Configuration configuration) {
    Map<Object, Object> persistenceMap  = ControllerModule.getPersistenceProperties(configuration);

    if (!configuration.getPersistenceType().equals(PersistenceType.IN_MEMORY)) {
      persistenceMap.put(JDBC_USER, configuration.getDatabaseUser());
      persistenceMap.put(JDBC_PASSWORD, configuration.getDatabasePassword());
      persistenceMap.put(PersistenceUnitProperties.CLASSLOADER, classLoader);
      persistenceMap.put(PersistenceUnitProperties.WEAVING, "static");
    }

    return Persistence.createEntityManagerFactory(puName.isPresent()?puName.get():VIEWS_PERSISTENCE_UNIT_NAME, persistenceMap);
  }
}
