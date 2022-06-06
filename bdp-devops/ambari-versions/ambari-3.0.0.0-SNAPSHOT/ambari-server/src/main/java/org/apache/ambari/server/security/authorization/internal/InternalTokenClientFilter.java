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

package org.apache.ambari.server.security.authorization.internal;

import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

public class InternalTokenClientFilter extends ClientFilter {
  public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
  private final InternalTokenStorage tokenStorage;

  @Inject
  public InternalTokenClientFilter(InternalTokenStorage tokenStorage) {
    this.tokenStorage = tokenStorage;
  }

  @Override
  public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
    cr.getHeaders().add(INTERNAL_TOKEN_HEADER, tokenStorage.getInternalToken());
    return getNext().handle(cr);
  }
}
