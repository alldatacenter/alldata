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

import org.apache.zookeeper.data.ACL;

import java.util.List;

/**
 * This class defines the methods that are required to specify
 * ACLs on Drill ZK nodes
 */

public interface ZKACLProvider {

  /**
   * Returns the list of ZK ACLs {@link ACL} to apply by default
   * on Drill znodes
   * @return  List of ZK ACLs {@link ACL}
   */
  List<ACL> getDrillDefaultAcl();

  /**
   * Returns the list of ZK ACLs {@link ACL} to apply for a specific
   * Drill znode
   * @param path The path for which the ACL is being looked up
   * @return List of ZK ACLs {@link ACL} for the provided path
   */
  List<ACL> getDrillAclForPath(String path);

}
