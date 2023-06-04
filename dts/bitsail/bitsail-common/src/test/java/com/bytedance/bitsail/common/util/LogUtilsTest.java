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

package com.bytedance.bitsail.common.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created 2020/12/23.
 */
public class LogUtilsTest {

  private static String generateLog(int length) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < length; i++) {
      stringBuilder.append("a");
    }
    return stringBuilder.toString();
  }

  @Test
  public void testLogCut() {
    String length100Str = generateLog(100);
    Assert.assertEquals(length100Str, LogUtils.logCut(length100Str, 500));

    String length500Str = generateLog(500);
    Assert.assertEquals(length500Str, LogUtils.logCut(length500Str, 500));

    String length501Str = generateLog(501);
    String length501StrCut = LogUtils.logCut(length501Str, 500);
    Assert.assertNotEquals(length501Str, length501StrCut);
    Assert.assertEquals(StringUtils.length(length501StrCut), 500);
  }

}