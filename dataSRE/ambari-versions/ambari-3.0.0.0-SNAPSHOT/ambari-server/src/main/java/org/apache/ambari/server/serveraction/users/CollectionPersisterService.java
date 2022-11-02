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

package org.apache.ambari.server.serveraction.users;

import java.util.Collection;
import java.util.Map;

/**
 * Contract defining operations that persist collections of data.
 */
public interface CollectionPersisterService<K, V> {

  /**
   * Persists the provided collection of data.
   *
   * @param collectionData the data to be persisted
   * @return true if all the records persisted successfully, false otherwise.
   */
  boolean persist(Collection<V> collectionData);


  /**
   * Persists the provided map of data.
   *
   * @param mapData the data to be persisted.
   * @return true if all the records persisted successfully, false otherwise.
   */
  boolean persistMap(Map<K, V> mapData);

}
