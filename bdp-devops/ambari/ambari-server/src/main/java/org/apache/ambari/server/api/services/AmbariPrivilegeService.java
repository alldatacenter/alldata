/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing privileges and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services;

import java.util.Collections;

import javax.ws.rs.Path;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

/**
 *  Service responsible for Ambari privilege resource requests.
 */
@Path("/privileges/")
public class AmbariPrivilegeService extends PrivilegeService {

  // ----- PrivilegeService --------------------------------------------------

  @Override
  protected ResourceInstance createPrivilegeResource(String privilegeId) {
    return createResource(Resource.Type.AmbariPrivilege,
        Collections.singletonMap(Resource.Type.AmbariPrivilege, privilegeId));
  }
}
