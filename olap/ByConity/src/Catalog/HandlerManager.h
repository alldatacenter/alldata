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
#include <brpc/stream.h>
#include <mutex>
#include <list>

namespace DB::Catalog
{

class StreamingHandlerBase;

class HandlerManager
{
public:
    using HandlerPtr = std::shared_ptr<StreamingHandlerBase>;
    using HandlerList = std::list<HandlerPtr>;

    void addHandler(const HandlerPtr & handler_ptr_);

    void removeHandler(const HandlerPtr & handler_ptr_);

private:
    HandlerList handlers;
    std::mutex mutex;
};

}
