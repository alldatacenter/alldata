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

package org.apache.ambari.view.filebrowser;

import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.validation.ValidationResult;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

public class PropertyValidatorTest {

  @Test
  public void testValidatePropertyWithValidWebhdfsURI(){
    Map<String,String> propertyMap = new HashMap<String, String>();
    propertyMap.put(PropertyValidator.WEBHDFS_URL,"webhdfs://host:1234/");
    ViewInstanceDefinition instanceDefinition = getInstanceDef(propertyMap);

    ValidationResult result =  new PropertyValidator().validateProperty(PropertyValidator.WEBHDFS_URL,instanceDefinition,null);
    assertEquals(result, ValidationResult.SUCCESS);
    assertEquals(result.isValid(),true);
  }

  @Test
  public void testValidatePropertyWithValidHdfsURI(){
    Map<String,String> propertyMap = new HashMap<String, String>();
    propertyMap.put(PropertyValidator.WEBHDFS_URL,"hdfs://host:1234/");
    ViewInstanceDefinition instanceDefinition = getInstanceDef(propertyMap);

    ValidationResult result =  new PropertyValidator().validateProperty(PropertyValidator.WEBHDFS_URL,instanceDefinition,null);
    assertEquals(result, ValidationResult.SUCCESS);
    assertEquals(result.isValid(),true);
  }

  @Test
  public void testValidatePropertyWithLocalFileURI(){
    Map<String,String> propertyMap = new HashMap<String, String>();
    propertyMap.put(PropertyValidator.WEBHDFS_URL,"file:///");
    ViewInstanceDefinition instanceDefinition = getInstanceDef(propertyMap);

    ValidationResult result =  new PropertyValidator().validateProperty(PropertyValidator.WEBHDFS_URL,instanceDefinition,null);
    assertEquals(result.getClass(), PropertyValidator.InvalidPropertyValidationResult.class);
    assertEquals(result.isValid(),false);
    assertEquals(result.getDetail(),"Must be valid URL");
  }

  private ViewInstanceDefinition getInstanceDef(Map<String,String> propertyMap){
    ViewInstanceDefinition instanceDefinition = createNiceMock(ViewInstanceDefinition.class);
    expect(instanceDefinition.getPropertyMap()).andReturn(propertyMap);
    replay(instanceDefinition);
    return  instanceDefinition;
  }
}
