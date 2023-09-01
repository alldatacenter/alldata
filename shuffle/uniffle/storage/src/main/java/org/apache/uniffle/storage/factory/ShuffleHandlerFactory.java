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

package org.apache.uniffle.storage.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.client.api.ShuffleServerClient;
import org.apache.uniffle.client.factory.ShuffleServerClientFactory;
import org.apache.uniffle.common.ClientType;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.util.RssUtils;
import org.apache.uniffle.storage.handler.api.ClientReadHandler;
import org.apache.uniffle.storage.handler.api.ShuffleDeleteHandler;
import org.apache.uniffle.storage.handler.impl.ComposedClientReadHandler;
import org.apache.uniffle.storage.handler.impl.HdfsClientReadHandler;
import org.apache.uniffle.storage.handler.impl.HdfsShuffleDeleteHandler;
import org.apache.uniffle.storage.handler.impl.LocalFileClientReadHandler;
import org.apache.uniffle.storage.handler.impl.LocalFileDeleteHandler;
import org.apache.uniffle.storage.handler.impl.MemoryClientReadHandler;
import org.apache.uniffle.storage.handler.impl.MultiReplicaClientReadHandler;
import org.apache.uniffle.storage.request.CreateShuffleDeleteHandlerRequest;
import org.apache.uniffle.storage.request.CreateShuffleReadHandlerRequest;
import org.apache.uniffle.storage.util.StorageType;

public class ShuffleHandlerFactory {

  private static ShuffleHandlerFactory INSTANCE;

  private ShuffleHandlerFactory() {
  }

  public static synchronized ShuffleHandlerFactory getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new ShuffleHandlerFactory();
    }
    return INSTANCE;
  }


  public ClientReadHandler createShuffleReadHandler(CreateShuffleReadHandlerRequest request) {
    if (CollectionUtils.isEmpty(request.getShuffleServerInfoList())) {
      throw new RuntimeException("Shuffle servers should not be empty!");
    }
    if (request.getShuffleServerInfoList().size() > 1) {
      List<ClientReadHandler> handlers = Lists.newArrayList();
      request.getShuffleServerInfoList().forEach((ssi) -> {
        handlers.add(ShuffleHandlerFactory.getInstance().createSingleReplicaClientReadHandler(request, ssi));
      });
      return new MultiReplicaClientReadHandler(handlers, request.getShuffleServerInfoList(),
          request.getExpectBlockIds(), request.getProcessBlockIds());
    } else {
      ShuffleServerInfo serverInfo = request.getShuffleServerInfoList().get(0);
      return createSingleReplicaClientReadHandler(request, serverInfo);
    }
  }

  public ClientReadHandler createSingleReplicaClientReadHandler(CreateShuffleReadHandlerRequest request,
                                                                ShuffleServerInfo serverInfo) {
    String storageType = request.getStorageType();
    StorageType type = StorageType.valueOf(storageType);

    if (StorageType.MEMORY == type) {
      throw new UnsupportedOperationException(
          "Doesn't support storage type for client read  :" + storageType);
    }

    if (StorageType.HDFS == type) {
      return getHdfsClientReadHandler(request, serverInfo);
    }
    if (StorageType.LOCALFILE == type) {
      return getLocalfileClientReaderHandler(request, serverInfo);
    }

    List<Supplier<ClientReadHandler>> handlers = new ArrayList<>();
    if (StorageType.withMemory(type)) {
      handlers.add(
          () -> getMemoryClientReadHandler(request, serverInfo)
      );
    }
    if (StorageType.withLocalfile(type)) {
      handlers.add(
          () -> getLocalfileClientReaderHandler(request, serverInfo)
      );
    }
    if (StorageType.withHDFS(type)) {
      handlers.add(
          () -> getHdfsClientReadHandler(request, serverInfo)
      );
    }
    if (handlers.isEmpty()) {
      throw new RssException("This should not happen due to the unknown storage type: " + storageType);
    }

    return new ComposedClientReadHandler(serverInfo, handlers);
  }

  private ClientReadHandler getMemoryClientReadHandler(CreateShuffleReadHandlerRequest request, ShuffleServerInfo ssi) {
    ShuffleServerClient shuffleServerClient = ShuffleServerClientFactory.getInstance().getShuffleServerClient(
        ClientType.GRPC.name(), ssi);
    Roaring64NavigableMap expectTaskIds = null;
    if (request.isExpectedTaskIdsBitmapFilterEnable()) {
      Roaring64NavigableMap realExceptBlockIds = RssUtils.cloneBitMap(request.getExpectBlockIds());
      realExceptBlockIds.xor(request.getProcessBlockIds());
      expectTaskIds = RssUtils.generateTaskIdBitMap(realExceptBlockIds, request.getIdHelper());
    }
    ClientReadHandler memoryClientReadHandler = new MemoryClientReadHandler(
        request.getAppId(),
        request.getShuffleId(),
        request.getPartitionId(),
        request.getReadBufferSize(),
        shuffleServerClient,
        expectTaskIds
    );
    return memoryClientReadHandler;
  }

  private ClientReadHandler getLocalfileClientReaderHandler(CreateShuffleReadHandlerRequest request,
                                                            ShuffleServerInfo ssi) {
    ShuffleServerClient shuffleServerClient = ShuffleServerClientFactory.getInstance().getShuffleServerClient(
        ClientType.GRPC.name(), ssi);
    return new LocalFileClientReadHandler(
        request.getAppId(), request.getShuffleId(), request.getPartitionId(),
        request.getIndexReadLimit(), request.getPartitionNumPerRange(), request.getPartitionNum(),
        request.getReadBufferSize(), request.getExpectBlockIds(), request.getProcessBlockIds(),
        shuffleServerClient, request.getDistributionType(), request.getExpectTaskIds()
    );
  }

  private ClientReadHandler getHdfsClientReadHandler(CreateShuffleReadHandlerRequest request, ShuffleServerInfo ssi) {
    return new HdfsClientReadHandler(
        request.getAppId(),
        request.getShuffleId(),
        request.getPartitionId(),
        request.getIndexReadLimit(),
        request.getPartitionNumPerRange(),
        request.getPartitionNum(),
        request.getReadBufferSize(),
        request.getExpectBlockIds(),
        request.getProcessBlockIds(),
        request.getStorageBasePath(),
        request.getHadoopConf(),
        request.getDistributionType(),
        request.getExpectTaskIds(),
        ssi.getId()
    );
  }

  public ShuffleDeleteHandler createShuffleDeleteHandler(CreateShuffleDeleteHandlerRequest request) {
    if (StorageType.HDFS.name().equals(request.getStorageType())) {
      return new HdfsShuffleDeleteHandler(request.getConf());
    } else if (StorageType.LOCALFILE.name().equals(request.getStorageType())) {
      return new LocalFileDeleteHandler();
    } else {
      throw new UnsupportedOperationException("Doesn't support storage type for shuffle delete handler:"
          + request.getStorageType());
    }
  }
}
