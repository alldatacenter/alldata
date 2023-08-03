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

#include <Statistics/StatsNdvBucketsResultImpl.h>

namespace DB::Statistics
{
template <typename T>
std::shared_ptr<StatsNdvBucketsResultImpl<T>>
StatsNdvBucketsResultImpl<T>::createImpl(const BucketBounds & bounds, std::vector<UInt64> counts, std::vector<double> ndvs)
{
    auto res = std::make_shared<StatsNdvBucketsResultImpl<T>>();
    auto bounds_impl_ptr = dynamic_cast<const BucketBoundsImpl<T> *>(&bounds);
    if (!bounds_impl_ptr)
    {
        throw Exception("mismatch data", ErrorCodes::LOGICAL_ERROR);
    }

    res->bounds_ = *bounds_impl_ptr;
    res->counts_ = std::move(counts);
    res->ndvs_ = std::move(ndvs);
    res->checkValid();
    return res;
}

std::shared_ptr<StatsNdvBucketsResult>
StatsNdvBucketsResult::create(const BucketBounds & bounds, std::vector<UInt64> counts, std::vector<double> ndvs)
{
    switch (bounds.getSerdeDataType())
    {
#define CASE(TYPE) \
    case SerdeDataType::TYPE: { \
        return StatsNdvBucketsResultImpl<TYPE>::createImpl(bounds, std::move(counts), std::move(ndvs)); \
    }
        ALL_TYPE_ITERATE(CASE)
#undef CASE

        default:
            throw Exception("unimplemented", ErrorCodes::LOGICAL_ERROR);
    }
}

template <typename T>
String StatsNdvBucketsResultImpl<T>::serialize() const
{
    checkValid();
    std::ostringstream ss;
    auto serde_data_type = getSerdeDataType();
    ss.write(reinterpret_cast<const char *>(&serde_data_type), sizeof(serde_data_type));
    Protos::StatsNdvBucketsResult pb;
    pb.set_bounds_blob(bounds_.serialize());

    for (auto & count : counts_)
    {
        pb.add_counts(count);
    }
    for (auto & ndv : ndvs_)
    {
        pb.add_ndvs(ndv);
    }
    pb.SerializeToOstream(&ss);
    return ss.str();
}
template <typename T>
String StatsNdvBucketsResultImpl<T>::serializeToJson() const
{
    checkValid();
    Poco::JSON::Object::Ptr ptr_json = new Poco::JSON::Object(true);
    Poco::JSON::Parser parser;
    ptr_json->set("bounds_blob", parser.parse(bounds_.serializeToJson()));
    Poco::JSON::Array array_counts;
    for (auto & count : counts_)
    {
        array_counts.add(int64_t(count));
    }
    ptr_json->set("counts", Poco::Dynamic::Var(array_counts));

    Poco::JSON::Array array_ndvs;
    for (auto & ndv : ndvs_)
    {
        array_ndvs.add(double(ndv));
    }
    ptr_json->set("ndvs", Poco::Dynamic::Var(array_ndvs));
    Poco::Dynamic::Var result(*ptr_json);
    return result.toString();
}
template <typename T>
void StatsNdvBucketsResultImpl<T>::deserialize(std::string_view raw_blob)
{
    std::tie(bounds_, counts_, ndvs_) = [raw_blob] {
        if (raw_blob.size() <= sizeof(SerdeDataType))
        {
            throw Exception("corrupted blob", ErrorCodes::LOGICAL_ERROR);
        }
        SerdeDataType serde_data_type;
        memcpy(&serde_data_type, raw_blob.data(), sizeof(serde_data_type));

        checkSerdeDataType<T>(serde_data_type);

        auto blob = raw_blob.substr(sizeof(serde_data_type), raw_blob.size() - sizeof(serde_data_type));
        Protos::StatsNdvBucketsResult pb;
        pb.ParseFromArray(blob.data(), blob.size());
        BucketBoundsImpl<T> bounds;
        bounds.deserialize(pb.bounds_blob());
        int64_t num_buckets = bounds.numBuckets();
        if (pb.counts_size() != num_buckets || pb.ndvs_size() != num_buckets)
        {
            throw Exception("Corrupted blob", ErrorCodes::LOGICAL_ERROR);
        }
        decltype(counts_) counts(num_buckets);
        decltype(ndvs_) ndvs(num_buckets);
        for (int64_t i = 0; i < num_buckets; ++i)
        {
            counts[i] = pb.counts(i);
            auto ndv = pb.ndvs(i);
            ndvs[i] = ndv;
        }
        return std::tuple{std::move(bounds), std::move(counts), std::move(ndvs)};
    }();
    checkValid();
}
template <typename T>
void StatsNdvBucketsResultImpl<T>::deserializeFromJson(std::string_view raw_blob)
{
    std::tie(bounds_, counts_, ndvs_) = [raw_blob] {
        Pparser parser;
        PVar var = parser.parse(std::string{raw_blob.data(), raw_blob.size()});
        PObject json_object = *var.extract<PObject::Ptr>();

        if (raw_blob.size() <= sizeof(SerdeDataType))
        {
            throw Exception("corrupted blob", ErrorCodes::LOGICAL_ERROR);
        }
        PVar var_bounds_blob = json_object.get("bounds_blob");
        PObject object_bounds_blob = *var_bounds_blob.extract<PObject::Ptr>();

        String type_str = object_bounds_blob.get("type_id").toString();
        SerdeDataType type_id = SerdeDataTypeFromString(type_str);
        checkSerdeDataType<T>(type_id);

        BucketBoundsImpl<T> bounds;

        bounds.deserializeFromJson(var_bounds_blob.toString());

        PVar counts_var, ndvs_var;
        counts_var = json_object.get("counts");
        ndvs_var = json_object.get("ndvs");
        PArray array_counts = *counts_var.extract<PArray::Ptr>();
        PArray array_ndvs = *ndvs_var.extract<PArray::Ptr>();

        size_t num_buckets = bounds.numBuckets();
        if (array_counts.size() != num_buckets || array_ndvs.size() != num_buckets)
        {
            throw Exception("Corrupted blob", ErrorCodes::LOGICAL_ERROR);
        }
        decltype(counts_) counts(num_buckets);
        decltype(ndvs_) ndvs(num_buckets);
        for (size_t i = 0; i < num_buckets; ++i)
        {
            counts[i] = array_counts.get(i).convert<uint64_t>();
            ndvs[i] = array_ndvs.get(i).convert<double>();
        }
        return std::tuple{std::move(bounds), std::move(counts), std::move(ndvs)};
    }();
    checkValid();
}

template <typename T>
void StatsNdvBucketsResultImpl<T>::writeSymbolStatistics(SymbolStatistics & symbol)
{
    checkValid();
    Buckets buckets;
    for (size_t i = 0; i < numBuckets(); ++i)
    {
        auto lb = static_cast<double>(bounds_[i]);
        auto ub = static_cast<double>(bounds_[i + 1]);
        auto count = counts_[i];
        auto ndv_estimate = ndvs_[i];
        auto [lb_inc, ub_inc] = bounds_.getBoundInclusive(i);

        // fix when ndv > count
        auto int_ndv = AdjustNdvWithCount(ndv_estimate, count);

        if (count > 0)
        {
            symbol.emplaceBackBucket(Bucket(lb, ub, int_ndv, count, lb_inc, ub_inc));
        }
    }
}

#define INITIATE(TYPE) template class StatsNdvBucketsResultImpl<TYPE>;
ALL_TYPE_ITERATE(INITIATE)
#undef INITIATE

}
