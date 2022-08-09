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

package org.apache.ambari.server.serveraction.kerberos;

/**
 * Indicates that the KDC admin credentials have not been set.
 */
public class KerberosMissingAdminCredentialsException extends KerberosOperationException {
  /**
   * The default error message to use when handling this exception
   */
  private static final String DEFAULT_MESSAGE = "Missing KDC administrator credentials.\n" +
      "The KDC administrator credentials must be set as a persisted or temporary credential resource." +
      "This may be done by issuing a POST to the /api/v1/clusters/:clusterName/credentials/kdc.admin.credential API entry point with the following payload:\n" +
      "{\n" +
      "  \"Credential\" : {\n" +
      "    \"principal\" : \"(PRINCIPAL)\", \"key\" : \"(PASSWORD)\", \"type\" : \"(persisted|temporary)\"}\n" +
      "  }\n" +
      "}";

  /**
   * Constructor using the default missing credentials message.
   */
  public KerberosMissingAdminCredentialsException() {
    this(DEFAULT_MESSAGE);
  }

  /**
   * Constructor.
   *
   * @param message error message
   */
  public KerberosMissingAdminCredentialsException(String message) {
    super(message);
  }

  /**
   * Constructor using the default message.
   *
   * @param cause   root cause
   */
  public KerberosMissingAdminCredentialsException(Throwable cause) {
    this(DEFAULT_MESSAGE, cause);
  }

  /**
   * Constructor.
   *
   * @param message error message
   * @param cause   root cause
   */
  public KerberosMissingAdminCredentialsException(String message, Throwable cause) {
    super(message, cause);
  }
}
