/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master.metamanage.metastore;

import org.apache.inlong.tubemq.server.Server;
import org.apache.inlong.tubemq.server.master.bdbstore.MasterGroupStatus;
import org.apache.inlong.tubemq.server.master.web.model.ClusterGroupVO;

public interface KeepAliveService extends Server {

    /**
     * Whether this node is the Master role now
     *
     * @return true if is Master role or else
     */
    boolean isMasterNow();

    /**
     * Get the timestamp when the current node becomes the Master role
     *
     * @return the since time
     */
    long getMasterSinceTime();

    /**
     * Get current Master address
     *
     * @return  the current Master address
     */
    String getMasterAddress();

    /**
     * Whether the Master node in active
     *
     * @return  true for Master, false for Slave
     */
    boolean isPrimaryNodeActive();

    /**
     * Transfer Master role to other replica node
     *
     * @throws Exception the exception information
     */
    void transferMaster() throws Exception;

    /**
     * Get group address info
     *
     * @return  the group address information
     */
    ClusterGroupVO getGroupAddressStrInfo();

    /**
     * Get Master group status
     *
     * @param isFromHeartbeat   whether called by hb thread
     * @return    the master group status
     */
    MasterGroupStatus getMasterGroupStatus(boolean isFromHeartbeat);
}
