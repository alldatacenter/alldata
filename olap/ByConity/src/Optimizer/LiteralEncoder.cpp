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

#include <Optimizer/LiteralEncoder.h>
#include <Optimizer/makeCastFunction.h>

#include <Core/Types.h>
#include <Functions/FunctionFactory.h>
#include <DataTypes/DataTypeLowCardinality.h>
#include <DataTypes/DataTypeNullable.h>
#include <Interpreters/Context.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTLiteral.h>

namespace DB
{

ASTPtr LiteralEncoder::encode(Field field, const DataTypePtr & type, ContextMutablePtr context)
{
    auto literal_ast = std::make_shared<ASTLiteral>(std::move(field));

    if (context->getSettingsRef().legacy_column_name_of_tuple_literal)
    {
        if (literal_ast->value.getType() == Field::Types::Tuple)
        {
            literal_ast->use_legacy_column_name_of_tuple = true;
        }
    }

    DataTypePtr result_type = TypeAnalyzer::getType(literal_ast, context, {});

    // use IDataType::getName as an additional check, as IDataType::equals may return unexpected result for data types
    // with custom serialization. e.g. equals(DataTypeUInt32, DataTypeIpv4) => true
    if (type->equals(*result_type) && type->getName() == result_type->getName())
        return literal_ast;

    return makeCastFunction(literal_ast, type);
}

ASTPtr LiteralEncoder::encodeForComparisonExpr(Field field, const DataTypePtr & type, ContextMutablePtr context)
{
    // do not add cast for NULL & simple types
    auto base_type = removeNullable(removeLowCardinality(type));
    if (field.isNull() || isNumber(base_type) || isStringOrFixedString(base_type))
        return std::make_shared<ASTLiteral>(std::move(field));

    return encode(std::move(field), base_type, context);
}
}
