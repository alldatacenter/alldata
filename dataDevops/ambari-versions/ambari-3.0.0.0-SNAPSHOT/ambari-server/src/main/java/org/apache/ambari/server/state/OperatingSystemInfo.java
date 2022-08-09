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


package org.apache.ambari.server.state;

import org.apache.ambari.server.controller.OperatingSystemResponse;


public class OperatingSystemInfo {
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((osType == null) ? 0 : osType.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    OperatingSystemInfo other = (OperatingSystemInfo) obj;
    if (osType == null) {
      if (other.osType != null)
        return false;
    } else if (!osType.equals(other.osType))
      return false;
    return true;
  }

  private String osType;

  public OperatingSystemInfo(String osType) {
    setOsType(osType);
  }

  public String getOsType() {
    return osType;
  }

  public void setOsType(String osType) {
    this.osType = osType;
  }
  
  public OperatingSystemResponse convertToResponse()
  {
    return new OperatingSystemResponse(getOsType());
  }
  
  

}
