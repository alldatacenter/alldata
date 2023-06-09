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

#include <common/types.h>
#include <Parsers/IAST_fwd.h>
#include <Parsers/IdentifierQuotingStyle.h>
#include <Common/Exception.h>
#include <Common/TypePromotion.h>
#include <Core/Settings.h>
#include <IO/WriteBufferFromString.h>

#include <algorithm>
#include <set>


class SipHash;


namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

using IdentifierNameSet = std::set<String>;

class WriteBuffer;
class ReadBuffer;

#define APPLY_AST_TYPES(M) \
    M(ASTAlterQuery) \
    M(ASTAlterCommand) \
    M(ASTAssignment) \
    M(ASTAsterisk) \
    M(ASTCheckQuery) \
    M(ASTColumnDeclaration) \
    M(ASTColumnsMatcher) \
    M(ASTColumnsApplyTransformer) \
    M(ASTColumnsExceptTransformer) \
    M(ASTColumnsReplaceTransformer) \
    M(ASTConstraintDeclaration) \
    M(ASTStorage) \
    M(ASTColumns) \
    M(ASTCreateQuery) \
    M(ASTCreateQuotaQuery) \
    M(ASTCreateRoleQuery) \
    M(ASTCreateRowPolicyQuery) \
    M(ASTCreateSettingsProfileQuery) \
    M(ASTCreateUserQuery) \
    M(ASTDictionaryLifetime) \
    M(ASTDictionaryLayout) \
    M(ASTDictionaryRange) \
    M(ASTDictionarySettings) \
    M(ASTDictionary) \
    M(ASTDictionaryAttributeDeclaration) \
    M(ASTDropAccessEntityQuery) \
    M(ASTDropQuery) \
    M(ASTExplainQuery) \
    M(ASTExpressionList) \
    M(ASTExternalDDLQuery) \
    M(ASTFunction) \
    M(ASTFunctionWithKeyValueArguments) \
    M(ASTGrantQuery) \
    M(ASTIdentifier) \
    M(ASTIndexDeclaration) \
    M(ASTInsertQuery) \
    M(ASTKillQueryQuery) \
    M(ASTLiteral) \
    M(ASTNameTypePair) \
    M(ASTOptimizeQuery) \
    M(ASTOrderByElement) \
    M(ASTPair) \
    M(ASTPartition) \
    M(ASTProjectionDeclaration) \
    M(ASTProjectionSelectQuery) \
    M(ASTQualifiedAsterisk) \
    M(ASTQueryParameter) \
    M(ASTQueryWithOutput) \
    M(ASTQueryWithTableAndOutput) \
    M(ASTRefreshQuery) \
    M(ASTRenameQuery) \
    M(ASTRolesOrUsersSet) \
    M(ASTRowPolicyName) \
    M(ASTRowPolicyNames) \
    M(ASTSampleRatio) \
    M(ASTSelectQuery) \
    M(ASTSelectWithUnionQuery) \
    M(ASTSetQuery) \
    M(ASTSetRoleQuery) \
    M(ASTSettingsProfileElement) \
    M(ASTSettingsProfileElements) \
    M(ASTShowAccessEntitiesQuery) \
    M(ASTShowCreateAccessEntityQuery) \
    M(ASTShowGrantsQuery) \
    M(ASTShowTablesQuery) \
    M(ASTSubquery) \
    M(ASTSystemQuery) \
    M(ASTTableIdentifier) \
    M(ASTTableExpression) \
    M(ASTTableJoin) \
    M(ASTArrayJoin) \
    M(ASTTablesInSelectQueryElement) \
    M(ASTTablesInSelectQuery) \
    M(ASTTTLElement) \
    M(ASTUseQuery) \
    M(ASTUserNameWithHost) \
    M(ASTUserNamesWithHost) \
    M(ASTWatchQuery) \
    M(ASTWindowDefinition) \
    M(ASTWithElement) \
    M(ASTFieldReference) \
    M(ASTCreateStatsQuery) \
    M(ASTDropStatsQuery) \
    M(ASTShowStatsQuery) \
    M(ASTSelectIntersectExceptQuery) \
    M(ASTWindowListElement) \
    M(ASTTEALimit) \
    M(ASTDumpInfoQuery) \
    M(ASTReproduceQuery) \
    M(ASTPartToolKit) \
    M(ASTQuantifiedComparison) \
    M(ASTTableColumnReference)
#define ENUM_TYPE(ITEM) ITEM,

