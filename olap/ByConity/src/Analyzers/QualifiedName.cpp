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

#include <Analyzers/QualifiedName.h>

namespace DB
{

QualifiedName QualifiedName::extractQualifiedName(const ASTIdentifier & identifier)
{
    return QualifiedName(identifier.nameParts());
}

QualifiedName QualifiedName::extractQualifiedName(const DatabaseAndTableWithAlias & db_and_table)
{
    if (!db_and_table.alias.empty())
        return {db_and_table.alias};
    else if (!db_and_table.database.empty())
        return {db_and_table.database, db_and_table.table};
    else if (!db_and_table.table.empty())
        return {db_and_table.table};
    else
        return {};
}

QualifiedName QualifiedName::getPrefix() const
{
    if (this->empty())
        throw Exception("Can not get prefix for an empty qualified name.", ErrorCodes::LOGICAL_ERROR);

    std::vector<String> prefix_parts {parts.begin(), parts.end() - 1};
    return QualifiedName(prefix_parts);
}

bool QualifiedName::hasSuffix(const QualifiedName & suffix) const
{
    if (suffix.empty())
        return true;
    else if (this->empty())
        return false;
    else
        return this->getLast() == suffix.getLast() && getPrefix().hasSuffix(suffix.getPrefix());
}

String QualifiedName::toString() const
{
    String str;
    for (const auto & part: parts)
    {
        if (!str.empty())
            str += '.';
        str += part;
    }
    return str;
}

}
