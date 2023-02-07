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
package org.apache.drill.exec.store.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.common.logical.security.CredentialsProvider;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link CredentialsProvider} that obtains credential values from
 * {@link Configuration} properties.
 * <p>
 * Its constructor accepts a map with credential names as keys and property names as values.
 */
public class HadoopCredentialsProvider implements CredentialsProvider {
  private final Configuration configuration;
  private final Map<String, String> propertyNames;

  public HadoopCredentialsProvider(Configuration configuration, Map<String, String> propertyNames) {
    this.configuration = configuration;
    this.propertyNames = propertyNames;
  }

  @JsonCreator
  public HadoopCredentialsProvider(@JsonProperty("propertyNames") Map<String, String> propertyNames) {
    this.configuration = new Configuration();
    this.propertyNames = propertyNames;
  }

  @Override
  public Map<String, String> getCredentials() {
    Map<String, String> credentials = new HashMap<>();
    propertyNames.forEach((key, value) -> {
      try {
        char[] credValue = configuration.getPassword(value);
        if (credValue != null) {
          credentials.put(key, new String(credValue));
        }
      } catch (IOException e) {
        throw new RuntimeException("Error while fetching credentials from configuration", e);
      }
    });

    return credentials;
  }

  public Map<String, String> getPropertyNames() {
    return propertyNames;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HadoopCredentialsProvider that = (HadoopCredentialsProvider) o;
    return Objects.equals(propertyNames, that.propertyNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(propertyNames);
  }
}
