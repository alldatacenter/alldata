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

package org.apache.ambari.server.security;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * InternalSSLSocketFactoryTrusting is a trusting {@link SSLSocketFactory} implementation
 * using the TLSv1.2 protocol.
 * <p>
 * This SSL socket factory should allow for at least the following SSL protocols:
 * <ul>
 * <li>TLSv1.2</li>
 * <li>TLSv1.1</li>
 * <li>TLSv1</li>
 * </ul>
 * <p>
 * However, the actual set of enabled SSL protocols is dependent on the underlying JVM.
 * <p>
 * This SSL socket factory creates trusting connections, meaning that the server's SSL certificate
 * will not be tested to see if it is trusted.
 */
public class InternalSSLSocketFactoryTrusting extends InternalSSLSocketFactory {
  public InternalSSLSocketFactoryTrusting() {
    super("TLSv1.2", true);
  }

  public static SocketFactory getDefault() {
    return new InternalSSLSocketFactoryTrusting();
  }
}