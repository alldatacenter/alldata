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
package org.apache.ranger.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.ranger.common.view.VEnum;
import org.apache.ranger.common.view.VEnumElement;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

public class TestRangerEnumUtil {

	@Autowired
	RangerEnumUtil xaEnumUtil = new RangerEnumUtil();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testGetEnums() {

		VEnumElement VEnumElement = new VEnumElement();
		VEnumElement.setEnumName("test1");
		VEnumElement.setElementName("test2");
		VEnumElement.setElementLabel("test3");
		VEnumElement.setElementValue(0);
		VEnumElement.setRbKey("11");
		List<VEnumElement> listVEnumElement = new ArrayList<VEnumElement>();

		VEnum vEnum = new VEnum();
		vEnum.setEnumName("test");
		vEnum.setElementList(listVEnumElement);
		             xaEnumUtil.enumList.add(vEnum);
		 List<VEnum>  dbvEnum= xaEnumUtil.getEnums();
	     Assert.assertNotNull(dbvEnum);
	}
	
	@Test
	public void testGetEnumEmpty() {
		
		String enumName = "";
	        xaEnumUtil.getEnum(enumName);
	    Assert.assertNotNull(xaEnumUtil.enumList.size() > 0);
	}

	@Test
	public void testGetEnum() {

		String enumName = "enumtest";

		VEnumElement vEnumElement1 = new VEnumElement();
		vEnumElement1.setEnumName(enumName);
		vEnumElement1.setElementName("test12");
		vEnumElement1.setElementLabel("test13");
		vEnumElement1.setElementValue(1);
		vEnumElement1.setRbKey("11");
		List<VEnumElement> VEnumElement = new ArrayList<VEnumElement>();
		                   VEnumElement.add(vEnumElement1);

		VEnum vEnum = new VEnum();
		vEnum.setEnumName(enumName);
		vEnum.setElementList(VEnumElement);

		xaEnumUtil.enumMap.put(enumName, vEnum);

		VEnum dbvEnum = xaEnumUtil.getEnum(enumName);

		Assert.assertNotNull(dbvEnum);
		Assert.assertEquals(enumName, dbvEnum.getEnumName());
	}

	@Test
	public void testGetLabelIsNUll() {
		String enumName = "CommonEnums.ActiveStatus";
		int enumValue = 1;		
		String value = xaEnumUtil.getLabel(enumName, enumValue);
		boolean checkValue=value.isEmpty();
		Assert.assertFalse(checkValue);
	}

	@Test
	public void testGetLabel() {
		testGetEnum();
		String enumName = "CommonEnums.ActiveStatus";
		int enumValue = 1;		
		String value = xaEnumUtil.getLabel(enumName, enumValue);
		Assert.assertNotNull(value);
	}
	
	@Test
	public void testgetValueIsNull() {
		String enumName = "CommonEnums.BooleanValue";
		String elementName = "BOOL_NONE";	
		int value = xaEnumUtil.getValue(enumName, elementName);
		Assert.assertEquals(0, value);
	}
	
	@Test
	public void testgetValue() {
		testGetEnum();
		String enumName = "CommonEnums.ActivationStatus";
		String elementName = "ACT_STATUS_DISABLED";	
		int value = xaEnumUtil.getValue(enumName, elementName);
		Assert.assertEquals(0, value);
	}
}