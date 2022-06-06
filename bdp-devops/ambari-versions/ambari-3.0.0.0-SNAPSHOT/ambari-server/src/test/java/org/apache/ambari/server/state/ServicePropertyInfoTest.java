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

import static junit.framework.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class ServicePropertyInfoTest {
  private static final String XML =
    "<property>\n" +
    "  <name>prop_name</name>\n" +
    "  <value>prop_value</value>\n"+
    "</property>";

  @Test
  public void testName() throws Exception {
    // Given
    ServicePropertyInfo p = getServiceProperty(XML);

    // When
    String name = p.getName();

    // Then
    assertEquals("prop_name", name);
  }

  @Test
  public void testValue() throws Exception {
    // Given
    ServicePropertyInfo p = getServiceProperty(XML);

    // When
    String value = p.getValue();

    // Then
    assertEquals("prop_value", value);
  }

  @Test
  public void testEquals() throws Exception {
    EqualsVerifier.forClass(ServicePropertyInfo.class)
      .suppress(Warning.NONFINAL_FIELDS)
      .verify();
  }


  public static ServicePropertyInfo getServiceProperty(String xml) throws JAXBException {
    JAXBContext jaxbContext = JAXBContext.newInstance(ServicePropertyInfo.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

    return unmarshaller.unmarshal(
      new StreamSource(
        new ByteArrayInputStream(xml.getBytes())
      ),
      ServicePropertyInfo.class)
    .getValue();
  }
}
