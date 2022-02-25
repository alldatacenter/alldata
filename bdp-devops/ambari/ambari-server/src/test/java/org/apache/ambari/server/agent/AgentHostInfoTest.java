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


package org.apache.ambari.server.agent;

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import junit.framework.Assert;

/**
 * This tests makes sure the contract between the server and agent for info
 * is in tact.
 */
public class AgentHostInfoTest {
  
  @Test
  public void testDeserializeHostInfo() throws JsonParseException, 
    JsonMappingException, IOException {
    String hostinfo = "{\"architecture\": \"x86_64\", " +
        "\"augeasversion\": \"0.10.0\"," +
    		"\"domain\": \"test.com\", " +
    		"\"facterversion\": \"1.6.10\"," +
    		"\"fqdn\": \"dev.test.com\", " +
    		"\"hardwareisa\": \"x86_64\", " +
    		"\"hardwaremodel\": \"x86_64\"," +
    		"\"hostname\": \"dev\", " +
    		"\"id\": \"root\", " +
    		"\"interfaces\": \"eth0,lo\", " +
    		"\"ipaddress\": \"10.0.2.15\"," +
    		"\"ipaddress_eth0\": \"10.0.2.15\"," +
    		"\"ipaddress_lo\": \"127.0.0.1\"," +
    		"\"is_virtual\": true," +
    		"\"kernel\": \"Linux\", " +
    		"\"kernelmajversion\": \"2.6\"," +
    		"\"kernelrelease\": \"2.6.18-238.12.1.el5\"," +
    		"\"kernelversion\": \"2.6.18\", " +
    		"\"lsbdistcodename\": \"Final\"," +
    		"\"lsbdistdescription\": \"CentOS release 5.8 (Final)\"," +
    		"\"lsbdistid\": \"CentOS\", " +
    		"\"lsbdistrelease\": \"5.8\", " +
    		"\"lsbmajdistrelease\": \"5\"," +
    		"\"macaddress\": \"08:00:27:D2:59:B2\", " +
    		"\"macaddress_eth0\": \"08:00:27:D2:59:B2\"," +
    		"\"manufacturer\": \"innotek GmbH\"," +
    		"\"memoryfree\": 2453667," +
    		"\"memorysize\": 3051356, " +
    		"\"memorytotal\": 3051356," +
    		"\"netmask\": \"255.255.255.0\"}";
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    HostInfo info = mapper.readValue(hostinfo, HostInfo.class);
    Assert.assertEquals(info.getMemoryTotal(), 3051356L);
    Assert.assertEquals(info.getKernel(), "Linux");
    Assert.assertEquals(info.getFQDN(),"dev.test.com");
    Assert.assertEquals(info.getAgentUserId(), "root");
    Assert.assertEquals(info.getMemorySize(), 3051356L);
    Assert.assertEquals(info.getArchitecture(), "x86_64");
  }
}
