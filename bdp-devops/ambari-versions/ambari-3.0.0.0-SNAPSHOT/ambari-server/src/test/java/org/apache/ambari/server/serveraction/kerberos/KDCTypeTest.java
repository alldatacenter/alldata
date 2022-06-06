/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.serveraction.kerberos;

import org.junit.Test;

import junit.framework.Assert;

public class KDCTypeTest {

  @Test
  public void testTranslateExact() {
    Assert.assertEquals(KDCType.MIT_KDC, KDCType.translate("MIT_KDC"));
  }

  @Test
  public void testTranslateCaseInsensitive() {
    Assert.assertEquals(KDCType.MIT_KDC, KDCType.translate("mit_kdc"));
  }

  @Test
  public void testTranslateHyphen() {
    Assert.assertEquals(KDCType.MIT_KDC, KDCType.translate("MIT-KDC"));
  }

  @Test
  public void testTranslateNull() {
    Assert.assertEquals(KDCType.NONE, KDCType.translate(null));
  }

  @Test
  public void testTranslateEmptyString() {
    Assert.assertEquals(KDCType.NONE, KDCType.translate(""));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTranslateNoTranslation() {
    KDCType.translate("NO TRANSLATION");
  }
}
