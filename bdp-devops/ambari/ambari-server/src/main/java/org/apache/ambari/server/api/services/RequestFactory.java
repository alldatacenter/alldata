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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;

/**
 * Factory for {@link Request} instances.
 */
public class RequestFactory {
  /**
   * Create a request instance.
   *
   * @param headers      http headers
   * @param uriInfo      uri information
   * @param requestType  http request type
   * @param resource     associated resource instance
   *
   * @return a new Request instance
   */
  public Request createRequest(HttpHeaders headers, RequestBody body, UriInfo uriInfo, Request.Type requestType,
                               ResourceInstance resource) {
    switch (requestType) {
      case GET:
        return createGetRequest(headers, body, uriInfo, resource);
      case PUT:
        return createPutRequest(headers, body, uriInfo, resource);
      case DELETE:
        return createDeleteRequest(headers, body, uriInfo, resource);
      case POST:
        return createPostRequest(headers, body, uriInfo, resource);
      default:
        throw new IllegalArgumentException("Invalid request type: " + requestType);
    }
  }

  /**
   * Create a GET request.  This will apply any eligible directives supplied in the URI.
   *
   * @param headers  http headers
   * @param uriInfo  uri information
   * @param resource associated resource instance
   * @return new post request
   */
  private Request createGetRequest(HttpHeaders headers, RequestBody body, UriInfo uriInfo, ResourceInstance resource) {
    applyDirectives(Request.Type.GET, body, uriInfo, resource);
    return new GetRequest(headers, body, uriInfo, resource);
  }

  /**
   * Create a POST request.  This will either be a standard post request or a query post request.
   * A query post request first applies a query to a collection resource and then creates
   * sub-resources to all matches of the predicate.
   *
   * @param headers   http headers
   * @param uriInfo   uri information
   * @param resource  associated resource instance
   *
   * @return new post request
   */
  private Request createPostRequest(HttpHeaders headers, RequestBody body, UriInfo uriInfo, ResourceInstance resource) {
    boolean batchCreate = !applyDirectives(Request.Type.POST, body, uriInfo, resource);;

    return (batchCreate) ?
        new QueryPostRequest(headers, body, uriInfo, resource) :
        new PostRequest(headers, body, uriInfo, resource);
  }

  /**
   * Creates a PUT request. It will apply any eligible directives supplied in the URI
   */
  private Request createPutRequest(HttpHeaders headers, RequestBody body, UriInfo uriInfo, ResourceInstance resource) {
    applyDirectives(Request.Type.PUT, body, uriInfo, resource);
    return new PutRequest(headers, body, uriInfo, resource);
  }

  /**
   * Creates a DELETE request. It will apply any eligible directives supplied in the URI
   */
  private DeleteRequest createDeleteRequest(HttpHeaders headers, RequestBody body, UriInfo uriInfo, ResourceInstance resource) {
    applyDirectives(Request.Type.DELETE, body, uriInfo, resource);
    return new DeleteRequest(headers, body, uriInfo, resource);
  }

  /**
   * Gather query parameters from uri and body query string.
   *
   * @param uriInfo  contains uri info
   * @param body     request body
   *
   * @return map of query parameters or an empty map if no parameters are present
   */
  private Map<String, String> getQueryParameters(UriInfo uriInfo, RequestBody body) {
    Map<String, String> queryParameters = new HashMap<>();
    for (Map.Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet()) {
      queryParameters.put(entry.getKey(), entry.getValue().get(0));
    }

    String bodyQueryString = body.getQueryString();
    if (bodyQueryString != null && ! bodyQueryString.isEmpty()) {
      String[] toks = bodyQueryString.split("&");
      for (String tok : toks) {
        String[] keyVal = tok.split("=");
        queryParameters.put(keyVal[0], keyVal.length == 2 ? keyVal[1] : "");
      }
    }
    return queryParameters;
  }

  /**
   * Applies directives and determines if a query predicate exists or not.
   * <p/>
   * Depending on the request type (POST, PUT, etc...), retrieves the appropriate set of directives:
   * <ul>
   * <li><code>POST</code> - {@link org.apache.ambari.server.api.resources.ResourceDefinition#getCreateDirectives()}</li>
   * <li><code>PUT</code> - {@link org.apache.ambari.server.api.resources.ResourceDefinition#getUpdateDirectives()}</li>
   * </ul>
   * <p/>
   * Note: Only <code>POST</code> and <code>PUT</code> are supported.
   * <p/>
   * Iterates through the query parameters adding those that are known to be directives to the map
   * of request info properties from {@link RequestBody#getRequestInfoProperties()}.
   * <p/>
   * If a query property is found that is not a directive, a query predicate exists and
   * <code>false</code> is returned; else all query parameters are directives, leaving no query predicate,
   * and <code>true</code> is returned.
   *
   * @return true if all the directives are supported; false if at least one directive is not supported
   */
  private boolean applyDirectives(Request.Type requestType, RequestBody body, UriInfo uriInfo, ResourceInstance resource) {
    Map<String, String> queryParameters = getQueryParameters(uriInfo, body);
    Map<String, String> requestInfoProperties;
    boolean allDirectivesApplicable = true;
    if (!queryParameters.isEmpty()) {
      ResourceDefinition resourceDefinition = resource.getResourceDefinition();
      Collection<String> directives;
      switch (requestType) {
        case PUT:
          directives = resourceDefinition.getUpdateDirectives();
          break;
        case POST:
          directives = resourceDefinition.getCreateDirectives();
          break;
        case GET:
          directives = resourceDefinition.getReadDirectives();
          break;
        case DELETE:
          directives = resourceDefinition.getDeleteDirectives();
          break;
        default:
          // not yet implemented for other types
          return false;
      }
      requestInfoProperties = body.getRequestInfoProperties();
      for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
        if (directives.contains(entry.getKey())) {
          requestInfoProperties.put(entry.getKey(), entry.getValue());
        }
        else {
          allDirectivesApplicable = false;
        }
      }
    }
    return allDirectivesApplicable;
  }
}
