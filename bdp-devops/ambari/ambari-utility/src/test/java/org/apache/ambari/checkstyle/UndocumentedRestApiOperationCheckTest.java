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
package org.apache.ambari.checkstyle;

import static org.apache.ambari.checkstyle.UndocumentedRestApiOperationCheck.MESSAGE;

import org.junit.Test;

import com.puppycrawl.tools.checkstyle.AbstractModuleTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;

public class UndocumentedRestApiOperationCheckTest extends AbstractModuleTestSupport {

  @Override
  protected String getPackageLocation() {
    return "org/apache/ambari/checkstyle";
  }

  @Test
  public void test() throws Exception {
    final DefaultConfiguration config = createModuleConfig(UndocumentedRestApiOperationCheck.class);
    final String[] expected = {
      "36: " + MESSAGE,
      "53: " + MESSAGE,
      "70: " + MESSAGE,
      "87: " + MESSAGE,
      "104: " + MESSAGE,
      "121: " + MESSAGE
    };

    verify(config, getPath("InputRestApiOperation.java"), expected);
  }

}
