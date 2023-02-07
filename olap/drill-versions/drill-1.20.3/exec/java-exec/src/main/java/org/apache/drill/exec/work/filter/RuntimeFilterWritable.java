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
package org.apache.drill.exec.work.filter;


import io.netty.buffer.DrillBuf;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.BitData;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * A binary wire transferable representation of the RuntimeFilter which contains
 * the runtime filter definition and its corresponding data.
 */
public class RuntimeFilterWritable implements AutoCloseables.Closeable{

  private BitData.RuntimeFilterBDef runtimeFilterBDef;

  private DrillBuf[] data;

  private String identifier;

  public RuntimeFilterWritable(BitData.RuntimeFilterBDef runtimeFilterBDef, DrillBuf... data) {
    List<Integer> bfSizeInBytes = runtimeFilterBDef.getBloomFilterSizeInBytesList();
    int bufArrLen = data.length;
    Preconditions.checkArgument(bfSizeInBytes.size() == bufArrLen, "the input DrillBuf number does not match the metadata definition!");
    this.runtimeFilterBDef = runtimeFilterBDef;
    this.data = data;
    this.identifier = "majorFragmentId:" + runtimeFilterBDef.getMajorFragmentId()
      + ",minorFragmentId:" + runtimeFilterBDef.getMinorFragmentId()
      + ", srcOperatorId:" + runtimeFilterBDef.getHjOpId();
  }

  public BitData.RuntimeFilterBDef getRuntimeFilterBDef() {
    return runtimeFilterBDef;
  }

  public DrillBuf[] getData() {
    return data;
  }

  public void setData(DrillBuf... data) {
    this.data = data;
  }


  public List<BloomFilter> unwrap() {
    List<Integer> sizeInBytes = runtimeFilterBDef.getBloomFilterSizeInBytesList();
    List<BloomFilter> bloomFilters = new ArrayList<>(sizeInBytes.size());
    for (int i = 0; i < sizeInBytes.size(); i++) {
      DrillBuf byteBuf = data[i];
      int offset = 0;
      int size = sizeInBytes.get(i);
      DrillBuf bloomFilterContent = byteBuf.slice(offset, size);
      BloomFilter bloomFilter = new BloomFilter(bloomFilterContent);
      bloomFilters.add(bloomFilter);
    }
    return bloomFilters;
  }

  public void aggregate(RuntimeFilterWritable runtimeFilterWritable) {
    List<BloomFilter> thisFilters = this.unwrap();
    List<BloomFilter> otherFilters = runtimeFilterWritable.unwrap();
    for (int i = 0; i < thisFilters.size(); i++) {
      BloomFilter thisOne = thisFilters.get(i);
      BloomFilter otherOne = otherFilters.get(i);
      thisOne.or(otherOne);
    }
    for (BloomFilter bloomFilter : otherFilters) {
      bloomFilter.getContent().clear();
    }
  }

  public RuntimeFilterWritable duplicate(BufferAllocator bufferAllocator) {
    int len = data.length;
    DrillBuf[] cloned = new DrillBuf[len];
    int i = 0;
    for (DrillBuf src : data) {
      int capacity = src.readableBytes();
      DrillBuf duplicateOne = bufferAllocator.buffer(capacity);
      int readerIndex = src.readerIndex();
      duplicateOne.writeBytes(src);
      src.readerIndex(readerIndex);
      cloned[i] = duplicateOne;
      i++;
    }
    return new RuntimeFilterWritable(runtimeFilterBDef, cloned);
  }

  public void retainBuffers(final int increment) {
    if (increment <= 0) {
      return;
    }
    for (final DrillBuf buf : data) {
      buf.retain(increment);
    }
  }
  //TODO: Not used currently because of DRILL-6826
  public RuntimeFilterWritable newRuntimeFilterWritable(BufferAllocator bufferAllocator) {
    int bufNum = data.length;
    DrillBuf [] newBufs = new DrillBuf[bufNum];
    int i = 0;
    for (DrillBuf buf : data) {
      DrillBuf transferredBuffer = buf.transferOwnership(bufferAllocator).buffer;
      newBufs[i] = transferredBuffer;
      i++;
    }
    return new RuntimeFilterWritable(this.runtimeFilterBDef, newBufs);
  }

  public String toString() {
    return identifier;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other instanceof RuntimeFilterWritable) {
      RuntimeFilterWritable otherRFW = (RuntimeFilterWritable) other;
      return this.identifier.equals(otherRFW.identifier);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return identifier.hashCode();
  }

  @Override
  public void close() {
    for (DrillBuf buf : data) {
      buf.release();
    }
  }
}
