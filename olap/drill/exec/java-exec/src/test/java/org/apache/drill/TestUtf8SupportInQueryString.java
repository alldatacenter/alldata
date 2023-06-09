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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill;

import mockit.Mock;
import mockit.MockUp;
import org.apache.calcite.util.Util;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.test.BaseTestQuery;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestUtf8SupportInQueryString extends BaseTestQuery {

  @Test
  public void testUtf8SupportInQueryStringByDefault() throws Exception {
    // can be defined in saffron.properties file present in classpath or system property
    testBuilder()
        .sqlQuery("select 'привет' as hello from (values(1))")
        .unOrdered()
        .baselineColumns("hello")
        .baselineValues("привет")
        .go();
  }

  @Test(expected = UserRemoteException.class)
  public void testDisableUtf8SupportInQueryString() throws Exception {
    final String charset = "ISO-8859-1";

    // Mocked Util.getDefaultCharset() since it uses static field Util.DEFAULT_CHARSET
    // which is initialized when declared using SaffronProperties.INSTANCE field which also is initialized
    // when declared.
    new MockUp<Util>()
    {
      @Mock
      Charset getDefaultCharset() {
        return Charset.forName(charset);
      }
    };

    final String hello = "привет";
    try {
      test("values('%s')", hello);
    } catch (UserRemoteException e) {
      assertThat(e.getMessage(), containsString(
          String.format("Failed to encode '%s' in character set '%s'", hello, charset)));
      throw e;
    }
  }

}
