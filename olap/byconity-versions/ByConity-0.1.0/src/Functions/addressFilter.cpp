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

#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <Functions/FunctionFactory.h>
#include <Functions/FunctionHelpers.h>
#include <Functions/geometryConverters.h>
#include <Columns/ColumnString.h>

#include <string>
#include <memory>
#include <cmath>

namespace DB
{

namespace ErrorCodes
{
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
    extern const int TOO_MANY_ARGUMENTS_FOR_FUNCTION;
    extern const int TOO_FEW_ARGUMENTS_FOR_FUNCTION;
}

// given by LBS Platform
static std::pair<Float64 , Float64> longlatOffset(Float64 lng, Float64 lat, Float64 alpha, Float64 dst)
{
    Float64 arc = 6371.393 * 1000;
    Float64 latRadian = lat / 180 * M_PI;
    lng += dst * sin(alpha) / (arc * cos(latRadian) * 2 * M_PI) * 360;
    lat += dst * cos(alpha) / (arc * 2 * M_PI) * 360;
    return std::make_pair(lng, lat);
}

static inline double radians(double d)
{
    return d * M_PI / 180.0;
}

// this function is for LBS Platform, to filter address within the limit distance
Float64 distanceByLBSWithoutSqrt(Float64 lat1, Float64 lon1, Float64 lat2, Float64 lon2)
{
    lon1=radians(lon1);
    lat1=radians(lat1);
    lon2=radians(lon2);
    lat2=radians(lat2);
    Float64 dx = lon2 - lon1;
    Float64 dy = lat2 - lat1;
    Float64 b = (lat1 + lat2) / 2;
    Float64 lx = dx * 6370996.81 * cos(b);
    Float64 ly = dy * 6370996.81;
    return lx * lx + ly * ly;
}

// this function is for LBS Platform, to filter address within the limit distance
class FunctionMultiAddressFilter : public IFunction
{
public:
    static inline const char * name = "multiAddressFilter";

    explicit FunctionMultiAddressFilter() = default;

    static FunctionPtr create(ContextPtr)
    {
        return std::make_shared<FunctionMultiAddressFilter>();
    }

    String getName() const override
    {
        return name;
    }

    bool isVariadic() const override
    {
        return true;
    }

    size_t getNumberOfArguments() const override
    {
        return 0;
    }

