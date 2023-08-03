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

#include <Analyzers/ImplementFunctionVisitor.h>

#include <Common/StringUtils/StringUtils.h>
#include <Parsers/ASTAsterisk.h>
#include <Storages/StorageMaterializedView.h>

namespace DB
{

namespace ErrorCodes
{
}

void ImplementFunction::visit(ASTFunction & node, ASTPtr &)
{
    rewriteAsTranslateQualifiedNamesVisitorDo(node);
    rewriteAsQueryNormalizerDo(node);
}

void ImplementFunction::rewriteAsTranslateQualifiedNamesVisitorDo(ASTFunction & node)
{
    ASTPtr & func_arguments = node.arguments;

    if (!func_arguments) return;

    String func_name_lowercase = Poco::toLower(node.name);
    if (func_name_lowercase == "count" &&
        func_arguments->children.size() == 1 &&
        func_arguments->children[0]->as<ASTAsterisk>())
        func_arguments->children.clear();
}

void ImplementFunction::rewriteAsQueryNormalizerDo(ASTFunction &)
{
}

}
