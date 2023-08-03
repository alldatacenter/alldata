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

#include <Storages/MergeTree/S3ObjectMetadata.h>
#include <fmt/core.h>
#include <Common/Exception.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
    extern const int LOGICAL_ERROR;
}

namespace S3ObjectMetadata
{

const String PART_GENERATOR_ID = "pg-id";

PartGeneratorID::PartGeneratorID(Type type_, const String& id_):
    type(type_), id(id_)
{
    if (id_.size() >= 128)
    {
        throw Exception(fmt::format("Task id too long, must shorter than 128 character, got {}",
            id_.size()), ErrorCodes::BAD_ARGUMENTS);
    }
}

PartGeneratorID::PartGeneratorID(const String& meta_)
{
    std::tie(type, id) = parse(meta_);
}

std::pair<PartGeneratorID::Type, String> PartGeneratorID::parse(const String& meta_)
{
    if (meta_.size() >= 2)
    {
        if (meta_[1] == '#')
        {
            Type type;
            switch(meta_[0])
            {
                case 'P': type = PART_WRITER; break;
                case 'T': type = TRANSACTION; break;
                case 'D': type = DUMPER; break;
            }

            return std::pair<Type, String>(type, meta_.substr(2));
        }
    }
    throw Exception(fmt::format("Invalid meta value {}", meta_),
        ErrorCodes::BAD_ARGUMENTS);
}

bool PartGeneratorID::verify(const String& meta_) const
{
    PartGeneratorID parsed_meta(meta_);
    return parsed_meta.type == type && parsed_meta.id == id;
}

String PartGeneratorID::str() const
{
    static String lookup_table = "PTD";
    int type_idx = static_cast<int>(type);
    if (type_idx > 2)
    {
        throw Exception("Invalid type value " + std::to_string(type_idx),
            ErrorCodes::LOGICAL_ERROR);
    }
    return fmt::format("{}#{}", lookup_table[type_idx], id);
}

}

}
