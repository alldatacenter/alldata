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
package org.apache.drill.exec.rpc.security;

import org.apache.hadoop.security.UserGroupInformation;

import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.io.IOException;
import java.util.Map;

/**
 * An implementation of this factory will be initialized once at startup, if the authenticator is enabled
 * (see {@link #getSimpleName}). For every request for this mechanism (i.e. after establishing a connection),
 * {@link #createSaslServer} will be invoked on the server-side and {@link #createSaslClient} will be invoked
 * on the client-side.
 *
 * Note:
 * + Custom authenticators must have a default constructor.
 *
 * Examples: PlainFactory and KerberosFactory.
 */
public interface AuthenticatorFactory extends AutoCloseable {

  /**
   * Name of the mechanism, in upper case.
   *
   * If this mechanism is present in the list of enabled mechanisms, an instance of this factory is loaded. Note
   * that the simple name maybe the same as it's SASL name.
   *
   * @return mechanism name
   */
  String getSimpleName();

  /**
   * Create and get the login user based on the given properties.
   *
   * @param properties config properties
   * @return ugi
   * @throws IOException
   */
  UserGroupInformation createAndLoginUser(Map<String, ?> properties) throws IOException;

  /**
   * The caller is responsible for {@link SaslServer#dispose disposing} the returned SaslServer.
   *
   * @param ugi ugi
   * @param properties config properties
   * @return sasl server
   * @throws SaslException
   */
  SaslServer createSaslServer(UserGroupInformation ugi, Map<String, ?> properties) throws SaslException;

  /**
   * The caller is responsible for {@link SaslClient#dispose disposing} the returned SaslClient.
   *
   * @param ugi ugi
   * @param properties config properties
   * @return sasl client
   * @throws SaslException
   */
  SaslClient createSaslClient(UserGroupInformation ugi, Map<String, ?> properties) throws SaslException;

}
