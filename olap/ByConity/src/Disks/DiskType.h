/*
 * Copyright 2016-2023 ClickHouse, Inc.
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


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <common/types.h>

namespace DB
{

struct DiskType
{
    // When add new type in Type, Please Update.
    static constexpr auto TYPE_NUM_IN_DISK_TYPE = 5;

    enum class Type
    {
        Local = 0,
        RAM = 1,
        S3 = 2,
        HDFS = 3,
        ByteHDFS = 4,
        ByteS3 = 5
    };
    static String toString(Type disk_type)
    {
        switch (disk_type)
        {
            case Type::Local:
                return "local";
            case Type::RAM:
                return "memory";
            case Type::S3:
                return "s3";
            case Type::HDFS:
                return "hdfs";
            case Type::ByteHDFS:
                return "bytehdfs";
            case Type::ByteS3:
                return "bytes3";
        }
        __builtin_unreachable();
    }
    
    static int toInt(Type disk_type);

    static Type toType(int disk_type_id);
};

}


