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
package org.apache.drill.metastore.operate;

import java.util.Collections;
import java.util.Map;

/**
 * Provides Metastore component implementation metadata,
 * including information about versioning support if any
 * and current properties applicable to the Metastore component instance.
 */
public interface Metadata {

  int UNDEFINED = -1;

  /**
   * Indicates if Metastore component supports versioning,
   * i.e. Metastore component version is changed each time write operation is executed.
   *
   * @return true if Metastore component supports versioning, false otherwise
   */
  boolean supportsVersioning();

  /**
   * Depending on Metastore component implementation, it may have version which can be used to determine
   * if anything has changed during last call to the Metastore component.
   * If Metastore component implementation, supports versioning,
   * version is changed each time Metastore component data has changed.
   * {@link #supportsVersioning()} indicates if Metastore component supports versioning.
   * If versioning is not supported, {@link #UNDEFINED} is returned.
   *
   * @return current metastore version
   */
  default long version() {
    return UNDEFINED;
  }

  /**
   * Depending on Metastore component implementation, it may have properties.
   * If Metastore component supports properties, map with properties names and values are returned,
   * otherwise empty map is returned.
   *
   * @return Metastore component implementation properties
   */
  default Map<String, String> properties() {
    return Collections.emptyMap();
  }
}
