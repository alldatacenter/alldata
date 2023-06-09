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
package org.apache.drill.metastore;

import org.apache.drill.metastore.components.tables.Tables;
import org.apache.drill.metastore.components.views.Views;

/**
 * Drill Metastore interface contains methods needed to be implemented by Metastore implementations.
 *
 * Drill Metastore main goal is to read and write Metastore data from / to Metastore components:
 * tables, views, etc.
 *
 * Besides implementing {@link Metastore}, Metastore implementation must have constructor
 * which accepts {@link org.apache.drill.common.config.DrillConfig}.
 */
public interface Metastore extends AutoCloseable {

  /**
   * @return Metastore Tables component implementation
   */
  Tables tables();

  /**
   * @return Metastore Views component implementation
   */
  Views views();
}
