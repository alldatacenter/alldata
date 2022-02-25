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
package org.apache.ambari.view.examples;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.AmbariStreamProvider;
import org.apache.ambari.view.URLStreamProvider;
import org.apache.ambari.view.cluster.Cluster;
import org.apache.commons.codec.binary.Base64;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.HashMap;

import java.util.Iterator;

/**
 * The AutoClusterService service.
 */
public class AutoClusterService {

  /**
   * The view context.
   */
  @Inject
  ViewContext context;
  
  /**
   * Handles: GET /.
   *
   * @param headers   http headers
   * @param ui        uri info
   *
   * @return value JSON representation
   */
  @GET
  @Path("/")
  @Produces({"text/plain", "application/json"})
  public Response getValue(@Context HttpHeaders headers, @Context UriInfo ui) {

    StringBuilder sb = new StringBuilder();
    try {
        InputStream in = null;

        //
        // do I have a cluster associated with this view instance?
        //
        Cluster c = context.getCluster();
        if (c != null) {
            AmbariStreamProvider stream = context.getAmbariStreamProvider();

            // use the cluster name and AmbariStreamProvider to access Ambari REST API
            String clusterName = c.getName();
            in = stream.readFrom("/api/v1/clusters/"+clusterName, "GET", null, null, true);
        } else {
            URLStreamProvider stream = context.getURLStreamProvider();
            
            // use the view instance configuration to access Ambari REST API
            String baseUrl = context.getProperties().get("ambari.server.url");
            String username = context.getProperties().get("ambari.server.username");
            String password = context.getProperties().get("ambari.server.password");

            HashMap<String, String> hds = new HashMap<String, String>();
            hds.put("X-Requested-By", "auto-cluster-view");

            String authString = username + ":" + password;
            byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
            String authStringEnc = new String(authEncBytes);

            hds.put("Authorization", "Basic " + authStringEnc);

            in = stream.readFrom(baseUrl, "GET", null, hds);
        }
        
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        String str = null;
        while ((str = r.readLine()) != null) {
            sb.append(str);
        }
        
    } catch (java.io.IOException ioe) {
        ioe.printStackTrace(); //!!!
    }
        
    return Response.ok(sb.toString()).build();
  }
    
} // end AutoClusterService
