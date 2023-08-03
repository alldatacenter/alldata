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

#include <Functions/FunctionFactory.h>

// TODO include this last because of a broken roaring header. See the comment inside.
#include <Functions/FunctionsBitmap.h>


namespace DB
{

void registerFunctionsBitmap(FunctionFactory & factory)
{
    factory.registerFunction<FunctionBitmapBuild>();
    factory.registerFunction<FunctionArrayToBitmap>();
    factory.registerFunction<FunctionBitmapToArray>();
    factory.registerFunction<FunctionBitmapSubsetInRange>();
    factory.registerFunction<FunctionBitmapSubsetLimit>();
    factory.registerFunction<FunctionBitmapTransform>();

    factory.registerFunction<FunctionBitmapSelfCardinality>();
    factory.registerFunction<FunctionBitmapMin>();
    factory.registerFunction<FunctionBitmapMax>();
    factory.registerFunction<FunctionBitmapAndCardinality>();
    factory.registerFunction<FunctionBitmapOrCardinality>();
    factory.registerFunction<FunctionBitmapXorCardinality>();
    factory.registerFunction<FunctionBitmapAndnotCardinality>();

    factory.registerFunction<FunctionBitmapAnd>();
    factory.registerFunction<FunctionBitmapOr>();
    factory.registerFunction<FunctionBitmapXor>();
    factory.registerFunction<FunctionBitmapAndnot>();

    factory.registerFunction<FunctionBitmapHasAll>();
    factory.registerFunction<FunctionBitmapHasAny>();
    factory.registerFunction<FunctionBitmapContains>();

    factory.registerFunction<FunctionEmptyBitmap>();
}
}
