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
package org.apache.drill.exec.store.easy.text.reader;

import static org.apache.drill.exec.memory.BoundsChecking.rangeCheck;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.hadoop.fs.ByteBufferReadable;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Seekable;
import org.apache.hadoop.io.compress.CompressionInputStream;

import io.netty.buffer.DrillBuf;
import io.netty.util.internal.PlatformDependent;

/**
 * Class that fronts an InputStream to provide a byte consumption interface.
 * Also manages only reading lines to and from each split.
 */
final class TextInput {

  private final byte[] lineSeparator;
  private final byte normalizedLineSeparator;
  private final TextParsingSettings settings;

  private long lineCount;
  private long charCount;

  /**
   * Starting position in the file.
   */
  private final long startPos;
  private final long endPos;

  private long streamPos;

  private final Seekable seekable;
  private final FSDataInputStream inputFS;
  private final InputStream input;

  private final DrillBuf buffer;
  private final ByteBuffer underlyingBuffer;
  private final long bStart;
  private final long bStartMinus1;

  private final boolean bufferReadable;

  /**
   * Whether there was a possible partial line separator on the previous
   * read so we dropped it and it should be appended to next read.
   */
  private int remByte = -1;

  /**
   * Current position in the buffer.
   */
  private int bufferPtr;

  /**
   * Length of valid data in the buffer.
   */
  private int length = -1;

  private boolean endFound;

  /**
   * Creates a new instance with the mandatory characters for handling newlines
   * transparently. lineSeparator the sequence of characters that represent a
   * newline, as defined in {@link TextParsingSettings#getNewLineDelimiter()}
   * normalizedLineSeparator the normalized newline character (as defined in
   * {@link TextParsingSettings#getNormalizedNewLine()}) that is used to replace any
   * lineSeparator sequence found in the input.
   */
  // TODO: Remove the DrillBuf; we're getting no benefit from the round trip
  // out to direct memory and back.
  TextInput(TextParsingSettings settings, InputStream input, DrillBuf readBuffer, long startPos, long endPos) {
    this.lineSeparator = settings.getNewLineDelimiter();
    byte normalizedLineSeparator = settings.getNormalizedNewLine();
    Preconditions.checkArgument(input instanceof Seekable, "Text input only supports an InputStream that supports Seekable.");
    boolean isCompressed = input instanceof CompressionInputStream;
    Preconditions.checkArgument(!isCompressed || startPos == 0, "Cannot use split on compressed stream.");

    // splits aren't allowed with compressed data.  The split length will be the
    // compressed size which means we'll normally end prematurely.
    if (isCompressed && endPos > 0) {
      endPos = Long.MAX_VALUE;
    }

    this.input = input;
    this.seekable = (Seekable) input;
    this.settings = settings;

    if (input instanceof FSDataInputStream) {
      this.inputFS = (FSDataInputStream) input;
      this.bufferReadable = inputFS.getWrappedStream() instanceof ByteBufferReadable;
    } else {
      this.inputFS = null;
      this.bufferReadable = false;
    }

    this.startPos = startPos;
    this.endPos = endPos;

    this.normalizedLineSeparator = normalizedLineSeparator;

    this.buffer = readBuffer;
    this.bStart = buffer.memoryAddress();
    this.bStartMinus1 = bStart -1;
    this.underlyingBuffer = buffer.nioBuffer(0, buffer.capacity());
  }

  /**
   * Test the input to position for read start.  If the input is a non-zero split or
   * splitFirstLine is enabled, input will move to appropriate complete line.
   * @throws IOException for input file read errors
   */
  final void start() throws IOException {
    lineCount = 0;
    if(startPos > 0){
      seekable.seek(startPos);
    }

    updateBuffer();
    if (length > 0) {
      if (startPos > 0 || settings.isSkipFirstLine()) {

        // move to next full record.
        try {
          skipLines(1);
        } catch (StreamFinishedPseudoException e) {
          // file does not have any more lines, ignore
        }
      }
    }
  }

  /**
   * Helper method to get the most recent characters consumed since the last record started.
   * May get an incomplete string since we don't support stream rewind.  Returns empty string for now.
   *
   * @return String of last few bytes.
   */
  public String getStringSinceMarkForError() {
    return " ";
  }

  long getPos() {
    return streamPos + bufferPtr;
  }

  public void mark() { }

  /**
   * Read some more bytes from the stream.  Uses the zero copy interface if available.
   * Otherwise, does byte copy.
   *
   * @throws IOException for input file read errors
   */
  private void read() throws IOException {
    if (bufferReadable) {

      if (remByte != -1) {
        for (int i = 0; i <= remByte; i++) {
          underlyingBuffer.put(lineSeparator[i]);
        }
        remByte = -1;
      }
      length = inputFS.read(underlyingBuffer);

    } else {
      byte[] b = new byte[underlyingBuffer.capacity()];
      if (remByte != -1){
        int remBytesNum = remByte + 1;
        System.arraycopy(lineSeparator, 0, b, 0, remBytesNum);
        length = input.read(b, remBytesNum, b.length - remBytesNum);
        remByte = -1;
      } else {
        length = input.read(b);
      }
      underlyingBuffer.put(b);
    }
  }

