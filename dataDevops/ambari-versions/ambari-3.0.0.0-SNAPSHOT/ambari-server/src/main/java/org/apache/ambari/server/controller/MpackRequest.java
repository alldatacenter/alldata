/**
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

import org.apache.ambari.server.orm.entities.MpackEntity;

/**
 * The {@link MpackRequest} encapsulates the data necessary to make a
 * backend request for {@link MpackEntity}.
 *
 */
public class MpackRequest {

  private Long id;
  private Long registryId;
  private String mpackName;
  private String mpackVersion;
  private String mpackUri;

  public MpackRequest(Long id) {
    this.setId(id);
  }

  public MpackRequest() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getMpackName() {
    return mpackName;
  }

  public void setMpackName(String mpackName) {
    this.mpackName = mpackName;
  }

  public String getMpackVersion() {
    return mpackVersion;
  }

  public void setMpackVersion(String mpackVersion) {
    this.mpackVersion = mpackVersion;
  }

  public String getMpackUri() {
    return mpackUri;
  }

  public void setMpackUri(String mpackUri) {
    this.mpackUri = mpackUri;
  }

  public Long getRegistryId() {
    return registryId;
  }

  public void setRegistryId(Long registryId) {
    this.registryId = registryId;
  }

  @Override
  public int hashCode() {
    int result;
    result = 31 + getId().hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MpackRequest)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    MpackRequest mpackRequest = (MpackRequest) obj;

    if (id == null) {
      if (mpackRequest.id != null) {
        return false;
      }
    } else if (!id.equals(mpackRequest.id)) {
      return false;
    }

    if (mpackName == null) {
      if (mpackRequest.mpackName != null) {
        return false;
      }
    } else if (!mpackName.equals(mpackRequest.mpackName)) {
      return false;
    }

    if (mpackUri == null) {
      if (mpackRequest.mpackUri != null) {
        return false;
      }
    } else if (!mpackUri.equals(mpackRequest.mpackUri)) {
      return false;
    }

    if (registryId == null) {
      if (mpackRequest.registryId != null) {
        return false;
      }
    } else if (!registryId.equals(mpackRequest.registryId)) {
      return false;
    }

    if (mpackVersion == null) {
      if (mpackRequest.mpackVersion != null) {
        return false;
      }
    } else if (!mpackVersion.equals(mpackRequest.mpackVersion)) {
      return false;
    }

    return true;
  }

}
