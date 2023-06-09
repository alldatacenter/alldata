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
#ifndef _DECIMAL_UTILS_H
#define _DECIMAL_UTILS_H

#include <boost/multiprecision/cpp_int.hpp>

namespace Drill {

class SlicedByteBuf;

struct DecimalValue
{
    DecimalValue() :
        m_unscaledValue(0),
        m_scale(0)
    {
        ;
    }

    DecimalValue(int32_t val, int32_t scale) : m_unscaledValue(val), m_scale(scale){ }
    DecimalValue(int64_t val, int32_t scale) : m_unscaledValue(val), m_scale(scale){ }
    boost::multiprecision::cpp_int m_unscaledValue;
    int32_t m_scale;
};

// These functions need not be exported. They are used by the templates that return the DecimalValue class.
DecimalValue getDecimalValueFromByteBuf(SlicedByteBuf& data, size_t startIndex, int nDecimalDigits, int scale, bool truncateScale);
DecimalValue getDecimalValueFromByteBuf(SlicedByteBuf& data, size_t length, int scale);
DecimalValue getDecimalValueFromDense(SlicedByteBuf& data, size_t startIndex, int nDecimalDigits, int scale, int maxPrecision, int width);

inline DecimalValue getDecimalValueFromIntermediate(SlicedByteBuf& data, size_t startIndex, int nDecimalDigits, int scale)
{
    // In the intermediate representation, we don't pad the scale with zeros. Set truncate to false.
    return getDecimalValueFromByteBuf(data, startIndex, nDecimalDigits, scale, false);
}

inline DecimalValue getDecimalValueFromSparse(SlicedByteBuf& data, size_t startIndex, int nDecimalDigits, int scale)
{
    // In the sparse representation, we pad the scale with zeroes for ease of arithmetic. Set truncate to true.
    return getDecimalValueFromByteBuf(data, startIndex, nDecimalDigits, scale, true);
}

} // namespace Drill
#endif
