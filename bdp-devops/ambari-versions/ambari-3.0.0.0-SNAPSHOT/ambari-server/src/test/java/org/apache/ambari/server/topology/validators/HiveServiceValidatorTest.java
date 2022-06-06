/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology.validators;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HiveServiceValidatorTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private ClusterTopology clusterTopologyMock;

  @Mock
  private Blueprint blueprintMock;

  @Mock
  private Configuration configurationMock;

  @TestSubject
  private HiveServiceValidator hiveServiceValidator = new HiveServiceValidator();

  @Before
  public void setUp() throws Exception {


  }

  @After
  public void tearDown() throws Exception {
    resetAll();
  }

  @Test
  public void testShouldValidationPassWhenHiveServiceIsNotInBlueprint() throws Exception {

    // GIVEN
    EasyMock.expect(clusterTopologyMock.getBlueprint()).andReturn(blueprintMock);
    EasyMock.expect(blueprintMock.getServices()).andReturn(Collections.emptySet());
    replayAll();

    // WHEN
    hiveServiceValidator.validate(clusterTopologyMock);

    // THEN

  }

  @Test(expected = InvalidTopologyException.class)
  public void testShouldValidationFailWhenHiveServiceIsMissingConfigType() throws Exception {

    // GIVEN
    Collection<String> blueprintServices = Arrays.asList("HIVE", "OOZIE");
    EasyMock.expect(clusterTopologyMock.getBlueprint()).andReturn(blueprintMock);
    EasyMock.expect(blueprintMock.getServices()).andReturn(blueprintServices);
    EasyMock.expect(clusterTopologyMock.getConfiguration()).andReturn(configurationMock);
    EasyMock.expect(configurationMock.getAllConfigTypes()).andReturn(Collections.emptySet());

    replayAll();

    // WHEN
    hiveServiceValidator.validate(clusterTopologyMock);

    // THEN

  }

  @Test
  public void testShouldValidationPassWhenCustomHiveDatabaseSettingsProvided() throws Exception {

    // GIVEN
    Collection<String> blueprintServices = Arrays.asList("HIVE", "OOZIE");
    Collection<String> configTypes = Arrays.asList("hive-env", "core-site", "hadoop-env");
    EasyMock.expect(clusterTopologyMock.getBlueprint()).andReturn(blueprintMock);
    EasyMock.expect(blueprintMock.getServices()).andReturn(blueprintServices);
    EasyMock.expect(clusterTopologyMock.getConfiguration()).andReturn(configurationMock);
    EasyMock.expect(configurationMock.getAllConfigTypes()).andReturn(configTypes);

    EasyMock.expect(configurationMock.getPropertyValue("hive-env", "hive_database")).andReturn("PSQL");
    replayAll();

    // WHEN
    hiveServiceValidator.validate(clusterTopologyMock);

    // THEN

  }

  @Test(expected = InvalidTopologyException.class)
  public void testShouldValidationFailWhenDefaultsAreUsedAndMysqlComponentIsMissing() throws Exception {
    // GIVEN
    Collection<String> blueprintServices = Arrays.asList("HIVE", "HDFS");
    Collection<String> configTypes = Arrays.asList("hive-env", "core-site", "hadoop-env");
    EasyMock.expect(clusterTopologyMock.getBlueprint()).andReturn(blueprintMock).anyTimes();
    EasyMock.expect(blueprintMock.getServices()).andReturn(blueprintServices).anyTimes();
    EasyMock.expect(blueprintMock.getComponents("HIVE")).andReturn(Collections.emptyList()).anyTimes();
    EasyMock.expect(clusterTopologyMock.getConfiguration()).andReturn(configurationMock);
    EasyMock.expect(configurationMock.getAllConfigTypes()).andReturn(configTypes);

    EasyMock.expect(configurationMock.getPropertyValue("hive-env", "hive_database")).andReturn("New MySQL Database");
    replayAll();


    // WHEN
    hiveServiceValidator.validate(clusterTopologyMock);

    // THEN

  }

  @Test
  public void testShouldValidationPassWhenDefaultsAreUsedAndMsqlComponentIsListed() throws Exception {
    // GIVEN
    Collection<String> blueprintServices = Arrays.asList("HIVE", "HDFS", "MYSQL_SERVER");
    Collection<String> hiveComponents = Arrays.asList("MYSQL_SERVER");
    Collection<String> configTypes = Arrays.asList("hive-env", "core-site", "hadoop-env");
    EasyMock.expect(clusterTopologyMock.getBlueprint()).andReturn(blueprintMock).anyTimes();
    EasyMock.expect(blueprintMock.getServices()).andReturn(blueprintServices).anyTimes();
    EasyMock.expect(blueprintMock.getComponents("HIVE")).andReturn(hiveComponents).anyTimes();
    EasyMock.expect(clusterTopologyMock.getConfiguration()).andReturn(configurationMock);
    EasyMock.expect(configurationMock.getAllConfigTypes()).andReturn(configTypes);

    EasyMock.expect(configurationMock.getPropertyValue("hive-env", "hive_database")).andReturn("New MySQL Database");
    replayAll();

    // WHEN
    hiveServiceValidator.validate(clusterTopologyMock);

    // THEN

  }
}