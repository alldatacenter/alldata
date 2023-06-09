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

#include <IO/ReadBufferFromFileBase.h>
#include <IO/ReadSettings.h>
#include <string>
#include <memory>


namespace DB
{

class MMappedFileCache;


/** Create an object to read data from a file.
  * estimated_size - the number of bytes to read
  * aio_threshold - the minimum number of bytes for asynchronous reads
  *
  * If aio_threshold = 0 or estimated_size < aio_threshold, read operations are executed synchronously.
  * Otherwise, the read operations are performed asynchronously.
  */
std::unique_ptr<ReadBufferFromFileBase> createReadBufferFromFileBase(
    const std::string & filename_,
    const ReadSettings& settings_,
    int flags_ = -1,
    char * existing_memory_ = nullptr,
    size_t alignment = 0);

}
