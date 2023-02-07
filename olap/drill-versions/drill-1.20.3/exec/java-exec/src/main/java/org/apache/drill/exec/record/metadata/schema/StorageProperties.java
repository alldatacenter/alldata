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
package org.apache.drill.exec.record.metadata.schema;

import org.apache.drill.exec.store.StorageStrategy;

/**
 * Holds storage properties used when writing schema container.
 */
public class StorageProperties {

  private final StorageStrategy storageStrategy;
  private final boolean overwrite;

  private StorageProperties(Builder builder) {
    this.storageStrategy = builder.storageStrategy;
    this.overwrite = builder.overwrite;
  }

  public static Builder builder() {
    return new Builder();
  }

  public StorageStrategy getStorageStrategy() {
    return storageStrategy;
  }

  public boolean isOverwrite() {
    return overwrite;
  }

  public static class Builder {

    private StorageStrategy storageStrategy = StorageStrategy.DEFAULT;
    private boolean overwrite;

    public Builder storageStrategy(StorageStrategy storageStrategy) {
      this.storageStrategy = storageStrategy;
      return this;
    }

    public Builder overwrite() {
      this.overwrite = true;
      return this;
    }

    public Builder overwrite(boolean overwrite) {
      this.overwrite = overwrite;
      return this;
    }

    public StorageProperties build() {
      return new StorageProperties(this);
    }
  }
}
