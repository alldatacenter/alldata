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

package org.apache.ambari.server.orm;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.NotificationState;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class AlertDaoHelper {

  private final static String HOSTNAME = "c6401.ambari.apache.org";
  private final static Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

  @Inject
  private AlertDefinitionDAO m_definitionDao;

  @Inject
  private AlertDispatchDAO m_dispatchDAO;

  @Inject
  private AlertsDAO m_alertsDAO;

  /**
   *
   */
  @Transactional
  public void populateData(Cluster cluster) throws Exception {
    // remove any definitions and start over
    List<AlertDefinitionEntity> definitions = m_definitionDao.findAll();
    for (AlertDefinitionEntity definition : definitions) {
      m_definitionDao.remove(definition);
    }

    AlertTargetEntity administrators = new AlertTargetEntity();
    administrators.setDescription("The Administrators");
    administrators.setNotificationType("EMAIL");
    administrators.setTargetName("Administrators");
    m_dispatchDAO.create(administrators);

    AlertTargetEntity operators = new AlertTargetEntity();
    operators.setDescription("The Operators");
    operators.setNotificationType("EMAIL");
    operators.setTargetName("Operators");
    m_dispatchDAO.create(operators);

    // create some definitions
    AlertDefinitionEntity namenode = new AlertDefinitionEntity();
    namenode.setDefinitionName("NAMENODE");
    namenode.setServiceName("HDFS");
    namenode.setComponentName("NAMENODE");
    namenode.setClusterId(cluster.getClusterId());
    namenode.setHash(UUID.randomUUID().toString());
    namenode.setScheduleInterval(Integer.valueOf(60));
    namenode.setScope(Scope.ANY);
    namenode.setSource("{\"type\" : \"SCRIPT\"}");
    namenode.setSourceType(SourceType.SCRIPT);
    m_definitionDao.create(namenode);

    AlertDefinitionEntity datanode = new AlertDefinitionEntity();
    datanode.setDefinitionName("DATANODE");
    datanode.setServiceName("HDFS");
    datanode.setComponentName("DATANODE");
    datanode.setClusterId(cluster.getClusterId());
    datanode.setHash(UUID.randomUUID().toString());
    datanode.setScheduleInterval(Integer.valueOf(60));
    datanode.setScope(Scope.HOST);
    datanode.setSource("{\"type\" : \"SCRIPT\"}");
    datanode.setSourceType(SourceType.SCRIPT);
    m_definitionDao.create(datanode);

    AlertDefinitionEntity aggregate = new AlertDefinitionEntity();
    aggregate.setDefinitionName("YARN_AGGREGATE");
    aggregate.setServiceName("YARN");
    aggregate.setComponentName(null);
    aggregate.setClusterId(cluster.getClusterId());
    aggregate.setHash(UUID.randomUUID().toString());
    aggregate.setScheduleInterval(Integer.valueOf(60));
    aggregate.setScope(Scope.SERVICE);
    aggregate.setSource("{\"type\" : \"SCRIPT\"}");
    aggregate.setSourceType(SourceType.SCRIPT);
    m_definitionDao.create(aggregate);

    // create some history
    AlertHistoryEntity nnHistory = new AlertHistoryEntity();
    nnHistory.setAlertState(AlertState.OK);
    nnHistory.setServiceName(namenode.getServiceName());
    nnHistory.setComponentName(namenode.getComponentName());
    nnHistory.setClusterId(cluster.getClusterId());
    nnHistory.setAlertDefinition(namenode);
    nnHistory.setAlertLabel(namenode.getDefinitionName());
    nnHistory.setAlertText(namenode.getDefinitionName());
    nnHistory.setAlertTimestamp(calendar.getTimeInMillis());
    nnHistory.setHostName(HOSTNAME);
    m_alertsDAO.create(nnHistory);

    AlertHistoryEntity dnHistory = new AlertHistoryEntity();
    dnHistory.setAlertState(AlertState.WARNING);
    dnHistory.setServiceName(datanode.getServiceName());
    dnHistory.setComponentName(datanode.getComponentName());
    dnHistory.setClusterId(cluster.getClusterId());
    dnHistory.setAlertDefinition(datanode);
    dnHistory.setAlertLabel(datanode.getDefinitionName());
    dnHistory.setAlertText(datanode.getDefinitionName());
    dnHistory.setAlertTimestamp(calendar.getTimeInMillis());
    dnHistory.setHostName(HOSTNAME);
    m_alertsDAO.create(dnHistory);

    AlertHistoryEntity aggregateHistory = new AlertHistoryEntity();
    aggregateHistory.setAlertState(AlertState.CRITICAL);
    aggregateHistory.setServiceName(aggregate.getServiceName());
    aggregateHistory.setComponentName(aggregate.getComponentName());
    aggregateHistory.setClusterId(cluster.getClusterId());
    aggregateHistory.setAlertDefinition(aggregate);
    aggregateHistory.setAlertLabel(aggregate.getDefinitionName());
    aggregateHistory.setAlertText(aggregate.getDefinitionName());
    aggregateHistory.setAlertTimestamp(calendar.getTimeInMillis());
    m_alertsDAO.create(aggregateHistory);

    AlertNoticeEntity nnPendingNotice = new AlertNoticeEntity();
    nnPendingNotice.setAlertHistory(nnHistory);
    nnPendingNotice.setAlertTarget(administrators);
    nnPendingNotice.setNotifyState(NotificationState.PENDING);
    nnPendingNotice.setUuid(UUID.randomUUID().toString());
    m_dispatchDAO.create(nnPendingNotice);

    AlertNoticeEntity dnDeliveredNotice = new AlertNoticeEntity();
    dnDeliveredNotice.setAlertHistory(dnHistory);
    dnDeliveredNotice.setAlertTarget(administrators);
    dnDeliveredNotice.setNotifyState(NotificationState.FAILED);
    dnDeliveredNotice.setUuid(UUID.randomUUID().toString());
    m_dispatchDAO.create(dnDeliveredNotice);

    AlertNoticeEntity aggregateFailedNotice = new AlertNoticeEntity();
    aggregateFailedNotice.setAlertHistory(aggregateHistory);
    aggregateFailedNotice.setAlertTarget(operators);
    aggregateFailedNotice.setNotifyState(NotificationState.FAILED);
    aggregateFailedNotice.setUuid(UUID.randomUUID().toString());
    m_dispatchDAO.create(aggregateFailedNotice);

    List<AlertHistoryEntity> histories = m_alertsDAO.findAll();
    assertEquals(3, histories.size());
  }
}
