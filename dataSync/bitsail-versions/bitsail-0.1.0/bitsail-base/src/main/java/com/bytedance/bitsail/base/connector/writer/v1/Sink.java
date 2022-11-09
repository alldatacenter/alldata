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

package com.bytedance.bitsail.base.connector.writer.v1;

import com.bytedance.bitsail.base.serializer.BinarySerializer;
import com.bytedance.bitsail.base.serializer.SimpleBinarySerializer;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.type.BitSailTypeInfoConverter;
import com.bytedance.bitsail.common.type.TypeInfoConverter;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

/**
 * Created 2022/6/10
 */
public interface Sink<InputT, CommitT extends Serializable, WriterStateT extends Serializable> extends Serializable {

  /**
   * @return The name of writer operation.
   */
  String getWriterName();

  /**
   * Configure writer with user defined options.
   *
   * @param commonConfiguration Common options.
   * @param writerConfiguration Options for writer.
   */
  void configure(BitSailConfiguration commonConfiguration, BitSailConfiguration writerConfiguration) throws Exception;

  /**
   * Create a writer for processing elements.
   *
   * @return An initialized writer.
   */
  Writer<InputT, CommitT, WriterStateT> createWriter(Writer.Context<WriterStateT> context) throws IOException;

  /**
   * @return A converter which supports conversion from BitSail {@link TypeInfo}
   * and external engine type.
   */
  default TypeInfoConverter createTypeInfoConverter() {
    return new BitSailTypeInfoConverter();
  }

  /**
   * @return A committer for commit committable objects.
   */
  default Optional<WriterCommitter<CommitT>> createCommitter() {
    return Optional.empty();
  }

  /**
   * @return A serializer which convert committable object to byte array.
   */
  default BinarySerializer<CommitT> getCommittableSerializer() {
    return new SimpleBinarySerializer<CommitT>();
  }

  /**
   * @return A serializer which convert state object to byte array.
   */
  default BinarySerializer<WriterStateT> getWriteStateSerializer() {
    return new SimpleBinarySerializer<WriterStateT>();
  }
}
