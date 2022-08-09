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

import java.io.IOException;

/**
 * KerberosDataFile is an interfaced expected to be implemented by all Kerberos data file
 * implementations
 */
public interface KerberosDataFile {

  /**
   * Opens the data file.
   * <p/>
   * This may be called multiple times and the appropriate action should occur depending on if the
   * file has been previously opened or closed.
   *
   * @throws java.io.IOException if an error occurs while opening the file
   */
  void open() throws IOException;

  /**
   * Closes the data file.
   * <p/>
   * This may be called multiple times and the appropriate action should occur depending on if the
   * file has been previously opened or closed.
   *
   * @throws java.io.IOException if an error occurs while closing the file
   */
  void close() throws IOException;
}
