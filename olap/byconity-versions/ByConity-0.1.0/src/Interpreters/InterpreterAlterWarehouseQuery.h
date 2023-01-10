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
#include <Parsers/ASTAlterWarehouseQuery.h>


namespace DB
{

/** Alters a logical virtual warehouse's settings.
  * Altering of resource behaviour (e.g. Num of workers) will not be performed by CNCH,
  * and serves more as collected information for CNCH
  * Syntax is as follows:
  * ALTER WAREHOUSE  vwname
  * [RENAME TO some_name]
  * [SIZE {XS, S, M, L, XL, XXL}]
  * [SETTINGS]
  * [auto_suspend = <some number in seconds>]
  * [auto_resume = 0|1,]
  * [max_worker_groups = <num>,]
  * [min_worker_groups = <num>,]
  * [num_workers = <num>,]
  * [max_concurrent_queries = num]
  */
class InterpreterAlterWarehouseQuery : public IInterpreter, WithContext
{
public:
    InterpreterAlterWarehouseQuery(const ASTPtr & query_ptr_, ContextPtr context_);

    /// Alter table or database.
    BlockIO execute() override;

private:
    ASTPtr query_ptr;
};
}
