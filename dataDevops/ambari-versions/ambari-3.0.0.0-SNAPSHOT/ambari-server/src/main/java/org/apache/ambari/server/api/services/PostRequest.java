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

package org.apache.ambari.server.api.services;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.handlers.CreateHandler;
import org.apache.ambari.server.api.handlers.RequestHandler;
import org.apache.ambari.server.api.resources.ResourceInstance;

/**
 * A POST request.
 */
public class PostRequest extends BaseRequest {
  /**
   * Constructor.
   *
   * @param headers     http headers
   * @param body        http body
   * @param uriInfo     uri information
   * @param resource    associated resource definition
   */
  public PostRequest(HttpHeaders headers, RequestBody body, UriInfo uriInfo, ResourceInstance resource) {
    super(headers, body, uriInfo, resource);
  }

  @Override
  public Type getRequestType() {
    return Type.POST;
  }

  @Override
  protected RequestHandler getRequestHandler() {
    return new CreateHandler();
  }
}
