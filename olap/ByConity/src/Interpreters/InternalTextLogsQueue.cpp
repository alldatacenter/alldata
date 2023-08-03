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

#include "InternalTextLogsQueue.h"
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeEnum.h>
#include <DataTypes/DataTypesNumber.h>
#include <common/logger_useful.h>

#include <Poco/Message.h>


namespace DB
{

InternalTextLogsQueue::InternalTextLogsQueue()
        : ConcurrentBoundedQueue<MutableColumns>(std::numeric_limits<int>::max()),
          max_priority(Poco::Message::Priority::PRIO_INFORMATION) {}


Block InternalTextLogsQueue::getSampleBlock()
{
    return Block {
        {std::make_shared<DataTypeDateTime>(), "event_time"},
        {std::make_shared<DataTypeUInt32>(),   "event_time_microseconds"},
        {std::make_shared<DataTypeString>(),   "host_name"},
        {std::make_shared<DataTypeString>(),   "query_id"},
        {std::make_shared<DataTypeUInt64>(),   "thread_id"},
        {std::make_shared<DataTypeInt8>(),     "priority"},
        {std::make_shared<DataTypeString>(),   "source"},
        {std::make_shared<DataTypeString>(),   "text"}
    };
}

MutableColumns InternalTextLogsQueue::getSampleColumns()
{
    static Block sample_block = getSampleBlock();
    return sample_block.cloneEmptyColumns();
}

void InternalTextLogsQueue::pushBlock(Block && log_block)
{
    static Block sample_block = getSampleBlock();

    if (blocksHaveEqualStructure(sample_block, log_block))
        (void)(emplace(log_block.mutateColumns()));
    else
        LOG_WARNING(&Poco::Logger::get("InternalTextLogsQueue"), "Log block have different structure");
}

const char * InternalTextLogsQueue::getPriorityName(int priority)
{
    /// See Poco::Message::Priority

    static constexpr const char * const PRIORITIES[] =
    {
        "Unknown",
        "Fatal",
        "Critical",
        "Error",
        "Warning",
        "Notice",
        "Information",
        "Debug",
        "Trace"
    };

    return (priority >= 1 && priority <= 8) ? PRIORITIES[priority] : PRIORITIES[0];
}

}
