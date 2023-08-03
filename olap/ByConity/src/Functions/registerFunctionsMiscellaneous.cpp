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
#    include <config_core.h>
#endif

namespace DB
{

class FunctionFactory;

void registerFunctionCurrentDatabase(FunctionFactory &);
void registerFunctionCurrentUser(FunctionFactory &);
void registerFunctionHostName(FunctionFactory &);
void registerFunctionHost(FunctionFactory &);
void registerFunctionFQDN(FunctionFactory &);
void registerFunctionVisibleWidth(FunctionFactory &);
void registerFunctionToTypeName(FunctionFactory &);
void registerFunctionGetSizeOfEnumType(FunctionFactory &);
void registerFunctionBlockSerializedSize(FunctionFactory &);
void registerFunctionToColumnTypeName(FunctionFactory &);
void registerFunctionDumpColumnStructure(FunctionFactory &);
void registerFunctionDefaultValueOfArgumentType(FunctionFactory &);
void registerFunctionDefaultValueOfTypeName(FunctionFactory &);
void registerFunctionBlockSize(FunctionFactory &);
void registerFunctionBlockNumber(FunctionFactory &);
void registerFunctionRowNumberInBlock(FunctionFactory &);
void registerFunctionRowNumberInAllBlocks(FunctionFactory &);
void registerFunctionNeighbor(FunctionFactory &);
void registerFunctionSleep(FunctionFactory &);
void registerFunctionSleepEachRow(FunctionFactory &);
void registerFunctionInvalidateStatsCache(FunctionFactory &);
void registerFunctionMaterialize(FunctionFactory &);
void registerFunctionIgnore(FunctionFactory &);
void registerFunctionIndexHint(FunctionFactory &);
void registerFunctionIdentity(FunctionFactory &);
void registerFunctionArrayJoin(FunctionFactory &);
void registerFunctionReplicate(FunctionFactory &);
void registerFunctionBar(FunctionFactory &);
void registerFunctionHasColumnInTable(FunctionFactory &);
void registerFunctionIsFinite(FunctionFactory &);
void registerFunctionIsInfinite(FunctionFactory &);
void registerFunctionIsNaN(FunctionFactory &);
void registerFunctionIfNotFinite(FunctionFactory &);
void registerFunctionThrowIf(FunctionFactory &);
void registerFunctionVersion(FunctionFactory &);
void registerFunctionBuildId(FunctionFactory &);
void registerFunctionUptime(FunctionFactory &);
void registerFunctionTimezone(FunctionFactory &);
void registerFunctionTimezoneOf(FunctionFactory &);
void registerFunctionRunningAccumulate(FunctionFactory &);
void registerFunctionRunningDifference(FunctionFactory &);
void registerFunctionRunningDifferenceStartingWithFirstValue(FunctionFactory &);
void registerFunctionRunningConcurrency(FunctionFactory &);
void registerFunctionFinalizeAggregation(FunctionFactory &);
void registerFunctionToLowCardinality(FunctionFactory &);
void registerFunctionLowCardinalityIndices(FunctionFactory &);
void registerFunctionLowCardinalityKeys(FunctionFactory &);
void registerFunctionLowCardinalityIsNoneEncoded(FunctionFactory &);
void registerFunctionsIn(FunctionFactory &);
void registerFunctionJoinGet(FunctionFactory &);
void registerFunctionFilesystem(FunctionFactory &);
void registerFunctionEvalMLMethod(FunctionFactory &);
void registerFunctionBasename(FunctionFactory &);
void registerFunctionTransform(FunctionFactory &);
void registerFunctionGetMacro(FunctionFactory &);
void registerFunctionGetScalar(FunctionFactory &);
void registerFunctionGetSetting(FunctionFactory &);
void registerFunctionIsConstant(FunctionFactory &);
void registerFunctionIsDecimalOverflow(FunctionFactory &);
void registerFunctionCountDigits(FunctionFactory &);
void registerFunctionGlobalVariable(FunctionFactory &);
void registerFunctionHasThreadFuzzer(FunctionFactory &);
void registerFunctionInitializeAggregation(FunctionFactory &);
void registerFunctionErrorCodeToName(FunctionFactory &);
void registerFunctionTcpPort(FunctionFactory &);
void registerFunctionByteSize(FunctionFactory &);
void registerFunctionFile(FunctionFactory & factory);
void registerFunctionConnectionId(FunctionFactory & factory);
void registerFunctionPartitionId(FunctionFactory & factory);
void registerFunctionPartitionStatus(FunctionFactory & factory);
void registerFunctionIsIPAddressContainedIn(FunctionFactory &);

#if USE_ICU
void registerFunctionConvertCharset(FunctionFactory &);
#endif

void registerFunctionsMiscellaneous(FunctionFactory & factory)
{
    registerFunctionCurrentDatabase(factory);
    registerFunctionCurrentUser(factory);
    registerFunctionHostName(factory);
    registerFunctionHost(factory);
    registerFunctionFQDN(factory);
    registerFunctionVisibleWidth(factory);
    registerFunctionToTypeName(factory);
    registerFunctionGetSizeOfEnumType(factory);
    registerFunctionBlockSerializedSize(factory);
    registerFunctionToColumnTypeName(factory);
    registerFunctionDumpColumnStructure(factory);
    registerFunctionDefaultValueOfArgumentType(factory);
    registerFunctionDefaultValueOfTypeName(factory);
    registerFunctionBlockSize(factory);
    registerFunctionBlockNumber(factory);
    registerFunctionRowNumberInBlock(factory);
    registerFunctionRowNumberInAllBlocks(factory);
    registerFunctionNeighbor(factory);
    registerFunctionSleep(factory);
    registerFunctionSleepEachRow(factory);
    registerFunctionInvalidateStatsCache(factory);
    registerFunctionMaterialize(factory);
    registerFunctionIgnore(factory);
    registerFunctionIndexHint(factory);
    registerFunctionIdentity(factory);
    registerFunctionArrayJoin(factory);
    registerFunctionReplicate(factory);
    registerFunctionBar(factory);
    registerFunctionHasColumnInTable(factory);
    registerFunctionIsFinite(factory);
    registerFunctionIsInfinite(factory);
    registerFunctionIsNaN(factory);
    registerFunctionIfNotFinite(factory);
    registerFunctionThrowIf(factory);
    registerFunctionVersion(factory);
    registerFunctionBuildId(factory);
    registerFunctionUptime(factory);
    registerFunctionTimezone(factory);
    registerFunctionTimezoneOf(factory);
    registerFunctionRunningAccumulate(factory);
    registerFunctionRunningDifference(factory);
    registerFunctionRunningDifferenceStartingWithFirstValue(factory);
    registerFunctionRunningConcurrency(factory);
    registerFunctionFinalizeAggregation(factory);
    registerFunctionToLowCardinality(factory);
    registerFunctionLowCardinalityIndices(factory);
    registerFunctionLowCardinalityKeys(factory);
    registerFunctionLowCardinalityIsNoneEncoded(factory);
    registerFunctionsIn(factory);
    registerFunctionJoinGet(factory);
    registerFunctionFilesystem(factory);
    registerFunctionEvalMLMethod(factory);
    registerFunctionBasename(factory);
    registerFunctionTransform(factory);
    registerFunctionGetMacro(factory);
    registerFunctionGetScalar(factory);
    registerFunctionGetSetting(factory);
    registerFunctionIsConstant(factory);
    registerFunctionIsDecimalOverflow(factory);
    registerFunctionCountDigits(factory);
    registerFunctionGlobalVariable(factory);
    registerFunctionHasThreadFuzzer(factory);
    registerFunctionInitializeAggregation(factory);
    registerFunctionErrorCodeToName(factory);
    registerFunctionTcpPort(factory);
    registerFunctionByteSize(factory);
    registerFunctionFile(factory);
    registerFunctionConnectionId(factory);
    registerFunctionPartitionId(factory);
    registerFunctionPartitionStatus(factory);
    registerFunctionIsIPAddressContainedIn(factory);

#if USE_ICU
    registerFunctionConvertCharset(factory);
#endif
}

}
