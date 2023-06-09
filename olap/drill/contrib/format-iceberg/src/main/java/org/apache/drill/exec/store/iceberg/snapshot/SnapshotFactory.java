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
package org.apache.drill.exec.store.iceberg.snapshot;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class SnapshotFactory {

  public static final SnapshotFactory INSTANCE = new SnapshotFactory();

  public Snapshot createSnapshot(SnapshotContext snapshotContext) {
    if (snapshotContext.getSnapshotId() != null) {
      Preconditions.checkArgument(snapshotContext.getSnapshotAsOfTime() == null,
        "Both 'snapshotId' and 'snapshotAsOfTime' cannot be specified");
      Preconditions.checkArgument(snapshotContext.getFromSnapshotId() == null,
        "Both 'snapshotId' and 'fromSnapshotId' cannot be specified");
      Preconditions.checkArgument(snapshotContext.getToSnapshotId() == null,
        "Both 'snapshotId' and 'toSnapshotId' cannot be specified");
      return new SnapshotById(snapshotContext.getSnapshotId());
    } else if (snapshotContext.getSnapshotAsOfTime() != null) {
      Preconditions.checkArgument(snapshotContext.getSnapshotId() == null,
        "Both 'snapshotId' and 'snapshotAsOfTime' cannot be specified");
      Preconditions.checkArgument(snapshotContext.getFromSnapshotId() == null,
        "Both 'snapshotAsOfTime' and 'fromSnapshotId' cannot be specified");
      Preconditions.checkArgument(snapshotContext.getToSnapshotId() == null,
        "Both 'snapshotAsOfTime' and 'toSnapshotId' cannot be specified");
      return new SnapshotByTime(snapshotContext.getSnapshotAsOfTime());
    } else if (snapshotContext.getFromSnapshotId() != null) {
      Preconditions.checkArgument(snapshotContext.getSnapshotId() == null,
        "Both 'snapshotId' and 'fromSnapshotId' cannot be specified");
      Preconditions.checkArgument(snapshotContext.getSnapshotAsOfTime() == null,
        "Both 'snapshotAsOfTime' and 'fromSnapshotId' cannot be specified");

      return snapshotContext.getToSnapshotId() == null
        ? new SnapshotAfter(snapshotContext.getFromSnapshotId())
        : new SnapshotsBetween(snapshotContext.getFromSnapshotId(), snapshotContext.getToSnapshotId());
    } else {
      return null;
    }
  }

  public static class SnapshotContext {
    private final Long snapshotId;

    private final Long snapshotAsOfTime;

    private final Long fromSnapshotId;

    private final Long toSnapshotId;

    SnapshotContext(SnapshotContextBuilder builder) {
      this.snapshotId = builder.snapshotId;
      this.snapshotAsOfTime = builder.snapshotAsOfTime;
      this.fromSnapshotId = builder.fromSnapshotId;
      this.toSnapshotId = builder.toSnapshotId;
    }

    public static SnapshotContextBuilder builder() {
      return new SnapshotContextBuilder();
    }

    public Long getSnapshotId() {
      return this.snapshotId;
    }

    public Long getSnapshotAsOfTime() {
      return this.snapshotAsOfTime;
    }

    public Long getFromSnapshotId() {
      return this.fromSnapshotId;
    }

    public Long getToSnapshotId() {
      return this.toSnapshotId;
    }

    public static class SnapshotContextBuilder {
      private Long snapshotId;

      private Long snapshotAsOfTime;

      private Long fromSnapshotId;

      private Long toSnapshotId;

      public SnapshotContextBuilder snapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
        return this;
      }

      public SnapshotContextBuilder snapshotAsOfTime(Long snapshotAsOfTime) {
        this.snapshotAsOfTime = snapshotAsOfTime;
        return this;
      }

      public SnapshotContextBuilder fromSnapshotId(Long fromSnapshotId) {
        this.fromSnapshotId = fromSnapshotId;
        return this;
      }

      public SnapshotContextBuilder toSnapshotId(Long toSnapshotId) {
        this.toSnapshotId = toSnapshotId;
        return this;
      }

      public SnapshotContext build() {
        return new SnapshotContext(this);
      }
    }
  }
}
