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

#include <Parsers/IAST.h>
#include <Interpreters/Context_fwd.h>
#include <cstddef>
#include <memory>


namespace DB
{

/** Prepares a pipe which produce data containing in INSERT query
  * Head of inserting data could be stored in INSERT ast directly
  * Remaining (tail) data could be stored in input_buffer_tail_part
  */

class Pipe;

Pipe getSourceFromFromASTInsertQuery(
        const ASTPtr & ast,
        ReadBuffer * input_buffer_tail_part,
        const Block & header,
        ContextPtr context,
        const ASTPtr & input_function);

}
