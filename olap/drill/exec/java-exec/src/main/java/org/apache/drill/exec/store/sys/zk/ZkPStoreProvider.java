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
package org.apache.drill.exec.store.sys.zk;

import org.apache.drill.exec.coord.zk.ZKClusterCoordinator;
import org.apache.drill.exec.exception.StoreException;
import org.apache.drill.exec.store.sys.PersistentStoreRegistry;
import org.apache.drill.exec.store.sys.store.provider.ZookeeperPersistentStoreProvider;

/**
 * Kept for possible references to old class name in configuration.
 *
 * @deprecated will be removed in 1.7
 *    use {@link ZookeeperPersistentStoreProvider} instead.
 */
public class ZkPStoreProvider extends ZookeeperPersistentStoreProvider {
  public ZkPStoreProvider(PersistentStoreRegistry<ZKClusterCoordinator> registry) throws StoreException {
    super(registry);
  }
}
