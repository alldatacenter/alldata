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

import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.utils.VersionUtils;

public class StackId implements Comparable<StackId> {

  private static final String NAME_SEPARATOR = "-";

  private String stackName;
  private String stackVersion;

  public StackId() {
    stackName = "";
    stackVersion = "";
  }

  public StackId(String stackId) {
    parseStackIdHelper(this, stackId);
  }

  public StackId(StackInfo stackInfo) {
    stackName = stackInfo.getName();
    stackVersion = stackInfo.getVersion();
  }

  public StackId(String stackName, String stackVersion) {
    this(stackName + NAME_SEPARATOR + stackVersion);
  }

  public StackId(StackEntity stackEntity) {
    this(stackEntity.getStackName(), stackEntity.getStackVersion());
  }

  /**
   * @return the stackName
   */
  public String getStackName() {
    return stackName;
  }

  /**
   * @return the stackVersion
   */
  public String getStackVersion() {
    return stackVersion;
  }

  /**
   * @return the stackVersion
   */
  public String getStackId() {
    if (stackName.isEmpty()
        && stackVersion.isEmpty()) {
      return "";
    }
    return stackName + NAME_SEPARATOR + stackVersion;
  }

  /**
   * @param stackId the stackVersion to set
   */
  public void setStackId(String stackId) {
    parseStackIdHelper(this, stackId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object object) {
    if (!(object instanceof StackId)) {
      return false;
    }
    if (this == object) {
      return true;
    }
    StackId s = (StackId) object;
    return stackName.equals(s.stackName) && stackVersion.equals(s.stackVersion);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = stackName != null ? stackName.hashCode() : 0;
    result = 31 * result + (stackVersion != null ? stackVersion.hashCode() : 0);
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(StackId other) {
    if (this == other) {
      return 0;
    }

    if (other == null) {
      throw new RuntimeException("Cannot compare with a null value.");
    }

    int returnValue = getStackName().compareTo(other.getStackName());
    if (returnValue == 0) {
      returnValue = VersionUtils.compareVersions(getStackVersion(), other.getStackVersion());
    } else {
      throw new RuntimeException("StackId with different names cannot be compared.");
    }
    return returnValue;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return getStackId();
  }

  private void parseStackIdHelper(StackId stackVersion, String stackId) {
    if (stackId == null || stackId.isEmpty()) {
      stackVersion.stackName = "";
      stackVersion.stackVersion = "";
      return;
    }

    int pos = stackId.indexOf('-');
    if (pos == -1 || (stackId.length() <= (pos + 1))) {
      throw new RuntimeException("Could not parse invalid Stack Id" + ", stackId=" + stackId);
    }

    stackVersion.stackName = stackId.substring(0, pos);
    stackVersion.stackVersion = stackId.substring(pos + 1);
  }
}
