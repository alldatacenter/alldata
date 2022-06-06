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

package org.apache.ambari.server.utils;

import java.util.regex.Pattern;

/**
 * HostUtils provides utilities related to processing and validating hosts and host names.
 */
public class HostUtils {
  /**
   * A regular expression to validate that a hostname meets the specifications described in RFC 1123.
   * <p>
   * Various sources on the Internet specific this regular expression for hostname validation.
   */
  private static final Pattern REGEX_VALID_HOSTNAME = Pattern.compile("^(?:(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*(?:[A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");

  /**
   * Validates a hostname to ensure it meets the specifications described in RFC 1123.
   * <p>
   * Note: this does not validate whether the host exists or if the name is a valid DNS name.
   * For example,  host.example.anything is a valid host name however may be an invalid DNS name
   * since "anything" may not be a valid top-level domain.
   *
   * @param hostname a hostname
   * @return true if the hostname is valid; false otherwise
   */
  public static boolean isValidHostname(String hostname) {
    return (hostname != null) && REGEX_VALID_HOSTNAME.matcher(hostname).matches();
  }
}