    DataTypePtr getReturnTypeImpl(const DataTypes & arguments) const override
    {
        if (arguments.size() < 5 || arguments.size() % 2 == 0)
            throw Exception("argument number not right", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        for (size_t arg_idx = 0; arg_idx < arguments.size(); arg_idx ++)
        {
            const auto arg = arguments[arg_idx].get();
            if (!WhichDataType(arg).isFloat64())
                throw Exception(
                    "Illegal type " + arg->getName() + " of argument " + std::to_string(arg_idx + 1) + " of function " + getName() + ". Must be Float64",
                    ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }

        return std::make_shared<DataTypeUInt8>();
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & /*result_type*/, size_t input_rows_count) const override
    {
        auto dst = ColumnUInt8::create();
        const auto size = input_rows_count;
        auto & dst_data = dst->getData();
        dst_data.resize(size);
        const auto point_size = (arguments.size() - 3) / 2;

        if (size > 0)
        {
            for (size_t i = 2; i < arguments.size(); i++)
            {
                if (!isColumnConst(*arguments[i].column))
                    throw Exception("argument after 2 should be const", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
            }
            const Float64 distance = static_cast<const ColumnConst *>(arguments[2].column.get())->getValue<Float64>();
            const Float64 compare_distance = pow(distance, 2);
            Float64 point_address[point_size * 2];
            Float64 point_address_bound[point_size * 4];

            for (size_t i = 0; i < point_size; i += 1)
            {
                point_address[2 * i] = static_cast<const ColumnConst *>(arguments[3 + 2 * i].column.get())->getValue<Float64>();
                point_address[2 * i + 1] = static_cast<const ColumnConst *>(arguments[4 + 2 * i].column.get())->getValue<Float64>();

                auto lng_min_lat_min = longlatOffset(point_address[2 * i], point_address[2 * i + 1], M_PI/4*5, distance * M_SQRT2);
                auto lng_max_lat_max = longlatOffset(point_address[2 * i], point_address[2 * i + 1], M_PI/4, distance * M_SQRT2);
                point_address_bound[4 * i] = lng_min_lat_min.first;
                point_address_bound[4 * i + 1] = lng_min_lat_min.second;
                point_address_bound[4 * i + 2] = lng_max_lat_max.first;
                point_address_bound[4 * i + 3] = lng_max_lat_max.second;
            }

            Float64 real_long;
            Float64 real_lat;
            for (size_t row = 0; row < size; row += 1)
            {
                dst_data[row] = 0;
                if (likely(!isColumnConst(*(arguments[0]).column)))
                    real_long = static_cast<const ColumnVector<Float64> *>(arguments[0].column.get())->getData()[row];
                else
                    real_long = static_cast<const ColumnConst *>(arguments[0].column.get())->getValue<Float64>();
                if (likely(!isColumnConst(*arguments[1].column)))
                    real_lat = static_cast<const ColumnVector<Float64> *>(arguments[1].column.get())->getData()[row];
                else
                    real_lat = static_cast<const ColumnConst *>(arguments[1].column.get())->getValue<Float64>();
                for (size_t i = 0; i < point_size; i += 1)
                {
                    if (real_long >= point_address_bound[4 * i] &&
                        real_long <= point_address_bound[4 * i + 2] &&
                        real_lat >= point_address_bound[4 * i + 1] &&
                        real_lat <=point_address_bound[4 * i + 3] &&
                        distanceByLBSWithoutSqrt(real_lat, real_long, point_address[2 * i + 1], point_address[2 * i]) <= compare_distance
                    )
                    {
                        dst_data[row] = 1;
                        break;
                    }
                }
            }
        }
        return dst;
    }

    bool useDefaultImplementationForConstants() const override
    {
        return true;
    }
};

// this function is for LBS Platform, to filter address within the limit distance
class FunctionMultiAddressMultiDistanceFilter : public IFunction
{
public:

    static constexpr auto name = "multiAddressMultiDistanceFilter";
    static FunctionPtr create(ContextPtr) { return std::make_shared<FunctionMultiAddressMultiDistanceFilter>(); }

private:

    String getName() const override { return name; }

    bool isVariadic() const override { return true; }
    size_t getNumberOfArguments() const override { return 0; }

    DataTypePtr getReturnTypeImpl(const DataTypes & arguments) const override
    {
        if (arguments.size() < 5 || ((arguments.size() - 2) % 3 != 0))
            throw Exception("argument number not right", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        for (size_t arg_idx = 0; arg_idx < arguments.size(); arg_idx++)
        {
            const auto arg = arguments[arg_idx].get();
            if (!WhichDataType(arg).isFloat64())
                throw Exception(
                    "Illegal type " + arg->getName() + " of argument " + std::to_string(arg_idx + 1) + " of function " + getName() + ". Must be Float64",
                    ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }

        return std::make_shared<DataTypeUInt8>();
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & /*result_type*/, size_t input_rows_count) const override
    {
        auto dst = ColumnUInt8::create();
        const auto size = input_rows_count;
        auto & dst_data = dst->getData();
        dst_data.resize(size);
        const auto point_size = (arguments.size() - 2) / 3;

        if (size > 0)
        {
            for (size_t i = 2; i < arguments.size(); i++)
            {
                if (!isColumnConst(*arguments[i].column))
                    throw Exception("argument after 2 should be const", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
            }

            Float64 distances[point_size];
            Float64 compare_distances[point_size];
            Float64 point_address[point_size * 2];
            Float64 point_address_bound[point_size * 8];

            for (size_t i = 0; i < point_size; i += 1)
            {
                distances[i] = static_cast<const ColumnConst *>(arguments[2 + 3 * i].column.get())->getValue<Float64>();
                compare_distances[i] = pow(distances[i], 2);
                point_address[2 * i] = static_cast<const ColumnConst *>(arguments[3 + 3 * i].column.get())->getValue<Float64>();
                point_address[2 * i + 1] = static_cast<const ColumnConst *>(arguments[4 + 3 * i].column.get())->getValue<Float64>();

                auto lng_min_lat_min = longlatOffset(point_address[2 * i], point_address[2 * i + 1], M_PI/4*5, distances[i] * M_SQRT2);
                auto lng_max_lat_max = longlatOffset(point_address[2 * i], point_address[2 * i + 1], M_PI/4, distances[i] * M_SQRT2);
                auto inner_lng_min_lat_min = longlatOffset(point_address[2 * i], point_address[2 * i + 1], M_PI/4*5, distances[i]);
                auto inner_lng_max_lat_max = longlatOffset(point_address[2 * i], point_address[2 * i + 1], M_PI/4, distances[i]);
                point_address_bound[8 * i] = lng_min_lat_min.first;
                point_address_bound[8 * i + 1] = lng_min_lat_min.second;
                point_address_bound[8 * i + 2] = lng_max_lat_max.first;
                point_address_bound[8 * i + 3] = lng_max_lat_max.second;
                point_address_bound[8 * i + 4] = inner_lng_min_lat_min.first;
                point_address_bound[8 * i + 5] = inner_lng_min_lat_min.second;
                point_address_bound[8 * i + 6] = inner_lng_max_lat_max.first;
                point_address_bound[8 * i + 7] = inner_lng_max_lat_max.second;
            }

            Float64 real_long;
            Float64 real_lat;
            for (size_t row = 0; row < size; row += 1)
            {
                dst_data[row] = 0;
                if (likely(!isColumnConst(*arguments[0].column)))
                    real_long = static_cast<const ColumnVector<Float64> *>(arguments[0].column.get())->getData()[row];
                else
                    real_long = static_cast<const ColumnConst *>(arguments[0].column.get())->getValue<Float64>();
                if (likely(!isColumnConst(*arguments[1].column)))
                    real_lat = static_cast<const ColumnVector<Float64> *>(arguments[1].column.get())->getData()[row];
                else
                    real_lat = static_cast<const ColumnConst *>(arguments[1].column.get())->getValue<Float64>();

                for (size_t i = 0; i < point_size; i += 1)
                {
                    if (real_long >= point_address_bound[8 * i + 4] &&
                        real_long <= point_address_bound[8 * i + 6] &&
                        real_lat >= point_address_bound[8 * i + 5] &&
                        real_lat <=point_address_bound[8 * i + 7]
                    )
                    {
                        dst_data[row] = 1;
                        break;
                    }
                    if (real_long >= point_address_bound[8 * i] &&
                        real_long <= point_address_bound[8 * i + 2] &&
                        real_lat >= point_address_bound[8 * i + 1] &&
                        real_lat <=point_address_bound[8 * i + 3] &&
                        distanceByLBSWithoutSqrt(real_lat, real_long, point_address[2 * i + 1], point_address[2 * i]) <= compare_distances[i] )
                    {
                        dst_data[row] = 1;
                        break;
                    }
                }
            }
        }
        return dst;
    }
};

void registerFunctionMultiAddressFilter(FunctionFactory & factory)
{
    factory.registerFunction<FunctionMultiAddressFilter>();
    factory.registerFunction<FunctionMultiAddressMultiDistanceFilter>();
}

}