  /**
   * Read more data into the buffer. Will also manage split end conditions.
   *
   * @throws IOException for input file read errors
   */
  private void updateBuffer() throws IOException {
    streamPos = seekable.getPos();
    underlyingBuffer.clear();

    if (endFound) {
      length = -1;
      return;
    }

    read();

    // check our data read allowance.
    if (streamPos + length >= this.endPos) {
      updateLengthBasedOnConstraint();
    }

    charCount += bufferPtr;
    bufferPtr = 1;

    buffer.writerIndex(underlyingBuffer.limit());
    buffer.readerIndex(underlyingBuffer.position());
  }

  /**
   * Checks to see if we can go over the end of our bytes constraint on the data. If so,
   * adjusts so that we can only read to the last character of the first line that crosses
   * the split boundary.
   */
  private void updateLengthBasedOnConstraint() {
    final long max = bStart + length;
    for (long m = bStart + (endPos - streamPos); m < max; m++) {
      for (int i = 0; i < lineSeparator.length; i++) {
        long mPlus = m + i;
        if (mPlus < max) {
          // we found a line separator and don't need to consult the next byte.
          if (lineSeparator[i] == PlatformDependent.getByte(mPlus) && i == lineSeparator.length - 1) {
            length = (int) (mPlus - bStart) + 1;
            endFound = true;
            return;
          }
        } else {
          // the last N characters of the read were remnant bytes. We'll hold off on dealing with these bytes until the next read.
          remByte = i;
          length = length - i;
          return;
        }
      }
    }
  }

  /**
   * Get next byte from stream.  Also maintains the current line count.  Will throw a
   * {@link StreamFinishedPseudoException} when the stream has run out of bytes.
   *
   * @return next byte from stream.
   * @throws IOException for input file read errors
   */
  public final byte nextChar() throws IOException {
    byte byteChar = nextCharNoNewLineCheck();
    int bufferPtrTemp = bufferPtr - 1;
    if (byteChar == lineSeparator[0]) {
      for (int i = 1; i < lineSeparator.length; i++, bufferPtrTemp++) {
        if (lineSeparator[i] != buffer.getByte(bufferPtrTemp)) {
          return byteChar;
        }
      }

      lineCount++;
      byteChar = normalizedLineSeparator;

      // we don't need to update buffer position if line separator is one byte long
      if (lineSeparator.length > 1) {
        bufferPtr += (lineSeparator.length - 1);
        if (bufferPtr >= length) {
          if (length != -1) {
            updateBuffer();
          } else {
            throw StreamFinishedPseudoException.INSTANCE;
          }
        }
      }
    }

    return byteChar;
  }

  /**
   * Get next byte from stream.  Do no maintain any line count  Will throw a StreamFinishedPseudoException
   * when the stream has run out of bytes.
   *
   * @return next byte from stream.
   * @throws IOException for input file read errors
   */
  public final byte nextCharNoNewLineCheck() throws IOException {

    if (length == -1) {
      throw StreamFinishedPseudoException.INSTANCE;
    }

    rangeCheck(buffer, bufferPtr - 1, bufferPtr);

    byte byteChar = PlatformDependent.getByte(bStartMinus1 + bufferPtr);

    if (bufferPtr >= length) {
      if (length != -1) {
        updateBuffer();
        bufferPtr--;
      } else {
        throw StreamFinishedPseudoException.INSTANCE;
      }
    }

    bufferPtr++;
    return byteChar;
  }

  /**
   * Number of lines read since the start of this split.
   * @return current line count
   */
  public final long lineCount() {
    return lineCount;
  }

  /**
   * Skip forward the number of line delimiters. If you are in the middle of a line,
   * a value of 1 will skip to the start of the next record.
   *
   * @param lines Number of lines to skip.
   * @throws IOException for input file read errors
   * @throws IllegalArgumentException if unable to skip the requested number
   * of lines
   */
  public final void skipLines(int lines) throws IOException {
    if (lines < 1) {
      return;
    }
    long expectedLineCount = this.lineCount + lines;

    try {
      do {
        nextChar();
      } while (lineCount < expectedLineCount);
      if (lineCount < lines) {
        throw new IllegalArgumentException("Unable to skip " + lines + " lines from line " + (expectedLineCount - lines) + ". End of input reached");
      }
    } catch (EOFException ex) {
      throw new IllegalArgumentException("Unable to skip " + lines + " lines from line " + (expectedLineCount - lines) + ". End of input reached");
    }
  }

  public final long charCount() {
    return charCount + bufferPtr;
  }

  public void close() throws IOException{
    input.close();
  }
}
