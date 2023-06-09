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

#include <Optimizer/Dump/Json2Pb.h>
#include <Statistics/StatisticsBaseImpl.h>
#include <Statistics/StatsColumnBasic.h>
#include <Statistics/StatsCpcSketch.h>
#include <Statistics/StatsDummy.h>
#include <Statistics/StatsKllSketchImpl.h>
#include <Statistics/StatsNdvBucketsExtendImpl.h>
#include <Statistics/StatsNdvBucketsImpl.h>
#include <Statistics/StatsTableBasic.h>
#include <Statistics/TypeMacros.h>
#include <Common/Exception.h>

namespace DB::Statistics
{
template <class StatsType>
std::shared_ptr<StatsType> createStatisticsUntyped(StatisticsTag tag, std::string_view blob)
{
    static_assert(std::is_base_of_v<StatisticsBase, StatsType>);
    CheckTag<StatsType>(tag);
    std::shared_ptr<StatsType> ptr = std::make_shared<StatsType>();
    ptr->deserialize(blob);
    return ptr;
}

template <class StatsType, typename T>
std::shared_ptr<StatsType> createStatisticsTypedImpl(StatisticsTag tag, std::string_view blob)
{
    static_assert(std::is_base_of_v<StatisticsBase, StatsType>);
    CheckTag<StatsType>(tag);
    using ImplType = typename StatsType::template Impl<T>;
    auto ptr = std::make_shared<ImplType>();
    ptr->deserialize(blob);
    return ptr;
}


#define UNTYPED_INSTANTIATION(STATS_TYPE) \
    template std::shared_ptr<Stats##STATS_TYPE> createStatisticsUntyped<Stats##STATS_TYPE>(StatisticsTag, std::string_view blob);
UNTYPED_STATS_ITERATE(UNTYPED_INSTANTIATION)
#undef UNTYPED_INSTANTIATION

#define TYPED_INSTANTIATION(STATS_TYPE) \
    template std::shared_ptr<Stats##STATS_TYPE> createStatisticsTyped<Stats##STATS_TYPE>(StatisticsTag, std::string_view blob);
TYPED_STATS_ITERATE(TYPED_INSTANTIATION)
#undef TYPED_INSTANTIATION

template <class StatsType>
std::shared_ptr<StatsType> createStatisticsTyped(StatisticsTag tag, std::string_view blob)
{
    if (blob.size() < sizeof(SerdeDataType))
    {
        throw Exception("statistics blob corrupted", ErrorCodes::LOGICAL_ERROR);
    }

    auto type_blob = blob.substr(0, sizeof(SerdeDataType));
    SerdeDataType serde_data_type;
    memcpy(&serde_data_type, type_blob.data(), type_blob.size());
    switch (serde_data_type)
    {
#define ENUM_CASE(TYPE) \
    case SerdeDataType::TYPE: { \
        return createStatisticsTypedImpl<StatsType, TYPE>(tag, blob); \
    }
        ALL_TYPE_ITERATE(ENUM_CASE)
#undef ENUM_CASE
        // NOTE: for compatibility, old version enum value is also supported
        case SerdeDataType::StringOldVersion:
            return createStatisticsTypedImpl<StatsType, String>(tag, blob);

        default: {
            throw Exception("unknown type", ErrorCodes::NOT_IMPLEMENTED);
        }
    }
}
template <class StatsType>
std::shared_ptr<StatsType> createStatisticsUntypedJson(StatisticsTag tag, std::string_view blob)
{
    static_assert(std::is_base_of_v<StatisticsBase, StatsType>);
    CheckTag<StatsType>(tag);
    std::shared_ptr<StatsType> ptr = std::make_shared<StatsType>();
    ptr->deserializeFromJson(blob);
    return ptr;
}
template <class StatsType, typename T>
std::shared_ptr<StatsType> createStatisticsTypedJsonImpl(StatisticsTag tag, std::string_view blob)
{
    static_assert(std::is_base_of_v<StatisticsBase, StatsType>);
    CheckTag<StatsType>(tag);
    using ImplType = typename StatsType::template Impl<T>;
    auto ptr = std::make_shared<ImplType>();
    ptr->deserializeFromJson(blob);
    return ptr;
}
template <class StatsType>
std::shared_ptr<StatsType> createStatisticsTypedJson(StatisticsTag tag, std::string_view blob)
{
    if (blob.size() < sizeof(SerdeDataType))
    {
        throw Exception("statistics blob corrupted", ErrorCodes::LOGICAL_ERROR);
    }
    Pparser parser;
    PVar var = parser.parse(std::string{blob.data(), blob.size()});
    PObject json_object = *var.extract<PObject::Ptr>();

    PVar var_bounds_blob = json_object.get("bounds_blob");
    PObject object_bounds_blob = *var_bounds_blob.extract<PObject::Ptr>();

    String type_str = object_bounds_blob.get("type_id").toString();
    SerdeDataType type_index = SerdeDataTypeFromString(type_str);

    switch (type_index)
    {
#define ENUM_CASE(TYPE) \
    case SerdeDataType::TYPE: { \
        return createStatisticsTypedJsonImpl<StatsType, TYPE>(tag, blob); \
    }
        ALL_TYPE_ITERATE(ENUM_CASE)
#undef ENUM_CASE

        case SerdeDataType::StringOldVersion:
            return createStatisticsTypedJsonImpl<StatsType, String>(tag, blob);
        default: {
            throw Exception("unknown type", ErrorCodes::NOT_IMPLEMENTED);
        }
    }
}

StatisticsBasePtr createStatisticsBase(StatisticsTag tag, std::string_view blob)
{
    // TODO: rewrite use macro iteration
    auto ptr = [&]() -> StatisticsBasePtr {
        switch (tag)
        {
            case StatisticsTag::DummyAlpha:
                return createStatisticsUntyped<StatsDummyAlpha>(tag, blob);
            case StatisticsTag::DummyBeta:
                return createStatisticsUntyped<StatsDummyBeta>(tag, blob);
            case StatisticsTag::TableBasic:
                return createStatisticsUntyped<StatsTableBasic>(tag, blob);
            case StatisticsTag::ColumnBasic:
                return createStatisticsUntyped<StatsColumnBasic>(tag, blob);
            case StatisticsTag::CpcSketch:
                return createStatisticsUntyped<StatsCpcSketch>(tag, blob);
            case StatisticsTag::KllSketch:
                return createStatisticsTyped<StatsKllSketch>(tag, blob);
            case StatisticsTag::NdvBuckets:
                return createStatisticsTyped<StatsNdvBuckets>(tag, blob);
            case StatisticsTag::NdvBucketsExtend:
                return createStatisticsTyped<StatsNdvBucketsExtend>(tag, blob);
            case StatisticsTag::NdvBucketsResult:
                return createStatisticsTyped<StatsNdvBucketsResult>(tag, blob);
            default: {
                throw Exception("Unimplemented Statistics Tag", ErrorCodes::NOT_IMPLEMENTED);
            }
        }
    }();

    return ptr;
}
StatisticsBasePtr createStatisticsBaseFromJson(StatisticsTag tag, std::string_view blob)
{
    auto ptr = [&]() -> StatisticsBasePtr {
        switch (tag)
        {
            case StatisticsTag::TableBasic:
                return createStatisticsUntypedJson<StatsTableBasic>(tag, blob);
            case StatisticsTag::ColumnBasic:
                return createStatisticsUntypedJson<StatsColumnBasic>(tag, blob);
            case StatisticsTag::NdvBucketsResult:
                return createStatisticsTypedJson<StatsNdvBucketsResult>(tag, blob);
            default: {
                throw Exception("Unimplemented Statistics Tag", ErrorCodes::NOT_IMPLEMENTED);
            }
        }
    }();
    return ptr;
}
} // namespace DB
