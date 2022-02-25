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

import org.apache.ambari.server.configuration.Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

/**
 * SinkConnectionFactory Tests.
 */
public class SinkConnectionFactoryTest {

  public static SinkConnectionFactory getFactory(String url, String driver) {
    Properties ambariProperties = new Properties();
    ambariProperties.setProperty(SinkConnectionFactory.SCOM_SINK_DB_URL, url);
    ambariProperties.setProperty(SinkConnectionFactory.SCOM_SINK_DB_DRIVER, driver);

    Configuration configuration =  new TestConfiguration(ambariProperties);

    SinkConnectionFactory connectionFactory = new SinkConnectionFactory();

    connectionFactory.init(configuration);

    return connectionFactory;
  }

  @Test
  public void testGetDatabaseUrl() throws Exception {
    SinkConnectionFactory factory = getFactory("myURL", "myDriver");
    Assert.assertEquals("myURL", factory.getDatabaseUrl());
  }

  @Test
  public void testGetDatabaseDriver() throws Exception {
    SinkConnectionFactory factory = getFactory("myURL", "myDriver");
    Assert.assertEquals("myDriver", factory.getDatabaseDriver());
  }

  private static class TestConfiguration extends Configuration {

    private TestConfiguration(Properties properties) {
      super(properties);
    }

    @Override
    protected void loadSSLParams() {
    }
  }
}
