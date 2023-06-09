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
package org.apache.drill.common.logical.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link CredentialsProvider} that holds credentials provided by user.
 * <p>
 * Its constructor accepts a map with credential names as keys and values as corresponding credential values.
 */
public class PlainCredentialsProvider implements CredentialsProvider {
  private static final Logger logger = LoggerFactory.getLogger(PlainCredentialsProvider.class);
  public static final CredentialsProvider EMPTY_CREDENTIALS_PROVIDER =
      new PlainCredentialsProvider(Collections.emptyMap());

  private final Map<String, String> credentials;

  @JsonCreator
  public PlainCredentialsProvider(@JsonProperty("credentials") Map<String, String> credentials) {
    this.credentials = credentials;
  }

  @Override
  @JsonIgnore(false)
  public Map<String, String> getCredentials() {
    return credentials;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PlainCredentialsProvider that = (PlainCredentialsProvider) o;
    return Objects.equals(credentials, that.credentials);
  }

  @Override
  public int hashCode() {
    return Objects.hash(credentials);
  }
}
