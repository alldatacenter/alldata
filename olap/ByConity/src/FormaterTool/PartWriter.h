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
#include <FormaterTool/PartToolkitBase.h>
#include <Poco/Logger.h>

namespace DB
{

class PartWriter : public PartToolkitBase
{
public:
    PartWriter(const ASTPtr & query_ptr_, ContextMutablePtr context_);
    ~PartWriter() override;
    void execute() override;

private:
    Poco::Logger * log = &Poco::Logger::get("PartWriter");
    String source_path;
    String data_format;
    String dest_path;
    String working_path;
};

}
