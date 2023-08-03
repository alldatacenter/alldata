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

void registerFunctionE(FunctionFactory & factory);
void registerFunctionPi(FunctionFactory & factory);
void registerFunctionExp(FunctionFactory & factory);
void registerFunctionLog(FunctionFactory & factory);
void registerFunctionExp2(FunctionFactory & factory);
void registerFunctionLog2(FunctionFactory & factory);
void registerFunctionLog1p(FunctionFactory & factory);
void registerFunctionExp10(FunctionFactory & factory);
void registerFunctionLog10(FunctionFactory & factory);
void registerFunctionSqrt(FunctionFactory & factory);
void registerFunctionCbrt(FunctionFactory & factory);
void registerFunctionErf(FunctionFactory & factory);
void registerFunctionErfc(FunctionFactory & factory);
void registerFunctionLGamma(FunctionFactory & factory);
void registerFunctionTGamma(FunctionFactory & factory);
void registerFunctionSin(FunctionFactory & factory);
void registerFunctionCos(FunctionFactory & factory);
void registerFunctionTan(FunctionFactory & factory);
void registerFunctionAsin(FunctionFactory & factory);
void registerFunctionAcos(FunctionFactory & factory);
void registerFunctionAtan(FunctionFactory & factory);
void registerFunctionAtan2(FunctionFactory & factory);
void registerFunctionSigmoid(FunctionFactory & factory);
void registerFunctionHypot(FunctionFactory & factory);
void registerFunctionSinh(FunctionFactory & factory);
void registerFunctionCosh(FunctionFactory & factory);
void registerFunctionTanh(FunctionFactory & factory);
void registerFunctionAsinh(FunctionFactory & factory);
void registerFunctionAcosh(FunctionFactory & factory);
void registerFunctionAtanh(FunctionFactory & factory);
void registerFunctionPow(FunctionFactory & factory);
void registerFunctionSign(FunctionFactory & factory);
void registerFunctionConv(FunctionFactory & factory);


void registerFunctionsMath(FunctionFactory & factory)
{
    registerFunctionE(factory);
    registerFunctionPi(factory);
    registerFunctionExp(factory);
    registerFunctionLog(factory);
    registerFunctionExp2(factory);
    registerFunctionLog2(factory);
    registerFunctionLog1p(factory);
    registerFunctionExp10(factory);
    registerFunctionLog10(factory);
    registerFunctionSqrt(factory);
    registerFunctionCbrt(factory);
    registerFunctionErf(factory);
    registerFunctionErfc(factory);
    registerFunctionLGamma(factory);
    registerFunctionTGamma(factory);
    registerFunctionSin(factory);
    registerFunctionCos(factory);
    registerFunctionTan(factory);
    registerFunctionAsin(factory);
    registerFunctionAcos(factory);
    registerFunctionAtan(factory);
    registerFunctionAtan2(factory);
    registerFunctionSigmoid(factory);
    registerFunctionHypot(factory);
    registerFunctionSinh(factory);
    registerFunctionCosh(factory);
    registerFunctionTanh(factory);
    registerFunctionAsinh(factory);
    registerFunctionAcosh(factory);
    registerFunctionAtanh(factory);
    registerFunctionPow(factory);
    registerFunctionSign(factory);
    registerFunctionConv(factory);
}

}
