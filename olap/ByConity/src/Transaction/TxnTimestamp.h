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

#include <Core/Types.h>

namespace DB
{

class TxnTimestamp {
public:
    TxnTimestamp() : _ts(0) {}
    TxnTimestamp(UInt64 ts) : _ts(ts) {}

    TxnTimestamp(const TxnTimestamp &) = default;
    TxnTimestamp & operator=(const TxnTimestamp &) = default;
    TxnTimestamp(TxnTimestamp &&) = default;
    TxnTimestamp & operator=(TxnTimestamp &&) = default;

    inline bool operator== (const TxnTimestamp rhs) const { return _ts == rhs._ts; }
    inline bool operator!= (const TxnTimestamp rhs) const { return _ts != rhs._ts; }
    inline bool operator<  (const TxnTimestamp rhs) const { return _ts <  rhs._ts; }
    inline bool operator<= (const TxnTimestamp rhs) const { return _ts <= rhs._ts; }
    inline bool operator>  (const TxnTimestamp rhs) const { return _ts >  rhs._ts; }
    inline bool operator>= (const TxnTimestamp rhs) const { return _ts >= rhs._ts; }

    inline bool operator== (const UInt64 rhs) const { return _ts == rhs; }
    inline bool operator!= (const UInt64 rhs) const { return _ts != rhs; }
    inline bool operator<  (const UInt64 rhs) const { return _ts <  rhs; }
    inline bool operator<= (const UInt64 rhs) const { return _ts <= rhs; }
    inline bool operator>  (const UInt64 rhs) const { return _ts >  rhs; }
    inline bool operator>= (const UInt64 rhs) const { return _ts >= rhs; }

    explicit operator bool() const { return _ts != 0; }
    inline operator UInt64() const { return _ts; }

    inline UInt64 toUInt64() const { return _ts; }
    inline UInt64 toMillisecond() const { return (_ts) >> 18; }
    inline UInt64 toSecond() const { return toMillisecond() / 1000; }

    static TxnTimestamp minTS() { return TxnTimestamp(0); }
    static TxnTimestamp maxTS() { return TxnTimestamp(UINT64_MAX); }
    static TxnTimestamp fallbackTS() { return TxnTimestamp(UINT64_MAX - 1); }

    std::string toString() const;

private:
    UInt64 _ts;
};

}
