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
package org.apache.drill.exec.expr.fn.impl;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

import io.netty.buffer.DrillBuf;

/**
 * A CharSequence is a readable sequence of char values. This interface provides
 * uniform, read-only access to many different kinds of char sequences. A char
 * value represents a character in the Basic Multilingual Plane (BMP) or a
 * surrogate. Refer to Unicode Character Representation for details.<br>
 * Specifically this implementation of the CharSequence adapts a Drill
 * {@link DrillBuf} to the CharSequence. The implementation is meant to be
 * re-used that is allocated once and then passed DrillBuf to adapt. This can be
 * handy to exploit API that consume CharSequence avoiding the need to create
 * string objects.
 *
 */
public class CharSequenceWrapper implements CharSequence {

  // The adapted drill buffer (in the case of US-ASCII)
  private DrillBuf buffer;
  // The converted bytes in the case of non ASCII
  private CharBuffer charBuffer;
  // initial char buffer capacity
  private static final int INITIAL_CHAR_BUF = 1024;
  // The decoder to use in the case of non ASCII
  private CharsetDecoder decoder;

  // The start offset into the drill buffer
  private int start;
  // The end offset into the drill buffer
  private int end;
  // Indicates that the current byte buffer contains only ascii chars
  private boolean usAscii;

  public CharSequenceWrapper() {
  }

  public CharSequenceWrapper(int start, int end, DrillBuf buffer) {
    setBuffer(start, end, buffer);
  }

  @Override
  public int length() {
    return end - start;
  }

  @Override
  public char charAt(int index) {
    if (usAscii) {
      // Each byte is a char, the index is relative to the start of the original buffer
      return (char) (buffer.getByte(start + index) & 0x00FF);
    } else {
      // The char buffer is a copy so the index directly corresponds
      return charBuffer.charAt(index);
    }
  }

  /**
   * When using the Java regex {@link Matcher} the subSequence is only called
   * when capturing groups. Drill does not currently use capture groups in the
   * UDF so this method is not required.<br>
   * It could be implemented by creating a new CharSequenceWrapper however
   * this would imply newly allocated objects which is what this wrapper tries
   * to avoid.
   *
   */
  @Override
  public CharSequence subSequence(int start, int end) {
    throw new UnsupportedOperationException();
  }

  /**
   * Set the DrillBuf to adapt to a CharSequence. This method can be used to
   * replace any previous DrillBuf thus avoiding recreating the
   * CharSequenceWrapper and thus re-using the CharSequenceWrapper object.
   *
   * @param start
   * @param end
   * @param buffer
   */
  public void setBuffer(int start, int end, DrillBuf buffer) {
    // Test if buffer is an ASCII string or not.
    usAscii = isAscii(start, end, buffer);

    if (usAscii) {
      // each byte equals one char
      this.start = start;
      this.end = end;
      this.buffer = buffer;
    } else {
      initCharBuffer();
      // Wrap with java byte buffer
      ByteBuffer byteBuf = buffer.nioBuffer(start, end - start);
      while (charBuffer.capacity() < Integer.MAX_VALUE) {
        byteBuf.mark();
        if (decodeUT8(byteBuf)) {
          break;
        }
        // Failed to convert because the char buffer was not large enough
        growCharBuffer();
        // Make sure to reset the byte buffer we need to reprocess it
        byteBuf.reset();
      }
      this.start = 0;
      this.end = charBuffer.position();
      // reset the char buffer so the index are relative to the start of the buffer
      charBuffer.rewind();
    }
  }

  /**
   * Test if the buffer contains only ASCII bytes.
   * @param start
   * @param end
   * @param buffer
   * @return
   */
  private boolean isAscii(int start, int end, DrillBuf buffer) {
    for (int i = start; i < end; i++) {
      byte bb = buffer.getByte(i);
      if (bb < 0) {
        //System.out.printf("Not a ASCII byte 0x%02X\n", bb);
        return false;
      }
    }
    return true;
  }

  /**
   * Initialize the charbuffer and decoder if they are not yet initialized.
   */
  private void initCharBuffer() {
    if (charBuffer == null) {
      charBuffer = CharBuffer.allocate(INITIAL_CHAR_BUF);
    }
    if (decoder == null) {
      decoder = Charset.forName("UTF-8").newDecoder();
    }
  }

  /**
   * Decode the buffer using the CharsetDecoder.
   * @param byteBuf
   * @return false if failed because the charbuffer was not big enough
   * @throws RuntimeException if it fails for encoding errors
   */
  private boolean decodeUT8(ByteBuffer byteBuf) {
    // We give it all of the input data in call.
    boolean endOfInput = true;
    decoder.reset();
    charBuffer.rewind();
    // Convert utf-8 bytes to sequence of chars
    CoderResult result = decoder.decode(byteBuf, charBuffer, endOfInput);
    if (result.isOverflow()) {
      // Not enough space in the charBuffer.
      return false;
    } else if (result.isError()) {
      // Any other error
      try {
        result.throwException();
      } catch (CharacterCodingException e) {
        throw new RuntimeException(e);
      }
    }
    return true;
  }

  /**
   * Grow the charbuffer making sure not to overflow size integer. Note
   * this grows in the same manner as the ArrayList that is it adds 50%
   * to the current size.
   */
  private void growCharBuffer() {
    // overflow-conscious code
    int oldCapacity = charBuffer.capacity();
    //System.out.println("old capacity " + oldCapacity);
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if (newCapacity < 0) {
      newCapacity = Integer.MAX_VALUE;
    }
    //System.out.println("new capacity " + newCapacity);
    charBuffer = CharBuffer.allocate(newCapacity);
  }

  /**
   * The regexp_replace function is implemented in a way to avoid the call to toString()
   * not to uselessly create a string object.
   */
  @Override
  public String toString() {
    throw new UnsupportedOperationException();
  }

}
