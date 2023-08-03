/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark;

/**
 * A util class to load spark adapter via spark version.
 * This util class will move to spark3-common module if multi spark modules added.
 */
public class SparkAdapterLoader {
  private static final SparkAdapter adapter = new Spark31Adapter();

  /**
   * This method will implement as SPI if multi spark modules added.
   * @return A SparkAdapter objects
   */
  public static SparkAdapter getOrLoad() {
    return adapter;
  }
}
