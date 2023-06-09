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


#include <DataTypes/DataTypeBitMap64.h>
#include <DataTypes/DataTypeFactory.h>
#include <DataTypes/Serializations/SerializationBitMap64.h>
#include <Columns/ColumnBitMap64.h>

#ifdef __SSE2__
#include <emmintrin.h>
#endif


namespace DB
{

MutableColumnPtr DataTypeBitMap64::createColumn() const
{
    return ColumnBitMap64::create();
}

bool DataTypeBitMap64::equals(const IDataType & rhs) const
{
    return typeid(rhs) == typeid(*this);
}

SerializationPtr DataTypeBitMap64::doGetDefaultSerialization() const
{
    return std::make_shared<SerializationBitMap64>();
}

static DataTypePtr create()
{
    return std::make_shared<DataTypeBitMap64>();
}

void registerDataTypeBitMap64(DataTypeFactory & factory)
{
    factory.registerSimpleDataType("BitMap64", create);
}

}
