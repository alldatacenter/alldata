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
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.controller.internal.AlertHistoryResourceProvider;
import org.apache.ambari.server.controller.internal.SortRequestImpl;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.SortRequest;
import org.apache.ambari.server.controller.spi.SortRequestProperty;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity_;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

/**
 * Tests the {@link JpaSortBuilder}.
 */
public class JpaSortBuilderTest {

  private Injector m_injector;

  @Before
  public void before() {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.injectMembers(this);
  }

  @After
  public void teardown() throws Exception {
    H2DatabaseCleaner.clearDatabase(m_injector.getProvider(EntityManager.class).get());
  }

  /**
   * Tests that adding a sort does not create another {@link Root} in the
   * {@link CriteriaQuery}. A duplicate root will cause a cartesian product
   * similar to:
   *
   * <pre>
   * SELECT t0.alert_id,
   *   t0.alert_instance,
   *   t0.alert_label,
   *   t0.alert_state,
   *   t0.alert_text,
   *   t0.alert_timestamp,
   *   t0.cluster_id,
   *   t0.component_name,
   *   t0.host_name,
   *   t0.service_name,
   *   t0.alert_definition_id
   * FROM   alert_history t0,
   *   alert_history t2,
   *   alert_definition t1
   * WHERE  ( ( t1.definition_name = ? )
   *     AND ( t1.definition_id = t2.alert_definition_id ) )
   * ORDER  BY t0.alert_timestamp DESC
   * </pre>
   *
   * where the root for {@code alert_history} is added twice.
   *
   * @throws Exception
   */
  @Test
  public void testSortDoesNotAddExtraRootPaths() throws Exception {
    // create a sort request against the entity directly
    List<SortRequestProperty> sortRequestProperties = new ArrayList<>();

    sortRequestProperties.add(
        new SortRequestProperty(AlertHistoryResourceProvider.ALERT_HISTORY_TIMESTAMP,
            org.apache.ambari.server.controller.spi.SortRequest.Order.ASC));

    SortRequest sortRequest = new SortRequestImpl(sortRequestProperties);

    // create a complex, cross-entity predicate
    Predicate predicate = new PredicateBuilder().property(
            AlertHistoryResourceProvider.ALERT_HISTORY_DEFINITION_NAME).equals("foo").toPredicate();

    MockAlertHistoryredicateVisitor visitor = new MockAlertHistoryredicateVisitor();
    PredicateHelper.visit(predicate, visitor);

    JpaSortBuilder<AlertHistoryEntity> sortBuilder = new JpaSortBuilder<>();
    List<Order> sortOrders = sortBuilder.buildSortOrders(sortRequest, visitor);

    Assert.assertEquals(sortOrders.size(), 1);

    // verify the CriteriaQuery has the correct roots
    // it should have one for the main query predicate
    CriteriaQuery<AlertHistoryEntity> query = visitor.getCriteriaQuery();
    Set<Root<?>> roots = query.getRoots();
    Assert.assertEquals(1, roots.size());
  }

  private final class MockAlertHistoryredicateVisitor
      extends JpaPredicateVisitor<AlertHistoryEntity> {

    /**
     * Constructor.
     *
     */
    public MockAlertHistoryredicateVisitor() {
      super(m_injector.getInstance(EntityManager.class), AlertHistoryEntity.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<AlertHistoryEntity> getEntityClass() {
      return AlertHistoryEntity.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends SingularAttribute<?, ?>> getPredicateMapping(String propertyId) {
      return AlertHistoryEntity_.getPredicateMapping().get(propertyId);
    }
  }
}
