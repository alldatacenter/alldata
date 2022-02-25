/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Property provider for host component resources that is used to read HTTP data from another server.
 */
public class HttpPropertyProvider extends BaseProvider implements PropertyProvider {

  private static final Logger LOG = LoggerFactory.getLogger(HttpPropertyProvider.class);

  private final StreamProvider streamProvider;
  private final String clusterNamePropertyId;
  private final String hostNamePropertyId;
  private final String publicHostNamePropertyId;
  private final String componentNamePropertyId;
  private final Clusters clusters;
  private final Map<String, List<HttpPropertyRequest>> httpPropertyRequests;


  // ----- Constructors ------------------------------------------------------

  public HttpPropertyProvider(
      StreamProvider stream,
      Clusters clusters,
      String clusterNamePropertyId,
      String hostNamePropertyId,
      String publicHostNamePropertyId,
      String componentNamePropertyId,
      Map<String, List<HttpPropertyRequest>> httpPropertyRequests) {

    super(getSupportedProperties(httpPropertyRequests));
    this.streamProvider = stream;
    this.clusterNamePropertyId = clusterNamePropertyId;
    this.hostNamePropertyId = hostNamePropertyId;
    this.publicHostNamePropertyId = publicHostNamePropertyId;
    this.componentNamePropertyId = componentNamePropertyId;
    this.clusters = clusters;
    this.httpPropertyRequests = httpPropertyRequests;
  }


  // ----- PropertyProvider --------------------------------------------------

  // get the complete set of Ambari properties that can be set by this property provider.
  private static Set<String> getSupportedProperties(Map<String, List<HttpPropertyRequest>> httpPropertyRequests) {
    Set<String> supportedProperties = new HashSet<>();

    for (List<HttpPropertyRequest> httpPropertyRequestList : httpPropertyRequests.values()) {
      for (HttpPropertyRequest httpPropertyRequest : httpPropertyRequestList) {
        supportedProperties.addAll(httpPropertyRequest.getSupportedProperties());
      }
    }
    return Collections.unmodifiableSet(supportedProperties);
  }


  // ----- helper methods ----------------------------------------------------

  @Override
  public Set<Resource> populateResources(Set<Resource> resources,
                                         Request request, Predicate predicate) throws SystemException {

    Set<String> ids = getRequestPropertyIds(request, predicate);

    if (ids.size() == 0) {
      return resources;
    }

    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);
      String hostName = (String) resource.getPropertyValue(hostNamePropertyId);
      String publicHostName = (String) resource.getPropertyValue(publicHostNamePropertyId);
      String componentName = (String) resource.getPropertyValue(componentNamePropertyId);

      if (clusterName != null && hostName != null && componentName != null &&
          httpPropertyRequests.containsKey(componentName)) {

        try {
          Cluster cluster = clusters.getCluster(clusterName);

          List<HttpPropertyRequest> httpPropertyRequestList = httpPropertyRequests.get(componentName);

          for (HttpPropertyRequest httpPropertyRequest : httpPropertyRequestList) {
            populateResource(httpPropertyRequest, resource, cluster, hostName, publicHostName);
          }
        } catch (AmbariException e) {
          String msg = String.format("Could not load cluster with name %s.", clusterName);
          LOG.debug(msg, e);
          throw new SystemException(msg, e);
        }
      }
    }
    return resources;
  }

  // populate the given resource from the given HTTP property request.
  private void populateResource(HttpPropertyRequest httpPropertyRequest, Resource resource,
                                Cluster cluster, String hostName, String publicHostName) throws SystemException {

    String url = httpPropertyRequest.getUrl(cluster, hostName);

    try {
      InputStream inputStream = streamProvider.readFrom(url);

      try {
        httpPropertyRequest.populateResource(resource, inputStream);
      } finally {
        try {
          inputStream.close();
        } catch (IOException ioe) {
          LOG.error(String.format("Error closing HTTP response stream %s", url), ioe);
        }
      }
    } catch (Exception e) {
      LOG.debug(String.format("Error reading HTTP response from %s", url), e);
      if(publicHostName != null && !publicHostName.equalsIgnoreCase(hostName)) {
        String publicUrl = httpPropertyRequest.getUrl(cluster, publicHostName);
        LOG.debug(String.format("Retry using public host name url %s", publicUrl));
        try {
          InputStream inputStream = streamProvider.readFrom(publicUrl);

          try {
            httpPropertyRequest.populateResource(resource, inputStream);
          } finally {
            try {
              inputStream.close();
            } catch (IOException ioe) {
              LOG.error(String.format("Error closing HTTP response stream %s", url), ioe);
            }
          }
        } catch (Exception ex) {
          LOG.debug(String.format("Error reading HTTP response from public host name url %s", url), ex);
        }
      }
    }
  }


  // ----- inner class : HttpPropertyRequest ---------------------------------

  /**
   * Represents an HTTP request to another server for properties to be
   * used to populate an Ambari resource.
   */
  public static abstract class HttpPropertyRequest {

    private final Map<String, String> propertyMappings;


    // ----- Constructors ----------------------------------------------------

    protected HttpPropertyRequest(Map<String, String> propertyMappings) {
      this.propertyMappings = propertyMappings;
    }


    // ----- PropertyRequest -------------------------------------------------

    /**
     * Get the names of the Ambari properties that can be set by this HTTP property request.
     *
     * @return the supported property names
     */
    public Collection<String> getSupportedProperties() {
      return propertyMappings.values();
    }

    /**
     * Get the property name mappings from source property to Ambari property.
     *
     * @return the source to Ambari property name mappings
     */
    protected Map<String, String> getPropertyMappings() {
      return propertyMappings;
    }

    /**
     * Get the URL used to make the HTTP request.
     *
     * @param cluster        the cluster of the resource being populated
     * @param hostName       the host name of the resource being populated
     * @return the URL to make the HTTP request
     *
     * @throws SystemException if the URL can not be obtained
     */
    public abstract String getUrl(Cluster cluster, String hostName) throws SystemException;

    /**
     * Populate the given resource from the given input stream.
     *
     * @param resource     the Ambari resource to populate
     * @param inputStream  the input stream from the HTTP request
     *
     * @throws SystemException  if the resource can not be populated
     */
    public abstract void populateResource(Resource resource, InputStream inputStream) throws SystemException;
  }
}
