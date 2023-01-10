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
#include <Interpreters/IInterpreter.h>
#include <Interpreters/SelectQueryOptions.h>

namespace DB
{
class InterpreterDumpInfoQueryUseOptimizer : public IInterpreter
{
public:
    InterpreterDumpInfoQueryUseOptimizer(ASTPtr & query_ptr_, ContextMutablePtr context_)
        : query_ptr(query_ptr_), context(context_), log(&Poco::Logger::get("InterpreterDumpInfoQueryUseOptimizer"))
    {
    }

    BlockIO execute() override;

private:
    ASTPtr query_ptr;
    ContextMutablePtr context;
    Poco::Logger * log;
    SelectQueryOptions options;
};
}
