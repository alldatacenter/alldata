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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeatunnelPermissionServiceImpl implements ISeatunnelPermissonService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SeatunnelPermissionServiceImpl.class);

    @Autowired private AvailableResourceRangeService availableResourceRangeService;

    @Override
    public void funcPermissionCheck(String permissionKey, int userId) {
        // user id will be replaced by shiro in ws when user id == 0
        LOGGER.warn("func permission check in whaletunnel");
    }

    @Override
    public void funcAndResourcePermissionCheck(
            String permissionKey, String sourceType, List<Object> sourceCodes, int userId) {
        // user id will be replaced by shiro in ws when user id == 0
        LOGGER.warn("func and resource permission check in seatunnel");
    }

    @Override
    public void resourcePostHandle(String sourceType, List<Object> sourceCodes, int userId) {
        // user id will be replaced by shiro in ws when user id == 0
        LOGGER.warn("resource post handle in seatunnel");
    }

    @Override
    public List<Object> availableResourceRange(String sourceType, int userId) {
        // user id will be replaced by shiro in ws when user id == 0
        LOGGER.warn("query available resource range in seatunnel");
        return availableResourceRangeService.queryAvailableResourceRangeBySourceType(
                sourceType, userId);
    }
}
