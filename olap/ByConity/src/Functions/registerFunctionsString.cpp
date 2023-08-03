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
#    include "config_functions.h"
#endif

namespace DB
{

class FunctionFactory;

void registerFunctionRepeat(FunctionFactory &);
void registerFunctionEmpty(FunctionFactory &);
void registerFunctionNotEmpty(FunctionFactory &);
void registerFunctionLength(FunctionFactory &);
void registerFunctionLengthUTF8(FunctionFactory &);
void registerFunctionIsValidUTF8(FunctionFactory &);
void registerFunctionToValidUTF8(FunctionFactory &);
void registerFunctionLower(FunctionFactory &);
void registerFunctionUpper(FunctionFactory &);
void registerFunctionLowerUTF8(FunctionFactory &);
void registerFunctionUpperUTF8(FunctionFactory &);
void registerFunctionReverse(FunctionFactory &);
void registerFunctionReverseUTF8(FunctionFactory &);
void registerFunctionsConcat(FunctionFactory &);
void registerFunctionFormat(FunctionFactory &);
void registerFunctionFormatRow(FunctionFactory &);
void registerFunctionSubstring(FunctionFactory &);
void registerFunctionCRC(FunctionFactory &);
void registerFunctionAppendTrailingCharIfAbsent(FunctionFactory &);
void registerFunctionStartsWith(FunctionFactory &);
void registerFunctionEndsWith(FunctionFactory &);
void registerFunctionTrim(FunctionFactory &);
void registerFunctionPadString(FunctionFactory &);
void registerFunctionRegexpQuoteMeta(FunctionFactory &);
void registerFunctionNormalizeQuery(FunctionFactory &);
void registerFunctionNormalizedQueryHash(FunctionFactory &);
void registerFunctionCountMatches(FunctionFactory &);
void registerFunctionEncodeXMLComponent(FunctionFactory &);
void registerFunctionDecodeXMLComponent(FunctionFactory &);
void registerFunctionExtractTextFromHTML(FunctionFactory &);
void registerFunctionsAppVersionCompare(FunctionFactory &);
void registerFunctionUnicodeToUTF8(FunctionFactory &);
void registerFunctionPumpZookeeper(FunctionFactory &);


#if USE_BASE64
void registerFunctionBase64Encode(FunctionFactory &);
void registerFunctionBase64Decode(FunctionFactory &);
void registerFunctionTryBase64Decode(FunctionFactory &);
#endif

void registerFunctionsString(FunctionFactory & factory)
{
    registerFunctionRepeat(factory);
    registerFunctionEmpty(factory);
    registerFunctionNotEmpty(factory);
    registerFunctionLength(factory);
    registerFunctionLengthUTF8(factory);
    registerFunctionIsValidUTF8(factory);
    registerFunctionToValidUTF8(factory);
    registerFunctionLower(factory);
    registerFunctionUpper(factory);
    registerFunctionLowerUTF8(factory);
    registerFunctionUpperUTF8(factory);
    registerFunctionReverse(factory);
    registerFunctionCRC(factory);
    registerFunctionReverseUTF8(factory);
    registerFunctionsConcat(factory);
    registerFunctionFormat(factory);
    registerFunctionFormatRow(factory);
    registerFunctionSubstring(factory);
    registerFunctionAppendTrailingCharIfAbsent(factory);
    registerFunctionStartsWith(factory);
    registerFunctionEndsWith(factory);
    registerFunctionTrim(factory);
    registerFunctionPadString(factory);
    registerFunctionRegexpQuoteMeta(factory);
    registerFunctionNormalizeQuery(factory);
    registerFunctionNormalizedQueryHash(factory);
    registerFunctionCountMatches(factory);
    registerFunctionEncodeXMLComponent(factory);
    registerFunctionDecodeXMLComponent(factory);
    registerFunctionExtractTextFromHTML(factory);
    registerFunctionsAppVersionCompare(factory);
    registerFunctionUnicodeToUTF8(factory);
    registerFunctionPumpZookeeper(factory);
#if USE_BASE64
    registerFunctionBase64Encode(factory);
    registerFunctionBase64Decode(factory);
    registerFunctionTryBase64Decode(factory);
#endif
}

}