enum class ASTType : UInt8
{
    APPLY_AST_TYPES(ENUM_TYPE) UNDEFINED,
};

#undef ENUM_TYPE

/** Element of the syntax tree (hereinafter - directed acyclic graph with elements of semantics)
  */
class IAST : public std::enable_shared_from_this<IAST>, public TypePromotion<IAST>
{
public:
    ASTs children;

    virtual ~IAST() = default;
    IAST() = default;
    IAST(const IAST &) = default;
    IAST & operator=(const IAST &) = default;

    /** Get the canonical name of the column if the element is a column */
    String getColumnName() const;

    /** Same as the above but ensure no alias names are used. This is for index analysis */
    String getColumnNameWithoutAlias() const;

    virtual void appendColumnName(WriteBuffer &) const
    {
        throw Exception("Trying to get name of not a column: " + getID(), ErrorCodes::LOGICAL_ERROR);
    }

    virtual void appendColumnNameWithoutAlias(WriteBuffer &) const
    {
        throw Exception("Trying to get name of not a column: " + getID(), ErrorCodes::LOGICAL_ERROR);
    }

    /** Get the alias, if any, or the canonical name of the column, if it is not. */
    virtual String getAliasOrColumnName() const { return getColumnName(); }

    /** Get the alias, if any, or an empty string if it does not exist, or if the element does not support aliases. */
    virtual String tryGetAlias() const { return String(); }

    /** Set the alias. */
    virtual void setAlias(const String & /*to*/)
    {
        throw Exception("Can't set alias of " + getColumnName(), ErrorCodes::LOGICAL_ERROR);
    }

    /** Get the text that identifies this element. */
    virtual String getID(char delimiter = '_') const = 0;

    /// AST type, it's used for serialize/deserialize.
    virtual ASTType getType() const { throw Exception("Not support", ErrorCodes::NOT_IMPLEMENTED); }

    ASTPtr ptr() { return shared_from_this(); }

    /** Get a deep copy of the tree. Cloned object must have the same range. */
    virtual ASTPtr clone() const = 0;

    /** Get hash code, identifying this element and its subtree.
      */
    using Hash = std::pair<UInt64, UInt64>;
    Hash getTreeHash() const;
    void updateTreeHash(SipHash & hash_state) const;
    virtual void updateTreeHashImpl(SipHash & hash_state) const;

    void dumpTree(WriteBuffer & ostr, size_t indent = 0) const;
    std::string dumpTree(size_t indent = 0) const;

    /** Check the depth of the tree.
      * If max_depth is specified and the depth is greater - throw an exception.
      * Returns the depth of the tree.
      */
    size_t checkDepth(size_t max_depth) const
    {
        return checkDepthImpl(max_depth, 0);
    }

    /** Get total number of tree elements
     */
    size_t size() const;

    /** Same for the total number of tree elements.
      */
    size_t checkSize(size_t max_size) const;

    /** Get `set` from the names of the identifiers
     */
    virtual void collectIdentifierNames(IdentifierNameSet & set) const
    {
        for (const auto & child : children)
            child->collectIdentifierNames(set);
    }

    template <typename T>
    void set(T * & field, const ASTPtr & child)
    {
        if (!child)
            return;

        T * casted = dynamic_cast<T *>(child.get());
        if (!casted)
            throw Exception("Could not cast AST subtree", ErrorCodes::LOGICAL_ERROR);

        children.push_back(child);
        field = casted;
    }

    template <typename T>
    void replace(T * & field, const ASTPtr & child)
    {
        if (!child)
            throw Exception("Trying to replace AST subtree with nullptr", ErrorCodes::LOGICAL_ERROR);

        T * casted = dynamic_cast<T *>(child.get());
        if (!casted)
            throw Exception("Could not cast AST subtree", ErrorCodes::LOGICAL_ERROR);

        for (ASTPtr & current_child : children)
        {
            if (current_child.get() == field)
            {
                current_child = child;
                field = casted;
                return;
            }
        }

        throw Exception("AST subtree not found in children", ErrorCodes::LOGICAL_ERROR);
    }

    template <typename T>
    void setOrReplace(T * & field, const ASTPtr & child)
    {
        if (field)
            replace(field, child);
        else
            set(field, child);
    }

