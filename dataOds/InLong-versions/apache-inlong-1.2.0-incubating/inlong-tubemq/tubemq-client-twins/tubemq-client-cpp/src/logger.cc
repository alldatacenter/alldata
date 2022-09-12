/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

#include "logger.h"

#include <log4cplus/fileappender.h>
#include <log4cplus/layout.h>
#include <log4cplus/logger.h>
#include <log4cplus/loggingmacros.h>
#include <stdarg.h>

#include <string>

#include "singleton.h"

namespace tubemq {

static const uint32_t kMBSize = 1024 * 1024;

Logger& GetLogger() { return Singleton<Logger>::Instance(); }

bool Logger::Init(const std::string& path, Logger::Level level, uint32_t file_max_size,
                  uint32_t file_num) {
  base_path_ = path;
  file_max_size_ = file_max_size;
  file_num_ = file_num;
  level_ = level;
  setup();
  return true;
}

bool Logger::Write(const char* format, ...) {
  char buf[8192];
  buf[sizeof(buf) - 1] = 0;
  va_list ap;
  va_start(ap, format);
  vsnprintf(buf, sizeof(buf) - 1, format, ap);
  va_end(ap);
  return writeStream(buf);
}

bool Logger::writeStream(const char* log) {
  auto logger = log4cplus::Logger::getInstance(instance_);
  // log4cplus::tostringstream _log4cplus_buf;
  // _log4cplus_buf << log;
  logger.forcedLog(log4cplus::TRACE_LOG_LEVEL, log);
  return true;
}

void Logger::setup() {
  bool immediate_fush = true;
  std::string pattern = "[%D{%Y-%m-%d %H:%M:%S.%q}][tid:%t]%m%n";
  auto logger_d = log4cplus::Logger::getInstance(instance_);
  logger_d.removeAllAppenders();
  logger_d.setLogLevel(log4cplus::TRACE_LOG_LEVEL);
  log4cplus::helpers::SharedObjectPtr<log4cplus::Appender> append_d(
      new log4cplus::RollingFileAppender(base_path_ + ".log", file_max_size_ * kMBSize, file_num_,
                                         immediate_fush));
  std::unique_ptr<log4cplus::Layout> layout_d(new log4cplus::PatternLayout(pattern));
  append_d->setLayout(std::move(layout_d));
  logger_d.addAppender(append_d);
}

}  // namespace tubemq
