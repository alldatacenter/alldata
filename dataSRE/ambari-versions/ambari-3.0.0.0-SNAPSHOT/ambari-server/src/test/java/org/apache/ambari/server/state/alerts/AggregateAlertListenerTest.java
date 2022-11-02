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
package org.apache.ambari.server.state.alerts;

import javax.persistence.EntityManager;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.AlertStateChangeEvent;
import org.apache.ambari.server.events.MockEventListener;
import org.apache.ambari.server.events.listeners.alerts.AlertAggregateListener;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertSummaryDTO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertFirmness;
import org.apache.ambari.server.state.alert.AggregateDefinitionMapping;
import org.apache.ambari.server.state.alert.AggregateSource;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.Reporting;
import org.apache.ambari.server.state.alert.Reporting.ReportTemplate;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import junit.framework.Assert;

/**
 * Tests the {@link AlertAggregateListener}.
 */
@Category({ category.AlertTest.class})
public class AggregateAlertListenerTest {

  private Injector m_injector;
  private MockEventListener m_listener;
  private AlertsDAO m_alertsDao;
  private AggregateDefinitionMapping m_aggregateMapping;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    m_injector.getInstance(GuiceJpaInitializer.class);
    m_listener = m_injector.getInstance(MockEventListener.class);

    m_alertsDao = m_injector.getInstance(AlertsDAO.class);

