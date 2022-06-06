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

import org.apache.ambari.server.collections.PredicateUtils;

import com.google.common.base.MoreObjects;

/**
 * {@link SingleSignOnInfo} encapsulates meta information about a service's support for single
 * sign-on (SSO) integration
 * <p>
 * The data is expected to be like
 * <pre>
 *   &lt;sso&gt;
 *     &lt;supported&gt;true&lt;/supported&gt;
 *     &lt;ssoEnabledTest&gt;
 *         {
 *           "equals": [
 *             "config-type/sso.enabled.property_name",
 *             "true"
 *           ]
 *         }
 *     &lt;/ssoEnabledTest&gt;
 *   &lt;/sso&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class SingleSignOnInfo {

  /**
   * Indicates whether the relevant service supports SSO integration (<code>true</code>) or not (<code>false</code>).
   */
  @XmlElement(name = "supported")
  private Boolean supported = Boolean.FALSE;

  /**
   * The configuration that can be used to determine if SSO integration has been enabled.
   * <p>
   * It is expected that this value is in the form of <code>configuration-type/property_name</code>
   *
   * @deprecated Use {@link #ssoEnabledTest} instead
   */
  @XmlElement(name = "enabledConfiguration")
  private String enabledConfiguration = null;

  /**
   * The configuration that can be used to determine if SSO integration has been enabled.
   * <p>
   * It is expected that this value is in the form of a valid JSON predicate ({@link PredicateUtils#fromJSON(String)}
   */
  @XmlElement(name = "ssoEnabledTest")
  private String ssoEnabledTest = null;

  /**
   * Indicates if Kerberos is required for SSO integration (<code>true</code>) or not (<code>false</code>)
   */
  @XmlElement(name = "kerberosRequired")
  private Boolean kerberosRequired = Boolean.FALSE;

  /**
   * Default constructor
   */
  public SingleSignOnInfo() {
  }

  /**
   * Constructor taking in values for supported and the configuration that can be used to determine
   * if it is enabled for not.
   *
   * @param supported            true if SSO integration is supported; false otherwise
   * @param enabledConfiguration the configuration that can be used to determine if SSO integration has been enabled
   * @param kerberosRequired     <code>true</code> if Kerberos is required for SSO integration; <code>false</code> otherwise
   */
  public SingleSignOnInfo(Boolean supported, String enabledConfiguration, Boolean kerberosRequired) {
    this.supported = supported;
    this.enabledConfiguration = enabledConfiguration;
    this.kerberosRequired = kerberosRequired;
  }

  /**
   * Tests whether the service supports SSO integration.
   *
   * @return true if SSO integration is supported; false otherwise
   */
  public boolean isSupported() {
    return Boolean.TRUE.equals(supported);
  }

  /**
   * Gets the value whether if the service supports SSO integration.
   * <p>
   * <code>null</code> indicates the value was not set
   *
   * @return true if SSO integration is supported; false if SSO integration is not supported; null if not specified
   */
  public Boolean getSupported() {
    return supported;
  }

  /**
   * Gets the value indicating whether the service supports SSO integration.
   *
   * @param supported true if SSO integration is supported; false if SSO integration is not supported; null if not specified
   */
  public void setSupported(Boolean supported) {
    this.supported = supported;
  }

  /**
   * @deprecated USe {@link #getSsoEnabledTest()} instead
   * <p>
   * Gets the configuration specification that can be used to determine if SSO has been enabled or not.
   *
   * @return a configuration specification (config-type/property_name)
   */
  public String getEnabledConfiguration() {
    return enabledConfiguration;
  }

  /**
   * @deprecated Use {@link #setSsoEnabledTest(String)} instead
   * <p>
   * Sets the configuration specification that can be used to determine if SSO has been enabled or not.
   *
   * @param enabledConfiguration a configuration specification (config-type/property_name)
   */
  public void setEnabledConfiguration(String enabledConfiguration) {
    this.enabledConfiguration = enabledConfiguration;
  }

  /**
   * Gets the configuration specification that can be used to determine if SSO has
   * been enabled or not.
   *
   * @return a configuration specification (a valid JSON predicate)
   */
  public String getSsoEnabledTest() {
    return ssoEnabledTest;
  }

  /**
   * Sets the configuration specification that can be used to determine if SSO has
   * been enabled or not.
   *
   * @param enabledConfiguration
   *          a configuration specification (a valid JSON predicate)
   */
  public void setSsoEnabledTest(String ssoEnabledTest) {
    this.ssoEnabledTest = ssoEnabledTest;
  }

  /**
   * @return the flag is Kerberos is required for SSO integration
   */
  public boolean isKerberosRequired() {
    return Boolean.TRUE.equals(kerberosRequired);
  }

  /**
   * Sets the flag is Kerberos is required for SSO integration
   */
  public void setKerberosRequired(Boolean kerberosRequired) {
    this.kerberosRequired = kerberosRequired;
  }

  /**
   * String representation of this object
   *
   * @return a string
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("supported", supported)
        .add("enabledConfiguration", enabledConfiguration)
        .add("ssoEnabledTest", ssoEnabledTest)
        .add("kerberosRequired", kerberosRequired)
        .toString();
  }
}
