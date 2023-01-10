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

#include <Catalog/HandlerManager.h>
#include <Catalog/StreamingHanlders.h>

namespace DB::Catalog
{

void HandlerManager::addHandler(const HandlerPtr & handler_ptr)
{
    std::lock_guard lock(mutex);
    auto it = handlers.insert(handlers.end(), handler_ptr);
    handler_ptr->handler_it = it;
}

void HandlerManager::removeHandler(const HandlerPtr & handler_ptr)
{
    std::lock_guard lock(mutex);
    handlers.erase(handler_ptr->handler_it);
}

}
