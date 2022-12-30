/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.conversion.hive.extractor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StringTextTest {

  private final String content = "test_text";
  private StringText stringText;

  @Before
  public void init() {
    stringText = new StringText(content);
  }

  @Test
  public void testBytes() {
    Assert.assertArrayEquals(content.getBytes(), stringText.copyBytes());
    Assert.assertArrayEquals(content.getBytes(), stringText.getBytes());
    Assert.assertEquals(content.getBytes().length, stringText.getLength());
  }

  @Test
  public void testCharAt() {
    Assert.assertEquals('_', stringText.charAt(4));
  }

  @Test
  public void testFind() {
    Assert.assertEquals(5, stringText.find("text"));
  }

  @Test
  public void testClearAndAppend() {
    stringText.clear();
    stringText.append("hello".getBytes(), 0, "hello".getBytes().length);
    Assert.assertTrue(new String(stringText.getBytes()).startsWith("hello"));
  }
}
