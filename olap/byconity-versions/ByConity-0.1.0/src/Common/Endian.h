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

#include <type_traits>
#include <stdint.h>

/// define macros: __BYTE_ORDER, __LITTLE_ENDIAN, __BIG_ENDIAN
#if defined(__linux__) || defined(__CYGWIN__)
#    include <endian.h>
#elif defined(__APPLE__)
#    include <machine/endian.h>
/// follow the linux convention
#    define __BYTE_ORDER BYTE_ORDER
#    define __LITTLE_ENDIAN LITTLE_ENDIAN
#    define __BIG_ENDIAN BIG_ENDIAN
#else
#    error architecture not supported
#endif

namespace DB
{
constexpr auto kIsLittleEndian = __BYTE_ORDER == __LITTLE_ENDIAN;
constexpr auto kIsBigEndian = !kIsLittleEndian;

namespace detail
{
    template <class T>
    struct EndianInt
    {
        static_assert((std::is_integral<T>::value && sizeof(T) <= 8), "template type parameter must be integral <= 8 bytes");
        static T swap(T x)
        {
            if constexpr (sizeof(T) == 1)
                return x;
            else if constexpr (sizeof(T) == 2)
                return __builtin_bswap16(x);
            else if constexpr (sizeof(T) == 4)
                return __builtin_bswap32(x);
            else
            {
                static_assert(sizeof(T) == 8, "template type parameter should be 8 bytes");
                return __builtin_bswap64(x);
            }
        }
        static T big(T x) { return kIsLittleEndian ? EndianInt::swap(x) : x; }
        static T little(T x) { return kIsBigEndian ? EndianInt::swap(x) : x; }
    };
} /// namespace detail

class Endian
{
public:
    enum class Order : uint8_t
    {
        LITTLE,
        BIG,
    };

    static constexpr Order order = kIsLittleEndian ? Order::LITTLE : Order::BIG;

    template <class T>
    static T swap(T x)
    {
        return DB::detail::EndianInt<T>::swap(x);
    }
    /// host2big or big2host
    template <class T>
    static T big(T x)
    {
        return DB::detail::EndianInt<T>::big(x);
    }
    /// host2little or little2host
    template <class T>
    static T little(T x)
    {
        return DB::detail::EndianInt<T>::little(x);
    }
};

} /// namespace DB
