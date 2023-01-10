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

#include <DataTypes/DataTypeDecimalBase.h>

#include <Common/assert_cast.h>
#include <Common/typeid_cast.h>
#include <Core/DecimalFunctions.h>
#include <DataTypes/DataTypeFactory.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <Interpreters/Context.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/IAST.h>

#include <type_traits>

namespace DB
{

namespace ErrorCodes
{
}

bool decimalCheckComparisonOverflow(ContextPtr context)
{
    return context->getSettingsRef().decimal_check_overflow;
}
bool decimalCheckArithmeticOverflow(ContextPtr context)
{
    return context->getSettingsRef().decimal_check_overflow;
}
bool decimalArithmeticPromoteStorage(ContextPtr context)
{
    return context->getSettingsRef().decimal_arithmetic_promote_storage;
}
bool decimalDivisionUseExtendedScale(ContextPtr context)
{
    return context->getSettingsRef().decimal_division_use_extended_scale;
}

template <typename T>
Field DataTypeDecimalBase<T>::getDefault() const
{
    return DecimalField(T(0), scale);
}

template <typename T>
MutableColumnPtr DataTypeDecimalBase<T>::createColumn() const
{
    return ColumnType::create(0, scale);
}

template <typename T>
T DataTypeDecimalBase<T>::getScaleMultiplier(UInt32 scale_)
{
    return DecimalUtils::scaleMultiplier<typename T::NativeType>(scale_);
}


/// Explicit template instantiations.
template class DataTypeDecimalBase<Decimal32>;
template class DataTypeDecimalBase<Decimal64>;
template class DataTypeDecimalBase<Decimal128>;
template class DataTypeDecimalBase<Decimal256>;
template class DataTypeDecimalBase<DateTime64>;

}
