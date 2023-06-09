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
package org.apache.drill.common.config;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.drill.exec.proto.UserProtos.Property;
import org.apache.drill.exec.proto.UserProtos.UserProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class DrillProperties extends Properties {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillProperties.class);

  // PROPERTY KEYS
  // definitions should be in lowercase

  public static final String ZOOKEEPER_CONNECTION = "zk";

  public static final String DRILLBIT_CONNECTION = "drillbit";

  // "tries" is max number of unique drillbits to try connecting
  // until successfully connected to one of them
  public static final String TRIES = "tries";

  public static final String SCHEMA = "schema";

  public static final String USER = "user";

  public static final String PASSWORD = "password";

  /**
   * Impersonation target name for Drill Inbound Impersonation
   */
  public static final String IMPERSONATION_TARGET = "impersonation_target";

  public static final String AUTH_MECHANISM = "auth";

  public static final String SERVICE_PRINCIPAL = "principal";

  public static final String SERVICE_NAME = "service_name";

  public static final String SERVICE_HOST = "service_host";

  public static final String REALM = "realm";

  public static final String KEYTAB = "keytab";

  public static final String SASL_ENCRYPT = "sasl_encrypt";

  // Should only be used for testing backward compatibility
  @VisibleForTesting
  public static final String TEST_SASL_LEVEL = "test_sasl_level";

  // for subject that has pre-authenticated to KDC (AS) i.e. required credentials are populated in
  // Subject's credentials set
  public static final String KERBEROS_FROM_SUBJECT = "from_subject";

  public static final String QUOTING_IDENTIFIERS = "quoting_identifiers";

  public static final String ENABLE_TLS = "enableTLS";
  public static final String TLS_PROTOCOL = "TLSProtocol";
  public static final String TRUSTSTORE_TYPE = "trustStoreType";
  public static final String TRUSTSTORE_PATH = "trustStorePath";
  public static final String TRUSTSTORE_PASSWORD = "trustStorePassword";
  public static final String DISABLE_HOST_VERIFICATION = "disableHostVerification";
  public static final String DISABLE_CERT_VERIFICATION = "disableCertificateVerification";
  public static final String TLS_HANDSHAKE_TIMEOUT = "TLSHandshakeTimeout";
  public static final String TLS_PROVIDER = "TLSProvider";
  public static final String USE_SYSTEM_TRUSTSTORE = "useSystemTrustStore";
  public static final String USE_MAPR_SSL_CONFIG = "useMapRSSLConfig";

  public static final String QUERY_TAGS = "queryTags";

  // Although all properties from the application are sent to the server (from the client), the following
  // sets of properties are used by the client and server respectively. These are reserved words.

  public static final ImmutableSet<String> ALLOWED_BY_CLIENT =
      ImmutableSet.of(
          ZOOKEEPER_CONNECTION, DRILLBIT_CONNECTION, TRIES,
          SCHEMA,
          USER, PASSWORD, IMPERSONATION_TARGET, AUTH_MECHANISM,
          SERVICE_PRINCIPAL, SERVICE_NAME, SERVICE_HOST, REALM, KEYTAB, KERBEROS_FROM_SUBJECT,
          ENABLE_TLS, TLS_PROTOCOL, TRUSTSTORE_TYPE, TRUSTSTORE_PATH, TRUSTSTORE_PASSWORD,
          DISABLE_HOST_VERIFICATION, DISABLE_CERT_VERIFICATION, TLS_HANDSHAKE_TIMEOUT, TLS_PROVIDER,
          USE_SYSTEM_TRUSTSTORE, QUERY_TAGS
      );

  public static final ImmutableSet<String> ACCEPTED_BY_SERVER = ImmutableSet.of(
      USER /** deprecated */, PASSWORD /** deprecated */,
      SCHEMA,
      IMPERSONATION_TARGET,
      QUOTING_IDENTIFIERS,
      QUERY_TAGS
  );

  private DrillProperties() {
  }

  @Override
  public Object setProperty(final String key, final String value) {
    return super.setProperty(key.toLowerCase(), value);
  }

  @Override
  public String getProperty(final String key) {
    return super.getProperty(key.toLowerCase());
  }

  @Override
  public String getProperty(final String key, final String defaultValue) {
    return super.getProperty(key.toLowerCase(), defaultValue);
  }

  public void merge(final Properties overrides) {
    if (overrides == null) {
      return;
    }
    for (final String key : overrides.stringPropertyNames()) {
      setProperty(key.toLowerCase(), overrides.getProperty(key));
    }
  }

  public void merge(final Map<String, String> overrides) {
    if (overrides == null) {
      return;
    }
    for (final String key : overrides.keySet()) {
      setProperty(key.toLowerCase(), overrides.get(key));
    }
  }

  /**
   * Returns a map of keys and values in this property list where the key and its corresponding value are strings,
   * including distinct keys in the default property list if a key of the same name has not already been found from
   * the main properties list.  Properties whose key or value is not of type <tt>String</tt> are omitted.
   * <p>
   * The returned map is not backed by the <tt>Properties</tt> object. Changes to this <tt>Properties</tt> are not
   * reflected in the map, or vice versa.
   *
   * @return  a map of keys and values in this property list where the key and its corresponding value are strings,
   *          including the keys in the default property list.
   */
  public Map<String, String> stringPropertiesAsMap() {
    final Map<String, String> map = new HashMap<>();
    for (final String property : stringPropertyNames()) {
      map.put(property, getProperty(property));
    }
    return map;
  }

  /**
   * Serializes properties into a protobuf message.
   *
   * @return the serialized properties
   */
  public UserProperties serializeForServer() {
    final UserProperties.Builder propsBuilder = UserProperties.newBuilder();
    for (final String key : stringPropertyNames()) {
      propsBuilder.addProperties(Property.newBuilder()
          .setKey(key)
          .setValue(getProperty(key))
          .build());
    }
    return propsBuilder.build();
  }

  /**
   * Deserializes the given properties into DrillProperties.
   *
   * @param userProperties serialized user properties
   * @param addOnlyKnownServerProperties add only properties known by server
   * @return params
   */
  public static DrillProperties createFromProperties(final UserProperties userProperties,
                                                     final boolean addOnlyKnownServerProperties) {
    final DrillProperties properties = new DrillProperties();
    for (final Property property : userProperties.getPropertiesList()) {
      final String key = property.getKey().toLowerCase();
      if (!addOnlyKnownServerProperties || ACCEPTED_BY_SERVER.contains(key)) {
        properties.setProperty(key, property.getValue());
      } else {
        logger.trace("Server does not recognize property: {}", key);
      }
    }
    return properties;
  }

  /**
   * Returns a new instance of DrillProperties from the given properties.
   *
   * @param properties user properties
   * @return params
   */
  public static DrillProperties createFromProperties(final Properties properties) {
    final DrillProperties drillProperties = new DrillProperties();
    if (properties != null) {
      for (final String key : properties.stringPropertyNames()) {
        final String lowerCaseKey = key.toLowerCase();
        drillProperties.setProperty(lowerCaseKey, properties.getProperty(key));
      }
    }
    return drillProperties;
  }

  public static DrillProperties createEmpty() {
    return new DrillProperties();
  }
}
