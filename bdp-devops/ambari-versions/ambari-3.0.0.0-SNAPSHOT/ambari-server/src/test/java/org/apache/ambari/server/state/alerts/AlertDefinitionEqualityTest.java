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

import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.alert.AggregateSource;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.MetricSource;
import org.apache.ambari.server.state.alert.PercentSource;
import org.apache.ambari.server.state.alert.PortSource;
import org.apache.ambari.server.state.alert.Reporting;
import org.apache.ambari.server.state.alert.Reporting.ReportTemplate;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.ScriptSource;
import org.apache.ambari.server.state.alert.Source;
import org.apache.ambari.server.state.alert.SourceType;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import junit.framework.TestCase;

/**
 * Tests equality of {@link AlertDefinition} for hashing and merging purposes.
 */
@Category({ category.AlertTest.class})
public class AlertDefinitionEqualityTest extends TestCase {

  @Test
  public void testAlertDefinitionEquality() {
    AlertDefinition ad1 = getAlertDefinition(SourceType.PORT);
    AlertDefinition ad2 = getAlertDefinition(SourceType.PORT);

    assertTrue(ad1.equals(ad2));
    assertTrue(ad1.deeplyEquals(ad2));

    // change 1 property and check that name equality still works
    ad2.setInterval(2);
    assertTrue(ad1.equals(ad2));
    assertFalse(ad1.deeplyEquals(ad2));

    // change the name and verify name equality is broken
    ad2.setName(getName() + " foo");
    assertFalse(ad1.equals(ad2));
    assertFalse(ad1.deeplyEquals(ad2));

    ad2 = getAlertDefinition(SourceType.AGGREGATE);
    assertFalse(ad1.deeplyEquals(ad2));

    ad2 = getAlertDefinition(SourceType.PORT);
    assertTrue(ad1.deeplyEquals(ad2));

    ad2.getSource().getReporting().getOk().setText("foo");
    assertFalse(ad1.deeplyEquals(ad2));
  }

  /**
   * @param sourceType
   * @return
   */
  private AlertDefinition getAlertDefinition(SourceType sourceType) {
    AlertDefinition definition = new AlertDefinition();
    definition.setClusterId(1);
    definition.setComponentName("component");
    definition.setEnabled(true);
    definition.setInterval(1);
    definition.setName("Name");
    definition.setScope(Scope.ANY);
    definition.setServiceName("ServiceName");
    definition.setLabel("Label");
    definition.setDescription("Description");
    definition.setSource(getSource(sourceType));

    return definition;
  }

  /**
   * @param type
   * @return
   */
  private Source getSource(SourceType type) {
    Source source = null;
    switch (type) {
      case AGGREGATE:
        source = new AggregateSource();
        ((AggregateSource) source).setAlertName("hdfs-foo");
        break;
      case METRIC:
        source = new MetricSource();
        break;
      case PERCENT:
        source = new PercentSource();
        break;
      case PORT:
        source = new PortSource();
        ((PortSource) source).setPort(80);
        ((PortSource) source).setUri("uri://foo");
        break;
      case SCRIPT:
        source = new ScriptSource();
        break;
      default:
        break;
    }

    source.setReporting(getReporting());
    return source;
  }

  /**
   * @return
   */
  private Reporting getReporting() {
    Reporting reporting = new Reporting();
    reporting.setCritical(getReportingTemplate(AlertState.CRITICAL));
    reporting.setWarning(getReportingTemplate(AlertState.WARNING));
    reporting.setOk(getReportingTemplate(AlertState.OK));

    return reporting;
  }

  /**
   * @param state
   * @return
   */
  private ReportTemplate getReportingTemplate(AlertState state) {
    ReportTemplate template = new ReportTemplate();
    switch (state) {
      case CRITICAL:
        template.setText("OH NO!");
        template.setValue(80.0);
        break;
      case OK:
        template.setText("No worries.");
        break;
      case UNKNOWN:
        break;
      case WARNING:
        template.setText("Getting nervous...");
        template.setValue(50.0);
        break;
      default:
        break;
    }
    return template;
  }
}
