/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ranger.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.ranger.view.VXResponse;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestJSONUtil {

	@Autowired
	JSONUtil jsonUtil = new JSONUtil();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testJsonToMapNull() {
		String jsonStr = null;
		Map<String, String> dbMap = jsonUtil.jsonToMap(jsonStr);
		Assert.assertEquals(dbMap.get(jsonStr), jsonStr);
	}

	@Test
	public void testJsonToMapIsEmpty() {
		String jsonStr = "";
		Map<String, String> dbMap = jsonUtil.jsonToMap(jsonStr);
		boolean isEmpty = dbMap.isEmpty();
		Assert.assertTrue(isEmpty);
	}

	@Test
	public void testJsonToMap() {
		String jsonStr = "{\"username\":\"admin\",\"password\":\"admin\",\"fs.default.name\":\"defaultnamevalue\",\"hadoop.security.authorization\":\"authvalue\",\"hadoop.security.authentication\":\"authenticationvalue\",\"hadoop.security.auth_to_local\":\"localvalue\",\"dfs.datanode.kerberos.principal\":\"principalvalue\",\"dfs.namenode.kerberos.principal\":\"namenodeprincipalvalue\",\"dfs.secondary.namenode.kerberos.principal\":\"secprincipalvalue\",\"commonNameForCertificate\":\"certificatevalue\"}";
		Map<String, String> dbMap = jsonUtil.jsonToMap(jsonStr);
	    Assert.assertNotNull(dbMap);
	}

	@Test
	public void testReadMapToString() {		
		Map<?, ?> map = new HashMap<Object, Object>();
		String value = jsonUtil.readMapToString(map);
		Assert.assertNotNull(value);
        }

        @Test
        public void testReadListToString() {
                String expectedJsonString = "[\"hdfs\",\"hive\",\"knox\"]";
                List<String> testList = new ArrayList<String>();

                testList.add("hdfs");
                testList.add("hive");
                testList.add("knox");

                String actualJsonString = jsonUtil.readListToString(testList);

                Assert.assertEquals(expectedJsonString, actualJsonString);
        }

        @Test
        public void testWriteObjectAsString(){
                String expectedJsonString = "{\"statusCode\":200,\"msgDesc\":\"Logout Successful\"}";
                VXResponse vXResponse = new VXResponse();
                vXResponse.setStatusCode(HttpServletResponse.SC_OK);
                vXResponse.setMsgDesc("Logout Successful");
                String actualJsonString = jsonUtil.writeObjectAsString(vXResponse);

                Assert.assertEquals(expectedJsonString, actualJsonString);

        }

        @Test
        public void testWriteJsonToJavaObject(){
                String jsonString = "[\"hdfs\",\"hive\",\"knox\"]";
                Set<String> expectedSet = new HashSet<String>();
                expectedSet.add("hive");
                expectedSet.add("hdfs");
                expectedSet.add("knox");
                Set<String> testSet = new HashSet<>();
                Set<String> actualSet = jsonUtil.writeJsonToJavaObject(jsonString, testSet.getClass());
                Assert.assertEquals(expectedSet, actualSet);


        }
}