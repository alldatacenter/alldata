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
// generate bounds to be used in NdvBuckets

#include <string_view>
#include <DataTypes/DataTypeString.h>
#include <Statistics/SerdeDataType.h>
#include <Statistics/SerdeUtils.h>

namespace DB::Statistics
{
template <typename T>
class BucketBoundsImpl;

// type erasure interface for BucketBoundsImpl
// for details, refer to definition of BucketBoundsImpl
class BucketBounds
{
public:
    template <typename T>
    using Impl = BucketBoundsImpl<T>;

    virtual String serialize() const = 0;
    virtual void deserialize(std::string_view blob) = 0;

    //serialize as json
    virtual String serializeToJson() const = 0;

    //deserialize from json
    virtual void deserializeFromJson(std::string_view) = 0;

    BucketBounds() = default;

    BucketBounds(const BucketBounds &) = default;
    BucketBounds(BucketBounds &&) = default;
    BucketBounds & operator=(const BucketBounds &) = default;
    BucketBounds & operator=(BucketBounds &&) = default;

    static SerdeDataType getSerdeDataTypeFromBlob(std::string_view raw_blob)
    {
        auto [serde_data_type, tail] = parseBlobWithHeader(raw_blob);
        (void)tail;
        return serde_data_type;
    }

    virtual SerdeDataType getSerdeDataType() const = 0;

    virtual ~BucketBounds() = default;

    virtual size_t numBuckets() const = 0;
    virtual String getElementAsString(int64_t index) const = 0;

    virtual std::pair<bool, bool> getBoundInclusive(size_t bucket_id) const = 0;
};

}
