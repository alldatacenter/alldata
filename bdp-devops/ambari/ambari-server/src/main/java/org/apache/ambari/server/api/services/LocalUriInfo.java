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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Internal {@link UriInfo} implementation. Most of the methods are not
 * currently supported.
 * 
 * Can be used for internal REST API calls with avoiding REST layer.
 */
public class LocalUriInfo implements UriInfo {

  private final URI uri;

  public LocalUriInfo(String uri) {
    try {
      this.uri = new URI(uri);
    } catch (URISyntaxException e) {
      throw new RuntimeException("URI syntax is not correct", e);
    }
  }

  @Override
  public URI getAbsolutePath() {
    throw new UnsupportedOperationException("Method is not supported");
  }

  @Override
  public UriBuilder getAbsolutePathBuilder() {
    throw new UnsupportedOperationException("Method is not supported");
  }

  @Override
  public URI getBaseUri() {
    throw new UnsupportedOperationException("Method is not supported");
  }

  @Override
  public UriBuilder getBaseUriBuilder() {
    throw new UnsupportedOperationException("Method is not supported");
  }

  @Override
  public List<Object> getMatchedResources() {
    throw new UnsupportedOperationException("Method is not supported");
  }

  @Override
  public List<String> getMatchedURIs() {
    throw new UnsupportedOperationException("Method is not supported");
  }

  @Override
  public List<String> getMatchedURIs(boolean arg0) {
    throw new UnsupportedOperationException("Method is not supported");
  }

  @Override
  public String getPath() {
    throw new UnsupportedOperationException("Method is not supported");
  }

  @Override
  public String getPath(boolean arg0) {
    throw new UnsupportedOperationException("Method is not supported");
  }

  @Override
  public MultivaluedMap<String, String> getPathParameters() {
    throw new UnsupportedOperationException("Method is not supported");
  }

  @Override
  public MultivaluedMap<String, String> getPathParameters(boolean arg0) {
    throw new UnsupportedOperationException("Method is not supported");
  }

  @Override
  public List<PathSegment> getPathSegments() {
    throw new UnsupportedOperationException("Method is not supported");
  }

  @Override
  public List<PathSegment> getPathSegments(boolean arg0) {
    throw new UnsupportedOperationException("Method is not supported");
  }

  @Override
  public MultivaluedMap<String, String> getQueryParameters() {
    List<NameValuePair> parametersList = URLEncodedUtils.parse(uri, "UTF-8");

    MultivaluedMap<String, String> parameters = new MultivaluedMapImpl();
    for (NameValuePair pair : parametersList) {
      parameters.add(pair.getName(), pair.getValue());
    }
    return parameters;
  }

  @Override
  public MultivaluedMap<String, String> getQueryParameters(boolean arg0) {
    throw new UnsupportedOperationException("Method is not supported");
  }

  @Override
  public URI getRequestUri() {
    return uri;
  }

  @Override
  public UriBuilder getRequestUriBuilder() {
    throw new UnsupportedOperationException("Method is not supported");
  }

}
