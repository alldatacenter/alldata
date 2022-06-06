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

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/**
 * KerberosOperationHandlerFactory gets relevant KerberosOperationHandlers given a KDCType.
 */
@Singleton
public class KerberosOperationHandlerFactory {

  @Inject
  private Injector injector;

  /**
   * Gets the relevant KerberosOperationHandler for some KDCType.
   * <p/>
   * If no KDCType is specified, {@link org.apache.ambari.server.serveraction.kerberos.KDCType#MIT_KDC}
   * will be assumed.
   *
   * @param kdcType the relevant KDCType
   * @return a KerberosOperationHandler
   * @throws java.lang.IllegalArgumentException if kdcType is null or the KDCType is an unexpected value
   */
  public KerberosOperationHandler getKerberosOperationHandler(KDCType kdcType) {
    if (kdcType == null) {
      throw new IllegalArgumentException("kdcType may not be null");
    }

    switch (kdcType) {
      case MIT_KDC:
        return injector.getInstance(MITKerberosOperationHandler.class);
      case ACTIVE_DIRECTORY:
        return injector.getInstance(ADKerberosOperationHandler.class);
      case IPA:
        return injector.getInstance(IPAKerberosOperationHandler.class);
      default:
        throw new IllegalArgumentException(String.format("Unexpected kdcType value: %s", kdcType.name()));
    }
  }
}
