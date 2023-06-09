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
package org.apache.drill.exec.store.dfs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Stores the workspace related config. A workspace has:
 *  - location which is a path.
 *  - writable flag to indicate whether the location supports creating new tables.
 *  - default storage format for new tables created in this workspace.
 */
@JsonIgnoreProperties(value = {"storageformat"}, ignoreUnknown = true)

public class WorkspaceConfig {

  /** Default workspace is a root directory which supports read, but not write. */
  public static final WorkspaceConfig DEFAULT = new WorkspaceConfig("/", false, null, false);

  private final String location;
  private final boolean writable;
  private final String defaultInputFormat;
  private final boolean allowAccessOutsideWorkspace; // do not allow access outside the workspace by default.
                                                     // For backward compatibility, the user can turn this
                                                     // on.
  public WorkspaceConfig(@JsonProperty("location") String location,
                         @JsonProperty("writable") boolean writable,
                         @JsonProperty("defaultInputFormat") String defaultInputFormat,
                         @JsonProperty("allowAccessOutsideWorkspace") boolean allowAccessOutsideWorkspace
      ) {
    this.location = location;
    this.writable = writable;
    this.defaultInputFormat = defaultInputFormat;
    this.allowAccessOutsideWorkspace = allowAccessOutsideWorkspace;
  }

  public String getLocation() {
    return location;
  }

  public boolean isWritable() {
    return writable;
  }

  public String getDefaultInputFormat() {
    return defaultInputFormat;
  }

  @JsonProperty
  public Boolean allowAccessOutsideWorkspace() {
    return allowAccessOutsideWorkspace;
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((defaultInputFormat == null) ? 0 : defaultInputFormat.hashCode());
    result = prime * result + ((location == null) ? 0 : location.hashCode());
    result = prime * result + (writable ? 1231 : 1237);
    result = prime * result + (allowAccessOutsideWorkspace ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    WorkspaceConfig other = (WorkspaceConfig) obj;
    if (defaultInputFormat == null) {
      if (other.defaultInputFormat != null) {
        return false;
      }
    } else if (!defaultInputFormat.equals(other.defaultInputFormat)) {
      return false;
    }
    if (location == null) {
      if (other.location != null) {
        return false;
      }
    } else if (!location.equals(other.location)) {
      return false;
    }
    if (writable != other.writable) {
      return false;
    }
    if (allowAccessOutsideWorkspace != other.allowAccessOutsideWorkspace) {
      return false;
    }
    return true;
  }

}
