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
package org.apache.drill.exec.util;

import org.apache.drill.shaded.guava.com.google.common.math.BigIntegerMath;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.DrillBuf;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public class DecimalUtility {

  public final static int MAX_DIGITS = 9;
  public final static int MAX_DIGITS_INT = 10;
  public final static int MAX_DIGITS_BIGINT = 19;
  public final static int DIGITS_BASE = 1000000000;
  public final static int INTEGER_SIZE = Integer.SIZE / 8;

  private static final Logger logger = LoggerFactory.getLogger(DecimalUtility.class);

  /**
   * Given the number of actual digits this function returns the
   * number of indexes it will occupy in the array of integers
   * which are stored in base 1 billion
   */
  public static int roundUp(int ndigits) {
    return (ndigits + MAX_DIGITS - 1) / MAX_DIGITS;
  }

  /**
   * Create a BigDecimal object using the data in the DrillBuf.
   * This function assumes that data is provided in a sparse format.
   */
  public static BigDecimal getBigDecimalFromSparse(DrillBuf data, int startIndex, int nDecimalDigits, int scale) {
    // In the sparse representation we pad the scale with zeroes for ease of arithmetic, need to truncate
    return getBigDecimalFromDrillBuf(data, startIndex, nDecimalDigits, scale, true);
  }

  /**
   * Create a BigDecimal object using the data in the DrillBuf.
   * This function assumes that data is provided in format supported by {@link BigInteger}.
   */
  public static BigDecimal getBigDecimalFromDrillBuf(DrillBuf bytebuf, int start, int length, int scale) {
    byte[] value = new byte[length];
    bytebuf.getBytes(start, value, 0, length);
    BigInteger unscaledValue = length == 0 ? BigInteger.ZERO : new BigInteger(value);
    return new BigDecimal(unscaledValue, scale);
  }

  /**
   * Create a BigDecimal object using the data in the DrillBuf.
   * This function assumes that data is provided in a non-dense format
   * It works on both sparse and intermediate representations.
   */
  public static BigDecimal getBigDecimalFromDrillBuf(ByteBuf data, int startIndex, int nDecimalDigits, int scale,
      boolean truncateScale) {

    // For sparse decimal type we have padded zeroes at the end, strip them while converting to BigDecimal.
    int actualDigits = scale % MAX_DIGITS;

    // Initialize the BigDecimal, first digit in the DrillBuf has the sign so mask it out
    BigInteger decimalDigits = BigInteger.valueOf(data.getInt(startIndex) & 0x7FFFFFFF);

    BigInteger base = BigInteger.valueOf(DIGITS_BASE);

    for (int i = 1; i < nDecimalDigits; i++) {
      BigInteger temp = BigInteger.valueOf(data.getInt(startIndex + i * INTEGER_SIZE));
      decimalDigits = decimalDigits.multiply(base);
      decimalDigits = decimalDigits.add(temp);
    }

    // Truncate any additional padding we might have added
    if (truncateScale && scale > 0 && actualDigits != 0) {
      BigInteger truncate = BigInteger.valueOf((int) Math.pow(10, MAX_DIGITS - actualDigits));
      decimalDigits = decimalDigits.divide(truncate);
    }

    // set the sign
    if ((data.getInt(startIndex) & 0x80000000) != 0) {
      decimalDigits = decimalDigits.negate();
    }

    return new BigDecimal(decimalDigits, scale);
  }

  /**
   * Returns a BigDecimal object from the dense decimal representation.
   * First step is to convert the dense representation into an intermediate representation
   * and then invoke getBigDecimalFromDrillBuf() to get the BigDecimal object
   */
  public static BigDecimal getBigDecimalFromDense(DrillBuf data, int startIndex, int nDecimalDigits,
                                                  int scale, int maxPrecision, int width) {

    /* This method converts the dense representation to
     * an intermediate representation. The intermediate
     * representation has one more integer than the dense
     * representation.
     */
    byte[] intermediateBytes = new byte[(nDecimalDigits + 1) * INTEGER_SIZE];

    // Start storing from the least significant byte of the first integer
    int intermediateIndex = 3;

    int[] mask = {0x03, 0x0F, 0x3F, 0xFF};
    int[] reverseMask = {0xFC, 0xF0, 0xC0, 0x00};

    int maskIndex;
    int shiftOrder;
    byte shiftBits;

    if (maxPrecision == 38) {
      maskIndex = 0;
      shiftOrder = 6;
      shiftBits = 0x00;
      intermediateBytes[intermediateIndex++] = (byte) (data.getByte(startIndex) & 0x7F);
    } else if (maxPrecision == 28) {
      maskIndex = 1;
      shiftOrder = 4;
      shiftBits = (byte) ((data.getByte(startIndex) & 0x03) << shiftOrder);
      intermediateBytes[intermediateIndex++] = (byte) (((data.getByte(startIndex) & 0x3C) & 0xFF) >>> 2);
    } else {
      throw new UnsupportedOperationException("Dense types with max precision 38 and 28 are only supported");
    }

    int inputIndex = 1;
    boolean sign = false;

    if ((data.getByte(startIndex) & 0x80) != 0) {
      sign = true;
    }

    while (inputIndex < width) {
      intermediateBytes[intermediateIndex] = (byte) ((shiftBits) | (((data.getByte(startIndex + inputIndex) & reverseMask[maskIndex]) & 0xFF) >>> (8 - shiftOrder)));
      shiftBits = (byte) ((data.getByte(startIndex + inputIndex) & mask[maskIndex]) << shiftOrder);

      inputIndex++;
      intermediateIndex++;

      if (((inputIndex - 1) % INTEGER_SIZE) == 0) {
        shiftBits = (byte) ((shiftBits & 0xFF) >>> 2);
        maskIndex++;
        shiftOrder -= 2;
      }
    }
    /* copy the last byte */
    intermediateBytes[intermediateIndex] = shiftBits;

    if (sign) {
      intermediateBytes[0] = (byte) (intermediateBytes[0] | 0x80);
    }

    final ByteBuf intermediate = UnpooledByteBufAllocator.DEFAULT.buffer(intermediateBytes.length);
    try {
      intermediate.setBytes(0, intermediateBytes);

      // In the intermediate representation we don't pad the scale with zeroes, so set truncate = false
      return getBigDecimalFromDrillBuf(intermediate, 0, nDecimalDigits + 1, scale, false);
    } finally {
      intermediate.release();
    }
  }

  /**
   * Function converts the BigDecimal and stores it in out internal sparse representation
   */
  public static void getSparseFromBigDecimal(BigDecimal input, ByteBuf data, int startIndex, int scale, int nDecimalDigits) {

    // Initialize the buffer
    data.setZero(startIndex, nDecimalDigits * INTEGER_SIZE);

    boolean sign = false;

    if (input.signum() == -1) {
      // negative input
      sign = true;
      input = input.abs();
    }

    // Truncate the input as per the scale provided
    input = input.setScale(scale, BigDecimal.ROUND_HALF_UP);

    // Separate out the integer part
    BigDecimal integerPart = input.setScale(0, BigDecimal.ROUND_DOWN);

    int destIndex = nDecimalDigits - roundUp(scale) - 1;

    // we use base 1 billion integer digits for out internal representation
    BigDecimal base = new BigDecimal(DIGITS_BASE);

    while (integerPart.compareTo(BigDecimal.ZERO) == 1) {
      // store the modulo as the integer value
      data.setInt(startIndex + destIndex * INTEGER_SIZE, integerPart.remainder(base).intValue());
      destIndex--;
      // Divide by base 1 billion
      integerPart = integerPart.divide(base, BigDecimal.ROUND_DOWN).setScale(0, BigDecimal.ROUND_DOWN);
    }

    /* Sparse representation contains padding of additional zeroes
     * so each digit contains MAX_DIGITS for ease of arithmetic
     */
    int actualDigits = scale % MAX_DIGITS;
    if (actualDigits != 0) {
      // Pad additional zeroes
      scale = scale + MAX_DIGITS - actualDigits;
      input = input.setScale(scale, BigDecimal.ROUND_DOWN);
    }

    //separate out the fractional part
    BigDecimal fractionalPart = input.remainder(BigDecimal.ONE).movePointRight(scale);

    destIndex = nDecimalDigits - 1;

    while (scale > 0) {
      // Get next set of MAX_DIGITS (9) store it in the DrillBuf
      fractionalPart = fractionalPart.movePointLeft(MAX_DIGITS);
      BigDecimal temp = fractionalPart.remainder(BigDecimal.ONE);

      data.setInt(startIndex + destIndex * INTEGER_SIZE, temp.unscaledValue().intValue());
      destIndex--;

      fractionalPart = fractionalPart.setScale(0, BigDecimal.ROUND_DOWN);
      scale -= MAX_DIGITS;
    }

    // Set the negative sign
    if (sign) {
      data.setInt(startIndex, data.getInt(startIndex) | 0x80000000);
    }
  }

  /**
   * Returns unsigned long value taken from specified {@link BigDecimal} input with specified scale
   *
   * @param input {@link BigDecimal} with desired value
   * @param scale scale of the value
   * @return long value taken from specified {@link BigDecimal}
   */
  public static long getDecimal18FromBigDecimal(BigDecimal input, int scale) {
    // Truncate or pad to set the input to the correct scale
    input = input.setScale(scale, BigDecimal.ROUND_HALF_UP);

    return input.unscaledValue().longValue();
  }

  /**
   * Returns unsigned int value taken from specified {@link BigDecimal} input with specified scale.
   *
   * @param input {@link BigDecimal} with desired value
   * @param scale scale of the value
   * @return int value taken from specified {@link BigDecimal}
   */
  public static int getDecimal9FromBigDecimal(BigDecimal input, int scale) {
    // Truncate or pad to set the input to the correct scale
    input = input.setScale(scale, BigDecimal.ROUND_HALF_UP);

    return input.unscaledValue().intValue();
  }

  /**
   * Returns {@link BigDecimal} value created from specified integer value with specified scale.
   *
   * @param input integer value to use for creating of {@link BigDecimal}
   * @param scale scale for resulting {@link BigDecimal}
   * @return {@link BigDecimal} value
   */
  public static BigDecimal getBigDecimalFromPrimitiveTypes(int input, int scale) {
    return BigDecimal.valueOf(input, scale);
  }

  /**
   * Returns {@link BigDecimal} value created from specified long value with specified scale.
   *
   * @param input long value to use for creating of {@link BigDecimal}
   * @param scale scale for resulting {@link BigDecimal}
   * @return {@link BigDecimal} value
   */
  public static BigDecimal getBigDecimalFromPrimitiveTypes(long input, int scale) {
    return BigDecimal.valueOf(input, scale);
  }

  /**
   * Compares two VarDecimal values, still stored in their respective Drill buffers
   *
   * @param left       left value Drill buffer
   * @param leftStart  start offset of left value
   * @param leftEnd    end offset of left value
   * @param leftScale  scale of left value
   * @param right      right value Drill buffer
   * @param rightStart start offset of right value
   * @param rightEnd   end offset of right value
   * @param rightScale scale of right value
   * @param absCompare comparison of absolute values is done iff this is true
   * @return 1 if left > right, 0 if left = right, -1 if left < right.  two values that are numerically equal, but with different
   * scales (e.g., 2.00 and 2), are considered equal.
   */
  public static int compareVarLenBytes(DrillBuf left, int leftStart, int leftEnd, int leftScale, DrillBuf right, int rightStart, int rightEnd, int rightScale, boolean absCompare) {
    byte[] rightBytes = new byte[rightEnd - rightStart];
    right.getBytes(rightStart, rightBytes, 0, rightEnd - rightStart);

    return compareVarLenBytes(left, leftStart, leftEnd, leftScale, rightBytes, rightScale, absCompare);
  }

  /**
   * Compares two VarDecimal values, still stored in Drill buffer and byte array
   *
   * @param left       left value Drill buffer
   * @param leftStart  start offset of left value
   * @param leftEnd    end offset of left value
   * @param leftScale  scale of left value
   * @param right      right value byte array
   * @param rightScale scale of right value
   * @param absCompare comparison of absolute values is done iff this is true
   * @return 1 if left > right, 0 if left = right, -1 if left < right.  two values that are numerically equal, but with different
   * scales (e.g., 2.00 and 2), are considered equal.
   */
  public static int compareVarLenBytes(DrillBuf left, int leftStart, int leftEnd, int leftScale, byte[] right, int rightScale, boolean absCompare) {
    java.math.BigDecimal bdLeft = getBigDecimalFromDrillBuf(left, leftStart, leftEnd - leftStart, leftScale);
    java.math.BigDecimal bdRight = new BigDecimal(right.length == 0 ? BigInteger.ZERO : new BigInteger(right), rightScale);
    if (absCompare) {
      bdLeft = bdLeft.abs();
      bdRight = bdRight.abs();
    }
    return bdLeft.compareTo(bdRight);
  }

  /**
   * Returns max length of byte array, required to store value with specified precision.
   *
   * @param precision the precision of value
   *
   * @return max length of byte array
   */
  public static int getMaxBytesSizeForPrecision(int precision) {
    if (precision == 0) {
      return 0;
    }
    if (precision < 300) { // normal case, use exact heuristic formula
      // Math.pow(10, precision) - 1 returns max integer value for the specified precision, example 999 for precision 3
      // Math.log(Math.pow(10, precision) - 1) / Math.log(2) is just log with base 2 to calculate size
      // in bits for max integer value mentioned above
      // Math.log(Math.pow(10, precision) - 1) / Math.log(2) + 1 in this expression was added 1 to the count of bits to
      // take into account case with negative value
      // size in bits was divided by size of byte to calculate bytes count required to store value
      // with the specified precision
      return (int) Math.ceil((Math.log(Math.pow(10, precision) - 1) / Math.log(2) + 1) / Byte.SIZE);
    } else {
      // for values greater than 304 Math.pow(10, precision) returns Infinity, therefore 1 is neglected in Math.log()
      return (int) Math.ceil((precision * Math.log(10) / Math.log(2) + 1) / Byte.SIZE);
    }
  }

  /**
   * Calculates and returns square root for specified BigDecimal
   * with specified number of digits alter decimal point.
   *
   * @param in    BigDecimal which square root should be calculated
   * @param scale number of digits alter decimal point in the result value.
   * @return square root for specified BigDecimal
   */
  public static BigDecimal sqrt(BigDecimal in, int scale) {
    // unscaled BigInteger value from specified BigDecimal with doubled number of digits after decimal point
    // was used to calculate sqrt using Guava's BigIntegerMath.
    BigInteger valueWithDoubleMaxPrecision =
        in.multiply(BigDecimal.TEN.pow(scale * 2)).setScale(0, RoundingMode.HALF_UP).unscaledValue();
    return new BigDecimal(
        BigIntegerMath.sqrt(valueWithDoubleMaxPrecision, RoundingMode.HALF_UP), scale);
  }

  /**
   * Checks that specified decimal minorType is obsolete.
   *
   * @param minorType type to check
   * @return true if specified decimal minorType is obsolete.
   */
  public static boolean isObsoleteDecimalType(TypeProtos.MinorType minorType) {
    return minorType == TypeProtos.MinorType.DECIMAL9 ||
        minorType == TypeProtos.MinorType.DECIMAL18 ||
        minorType == TypeProtos.MinorType.DECIMAL28SPARSE ||
        minorType == TypeProtos.MinorType.DECIMAL38SPARSE;
  }

  /**
   * Returns default precision for specified {@link org.apache.drill.common.types.TypeProtos.MinorType}
   * or returns specified defaultPrecision if {@link org.apache.drill.common.types.TypeProtos.MinorType} isn't
   * {@link org.apache.drill.common.types.TypeProtos.MinorType#INT} or {@link org.apache.drill.common.types.TypeProtos.MinorType#BIGINT}.
   *
   * @param minorType        type wich precision should be received
   * @param defaultPrecision default value for precision
   * @return default precision for specified {@link org.apache.drill.common.types.TypeProtos.MinorType}
   */
  public static int getDefaultPrecision(TypeProtos.MinorType minorType, int defaultPrecision) {
    switch (minorType) {
      case INT:
        return MAX_DIGITS_INT;
      case BIGINT:
        return MAX_DIGITS_BIGINT;
      default:
        return defaultPrecision;
    }
  }

  /**
   * Checks that the specified value may be fit into the value with specified
   * {@code desiredPrecision} precision and {@code desiredScale} scale.
   * Otherwise, the exception is thrown.
   *
   * @param value            BigDecimal value to check
   * @param desiredPrecision precision for the resulting value
   * @param desiredScale     scale for the resulting value
   */
  public static void checkValueOverflow(BigDecimal value, int desiredPrecision, int desiredScale) {
    if (value.precision() - value.scale() > desiredPrecision - desiredScale) {
      throw UserException.validationError()
          .message("Value %s overflows specified precision %s with scale %s.",
              value, desiredPrecision, desiredScale)
          .build(logger);
    }
  }
}
