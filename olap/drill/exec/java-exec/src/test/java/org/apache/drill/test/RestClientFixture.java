/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.test;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.exec.server.rest.StatusResources;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.util.List;

/**
 * Represents a client for the Drill Rest API.
 */
public class RestClientFixture implements AutoCloseable {
  /**
   * A builder for the rest client.
   */
  public static class Builder {
    private ClusterFixture cluster;

    public Builder(ClusterFixture cluster) {
      this.cluster = Preconditions.checkNotNull(cluster);
    }

    public RestClientFixture build() {
      return new RestClientFixture(cluster);
    }
  }

  private final WebTarget baseTarget;
  private final Client client;

  private RestClientFixture(ClusterFixture cluster) {
    int port = cluster.drillbit().getWebServerPort();
    String address = cluster.drillbits().iterator().next().getContext().getEndpoint().getAddress();
    String baseURL = "http://" + address + ":" + port;

    ClientConfig cc = new ClientConfig();
    cc.register(JacksonJsonProvider.class);
    client = JerseyClientBuilder.createClient(cc);
    baseTarget = client.target(baseURL);
  }

  /**
   * Gets all the external options from the rest api.
   * @return All the external options
   */
  public List<StatusResources.OptionWrapper> getStatusOptions() {
    return baseTarget.path(StatusResources.PATH_OPTIONS_JSON)
      .request(MediaType.APPLICATION_JSON)
      .get(new GenericType<List<StatusResources.OptionWrapper>>() {});
  }

  public List<StatusResources.OptionWrapper> getStatusInternalOptions() {
    return baseTarget.path(StatusResources.PATH_INTERNAL_OPTIONS_JSON)
      .request(MediaType.APPLICATION_JSON)
      .get(new GenericType<List<StatusResources.OptionWrapper>>() {});
  }

  public MultivaluedMap<String, String> getResponseHeaders(String relativeResourcePath) {
    Response response = baseTarget.path(relativeResourcePath)
        .request(MediaType.TEXT_HTML)
        .get();
    return response.getStringHeaders();
  }

  /**
   * Gets the external option corresponding to the given name.
   * @param name The name of the external option to retrieve.
   * @return The external option corresponding to the given name.
   */
  @Nullable
  public StatusResources.OptionWrapper getStatusOption(String name) {
    return getStatusOptionHelper(getStatusOptions(), name);
  }

  /**
   * Gets the internal option corresponding to the given name.
   * @param name The name of the internal option to retrieve.
   * @return The internal option corresponding to the given name.
   */
  @Nullable
  public StatusResources.OptionWrapper getStatusInternalOption(String name) {
    return getStatusOptionHelper(getStatusInternalOptions(), name);
  }

  private StatusResources.OptionWrapper getStatusOptionHelper(List<StatusResources.OptionWrapper> options,
                                                              String name) {
    for (StatusResources.OptionWrapper option: options) {
      if (option.getName().equals(name)) {
        return option;
      }
    }

    return null;
  }

  @Override
  public void close() throws Exception {
    client.close();
  }
}
