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

namespace DB
{
class FunctionFactory;

void registerFunctionArray(FunctionFactory &);
void registerFunctionArrayElement(FunctionFactory &);
void registerFunctionArrayResize(FunctionFactory &);
void registerFunctionHas(FunctionFactory &);
void registerFunctionHasAll(FunctionFactory &);
void registerFunctionHasAny(FunctionFactory &);
void registerFunctionHasSubstr(FunctionFactory &);
void registerFunctionIndexOf(FunctionFactory &);
void registerFunctionCountEqual(FunctionFactory &);
void registerFunctionArrayIntersect(FunctionFactory &);
void registerFunctionArrayPushFront(FunctionFactory &);
void registerFunctionArrayPushBack(FunctionFactory &);
void registerFunctionArrayPopFront(FunctionFactory &);
void registerFunctionArrayPopBack(FunctionFactory &);
void registerFunctionArrayConcat(FunctionFactory &);
void registerFunctionArraySlice(FunctionFactory &);
void registerFunctionArrayReverse(FunctionFactory &);
void registerFunctionArrayReduce(FunctionFactory &);
void registerFunctionRange(FunctionFactory &);
void registerFunctionsEmptyArray(FunctionFactory &);
void registerFunctionEmptyArrayToSingle(FunctionFactory &);
void registerFunctionArrayEnumerate(FunctionFactory &);
void registerFunctionArrayEnumerateUniq(FunctionFactory &);
void registerFunctionArrayEnumerateDense(FunctionFactory &);
void registerFunctionArrayEnumerateUniqRanked(FunctionFactory &);
void registerFunctionArrayEnumerateDenseRanked(FunctionFactory &);
void registerFunctionArrayUniq(FunctionFactory &);
void registerFunctionArrayDistinct(FunctionFactory &);
void registerFunctionArrayFlatten(FunctionFactory &);
void registerFunctionArrayWithConstant(FunctionFactory &);
void registerFunctionArrayZip(FunctionFactory &);
void registerFunctionArrayAUC(FunctionFactory &);
void registerFunctionArrayReduceInRanges(FunctionFactory &);
void registerFunctionMapOp(FunctionFactory &);
void registerFunctionMapPopulateSeries(FunctionFactory &);
void registerFunctionArraySetCheck(FunctionFactory &);
void registerFunctionArraySetGet(FunctionFactory &);
void registerFunctionArraySetGetAny(FunctionFactory &);

void registerFunctionsArray(FunctionFactory & factory)
{
    registerFunctionArray(factory);
    registerFunctionArrayElement(factory);
    registerFunctionArrayResize(factory);
    registerFunctionHas(factory);
    registerFunctionHasAll(factory);
    registerFunctionHasAny(factory);
    registerFunctionHasSubstr(factory);
    registerFunctionIndexOf(factory);
    registerFunctionCountEqual(factory);
    registerFunctionArrayIntersect(factory);
    registerFunctionArrayPushFront(factory);
    registerFunctionArrayPushBack(factory);
    registerFunctionArrayPopFront(factory);
    registerFunctionArrayPopBack(factory);
    registerFunctionArrayConcat(factory);
    registerFunctionArraySlice(factory);
    registerFunctionArrayReverse(factory);
    registerFunctionArrayReduce(factory);
    registerFunctionArrayReduceInRanges(factory);
    registerFunctionRange(factory);
    registerFunctionsEmptyArray(factory);
    registerFunctionEmptyArrayToSingle(factory);
    registerFunctionArrayEnumerate(factory);
    registerFunctionArrayEnumerateUniq(factory);
    registerFunctionArrayEnumerateDense(factory);
    registerFunctionArrayEnumerateUniqRanked(factory);
    registerFunctionArrayEnumerateDenseRanked(factory);
    registerFunctionArrayUniq(factory);
    registerFunctionArrayDistinct(factory);
    registerFunctionArrayFlatten(factory);
    registerFunctionArrayWithConstant(factory);
    registerFunctionArrayZip(factory);
    registerFunctionArrayAUC(factory);
    registerFunctionMapOp(factory);
    registerFunctionMapPopulateSeries(factory);
    registerFunctionArraySetCheck(factory);
    registerFunctionArraySetGet(factory);
    registerFunctionArraySetGetAny(factory);
}

}
