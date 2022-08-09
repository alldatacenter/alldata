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

package org.apache.ambari.server.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * ServiceInfo tests.
 */
public class ServiceInfoTest {

  @Test
  public void testIsRestartRequiredAfterRackChange() throws Exception {

    String serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>RESTART</name>\n" +
        "      <displayName>RESTART</displayName>\n" +
        "      <comment>Apache Hadoop Distributed File System</comment>\n" +
        "      <version>2.1.0.2.0</version>\n" +
        "      <restartRequiredAfterRackChange>true</restartRequiredAfterRackChange>\n" +
        "    </service>\n" +
        "    <service>\n" +
        "      <name>NO_RESTART</name>\n" +
        "      <displayName>NO_RESTART</displayName>\n" +
        "      <comment>Apache Hadoop Distributed File System</comment>\n" +
        "      <version>2.1.0.2.0</version>\n" +
        "      <restartRequiredAfterRackChange>false</restartRequiredAfterRackChange>\n" +
        "    </service>\n" +
        "    <service>\n" +
        "      <name>DEFAULT_RESTART</name>\n" +
        "      <displayName>DEFAULT_RESTART</displayName>\n" +
        "      <comment>Apache Hadoop Distributed File System</comment>\n" +
        "      <version>2.1.0.2.0</version>\n" +
        "    </service>\n" +
        "    <service>\n" +
        "      <name>HCFS_SERVICE</name>\n" +
        "      <displayName>HCFS_SERVICE</displayName>\n" +
        "      <comment>Hadoop Compatible File System</comment>\n" +
        "      <version>2.1.1.0</version>\n" +
        "      <serviceType>HCFS</serviceType>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";

    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);

