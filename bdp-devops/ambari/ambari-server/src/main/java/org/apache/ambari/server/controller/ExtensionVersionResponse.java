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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.stack.Validable;

/**
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
public class ExtensionVersionResponse implements Validable{

  private String extensionName;
  private String extensionVersion;
  private boolean valid;
  private String parentVersion;

  public ExtensionVersionResponse(String extensionVersion, String parentVersion,
                              boolean valid, Collection<String> errorSet) {
    setExtensionVersion(extensionVersion);
    setParentVersion(parentVersion);
    setValid(valid);
    addErrors(errorSet);
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  private Set<String> errorSet = new HashSet<>();

  @Override
  public void addError(String error) {
    errorSet.add(error);
  }

  @Override
  public Collection<String> getErrors() {
    return errorSet;
  }

  @Override
  public void addErrors(Collection<String> errors) {
    this.errorSet.addAll(errors);
  }


  public String getExtensionName() {
    return extensionName;
  }

  public void setExtensionName(String extensionName) {
    this.extensionName = extensionName;
  }

  public String getExtensionVersion() {
    return extensionVersion;
  }

  public void setExtensionVersion(String extensionVersion) {
    this.extensionVersion = extensionVersion;
  }

  public String getParentVersion() {
    return parentVersion;
  }

  public void setParentVersion(String parentVersion) {
    this.parentVersion = parentVersion;
  }
}
