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

import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;

import java.util.List;

/**
 * ZKDefaultACLProvider provides the ACLs for znodes created in a un-secure installation.
 *
 * If this ACLProvider is used, everyone gets unrestricted access to the Drill znodes
 *
 */
@ZKACLProviderTemplate(type = "open")
public class ZKDefaultACLProvider implements ZKACLProvider {

    public ZKDefaultACLProvider(ZKACLContextProvider context) { }

    public List<ACL> getDrillDefaultAcl() {
        return ZooDefs.Ids.OPEN_ACL_UNSAFE;
    }

    public List<ACL> getDrillAclForPath(String path) {
        return ZooDefs.Ids.OPEN_ACL_UNSAFE;
    }

}
