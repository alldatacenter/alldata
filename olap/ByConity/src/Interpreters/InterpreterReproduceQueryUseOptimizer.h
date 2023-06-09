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
class InterpreterReproduceQueryUseOptimizer : public IInterpreter
{
public:
    InterpreterReproduceQueryUseOptimizer(ASTPtr & query_ptr_, ContextMutablePtr & context_)
        : query_ptr(query_ptr_), context(context_), log(&Poco::Logger::get("InterpreterReproduceQueryUseOptimizer"))
    {
    }

    ASTPtr parse(const String & query);
    QueryPlanPtr plan(ASTPtr ast);
    void createTablesFromJson(const String & path);
    void createClusterInfo(const String & path);
    void resetTransaction();

    BlockIO execute() override;

private:
    ASTPtr query_ptr;
    ContextMutablePtr context;
    SelectQueryOptions options;
    Poco::Logger * log;
};

}
