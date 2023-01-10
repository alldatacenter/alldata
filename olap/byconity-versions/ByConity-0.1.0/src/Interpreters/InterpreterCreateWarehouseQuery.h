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
#include <Interpreters/Context_fwd.h>
#include <Parsers/ASTCreateWarehouseQuery.h>


namespace DB
{

/** Creates a logical VW for query and task execution.
  * The physical VW resource will be separately started up, and the workers will register themselves to the Resource Manager
  * Syntax is as follows:
  * CREATE WAREHOUSE [IF NOT EXISTS] vwname
  * [SIZE {XS, S, M, L, XL, XXL}] // Either size or num_workers must be specified
  * [SETTINGS]
  * [auto_suspend = <some number in seconds>]
  * [auto_resume = 0|1,]
  * [max_worker_groups = <num>,]
  * [min_worker_groups = <num>,]
  * [num_workers = <num>,]
  * [max_concurrent_queries = num]
  */
class InterpreterCreateWarehouseQuery : public IInterpreter, WithContext
{
public:
    InterpreterCreateWarehouseQuery(const ASTPtr & query_ptr_, ContextPtr context_);

    /// Create table or database.
    BlockIO execute() override;

private:
    ASTPtr query_ptr;
};
}
