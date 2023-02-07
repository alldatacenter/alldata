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
package org.apache.drill.exec.coord.zk;

import org.apache.curator.framework.api.ACLProvider;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.zookeeper.data.ACL;

import java.util.List;

/**
 * This class hides the {@link ZKACLProvider} from Curator-specific functions
 *
 * This is done so that ACL Providers have to be aware only about ZK ACLs and the Drill {@link ZKACLProvider} interface
 * ACL Providers should not be concerned with the framework (Curator) used by Drill to access ZK.
 * If Drill stops using Curator, then existing {@link ZKACLProvider} implementations will still work.
 */

public class ZKACLProviderDelegate implements ACLProvider {
  @VisibleForTesting
  ZKACLProvider aclProvider;

  public ZKACLProviderDelegate(ZKACLProvider aclProvider) {
    this.aclProvider = aclProvider;
  }

  @Override
  final public List<ACL> getDefaultAcl() {
    return aclProvider.getDrillDefaultAcl();
  }

  @Override
  final public List<ACL> getAclForPath(String path) {
    return aclProvider.getDrillAclForPath(path);
  }

}