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
#include <Core/DecimalFunctions.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
}

template <typename T, typename... Args>
class HasExecuteTime
{
    template <typename C,
              typename = decltype( std::declval<C>().executeTime(std::declval<Args>()...) )>
    static std::true_type test(int);
    template <typename C>
    static std::false_type test(...);

public:
    static constexpr bool value = decltype(test<T>(0))::value;
};

template <typename Transform>
class TransformTime
{
public:
    static constexpr auto name = Transform::name;

    TransformTime(UInt32 scale_ = 0, Transform t = {})
        : scale_multiplier(DecimalUtils::scaleMultiplier<Decimal64::NativeType>(scale_)),
        wrapped_transform(t)
    {}

    template <typename ... Args>
    inline auto NO_SANITIZE_UNDEFINED execute(const Decimal64 & t, Args && ... args) const
    {
        if constexpr (HasExecuteTime<Transform, Decimal64, decltype(scale_multiplier), Args...>::value)
        {
            return wrapped_transform.executeTime(t, scale_multiplier, std::forward<Args>(args)...);
        }
        else if constexpr (HasExecuteTime<Transform, DecimalUtils::DecimalComponents<Decimal64>, Args...>::value)
        {
            auto components = DecimalUtils::splitWithScaleMultiplier(t, scale_multiplier);

            const auto result = wrapped_transform.executeTime(components, std::forward<Args>(args)...);
            using ResultType = std::decay_t<decltype(result)>;

            if constexpr (std::is_same_v<DecimalUtils::DecimalComponents<Decimal64>, ResultType>)
            {
                return DecimalUtils::decimalFromComponentsWithMultiplier<Decimal64>(result, scale_multiplier);
            }
            else
            {
                return result;
            }
        }
        else
        {
            throw Exception("Time type is not supported for function "
                            + std::string(name), ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
            return 0;
        }
    }

    template <typename T, typename ... Args, typename = std::enable_if_t<!std::is_same_v<T, Decimal64>>>
    inline auto execute(const T & , Args && ...) const
    {
        throw Exception("Time type is not supported for function "
                        + std::string(name), ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        return 0;
    }

private:
    Decimal64::NativeType scale_multiplier = 1;
    Transform wrapped_transform;
};

}
