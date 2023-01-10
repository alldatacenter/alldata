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
#include <type_traits>
#include <Common/Brpc/NamedConfigHolder.h>

namespace DB
{
/// Config held by SealedConfigHolder is not queryable, it is only available in callback context
template <typename TDerived, typename TConfig, typename TDelete = std::default_delete<TConfig>>
class SealedConfigHolder : public NamedConfigHolder<TDerived, TConfig, TDelete>
{
public:
    explicit SealedConfigHolder() = default;

    void init(RawConfAutoPtr raw_conf_ptr) override;

    void reload(RawConfAutoPtr raw_conf_ptr) override;

    ~SealedConfigHolder() override;

private:
    std::unique_ptr<TConfig, TDelete> current_conf_ptr;
    mutable std::mutex conf_mutex;
    inline std::unique_ptr<TConfig, TDelete> buildTypedConfig(RawConfAutoPtr raw_conf_ptr);
};


template <typename TDerived, typename TConfig, typename TDelete>
void SealedConfigHolder<TDerived, TConfig, TDelete>::init(RawConfAutoPtr raw_conf_ptr)
{
    auto new_conf_ptr = buildTypedConfig(raw_conf_ptr);
    std::lock_guard<std::mutex> guard(conf_mutex);
    current_conf_ptr = std::move(new_conf_ptr);
    this->afterInit(current_conf_ptr.get());
}

template <typename TDerived, typename TConfig, typename TDelete>
void SealedConfigHolder<TDerived, TConfig, TDelete>::reload(RawConfAutoPtr raw_conf_ptr)
{
    auto new_conf_ptr = buildTypedConfig(raw_conf_ptr);

    if (!this->hasChanged(current_conf_ptr.get(), new_conf_ptr.get()))
        return;

    // Config has changed, update current_conf_ptr
    std::lock_guard<std::mutex> guard(conf_mutex);
    std::unique_ptr<TConfig, TDelete> old_conf_ptr_deleter;
    auto old_ptr = current_conf_ptr.release();
    if(old_ptr){
        old_conf_ptr_deleter.reset(old_ptr);
    }
    current_conf_ptr = std::move(new_conf_ptr);

    this->onChange(old_conf_ptr_deleter.get(), current_conf_ptr.get());
    if (this->reload_callback)
        this->reload_callback(old_conf_ptr_deleter.get(), current_conf_ptr.get());
}

template <typename TDerived, typename TConfig, typename TDelete>
SealedConfigHolder<TDerived, TConfig, TDelete>::~SealedConfigHolder()
{
    std::lock_guard<std::mutex> guard(conf_mutex);
    if (this->current_conf_ptr)
        this->current_conf_ptr.reset();
}
template <typename TDerived, typename TConfig, typename TDelete>
std::unique_ptr<TConfig, TDelete> SealedConfigHolder<TDerived, TConfig, TDelete>::buildTypedConfig(RawConfAutoPtr raw_conf_ptr)
{
    std::unique_ptr<TConfig, TDelete> new_conf_ptr;
    // if TConfig is RawConfig, we can skip createTypedConfig
    if constexpr (std::is_same_v<TConfig, RawConfig>)
    {
        raw_conf_ptr->duplicate();
        return std::move(std::unique_ptr<TConfig, TDelete>(raw_conf_ptr.get()));
    }
    else
        return std::move(this->createTypedConfig(raw_conf_ptr));
}

}
