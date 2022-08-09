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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller;

import org.apache.ambari.server.controller.internal.RequestResourceProvider;

import io.swagger.annotations.ApiModelProperty;

/**
 * Request schema for endpoint {@link org.apache.ambari.server.api.services.RequestService#updateRequests(String,
 *    javax.ws.rs.core.HttpHeaders, javax.ws.rs.core.UriInfo, String)}
 *
 * The interface is not actually implemented, it only carries swagger annotations.
 */
public interface RequestPutRequest extends ApiModel {
  @ApiModelProperty(name = RequestResourceProvider.REQUESTS)
  RequestRequest getRequestRequest();
}