    // !!! need a synchronous op for testing
    EventBusSynchronizer.synchronizeAlertEventPublisher(m_injector).register(m_listener);
    EventBusSynchronizer.synchronizeAmbariEventPublisher(m_injector).register(m_listener);
  }

  /**
   * @throws Exception
   */
  @After
  public void teardown() throws Exception {
    H2DatabaseCleaner.clearDatabase(m_injector.getProvider(EntityManager.class).get());
    m_injector = null;
  }

  /**
   * Tests that the {@link AlertAggregateListener} caches values of the
   * aggregates and only triggers events when needed.
   *
   * @throws Exception
   */
  @Test
  public void testAlertNoticeCreationFromEvent() throws Exception {
    AlertDefinition aggregateDefinition = getAggregateAlertDefinition();
    AlertCurrentEntity currentEntityMock = EasyMock.createNiceMock(AlertCurrentEntity.class);
    AlertHistoryEntity historyEntityMock = EasyMock.createNiceMock(AlertHistoryEntity.class);

    EasyMock.expect(currentEntityMock.getAlertHistory()).andReturn(historyEntityMock).atLeastOnce();

    EasyMock.expect(
        m_aggregateMapping.getAggregateDefinition(EasyMock.anyLong(), EasyMock.eq("mock-alert"))).andReturn(
        aggregateDefinition).atLeastOnce();

    AlertSummaryDTO summaryDTO = new AlertSummaryDTO(5,0,0,0,0);
    EasyMock.expect(
        m_alertsDao.findAggregateCounts(EasyMock.anyLong(), EasyMock.eq("mock-aggregate-alert"))).andReturn(
        summaryDTO).atLeastOnce();

    EasyMock.replay(m_alertsDao, m_aggregateMapping, currentEntityMock);

    // check that we're starting at 0
    Assert.assertEquals(0, m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    // trigger an alert which will trigger the aggregate
    Alert alert = new Alert("mock-alert", null, null, null, null, null);
    AlertAggregateListener aggregateListener = m_injector.getInstance(AlertAggregateListener.class);
    AlertStateChangeEvent event = new AlertStateChangeEvent(0, alert, currentEntityMock, null,
        AlertFirmness.HARD);
    aggregateListener.onAlertStateChangeEvent(event);

    // verify that one AlertReceivedEvent was fired (it's the one the listener
    // creates for the aggregate)
    Assert.assertEquals(1, m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    // fire the same alert event again; the cache in the aggregate listener
    // should prevent it from firing a new alert received event of its own
    aggregateListener.onAlertStateChangeEvent(event);

    // check that we're still at 1
    Assert.assertEquals(1, m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    // now change the returned summary DTO so that a new alert will get generated
    summaryDTO.setOkCount(0);
    summaryDTO.setCriticalCount(5);
    aggregateListener.onAlertStateChangeEvent(event);
    Assert.assertEquals(2, m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));
  }

  /**
   * Tests that the {@link AlertAggregateListener} disregards
   * {@link AlertFirmness#SOFT} alerts.
   *
   * @throws Exception
   */
  @Test
  public void testNoAggregateCalculationOnSoftAlert() throws Exception {
    AlertDefinition aggregateDefinition = getAggregateAlertDefinition();
    AlertCurrentEntity currentEntityMock = EasyMock.createNiceMock(AlertCurrentEntity.class);
    AlertHistoryEntity historyEntityMock = EasyMock.createNiceMock(AlertHistoryEntity.class);

    EasyMock.expect(currentEntityMock.getAlertHistory()).andReturn(historyEntityMock).atLeastOnce();
    EasyMock.expect(currentEntityMock.getFirmness()).andReturn(AlertFirmness.SOFT).atLeastOnce();

    EasyMock.expect(m_aggregateMapping.getAggregateDefinition(EasyMock.anyLong(),
        EasyMock.eq("mock-alert"))).andReturn(aggregateDefinition).atLeastOnce();

    AlertSummaryDTO summaryDTO = new AlertSummaryDTO(5, 0, 0, 0, 0);
    EasyMock.expect(m_alertsDao.findAggregateCounts(EasyMock.anyLong(),
        EasyMock.eq("mock-aggregate-alert"))).andReturn(summaryDTO).atLeastOnce();

    EasyMock.replay(m_alertsDao, m_aggregateMapping, currentEntityMock);

    // check that we're starting at 0
    Assert.assertEquals(0, m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    // trigger an alert which would normally trigger the aggregate, except that
    // the alert will be SOFT and should not cause a recalculation
    Alert alert = new Alert("mock-alert", null, null, null, null, null);
    AlertAggregateListener aggregateListener = m_injector.getInstance(AlertAggregateListener.class);
    AlertStateChangeEvent event = new AlertStateChangeEvent(0, alert, currentEntityMock, null,
        AlertFirmness.HARD);
    aggregateListener.onAlertStateChangeEvent(event);

    // ensure that the aggregate listener did not trigger an alert in response
    // to the SOFT alert
    Assert.assertEquals(0, m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));
  }

  /**
   * Gets a mocked {@link AlertDefinition}.
   *
   * @return
   */
  private AlertDefinition getAggregateAlertDefinition() {
    // setup the mocks for the aggregate definition to avoid NPEs
    AlertDefinition aggregateDefinition = new AlertDefinition();
    aggregateDefinition.setName("mock-aggregate-alert");
    AggregateSource aggregateSource = new AggregateSource();
    aggregateSource.setAlertName("mock-aggregate-alert");
    Reporting reporting = new Reporting();
    ReportTemplate criticalTemplate = new ReportTemplate();
    ReportTemplate okTemplate = new ReportTemplate();
    criticalTemplate.setValue(.05);
    criticalTemplate.setText("CRITICAL");
    okTemplate.setText("OK");
    reporting.setCritical(criticalTemplate);
    reporting.setWarning(criticalTemplate);
    reporting.setOk(okTemplate);
    aggregateSource.setReporting(reporting);
    aggregateDefinition.setSource(aggregateSource);

    return aggregateDefinition;
  }

  /**
   *
   */
  private class MockModule implements Module {
    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Binder binder) {
      m_alertsDao = EasyMock.createMock(AlertsDAO.class);
      m_aggregateMapping = EasyMock.createMock(AggregateDefinitionMapping.class);
      binder.bind(AlertsDAO.class).toInstance(m_alertsDao);
      binder.bind(AggregateDefinitionMapping.class).toInstance(m_aggregateMapping);
    }
  }
}
