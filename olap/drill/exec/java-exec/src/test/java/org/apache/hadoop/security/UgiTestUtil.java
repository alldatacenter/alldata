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
package org.apache.hadoop.security;

import java.io.IOException;

/**
 * UserGroupInformation is statically initialized, and depending on the order of how tests are run, the internal state
 * maybe different, which causes tests to fail sometimes. This class exposes a static package-private method so that
 * tests that change the internal state of UserGroupInformation are able to reset the internal state after completion.
 *
 * To be used for testing purposes only.
 */
public final class UgiTestUtil {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UgiTestUtil.class);

  public static void resetUgi() {
    UserGroupInformation.reset();
    try {
      UserGroupInformation.getLoginUser(); // re-init
    } catch (IOException ignored) {
    }
  }

  private UgiTestUtil() {
  }
}
