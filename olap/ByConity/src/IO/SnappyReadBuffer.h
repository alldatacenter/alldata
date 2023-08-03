/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include <Common/PODArray.h>
#include <IO/BufferWithOwnMemory.h>
#include <snappy/snappy.h>
#include <Poco/Logger.h>
#include <common/logger_useful.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
    extern const int LOGICAL_ERROR;
    extern const int CORRUPTED_DATA;
}

class ReadBuffer;

template<class T>
inline T GetInt(const char*)
{
      throw Exception("Not implemented!", ErrorCodes::NOT_IMPLEMENTED);
}

template<>
inline UInt16 GetInt(const char* buf) {
      const unsigned char * ubuf = reinterpret_cast<const unsigned char*>(buf);
      return (buf[0] << 8) | ubuf[1];
}

template<>
inline UInt32 GetInt(const char* buf) {
      const unsigned char * ubuf = reinterpret_cast<const unsigned char*>(buf);
      return (ubuf[0] << 24) | (ubuf[1] << 16) | (ubuf[2] << 8) | ubuf[3];
}

#if !__clang__
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wsign-compare"
#endif

using Poco::Logger;

/* The main decompress logic is copied from Impala implementation */
template <bool SNAPPY_BLOCKED = false>
class SnappyReadBuffer : public BufferWithOwnMemory<ReadBuffer>
{
private:
    static bool SnappyBlockDecompress(size_t input_len, const char* input, bool size_only, size_t* output_len, char * output)
    {
        size_t buffer_size = *output_len;
        *output_len = 0;
        size_t uncompressed_total_len = 0;
        while (input_len > 0)
        {
            // the four bytes are length info
            UInt32 uncompressed_block_len = GetInt<UInt32>(input);
            input += sizeof(UInt32);
            input_len -= sizeof(UInt32);

            if (!size_only)
            {
                size_t remaining_output_size = buffer_size - uncompressed_total_len;
                if (remaining_output_size < uncompressed_block_len)
                {
                    throw Exception("SNAPPY_DECOMPRESS_DECOMPRESS_SIZE_INCORRECT", ErrorCodes::CORRUPTED_DATA);
                }
            }

            while (uncompressed_block_len > 0)
            {
                // Read the length of the next snappy compressed block.
                size_t compressed_len = GetInt<UInt32>(input);
                input += sizeof(UInt32);
                input_len -= sizeof(UInt32);

                if (compressed_len == 0 || compressed_len > input_len)
                {
                    throw Exception("SNAPPY_DECOMPRESS_INVALID_COMPRESSED_LENGTH", ErrorCodes::CORRUPTED_DATA);
                }

                // Read how big the output will be.
                size_t uncompressed_len;
                if (!snappy::GetUncompressedLength(input, compressed_len, &uncompressed_len))
                {
                    throw Exception("SNAPPY_DECOMPRESS_UNCOMPRESSED_LENGTH_FAILED", ErrorCodes::CORRUPTED_DATA);
                }

                if (uncompressed_len <= 0)
                {
                    throw Exception("SNAPPY_DECOMPRESS_UNCOMPRESSED_LENGTH_FAILED", ErrorCodes::CORRUPTED_DATA);
                }

                if (!size_only)
                {
                    // Check output bounds
                    size_t remaining_output_size = buffer_size - uncompressed_total_len;
                    if (remaining_output_size < uncompressed_len)
                    {
                        throw Exception("SNAPPY_DECOMPRESS_DECOMPRESS_SIZE_INCORRECT", ErrorCodes::CORRUPTED_DATA);
                    }
                    // Decompress this snappy block
                    if (!snappy::RawUncompress(input, compressed_len, output))
                    {
                        throw Exception("SNAPPY_DECOMPRESS_RAW_UNCOMPRESS_FAILED", ErrorCodes::CORRUPTED_DATA);
                    }
                    output += uncompressed_len;
                }

                input += compressed_len;
                input_len -= compressed_len;
                uncompressed_block_len -= uncompressed_len;
                uncompressed_total_len += uncompressed_len;
            }
        }
        *output_len = uncompressed_total_len;

        return true;
    }

    /* Hadoop-Snappy format */
    bool SnappyBlockDecompressorProcessBlock(
        size_t input_len, const char* input, size_t *output_len, char* output)
    {
        size_t output_length_local = *output_len;
        *output_len = 0;

        if (!SnappyBlockDecompress(input_len, input, false, &output_length_local, output))
        {
            throw Exception("SnappyBlockDecompress fail", ErrorCodes::CORRUPTED_DATA);
        }
        *output_len = output_length_local;

        return true;
    }


    bool nextImpl() override
    {
        // early out if it reached eof
        if (eof) return false;

        if (SNAPPY_BLOCKED)
        {
            size_t size_uncompressed;
            do
            {
                size_compressed = readBlockCompressedData(size_uncompressed);
                if (!size_compressed)
                {
                    // free resource early
                    freeResource();
                    return false;
                }
            } while (size_uncompressed == 0); // skip empty frame inside

            memory.resize(size_uncompressed);
            working_buffer = Buffer(&memory[0], &memory[size_uncompressed]);
            decompress(working_buffer.begin(), size_uncompressed);
            return true;
        }
        else
        {
            if (!readAndDecompress)
            {
                readAndDecompressUtil();
                return  (size_compressed != 0);
            }
            // free resource early
            freeResource();
            return false;
        }

        // free resource early
        freeResource();
        return false;
    }

