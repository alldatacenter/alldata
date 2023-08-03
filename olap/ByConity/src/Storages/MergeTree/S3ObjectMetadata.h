/*
 * Copyright 2023 Bytedance Ltd. and/or its affiliates.
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

#pragma once

#include <map>
#include <Core/Types.h>

namespace DB::S3ObjectMetadata
{

/// Mark generator of one part, since we only generate part's part at local
/// there maybe some part already have same part id in s3, so when write part
/// fail, it should have pg-id to mark the generator, when transaction rollback
/// or part writer clean up, it will use corresponding pg-id to determine if this
/// part is generate by this part_writer execution or transaction
extern const String PART_GENERATOR_ID;

/// Value of PART_GENERATOR_ID
class PartGeneratorID
{
public:
    enum Type
    {
        PART_WRITER = 0,
        TRANSACTION = 1,
        DUMPER = 2,
    };

    PartGeneratorID(Type type_, const String& id_);
    PartGeneratorID(const String& meta_);

    bool verify(const String& meta_) const;

    String str() const;

    static std::pair<Type, String> parse(const String& meta_);

    Type type;
    String id;
};

}
