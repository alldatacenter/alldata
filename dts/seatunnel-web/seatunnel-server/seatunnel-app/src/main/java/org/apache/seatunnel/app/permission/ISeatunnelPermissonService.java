/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.app.permission;

import java.util.List;

public interface ISeatunnelPermissonService {

    /**
     * func permission check
     *
     * @param permissionKey permissionKey
     * @param userId userId
     */
    void funcPermissionCheck(String permissionKey, int userId);

    /**
     * func permission and resource permission check
     *
     * @param permissionKey permissionKey
     * @param resourceType resourceType
     * @param resourceCodes resourceCodes
     * @param userId userId
     */
    void funcAndResourcePermissionCheck(
            String permissionKey, String resourceType, List<Object> resourceCodes, int userId);

    /**
     * resource post handle
     *
     * @param resourceType resourceType
     * @param resourceCodes resourceCodes
     * @param userId userId
     */
    void resourcePostHandle(String resourceType, List<Object> resourceCodes, int userId);

    /**
     * available resource range
     *
     * @param resourceType resourceType
     * @param userId userId
     * @return list
     */
    List<Object> availableResourceRange(String resourceType, int userId);
}
