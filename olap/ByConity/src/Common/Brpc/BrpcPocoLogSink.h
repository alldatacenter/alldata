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

#include <boost/noncopyable.hpp>
#include <butil/logging.h>
#include <butil/strings/string_piece.h>
#include <common/logger_useful.h>

namespace DB
{
/// A BRPC LogSink for adapting to POCO logging system
class BrpcPocoLogSink : public ::logging::LogSink, private boost::noncopyable
{
public:
    explicit BrpcPocoLogSink() { logger = &Poco::Logger::get("brpc"); }

    ~BrpcPocoLogSink() override = default;

    bool OnLogMessage(int severity, const char * file, int line, const butil::StringPiece & content) override;


private:
    Poco::Logger * logger;
};

static inline Poco::Message::Priority brpc2PocoLogPriority(int brpcLogPriority)
{
    switch (brpcLogPriority)
    {
        case ::logging::BLOG_INFO:
            return Poco::Message::PRIO_INFORMATION;
        case ::logging::BLOG_NOTICE:
            return Poco::Message::PRIO_NOTICE;
        case ::logging::BLOG_WARNING:
            return Poco::Message::PRIO_WARNING;
        case ::logging::BLOG_ERROR:
            return Poco::Message::PRIO_ERROR;
        case ::logging::BLOG_FATAL:
            return Poco::Message::PRIO_FATAL;
        default:
            // mapping brpc multiple verbose_levels to PRIO_TRACE
            return Poco::Message::PRIO_TRACE;
    }
}

static inline int poco2BrpcLogPriority(int pocoLogLevel)
{
    auto poco_log_priority = static_cast<Poco::Message::Priority>(pocoLogLevel);
    switch (poco_log_priority)
    {
        case Poco::Message::PRIO_INFORMATION:
            return ::logging::BLOG_INFO;
        case Poco::Message::PRIO_NOTICE:
            return ::logging::BLOG_NOTICE;
        case Poco::Message::PRIO_WARNING:
            return ::logging::BLOG_WARNING;
        case Poco::Message::PRIO_ERROR:
            return ::logging::BLOG_ERROR;
        case Poco::Message::PRIO_CRITICAL:
        case Poco::Message::PRIO_FATAL:
            return ::logging::BLOG_FATAL;
        case Poco::Message::PRIO_DEBUG:
#ifndef NDEBUG
            return ::logging::BLOG_INFO;
#else
            return ::logging::BLOG_VERBOSE;
#endif
        default:
            return ::logging::BLOG_VERBOSE;
    }
}
}
