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
package org.apache.drill.test;

import org.apache.drill.common.util.GuavaPatcher;
import org.apache.drill.common.util.ProtobufPatcher;

/**
 * Contains patchers that must be executed at the very beginning of test runs.
 * All Drill test classes should be inherited from it to avoid exceptions (e.g. NoSuchMethodError etc.).
 */
public class BaseTest {

  static {
    /*
     * HBase and MapR-DB clients use older version of protobuf,
     * and override some methods that became final in recent versions.
     * This code removes these final modifiers.
     */
    ProtobufPatcher.patch();
    /*
     * Some libraries, such as Hadoop, HBase, Iceberg depend on incompatible versions of Guava.
     * This code adds back some methods to so that the libraries can work with single Guava version.
     */
    GuavaPatcher.patch();
  }
}
