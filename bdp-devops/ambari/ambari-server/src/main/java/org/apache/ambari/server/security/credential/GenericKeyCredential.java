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

/**
 * GenericKeyCredential encapsulates a credential consisting of single key (for example, a password).
 */
public class GenericKeyCredential implements Credential {

  /**
   * The plaintext password value
   */
  private char[] key = null;

  /**
   * Creates an empty GenericKeyCredential
   */
  public GenericKeyCredential() {
  }

  /**
   * Creates a new GenericKeyCredential
   *
   * @param key a char array containing the key for this credential
   */
  public GenericKeyCredential(char[] key) {
    this.key = key;
  }

  /**
   * @return a char array containing the key for this credential
   */
  public char[] getKey() {
    return key;
  }

  /**
   * @param key a char array containing the key for this credential
   */
  public void setKey(char[] key) {
    this.key = key;
  }


  /**
   * Returns a value representation of this GenericKeyCredential
   *
   * @return a String containing the value representation of this GenericKeyCredential
   */
  @Override
  public char[] toValue() {
    return this.key;
  }

  /**
   * Renders a new GenericKeyCredential from its value representation
   *
   * @param value a String containing the value representation of this GenericKeyCredential
   * @return a new GenericKeyCredential
   */
  public static GenericKeyCredential fromValue(String value) throws InvalidCredentialValueException {
    return new GenericKeyCredential((value == null) ? null : value.toCharArray());
  }
}
