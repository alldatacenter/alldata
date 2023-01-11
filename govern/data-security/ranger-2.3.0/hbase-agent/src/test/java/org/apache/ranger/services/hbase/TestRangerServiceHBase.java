/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.services.hbase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestRangerServiceHBase {
	static final String 	sdName		  =  "svcDef-HBase";
	static final String 	serviceName   =  "HBaseDef";
	HashMap<String, Object> responseData  =  null;
	Map<String, String> 	configs 	  =  null;
	RangerServiceHBase 		svcHBase	  =  null;
	RangerServiceDef 		sd 			  =  null;
	RangerService			svc			  =  null;
	ResourceLookupContext   lookupContext =  null;
	
	
	@Before
	public void setup() {
		configs 	= new HashMap<String,String>();
		lookupContext = new ResourceLookupContext();
		
		buildHbaseConnectionConfig();
		buildLookupContext();
		
		sd		 = new RangerServiceDef(sdName, "org.apache.ranger.services.hbase.RangerServiceHBase", "TestService", "test servicedef description", null, null, null, null, null, null, null);
		svc   	 = new RangerService(sdName, serviceName, "unit test hbase resource lookup and validateConfig", null, configs);
		svcHBase = new RangerServiceHBase();
		svcHBase.init(sd, svc);
	}
	
	@Test
	public void testValidateConfig() {

		/* TODO: does this test require a live HBase environment?
		 *
		HashMap<String,Object> ret = null;
		String errorMessage = null;
		
		try {
			ret = svcHBase.validateConfig();
		}catch (Exception e) {
			errorMessage = e.getMessage();
			if ( e instanceof HadoopException) {
				errorMessage = "HadoopException";
			}
		}
		
		if ( errorMessage != null) {
			assertTrue(errorMessage.contains("HadoopException"));
		} else {
			assertNotNull(ret);
		}
		*
		*/
	}
	
	
	@Test
	public void	testLookUpResource() {
		/* TODO: does this test require a live HBase environment?
		 *
		List<String> ret 	= new ArrayList<String>();
		List<String> mockresult = new ArrayList<String>(){{add("iemployee");add("idepartment");}};
		String errorMessage = null;
		HBaseClient hbaseClient = new HBaseClient("hbasedev", configs);
		try {
			Mockito.when(hbaseClient.getTableList("iem", null)).thenReturn(mockresult);
			ret = svcHBase.lookupResource(lookupContext);
		}catch (Throwable e) {
			errorMessage = e.getMessage();
			if ( e instanceof HadoopException) {
				errorMessage = "HadoopException";
			}
		}
		
		if ( errorMessage != null) {
			assertTrue(errorMessage.contains("HadoopException"));
		} else {
			assertNotNull(ret);
		}
		*
		*/
	}
	
	public void buildHbaseConnectionConfig() {
		configs.put("username", "hbaseuser");
		configs.put("password", "*******");
		configs.put("hadoop.security.authentication", "simple");
		configs.put("hbase.master.kerberos.principal", "hbase/_HOST@EXAMPLE.COM");
		configs.put("hbase.security.authentication", "simple");
		configs.put("hbase.zookeeper.property.clientPort", "2181");
		configs.put("hbase.zookeeper.quorum", "localhost");
		configs.put("zookeeper.znode.parent","/hbase-unsecure");
		configs.put("isencrypted", "true");
	}

	public void buildLookupContext() {
		Map<String, List<String>> resourceMap = new HashMap<String,List<String>>();
		resourceMap.put(null, null);
		lookupContext.setUserInput("iem");
		lookupContext.setResourceName("table");
		lookupContext.setResources(resourceMap);
	}
	
			
	@After
	public void tearDown() {
		sd  = null;
		svc = null;
	}
	
}
