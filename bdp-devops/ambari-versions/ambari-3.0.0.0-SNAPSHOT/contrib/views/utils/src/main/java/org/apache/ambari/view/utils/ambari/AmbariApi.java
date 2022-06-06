/**
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

package org.apache.ambari.view.utils.ambari;

import org.apache.ambari.view.AmbariHttpException;
import org.apache.ambari.view.URLStreamProvider;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.cluster.Cluster;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides API to Ambari. Supports both Local and Remote cluster association.
 * Also provides API to get cluster topology (determine what node contains specific service)
 * on both local and remote cluster.
 */
public class AmbariApi {

  private ViewContext context;
  private Services services;

  private String requestedBy = "views";

  public static String API_PREFIX = "/api/v1/clusters/";

  /**
   * Constructor for Ambari API based on ViewContext
   * @param context View Context
   */
  public AmbariApi(ViewContext context) {
    this.context = context;
  }

  /**
   *  Set requestedBy header
   *
   * @param requestedBy
   */
  public void setRequestedBy(String requestedBy) {
    this.requestedBy = requestedBy;
  }

  /**
   * Shortcut for GET method
   * @param path REST API path
   * @return response
   * @throws AmbariApiException
   */
  public String requestClusterAPI(String path) throws AmbariApiException, AmbariHttpException {
    return requestClusterAPI(path, "GET", null, null);
  }

  /**
   * Request to Ambari REST API for current cluster. Supports both local and remote cluster
   * @param path REST API path after cluster name e.g. /api/v1/clusters/mycluster/[method]
   * @param method HTTP method
   * @param data HTTP data
   * @param headers HTTP headers
   * @return response
   * @throws AmbariApiException IO error or not associated with cluster
   */
  public String requestClusterAPI(String path, String method, String data, Map<String, String> headers) throws AmbariApiException, AmbariHttpException {
    String response;

    try {

      if (context.getAmbariClusterStreamProvider() == null || context.getCluster() == null) {
        throw new NoClusterAssociatedException(
            "RA030 View is not associated with any cluster. No way to request Ambari.");
      }

      if(!path.startsWith("/")) path = "/" + path;

      path =  API_PREFIX + context.getCluster().getName() + path;

      InputStream inputStream = context.getAmbariClusterStreamProvider().readFrom(path, method, data, addRequestedByHeader(headers));
      response = IOUtils.toString(inputStream);
    } catch (IOException e) {
      throw new AmbariApiException("RA040 I/O error while requesting Ambari", e);
    }
    return response;
  }

  /**
   * Request to Ambari REST API. Supports both local and remote cluster
   * @param path REST API path, e.g. /api/v1/clusters/mycluster/
   * @param method HTTP method
   * @param data HTTP data
   * @param headers HTTP headers
   * @return response
   * @throws AmbariApiException IO error or not associated with cluster
   */
  public String readFromAmbari(String path, String method, String data, Map<String, String> headers) throws AmbariApiException, AmbariHttpException {
    String response;

    try {

      if (context.getAmbariClusterStreamProvider() == null) {
        throw new NoClusterAssociatedException(
            "RA060 View is not associated with any cluster. No way to request Ambari.");
      }

      InputStream inputStream = context.getAmbariClusterStreamProvider().readFrom(path, method, data, addRequestedByHeader(headers));
      response = IOUtils.toString(inputStream);
    } catch (IOException e) {
      throw new AmbariApiException("RA050 I/O error while requesting Ambari", e);
    }

    return response;
  }



  /**
   * Provides access to service-specific utilities
   * @return object with service-specific methods
   */
  public Services getServices() {
    if (services == null) {
      services = new Services(this, context);
    }
    return services;
  }

  private Map<String,String> addRequestedByHeader(Map<String,String> headers){
    if(headers == null){
      headers = new HashMap<String, String>();
    }

    headers.put("X-Requested-By",this.requestedBy);

    return headers;
  }

  /**
   * Check if view is associated with cluster
   *
   * @return isClusterAssociated
   */
  public boolean isClusterAssociated(){
    return context.getCluster() != null;
  }

}
