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
/**
 * Communication security.
 * <p>
 * Drill uses Java's SASL library to authenticate clients (users and other bits). This is achieved using
 * {@link org.apache.drill.exec.rpc.security.AuthenticationOutcomeListener} on the client-side, and
 * {@link org.apache.drill.exec.rpc.security.ServerAuthenticationHandler} on the server-side.
 * <p>
 * If authentication is enabled, {@link org.apache.drill.exec.rpc.security.AuthenticatorFactory authenticator factory}
 * implementations are discovered at startup from {@link org.apache.drill.common.scanner.persistence.ScanResult
 * scan result} using {@link org.apache.drill.exec.rpc.security.AuthenticatorProviderImpl}. At connection time, after
 * handshake, if either side requires authentication, a series of SASL messages are exchanged. Without successful
 * authentication, any subsequent messages will result in failure and connection drop.
 * <p>
 * Out of the box, Drill supports {@link org.apache.drill.exec.rpc.security.kerberos.KerberosFactory KERBEROS}
 * (through GSSAPI) and {@link org.apache.drill.exec.rpc.security.plain.PlainFactory PLAIN} (through
 * {@link org.apache.drill.exec.rpc.user.security.UserAuthenticator}) mechanisms.
 *
 * @see <a href="https://issues.apache.org/jira/browse/DRILL-4280">
 * DRILL-4280 (design and configuration)</a>
 * @see <a href="https://docs.oracle.com/javase/7/docs/api/javax/security/sasl/package-summary.html">
 * Java's SASL Library</a>
 */
package org.apache.drill.exec.rpc.security;
