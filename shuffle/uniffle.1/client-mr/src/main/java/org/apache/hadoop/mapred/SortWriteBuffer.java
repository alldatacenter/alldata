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

package org.apache.hadoop.mapred;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.io.serializer.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortWriteBuffer<K, V> extends OutputStream  {

  private static final Logger LOG = LoggerFactory.getLogger(SortWriteBuffer.class);
  private long copyTime = 0;
  private final List<WrappedBuffer> buffers = Lists.newArrayList();
  private final List<Record<K>> records = Lists.newArrayList();
  private int dataLength = 0;
  private long sortTime = 0;
  private final RawComparator<K> comparator;
  private long maxSegmentSize;
  private int partitionId;
  private Serializer<K> keySerializer;
  private Serializer<V> valSerializer;
  private int currentOffset = 0;
  private int currentIndex = 0;

  public SortWriteBuffer(
      int partitionId,
      RawComparator<K> comparator,
      long maxSegmentSize,
      Serializer<K> keySerializer,
      Serializer<V> valueSerializer) {
    this.partitionId = partitionId;
    this.comparator = comparator;
    this.maxSegmentSize = maxSegmentSize;
    this.keySerializer = keySerializer;
    this.valSerializer = valueSerializer;
  }

  public int  addRecord(K key, V value) throws IOException {
    keySerializer.open(this);
    valSerializer.open(this);
    int lastOffSet = currentOffset;
    int lastIndex = currentIndex;
    int lastDataLength = dataLength;
    int keyIndex = lastIndex;
    keySerializer.serialize(key);
    int keyLength = dataLength - lastDataLength;
    int keyOffset = lastOffSet;
    if (compact(lastIndex, lastOffSet, keyLength)) {
      keyOffset = lastOffSet;
      keyIndex = lastIndex;
    }
    lastDataLength = dataLength;
    valSerializer.serialize(value);
    int valueLength = dataLength - lastDataLength;
    records.add(new Record<K>(keyIndex, keyOffset, keyLength, valueLength));
    return keyLength + valueLength;
  }

  public void clear() {
    buffers.clear();
    records.clear();
  }

  public synchronized byte[] getData() {
    int extraSize = 0;
    for (Record<K> record : records) {
      extraSize += WritableUtils.getVIntSize(record.getKeyLength());
      extraSize += WritableUtils.getVIntSize(record.getValueLength());
    }

    extraSize += WritableUtils.getVIntSize(-1);
    extraSize += WritableUtils.getVIntSize(-1);
    byte[] data = new byte[dataLength + extraSize];
    int offset = 0;
    long startSort = System.currentTimeMillis();
    records.sort(new Comparator<Record<K>>() {
      @Override
      public int compare(Record<K> o1, Record<K> o2) {
        return comparator.compare(
            buffers.get(o1.getKeyIndex()).getBuffer(),
            o1.getKeyOffSet(),
            o1.getKeyLength(),
            buffers.get(o2.getKeyIndex()).getBuffer(),
            o2.getKeyOffSet(),
            o2.getKeyLength());
      }
    });
    long startCopy =  System.currentTimeMillis();
    sortTime += startCopy - startSort;

    for (Record<K> record : records) {
      offset = writeDataInt(data, offset, record.getKeyLength());
      offset = writeDataInt(data, offset, record.getValueLength());
      int recordLength = record.getKeyLength() + record.getValueLength();
      int copyOffset = record.getKeyOffSet();
      int copyIndex = record.getKeyIndex();
      while (recordLength > 0) {
        byte[] srcBytes = buffers.get(copyIndex).getBuffer();
        int length = copyOffset + recordLength;
        int copyLength = recordLength;
        if (length > srcBytes.length) {
          copyLength = srcBytes.length - copyOffset;
        }
        System.arraycopy(srcBytes, copyOffset, data, offset, copyLength);
        copyOffset = 0;
        copyIndex++;
        recordLength -= copyLength;
        offset += copyLength;
      }
    }
    offset = writeDataInt(data, offset, -1);
    writeDataInt(data, offset, -1);
    copyTime += System.currentTimeMillis() - startCopy;
    return data;
  }

  private boolean compact(int lastIndex, int lastOffset, int dataLength) {
    if (lastIndex != currentIndex) {
      LOG.debug("compact lastIndex {}, currentIndex {}, lastOffset {} currentOffset {} dataLength {}",
          lastIndex, currentIndex, lastOffset, currentOffset, dataLength);
      WrappedBuffer buffer = new WrappedBuffer(lastOffset + dataLength);
      // copy data
      int offset = 0;
      for (int i = lastIndex; i < currentIndex; i++) {
        byte[] sourceBuffer = buffers.get(i).getBuffer();
        System.arraycopy(sourceBuffer, 0, buffer.getBuffer(), offset, sourceBuffer.length);
        offset += sourceBuffer.length;
      }
      System.arraycopy(buffers.get(currentIndex).getBuffer(), 0, buffer.getBuffer(), offset, currentOffset);
      // remove data
      for (int i = currentIndex; i >= lastIndex; i--) {
        buffers.remove(i);
      }
      buffers.add(buffer);
      currentOffset = 0;
      WrappedBuffer anotherBuffer = new WrappedBuffer((int)maxSegmentSize);
      buffers.add(anotherBuffer);
      currentIndex = buffers.size() - 1;
      return true;
    }
    return false;
  }

  private int writeDataInt(byte[] data, int offset, long dataInt) {
    if (dataInt >= -112L && dataInt <= 127L) {
      data[offset] = (byte)((int)dataInt);
      offset++;
    } else {
      int len = -112;
      if (dataInt < 0L) {
        dataInt = ~dataInt;
        len = -120;
      }

      for (long tmp = dataInt; tmp != 0L; --len) {
        tmp >>= 8;
      }

      data[offset] = (byte)len;
      offset++;
      len = len < -120 ? -(len + 120) : -(len + 112);

      for (int idx = len; idx != 0; --idx) {
        int shiftBits = (idx - 1) * 8;
        long mask = 255L << shiftBits;
        data[offset] = ((byte)((int)((dataInt & mask) >> shiftBits)));
        offset++;
      }
    }
    return offset;
  }

  public int getDataLength() {
    return dataLength;
  }

  public long getCopyTime() {
    return copyTime;
  }

  public long getSortTime() {
    return sortTime;
  }

  public int getPartitionId() {
    return partitionId;
  }

  @Override
  public void write(int b) throws IOException {
    if (buffers.isEmpty()) {
      buffers.add(new WrappedBuffer((int) maxSegmentSize));
    }
    if (1 + currentOffset > maxSegmentSize) {
      currentIndex++;
      currentOffset = 0;
      buffers.add(new WrappedBuffer((int) maxSegmentSize));
    }
    WrappedBuffer buffer = buffers.get(currentIndex);
    buffer.getBuffer()[currentOffset] = (byte) b;
    currentOffset++;
    dataLength++;
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (b == null) {
      throw new NullPointerException();
    } else if ((off < 0) || (off > b.length) || (len < 0)
        || ((off + len) > b.length) || ((off + len) < 0)) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {
      return;
    }
    if (buffers.isEmpty()) {
      buffers.add(new WrappedBuffer((int) maxSegmentSize));
    }
    int bufferNum = (int)((currentOffset + len) / maxSegmentSize);
    for (int i = 0; i < bufferNum; i++) {
      buffers.add(new WrappedBuffer((int) maxSegmentSize));
    }
    int index = currentIndex;
    int offset = currentOffset;
    int srcPos = 0;
    while (len > 0) {
      int copyLength = 0;
      if (offset + len >= maxSegmentSize) {
        copyLength = (int) (maxSegmentSize - offset);
        currentOffset = 0;
      } else {
        copyLength = len;
        currentOffset += len;
      }
      System.arraycopy(b, srcPos, buffers.get(index).getBuffer(), offset, copyLength);
      offset = 0;
      srcPos += copyLength;
      index++;
      len -= copyLength;
      dataLength += copyLength;
    }
    currentIndex += bufferNum;
  }

  private static final class Record<K> {

    private final int keyIndex;
    private final int keyOffSet;
    private final int keyLength;
    private final int valueLength;

    Record(int keyIndex,
           int keyOffset,
           int keyLength,
           int valueLength) {
      this.keyIndex = keyIndex;
      this.keyOffSet = keyOffset;
      this.keyLength = keyLength;
      this.valueLength = valueLength;
    }

    public int getKeyIndex() {
      return keyIndex;
    }

    public int getKeyOffSet() {
      return keyOffSet;
    }

    public int getKeyLength() {
      return keyLength;
    }

    public int getValueLength() {
      return valueLength;
    }
  }

  private static final class WrappedBuffer {

    private byte[] buffer;
    private int size;

    WrappedBuffer(int size) {
      this.buffer = new byte[size];
      this.size = size;
    }

    public byte[] getBuffer() {
      return buffer;
    }

    public int getSize() {
      return size;
    }
  }

}
