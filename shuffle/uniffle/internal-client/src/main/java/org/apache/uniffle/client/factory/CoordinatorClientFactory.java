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

package org.apache.uniffle.client.factory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.client.api.CoordinatorClient;
import org.apache.uniffle.client.impl.grpc.CoordinatorGrpcClient;
import org.apache.uniffle.common.ClientType;

public class CoordinatorClientFactory {
  private static final Logger LOG = LoggerFactory.getLogger(CoordinatorClientFactory.class);

  private ClientType clientType;

  public CoordinatorClientFactory(ClientType clientType) {
    this.clientType = clientType;
  }

  public CoordinatorClient createCoordinatorClient(String host, int port) {
    if (clientType.equals(ClientType.GRPC)) {
      return new CoordinatorGrpcClient(host, port);
    } else {
      throw new UnsupportedOperationException("Unsupported client type " + clientType);
    }
  }

  public List<CoordinatorClient> createCoordinatorClient(String coordinators) {
    LOG.info("Start to create coordinator clients from {}", coordinators);
    List<CoordinatorClient> coordinatorClients = Lists.newLinkedList();
    String[] coordinatorList = coordinators.trim().split(",");
    if (coordinatorList.length == 0) {
      String msg = "Invalid " + coordinators;
      LOG.error(msg);
      throw new RuntimeException(msg);
    }

    for (String coordinator: coordinatorList) {
      String[] ipPort = coordinator.trim().split(":");
      if (ipPort.length != 2) {
        String msg = "Invalid coordinator format " + Arrays.toString(ipPort);
        LOG.error(msg);
        throw new RuntimeException(msg);
      }

      String host = ipPort[0];
      int port = Integer.parseInt(ipPort[1]);
      CoordinatorClient coordinatorClient = createCoordinatorClient(host, port);
      coordinatorClients.add(coordinatorClient);
      LOG.info("Add coordinator client {}", coordinatorClient.getDesc());
    }
    LOG.info("Finish create coordinator clients {}",
        coordinatorClients.stream().map(CoordinatorClient::getDesc).collect(Collectors.joining(", ")));
    return coordinatorClients;
  }
}
