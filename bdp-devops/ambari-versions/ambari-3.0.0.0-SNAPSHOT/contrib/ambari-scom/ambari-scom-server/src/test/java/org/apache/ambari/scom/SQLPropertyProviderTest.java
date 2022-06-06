/**
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

package org.apache.ambari.scom;

import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.jdbc.ConnectionFactory;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * SQLPropertyProvider Tests
 */
public class SQLPropertyProviderTest {

  private static final String PROPERTY_ID_1 = "metrics/rpc/RpcQueueTime_avg_time";
  private static final String PROPERTY_ID_2 = "metrics/rpc/RpcSlowResponse_num_ops";
  private static final String CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final String HOST_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "host_name");
  private static final String COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");
  private static final String SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "service_name");

  @Test
  public void testPopulateResources() throws Exception {

    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
    Connection connection = createNiceMock(Connection.class);
    Statement statement = createNiceMock(Statement.class);
    ResultSet resultSet = createNiceMock(ResultSet.class);

    // set expectations
    expect(connectionFactory.getConnection()).andReturn(connection).once();
    expect(connection.createStatement()).andReturn(statement).once();
    expect(statement.executeQuery(anyObject(String.class))).andReturn(resultSet).once();
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999990L);
    expect(resultSet.getNString("MetricValue")).andReturn("0");
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999991L);
    expect(resultSet.getNString("MetricValue")).andReturn("1");
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999992L);
    expect(resultSet.getNString("MetricValue")).andReturn("2");
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999993L);
    expect(resultSet.getNString("MetricValue")).andReturn("3");
    expect(resultSet.next()).andReturn(false);

    // replay
    replay(connectionFactory, connection, statement, resultSet);

    SQLPropertyProvider provider = new SQLPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent),
        new TestHostInfoProvider(),
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID,
        SERVICE_NAME_PROPERTY_ID,
        connectionFactory);

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");
    resource.setProperty(SERVICE_NAME_PROPERTY_ID, "HDFS");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(PROPERTY_ID_1, new TemporalInfoImpl(10L, 20L, 1L));
    Request request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID_1), temporalInfoMap);

    Assert.assertEquals(1, provider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertTrue(resource.getPropertyValue(PROPERTY_ID_1) instanceof Number[][]);

    Number[][] datapoints = (Number[][]) resource.getPropertyValue(PROPERTY_ID_1);

    for (int i = 0; i < datapoints.length; ++i) {
      Assert.assertEquals((long) i, datapoints[i][0]);
      Assert.assertEquals((999990L + i)/1000, datapoints[i][1]);
    }

    // verify
    verify(connectionFactory, connection, statement, resultSet);
  }

  @Test
  public void testPopulateResources_temporalStartTimeOnly() throws Exception {

    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
    Connection connection = createNiceMock(Connection.class);
    Statement statement = createNiceMock(Statement.class);
    ResultSet resultSet = createNiceMock(ResultSet.class);

    // set expectations
    expect(connectionFactory.getConnection()).andReturn(connection).once();
    expect(connection.createStatement()).andReturn(statement).once();
    expect(statement.executeQuery(anyObject(String.class))).andReturn(resultSet).once();
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999990L);
    expect(resultSet.getNString("MetricValue")).andReturn("0");
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999991L);
    expect(resultSet.getNString("MetricValue")).andReturn("1");
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999992L);
    expect(resultSet.getNString("MetricValue")).andReturn("2");
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999993L);
    expect(resultSet.getNString("MetricValue")).andReturn("3");
    expect(resultSet.next()).andReturn(false);

    // replay
    replay(connectionFactory, connection, statement, resultSet);

    TestHostInfoProvider hostProvider = new TestHostInfoProvider();

    SQLPropertyProvider provider = new SQLPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent),
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID,
        SERVICE_NAME_PROPERTY_ID,
        connectionFactory);

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");
    resource.setProperty(SERVICE_NAME_PROPERTY_ID, "HDFS");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(PROPERTY_ID_1, new TemporalInfoImpl(10L, -1L, -1L));
    Request request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID_1), temporalInfoMap);

    Assert.assertEquals(1, provider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertTrue(resource.getPropertyValue(PROPERTY_ID_1) instanceof Number[][]);

    Number[][] datapoints = (Number[][]) resource.getPropertyValue(PROPERTY_ID_1);

    for (int i = 0; i < datapoints.length; ++i) {
      Assert.assertEquals((long) i, datapoints[i][0]);
      Assert.assertEquals((999990L + i) / 1000, datapoints[i][1]);
    }

    // verify
    verify(connectionFactory, connection, statement, resultSet);
  }

  @Test
  public void testPopulateResources_hostNameProperty() throws Exception {

    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
    Connection connection = createNiceMock(Connection.class);
    Statement statement = createNiceMock(Statement.class);
    ResultSet resultSet = createNiceMock(ResultSet.class);

    // set expectations
    expect(connectionFactory.getConnection()).andReturn(connection).once();
    expect(connection.createStatement()).andReturn(statement).once();
    expect(statement.executeQuery(anyObject(String.class))).andReturn(resultSet).once();
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999990L);
    expect(resultSet.getNString("MetricValue")).andReturn("0");
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999991L);
    expect(resultSet.getNString("MetricValue")).andReturn("1");
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999992L);
    expect(resultSet.getNString("MetricValue")).andReturn("2");
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999993L);
    expect(resultSet.getNString("MetricValue")).andReturn("3");
    expect(resultSet.next()).andReturn(false);

    // replay
    replay(connectionFactory, connection, statement, resultSet);

    TestHostInfoProvider hostProvider = new TestHostInfoProvider();

    SQLPropertyProvider provider = new SQLPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent),
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID,
        SERVICE_NAME_PROPERTY_ID,
        connectionFactory);

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");
    resource.setProperty(SERVICE_NAME_PROPERTY_ID, "HDFS");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(PROPERTY_ID_1, new TemporalInfoImpl(10L, -1L, -1L));
    Request request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID_1), temporalInfoMap);

    provider.populateResources(Collections.singleton(resource), request, null);

    Assert.assertEquals("domU-12-31-39-0E-34-E1.compute-1.internal", hostProvider.getHostId());
    Assert.assertNull(hostProvider.getClusterName());
    Assert.assertNull(hostProvider.getComponentName());

    // verify
    verify(connectionFactory, connection, statement, resultSet);
  }

  @Test
  public void testPopulateResources_noHostNameProperty() throws Exception {

    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
    Connection connection = createNiceMock(Connection.class);
    Statement statement = createNiceMock(Statement.class);
    ResultSet resultSet = createNiceMock(ResultSet.class);

    // set expectations
    expect(connectionFactory.getConnection()).andReturn(connection).once();
    expect(connection.createStatement()).andReturn(statement).once();
    expect(statement.executeQuery(anyObject(String.class))).andReturn(resultSet).once();
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999990L);
    expect(resultSet.getNString("MetricValue")).andReturn("0");
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999991L);
    expect(resultSet.getNString("MetricValue")).andReturn("1");
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999992L);
    expect(resultSet.getNString("MetricValue")).andReturn("2");
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999993L);
    expect(resultSet.getNString("MetricValue")).andReturn("3");
    expect(resultSet.next()).andReturn(false);

    // replay
    replay(connectionFactory, connection, statement, resultSet);

    TestHostInfoProvider hostProvider = new TestHostInfoProvider();

    SQLPropertyProvider provider = new SQLPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent),
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        null,
        COMPONENT_NAME_PROPERTY_ID,
        SERVICE_NAME_PROPERTY_ID,
        connectionFactory);

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");
    resource.setProperty(SERVICE_NAME_PROPERTY_ID, "HDFS");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(PROPERTY_ID_1, new TemporalInfoImpl(10L, -1L, -1L));
    Request request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID_1), temporalInfoMap);

    provider.populateResources(Collections.singleton(resource), request, null);

    Assert.assertNull(hostProvider.getHostId());
    Assert.assertEquals("c1", hostProvider.getClusterName());
    Assert.assertEquals("DATANODE", hostProvider.getComponentName());

    // verify
    verify(connectionFactory, connection, statement, resultSet);
  }

  @Test
  public void testPopulateResources_pointInTime() throws Exception {

    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
    Connection connection = createNiceMock(Connection.class);
    Statement statement = createNiceMock(Statement.class);
    ResultSet resultSet = createNiceMock(ResultSet.class);

    // set expectations
    expect(connectionFactory.getConnection()).andReturn(connection).once();
    expect(connection.createStatement()).andReturn(statement).once();
    expect(statement.executeQuery(anyObject(String.class))).andReturn(resultSet).once();
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getString("RecordTypeContext")).andReturn("rpc");
    expect(resultSet.getString("RecordTypeName")).andReturn("rpc");
    expect(resultSet.getString("TagPairs")).andReturn("");
    expect(resultSet.getString("MetricName")).andReturn("RpcSlowResponse_num_ops");
    expect(resultSet.getString("ServiceName")).andReturn("datanode");
    expect(resultSet.getString("NodeName")).andReturn("host1");
    expect(resultSet.getNString("MetricValue")).andReturn("0");
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999990L);
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getString("RecordTypeContext")).andReturn("rpc");
    expect(resultSet.getString("RecordTypeName")).andReturn("rpc");
    expect(resultSet.getString("TagPairs")).andReturn("");
    expect(resultSet.getString("MetricName")).andReturn("RpcSlowResponse_num_ops");
    expect(resultSet.getString("ServiceName")).andReturn("datanode");
    expect(resultSet.getString("NodeName")).andReturn("host1");
    expect(resultSet.getNString("MetricValue")).andReturn("1");
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999991L);
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getString("RecordTypeContext")).andReturn("rpc");
    expect(resultSet.getString("RecordTypeName")).andReturn("rpc");
    expect(resultSet.getString("TagPairs")).andReturn("");
    expect(resultSet.getString("MetricName")).andReturn("RpcSlowResponse_num_ops");
    expect(resultSet.getString("ServiceName")).andReturn("datanode");
    expect(resultSet.getString("NodeName")).andReturn("host1");
    expect(resultSet.getNString("MetricValue")).andReturn("2");
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999992L);
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getString("RecordTypeContext")).andReturn("rpc");
    expect(resultSet.getString("RecordTypeName")).andReturn("rpc");
    expect(resultSet.getString("TagPairs")).andReturn("");
    expect(resultSet.getString("MetricName")).andReturn("RpcSlowResponse_num_ops");
    expect(resultSet.getString("ServiceName")).andReturn("datanode");
    expect(resultSet.getString("NodeName")).andReturn("host1");
    expect(resultSet.getNString("MetricValue")).andReturn("3");
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999993L);
    expect(resultSet.next()).andReturn(false);

    // replay
    replay(connectionFactory, connection, statement, resultSet);

    SQLPropertyProvider provider = new SQLPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent),
        new TestHostInfoProvider(),
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID,
        SERVICE_NAME_PROPERTY_ID,
        connectionFactory);

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");
    resource.setProperty(SERVICE_NAME_PROPERTY_ID, "HDFS");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    Request request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID_2), temporalInfoMap);

    Assert.assertEquals(1, provider.populateResources(Collections.singleton(resource), request, null).size());

    // should be the last value of the time series...
    Assert.assertEquals( 3L, resource.getPropertyValue(PROPERTY_ID_2));

    // verify
    verify(connectionFactory, connection, statement, resultSet);
  }


  @Test
  public void testPopulateResources_multi() throws Exception {

    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
    Connection connection = createNiceMock(Connection.class);
    Statement statement = createNiceMock(Statement.class);
    ResultSet resultSet = createNiceMock(ResultSet.class);

    // set expectations
    expect(connectionFactory.getConnection()).andReturn(connection).once();
    expect(connection.createStatement()).andReturn(statement).once();
    expect(statement.executeQuery(anyObject(String.class))).andReturn(resultSet).once();
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getString("RecordTypeContext")).andReturn("rpc");
    expect(resultSet.getString("RecordTypeName")).andReturn("rpc");
    expect(resultSet.getString("TagPairs")).andReturn("");
    expect(resultSet.getString("MetricName")).andReturn("RpcQueueTime_avg_time");
    expect(resultSet.getString("ServiceName")).andReturn("datanode");
    expect(resultSet.getString("NodeName")).andReturn("host1");
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999990L);
    expect(resultSet.getNString("MetricValue")).andReturn("0");
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getString("RecordTypeContext")).andReturn("rpc");
    expect(resultSet.getString("RecordTypeName")).andReturn("rpc");
    expect(resultSet.getString("TagPairs")).andReturn("");
    expect(resultSet.getString("MetricName")).andReturn("RpcQueueTime_avg_time");
    expect(resultSet.getString("ServiceName")).andReturn("datanode");
    expect(resultSet.getString("NodeName")).andReturn("host1");
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999991L);
    expect(resultSet.getNString("MetricValue")).andReturn("1");

    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getString("RecordTypeContext")).andReturn("rpc");
    expect(resultSet.getString("RecordTypeName")).andReturn("rpc");
    expect(resultSet.getString("TagPairs")).andReturn("");
    expect(resultSet.getString("MetricName")).andReturn("RpcSlowResponse_num_ops");
    expect(resultSet.getString("ServiceName")).andReturn("datanode");
    expect(resultSet.getString("NodeName")).andReturn("host1");
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999992L);
    expect(resultSet.getNString("MetricValue")).andReturn("2");
    expect(resultSet.next()).andReturn(true);
    expect(resultSet.getString("RecordTypeContext")).andReturn("rpc");
    expect(resultSet.getString("RecordTypeName")).andReturn("rpc");
    expect(resultSet.getString("TagPairs")).andReturn("");
    expect(resultSet.getString("MetricName")).andReturn("RpcSlowResponse_num_ops");
    expect(resultSet.getString("ServiceName")).andReturn("datanode");
    expect(resultSet.getString("NodeName")).andReturn("host1");
    expect(resultSet.getLong("RecordTimeStamp")).andReturn(999993L);
    expect(resultSet.getNString("MetricValue")).andReturn("3");
    expect(resultSet.next()).andReturn(false);

    // replay
    replay(connectionFactory, connection, statement, resultSet);

    SQLPropertyProvider provider = new SQLPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent),
        new TestHostInfoProvider(),
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID,
        SERVICE_NAME_PROPERTY_ID,
        connectionFactory);

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");
    resource.setProperty(SERVICE_NAME_PROPERTY_ID, "HDFS");

    // ask for two properties ... on temporal, one point in time
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(PROPERTY_ID_1, new TemporalInfoImpl(10L, 20L, 1L));

    Set<String> propertyIds = new LinkedHashSet<String>();
    propertyIds.add(PROPERTY_ID_1);
    propertyIds.add(PROPERTY_ID_2);

    Request request = PropertyHelper.getReadRequest(propertyIds, temporalInfoMap);

    Assert.assertEquals(1, provider.populateResources(Collections.singleton(resource), request, null).size());

    // check the temporal value
    Assert.assertTrue(resource.getPropertyValue(PROPERTY_ID_1) instanceof Number[][]);

    Number[][] datapoints = (Number[][]) resource.getPropertyValue(PROPERTY_ID_1);

    for (int i = 0; i < datapoints.length; ++i) {
      Assert.assertEquals((long) i, datapoints[i][0]);
      Assert.assertEquals((999990L + i) / 1000, datapoints[i][1]);
    }

    // check the point in time value ... should be the last value of the time series...
    Assert.assertEquals( 3L, resource.getPropertyValue(PROPERTY_ID_2));

    // verify
    verify(connectionFactory, connection, statement, resultSet);
  }
}
