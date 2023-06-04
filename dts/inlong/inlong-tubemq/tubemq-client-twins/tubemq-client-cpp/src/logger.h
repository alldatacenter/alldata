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

#ifndef _TUBEMQ_LOG_FILE_
#define _TUBEMQ_LOG_FILE_

#include <stdint.h>

#include <string>
#include <vector>

namespace tubemq {
class Logger;

Logger& GetLogger();

#define LOG_LEVEL(level, fmt, ...)                                                   \
  {                                                                                  \
    if (tubemq::GetLogger().IsEnable(level)) {                                       \
      tubemq::GetLogger().Write("[%s:%d][%s]" fmt, __func__, __LINE__,               \
                                tubemq::Logger::Level2String(level), ##__VA_ARGS__); \
    }                                                                                \
  }

#define LOG_TRACE(fmt, ...) \
  LOG_TUBEMQ(tubemq::GetLogger(), tubemq::Logger::kTrace, fmt, ##__VA_ARGS__)
#define LOG_DEBUG(fmt, ...) \
  LOG_TUBEMQ(tubemq::GetLogger(), tubemq::Logger::kDebug, fmt, ##__VA_ARGS__)
#define LOG_INFO(fmt, ...) \
  LOG_TUBEMQ(tubemq::GetLogger(), tubemq::Logger::kInfo, fmt, ##__VA_ARGS__)
#define LOG_WARN(fmt, ...) \
  LOG_TUBEMQ(tubemq::GetLogger(), tubemq::Logger::kWarn, fmt, ##__VA_ARGS__)
#define LOG_ERROR(fmt, ...) \
  LOG_TUBEMQ(tubemq::GetLogger(), tubemq::Logger::kError, fmt, ##__VA_ARGS__)

#define LOG_TUBEMQ(logger, level, fmt, ...)                                                    \
  {                                                                                            \
    if (logger.IsEnable(level)) {                                                              \
      logger.Write("[%s:%d][%s]" fmt, __func__, __LINE__, tubemq::Logger::Level2String(level), \
                   ##__VA_ARGS__);                                                             \
    }                                                                                          \
  }

class Logger {
 public:
  enum Level {
    kTrace = 0,
    kDebug = 1,
    kInfo = 2,
    kWarn = 3,
    kError = 4,
  };

  // size: MB
  Logger()
      : file_max_size_(100),
        file_num_(10),
        level_(kError),
        base_path_("tubemq"),
        instance_("TubeMQ") {}

  ~Logger(void) {}

  // path example: ../log/tubemq
  // size: MB
  bool Init(const std::string& path, Level level, uint32_t file_max_size = 100,
            uint32_t file_num = 10);

  bool Write(const char* sFormat, ...) __attribute__((format(printf, 2, 3)));
  inline bool WriteStream(const std::string& msg) { return writeStream(msg.c_str()); }

  inline void SetInstance(const std::string& instance) { instance_ = instance; }
  inline bool IsEnable(Level level) {
    if (level_ <= level) {
      return true;
    } else {
      return false;
    }
  }

  static const char* Level2String(Level level) {
    static const char* level_names[] = {
        "TRACE", "DEBUG", "INFO", "WARN", "ERROR",
    };
    return level_names[level];
  }

 private:
  void setup();
  bool writeStream(const char* msg);

 private:
  uint32_t file_max_size_;
  uint16_t file_num_;
  uint8_t level_;

  std::string base_path_;
  std::string instance_;
  std::string err_msg_;
};
}  // namespace tubemq
#endif  // _TUBEMQ_LOG_FILE_
