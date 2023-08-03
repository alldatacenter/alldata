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

#pragma once

#include <Interpreters/IInterpreter.h>
#include <Parsers/IAST_fwd.h>
#include <Parsers/ASTSelectQuery.h>

namespace DB
{

/// Returns single row with explain results
class InterpreterExplainQuery : public IInterpreter, WithMutableContext
{
public:
    InterpreterExplainQuery(const ASTPtr & query_, ContextMutablePtr context_) : WithMutableContext(context_),
        query(query_), log(&Poco::Logger::get("InterpreterExplainQuery")) {}

    BlockIO execute() override;

    Block getSampleBlock();

private:
    ASTPtr query;
    Poco::Logger * log;

    BlockInputStreamPtr executeImpl();

    void rewriteDistributedToLocal(ASTPtr & ast);

    void elementDatabaseAndTable(const ASTSelectQuery & select_query, const ASTPtr & where, WriteBuffer & buffer);

    void elementWhere(const ASTPtr & where, WriteBuffer & buffer);

    void elementDimensions(const ASTPtr & select, WriteBuffer & buffer);

    void elementMetrics(const ASTPtr & select, WriteBuffer & buffer);

    void elementGroupBy(const ASTPtr & group_by, WriteBuffer & buffer);

    void listPartitionKeys(StoragePtr & storage, WriteBuffer & buffer);

    void listRowsOfOnePartition(StoragePtr & storage, const ASTPtr & group_by, const ASTPtr & where, WriteBuffer & buffer);

    std::optional<String> getActivePartCondition(StoragePtr & storage);

    void explainUsingOptimizer(const ASTPtr & ast, WriteBuffer & buffer, bool & single_line);
};


}
