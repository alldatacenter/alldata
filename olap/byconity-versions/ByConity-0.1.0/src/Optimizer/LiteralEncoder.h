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

#include <Analyzers/TypeAnalyzer.h>
#include <Core/Field.h>
#include <DataTypes/IDataType.h>
#include <Interpreters/Context_fwd.h>
#include <Parsers/IAST_fwd.h>

namespace DB
{

class LiteralEncoder
{
public:
    // create an ASTLiteral by a Field and its desired type
    static ASTPtr encode(Field field, const DataTypePtr & type, ContextMutablePtr context);

    // create an ASTLiteral for a comparison expression e.g. symbol_x = ASTLiteral(`field`),
    // param `type` is the type of symbol_x
    static ASTPtr encodeForComparisonExpr(Field field, const DataTypePtr & type, ContextMutablePtr context);
};
}
