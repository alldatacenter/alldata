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
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * {@link ServiceLdapInfo} encapsulates meta information about a service's
 * support LDAP integration
 * <p>
 * The data is expected to be like
 * 
 * <pre>
 *   &lt;ldap&gt;
 *     &lt;supported&gt;true&lt;/supported&gt;
 *     &lt;ldapEnabledTest&gt;
 *         {
 *           "equals": [
 *             "service-site/ranger.authentication.method",
 *             "LDAP"
 *           ]
 *         }
 *     &lt;/ldapEnabledTest&gt;
 *   &lt;/ldap&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceLdapInfo {

  /**
   * Indicates whether the relevant service supports LDAP integration
   * (<code>true</code>) or not (<code>false</code>).
   */
  @XmlElement(name = "supported")
  private Boolean supported = Boolean.FALSE;

  /**
   * The configuration that can be used to determine if LDAP integration has been
   * enabled.
   * <p>
   * It is expected that this value is in the form of a valid JSON predicate
   * ({@link PredicateUtils#fromJSON(String)}
   */
  @XmlElement(name = "ldapEnabledTest")
  private String ldapEnabledTest = null;

  /**
   * Default constructor
   */
  public ServiceLdapInfo() {
    this(Boolean.FALSE, null);
  }

  /**
   * Constructor taking in values for supported and the configuration that can be
   * used to determine if it is enabled for not.
   *
   * @param supported
   *          true if LDAP integration is supported; false otherwise
   * @param ldapEnabledTest
   *          the configuration that can be used to determine if LDAP integration
   *          has been enabled
   */
  public ServiceLdapInfo(Boolean supported, String ldapEnabledTest) {
    this.supported = supported;
    this.ldapEnabledTest = ldapEnabledTest;
  }

  /**
   * Gets the value whether if the service supports LDAP integration.
   * <p>
   * <code>null</code> indicates the value was not set
   *
   * @return true if LDAP integration is supported; false if LDAP integration is
   *         not supported; null if not specified
   */
  public Boolean getSupported() {
    return supported;
  }

  /**
   * Tests whether the service supports LDAP integration.
   *
   * @return true if LDAP integration is supported; false otherwise
   */
  public boolean isSupported() {
    return Boolean.TRUE.equals(supported);
  }

  /**
   * Sets the value indicating whether the service supports LDAP integration.
   *
   * @param supported
   *          true if LDAP integration is supported; false if LDAP integration is
   *          not supported; null if not specified
   */
  public void setSupported(Boolean supported) {
    this.supported = supported;
  }

  /**
   * Gets the configuration specification that can be used to determine if LDAP
   * has been enabled or not.
   *
   * @return a configuration specification (a valid JSON predicate)
   */
  public String getLdapEnabledTest() {
    return ldapEnabledTest;
  }

  /**
   * Sets the configuration specification that can be used to determine if LDAP
   * has been enabled or not.
   *
   * @param ldapEnabledTest
   *          a configuration specification (a valid JSON predicate)
   */
  public void setLdapEnabledTest(String ldapEnabledTest) {
    this.ldapEnabledTest = ldapEnabledTest;
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public boolean equals(Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
