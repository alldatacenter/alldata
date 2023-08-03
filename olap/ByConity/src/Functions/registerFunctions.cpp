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

#if !defined(ARCADIA_BUILD)
#    include <Common/config.h>
#endif

#include <Functions/FunctionFactory.h>


namespace DB
{
void registerFunctionsArithmetic(FunctionFactory &);
void registerFunctionsArray(FunctionFactory &);
void registerFunctionsTuple(FunctionFactory &);
void registerFunctionsMap(FunctionFactory &);
void registerFunctionsByteMap(FunctionFactory &);
void registerFunctionsBitmap(FunctionFactory &);
void registerFunctionsBloomFilter(FunctionFactory &);
void registerFunctionsCoding(FunctionFactory &);
void registerFunctionsComparison(FunctionFactory &);
void registerFunctionsConditional(FunctionFactory &);
void registerFunctionsConversion(FunctionFactory &);
void registerFunctionsDateTime(FunctionFactory &);
void registerFunctionsEmbeddedDictionaries(FunctionFactory &);
void registerFunctionsExternalDictionaries(FunctionFactory &);
void registerFunctionsExternalModels(FunctionFactory &);
void registerFunctionsFormatting(FunctionFactory &);
void registerFunctionsHashing(FunctionFactory &);
void registerFunctionsHigherOrder(FunctionFactory &);
void registerFunctionsLogical(FunctionFactory &);
void registerFunctionsMiscellaneous(FunctionFactory &);
void registerFunctionsRandom(FunctionFactory &);
void registerFunctionsReinterpret(FunctionFactory &);
void registerFunctionsRound(FunctionFactory &);
void registerFunctionsString(FunctionFactory &);
void registerFunctionsStringArray(FunctionFactory &);
void registerFunctionsStringSearch(FunctionFactory &);
void registerFunctionsStringRegexp(FunctionFactory &);
void registerFunctionsStringSimilarity(FunctionFactory &);
void registerFunctionsURL(FunctionFactory &);
void registerFunctionsVisitParam(FunctionFactory &);
void registerFunctionsMath(FunctionFactory &);
void registerFunctionsGeo(FunctionFactory &);
void registerFunctionsIntrospection(FunctionFactory &);
void registerFunctionsNull(FunctionFactory &);
void registerFunctionsJSON(FunctionFactory &);
void registerFunctionsSQLJSON(FunctionFactory &);
void registerFunctionToJSONString(FunctionFactory &);
void registerFunctionsConsistentHashing(FunctionFactory & factory);
void registerFunctionsUnixTimestamp64(FunctionFactory & factory);
void registerFromUnixTimestampMilli(FunctionFactory & factory);
void registerFunctionBitHammingDistance(FunctionFactory & factory);
void registerFunctionTupleHammingDistance(FunctionFactory & factory);
void registerFunctionsStringHash(FunctionFactory & factory);
void registerFunctionValidateNestedArraySizes(FunctionFactory & factory);
#if !defined(ARCADIA_BUILD)
void registerFunctionBayesAB(FunctionFactory &);
#endif
void registerFunctionTid(FunctionFactory & factory);
void registerFunctionLogTrace(FunctionFactory & factory);
void registerFunctionTopoFindDown(FunctionFactory &);
void registerFunctionDtsPartition(FunctionFactory &);

#if USE_SSL
void registerFunctionEncrypt(FunctionFactory & factory);
void registerFunctionDecrypt(FunctionFactory & factory);
void registerFunctionAESEncryptMysql(FunctionFactory & factory);
void registerFunctionAESDecryptMysql(FunctionFactory & factory);

#endif

void registerInternalFunctionDynamicFilter(FunctionFactory &);
void registerFunctionBucketBoundsSearch(FunctionFactory & factory);
void registerFunctionGetHostWithPorts(FunctionFactory & factory);

void registerFunctions()
{
    auto & factory = FunctionFactory::instance();

    registerFunctionsArithmetic(factory);
    registerFunctionsArray(factory);
    registerFunctionsTuple(factory);
#ifdef USE_COMMUNITY_MAP
    registerFunctionsMap(factory);
#else
    registerFunctionsByteMap(factory);
#endif
#if !defined(ARCADIA_BUILD)
    registerFunctionsBitmap(factory);
#endif
    registerFunctionsBloomFilter(factory);
    registerFunctionsCoding(factory);
    registerFunctionsComparison(factory);
    registerFunctionsConditional(factory);
    registerFunctionsConversion(factory);
    registerFunctionsDateTime(factory);
    registerFunctionsEmbeddedDictionaries(factory);
    registerFunctionsExternalDictionaries(factory);
    registerFunctionsExternalModels(factory);
    registerFunctionsFormatting(factory);
    registerFunctionsHashing(factory);
    registerFunctionsHigherOrder(factory);
    registerFunctionsLogical(factory);
    registerFunctionsMiscellaneous(factory);
    registerFunctionsRandom(factory);
    registerFunctionsReinterpret(factory);
    registerFunctionsRound(factory);
    registerFunctionsString(factory);
    registerFunctionsStringArray(factory);
    registerFunctionsStringSearch(factory);
    registerFunctionsStringRegexp(factory);
    registerFunctionsStringSimilarity(factory);
    registerFunctionsURL(factory);
    registerFunctionsVisitParam(factory);
    registerFunctionsMath(factory);
    registerFunctionsGeo(factory);
    registerFunctionsNull(factory);
    registerFunctionsJSON(factory);
    registerFunctionsSQLJSON(factory);
    registerFunctionToJSONString(factory);
    registerFunctionsIntrospection(factory);
    registerFunctionsConsistentHashing(factory);
    registerFunctionsUnixTimestamp64(factory);
    registerFromUnixTimestampMilli(factory);
    registerFunctionBitHammingDistance(factory);
    registerFunctionTupleHammingDistance(factory);
    registerFunctionsStringHash(factory);
    registerFunctionValidateNestedArraySizes(factory);

#if !defined(ARCADIA_BUILD)
    registerFunctionBayesAB(factory);
#endif

#if USE_SSL
    registerFunctionEncrypt(factory);
    registerFunctionDecrypt(factory);
    registerFunctionAESEncryptMysql(factory);
    registerFunctionAESDecryptMysql(factory);
#endif
    registerFunctionTid(factory);
    registerFunctionLogTrace(factory);
    registerFunctionTopoFindDown(factory);
    registerInternalFunctionDynamicFilter(factory);
    registerFunctionBucketBoundsSearch(factory);
    registerFunctionGetHostWithPorts(factory);

    registerFunctionDtsPartition(factory);
}

}
