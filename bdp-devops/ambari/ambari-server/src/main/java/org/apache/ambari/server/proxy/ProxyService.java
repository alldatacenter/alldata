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

package org.apache.ambari.server.proxy;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.view.ImpersonatorSettingImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

@Path("/")
public class ProxyService {

  public static final int URL_CONNECT_TIMEOUT = 20000;
  public static final int URL_READ_TIMEOUT = 15000;
  public static final int HTTP_ERROR_RANGE_START = Response.Status.BAD_REQUEST.getStatusCode();

  private static final String REQUEST_TYPE_GET = "GET";
  private static final String REQUEST_TYPE_POST = "POST";
  private static final String REQUEST_TYPE_PUT = "PUT";
  private static final String REQUEST_TYPE_DELETE = "DELETE";
  private static final String QUERY_PARAMETER_URL = "url=";
  private static final String AMBARI_PROXY_PREFIX = "AmbariProxy-";
  private static final String ERROR_PROCESSING_URL = "Error occurred during processing URL ";
  private static final String INVALID_PARAM_IN_URL = "Invalid query params found in URL ";

  private final static Logger LOG = LoggerFactory.getLogger(ProxyService.class);

  @GET @ApiIgnore // until documented
  public Response processGetRequestForwarding(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(REQUEST_TYPE_GET, ui, null, headers);
  }

  @POST @ApiIgnore // until documented
  @Consumes({MediaType.WILDCARD, MediaType.TEXT_PLAIN, MediaType.TEXT_XML, MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
  public Response processPostRequestForwarding(InputStream body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(REQUEST_TYPE_POST, ui, body, headers);
  }

  @PUT @ApiIgnore // until documented
  @Consumes({MediaType.WILDCARD, MediaType.TEXT_PLAIN, MediaType.TEXT_XML, MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
  public Response processPutRequestForwarding(InputStream body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(REQUEST_TYPE_PUT, ui, body, headers);
  }

  @DELETE @ApiIgnore // until documented
  public Response processDeleteRequestForwarding(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(REQUEST_TYPE_DELETE, ui, null, headers);
  }

  private Response handleRequest(String requestType, UriInfo ui, InputStream body, HttpHeaders headers) {
    URLStreamProvider urlStreamProvider = new URLStreamProvider(URL_CONNECT_TIMEOUT,
                                                URL_READ_TIMEOUT, null, null, null);
    String query = ui.getRequestUri().getQuery();
    if (query != null && query.indexOf(QUERY_PARAMETER_URL) != -1) {
      String url = query.replaceFirst(QUERY_PARAMETER_URL, "");

      MultivaluedMap<String, String> m = ui.getQueryParameters();
      if (m.containsKey(ImpersonatorSettingImpl.DEFAULT_DO_AS_PARAM)) {              // Case doesn't matter
        LOG.error(INVALID_PARAM_IN_URL + url);
        return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).type(MediaType.TEXT_PLAIN).
            entity(INVALID_PARAM_IN_URL).build();
      }
      
      try {
        HttpURLConnection connection = urlStreamProvider.processURL(url, requestType, body, getHeaderParamsToForward(headers));
        int responseCode = connection.getResponseCode();
        InputStream resultInputStream = null;
        if (responseCode >= HTTP_ERROR_RANGE_START) {
          resultInputStream = connection.getErrorStream();
        } else {
          resultInputStream = connection.getInputStream();
        }
        String contentType = connection.getContentType();
        Response.ResponseBuilder rb = Response.status(responseCode);
        if (contentType.indexOf(APPLICATION_JSON) != -1) {
          rb.entity(new Gson().fromJson(new InputStreamReader(resultInputStream), Map.class));
        } else {
          rb.entity(resultInputStream);
        }
        return rb.type(contentType).build();
      } catch (IOException e) {
        LOG.error(ERROR_PROCESSING_URL + url, e);
        return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).type(MediaType.TEXT_PLAIN).
               entity(e.getMessage()).build();
      }
    }
    return null;
  }

  private Map<String, List<String>> getHeaderParamsToForward(HttpHeaders headers) {
    Map<String, List<String>> headerParamsToForward = new HashMap<>();
    for (String paramName: headers.getRequestHeaders().keySet()) {
      if (paramName.startsWith(AMBARI_PROXY_PREFIX)) {
        headerParamsToForward.put(paramName.replaceAll(AMBARI_PROXY_PREFIX,""), headers.getRequestHeader(paramName));
      }
    }
    return headerParamsToForward;
  }

}
