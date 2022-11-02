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
package org.apache.ambari.server.security.credential;

import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * PrincipalKeyCredential encapsulates a credential consisting of a principal (or username) and
 * a (secret) key.
 */
public class PrincipalKeyCredential implements Credential {

  private static final String VALUE_PREFIX = "PrincipalKeyCredential";
  /**
   * This principal value
   */
  private String principal;

  /**
   * The plaintext key value
   */
  private char[] key;

  /**
   * Creates an empty PrincipalKeyCredential
   */
  public PrincipalKeyCredential() {
    this(null, (char[]) null);
  }

  /**
   * Creates a new PrincipalKeyCredential
   *
   * @param principal a String containing the principal name for this credential
   * @param key       a String containing the secret key for this credential
   */
  public PrincipalKeyCredential(String principal, String key) {
    this(principal, (key == null) ? null : key.toCharArray());
  }

  /**
   * Creates a new PrincipalKeyCredential
   *
   * @param principal a String containing the principal name for this credential
   * @param key       a char array containing the secret key for this credential
   */
  public PrincipalKeyCredential(String principal, char[] key) {
    this.principal = principal;
    this.key = key;
  }

  /**
   * @return a String containing the principal name for this credential
   */
  public String getPrincipal() {
    return principal;
  }

  /**
   * @param principal a String containing the principal name for this credential
   */
  public void setPrincipal(String principal) {
    this.principal = principal;
  }

  /**
   * @return a char array containing the secret key for this credential
   */
  public char[] getKey() {
    return key;
  }

  /**
   * @param key a char array containing the secret key for this credential
   */
  public void setKey(char[] key) {
    this.key = key;
  }


  /**
   * Returns a value representation of this PrincipalKeyCredential
   *
   * @return a String containing the value representation of this PrincipalKeyCredential
   */
  @Override
  public char[] toValue() {
    StringBuilder builder = new StringBuilder();
    builder.append("PrincipalKeyCredential");
    builder.append(new Gson().toJson(this));
    return builder.toString().toCharArray();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    } else if (this == obj) {
      return true;
    } else if (obj.getClass() == this.getClass()) {
      PrincipalKeyCredential other = (PrincipalKeyCredential) obj;
      return ((this.principal == null) ? (other.principal == null) : this.principal.equals(other.principal)) &&
          ((this.key == null) ? (other.key == null) : Arrays.equals(this.key, other.key));
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return ((principal == null) ? 0 : principal.hashCode()) +
        ((key == null) ? 0 : Arrays.hashCode(key));
  }

  /**
   * Renders a new PrincipalKeyCredential from its value representation
   *
   * @param value a String containing the value representation of this PrincipalKeyCredential
   * @return a new PrincipalKeyCredential or null if a new PrincipalKeyCredential cannot be created
   */
  public static PrincipalKeyCredential fromValue(String value) throws InvalidCredentialValueException {
    if (isValidValue(value)) {
      value = value.substring(VALUE_PREFIX.length());
      try {
        return (value.isEmpty()) ? null : new Gson().fromJson(value, PrincipalKeyCredential.class);
      } catch (JsonSyntaxException e) {
        throw new InvalidCredentialValueException("The value does not represent a PrincipalKeyCredential", e);
      }
    } else {
      throw new InvalidCredentialValueException("The value does not represent a PrincipalKeyCredential");
    }
  }

  public static boolean isValidValue(String value) {
    return ((value != null) && value.startsWith(VALUE_PREFIX));
  }
}
