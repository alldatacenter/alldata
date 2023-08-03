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

#include <Interpreters/Context.h>
#include <Interpreters/InDepthNodeVisitor.h>
#include <Parsers/ASTFunction.h>

namespace DB
{

// this visitor keeps same ASTFunction rewriting logic of `TranslateQualifiedNamesVisitor` & `QueryNormalizer`
struct ImplementFunction
{
    using TypeToVisit = ASTFunction;

    struct ExtractedSettings
    {
        const String count_distinct_implementation;

        template <typename T>
        ExtractedSettings(const T & settings_): // NOLINT(google-explicit-constructor)
            count_distinct_implementation(settings_.count_distinct_implementation)
        {}
    };

    explicit ImplementFunction(ContextMutablePtr context_):
        context(std::move(context_)), settings(context->getSettingsRef())
    {}

    void visit(ASTFunction & function, ASTPtr & ast);

    static void rewriteAsTranslateQualifiedNamesVisitorDo(ASTFunction & node);
    static void rewriteAsQueryNormalizerDo(ASTFunction & node);

    ContextMutablePtr context;
    ExtractedSettings settings;
};

using ImplementFunctionMatcher = OneTypeMatcher<ImplementFunction>;
using ImplementFunctionVisitor = InDepthNodeVisitor<ImplementFunctionMatcher, true>;

}
