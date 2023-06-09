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
package org.apache.drill.exec.planner.index;

import org.apache.drill.exec.planner.logical.DrillTable;


/**
 * SchemaFactory of a storage plugin that can used to store index tables should expose this interface to allow
 * IndexDiscovers discovering the index table without adding dependency to the storage plugin.
 */
public interface IndexDiscoverable {

  /**
   * return the found DrillTable with path (e.g. names={"elasticsearch", "staffidx", "stjson"})
   * @param discover
   * @param desc
   * @return
   */
    DrillTable findTable(IndexDiscover discover, DrillIndexDescriptor desc);

}
