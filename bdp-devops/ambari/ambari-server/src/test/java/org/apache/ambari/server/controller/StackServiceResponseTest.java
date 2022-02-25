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

package org.apache.ambari.server.controller;

import static org.junit.Assert.assertTrue;

import org.apache.ambari.server.state.ServiceInfo;
import org.junit.Before;
import org.junit.Test;



public class StackServiceResponseTest {

  private ServiceInfo serviceInfo;

  @Before
  public void setUp() {
    serviceInfo = new ServiceInfo();
  }

  @Test
  public void testDefaultServiceVisibilityProperties() {
    StackServiceResponse stackServiceResponse = new StackServiceResponse(serviceInfo);


    assertTrue("true".equals(stackServiceResponse.getServiceProperties().get(ServiceInfo.DEFAULT_SERVICE_INSTALLABLE_PROPERTY.getKey())));
    assertTrue("true".equals(stackServiceResponse.getServiceProperties().get(ServiceInfo.DEFAULT_SERVICE_MANAGED_PROPERTY.getKey())));
    assertTrue("true".equals(stackServiceResponse.getServiceProperties().get(ServiceInfo.DEFAULT_SERVICE_MONITORED_PROPERTY.getKey())));
  }


}
