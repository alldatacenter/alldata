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

#include <Common/ConfigurationCommon.h>

#include <Common/Exception.h>
#include <common/logger_useful.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int NO_ELEMENTS_IN_CONFIG;
}

bool ConfigurationFieldBase::checkField(const PocoAbstractConfig & config, const String & current_prefix)
{
    full_key = current_prefix.empty() ? init_key : current_prefix + "." + init_key;

    if (config.has(full_key))
    {
        existed = true;

        if (deprecated())
        {
            LOG_WARNING(
                &Poco::Logger::get("Configuration"), "Config element {} is deprecated. Please remove corresponding tags!", full_key);
        }
    }
    else
    {
        if (recommended())
        {
            LOG_DEBUG(
                &Poco::Logger::get("Configuration"),
                "Config element {} is recommended to set in config.xml. You'd better customize it.", full_key);
        }
        else if (required())
        {
            throw Exception(
                "Config element " + full_key + " is required, but it is not in config.xml. Please add corresponding tags!",
                ErrorCodes::NO_ELEMENTS_IN_CONFIG);
        }
    }
    return existed;
}

void IConfiguration::loadFromPocoConfig(const PocoAbstractConfig & config, const String & current_prefix)
{
    for (auto * field : fields)
    {
        if (field->checkField(config, current_prefix))
            field->loadField(config);
    }

    loadFromPocoConfigImpl(config, current_prefix);
}

void IConfiguration::reloadFromPocoConfig(const PocoAbstractConfig & config)
{
    for (auto * field : fields)
        field->reloadField(config);

    for (auto * sub_config : sub_configs)
        sub_config->reloadFromPocoConfig(config);
}

}
