/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller;

import java.util.Map;

import org.apache.ambari.server.controller.internal.ArtifactResourceProvider;

import io.swagger.annotations.ApiModelProperty;

/**
 * Request schema for endpoint {@link org.apache.ambari.server.api.services.ClusterService#createClusterArtifact(String,
 * javax.ws.rs.core.HttpHeaders, javax.ws.rs.core.UriInfo, String, String)}
 *
 * The interface is not actually implemented, it only carries swagger annotations.
 */
@SuppressWarnings("unused")
public interface ClusterArtifactRequest extends ApiModel {

  @ApiModelProperty(name = ArtifactResourceProvider.RESPONSE_KEY)
  ClusterArtifactRequestInfo getClusterArtifactRequestInfo();

  @ApiModelProperty(name = ArtifactResourceProvider.ARTIFACT_DATA_PROPERTY)
  Map<String, Object> getArtifactData();

  interface ClusterArtifactRequestInfo {
    @ApiModelProperty(name = ArtifactResourceProvider.ARTIFACT_NAME)
    String getArtifactName();
  }

}
