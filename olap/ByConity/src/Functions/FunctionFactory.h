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

#include <Interpreters/Context_fwd.h>
#include <Common/IFactoryWithAliases.h>
#include <Functions/IFunction.h>
#include <Functions/IFunctionAdaptors.h>

#include <functional>
#include <memory>
#include <string>
#include <unordered_map>
#include <optional>

namespace DB
{

/** Creates function by name.
  * Function could use for initialization (take ownership of shared_ptr, for example)
  *  some dictionaries from Context.
  */
class FunctionFactory : private boost::noncopyable,
                        public IFactoryWithAliases<std::function<FunctionOverloadResolverPtr(ContextPtr)>>
{
public:
    static FunctionFactory & instance();

    template <typename Function>
    void registerFunction(CaseSensitiveness case_sensitiveness = CaseSensitive)
    {
        registerFunction<Function>(Function::name, case_sensitiveness);
    }

    template <typename Function>
    void registerFunction(const std::string & name, CaseSensitiveness case_sensitiveness = CaseSensitive)
    {

        if constexpr (std::is_base_of_v<IFunction, Function>)
            registerFunction(name, &adaptFunctionToOverloadResolver<Function>, case_sensitiveness);
        else
            registerFunction(name, &Function::create, case_sensitiveness);
    }

    /// This function is used by YQL - internal Yandex product that depends on ClickHouse by source code.
    std::vector<std::string> getAllNames() const;

    /// Throws an exception if not found.
    FunctionOverloadResolverPtr get(const std::string & name, ContextPtr context) const;

    /// Returns nullptr if not found.
    FunctionOverloadResolverPtr tryGet(const std::string & name, ContextPtr context) const;

    // Return the canonical name (the name used in registration) whatever the input is a name
    // or an alias and whether the canonical name is case-sensitive. Return the lowercase names
    // for case-insensitive names.
    std::optional<String> getCanonicalName(const String & name_or_alias) const;

    /// The same methods to get developer interface implementation.
    FunctionOverloadResolverPtr getImpl(const std::string & name, ContextPtr context) const;
    FunctionOverloadResolverPtr tryGetImpl(const std::string & name, ContextPtr context) const;

    /// Register a function by its name.
    /// No locking, you must register all functions before usage of get.
    void registerFunction(
        const std::string & name,
        Value creator,
        CaseSensitiveness case_sensitiveness = CaseSensitive);

private:
    using Functions = std::unordered_map<std::string, Value>;

    Functions functions;
    Functions case_insensitive_functions;

    template <typename Function>
    static FunctionOverloadResolverPtr adaptFunctionToOverloadResolver(ContextPtr context)
    {
        return std::make_unique<FunctionToOverloadResolverAdaptor>(Function::create(context));
    }

    const Functions & getMap() const override { return functions; }

    const Functions & getCaseInsensitiveMap() const override { return case_insensitive_functions; }

    String getFactoryName() const override { return "FunctionFactory"; }
};

}
