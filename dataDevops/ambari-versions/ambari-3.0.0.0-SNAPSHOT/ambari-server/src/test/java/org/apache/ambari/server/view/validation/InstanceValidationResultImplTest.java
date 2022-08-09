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

package org.apache.ambari.server.view.validation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ambari.view.validation.ValidationResult;
import org.junit.Test;

import junit.framework.Assert;

/**
 * InstanceValidationResultImpl tests.
 */
public class InstanceValidationResultImplTest {

  @Test
  public void testGetPropertyResults() throws Exception {
    ValidationResult result = new ValidationResultImpl(true, "detail");
    Map<String, ValidationResult> propertyResults = new HashMap<>();

    propertyResults.put("foo", new ValidationResultImpl(true, "foo detail"));
    propertyResults.put("bar", new ValidationResultImpl(false, "bar detail"));

    InstanceValidationResultImpl instanceValidationResult = new InstanceValidationResultImpl(result, propertyResults);

    propertyResults = instanceValidationResult.getPropertyResults();

    Assert.assertEquals(2, propertyResults.size());
    Assert.assertTrue(propertyResults.containsKey("foo"));
    Assert.assertTrue(propertyResults.containsKey("bar"));

    Assert.assertTrue(propertyResults.get("foo").isValid());
    Assert.assertFalse(propertyResults.get("bar").isValid());
  }

  @Test
  public void testToJson() throws Exception {
    ValidationResult result = new ValidationResultImpl(true, "detail");
    Map<String, ValidationResult> propertyResults = new LinkedHashMap<>();

    propertyResults.put("foo", new ValidationResultImpl(true, "foo detail"));
    propertyResults.put("bar", new ValidationResultImpl(false, "bar detail"));

    InstanceValidationResultImpl instanceValidationResult = new InstanceValidationResultImpl(result, propertyResults);

    Assert.assertEquals("{\"propertyResults\":{\"foo\":{\"valid\":true,\"detail\":\"foo detail\"},\"bar\":{\"valid\":false,\"detail\":\"bar detail\"}},\"valid\":false,\"detail\":\"The instance has invalid properties.\"}",
        instanceValidationResult.toJson());
  }
}
