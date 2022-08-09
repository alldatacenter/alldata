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

import org.apache.ambari.server.orm.entities.ExtensionEntity;
import org.apache.ambari.server.utils.VersionUtils;

/**
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
public class ExtensionId implements Comparable<ExtensionId> {

  private static final String NAME_SEPARATOR = "-";

  private String extensionName;
  private String extensionVersion;

  public ExtensionId() {
    extensionName = "";
    extensionVersion = "";
  }

  public ExtensionId(String extensionId) {
    parseExtensionIdHelper(this, extensionId);
  }

  public ExtensionId(ExtensionInfo extension) {
    extensionName = extension.getName();
    extensionVersion = extension.getVersion();
  }

  public ExtensionId(String extensionName, String extensionVersion) {
    this(extensionName + NAME_SEPARATOR + extensionVersion);
  }

  public ExtensionId(ExtensionEntity entity) {
    this(entity.getExtensionName(), entity.getExtensionVersion());
  }

  /**
   * @return the extensionName
   */
  public String getExtensionName() {
    return extensionName;
  }

  /**
   * @return the extensionVersion
   */
  public String getExtensionVersion() {
    return extensionVersion;
  }

  /**
   * @return the extensionVersion
   */
  public String getExtensionId() {
    if (extensionName.isEmpty()
        && extensionVersion.isEmpty()) {
      return "";
    }
    return extensionName + NAME_SEPARATOR + extensionVersion;
  }

  /**
   * @param extensionId the extensionVersion to set
   */
  public void setExtensionId(String extensionId) {
    parseExtensionIdHelper(this, extensionId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object object) {
    if (!(object instanceof ExtensionId)) {
      return false;
    }
    if (this == object) {
      return true;
    }
    ExtensionId s = (ExtensionId) object;
    return extensionName.equals(s.extensionName) && extensionVersion.equals(s.extensionVersion);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = extensionName != null ? extensionName.hashCode() : 0;
    result = 31 * result + (extensionVersion != null ? extensionVersion.hashCode() : 0);
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(ExtensionId other) {
    if (this == other) {
      return 0;
    }

    if (other == null) {
      throw new RuntimeException("Cannot compare with a null value.");
    }

    int returnValue = getExtensionName().compareTo(other.getExtensionName());
    if (returnValue == 0) {
      returnValue = VersionUtils.compareVersions(getExtensionVersion(), other.getExtensionVersion());
    } else {
      throw new RuntimeException("ExtensionId with different names cannot be compared.");
    }
    return returnValue;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return getExtensionId();
  }

  private void parseExtensionIdHelper(ExtensionId extensionVersion, String extensionId) {
    if (extensionId == null || extensionId.isEmpty()) {
      extensionVersion.extensionName = "";
      extensionVersion.extensionVersion = "";
      return;
    }

    int pos = extensionId.indexOf('-');
    if (pos == -1 || (extensionId.length() <= (pos + 1))) {
      throw new RuntimeException("Could not parse invalid Extension Id" + ", extensionId=" + extensionId);
    }

    extensionVersion.extensionName = extensionId.substring(0, pos);
    extensionVersion.extensionVersion = extensionId.substring(pos + 1);
  }
}
