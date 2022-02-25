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

package org.apache.ambari.server.audit.request;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.controller.spi.Resource;

public class AllPostAndPutCreator extends AbstractBaseCreator {

  @Override
  public Set<Request.Type> getRequestTypes() {
    return new HashSet<>(Arrays.asList(Request.Type.POST, Request.Type.PUT));
  }

  @Override
  public Set<Resource.Type> getResourceTypes() {
    return null;
  }

  @Override
  public Set<ResultStatus.STATUS> getResultStatuses() {
    return null;
  }
}
