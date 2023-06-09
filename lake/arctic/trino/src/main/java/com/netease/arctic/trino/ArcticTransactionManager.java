
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

package com.netease.arctic.trino;

import io.trino.spi.classloader.ThreadContextClassLoader;
import io.trino.spi.connector.ConnectorTransactionHandle;

import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

/**
 * This is used to guarantee one transaction to one {@link ArcticConnectorMetadata}
 */
public class ArcticTransactionManager {
  private final ArcticMetadataFactory metadataFactory;
  private final ClassLoader classLoader;
  private final ConcurrentMap<ConnectorTransactionHandle, MemoizedMetadata> transactions = new ConcurrentHashMap<>();

  @Inject
  public ArcticTransactionManager(ArcticMetadataFactory metadataFactory) {
    this(metadataFactory, Thread.currentThread().getContextClassLoader());
  }

  public ArcticTransactionManager(ArcticMetadataFactory metadataFactory, ClassLoader classLoader) {
    this.metadataFactory = requireNonNull(metadataFactory, "metadataFactory is null");
    this.classLoader = requireNonNull(classLoader, "classLoader is null");
  }

  public void begin(ConnectorTransactionHandle transactionHandle) {
    MemoizedMetadata previousValue = transactions.putIfAbsent(transactionHandle, new MemoizedMetadata());
    checkState(previousValue == null);
  }

  public ArcticConnectorMetadata get(ConnectorTransactionHandle transactionHandle) {
    return transactions.get(transactionHandle).get();
  }

  public void commit(ConnectorTransactionHandle transaction) {
    MemoizedMetadata transactionalMetadata = transactions.remove(transaction);
    checkArgument(transactionalMetadata != null, "no such transaction: %s", transaction);
  }

  public void rollback(ConnectorTransactionHandle transaction) {
    MemoizedMetadata transactionalMetadata = transactions.remove(transaction);
    checkArgument(transactionalMetadata != null, "no such transaction: %s", transaction);
    transactionalMetadata.optionalGet().ifPresent(metadata -> {
      try (ThreadContextClassLoader ignored = new ThreadContextClassLoader(classLoader)) {
        metadata.rollback();
      }
    });
  }

  private class MemoizedMetadata {
    @GuardedBy("this")
    private ArcticConnectorMetadata metadata;

    public synchronized Optional<ArcticConnectorMetadata> optionalGet() {
      return Optional.ofNullable(metadata);
    }

    public synchronized ArcticConnectorMetadata get() {
      if (metadata == null) {
        try (ThreadContextClassLoader ignored = new ThreadContextClassLoader(classLoader)) {
          metadata = metadataFactory.create();
        }
      }
      return metadata;
    }
  }
}
