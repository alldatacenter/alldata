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

package com.netease.arctic.log;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.utils.IdGenerator;
import org.apache.iceberg.relocated.com.google.common.primitives.Longs;
import org.apache.iceberg.types.Type;

import java.io.Serializable;

import static com.netease.arctic.utils.FlipUtil.convertToByte;

/**
 * Wrap log data: log version, upstreamId, EpicNo, Flip, actual value {@link T} information.
 */
public interface LogData<T> {
  byte[] MAGIC_NUMBER = new byte[]{'n', 'e', 't'};

  /**
   * The version mark the {@link LogData}
   *
   * @return {@link FormatVersion} ordinarily
   */
  default String getVersion() {
    return new String(getVersionBytes());
  }

  byte[] getVersionBytes();

  /**
   * The job id generated from an upstream job(e.g. arctic log writer operator in the flink application)
   *
   * @return generated from {@link IdGenerator} ordinarily
   */
  default String getUpstreamId() {
    return new String(getUpstreamIdBytes());
  }

  byte[] getUpstreamIdBytes();

  /**
   * The epic number indicates a batch of data in the log queue(e.g. Kafka), the EpicNo is similar to the checkpoint id
   * in the flink application.
   *
   * @return epic number
   */
  long getEpicNo();

  default byte[] getEpicNoBytes() {
    return Longs.toByteArray(getEpicNo());
  }

  /**
   * Flip flag the upstream job whether happened failover or restored from a last completed checkpoint,
   * flip is true means downstream job should retract the duplicate data in the log queue.
   * flip is false means this is a normal data in the log queue(e.g. Kafka).
   *
   * @return true: retract message in the log queue, false: regard the message as normal data.
   */
  boolean getFlip();

  default byte getFlipByte() {
    return convertToByte(getFlip());
  }

  /**
   * Flag change log data.
   *
   * @return a change log data type
   */
  ChangeAction getChangeAction();

  default byte getChangeActionByte() {
    return getChangeAction().toByteValue();
  }

  /**
   * @return rowData if in the flink application
   */
  T getActualValue();

  /**
   * Accessor for getting the field of a row during runtime.
   */
  interface FieldGetter<T> extends Serializable {
    Object getFieldOrNull(T row, int fieldPos);
  }

  interface FieldGetterFactory<T> extends Serializable {
    FieldGetter<T> createFieldGetter(Type fieldType, int fieldPos);
  }

  /**
   * used by Log deserialization
   *
   * @param <T>
   */
  interface Factory<T> extends Serializable {
    T createActualValue(Object[] objects, Type[] fieldTypes);

    LogData<T> create(T t, Object... headers);

    Class<?> getActualValueClass();

    Object convertIfNecessary(Type primitiveType, Object obj);
  }
}
