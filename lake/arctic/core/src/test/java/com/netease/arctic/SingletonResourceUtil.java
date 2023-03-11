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

package com.netease.arctic;

public class SingletonResourceUtil {

  private static final String USE_SINGLETON_TEST_RESOURCE_PROPERTY = "singleton-test-resource";
  private static boolean USE_SINGLETON_TEST_RESOURCE = true;

  static {
    USE_SINGLETON_TEST_RESOURCE =
        Boolean.valueOf(System.getProperty(USE_SINGLETON_TEST_RESOURCE_PROPERTY, "true"));
  }

  public static boolean isUseSingletonResource() {
    return USE_SINGLETON_TEST_RESOURCE;
  }
}
