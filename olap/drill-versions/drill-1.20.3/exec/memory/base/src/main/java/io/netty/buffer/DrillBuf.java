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
package io.netty.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.drill.common.HistoricalLog;
import org.apache.drill.exec.memory.AllocationManager.BufferLedger;
import org.apache.drill.exec.memory.BaseAllocator;
import org.apache.drill.exec.memory.BaseAllocator.Verbosity;
import org.apache.drill.exec.memory.BoundsChecking;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.BufferManager;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.PlatformDependent;

/**
 * Drill data structure for accessing and manipulating data buffers. This class
 * is integrated with the Drill memory management layer for quota enforcement
 * and buffer sharing.
 */
@SuppressWarnings("unused")
public final class DrillBuf extends AbstractByteBuf implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(DrillBuf.class);

  private static final AtomicLong idGenerator = new AtomicLong(0);

  private final long id = idGenerator.incrementAndGet();
  private final AtomicInteger refCnt;
  private final UnsafeDirectLittleEndian udle;
  private final long addr;
  private final int offset;
  private final BufferLedger ledger;
  private final BufferManager bufManager;
  private final boolean isEmpty;
  private volatile int length;
  private final HistoricalLog historicalLog = BaseAllocator.DEBUG ?
      new HistoricalLog(BaseAllocator.DEBUG_LOG_LENGTH, "DrillBuf[%d]", id) : null;

  public DrillBuf(
      final AtomicInteger refCnt,
      final BufferLedger ledger,
      final UnsafeDirectLittleEndian byteBuf,
      final BufferManager manager,
      final ByteBufAllocator alloc,
      final int offset,
      final int length,
      boolean isEmpty) {
    super(byteBuf.maxCapacity());
    this.refCnt = refCnt;
    this.udle = byteBuf;
    this.isEmpty = isEmpty;
    this.bufManager = manager;
    this.addr = byteBuf.memoryAddress() + offset;
    this.ledger = ledger;
    this.length = length;
    this.offset = offset;

    if (BaseAllocator.DEBUG) {
      historicalLog.recordEvent("create()");
    }
  }

  public DrillBuf reallocIfNeeded(final int size) {
    Preconditions.checkArgument(size >= 0, "reallocation size must be non-negative");

    if (this.capacity() >= size) {
      return this;
    }

    if (bufManager != null) {
      return bufManager.replace(this, size);
    } else {
      throw new UnsupportedOperationException("Realloc is only available in the context of an operator's UDFs");
    }
  }

  @Override
  public int refCnt() {
    if (isEmpty) {
      return 1;
    } else {
      return refCnt.get();
    }
  }

  public long addr() { return addr; }

  private long addr(int index) {
    return addr + index;
  }

  private void chk(int index, int width) {
    BoundsChecking.lengthCheck(this, index, width);
  }

  /**
   * Create a new DrillBuf that is associated with an alternative allocator for
   * the purposes of memory ownership and accounting. This has no impact on the
   * reference counting for the current DrillBuf except in the situation where
   * the passed in Allocator is the same as the current buffer.
   *
   * This operation has no impact on the reference count of this DrillBuf. The
   * newly created DrillBuf with either have a reference count of 1 (in the case
   * that this is the first time this memory is being associated with the new
   * allocator) or the current value of the reference count + 1 for the other
   * AllocationManager/BufferLedger combination in the case that the provided
   * allocator already had an association to this underlying memory.
   *
   * @param target
   *          The target allocator to create an association with.
   * @return A new DrillBuf which shares the same underlying memory as this
   *         DrillBuf.
   */
  public DrillBuf retain(BufferAllocator target) {

    if (isEmpty) {
      return this;
    }

    if (BaseAllocator.DEBUG) {
      historicalLog.recordEvent("retain(%s)", target.getName());
    }
    final BufferLedger otherLedger = this.ledger.getLedgerForAllocator(target);
    return otherLedger.newDrillBuf(offset, length, null);
  }

  /**
   * Transfer the memory accounting ownership of this DrillBuf to another
   * allocator. This will generate a new DrillBuf that carries an association
   * with the underlying memory of this DrillBuf. If this DrillBuf is connected
   * to the owning BufferLedger of this memory, that memory ownership/accounting
   * will be transferred to the target allocator. If this DrillBuf does not
   * currently own the memory underlying it (and is only associated with it),
   * this does not transfer any ownership to the newly created DrillBuf.
   * <p>
   * This operation has no impact on the reference count of this DrillBuf. The
   * newly created DrillBuf with either have a reference count of 1 (in the case
   * that this is the first time this memory is being associated with the new
   * allocator) or the current value of the reference count for the other
   * AllocationManager/BufferLedger combination in the case that the provided
   * allocator already had an association to this underlying memory.
   * <p>
   * Transfers will always succeed, even if that puts the other allocator into
   * an overlimit situation. This is possible due to the fact that the original
   * owning allocator may have allocated this memory out of a local reservation
   * whereas the target allocator may need to allocate new memory from a parent
   * or RootAllocator. This operation is done in a mostly-lockless but
   * consistent manner. As such, the overlimit==true situation could occur
   * slightly prematurely to an actual overlimit==true condition. This is simply
   * conservative behavior which means we may return overlimit slightly sooner
   * than is necessary.
   *
   * @param target
   *          The allocator to transfer ownership to.
   * @return A new transfer result with the impact of the transfer (whether it
   *         was overlimit) as well as the newly created DrillBuf.
   */
  public TransferResult transferOwnership(BufferAllocator target) {

    if (isEmpty) {
      return new TransferResult(true, this);
    }

    final BufferLedger otherLedger = this.ledger.getLedgerForAllocator(target);
    final DrillBuf newBuf = otherLedger.newDrillBuf(offset, length, null);
    final boolean allocationFit = this.ledger.transferBalance(otherLedger);
    return new TransferResult(allocationFit, newBuf);
  }

  /**
   * Visible only for memory allocation calculations.
   *
   * @return The {@link BufferLedger} associated with this {@link DrillBuf}.
   */
  public BufferLedger getLedger() { return ledger; }

  /**
   * The outcome of a Transfer.
   */
  public class TransferResult {

    /**
     * Whether this transfer fit within the target allocator's capacity.
     */
    public final boolean allocationFit;

    /**
     * The newly created buffer associated with the target allocator.
     */
    public final DrillBuf buffer;

    private TransferResult(boolean allocationFit, DrillBuf buffer) {
      this.allocationFit = allocationFit;
      this.buffer = buffer;
    }
  }

  @Override
  public boolean release() {
    return release(1);
  }

  /**
   * Release the provided number of reference counts.
   */
  @Override
  public boolean release(int decrement) {

    if (isEmpty) {
      return false;
    }

    if (decrement < 1) {
      throw new IllegalStateException(String.format("release(%d) argument is not positive. Buffer Info: %s",
          decrement, toVerboseString()));
    }

    final int refCnt = ledger.decrement(decrement);

    if (BaseAllocator.DEBUG) {
      historicalLog.recordEvent("release(%d). original value: %d", decrement, refCnt + decrement);
    }

    if (refCnt < 0) {
      throw new IllegalStateException(
          String.format("DrillBuf[%d] refCnt has gone negative. Buffer Info: %s", id, toVerboseString()));
    }
    return refCnt == 0;
  }

  @Override
  public int capacity() {
    return length;
  }

  @Override
  public synchronized DrillBuf capacity(int newCapacity) {

    if (newCapacity == length) {
      return this;
    }

    Preconditions.checkArgument(newCapacity >= 0);

    if (newCapacity < length) {
      length = newCapacity;
      return this;
    }

    throw new UnsupportedOperationException("Buffers don't support resizing that increases the size.");
  }

  @Override
  public ByteBufAllocator alloc() {
    return udle.alloc();
  }

  @Override
  public ByteOrder order() {
    return ByteOrder.LITTLE_ENDIAN;
  }

  @Override
  public ByteBuf order(ByteOrder endianness) {
    return this;
  }

  @Override
  public ByteBuf unwrap() {
    return udle;
  }

  @Override
  public boolean isDirect() {
    return true;
  }

  @Override
  public ByteBuf readBytes(int length) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ByteBuf readSlice(int length) {
    final ByteBuf slice = slice(readerIndex(), length);
    readerIndex(readerIndex() + length);
    return slice;
  }

  @Override
  public ByteBuf copy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ByteBuf copy(int index, int length) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ByteBuf slice() {
    return slice(readerIndex(), readableBytes());
  }

  public static String bufferState(final ByteBuf buf) {
    final int cap = buf.capacity();
    final int mcap = buf.maxCapacity();
    final int ri = buf.readerIndex();
    final int rb = buf.readableBytes();
    final int wi = buf.writerIndex();
    final int wb = buf.writableBytes();
    return String.format("cap/max: %d/%d, ri: %d, rb: %d, wi: %d, wb: %d",
        cap, mcap, ri, rb, wi, wb);
  }

  @Override
  public DrillBuf slice(int index, int length) {

    if (isEmpty) {
      return this;
    }

    /*
     * Re the behavior of reference counting, see http://netty.io/wiki/reference-counted-objects.html#wiki-h3-5, which
     * explains that derived buffers share their reference count with their parent
     */
    final DrillBuf newBuf = ledger.newDrillBuf(offset + index, length);
    newBuf.writerIndex(length);
    return newBuf;
  }

  @Override
  public DrillBuf duplicate() {
    return slice(0, length);
  }

  @Override
  public int nioBufferCount() {
    return 1;
  }

  @Override
  public ByteBuffer nioBuffer() {
    return nioBuffer(readerIndex(), readableBytes());
  }

  @Override
  public ByteBuffer nioBuffer(int index, int length) {
    return udle.nioBuffer(offset + index, length);
  }

  @Override
  public ByteBuffer internalNioBuffer(int index, int length) {
    return udle.internalNioBuffer(offset + index, length);
  }

  @Override
  public ByteBuffer[] nioBuffers() {
    return new ByteBuffer[] { nioBuffer() };
  }

  @Override
  public ByteBuffer[] nioBuffers(int index, int length) {
    return new ByteBuffer[] { nioBuffer(index, length) };
  }

  @Override
  public boolean hasArray() {
    return udle.hasArray();
  }

  @Override
  public byte[] array() {
    return udle.array();
  }

  @Override
  public int arrayOffset() {
    return udle.arrayOffset();
  }

  @Override
  public boolean hasMemoryAddress() {
    return true;
  }

  @Override
  public long memoryAddress() {
    return this.addr;
  }

  @Override
  public String toString() {
    return String.format("DrillBuf[%d], udle: [%d %d..%d]", id, udle.id, offset, offset + capacity());
  }

  @Override
  public String toString(Charset charset) {
    return toString(readerIndex, readableBytes(), charset);
  }

  @Override
  public String toString(int index, int length, Charset charset) {

    if (length == 0) {
      return "";
    }

    return ByteBufUtil.decodeString(this, index, length, charset);
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public boolean equals(Object obj) {
    // identity equals only.
    return this == obj;
  }

  @Override
  public ByteBuf retain(int increment) {
    Preconditions.checkArgument(increment > 0, "retain(%d) argument is not positive", increment);

    if (isEmpty) {
      return this;
    }

    if (BaseAllocator.DEBUG) {
      historicalLog.recordEvent("retain(%d)", increment);
    }

    final int originalReferenceCount = refCnt.getAndAdd(increment);
    Preconditions.checkArgument(originalReferenceCount > 0);
    return this;
  }

  @Override
  public ByteBuf retain() {
    return retain(1);
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
  public long getLong(int index) {
    chk(index, 8);
    return PlatformDependent.getLong(addr(index));
  }

  @Override
  public long getLongLE(int index) {
    chk(index, 8);
    final long var = PlatformDependent.getLong(addr(index));
    return Long.reverseBytes(var);
  }

  @Override
  public float getFloat(int index) {
    return Float.intBitsToFloat(getInt(index));
  }

  @Override
  public double getDouble(int index) {
    return Double.longBitsToDouble(getLong(index));
  }

  @Override
  public char getChar(int index) {
    return (char) getShort(index);
  }

  @Override
  public long getUnsignedInt(int index) {
    return getInt(index) & 0xFFFFFFFFL;
  }

  @Override
  public int getInt(int index) {
    chk(index, 4);
    return PlatformDependent.getInt(addr(index));
  }

  @Override
  public int getIntLE(int index) {
    chk(index, 4);
    final int var = PlatformDependent.getInt(addr(index));
    return Integer.reverseBytes(var);
  }

  @Override
  public int getUnsignedShort(int index) {
    return getShort(index) & 0xFFFF;
  }

  @Override
  public short getShort(int index) {
    chk(index, 2);
    return PlatformDependent.getShort(addr(index));
  }

  @Override
  public short getShortLE(int index) {
    chk(index, 2);
    final short var = PlatformDependent.getShort(addr(index));
    return Short.reverseBytes(var);
  }

  @Override
  public int getUnsignedMedium(int index) {
    final long addr = addr(index);
    return (PlatformDependent.getByte(addr) & 0xff) << 16 |
            (PlatformDependent.getByte(addr + 1) & 0xff) << 8 |
            PlatformDependent.getByte(addr + 2) & 0xff;
  }

  @Override
  public int getUnsignedMediumLE(int index) {
    final long addr = this.addr(index);
    return PlatformDependent.getByte(addr) & 255 |
            (Short.reverseBytes(PlatformDependent.getShort(addr + 1L)) & '\uffff') << 8;
  }

  @Override
  public ByteBuf setShort(int index, int value) {
    chk(index, 2);
    PlatformDependent.putShort(addr(index), (short) value);
    return this;
  }

  @Override
  public ByteBuf setShortLE(int index, int value) {
    chk(index, 2);
    PlatformDependent.putShort(addr(index), Short.reverseBytes((short)value));
    return this;
  }

  @Override
  public ByteBuf setMedium(int index, int value) {
    chk(index, 3);
    long addr = this.addr(index);
    PlatformDependent.putByte(addr, (byte)(value >>> 16));
    PlatformDependent.putShort(addr + 1L, (short)value);
    return this;
  }

  @Override
  public ByteBuf setMediumLE(int index, int value) {
    chk(index, 3);
    long addr = this.addr(index);
    PlatformDependent.putByte(addr, (byte)value);
    PlatformDependent.putShort(addr + 1L, Short.reverseBytes((short)(value >>> 8)));
    return this;
  }

  @Override
  public ByteBuf setInt(int index, int value) {
    chk(index, 4);
    PlatformDependent.putInt(addr(index), value);
    return this;
  }

  @Override
  public ByteBuf setIntLE(int index, int value) {
    chk(index, 4);
    PlatformDependent.putInt(addr(index), Integer.reverseBytes(value));
    return this;
  }

  @Override
  public ByteBuf setLong(int index, long value) {
    chk(index, 8);
    PlatformDependent.putLong(addr(index), value);
    return this;
  }

  @Override
  public ByteBuf setLongLE(int index, long value) {
    chk(index, 4);
    PlatformDependent.putLong(addr(index), Long.reverseBytes(value));
    return this;
  }

  @Override
  public ByteBuf setChar(int index, int value) {
    chk(index, 2);
    PlatformDependent.putShort(addr(index), (short) value);
    return this;
  }

  @Override
  public ByteBuf setFloat(int index, float value) {
    chk(index, 4);
    PlatformDependent.putInt(addr(index), Float.floatToRawIntBits(value));
    return this;
  }

  @Override
  public ByteBuf setDouble(int index, double value) {
    chk(index, 8);
    PlatformDependent.putLong(addr(index), Double.doubleToRawLongBits(value));
    return this;
  }

  @Override
  public ByteBuf writeShort(int value) {
    BoundsChecking.ensureWritable(this, 2);
    PlatformDependent.putShort(addr(writerIndex), (short) value);
    writerIndex += 2;
    return this;
  }

  @Override
  public ByteBuf writeInt(int value) {
    BoundsChecking.ensureWritable(this, 4);
    PlatformDependent.putInt(addr(writerIndex), value);
    writerIndex += 4;
    return this;
  }

  @Override
  public ByteBuf writeLong(long value) {
    BoundsChecking.ensureWritable(this, 8);
    PlatformDependent.putLong(addr(writerIndex), value);
    writerIndex += 8;
    return this;
  }

  @Override
  public ByteBuf writeChar(int value) {
    BoundsChecking.ensureWritable(this, 2);
    PlatformDependent.putShort(addr(writerIndex), (short) value);
    writerIndex += 2;
    return this;
  }

  @Override
  public ByteBuf writeFloat(float value) {
    BoundsChecking.ensureWritable(this, 4);
    PlatformDependent.putInt(addr(writerIndex), Float.floatToRawIntBits(value));
    writerIndex += 4;
    return this;
  }

  @Override
  public ByteBuf writeDouble(double value) {
    BoundsChecking.ensureWritable(this, 8);
    PlatformDependent.putLong(addr(writerIndex), Double.doubleToRawLongBits(value));
    writerIndex += 8;
    return this;
  }

  @Override
  public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
    udle.getBytes(index + offset, dst, dstIndex, length);
    return this;
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuffer dst) {
    udle.getBytes(index + offset, dst);
    return this;
  }

  @Override
  public ByteBuf setByte(int index, int value) {
    chk(index, 1);
    PlatformDependent.putByte(addr(index), (byte) value);
    return this;
  }

  public void setByte(int index, byte b) {
    chk(index, 1);
    PlatformDependent.putByte(addr(index), b);
  }

  public void writeByteUnsafe(byte b) {
    PlatformDependent.putByte(addr(readerIndex), b);
    readerIndex++;
  }

  @Override
  protected byte _getByte(int index) {
    return getByte(index);
  }

  @Override
  protected short _getShort(int index) {
    return getShort(index);
  }

  @Override
  protected short _getShortLE(int index) {
    return getShortLE(index);
  }

  @Override
  protected int _getInt(int index) {
    return getInt(index);
  }

  @Override
  protected int _getIntLE(int index) {
    return getIntLE(index);
  }

  @Override
  protected long _getLong(int index) {
    return getLong(index);
  }

  @Override
  protected long _getLongLE(int index) {
    return getLongLE(index);
  }

  @Override
  protected void _setByte(int index, int value) {
    setByte(index, value);
  }

  @Override
  protected void _setShort(int index, int value) {
    setShort(index, value);
  }

  @Override
  protected void _setShortLE(int index, int value) {
    setShortLE(index, value);
  }

  @Override
  protected void _setMedium(int index, int value) {
    setMedium(index, value);
  }

  @Override
  protected void _setMediumLE(int index, int value) {
    setMediumLE(index, value);
  }

  @Override
  protected void _setInt(int index, int value) {
    setInt(index, value);
  }

  @Override
  protected void _setIntLE(int index, int value) {
    setIntLE(index, value);
  }

  @Override
  protected void _setLong(int index, long value) {
    setLong(index, value);
  }

  @Override
  protected void _setLongLE(int index, long value) {
    setLongLE(index, value);
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
    final int BULK_COPY_THR = 16;
    // Performance profiling indicated that using the "putByte()" method is faster for short
    // data lengths (less than 16 bytes) than using the "copyMemory()" method.
    if (length < BULK_COPY_THR && udle.hasMemoryAddress() && dst.hasMemoryAddress()) {
      if (dst.capacity() < (dstIndex + length)) {
        throw new IndexOutOfBoundsException();
      }
      for (int idx = 0; idx < length; ++idx) {
        byte value = getByte(index + idx);
        PlatformDependent.putByte(dst.memoryAddress() + dstIndex + idx, value);
      }
    } else {
    udle.getBytes(index + offset, dst, dstIndex, length);
    }
    return this;
  }

  @Override
  public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
    udle.getBytes(index + offset, out, length);
    return this;
  }

  @Override
  public int getBytes(int index, FileChannel out, long position, int length) throws IOException {
    return udle.getBytes(index + offset, out, position, length);
  }

  @Override
  protected int _getUnsignedMedium(int index) {
    return getUnsignedMedium(index);
  }

  @Override
  protected int _getUnsignedMediumLE(int index) {
    return getUnsignedMediumLE(index);
  }

  @Override
  public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
    return udle.getBytes(index + offset, out, length);
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
    udle.setBytes(index + offset, src, srcIndex, length);
    return this;
  }

  public ByteBuf setBytes(int index, ByteBuffer src, int srcIndex, int length) {
    if (src.isDirect()) {
      checkIndex(index, length);
      PlatformDependent.copyMemory(PlatformDependent.directBufferAddress(src) + srcIndex, this.memoryAddress() + index,
          length);
    } else {
      if (srcIndex == 0 && src.capacity() == length) {
        udle.setBytes(index + offset, src);
      } else {
        Buffer newBuf = src.duplicate();
        newBuf.position(srcIndex);
        newBuf.limit(srcIndex + length);
        udle.setBytes(index + offset, src);
      }
    }

    return this;
  }

  @Override
  public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
    udle.setBytes(index + offset, src, srcIndex, length);
    return this;
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuffer src) {
    udle.setBytes(index + offset, src);
    return this;
  }

  @Override
  public int setBytes(int index, InputStream in, int length) throws IOException {
    return udle.setBytes(index + offset, in, length);
  }

  @Override
  public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
    return udle.setBytes(index + offset, in, length);
  }

  @Override
  public int setBytes(int index, FileChannel in, long position, int length) throws IOException {
    return udle.setBytes(index + offset, in, position, length);
  }

  @Override
  public byte getByte(int index) {
    chk(index, 1);
    return PlatformDependent.getByte(addr(index));
  }

  @Override
  public void close() {
    release();
  }

  /**
   * Returns the possible memory consumed by this DrillBuf in the worse case scenario. (not shared, connected to larger
   * underlying buffer of allocated memory)
   *
   * @return Size in bytes.
   */
  public int getPossibleMemoryConsumed() {
    return ledger.getSize();
  }

  /**
   * Return that is Accounted for by this buffer (and its potentially shared siblings within the context of the
   * associated allocator).
   *
   * @return Size in bytes.
   */
  public int getActualMemoryConsumed() {
    return ledger.getAccountedSize();
  }

  private final static int LOG_BYTES_PER_ROW = 10;
  private static final char[] HEX_CHAR = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

  /**
   * Return the buffer's byte contents in the form of a hex dump.
   *
   * @param start
   *          the starting byte index
   * @param length
   *          how many bytes to log
   * @return A hex dump in a String.
   */
  public String toHexString(final int start, final int length) {
    Preconditions.checkArgument(start >= 0);
    final StringBuilder sb = new StringBuilder("buffer byte dump");
    final int end = Math.min(length, this.length - start);
    for (int i = 0; i < end; i++) {
      if (i % LOG_BYTES_PER_ROW == 0) {
        sb.append(String.format("%n [%05d-%05d]", i + start, Math.min(i + LOG_BYTES_PER_ROW - 1, end - 1) + start));
      }
      byte b = _getByte(i + start);
      sb.append(" 0x").append(HEX_CHAR[b >> 4]).append(HEX_CHAR[b & 0x0F]);
    }
    if (length > end) {
      sb.append(String.format("%n [%05d-%05d] <ioob>", start + end, start + length));
    }
    return sb.append(System.lineSeparator()).toString();
  }

  /**
   * Get the integer id assigned to this DrillBuf for debugging purposes.
   *
   * @return integer id
   */
  public long getId() {
    return id;
  }

  public String toVerboseString() {
    if (isEmpty) {
      return toString();
    }

    StringBuilder sb = new StringBuilder();
    ledger.print(sb, 0, Verbosity.LOG_WITH_STACKTRACE);
    return sb.toString();
  }

  public void print(StringBuilder sb, int indent, Verbosity verbosity) {
    BaseAllocator.indent(sb, indent).append(toString());

    if (BaseAllocator.DEBUG && !isEmpty && verbosity.includeHistoricalLog) {
      sb.append("\n");
      historicalLog.buildHistory(sb, indent + 1, verbosity.includeStackTraces);
    }
  }

  /**
   * Convenience method to read buffer bytes into a newly allocated byte
   * array.
   *
   * @param srcOffset the offset into this buffer of the data to read
   * @param length number of bytes to read
   * @return byte array with the requested bytes
   */
  public byte[] unsafeGetMemory(int srcOffset, int length) {
    byte buf[] = new byte[length];
    PlatformDependent.copyMemory(addr + srcOffset, buf, 0, length);
    return buf;
  }

  // --------------------------------------------------------------------------
  // Helper Methods for code that needs efficient processing on byte arrays;
  // this should be done only when direct memory cannot be used
  // --------------------------------------------------------------------------

  /** Number of bytes in a long */
  public static final int LONG_NUM_BYTES  = 8;
  /** Number of bytes in an int */
  public static final int INT_NUM_BYTES   = 4;
  /** Number of bytes in a short */
  public static final int SHORT_NUM_BYTES = 2;

  /**
   * @param data source byte array
   * @param index index within the byte array
   * @return short value starting at data+index
   */
  public static short getShort(byte[] data, int index) {
    check(index, SHORT_NUM_BYTES, data.length);
    return PlatformDependent.getShort(data, index);
  }

  /**
   * @param data source byte array
   * @param index index within the byte array
   * @return integer value starting at data+index
   */
  public static int getInt(byte[] data, int index) {
    check(index, INT_NUM_BYTES, data.length);
    return PlatformDependent.getInt(data, index);
  }

  /**
   * @param data data source byte array
   * @param index index within the byte array
   * @return long value read at data_index
   */
  public static long getLong(byte[] data, int index) {
    check(index, LONG_NUM_BYTES, data.length);
    return PlatformDependent.getLong(data, index);
  }

  /**
   * Read a short at position src+srcIndex and copy it to the dest+destIndex
   *
   * @param src source byte array
   * @param srcIndex source index
   * @param dest destination byte array
   * @param destIndex destination index
   */
  public static void putShort(byte[] src, int srcIndex, byte[] dest, int destIndex) {
    check(srcIndex, SHORT_NUM_BYTES, src.length);
    check(destIndex,SHORT_NUM_BYTES, dest.length);

    short value = PlatformDependent.getShort(src, srcIndex);
    PlatformDependent.putShort(dest, destIndex, value);
  }

  /**
   * Read an integer at position src+srcIndex and copy it to the dest+destIndex
   *
   * @param src source byte array
   * @param srcIndex source index
   * @param dest destination byte array
   * @param destIndex destination index
   */
  public static void putInt(byte[] src, int srcIndex, byte[] dest, int destIndex) {
    check(srcIndex, INT_NUM_BYTES, src.length);
    check(destIndex,INT_NUM_BYTES, dest.length);

    int value = PlatformDependent.getInt(src, srcIndex);
    PlatformDependent.putInt(dest, destIndex, value);
  }

  /**
   * Read a long at position src+srcIndex and copy it to the dest+destIndex
   *
   * @param src source byte array
   * @param srcIndex source index
   * @param dest destination byte array
   * @param destIndex destination index
   */
  public static void putLong(byte[] src, int srcIndex, byte[] dest, int destIndex) {
    check(srcIndex, LONG_NUM_BYTES, src.length);
    check(destIndex,LONG_NUM_BYTES, dest.length);

    long value = PlatformDependent.getLong(src, srcIndex);
    PlatformDependent.putLong(dest, destIndex, value);
  }

  /**
   * Copy a short value to the dest+destIndex
   *
   * @param dest destination byte array
   * @param destIndex destination index
   * @param value a short value
   */
  public static void putShort(byte[] dest, int destIndex, short value) {
    check(destIndex, SHORT_NUM_BYTES, dest.length);
    PlatformDependent.putShort(dest, destIndex, value);
  }

  /**
   * Copy an integer value to the dest+destIndex
   *
   * @param dest destination byte array
   * @param destIndex destination index
   * @param value an int value
   */
  public static void putInt(byte[] dest, int destIndex, int value) {
    check(destIndex, INT_NUM_BYTES, dest.length);
    PlatformDependent.putInt(dest, destIndex, value);
  }

  /**
   * Copy a long value to the dest+destIndex
   *
   * @param dest destination byte array
   * @param destIndex destination index
   * @param value a long value
   */
  public static void putLong(byte[] dest, int destIndex, long value) {
    check(destIndex, LONG_NUM_BYTES, dest.length);
    PlatformDependent.putLong(dest, destIndex, value);
  }

  private static void check(int index, int len, int bufferLen) {
    if (BoundsChecking.BOUNDS_CHECKING_ENABLED) {
      if (index < 0 || len < 0) {
        throw new IllegalArgumentException(String.format("index: [%d], len: [%d]", index, len));
      }
      if ((index + len) > bufferLen) {
        throw new IndexOutOfBoundsException(String.format("Trying to access more than buffer length; index: [%d], len: [%d], buffer-len: [%d]", index, len, bufferLen));
      }
    }
  }

}
