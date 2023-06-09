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

#include "IAction.h"
#include <Interpreters/Context_fwd.h>
#include <Interpreters/Context.h>

namespace DB
{

IAction::IAction(const ContextPtr & query_context_, const TxnTimestamp & txn_id_)
    : WithContext(query_context_), global_context(*query_context_->getGlobalContext()), txn_id(txn_id_)
{
}

}
