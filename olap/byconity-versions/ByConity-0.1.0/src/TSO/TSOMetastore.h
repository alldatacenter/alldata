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
#include <Core/Types.h>

namespace DB
{

namespace TSO
{

class TSOMetastore
{
public:
    TSOMetastore(const String & key_name_)
        : key_name(key_name_) {}
    virtual void put(const String & value) = 0;
    virtual void get(String & value) = 0;
    virtual void clean() = 0;
    virtual ~TSOMetastore() {}

    String key_name;
};

using TSOMetastorePtr = std::shared_ptr<TSOMetastore>;

}

}
