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
package org.apache.drill.exec.ops;

import io.netty.buffer.DrillBuf;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.holders.ValueHolder;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.PartitionExplorer;
import org.apache.drill.shaded.guava.com.google.common.base.Function;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;

/**
 * Defines the query state and shared resources available to UDFs through
 * injectables. For use in a function, include a {@link javax.inject.Inject}
 * annotation on a UDF class member with any of the types available through
 * this interface.
 */
public interface UdfUtilities {

  // Map between injectable classes and their respective getter methods
  // used for code generation
  ImmutableMap<Class<?>, String> INJECTABLE_GETTER_METHODS =
      new ImmutableMap.Builder<Class<?>, String>()
          .put(DrillBuf.class, "getManagedBuffer")
          .put(PartitionExplorer.class, "getPartitionExplorer")
          .put(ContextInformation.class, "getContextInformation")
          .put(OptionManager.class, "getOptions")
          .put(BufferManager.class, "getManagedBufferManager")
          .build();


  /**
   * Get the context information such as:
   *  - query start time
   *  - root fragment timezone
   *  - query userName
   *  - system userName
   *  - default schema name in current session at the time of query.
   *
   * @return - ContextInformation
   */
  ContextInformation getContextInformation();

  /**
   * For UDFs to allocate general purpose intermediate buffers we provide the
   * DrillBuf type as an injectable, which provides access to an off-heap
   * buffer that can be tracked by Drill and re-allocated as needed.
   *
   * @return - a buffer managed by Drill, connected to the fragment allocator
   *           for memory management
   */
  DrillBuf getManagedBuffer();

  /**
   * A partition explorer allows UDFs to view the sub-partitions below a
   * particular partition. This allows for the implementation of UDFs to
   * query against the partition information, without having to read
   * the actual data contained in the partition. This interface is designed
   * for UDFs that take only constant inputs, as this interface will only
   * be useful if we can evaluate the constant UDF at planning time.
   *
   * Any function defined to use this interface that is not evaluated
   * at planning time by the constant folding rule will be querying
   * the storage plugin for meta-data for each record processed.
   *
   * Be sure to check the query plans to see that this expression has already
   * been evaluated during planning if you write UDFs against this interface.
   *
   * See {@link org.apache.drill.exec.expr.fn.impl.DirectoryExplorers} for
   * example usages of this interface.
   *
   * @return - an object for exploring partitions of all available schemas
   */
  PartitionExplorer getPartitionExplorer();

  /**
   * Works with value holders cache which holds constant value and its wrapper by type.
   * If value is absent uses holderInitializer to create holder and adds it to cache.
   *
   * @return - a wrapper object for an constant value.
   */
  ValueHolder getConstantValueHolder(String value, MinorType type, Function<DrillBuf, ValueHolder> holderInitializer);
}
