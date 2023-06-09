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
package org.apache.drill.exec.record;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ByteProcessor;
import io.netty.buffer.DrillBuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

public class DeadBuf extends ByteBuf {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DeadBuf.class);

  private static final String ERROR_MESSAGE = "Attemped to access a DeadBuf. This would happen if you attempted to interact with a buffer that has been moved or not yet initialized.";

  public static final DrillBuf DEAD_BUFFER = null;

  private DeadBuf(){}


  @Override
  public boolean isReadable(int size) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public boolean isWritable(int size) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int refCnt() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public boolean release() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public boolean release(int decrement) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int capacity() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf capacity(int newCapacity) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int maxCapacity() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBufAllocator alloc() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteOrder order() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf order(ByteOrder endianness) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf unwrap() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public boolean isDirect() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int readerIndex() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf readerIndex(int readerIndex) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int writerIndex() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writerIndex(int writerIndex) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setIndex(int readerIndex, int writerIndex) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int readableBytes() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int writableBytes() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int maxWritableBytes() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public boolean isReadable() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public boolean isWritable() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf clear() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf markReaderIndex() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf resetReaderIndex() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf markWriterIndex() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf resetWriterIndex() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf discardReadBytes() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf discardSomeReadBytes() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf ensureWritable(int minWritableBytes) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int ensureWritable(int minWritableBytes, boolean force) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public boolean getBoolean(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public byte getByte(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public short getUnsignedByte(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public short getShort(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public short getShortLE(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int getUnsignedShort(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int getUnsignedShortLE(int index) {
      throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int getMedium(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int getMediumLE(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int getUnsignedMedium(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int getUnsignedMediumLE(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int getInt(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int getIntLE(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public long getUnsignedInt(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public long getUnsignedIntLE(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public long getLong(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public long getLongLE(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public CharSequence getCharSequence(int index, int length, Charset charset) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public char getChar(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public float getFloat(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public double getDouble(int index) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int getBytes(int index, FileChannel out, long position, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuf dst) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuf dst, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf getBytes(int index, byte[] dst) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuffer dst) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setBoolean(int index, boolean value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setByte(int index, int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setShort(int index, int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setShortLE(int index, int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setMedium(int index, int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setMediumLE(int index, int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setInt(int index, int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setIntLE(int index, int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setLong(int index, long value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setLongLE(int index, long value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int setCharSequence(int index, CharSequence sequence, Charset charset) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setChar(int index, int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setFloat(int index, float value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setDouble(int index, double value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int setBytes(int index, FileChannel in, long position, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuf src) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuf src, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setBytes(int index, byte[] src) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuffer src) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int setBytes(int index, InputStream in, int length) throws IOException {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf setZero(int index, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);

  }

  @Override
  public boolean readBoolean() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);

  }

  @Override
  public byte readByte() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);

  }

  @Override
  public short readUnsignedByte() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);

  }

  @Override
  public short readShort() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public short readShortLE() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int readUnsignedShort() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);

  }

  @Override
  public int readUnsignedShortLE() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int readMedium() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int readMediumLE() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int readUnsignedMedium() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int readUnsignedMediumLE() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int readInt() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int readIntLE() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public long readUnsignedInt() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public long readUnsignedIntLE() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public long readLong() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public long readLongLE() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public char readChar() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public CharSequence readCharSequence(int length, Charset charset) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public float readFloat() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);

  }

  @Override
  public double readDouble() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int readBytes(FileChannel out, long position, int length) throws IOException {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf readBytes(int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf readSlice(int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf readBytes(ByteBuf dst) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf readBytes(ByteBuf dst, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf readBytes(byte[] dst) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf readBytes(ByteBuffer dst) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf readBytes(OutputStream out, int length) throws IOException {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int readBytes(GatheringByteChannel out, int length) throws IOException {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf skipBytes(int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeBoolean(boolean value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeByte(int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeShort(int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeShortLE(int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeMedium(int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeMediumLE(int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeInt(int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeIntLE(int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeLong(long value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeLongLE(long value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeChar(int value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeFloat(float value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeDouble(double value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int writeBytes(FileChannel in, long position, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int writeCharSequence(CharSequence sequence, Charset charset) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeBytes(ByteBuf src) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeBytes(ByteBuf src, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeBytes(byte[] src) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeBytes(byte[] src, int srcIndex, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeBytes(ByteBuffer src) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int writeBytes(InputStream in, int length) throws IOException {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf writeZero(int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int indexOf(int fromIndex, int toIndex, byte value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }


  @Override
  public int bytesBefore(byte value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

    @Override
  public int bytesBefore(int length, byte value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }


  @Override
  public int bytesBefore(int index, int length, byte value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }


  @Override
  public ByteBuf copy() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf copy(int index, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf readRetainedSlice(int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf slice() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf retainedSlice() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf slice(int index, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf retainedSlice(int index, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf retainedDuplicate() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf duplicate() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int nioBufferCount() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuffer nioBuffer() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuffer nioBuffer(int index, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuffer[] nioBuffers() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuffer[] nioBuffers(int index, int length) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public boolean hasArray() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public byte[] array() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int arrayOffset() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public boolean hasMemoryAddress() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public long memoryAddress() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public String toString(Charset charset) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public String toString(int index, int length, Charset charset) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public int compareTo(ByteBuf buffer) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf retain(int increment) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf retain() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public boolean isReadOnly() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public ByteBuf asReadOnly() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public boolean equals(Object arg0) {
    return false;
  }

  @Override
  public ByteBuf touch() {
    return this;
  }

  @Override
  public ByteBuf touch(Object hint) {
    return this;
  }

  @Override
  public int forEachByte(ByteProcessor arg0) {
    return 0;
  }


  @Override
  public int forEachByte(int arg0, int arg1, ByteProcessor arg2) {
    return 0;
  }


  @Override
  public int forEachByteDesc(ByteProcessor arg0) {
    return 0;
  }


  @Override
  public int forEachByteDesc(int arg0, int arg1, ByteProcessor arg2) {
    return 0;
  }


  @Override
  public int hashCode() {
    return 0;
  }


  @Override
  public ByteBuffer internalNioBuffer(int arg0, int arg1) {
    return null;
  }


  @Override
  public String toString() {
    return null;
  }


}
