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

#include <Core/Names.h>
#include <QueryPlan/SymbolAllocator.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/ProjectionStep.h>

namespace DB
{
class IAST;
using ASTPtr = std::shared_ptr<IAST>;

// todo@kaixi: implement for all plan nodes
class SymbolMapper
{
public:
    using Symbol = std::string;
    using MappingFunction = std::function<std::string(const std::string &)>;

    explicit SymbolMapper(MappingFunction mapping_function_) : mapping_function(std::move(mapping_function_)) { }

    static SymbolMapper symbolMapper(const std::unordered_map<Symbol, Symbol> & mapping);
    static SymbolMapper symbolReallocator(std::unordered_map<Symbol, Symbol> & mapping, SymbolAllocator & symbolAllocator);

    std::string map(const std::string & symbol) { return mapping_function(symbol); }
    Names map(const Names & symbols);
    NameSet mapToDistinct(const Names & symbols);
    NamesAndTypes map(const NamesAndTypes & name_and_types);
    Block map(const Block & name_and_types);
    DataStreams map(const DataStreams & data_streams);
    DataStream map(const DataStream & data_stream);
    ASTPtr map(const ASTPtr & expr);
    ASTPtr map(const ConstASTPtr & expr);

    std::shared_ptr<JoinStep> map(const JoinStep & join);

private:
    MappingFunction mapping_function;

    class IdentifierRewriter;
};

}
