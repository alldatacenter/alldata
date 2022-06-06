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

package org.apache.ambari.server.api.services.stackadvisor.commands;

import java.io.File;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRunner;
import org.apache.ambari.server.api.services.stackadvisor.validations.ValidationResponse;
import org.apache.ambari.server.controller.internal.AmbariServerConfigurationHandler;
import org.apache.ambari.server.state.ServiceInfo;

/**
 * {@link StackAdvisorCommand} implementation for component-layout validation.
 */
public class ComponentLayoutValidationCommand extends StackAdvisorCommand<ValidationResponse> {

  public ComponentLayoutValidationCommand(File recommendationsDir,
                                          String recommendationsArtifactsLifetime,
                                          ServiceInfo.ServiceAdvisorType serviceAdvisorType,
                                          int requestId,
                                          StackAdvisorRunner saRunner,
                                          AmbariMetaInfo metaInfo,
                                          AmbariServerConfigurationHandler ambariServerConfigurationHandler) {
    super(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType, requestId, saRunner, metaInfo, ambariServerConfigurationHandler);
  }

  @Override
  protected StackAdvisorCommandType getCommandType() {
    return StackAdvisorCommandType.VALIDATE_COMPONENT_LAYOUT;
  }

  @Override
  protected void validate(StackAdvisorRequest request) throws StackAdvisorException {
    if (request.getHosts() == null || request.getHosts().isEmpty() || request.getServices() == null
        || request.getServices().isEmpty()) {
      throw new StackAdvisorException("Hosts, services and recommendations must not be empty");
    }
  }

  @Override
  protected ValidationResponse updateResponse(StackAdvisorRequest request, ValidationResponse response) {
    return response;
  }

  @Override
  protected String getResultFileName() {
    return "component-layout-validation.json";
  }

}
