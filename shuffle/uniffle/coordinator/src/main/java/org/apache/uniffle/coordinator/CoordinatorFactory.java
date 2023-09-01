/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.coordinator;

import org.apache.uniffle.common.rpc.GrpcServer;
import org.apache.uniffle.common.rpc.ServerInterface;

public class CoordinatorFactory {

  private final CoordinatorServer coordinatorServer;
  private final CoordinatorConf conf;

  public CoordinatorFactory(CoordinatorServer coordinatorServer) {
    this.coordinatorServer = coordinatorServer;
    this.conf = coordinatorServer.getCoordinatorConf();
  }

  public ServerInterface getServer() {
    String type = conf.getString(CoordinatorConf.RPC_SERVER_TYPE);
    if (type.equals(ServerType.GRPC.name())) {
      return new GrpcServer(conf, new CoordinatorGrpcService(coordinatorServer),
          coordinatorServer.getGrpcMetrics());
    } else {
      throw new UnsupportedOperationException("Unsupported server type " + type);
    }
  }

  enum ServerType {
    GRPC
  }
}
