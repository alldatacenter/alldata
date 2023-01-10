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
#include <memory>
#include <mutex>
#include <string>
#include <type_traits>
#include <butil/containers/doubly_buffered_data.h>
#include <Common/Brpc/NamedConfigHolder.h>
#include <common/logger_useful.h>
#include <iostream>
#include <Poco/DateTime.h>
#include <Poco/DateTimeFormatter.h>

namespace DB
{
template <typename TConfig>
struct SetConfigFn
{
    TConfig * new_config;
    TConfig * old_config;

    bool operator()(TConfig *& ptr)
    {
        old_config = ptr;
        ptr = new_config;
        return true;
    }
};

/// Querying config from QueryableConfigHolder almost lock-free, at cost that
/// reloading config is much slower. It's very suitable for config which have
/// a lot of concurrent read-only ops from many threads and occasional modifications of data.
/// As a side effect, QueryableConfigHolder store a thread-local data.

/// held by BrpcApplication(singleton), use std::cout for log
template <typename TDerived, typename TConfig, typename TDelete = std::default_delete<TConfig>>
class QueryableConfigHolder : public NamedConfigHolder<TDerived, TConfig, TDelete>
{
public:
    explicit QueryableConfigHolder() {}

    void init(RawConfAutoPtr raw_conf_ptr) override;

    void reload(RawConfAutoPtr raw_conf_ptr) override;

    TConfig queryConfig();

    ~QueryableConfigHolder() override;

private:
    butil::DoublyBufferedData<TConfig *> doubly_buffered_config;
    mutable std::mutex conf_mutex;
    TConfig * modifyInternalConfig(TConfig * new_conf);
    using ConfigScopedPtr = typename butil::DoublyBufferedData<TConfig *>::ScopedPtr;
};


template <typename TDerived, typename TConfig, typename TDelete>
void QueryableConfigHolder<TDerived, TConfig, TDelete>::init(RawConfAutoPtr raw_conf_ptr)
{
    std::unique_ptr<TConfig, TDelete> new_conf_ptr = std::move(this->createTypedConfig(raw_conf_ptr));
    TConfig * old_conf_ptr = this->modifyInternalConfig(new_conf_ptr.get());
    // new_conf is owned by doubly_buffered_data now
    TConfig * new_conf = new_conf_ptr.release();

    if (unlikely(old_conf_ptr != nullptr))
    {
        TDelete()(old_conf_ptr);
        std::cout << Poco::DateTimeFormatter::format(Poco::DateTime(), "%Y.%m.%d %H:%M:%S.%i") << " <Error> "
                  << "QueryableConfigHolder::init " << TDerived::name + " config has value before init, name";
    }

    this->afterInit(new_conf);
}

template <typename TDerived, typename TConfig, typename TDelete>
void QueryableConfigHolder<TDerived, TConfig, TDelete>::reload(RawConfAutoPtr raw_conf_ptr)
{
    const TConfig * old_conf_ptr;

    // Since read is much faster than modify, reading current config first to avoid unnecessary modify.
    {
        ConfigScopedPtr s;
        if (unlikely(doubly_buffered_config.Read(&s) != 0))
            throw Poco::IllegalStateException("Fail to read config!");
        old_conf_ptr = (*s);
    }
    auto new_conf_ptr = std::move(this->createTypedConfig(raw_conf_ptr));

    if (!this->hasChanged(old_conf_ptr, new_conf_ptr.get()))
        return;

    std::lock_guard<std::mutex> guard(conf_mutex);
    /// Config changed, we should update doubly_buffered_data and release old config data
    TConfig * old_conf_to_del = this->modifyInternalConfig(new_conf_ptr.get());
    // new_conf is owned by doubly_buffered_data now
    const auto readonly_new_conf = new_conf_ptr.release();
    std::unique_ptr<TConfig, TDelete> old_ptr_deleter;
    if(likely(old_conf_to_del))
        old_ptr_deleter.reset(old_conf_to_del);

    this->onChange(old_conf_to_del, readonly_new_conf);
    if (this->reload_callback)
        this->reload_callback(old_conf_to_del, readonly_new_conf);
}


template <typename TDerived, typename TConfig, typename TDelete>
QueryableConfigHolder<TDerived, TConfig, TDelete>::~QueryableConfigHolder()
{
}


template <typename TDerived, typename TConfig, typename TDelete>
TConfig QueryableConfigHolder<TDerived, TConfig, TDelete>::queryConfig()
{
    ConfigScopedPtr s;
    if (unlikely(doubly_buffered_config.Read(&s) != 0))
        throw Poco::IllegalStateException("Fail to read config!");

    TConfig * config_ptr = (*s);
    if (unlikely(config_ptr == nullptr))
        throw Poco::IllegalStateException("Config is NULL");

    return *config_ptr;
}

template <typename TDerived, typename TConfig, typename TDelete>
TConfig * QueryableConfigHolder<TDerived, TConfig, TDelete>::modifyInternalConfig(TConfig * new_conf)
{
    SetConfigFn<TConfig> fn = {new_conf, nullptr};
    auto ret = doubly_buffered_config.Modify(fn);
    if (unlikely(!ret))
        throw Poco::IllegalStateException("Fail to modify config, result: " + std::to_string(ret));
    return fn.old_config;
}
}
