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

import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.alert.MetricSource;
import org.apache.ambari.server.state.kerberos.VariableReplacementHelper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * The {@link UriInfo} class is used to represent a complex URI structure where
 * there can be both a plaintext and SSL URI. This is used in cases where the
 * alert definition needs a way to expose which URL (http or https) should be
 * used to gather data. Currently, only {@link MetricSource} uses this, but it
 * can be swapped out in other source types where a plain string is used for the
 * URI.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UriInfo {
  /**
   * The HTTP URI to use.
   */
  @SerializedName("acceptable_codes")
  private Set<Integer> m_acceptableCodes;

  /**
   * The HTTP URI to use.
   */
  @SerializedName("http")
  private String m_httpUri;

  /**
   * The HTTPS URI to use.
   */
  @SerializedName("https")
  private String m_httpsUri;

  /**
   * The configuration property to check to determine if HTTP or HTTPS should be
   * used.
   */
  @SerializedName("https_property")
  private String m_httpsProperty;

  /**
   * The value to check {@link #m_httpsProperty} against to determine if HTTPS
   * should be used.
   */
  @SerializedName("https_property_value")
  private String m_httpsPropertyValue;

  /**
   * Kerberos keytab path to use.
   */
  @SerializedName("kerberos_keytab")
  private String m_kerberosKeytab;

  /**
   * Kerberos principal name to use.
   */
  @SerializedName("kerberos_principal")
  private String m_kerberosPrincipal;

  /**
   * A default port to use on the host running the alert if no URLs can be
   * found.
   */
  @SerializedName("default_port")
  private Number m_port = 0;

  /**
   * An optional timeout value for connections.
   */
  @SerializedName("connection_timeout")
  @JsonProperty("connection_timeout")
  private float m_connectionTimeout = 5.0f;


  /**
   * An optional read timeout value for connections.
   */
  @SerializedName("read_timeout")
  private float readTimeout = 15.0f;

  /**
   * If present, then the component supports HA mode and the properties
   * contained within need to be checked to see if an HA URI is required to be
   * constructed instead of using {@link #m_httpUri}, {@link #m_httpsUri} and
   * {@link #m_httpsProperty}.
   */
  @SerializedName("high_availability")
  private HighAvailability m_highAvailability;

  /**
   * Gets the plaintext (HTTP) URI that can be used to retrieve alert
   * information.
   *
   * @return the httpUri the URI (or {@code null} to always use the secure URL).
   */
  @JsonProperty("http")
  public String getHttpUri() {
    return m_httpUri;
  }

  /**
   * Sets the plaintext (HTTP) URI that can be used to retrieve alert
   * information.
   *
   * @param httpUri
   *          the plaintext URI or {@code null} for none.
   */
  public void setHttpUri(String httpUri) {
    m_httpUri = httpUri;
  }

  public void setHttpsUri(String httpsUri) {
    this.m_httpsUri = httpsUri;
  }

  public void setHttpsPropertyValue(String m_httpsPropertyValue) {
    this.m_httpsPropertyValue = m_httpsPropertyValue;
  }

  public void setHttpsProperty(String m_httpsProperty) {
    this.m_httpsProperty = m_httpsProperty;
  }

  /**
   * Gets the default port to use on the host running the alert if none of the
   * http properties are available.
   *
   * @return the default port if none of the http properties are found.
   */
  @JsonProperty("default_port")
  public Number getDefaultPort() {
    return m_port;
  }

  /**
   * Gets the secure (HTTPS) URI that can be used to retrieve alert information.
   *
   * @return the httpsUri the URI (or {@code null} to always use the insecure
   *         URL).
   */
  @JsonProperty("https")
  public String getHttpsUri() {
    return m_httpsUri;
  }

  /**
   * The configuration property that can be used to determine if the secure URL
   * should be used.
   *
   * @return the httpsProperty the configuration property, or {@code null} for
   *         none.
   */
  @JsonProperty("https_property")
  public String getHttpsProperty() {
    return m_httpsProperty;
  }

  /**
   * The literal value to use when comparing to the result from
   * {@link #getHttpsProperty()}.
   *
   * @return the httpsPropertyValue the literal value that indicates SSL mode is
   *         enabled, or {@code null} for none.
   */
  @JsonProperty("https_property_value")
  public String getHttpsPropertyValue() {
    return m_httpsPropertyValue;
  }

  /**
   * The configuration property with kerberos keytab path.
   *
   * @return the configuration property, or {@code null} for none.
   */
  @JsonProperty("kerberos_keytab")
  public String getKerberosKeytab() {
    return m_kerberosKeytab;
  }

  /**
   * The configuration property with kerberos principal name.
   *
   * @return the configuration property, or {@code null} for none.
   */
  @JsonProperty("kerberos_principal")
  public String getKerberosPrincipal() {
    return m_kerberosPrincipal;
  }

  /**
   * Gets the HA structure to use when determining if the component is in HA
   * mode and requires the URL to be built dynamically.
   *
   * @return the HA structure or {@code null} if the component does not support
   *         HA mode.
   */
  @JsonProperty("high_availability")
  public HighAvailability getHighAvailability() {
    return m_highAvailability;
  }

  public Set<Integer> getAcceptableCodes() {
    return m_acceptableCodes;
  }

  public void setAcceptableCodes(Set<Integer> m_acceptableCodes) {
    this.m_acceptableCodes = m_acceptableCodes;
  }

  /**
   * The {@link HighAvailability} structure is used to hold information about
   * how HA URIs are constructed if the service supports HA mode. For example
   *
   * <pre>
   * high_availability": {
   *   "nameservice": "{{hdfs-site/dfs.internal.nameservices}}",
   *   "alias_key" : "dfs.ha.namenodes.{{ha-nameservice}}",
   *   "http_pattern" : "dfs.namenode.http-address.{{ha-nameservice}}.{{alias}}",
   *   "https_pattern" : "dfs.namenode.https-address.{{ha-nameservice}}.{{alias}}"
   * }
   * </pre>
   *
   * Where the nameservice is {@code c1ha} and the alias key is
   * {@code dfs.ha.namenodes.c1ha}. In this case the http pattern is defined as
   * dfs.namenode.http-address.{{ha-nameservice}}.{{alias}}
   */
  public class HighAvailability {
    /**
     * The key that represents the name service. The alert will use the
     * existance of this key as the marker for HA mode.
     */
    @SerializedName("nameservice")
    private String m_nameservice;

    /**
     * The key that will be used to retrieve the aliases for each host.
     */
    @SerializedName("alias_key")
    private String m_aliasKey;

    /**
     * The parameterized pattern for determining the HTTP URL to use.
     */
    @SerializedName("http_pattern")
    private String m_httpPattern;

    /**
     * The parameterized pattern for determining the HTTPS URL to use.
     */
    @SerializedName("https_pattern")
    private String m_httpsPattern;

    /**
     * Gets the nameservice name.
     *
     * @return the nameservice
     */
    @JsonProperty("nameservice")
    public String getNameservice() {
      return m_nameservice;
    }

    /**
     * Gets the parameterized key to use when retrieving the host aliases.
     *
     * @return the alias key
     */
    @JsonProperty("alias_key")
    public String getAliasKey() {
      return m_aliasKey;
    }

    /**
     * Get the parameterized HTTP pattern to use.
     *
     * @return the httpPattern
     */
    @JsonProperty("http_pattern")
    public String getHttpPattern() {
      return m_httpPattern;
    }

    /**
     * Get the parameterized HTTPS pattern to use.
     *
     * @return the httpsPattern
     */
    @JsonProperty("https_pattern")
    public String getHttpsPattern() {
      return m_httpsPattern;
    }
  }

  public URI resolve(Map<String, Map<String, String>> config) throws AmbariException {
    VariableReplacementHelper variableReplacer = new VariableReplacementHelper();
    String httpsProperty = variableReplacer.replaceVariables(m_httpsProperty, config);
    String httpsPropertyValue = variableReplacer.replaceVariables(m_httpsPropertyValue, config);
    return httpsProperty == null || !httpsProperty.equals(httpsPropertyValue)
      ? URI.create(String.format("http://%s", variableReplacer.replaceVariables(m_httpUri, config)))
      : URI.create(String.format("https://%s", variableReplacer.replaceVariables(m_httpsUri, config)));
  }

  public int getConnectionTimeoutMsec() {
    return (int) m_connectionTimeout * 1000;
  }

  public int getReadTimeoutMsec() {
    return (int) readTimeout * 1000;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;

    result = prime * result + ((m_httpUri == null) ? 0 : m_httpUri.hashCode());

    result = prime * result
        + ((m_httpsProperty == null) ? 0 : m_httpsProperty.hashCode());

    result = prime
        * result
        + ((m_httpsPropertyValue == null) ? 0 : m_httpsPropertyValue.hashCode());

    result = prime * result
        + ((m_httpsUri == null) ? 0 : m_httpsUri.hashCode());

    result = prime * result + m_port.intValue();
    return result;
  }

  /**
   * {@inheritDoc}
   */
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

    UriInfo other = (UriInfo) obj;
    if (m_httpUri == null) {
      if (other.m_httpUri != null) {
        return false;
      }
    } else if (!m_httpUri.equals(other.m_httpUri)) {
      return false;
    }

    if (m_httpsProperty == null) {
      if (other.m_httpsProperty != null) {
        return false;
      }
    } else if (!m_httpsProperty.equals(other.m_httpsProperty)) {
      return false;
    }

    if (m_httpsPropertyValue == null) {
      if (other.m_httpsPropertyValue != null) {
        return false;
      }
    } else if (!m_httpsPropertyValue.equals(other.m_httpsPropertyValue)) {
      return false;
    }

    if (m_httpsUri == null) {
      if (other.m_httpsUri != null) {
        return false;
      }
    } else if (!m_httpsUri.equals(other.m_httpsUri)) {
      return false;
    }

    if (m_connectionTimeout != other.m_connectionTimeout) {
      return false;
    }

    if (m_port.intValue() != other.m_port.intValue()) {
      return false;
    }
    return true;
  }
}
