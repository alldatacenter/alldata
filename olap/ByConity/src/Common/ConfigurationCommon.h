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

#include <mutex>
#include <vector>
#include <type_traits>
#include <Core/Types.h>
#include <Poco/Util/AbstractConfiguration.h>
#include <boost/noncopyable.hpp>

using PocoAbstractConfig = Poco::Util::AbstractConfiguration;

namespace DB
{
namespace ConfigFlag
{
    constexpr auto Default = 0x00;
    constexpr auto Recommended = 0x01;
    constexpr auto Required = 0x02;
    constexpr auto Deprecated = 0x4;
}

struct ConfigurationFieldBase : private boost::noncopyable
{
    String name;
    String init_key;
    String full_key; /// assigned in checkField()
    String type;
    String description;

    UInt64 flags{0};
    bool existed{false}; /// modified in checkField()
    bool immutable{true}; // init in subclass ctor

    bool recommended() const { return flags & ConfigFlag::Recommended; }
    bool required() const { return flags & ConfigFlag::Required; }
    bool deprecated() const { return flags & ConfigFlag::Deprecated; }

    virtual ~ConfigurationFieldBase() = default;

    virtual String toString() const = 0; /// safe

    bool checkField(const PocoAbstractConfig & config, const String & current_prefix); /// unsafe
    virtual void loadField(const PocoAbstractConfig & config) = 0; /// unsafe
    virtual void reloadField(const PocoAbstractConfig &) { } /// safe
};

template <class T>
struct ConfigurationFieldNumber : public ConfigurationFieldBase
{
    explicit ConfigurationFieldNumber(T v) : value(v) { immutable = true; }

    String toString() const override { return std::to_string(value); }

    void loadField(const PocoAbstractConfig & config) override
    {
        if constexpr (std::is_same_v<T, bool>)
            value = config.getBool(full_key, value);
        if constexpr (std::is_floating_point_v<T>)
            value = config.getDouble(full_key, value);
        else if constexpr (std::is_signed_v<T>)
            value = config.getInt64(full_key, value);
        else
            value = config.getUInt64(full_key, value);
    }

    operator T() const { return value; }

    T value;
};

using ConfigurationFieldBool = ConfigurationFieldNumber<bool>;
using ConfigurationFieldInt64 = ConfigurationFieldNumber<Int64>;
using ConfigurationFieldUInt64 = ConfigurationFieldNumber<UInt64>;
using ConfigurationFieldFloat32 = ConfigurationFieldNumber<Float32>;
using ConfigurationFieldFloat64 = ConfigurationFieldNumber<Float64>;

template <class T>
struct ConfigurationFieldMutableNumber : public ConfigurationFieldBase
{
    explicit ConfigurationFieldMutableNumber(T v) : value(v) { immutable = false; }

    String toString() const override { return std::to_string(safeGet()); }

    void loadField(const PocoAbstractConfig & config) override
    {
        if constexpr (std::is_same_v<T, bool>)
            value = config.getBool(full_key, value);
        else if constexpr (std::is_floating_point_v<T>)
            value = config.getDouble(full_key, value);
        else if constexpr (std::is_signed_v<T>)
            value = config.getInt64(full_key, value);
        else
            value = config.getUInt64(full_key, value);
    }

    void reloadField(const PocoAbstractConfig & config) override { loadField(config); }

    T safeGet() const { return value; }

private:
    std::atomic<T> value;
};

using ConfigurationFieldMutableBool = ConfigurationFieldMutableNumber<bool>;
using ConfigurationFieldMutableInt64 = ConfigurationFieldMutableNumber<Int64>;
using ConfigurationFieldMutableUInt64 = ConfigurationFieldMutableNumber<UInt64>;
using ConfigurationFieldMutableFloat32 = ConfigurationFieldMutableNumber<Float32>;
using ConfigurationFieldMutableFloat64 = ConfigurationFieldMutableNumber<Float64>;

struct ConfigurationFieldString : public ConfigurationFieldBase
{
    explicit ConfigurationFieldString(String v) : value(std::move(v)) { immutable = true; }

    String toString() const override { return value; }

    void loadField(const PocoAbstractConfig & config) override { value = config.getString(full_key, value); }

    operator const String &() const { return value; }

    String value;
};

struct ConfigurationFieldMutableString : public ConfigurationFieldBase
{
    explicit ConfigurationFieldMutableString(String v) : value(std::move(v)) { immutable = false; }

    String toString() const override { return safeGet(); }

    void loadField(const PocoAbstractConfig & config) override { value = config.getString(full_key, value); }

    void reloadField(const PocoAbstractConfig & config) override
    {
        std::lock_guard lock(m);
        loadField(config);
    }

    String safeGet() const
    {
        std::lock_guard lock(m);
        return value;
    }

private:
    String value;
    mutable std::mutex m;
};

struct IConfiguration : private boost::noncopyable
{
public:
    auto & getSubConfigs() const { return sub_configs; }
    auto & getFields() const { return fields; }

    virtual ~IConfiguration() = default;

    void loadFromPocoConfig(const PocoAbstractConfig & config, const String & current_prefix);

    /// Load your sub configs & custom fields here
    virtual void loadFromPocoConfigImpl(const PocoAbstractConfig &, const String &) { }

    void reloadFromPocoConfig(const PocoAbstractConfig & config);

protected:
    std::vector<IConfiguration *> sub_configs;
    std::vector<ConfigurationFieldBase *> fields;
};

#define DECLARE_CONFIG_DATA(CONFIG_DATA_NAME, CONFIG_FIELDS_LIST) \
    struct CONFIG_DATA_NAME : public IConfiguration \
    { \
        CONFIG_FIELDS_LIST(DECLARE_CONFIG_MEMBER) \
        CONFIG_DATA_NAME() { CONFIG_FIELDS_LIST(INIT_CONFIG_MEMBER) } \
    };

#define DECLARE_CONFIG_MEMBER(TYPE, NAME, KEY, DEFAULT, FLAGS, DESC) ConfigurationField##TYPE NAME{DEFAULT};
#define INIT_CONFIG_MEMBER(TYPE, NAME, KEY, DEFAULT, FLAGS, DESC) \
    NAME.name = #NAME; \
    NAME.init_key = (sizeof(KEY) == 1) ? #NAME : KEY; \
    NAME.flags = FLAGS; \
    NAME.type = #TYPE; \
    NAME.description = DESC; \
    fields.push_back(&NAME);

}