    assertTrue(serviceInfoMap.get("RESTART").isRestartRequiredAfterRackChange());
    assertFalse(serviceInfoMap.get("NO_RESTART").isRestartRequiredAfterRackChange());
    assertNull(serviceInfoMap.get("DEFAULT_RESTART").isRestartRequiredAfterRackChange());
    assertEquals(serviceInfoMap.get("HCFS_SERVICE").getServiceType(), "HCFS");
  }

  @Test
  public void testCustomMetricsWidgetsFiles() throws Exception {

    String serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>CUSTOM</name>\n" +
        "      <displayName>CUSTOM</displayName>\n" +
        "      <metricsFileName>CUSTOM_metrics.json</metricsFileName>\n" +
        "      <widgetsFileName>CUSTOM_widgets.json</widgetsFileName>\n" +
        "    </service>\n" +
        "    <service>\n" +
        "      <name>DEFAULT</name>\n" +
        "      <displayName>DEFAULT</displayName>\n" +
        "      <comment>Apache Hadoop Distributed File System</comment>\n" +
        "      <version>2.1.0.2.0</version>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";

    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);

    assertEquals("CUSTOM_metrics.json", serviceInfoMap.get("CUSTOM").getMetricsFileName());
    assertEquals("CUSTOM_widgets.json", serviceInfoMap.get("CUSTOM").getWidgetsFileName());
    assertEquals("metrics.json", serviceInfoMap.get("DEFAULT").getMetricsFileName());
    assertEquals("widgets.json", serviceInfoMap.get("DEFAULT").getWidgetsFileName());
  }

  @Test
  public void testSelectionField() throws Exception {
    String serviceInfoXmlDeprecated = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>DEPRECATED</name>\n" +
        "      <selection>DEPRECATED</selection>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";
    Map<String, ServiceInfo> serviceInfoMapDeprecated = getServiceInfo(serviceInfoXmlDeprecated);
    ServiceInfo deprecated = serviceInfoMapDeprecated.get("DEPRECATED");
    assertEquals(deprecated.getSelection(), ServiceInfo.Selection.DEPRECATED);
    assertFalse(deprecated.isSelectionEmpty());

    String serviceInfoXmlDefault = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>DEFAULT</name>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";
    Map<String, ServiceInfo> serviceInfoMapDefault = getServiceInfo(serviceInfoXmlDefault);
    ServiceInfo defaultSi = serviceInfoMapDefault.get("DEFAULT");
    assertEquals(defaultSi.getSelection(), ServiceInfo.Selection.DEFAULT);
    assertTrue(defaultSi.isSelectionEmpty());
  }

  /**
   * Tests the presence and absence of the credential-store block.
   *
   * @throws Exception
   */
  @Test
  public void testCredentialStoreFields() throws Exception {
    /*
     * <credential-store> supported and enabled.
     */
    String serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>RANGER</name>\n" +
        "      <credential-store>\n" +
        "          <supported>true</supported>\n" +
        "          <enabled>true</enabled>\n" +
        "          <required>true</required>\n" +
        "      </credential-store>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";
    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);
    ServiceInfo service = serviceInfoMap.get("RANGER");
    assertTrue(service.isCredentialStoreSupported());
    assertTrue(service.isCredentialStoreEnabled());
    assertTrue(service.isCredentialStoreRequired());

    /*
     * <credential-store> supported but not enabled.
     */
    serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>HIVE</name>\n" +
        "      <credential-store>\n" +
        "          <supported>true</supported>\n" +
        "          <enabled>false</enabled>\n" +
        "          <required>false</required>\n" +
        "      </credential-store>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";
    serviceInfoMap = getServiceInfo(serviceInfoXml);
    service = serviceInfoMap.get("HIVE");
    assertTrue(service.isCredentialStoreSupported());
    assertFalse(service.isCredentialStoreEnabled());
    assertFalse(service.isCredentialStoreRequired());

    /*
     * <credential-store> is missing
     */
    serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>AMBARI_METRICS</name>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";
    serviceInfoMap = getServiceInfo(serviceInfoXml);
    service = serviceInfoMap.get("AMBARI_METRICS");
    assertFalse(service.isCredentialStoreSupported());
    assertFalse(service.isCredentialStoreEnabled());

    /*
     * <credential-store><enabled> is missing. Invalid
     * scenario. So both values should be false.
     */
    serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>HBASE</name>\n" +
        "      <credential-store>\n" +
        "          <supported>true</supported>\n" +
        "      </credential-store>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";
    serviceInfoMap = getServiceInfo(serviceInfoXml);
    service = serviceInfoMap.get("HBASE");
    assertTrue(service.isCredentialStoreSupported());
    assertFalse(service.isCredentialStoreEnabled());
  }

  @Test
  public void testCredentialStoreInfoValidity() throws Exception {
    // Valid: Supported->True, Enabled->False
    String serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>RANGER</name>\n" +
        "      <credential-store>\n" +
        "          <supported>true</supported>\n" +
        "          <enabled>false</enabled>\n" +
        "      </credential-store>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";

    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);
    ServiceInfo serviceInfo = serviceInfoMap.get("RANGER");
    assertTrue(serviceInfo.isValid());

    // Valid: Supported->True, Enabled->True
    serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>RANGER</name>\n" +
        "      <credential-store>\n" +
        "          <supported>true</supported>\n" +
        "          <enabled>true</enabled>\n" +
        "      </credential-store>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";

    serviceInfoMap = getServiceInfo(serviceInfoXml);
    serviceInfo = serviceInfoMap.get("RANGER");
    assertTrue(serviceInfo.isValid());

    // Valid: Supported->False, Enabled->False
    serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>RANGER</name>\n" +
        "      <credential-store>\n" +
        "          <supported>false</supported>\n" +
        "          <enabled>false</enabled>\n" +
        "      </credential-store>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";

    serviceInfoMap = getServiceInfo(serviceInfoXml);
    serviceInfo = serviceInfoMap.get("RANGER");
    assertTrue(serviceInfo.isValid());

    // Valid: credential-store not specified
    serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>RANGER</name>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";

    serviceInfoMap = getServiceInfo(serviceInfoXml);
    serviceInfo = serviceInfoMap.get("RANGER");
    assertTrue(serviceInfo.isValid());

    // Specified but invalid
    // Invalid: Supported->False, Enabled->True
    serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>RANGER</name>\n" +
        "      <credential-store>\n" +
        "          <supported>false</supported>\n" +
        "          <enabled>true</enabled>\n" +
        "      </credential-store>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";

    serviceInfoMap = getServiceInfo(serviceInfoXml);
    serviceInfo = serviceInfoMap.get("RANGER");
    assertFalse("Credential store is enabled for a service that does not support it", serviceInfo.isValid());

    // Invalid: Supported->Unspecified, Enabled->Unspecified
    serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>RANGER</name>\n" +
        "      <credential-store>\n" +
        "      </credential-store>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";

    serviceInfoMap = getServiceInfo(serviceInfoXml);
    serviceInfo = serviceInfoMap.get("RANGER");
    assertFalse("Credential store details not specified", serviceInfo.isValid());

    // Invalid: Supported->Specified, Enabled->Unspecified
    serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>RANGER</name>\n" +
        "      <credential-store>\n" +
        "          <supported>true</supported>\n" +
        "      </credential-store>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";

    serviceInfoMap = getServiceInfo(serviceInfoXml);
    serviceInfo = serviceInfoMap.get("RANGER");
    assertFalse("Credential store enabled not specified", serviceInfo.isValid());

    // Invalid: Supported->Unspecified, Enabled->Specified
    serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>RANGER</name>\n" +
        "      <credential-store>\n" +
        "          <enabled>true</enabled>\n" +
        "      </credential-store>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";

    serviceInfoMap = getServiceInfo(serviceInfoXml);
    serviceInfo = serviceInfoMap.get("RANGER");
    assertFalse("Credential store supported not specified", serviceInfo.isValid());
  }

  @Test
  public void testSetRestartRequiredAfterRackChange() throws Exception {
    ServiceInfo serviceInfo = new ServiceInfo();

    serviceInfo.setRestartRequiredAfterRackChange(true);
    assertTrue(serviceInfo.isRestartRequiredAfterRackChange());

    serviceInfo.setRestartRequiredAfterRackChange(false);
    assertFalse(serviceInfo.isRestartRequiredAfterRackChange());
  }

  @Test
  public void testServiceProperties() throws Exception {
    String serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>WITH_PROPS</name>" +
            "      <displayName>WITH_PROPS</displayName>" +
            "      <properties>" +
            "        <property>" +
            "          <name>PROP1</name>" +
            "          <value>VAL1</value>" +
            "        </property>" +
            "        <property>" +
            "          <name>PROP2</name>" +
            "          <value>VAL2</value>" +
            "        </property>" +
            "      </properties>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";

    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);

    Map<String, String> serviceProperties = serviceInfoMap.get("WITH_PROPS").getServiceProperties();

    assertTrue(serviceProperties.containsKey("PROP1"));
    assertEquals("VAL1", serviceProperties.get("PROP1"));

    assertTrue(serviceProperties.containsKey("PROP2"));
    assertEquals("VAL2", serviceProperties.get("PROP2"));

  }


  @Test
  public void testDefaultVisibilityServiceProperties() throws Exception {
    // Given
    String serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>WITH_PROPS</name>" +
            "      <displayName>WITH_PROPS</displayName>" +
            "      <properties>" +
            "        <property>" +
            "          <name>PROP1</name>" +
            "          <value>VAL1</value>" +
            "        </property>" +
            "        <property>" +
            "          <name>PROP2</name>" +
            "          <value>VAL2</value>" +
            "        </property>" +
            "      </properties>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";

    // When
    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);

    // Then
    Map<String, String> serviceProperties = serviceInfoMap.get("WITH_PROPS").getServiceProperties();


    assertTrue("true".equals(serviceProperties.get(ServiceInfo.DEFAULT_SERVICE_INSTALLABLE_PROPERTY.getKey())));
    assertTrue("true".equals(serviceProperties.get(ServiceInfo.DEFAULT_SERVICE_MANAGED_PROPERTY.getKey())));
    assertTrue("true".equals(serviceProperties.get(ServiceInfo.DEFAULT_SERVICE_MONITORED_PROPERTY.getKey())));
  }

  @Test
  public void testVisibilityServicePropertyOverride() throws Exception {
    // Given
    String serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>WITH_PROPS</name>" +
            "      <displayName>WITH_PROPS</displayName>" +
            "      <properties>" +
            "        <property>" +
            "          <name>PROP1</name>" +
            "          <value>VAL1</value>" +
            "        </property>" +
            "        <property>" +
            "          <name>PROP2</name>" +
            "          <value>VAL2</value>" +
            "        </property>" +
            "        <property>" +
            "          <name>managed</name>" +
            "          <value>false</value>" +
            "        </property>" +
            "      </properties>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";

    // When
    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);

    // Then
    Map<String, String> serviceProperties = serviceInfoMap.get("WITH_PROPS").getServiceProperties();


    assertTrue("true".equals(serviceProperties.get(ServiceInfo.DEFAULT_SERVICE_INSTALLABLE_PROPERTY.getKey())));
    assertTrue("false".equals(serviceProperties.get(ServiceInfo.DEFAULT_SERVICE_MANAGED_PROPERTY.getKey())));
    assertTrue("true".equals(serviceProperties.get(ServiceInfo.DEFAULT_SERVICE_MONITORED_PROPERTY.getKey())));

  }

  @Test
  public void testDuplicateServicePropertyValidationAfterXmlDeserialization() throws Exception {
    // Given
    String serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <version>1.0</version>" +
            "      <name>WITH_DUPLICATE_PROPS</name>" +
            "      <displayName>WITH_PROPS</displayName>" +
            "      <properties>" +
            "        <property>" +
            "          <name>PROP1</name>" +
            "          <value>VAL1</value>" +
            "        </property>" +
            "        <property>" +
            "          <name>PROP1</name>" +
            "          <value>VAL2</value>" +
            "        </property>" +
            "        <property>" +
            "          <name>managed</name>" +
            "          <value>false</value>" +
            "        </property>" +
            "      </properties>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";

    // When
    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);
    ServiceInfo serviceInfo = serviceInfoMap.get("WITH_DUPLICATE_PROPS");

    // Then
    assertFalse("Service info should be in invalid state due to duplicate service property names !", serviceInfo.isValid());

    assertTrue("Service info error collection should contain the name of the duplicate service property !", serviceInfo.getErrors().contains("Duplicate service property with name 'PROP1' found in " + serviceInfo.getName() + ":" + serviceInfo.getVersion() + " service definition !"));

  }

  @Test
  public void testDuplicateServicePropertyValidationAfterSet() {
    // Given
    ServicePropertyInfo p1 = new ServicePropertyInfo();
    p1.setName("PROP1");
    p1.setValue("V1");

    ServicePropertyInfo p2 = new ServicePropertyInfo();
    p2.setName("PROP1");
    p2.setValue("V2");

    List<ServicePropertyInfo> servicePropertyList = Lists.newArrayList(p1, p2);

    ServiceInfo serviceInfo = new ServiceInfo();
    serviceInfo.setName("TEST_NAME");
    serviceInfo.setVersion("TEST_VERSION");
    serviceInfo.setServicePropertyList(servicePropertyList);

    // Then
    assertFalse("Service info should be in invalid state due to duplicate service property names !", serviceInfo.isValid());

    assertTrue("Service info error collection should contain the name of the duplicate service property !", serviceInfo.getErrors().contains("Duplicate service property with name 'PROP1' found in " + serviceInfo.getName() + ":" + serviceInfo.getVersion() + " service definition !"));

  }


  @Test
  public void testMultiplePimaryLogsValidationAfterXmlDeserialization() throws Exception {
    // Given
    String serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <version>1.0</version>" +
            "      <name>WITH_MULTIPLE_PRIMARY_LOGS</name>" +
            "      <displayName>WITH_MULTIPLE_PRIMARY_LOGS</displayName>" +
            "      <properties>" +
            "        <property>" +
            "          <name>managed</name>" +
            "          <value>false</value>" +
            "        </property>" +
            "      </properties>" +
            "      <components> " +
            "        <component> " +
            "          <name>COMPONENT_WITH_MULTIPLE_PRIMARY_LOG</name> " +
            "          <displayName>COMPONENT_WITH_MULTIPLE_PRIMARY_LOG</displayName> " +
            "          <category>MASTER</category> " +
            "          <cardinality>0-1</cardinality> " +
            "          <versionAdvertised>true</versionAdvertised> " +
            "          <logs> " +
            "            <log> " +
            "              <logId>log1</logId> " +
            "              <primary>true</primary> " +
            "            </log> " +
            "            <log> " +
            "              <logId>log2</logId> " +
            "              <primary>true</primary> " +
            "            </log> " +
            "          </logs> " +
            "       </component> " +
            "      </components> " +
            "    </service>" +
            "  </services>" +
            "</metainfo>";

    // When
    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);
    ServiceInfo serviceInfo = serviceInfoMap.get("WITH_MULTIPLE_PRIMARY_LOGS");

    // Then
    assertFalse("Service info should be in invalid state due to multiple primary logs at one of it's components!", serviceInfo.isValid());

    assertTrue("Service info error collection should contain the name of the component with the multiple primary logs!",
        serviceInfo.getErrors().contains("More than one primary log exists for the component COMPONENT_WITH_MULTIPLE_PRIMARY_LOG"));
  }


  @Test
  public void testSetServicePropertiesAfterPropertyListSet() {
    // Given
    ServicePropertyInfo p1 = new ServicePropertyInfo();
    p1.setName("PROP1");
    p1.setValue("V1");

    ServicePropertyInfo p2 = new ServicePropertyInfo();
    p2.setName("PROP2");
    p2.setValue("V2");

    List<ServicePropertyInfo> servicePropertyList = Lists.newArrayList(p1, p2);

    ServiceInfo serviceInfo = new ServiceInfo();
    serviceInfo.setName("TEST_NAME");
    serviceInfo.setVersion("TEST_VERSION");
    serviceInfo.setServicePropertyList(servicePropertyList);

    // When
    Map<String, String> serviceProperties = serviceInfo.getServiceProperties();

    // Then
    assertTrue(serviceProperties.containsKey("PROP1"));
    assertEquals("V1", serviceProperties.get("PROP1"));

    assertTrue(serviceProperties.containsKey("PROP2"));
    assertEquals("V2", serviceProperties.get("PROP2"));

    assertTrue("true".equals(serviceProperties.get(ServiceInfo.DEFAULT_SERVICE_INSTALLABLE_PROPERTY.getKey())));
    assertTrue("true".equals(serviceProperties.get(ServiceInfo.DEFAULT_SERVICE_MANAGED_PROPERTY.getKey())));
    assertTrue("true".equals(serviceProperties.get(ServiceInfo.DEFAULT_SERVICE_MONITORED_PROPERTY.getKey())));
  }

  @Test
  public void testSupportDeleteViaUI() throws Exception {
    //Explicitly set to true
    String serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>HDFS</name>" +
            "      <displayName>HDFS</displayName>" +
            "      <supportDeleteViaUI>true</supportDeleteViaUI>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";
    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);
    assertTrue(serviceInfoMap.get("HDFS").isSupportDeleteViaUI());

    //Explicitly set to false
    serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>HDFS</name>" +
            "      <displayName>HDFS</displayName>" +
            "      <supportDeleteViaUI>false</supportDeleteViaUI>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";
    serviceInfoMap = getServiceInfo(serviceInfoXml);
    assertFalse(serviceInfoMap.get("HDFS").isSupportDeleteViaUI());

    //Default to true
    serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>HDFS</name>" +
            "      <displayName>HDFS</displayName>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";
    serviceInfoMap = getServiceInfo(serviceInfoXml);
    assertTrue(serviceInfoMap.get("HDFS").isSupportDeleteViaUI());
  }

  @Test
  public void testSingleSignOnSupport() throws JAXBException {
    // Implicit SSO setting
    String serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>SERVICE</name>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";
    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);
    assertFalse(serviceInfoMap.get("SERVICE").isSingleSignOnSupported());
    assertTrue(serviceInfoMap.get("SERVICE").isValid());

    SingleSignOnInfo singleSignOnInfo = serviceInfoMap.get("SERVICE").getSingleSignOnInfo();
    assertNull(singleSignOnInfo);

    // Explicit SSO setting (true)
    serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>SERVICE</name>" +
            "      <sso>" +
            "        <supported>true</supported>" +
            "        <enabledConfiguration>config-type/property_name</enabledConfiguration>" +
            "        <kerberosRequired>true</kerberosRequired> " +
            "      </sso>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";
    serviceInfoMap = getServiceInfo(serviceInfoXml);
    assertTrue(serviceInfoMap.get("SERVICE").isSingleSignOnSupported());
    assertTrue(serviceInfoMap.get("SERVICE").isValid());

    singleSignOnInfo = serviceInfoMap.get("SERVICE").getSingleSignOnInfo();
    assertNotNull(singleSignOnInfo);
    assertTrue(singleSignOnInfo.isSupported());
    assertEquals(Boolean.TRUE, singleSignOnInfo.getSupported());
    assertEquals("config-type/property_name", singleSignOnInfo.getEnabledConfiguration());
    assertTrue(singleSignOnInfo.isKerberosRequired());

    // Explicit SSO setting (false)
    serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>SERVICE</name>" +
            "      <sso>" +
            "        <supported>false</supported>" +
            "      </sso>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";
    serviceInfoMap = getServiceInfo(serviceInfoXml);
    assertFalse(serviceInfoMap.get("SERVICE").isSingleSignOnSupported());
    assertTrue(serviceInfoMap.get("SERVICE").isValid());

    singleSignOnInfo = serviceInfoMap.get("SERVICE").getSingleSignOnInfo();
    assertNotNull(singleSignOnInfo);
    assertFalse(singleSignOnInfo.isSupported());
    assertEquals(Boolean.FALSE, singleSignOnInfo.getSupported());
    assertNull(singleSignOnInfo.getEnabledConfiguration());

    // Explicit SSO setting (invalid)
    serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>SERVICE</name>" +
            "      <sso>" +
            "        <supported>true</supported>" +
            "      </sso>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";
    serviceInfoMap = getServiceInfo(serviceInfoXml);
    assertTrue(serviceInfoMap.get("SERVICE").isSingleSignOnSupported());
    assertFalse(serviceInfoMap.get("SERVICE").isValid());
    assertEquals(1, serviceInfoMap.get("SERVICE").getErrors().size());

    singleSignOnInfo = serviceInfoMap.get("SERVICE").getSingleSignOnInfo();
    assertNotNull(singleSignOnInfo);
    assertTrue(singleSignOnInfo.isSupported());
    assertEquals(Boolean.TRUE, singleSignOnInfo.getSupported());
    assertNull(singleSignOnInfo.getEnabledConfiguration());
    assertNull(singleSignOnInfo.getSsoEnabledTest());
  }

  /**
   * Tests the presence and absence of the kerberosEnabledTest block.
   */
  @Test
  public void testKerberosEnabledTest() throws Exception {
    Map<String, ServiceInfo> serviceInfoMap;
    ServiceInfo service;

    String kerberosEnabledTest =
        "{\n" +
            "  \"or\": [\n" +
            "    {\n" +
            "      \"equals\": [\n" +
            "        \"core-site/hadoop.security.authentication\",\n" +
            "        \"kerberos\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"equals\": [\n" +
            "        \"hdfs-site/hadoop.security.authentication\",\n" +
            "        \"kerberos\"\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    String serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>HDFS</name>\n" +
        "      <kerberosEnabledTest>\n" +
        kerberosEnabledTest +
        "      </kerberosEnabledTest>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";
    serviceInfoMap = getServiceInfo(serviceInfoXml);
    service = serviceInfoMap.get("HDFS");
    assertEquals(kerberosEnabledTest, service.getKerberosEnabledTest().trim());

    /*
     * <kerberosEnabledTest> is missing
     */
    serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>HDFS</name>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";
    serviceInfoMap = getServiceInfo(serviceInfoXml);
    service = serviceInfoMap.get("HDFS");
    assertNull(service.getKerberosEnabledTest());
  }
  
  @Test
  public void testLdapIntegrationSupport() throws Exception {
    // Implicit SSO setting
    String serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>SERVICE</name>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";
    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);
    assertFalse(serviceInfoMap.get("SERVICE").isLdapSupported());
    assertTrue(serviceInfoMap.get("SERVICE").isValid());

    ServiceLdapInfo ldapInfo = serviceInfoMap.get("SERVICE").getLdapInfo();
    assertNull(ldapInfo);

    // Explicit LDAP setting (true)
    serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>SERVICE</name>" +
            "      <ldap>" +
            "        <supported>true</supported>" +
            "        <ldapEnabledTest>{\"equals\": [\"config-type/property_name\", \"true\"]}</ldapEnabledTest>" +
            "      </ldap>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";
    serviceInfoMap = getServiceInfo(serviceInfoXml);
    assertTrue(serviceInfoMap.get("SERVICE").isLdapSupported());
    assertTrue(serviceInfoMap.get("SERVICE").isValid());

    ldapInfo = serviceInfoMap.get("SERVICE").getLdapInfo();
    assertNotNull(ldapInfo);
    assertTrue(ldapInfo.isSupported());
    assertEquals("{\"equals\": [\"config-type/property_name\", \"true\"]}", ldapInfo.getLdapEnabledTest());

    // Explicit LDAP setting (false)
    serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>SERVICE</name>" +
            "      <ldap>" +
            "        <supported>false</supported>" +
            "      </ldap>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";
    serviceInfoMap = getServiceInfo(serviceInfoXml);
    assertFalse(serviceInfoMap.get("SERVICE").isLdapSupported());
    assertTrue(serviceInfoMap.get("SERVICE").isValid());

    ldapInfo = serviceInfoMap.get("SERVICE").getLdapInfo();
    assertNotNull(ldapInfo);
    assertFalse(ldapInfo.isSupported());
    assertNull(ldapInfo.getLdapEnabledTest());

    // Explicit SSO setting (invalid)
    serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>SERVICE</name>" +
            "      <ldap>" +
            "        <supported>true</supported>" +
            "      </ldap>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";
    serviceInfoMap = getServiceInfo(serviceInfoXml);
    assertTrue(serviceInfoMap.get("SERVICE").isLdapSupported());
    assertFalse(serviceInfoMap.get("SERVICE").isValid());
    assertEquals(1, serviceInfoMap.get("SERVICE").getErrors().size());
  }

  @Test
  public void testIsRollingRestartSupported() throws JAXBException {
    // set to True
    String serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>SERVICE</name>" +
            "      <rollingRestartSupported>true</rollingRestartSupported>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";
    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);
    assertTrue(serviceInfoMap.get("SERVICE").isRollingRestartSupported());
    assertTrue(serviceInfoMap.get("SERVICE").isValid());

    // set to False
    serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>SERVICE</name>" +
            "      <rollingRestartSupported>false</rollingRestartSupported>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";
    serviceInfoMap = getServiceInfo(serviceInfoXml);
    assertFalse(serviceInfoMap.get("SERVICE").isRollingRestartSupported());
    assertTrue(serviceInfoMap.get("SERVICE").isValid());

    // default to False
    serviceInfoXml =
        "<metainfo>" +
            "  <schemaVersion>2.0</schemaVersion>" +
            "  <services>" +
            "    <service>" +
            "      <name>SERVICE</name>" +
            "    </service>" +
            "  </services>" +
            "</metainfo>";
    serviceInfoMap = getServiceInfo(serviceInfoXml);
    assertFalse(serviceInfoMap.get("SERVICE").isRollingRestartSupported());
    assertTrue(serviceInfoMap.get("SERVICE").isValid());
  }

  private static Map<String, ServiceInfo> getServiceInfo(String xml) throws JAXBException {
    InputStream configStream = new ByteArrayInputStream(xml.getBytes());
    JAXBContext jaxbContext = JAXBContext.newInstance(ServiceMetainfoXml.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    ServiceMetainfoXml serviceMetainfoXml = (ServiceMetainfoXml) unmarshaller.unmarshal(configStream);

    Map<String, ServiceInfo> serviceInfoMap = new HashMap<>();
    for (ServiceInfo serviceInfo : serviceMetainfoXml.getServices()) {
      serviceInfoMap.put(serviceInfo.getName(), serviceInfo);
    }
    return serviceInfoMap;
  }
}


