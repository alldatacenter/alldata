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

package org.apache.ambari.server.controller;


import io.swagger.annotations.ApiModelProperty;

public class StackResponse {

  private String stackName;

  public StackResponse(String stackName) {
    setStackName(stackName);
  }

  @ApiModelProperty(name = "stack_name")
  public String getStackName() {
    return stackName;
  }

  public void setStackName(String stackName) {
    this.stackName = stackName;
  }

  @Override
  public int hashCode() {
    int result;
    result = 31 + getStackName().hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof StackResponse)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    StackResponse stackResponse = (StackResponse) obj;
    return getStackName().equals(stackResponse.getStackName());
  }

  /**
   * Interface to help correct Swagger documentation generation
   */
  public interface StackResponseSwagger extends ApiModel {
    @ApiModelProperty(name = "Stacks")
    public StackResponse getStackResponse();
  }

}

