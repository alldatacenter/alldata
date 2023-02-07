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
package org.apache.drill.exec.rpc.user;

import java.lang.management.ManagementFactory;

import org.apache.drill.common.Version;
import org.apache.drill.common.util.DrillVersionInfo;
import org.apache.drill.exec.proto.UserProtos.RpcEndpointInfos;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Utility class for User RPC
 *
 */
public final class UserRpcUtils {
  private UserRpcUtils() {}

  /*
   * Template for the endpoint infos.
   *
   * It speeds up things not to check application JMX for
   * each connection.
   */
  private static final RpcEndpointInfos INFOS_TEMPLATE =
      RpcEndpointInfos.newBuilder()
        .setApplication(ManagementFactory.getRuntimeMXBean().getName())
        .setVersion(DrillVersionInfo.getVersion())
        .setMajorVersion(DrillVersionInfo.getMajorVersion())
        .setMinorVersion(DrillVersionInfo.getMinorVersion())
        .setPatchVersion(DrillVersionInfo.getPatchVersion())
        .setBuildNumber(DrillVersionInfo.getBuildNumber())
        .setVersionQualifier(DrillVersionInfo.getQualifier())
        .buildPartial();

  /**
   * Returns a {@code RpcEndpointInfos} instance
   *
   * The instance is populated based on Drill version informations
   * from the classpath and runtime information for the application
   * name.
   *
   * @param name the endpoint name.
   * @return a {@code RpcEndpointInfos} instance
   * @throws NullPointerException if name is null
   */
  public static RpcEndpointInfos getRpcEndpointInfos(String name) {
    RpcEndpointInfos infos = RpcEndpointInfos.newBuilder(INFOS_TEMPLATE)
        .setName(Preconditions.checkNotNull(name))
        .build();

    return infos;
  }

  /**
   * Get the version from a {@code RpcEndpointInfos} instance
   */
  public static Version getVersion(RpcEndpointInfos infos) {
    return new Version(
        infos.getVersion(),
        infos.getMajorVersion(),
        infos.getMinorVersion(),
        infos.getPatchVersion(),
        infos.getBuildNumber(),
        infos.getVersionQualifier());
  }
}
