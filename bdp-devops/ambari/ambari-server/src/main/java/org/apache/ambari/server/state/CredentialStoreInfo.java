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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * Represents credential store information
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CredentialStoreInfo {
  /**
   * Use Boolean data-type internally, so that we can validate the XML.
   */

  @XmlElement(name = "supported")
  private Boolean supported = null;

  @XmlElement(name = "required")
  private Boolean required = null;

  @XmlElement(name = "enabled")
  private Boolean enabled = null;

  /**
   * Default constructor
   */
  public CredentialStoreInfo() {
  }

  /**
   * Constructor taking in values for supported and enabled
   *
   * @param supported
   * @param enabled
   * @param required
   */
  public CredentialStoreInfo(Boolean supported, Boolean enabled, Boolean required) {
    this.supported = supported;
    this.enabled = enabled;
    this.required = required;
  }

  /**
   * Gets a value indicating if the service supports credential store. If null, this was not specified.
   *
   * @return
   */
  public Boolean isSupported() {
    return supported;
  }

  /**
   * Set whether a service supports credential store.
   *
   * @param supported
   */
  public void setSupported(Boolean supported) {
    this.supported = supported;
  }

  /**
   * Gets a value indicating whether the service is enabled for credential store use.
   *
   * @return - true, false, null if not specified.
   */
  public Boolean isEnabled() {
    return enabled;
  }

  /**
   * Set whether the service is enabled for credential store use.
   *
   * @param enabled - true, false, null.
   */
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Gets a value indicating whether the service requires credential store.
   *
   * @return - true, false, null if not specified.
   */
  public Boolean isRequired() {
    return required;
  }

  /**
   * Set whether the service requires credential store
   *
   * @param required - true, false, null.
   */
  public void setRequired(Boolean required) {
    this.required = required;
  }

  /**
   * String representation of this object
   *
   * @return
   */
  @Override
  public String toString() {
    return "CredentialStoreInfo{" +
           "supported=" + supported +
           ", required=" + required +
           ", enabled=" + enabled +
           '}';
  }
}
