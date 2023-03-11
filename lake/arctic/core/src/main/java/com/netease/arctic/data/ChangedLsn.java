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

package com.netease.arctic.data;

import java.io.Serializable;
import java.util.Objects;


/**
 * Global row sequence number.
 * <p>
 * Consist of two parts:
 * <ul>
 *   <li>id of transaction row written into the table
 *   <li>row sequence within the transaction
 * </ul>
 */

public class ChangedLsn implements Comparable<ChangedLsn>, Serializable {

  private final long transactionId;
  private final long fileOffset;

  public static ChangedLsn of(long transactionId, long fileOffset) {
    return new ChangedLsn(transactionId, fileOffset);
  }

  public static ChangedLsn of(byte[] bytes) {
    return of(((long) bytes[15] << 56) |
                    ((long) bytes[14] & 0xff) << 48 |
                    ((long) bytes[13] & 0xff) << 40 |
                    ((long) bytes[12] & 0xff) << 32 |
                    ((long) bytes[11] & 0xff) << 24 |
                    ((long) bytes[10] & 0xff) << 16 |
                    ((long) bytes[9] & 0xff) << 8 |
                    ((long) bytes[8] & 0xff),
            ((long) bytes[7] << 56) |
                    ((long) bytes[6] & 0xff) << 48 |
                    ((long) bytes[5] & 0xff) << 40 |
                    ((long) bytes[4] & 0xff) << 32 |
                    ((long) bytes[3] & 0xff) << 24 |
                    ((long) bytes[2] & 0xff) << 16 |
                    ((long) bytes[1] & 0xff) << 8 |
                    ((long) bytes[0] & 0xff));
  }

  private ChangedLsn(long transactionId, long fileOffset) {
    this.transactionId = transactionId;
    this.fileOffset = fileOffset;
  }

  public long transactionId() {
    return transactionId;
  }

  public long fileOffset() {
    return fileOffset;
  }


  @Override
  public int compareTo(ChangedLsn another) {
    if (transactionId > another.transactionId()) {
      return 1;
    } else if (transactionId < another.transactionId) {
      return -1;
    } else {
      if (fileOffset > another.fileOffset()) {
        return 1;
      } else if (fileOffset < another.fileOffset) {
        return -1;
      } else {
        return 0;
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChangedLsn recordLsn = (ChangedLsn) o;
    return transactionId == recordLsn.transactionId &&
            fileOffset == recordLsn.fileOffset;
  }

  @Override
  public int hashCode() {
    return Objects.hash(transactionId, fileOffset);
  }

  @Override
  public String toString() {
    return new StringBuilder("RecordLsn(").append(transactionId)
            .append(", ").append(fileOffset).append(")").toString();
  }

  public byte[] toBytes() {
    return new byte[] {
        (byte) transactionId,
        (byte) (transactionId >> 8),
        (byte) (transactionId >> 16),
        (byte) (transactionId >> 24),
        (byte) (transactionId >> 32),
        (byte) (transactionId >> 40),
        (byte) (transactionId >> 48),
        (byte) (transactionId >> 56),
        (byte) fileOffset,
        (byte) (fileOffset >> 8),
        (byte) (fileOffset >> 16),
        (byte) (fileOffset >> 24),
        (byte) (fileOffset >> 32),
        (byte) (fileOffset >> 40),
        (byte) (fileOffset >> 48),
        (byte) (fileOffset >> 56)
    };
  }
}
