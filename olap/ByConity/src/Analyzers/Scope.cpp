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

#include <Analyzers/Scope.h>

namespace DB
{

bool FieldDescription::matchName(const QualifiedName & target_name) const
{
    // match name
    bool matched = !name.empty() && !target_name.empty() && name == target_name.getLast();

    // match prefix
    if (matched)
        matched = prefix.hasSuffix(target_name.getPrefix());

    return matched;
}

bool FieldDescription::matchName(const String & target_name) const
{
    return !name.empty() && name == target_name;
}

FieldDescription FieldDescription::withNewName(const String & new_name) const
{
    return {new_name, type, prefix, origin_table, origin_table_ast, origin_column, index_of_origin_scope, substituted_by_asterisk};
}

FieldDescription FieldDescription::withNewPrefix(const QualifiedName & new_prefix) const
{
    return {name, type, new_prefix, origin_table, origin_table_ast, origin_column, index_of_origin_scope, substituted_by_asterisk};
}

void FieldDescription::copyOriginInfo(const FieldDescription & source_field)
{
    origin_table = source_field.origin_table;
    origin_table_ast = source_field.origin_table_ast;
    origin_column = source_field.origin_column;
    index_of_origin_scope = source_field.index_of_origin_scope;
}

ResolvedField::ResolvedField(ScopePtr scope_, size_t local_index_): scope(scope_), local_index(local_index_)
{
    hierarchy_index = scope->getHierarchyOffset() + local_index;
}

const FieldDescription & ResolvedField::getFieldDescription() const
{
    return scope->at(local_index);
}

ScopePtr Scope::getLocalParent() const
{
    return query_boundary ? nullptr : parent;
}

bool Scope::isLocalScope(ScopePtr other) const
{
    return this == other || (getLocalParent() != nullptr && getLocalParent()->isLocalScope(other));
}

size_t Scope::getHierarchyOffset() const
{
    const auto *local_parent = getLocalParent();
    return local_parent ? local_parent->getHierarchySize() : 0;
}

size_t Scope::getHierarchySize() const
{
    const auto *local_parent = getLocalParent();
    return (local_parent ? local_parent->getHierarchySize() : 0) + size();
}

Names Scope::getOriginColumns() const
{
    Names columns;
    columns.reserve(field_descriptions.size());

    for (const auto & field: field_descriptions)
        columns.push_back(field.origin_column);

    return columns;
}

ScopePtr ScopeFactory::createScope(Scope::ScopeType type, ScopePtr parent, bool query_boundary, FieldDescriptions field_descriptions)
{
    scopes.emplace_back(type, parent, query_boundary, std::move(field_descriptions));
    return &scopes.back();
}

ScopePtr ScopeFactory::createLambdaScope(ScopePtr parent, FieldDescriptions field_descriptions)
{
    return createScope(Scope::ScopeType::LAMBDA, parent, false, std::move(field_descriptions));
}

}
