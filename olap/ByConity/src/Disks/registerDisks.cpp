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

#include "registerDisks.h"

#include "DiskFactory.h"

#if !defined(ARCADIA_BUILD)
#    include <Common/config.h>
#endif

namespace DB
{

void registerDiskLocal(DiskFactory & factory);
void registerDiskMemory(DiskFactory & factory);

#if USE_AWS_S3
void registerDiskS3(DiskFactory & factory);
void registerDiskByteS3(DiskFactory& factory);
#endif

#if USE_HDFS
void registerDiskHDFS(DiskFactory & factory);
void registerDiskByteHDFS(DiskFactory & factory);
#endif


void registerDisks()
{
    auto & factory = DiskFactory::instance();

    registerDiskLocal(factory);
    registerDiskMemory(factory);

#if USE_AWS_S3
    registerDiskS3(factory);
    registerDiskByteS3(factory);
#endif

#if USE_HDFS
    registerDiskHDFS(factory);
    registerDiskByteHDFS(factory);
#endif
}

}
