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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link CredentialsProvider} that obtains credential values from
 * environment variables.
 * <p>
 * Its constructor accepts a map with credential names as keys and env variable names as values.
 */
public class EnvCredentialsProvider implements CredentialsProvider {
  private final Map<String, String> envVariables;

  @JsonCreator
  public EnvCredentialsProvider(@JsonProperty("envVariableNames") Map<String, String> envVariableNames) {
    this.envVariables = envVariableNames;
  }

  @Override
  public Map<String, String> getCredentials() {
    Map<String, String> credentials = new HashMap<>();
    envVariables.forEach((key, value) -> credentials.put(key, System.getenv(value)));

    return credentials;
  }

  public Map<String, String> getEnvVariables() {
    return envVariables;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnvCredentialsProvider that = (EnvCredentialsProvider) o;
    return Objects.equals(envVariables, that.envVariables);
  }

  @Override
  public int hashCode() {
    return Objects.hash(envVariables);
  }
}
