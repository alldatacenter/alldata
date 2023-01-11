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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.store;

import java.util.List;
import java.util.Map;

import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.util.SearchFilter;

public interface SecurityZoneStore {

    void init() throws Exception;

    RangerSecurityZone createSecurityZone(RangerSecurityZone securityZone) throws Exception;

    RangerSecurityZone updateSecurityZoneById(RangerSecurityZone securityZone) throws Exception;

    void deleteSecurityZoneByName(String zoneName) throws Exception;

    void deleteSecurityZoneById(Long zoneId) throws Exception;

    RangerSecurityZone getSecurityZone(Long id) throws Exception;

    RangerSecurityZone getSecurityZoneByName(String name) throws Exception;

    List<RangerSecurityZone> getSecurityZones(SearchFilter filter) throws Exception;

    Map<String, RangerSecurityZone.RangerSecurityZoneService> getSecurityZonesForService(String serviceName);

}

