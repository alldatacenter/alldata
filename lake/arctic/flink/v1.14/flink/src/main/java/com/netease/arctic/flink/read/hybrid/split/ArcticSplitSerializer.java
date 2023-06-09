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

package com.netease.arctic.flink.read.hybrid.split;

import com.netease.arctic.log.Bytes;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.flink.util.InstantiationUtil;

import java.io.IOException;

/**
 * Serializer that serializes and deserializes {@link ArcticSplit}.
 */
public class ArcticSplitSerializer implements SimpleVersionedSerializer<ArcticSplit> {
  public static final ArcticSplitSerializer INSTANCE = new ArcticSplitSerializer();
  private static final int VERSION = 1;

  private static final byte SNAPSHOT_SPLIT_FLAG = 1;
  private static final byte CHANGELOG_SPLIT_FLAG = 2;

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public byte[] serialize(ArcticSplit split) throws IOException {
    if (split == null) {
      return new byte[0];
    }
    if (split.isSnapshotSplit()) {
      SnapshotSplit snapshotSplit = (SnapshotSplit) split;
      byte[] content = InstantiationUtil.serializeObject(snapshotSplit);
      return Bytes.mergeByte(new byte[]{SNAPSHOT_SPLIT_FLAG}, content);

    } else if (split.isChangelogSplit()) {
      ChangelogSplit changelogSplit = (ChangelogSplit) split;
      byte[] content = InstantiationUtil.serializeObject(changelogSplit);
      return Bytes.mergeByte(new byte[]{CHANGELOG_SPLIT_FLAG}, content);
    } else {
      throw new IllegalArgumentException(
          String.format("This arctic split is not supported, class %s.", split.getClass().getSimpleName()));
    }
  }

  @Override
  public ArcticSplit deserialize(int version, byte[] serialized) throws IOException {
    if (serialized.length == 0) {
      return null;
    }
    try {
      byte flag = serialized[0];
      if (version == VERSION) {
        byte[] content = Bytes.subByte(serialized, 1, serialized.length - 1);
        if (flag == SNAPSHOT_SPLIT_FLAG) {
          return InstantiationUtil.<SnapshotSplit>deserializeObject(content, SnapshotSplit.class.getClassLoader());
        } else if (flag == CHANGELOG_SPLIT_FLAG) {
          return InstantiationUtil.<ChangelogSplit>deserializeObject(content, ChangelogSplit.class.getClassLoader());
        } else {
          throw new IllegalArgumentException("this flag split is unsupported. available: 1,2.");
        }
      }
    } catch (ClassNotFoundException e) {
      throw new FlinkRuntimeException("deserialize split failed", e);
    }
    throw new FlinkRuntimeException(
        String.format("this version %s is not supported during deserialize split.", version));
  }
}
