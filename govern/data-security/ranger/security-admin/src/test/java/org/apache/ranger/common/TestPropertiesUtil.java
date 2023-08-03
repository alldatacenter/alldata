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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

public class TestPropertiesUtil {

	@Autowired
	PropertiesUtil propertiesUtil;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testGetPropertyNull() {
		String key=null;
		String defaultValue="test";
		String value= PropertiesUtil.getProperty(key, defaultValue);
		Assert.assertNull(value);
	}
	
	@Test
	public void testGetProperty() {
		String key="1";
		String defaultValue="test";
		String value= PropertiesUtil.getProperty(key, defaultValue);
		Assert.assertNotNull(value);
	}	
	
	@Test
	public void testgetPropertyNullValue(){
		String key=null;
		String value = PropertiesUtil.getProperty(key);
		Assert.assertNull(value);
	}
	
	@Test
	public void testGetIntPropertyNull1(){
		String key=null;
		Integer value = PropertiesUtil.getIntProperty(key);
		Assert.assertNull(value);
	}
	
	@Test
	public void testGetIntPropertyl1(){
		String key="1";
		Integer value= PropertiesUtil.getIntProperty(key);
		Assert.assertNull(value);
	}	
	
	@Test
	public void testGetIntPropertyNull(){
		String key=null;
		int defaultValue=0;
		Integer value = PropertiesUtil.getIntProperty(key, defaultValue);
		Assert.assertNotNull(value);
		Assert.assertEquals(value.intValue(), defaultValue);
	}
	
	@Test
	public void testGetIntPropertyl(){
		String key="1";
		int defaultValue=1;
		Integer value= PropertiesUtil.getIntProperty(key, defaultValue);
		Assert.assertEquals(value, Integer.valueOf(defaultValue));
	}

	@Test
	public void testGetLongProperty(){
		String key="longKey";
		long defaultValue=23L;
		Long value = PropertiesUtil.getLongProperty(key, defaultValue);
		Assert.assertNotNull(value);
		Assert.assertEquals(value.intValue(), defaultValue);
	}

	@Test
	public void testGetBooleanPropertyNull() {
		String key = null;
		boolean defaultValue = true;
		boolean returnAvlue = PropertiesUtil.getBooleanProperty(key , defaultValue);
		Assert.assertTrue(returnAvlue);
	}
	
	@Test
	public void testGetBooleanProperty() {
		String key = "1";
		boolean defaultValue = true;
		boolean returnAvlue = PropertiesUtil.getBooleanProperty(key , defaultValue);
		Assert.assertTrue(returnAvlue);
	}
	
	@Test
        public void testGetPropertyStringListForNull(){
		String key = null;
		PropertiesUtil.getPropertyStringList(key);
		Assert.assertNull(key);
	}
	
        @Test
        public void testGetPropertyStringList(){
                String key = "ranger.users.roles.list";

                PropertiesUtil.getPropertiesMap().put("ranger.users.roles.list", "read,write,access");
                String[] actualroles = PropertiesUtil.getPropertyStringList(key);

                Assert.assertEquals("read", actualroles[0]);
                Assert.assertEquals("write", actualroles[1]);
                Assert.assertEquals("access", actualroles[2]);

        }

}