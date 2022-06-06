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

package org.apache.ambari.server.stack;

import java.io.File;

import org.apache.ambari.server.AmbariException;

/**
 * Encapsulates IO operations on a common services directory.
 */
public class CommonServiceDirectory extends ServiceDirectory {

  /**
   * Constructor.
   *
   * @param servicePath     path of the service directory
   * @throws org.apache.ambari.server.AmbariException if unable to parse the service directory
   */
  public CommonServiceDirectory(String servicePath) throws AmbariException {
    super(servicePath);
  }

  /**
   * Obtain the advisor name.
   *
   * @return advisor name
   */
  @Override
  public String getAdvisorName(String serviceName) {
    if (getAdvisorFile() == null || serviceName == null)
      return null;

    File serviceVersionDir = new File(getAbsolutePath());
    String serviceVersion = serviceVersionDir.getName().replaceAll("\\.", "");

    String advisorName = serviceName + serviceVersion + "ServiceAdvisor";
    return advisorName;
  }

  /**
   * @return the service name-version (will be used for logging purposes by superclass)
   */
  @Override
  public String getService() {
    File serviceVersionDir = new File(getAbsolutePath());
    File serviceDir = serviceVersionDir.getParentFile();

    String service = String.format("%s-%s", serviceDir.getName(), serviceVersionDir.getName());
    return service;
  }

  /**
   * @return the resources directory
   */
  @Override
  protected File getResourcesDirectory() {
    File serviceVersionDir = new File(getAbsolutePath());
    return serviceVersionDir.getParentFile().getParentFile().getParentFile();
  }

  /**
   * @return the text common-services (will be used for logging purposes by superclass)
   */
  @Override
  public String getStack() {
    return "common-services";
  }
}
