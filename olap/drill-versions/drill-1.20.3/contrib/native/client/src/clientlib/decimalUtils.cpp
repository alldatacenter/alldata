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
#include <vector>
#include "drill/recordBatch.hpp"
#include "drill/decimalUtils.hpp"

#ifdef _WIN32
#define bswap_16                    _byteswap_ushort
#define bswap_32                    _byteswap_ulong
#define bswap_64                    _byteswap_uint64
#elif defined(__linux__)
#include <byteswap.h>   //for bswap_16,32,64
#elif defined(__APPLE__)
#include <libkern/OSByteOrder.h>
#define bswap_16 OSSwapInt16
#define bswap_32 OSSwapInt32
#define bswap_64 OSSwapInt64
#endif

using namespace boost::multiprecision;

#define MAX_DIGITS 9
#define INTEGER_SIZE sizeof(uint32_t)
#define DIGITS_BASE 1000000000

namespace Drill
{

// Code is ported from Java DecimalUtility.java implementation.
// The big differences are that we expose a DecimalValue struct,
// We store the unscaled value in a boost cpp_int
// And that when we retrieve integer data off the wire, we need to do an endianness swap on.
// We only do the endianness swap on dense types.
// on little-endian platforms.

DecimalValue getDecimalValueFromByteBuf(SlicedByteBuf& data, size_t startIndex, int nDecimalDigits, int scale, bool truncateScale)
{

    // For sparse decimal type we have padded zeroes at the end, strip them while converting to BigDecimal.
    int32_t actualDigits;

    // Indicate if we need an endianness swap (truncateScale = true for sparse types, which don't need an endianness swap).
    bool needsEndiannessSwap = !truncateScale;

    // Initialize the BigDecimal, first digit in the ByteBuf has the sign so mask it out
    cpp_int decimalDigits = (needsEndiannessSwap ?
            bswap_32(data.getUint32(startIndex)) & 0x7FFFFFFF :
            (data.getUint32(startIndex) & 0x7FFFFFFF));

    cpp_int base(DIGITS_BASE);

    for (int i = 1; i < nDecimalDigits; i++) {

        // Note: we need to byteswap when we retrieve integers off the wire since they are
        // stored in big-endian format.
        cpp_int temp = (needsEndiannessSwap ?
                bswap_32(data.getUint32(startIndex + (i * INTEGER_SIZE))) :
                (data.getUint32(startIndex + (i * INTEGER_SIZE))));

        decimalDigits *= base;
        decimalDigits += temp;
    }

    // Truncate any additional padding we might have added
    if (truncateScale && scale > 0 && (actualDigits = scale % MAX_DIGITS) != 0) {
        cpp_int truncate = (int32_t) std::pow(10.0, (MAX_DIGITS - actualDigits));
        decimalDigits /= truncate;
    }

    DecimalValue val;
    val.m_unscaledValue = decimalDigits;

    // set the sign
    if ((data.getUint32(startIndex) & 0x80000000) != 0)
    {
        val.m_unscaledValue *= -1;
    }

    val.m_scale = scale;
    return val;
}

DecimalValue getDecimalValueFromByteBuf(SlicedByteBuf& data, size_t length, int scale) {

    cpp_int decimalDigits;
    // casts the first unsigned byte to signed to determine the sign of the value
    decimalDigits = decimalDigits | cpp_int(static_cast<int8_t>(data.getByte(0))) << (length - 1) * 8;
    for (int pos = length - 1; pos > 0; pos--) {
        decimalDigits = decimalDigits | cpp_int(data.getByte(pos)) << (length - pos - 1) * 8;
    }

    DecimalValue val;
    val.m_unscaledValue = decimalDigits;
    val.m_scale = scale;
    return val;
}

DecimalValue getDecimalValueFromDense(SlicedByteBuf& data, size_t startIndex, int nDecimalDigits, int scale, int maxPrecision, int width)
{
    /* This method converts the dense representation to
     * an intermediate representation. The intermediate
     * representation has one more integer than the dense
     * representation.
     */
    std::vector<uint8_t> intermediateBytes((nDecimalDigits + 1) * INTEGER_SIZE, 0);

    int32_t intermediateIndex = 3;

    int32_t mask[] = {0x03, 0x0F, 0x3F, 0xFF};
    int32_t reverseMask[] = {0xFC, 0xF0, 0xC0, 0x00};

    int32_t maskIndex;
    int32_t shiftOrder;
    uint8_t shiftBits;

    // TODO: Some of the logic here is common with casting from Dense to Sparse types, factor out common code
    if (maxPrecision == 38) {
        maskIndex = 0;
        shiftOrder = 6;
        shiftBits = 0x00;
        intermediateBytes[intermediateIndex++] = (uint8_t) (data.getByte(startIndex) & 0x7F);
    } else if (maxPrecision == 28) {
        maskIndex = 1;
        shiftOrder = 4;
        shiftBits = (uint8_t) ((data.getByte(startIndex) & 0x03) << shiftOrder);
        intermediateBytes[intermediateIndex++] = (uint8_t) (((data.getByte(startIndex) & 0x3C) & 0xFF) >> 2);
    } else {
        assert("Dense types with max precision 38 and 28 are only supported");
    }

    int32_t inputIndex = 1;
    bool sign = false;

    if ((data.getByte(startIndex) & 0x80) != 0) {
        sign = true;
    }

    while (inputIndex < width) {

        intermediateBytes[intermediateIndex] = (uint8_t) ((shiftBits) | (((data.getByte(startIndex + inputIndex) & reverseMask[maskIndex]) & 0xFF) >> (8 - shiftOrder)));

        shiftBits = (uint8_t) ((data.getByte(startIndex + inputIndex) & mask[maskIndex]) << shiftOrder);

        inputIndex++;
        intermediateIndex++;

        if (((inputIndex - 1) % INTEGER_SIZE) == 0) {
            shiftBits = (uint8_t) ((shiftBits & 0xFF) >> 2);
            maskIndex++;
            shiftOrder -= 2;
        }

    }
    /* copy the last byte */
    intermediateBytes[intermediateIndex] = shiftBits;

    if (sign) {
        intermediateBytes[0] = (uint8_t) (intermediateBytes[0] | 0x80);
    }

    SlicedByteBuf intermediateData(&intermediateBytes[0], 0, intermediateBytes.size());
    return getDecimalValueFromIntermediate(intermediateData, 0, nDecimalDigits + 1, scale);
}

}