    void setOrReplaceAST(ASTPtr & old_ast, const ASTPtr & new_ast)
    {
        if (!new_ast)
            throw Exception("Trying to set or replace AST subtree with nullptr", ErrorCodes::LOGICAL_ERROR);

        if (old_ast == new_ast)
            return;

        /// set ast
        if (!old_ast)
        {
            old_ast = new_ast;
            children.push_back(old_ast);
            return;
        }

        /// replace ast
        for (ASTPtr & current_child: children)
        {
            if (current_child == old_ast)
            {
                current_child = new_ast;
                old_ast = new_ast;
                return;
            }
        }

        throw Exception("AST subtree not found in children", ErrorCodes::LOGICAL_ERROR);
    }
    ASTs & getChildren() { return children; }
    void replaceChildren(ASTs & children_) { children = std::move(children_); }

    /// Convert to a string.

    /// Format settings.
    struct FormatSettings
    {
        WriteBuffer & ostr;
        bool hilite = false;
        bool one_line;
        bool always_quote_identifiers = false;
        bool without_alias = false;
        IdentifierQuotingStyle identifier_quoting_style = IdentifierQuotingStyle::Backticks;

        // Newline or whitespace.
        char nl_or_ws;

        FormatSettings(WriteBuffer & ostr_, bool one_line_, bool without_alias_ = false)
            : ostr(ostr_), one_line(one_line_), without_alias(without_alias_)
        {
            nl_or_ws = one_line ? ' ' : '\n';
        }

        FormatSettings(WriteBuffer & ostr_, const FormatSettings & other)
            : ostr(ostr_), hilite(other.hilite), one_line(other.one_line),
            always_quote_identifiers(other.always_quote_identifiers),
            identifier_quoting_style(other.identifier_quoting_style)
        {
            nl_or_ws = one_line ? ' ' : '\n';
        }

        void writeIdentifier(const String & name) const;
    };

    /// State. For example, a set of nodes can be remembered, which we already walk through.
    struct FormatState
    {
        /** The SELECT query in which the alias was found; identifier of a node with such an alias.
          * It is necessary that when the node has met again, output only the alias.
          */
        std::set<std::tuple<
            const IAST * /* SELECT query node */,
            std::string /* alias */,
            Hash /* printed content */>> printed_asts_with_alias;
    };

    /// The state that is copied when each node is formatted. For example, nesting level.
    struct FormatStateStacked
    {
        UInt8 indent = 0;
        bool need_parens = false;
        bool expression_list_always_start_on_new_line = false;  /// Line feed and indent before expression list even if it's of single element.
        bool expression_list_prepend_whitespace = false; /// Prepend whitespace (if it is required)
        bool surround_each_list_element_with_parens = false;
        const IAST * current_select = nullptr;
    };

    void format(const FormatSettings & settings) const
    {
        FormatState state;
        formatImpl(settings, state, FormatStateStacked());
    }

    virtual void formatImpl(const FormatSettings & /*settings*/, FormatState & /*state*/, FormatStateStacked /*frame*/) const
    {
        throw Exception("Unknown element in AST: " + getID(), ErrorCodes::LOGICAL_ERROR);
    }

    // A simple way to add some user-readable context to an error message.
    std::string formatForErrorMessage() const;
    template <typename AstArray>
    static std::string formatForErrorMessage(const AstArray & array);

    void cloneChildren();

    virtual void serialize(WriteBuffer &) const { throw Exception("Not implement serialize of " + getID(), ErrorCodes::NOT_IMPLEMENTED); }
    virtual void deserializeImpl(ReadBuffer &) { throw Exception("Not implement deserializeImpl AST", ErrorCodes::NOT_IMPLEMENTED); }
    static ASTPtr deserialize(ReadBuffer &) { throw Exception("Not implement deserialize AST", ErrorCodes::NOT_IMPLEMENTED); }

public:
    /// For syntax highlighting.
    static const char * hilite_keyword;
    static const char * hilite_identifier;
    static const char * hilite_function;
    static const char * hilite_operator;
    static const char * hilite_alias;
    static const char * hilite_substitution;
    static const char * hilite_none;

private:
    size_t checkDepthImpl(size_t max_depth, size_t level) const;
};

template <typename AstArray>
std::string IAST::formatForErrorMessage(const AstArray & array)
{
    WriteBufferFromOwnString buf;
    for (size_t i = 0; i < array.size(); ++i)
    {
        if (i > 0)
        {
            const char * delim = ", ";
            buf.write(delim, strlen(delim));
        }
        array[i]->format(IAST::FormatSettings(buf, true /* one line */));
    }
    return buf.str();
}

}
