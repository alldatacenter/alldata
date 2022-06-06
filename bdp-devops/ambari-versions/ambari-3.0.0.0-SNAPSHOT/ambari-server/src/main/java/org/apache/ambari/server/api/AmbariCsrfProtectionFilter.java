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
package org.apache.ambari.server.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.api.services.serializers.JsonSerializer;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

public class AmbariCsrfProtectionFilter implements ContainerRequestFilter {
  private static final Set<String> IGNORED_METHODS;
  private static final String CSRF_HEADER = "X-Requested-By";
  private static final String ERROR_MESSAGE = "CSRF protection is turned on. " + CSRF_HEADER + " HTTP header is required.";
  private static final JsonSerializer JSON_SERIALIZER = new JsonSerializer();

  static {
    HashSet<String> methods = new HashSet<>();
    methods.add("GET");
    methods.add("OPTIONS");
    methods.add("HEAD");

    IGNORED_METHODS = Collections.unmodifiableSet(methods);

  }

  @Override
  public ContainerRequest filter(ContainerRequest containerRequest) {
    if (!IGNORED_METHODS.contains(containerRequest.getMethod()) &&
            !containerRequest.getRequestHeaders().containsKey(CSRF_HEADER)) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(
              JSON_SERIALIZER.serializeError(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, ERROR_MESSAGE))
      ).type(MediaType.TEXT_PLAIN_TYPE).build());
    }
    return containerRequest;
  }
}
