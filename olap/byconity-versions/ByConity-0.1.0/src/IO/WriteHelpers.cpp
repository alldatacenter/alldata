/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include <IO/WriteHelpers.h>
#include <inttypes.h>
#include <Common/hex.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

const char null_array[NULL_ARRAY_SZ] __attribute__ ((aligned (64))) = {0};

template <typename IteratorSrc, typename IteratorDst>
void formatHex(IteratorSrc src, IteratorDst dst, const size_t num_bytes)
{
    size_t src_pos = 0;
    size_t dst_pos = 0;
    for (; src_pos < num_bytes; ++src_pos)
    {
        writeHexByteLowercase(src[src_pos], &dst[dst_pos]);
        dst_pos += 2;
    }
}

void formatUUID(const UInt8 * src16, UInt8 * dst36)
{
    formatHex(&src16[0], &dst36[0], 4);
    dst36[8] = '-';
    formatHex(&src16[4], &dst36[9], 2);
    dst36[13] = '-';
    formatHex(&src16[6], &dst36[14], 2);
    dst36[18] = '-';
    formatHex(&src16[8], &dst36[19], 2);
    dst36[23] = '-';
    formatHex(&src16[10], &dst36[24], 6);
}

/** Function used when byte ordering is important when parsing uuid
 *  ex: When we create an UUID type
 */
void formatUUID(std::reverse_iterator<const UInt8 *> src16, UInt8 * dst36)
{
    formatHex(src16 + 8, &dst36[0], 4);
    dst36[8] = '-';
    formatHex(src16 + 12, &dst36[9], 2);
    dst36[13] = '-';
    formatHex(src16 + 14, &dst36[14], 2);
    dst36[18] = '-';
    formatHex(src16, &dst36[19], 2);
    dst36[23] = '-';
    formatHex(src16 + 2, &dst36[24], 6);
}


void writeException(const Exception & e, WriteBuffer & buf, bool with_stack_trace)
{
    writeBinary(e.code(), buf);
    writeBinary(String(e.name()), buf);
    writeBinary(e.displayText(), buf);

    if (with_stack_trace)
        writeBinary(e.getStackTraceString(), buf);
    else
        writeBinary(String(), buf);

    bool has_nested = false;
    writeBinary(has_nested, buf);
}


/// The same, but quotes apply only if there are characters that do not match the identifier without quotes
template <typename F>
static inline void writeProbablyQuotedStringImpl(const StringRef & s, WriteBuffer & buf, F && write_quoted_string)
{
    if (isValidIdentifier(std::string_view{s}))
        writeString(s, buf);
    else
        write_quoted_string(s, buf);
}

void writeProbablyBackQuotedString(const StringRef & s, WriteBuffer & buf)
{
    writeProbablyQuotedStringImpl(s, buf, [](const StringRef & s_, WriteBuffer & buf_) { return writeBackQuotedString(s_, buf_); });
}

void writeProbablyDoubleQuotedString(const StringRef & s, WriteBuffer & buf)
{
    writeProbablyQuotedStringImpl(s, buf, [](const StringRef & s_, WriteBuffer & buf_) { return writeDoubleQuotedString(s_, buf_); });
}

void writeProbablyBackQuotedStringMySQL(const StringRef & s, WriteBuffer & buf)
{
    writeProbablyQuotedStringImpl(s, buf, [](const StringRef & s_, WriteBuffer & buf_) { return writeBackQuotedStringMySQL(s_, buf_); });
}

void writePointerHex(const void * ptr, WriteBuffer & buf)
{
    writeString("0x", buf);
    char hex_str[2 * sizeof(ptr)];
    writeHexUIntLowercase(reinterpret_cast<uintptr_t>(ptr), hex_str);
    buf.write(hex_str, 2 * sizeof(ptr));
}

}
