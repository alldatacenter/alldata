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

import org.apache.ambari.view.validation.ValidationResult;
import org.junit.Test;

import junit.framework.Assert;

/**
 * ValidationResultImpl tests.
 */
public class ValidationResultImplTest {

  @Test
  public void testIsValid() throws Exception {
    ValidationResult result = new ValidationResultImpl(true, "detail");
    Assert.assertTrue(result.isValid());

    result = new ValidationResultImpl(false, "detail");
    Assert.assertFalse(result.isValid());
  }

  @Test
  public void testGetDetail() throws Exception {
    ValidationResult result = new ValidationResultImpl(true, "detail");
    Assert.assertEquals("detail", result.getDetail());
  }

  @Test
  public void testCreate() throws Exception {
    ValidationResult result = ValidationResultImpl.create(new ValidationResultImpl(true, "is true"));
    Assert.assertTrue(result.isValid());
    Assert.assertEquals("is true", result.getDetail());

    result = ValidationResultImpl.create(new ValidationResultImpl(false, "is false"));
    Assert.assertFalse(result.isValid());
    Assert.assertEquals("is false", result.getDetail());
  }
}
