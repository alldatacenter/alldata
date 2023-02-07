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

package org.apache.drill.exec.store.hdf5;

import java.util.HashMap;
import java.util.Map;

public class HDF5DrillMetadata {
  private String path;

  private String dataType;

  private Map<String, HDF5Attribute> attributes;

  private boolean isLink;

  public HDF5DrillMetadata() {
    attributes = new HashMap<>();
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    this.path = path;
  }

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  public Map<String, HDF5Attribute> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, HDF5Attribute> attribs) {
    this.attributes = attribs;
  }

  public boolean isLink() {
    return isLink;
  }

  public void setLink(boolean linkStatus) {
    isLink = linkStatus;
  }
}