    /// Flag that whether HDFS or file data are read into compressed buffer and decompressed
    bool readAndDecompress = false;

    size_t size_compressed = 0;

    /// Flag whether eof reached for sake of speeding up  nextImpl check
    bool eof = false;
protected:
    std::unique_ptr<ReadBuffer> compressed_in;

    /// Otherwise copy parts of data to 'own_compressed_buffer'.
    PODArray<char> own_compressed_buffer;

    size_t readBlockCompressedData(size_t& size_uncompressed)
    {
        own_compressed_buffer.resize(4); // uncompressed_block_len
        size_t readLen = 0, totalLen = 0;
        size_t uncompressed_total_len = 0;
        readLen = compressed_in->read(&own_compressed_buffer[0] + totalLen, 4);
        if (readLen == 0)
        {
            return 0;
        }
        else if (readLen != 4)
        {
            throw Exception("Corrupt BlockCompressed read", ErrorCodes::CORRUPTED_DATA);
        }

        totalLen += 4;

        UInt32 uncompressed_block_len = GetInt<UInt32>(&own_compressed_buffer[0]);
        while (uncompressed_block_len > 0)
        {
            own_compressed_buffer.resize(totalLen + 4); // compressed_len
            readLen = compressed_in->read(&own_compressed_buffer[0] + totalLen, 4);
            if (readLen != 4)
            {
                throw Exception("Corrupt BlockCompressed read", ErrorCodes::CORRUPTED_DATA);
            }
            UInt32 compressed_len = GetInt<UInt32>(&own_compressed_buffer[totalLen]);
            totalLen += 4;
            if (compressed_len == 0)
            {
                throw Exception("SNAPPY_DECOMPRESS_INVALID_COMPRESSED_LENGTH", ErrorCodes::CORRUPTED_DATA);
            }
            own_compressed_buffer.resize(totalLen + compressed_len);
            readLen = compressed_in->read(&own_compressed_buffer[totalLen], compressed_len);

            if (readLen != compressed_len)
            {
                throw Exception("Read compressed data size mismatch", ErrorCodes::CORRUPTED_DATA);
            }

            size_t uncompressed_len;
            if (!snappy::GetUncompressedLength(&own_compressed_buffer[totalLen], compressed_len, &uncompressed_len))
            {
                throw Exception("SNAPPY_DECOMPRESS_UNCOMPRESSED_LENGTH_FAILED", ErrorCodes::CORRUPTED_DATA);
            }

            totalLen += readLen;

            if (uncompressed_len > uncompressed_block_len)
            {
                throw Exception("Uncompressed subblock length greater than block length", ErrorCodes::CORRUPTED_DATA);
            }
            uncompressed_block_len -= uncompressed_len;
            uncompressed_total_len += uncompressed_len;
        }

        size_uncompressed = uncompressed_total_len;
        return totalLen;
    }


    /// Read whole HDFS file into buffer, and return number of compressed bytes read.
    size_t readCompressedData(size_t & size_decompressed)
    {
        if (SNAPPY_BLOCKED)
        {
            throw Exception("readCompressedData should not be called in this setting", ErrorCodes::LOGICAL_ERROR);
        }

        // Read all compressed data from HDFS data stream as we don't support decode in streaming mode
        const size_t allocUnit = 1024*1024;

        own_compressed_buffer.resize(allocUnit); // 1MB
        size_t readLen = 0, totalLen = 0;
        while ((readLen = compressed_in->read(&own_compressed_buffer[0] + totalLen, allocUnit)))
        {
            totalLen += readLen;
            // reach eof
            if (readLen < allocUnit) break;
            own_compressed_buffer.resize(totalLen + allocUnit);
        }
        LOG_DEBUG((&Logger::get("SnappyReadBuffer")), "Finish read snappy file.");
        // set it for BlockDecompress
        size_compressed = totalLen;
        // calculate size_decompressed
        SnappyBlockDecompress(totalLen, &own_compressed_buffer[0], true,/*size_only*/ &size_decompressed, nullptr);
        return size_compressed;
    }

    inline void decompress(char * to, size_t size_decompressed)
    {
        SnappyBlockDecompressorProcessBlock(size_compressed, &own_compressed_buffer[0], &size_decompressed, to);
    }

    inline void freeResource() override
    {
        eof = true;
        own_compressed_buffer.deep_clear();
        BufferWithOwnMemory<ReadBuffer>::freeResource();
    }

public:
    SnappyReadBuffer(std::unique_ptr<ReadBuffer> in) : eof(false), compressed_in(std::move(in)){ }

    void readAndDecompressUtil()
    {
        // Already do the routine, don't need do again as we adopt one pass protocol
        if (readAndDecompress) return;
        size_t size_decompressed;
        // Record size_compressed here
        size_compressed = readCompressedData(size_decompressed);
        //TODO: check whether we need check len here
        memory.resize(size_decompressed);
        working_buffer = Buffer(&memory[0], &memory[size_decompressed]);
        decompress(working_buffer.begin(), size_decompressed);
        readAndDecompress = true;

        LOG_DEBUG((&Logger::get("SnappyReadBuffer")), "Finish decompress snappy file.");
    }

    ~SnappyReadBuffer() override = default;

    /// The compressed size of the current block.
    size_t getSizeCompressed() const
    {
        return size_compressed;
    }
};

#if !__clang__
#pragma GCC diagnostic pop
#endif

}
