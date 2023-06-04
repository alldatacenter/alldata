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

#ifndef DATAPROXY_SDK_BASE_LOGGER_H_
#define DATAPROXY_SDK_BASE_LOGGER_H_

#include <stdint.h>
#include <string.h>
#include <string>
#include <unistd.h>
#include <sys/types.h>
#include <vector>

#include "sdk_constant.h"
#include "sdk_core.h"

namespace dataproxy_sdk
{
    #ifndef  SDK_CFG_LOG_LEVEL
    #define  SDK_CFG_LOG_LEVEL   g_config.log_level_
    #endif

    #ifndef SDK_CFG_LOG_NUM
    #define SDK_CFG_LOG_NUM      g_config.log_num_
    #endif

    #ifndef SDK_CFG_LOG_SIZE
    #define SDK_CFG_LOG_SIZE     g_config.log_size_
    #endif

    #ifndef SDK_CFG_LOG_PATH
    #define SDK_CFG_LOG_PATH     g_config.log_path_.c_str()
    #endif

    #define NAME_LEN            512

    // only show fileName
    #define __FILENAME__ (strrchr(__FILE__, '/') ? strrchr(__FILE__, '/') + 1 : __FILE__)

    enum _LOG_LEVEL_
    {
        _LOG_ERROR = 0x00,
        _LOG_WARN,
        _LOG_INFO,
        _LOG_DEBUG,
        _LOG_TRACE,
        _LOG_STAT,
        _LOG_MAX_FILE
    };

    enum _LOG_TYPE_
    {
        _LOG_PTY = 0x01,
        _LOG_FILE = 0x02,
    };

    enum _LOG_SHIFT_TYPE_
    {
        _LOG_SHIFT_SIZE     = 0,    /*shift by size*/
        _LOG_SHIFT_COUNT    = 1,    /*shift by log count*/
    };

    /* CFG debug info */
    enum SDK_LOG_DEBUG_EN {
        SDK_LOG_CHANGE_LEVEL,
    };

    typedef struct tag_debug_log_cfg
    {
        int32_t   level;
        int32_t   num;
        int32_t   size;
        int32_t   shift_type;
        int32_t   file_type;
        char    path[NAME_LEN];
    } debug_log_cfg;

    typedef struct tag_debug_log_file {
        char    file_list[_LOG_MAX_FILE][NAME_LEN];
        int32_t   shift_type;
        int32_t   max_lognum;
        int32_t   max_size;
        int32_t   max_count;
        int32_t   log_count;
    } debug_log_file;

    typedef struct tag_debug_log_run {
        pid_t   log_pid;
        uint8_t   log_type;
        uint8_t   log_level;
        int32_t   log_change_min;
    } debug_log_run;

    #define _log_pid_         gst_log_run.log_pid
    #define _log_type_        gst_log_run.log_type
    #define _log_level_       gst_log_run.log_level


    #define LOG_STAT(fmt, args...)                                          \
    do{                                                                     \
        char    log_time[50] = {0};                                         \
        if (_log_type_ & _LOG_FILE)                                         \
        {                                                                   \
            log_print(_LOG_STAT, 1, (char *)"PID[%d]STAT:%s:%.3d<%s>: "fmt,\
                    _log_pid_, __FILENAME__, __LINE__, __FUNCTION__, ##args);   \
        }                                                                   \
        if (_log_type_ & _LOG_PTY)                                          \
        {                                                                   \
            debug_get_date(log_time);                                       \
            fprintf(stderr, (char *)"[%s]PID[%d]STAT:%s:%.3d<%s>: "fmt"\n",\
                    log_time, _log_pid_, __FILENAME__, __LINE__,                \
                    __FUNCTION__, ##args);                                  \
        }                                                                   \
    }while(0)

    #define LOG_ERROR(fmt, args...)                                          \
    do{                                                                     \
        char    log_time[50] = {0};                                         \
        if (_log_type_ & _LOG_FILE)                                         \
        {                                                                   \
            log_print(_LOG_ERROR, 1, (char *)"PID[%d]ERR:%s:%.3d<%s>: "fmt,\
                    _log_pid_, __FILENAME__, __LINE__, __FUNCTION__, ##args);   \
        }                                                                   \
        if (_log_type_ & _LOG_PTY)                                          \
        {                                                                   \
            debug_get_date(log_time);                                       \
            fprintf(stderr, (char *)"[%s]PID[%d]ERR:%s:%.3d<%s>: "fmt"\n", \
                    log_time, _log_pid_, __FILENAME__, __LINE__,                \
                    __FUNCTION__, ##args);                                  \
        }                                                                   \
    }while(0)

    #define LOG_WARN(fmt, args...)                                          \
    do{                                                                     \
        char    log_time[50] = {0};                                         \
        if (_log_level_ < _LOG_WARN)                                        \
            {break;}                                                        \
        if (_log_type_ & _LOG_FILE)                                         \
        {                                                                   \
            log_print(_LOG_WARN, 1, (char *)"PID[%d]WARN:%s:%.3d<%s>: "fmt,\
                    _log_pid_, __FILENAME__, __LINE__, __FUNCTION__, ##args);   \
        }                                                                   \
        if (_log_type_ & _LOG_PTY)                                          \
        {                                                                   \
            debug_get_date(log_time);                                       \
            fprintf(stderr, (char *)"[%s]PID[%d]WARN:%s:%.3d<%s>: "fmt"\n",\
                    log_time, _log_pid_, __FILENAME__, __LINE__,                \
                    __FUNCTION__, ##args);                                  \
        }                                                                   \
    }while(0)

    #define LOG_INFO(fmt, args...)                                          \
    do{                                                                     \
        char    log_time[50] = {0};                                         \
        if (_log_level_ < _LOG_INFO)                                        \
            {break;}                                                        \
        if (_log_type_ & _LOG_FILE)                                         \
        {                                                                   \
            log_print(_LOG_INFO, 1, (char *)"PID[%d]INFO:%s:%.3d<%s>: "fmt,\
                    _log_pid_, __FILENAME__, __LINE__, __FUNCTION__, ##args);   \
        }                                                                   \
        if (_log_type_ & _LOG_PTY)                                          \
        {                                                                   \
            debug_get_date(log_time);                                       \
            fprintf(stderr, (char *)"[%s]PID[%d]INFO:%s:%.3d<%s>: "fmt"\n",\
                    log_time, _log_pid_, __FILENAME__, __LINE__,                \
                    __FUNCTION__, ##args);                                  \
        }                                                                   \
    }while(0)

    #define LOG_DEBUG(fmt, args...)                                         \
    do{                                                                     \
        char    log_time[50] = {0};                                         \
        if (_log_level_ < _LOG_DEBUG)                                       \
            {break;}                                                        \
        if (_log_type_ & _LOG_FILE)                                         \
        {                                                                   \
            log_print(_LOG_DEBUG, 1, (char *)"PID[%d]DEBUG:%s:%.3d<%s>: "fmt,\
                    _log_pid_, __FILENAME__, __LINE__, __FUNCTION__, ##args);   \
        }                                                                   \
        if (_log_type_ & _LOG_PTY)                                          \
        {                                                                   \
            debug_get_date(log_time);                                       \
            fprintf(stderr, (char *)"[%s]PID[%d]DEBUG:%s:%.3d<%s>: "fmt"\n",\
                    log_time, _log_pid_, __FILENAME__, __LINE__,                \
                    __FUNCTION__, ##args);                                  \
        }                                                                   \
    }while(0)

    #define LOG_TRACE(fmt, args...)                                         \
    do{                                                                     \
        char    log_time[50] = {0};                                         \
        if (_log_level_ < _LOG_TRACE)                                       \
            {break;}                                                        \
        if (_log_type_ & _LOG_FILE)                                         \
        {                                                                   \
            log_print(_LOG_TRACE, 1, (char *)"PID[%d]TRACE:%s:%.3d<%s>: "fmt,   \
                    _log_pid_, __FILENAME__, __LINE__, __FUNCTION__, ##args);   \
        }                                                                   \
        if (_log_type_ & _LOG_PTY)                                          \
        {                                                                   \
            debug_get_date(log_time);                                       \
            fprintf(stderr, (char *)"[%s]PID[%d]TRACE:%s:%.3d<%s>: "fmt"\n",\
                    log_time, _log_pid_, __FILENAME__, __LINE__,                \
                    __FUNCTION__, ##args);                                  \
        }                                                                   \
    }while(0)

    #ifdef  __cplusplus
    extern "C" {
    #endif

    extern debug_log_run gst_log_run;
    extern debug_log_cfg gst_log_cfg;
    extern debug_log_file gst_log_file;
    extern void debug_get_date(char *logTime);

    int32_t log_print(int32_t log_level, int32_t log_time, char *format, ...);
    int32_t debug_init_log();
    void tc_log_level_restore(void);

    #ifdef  __cplusplus
    }
    #endif

}  // namespace dataproxy_sdk

#endif  // CPAI_BASE_LOGGER_H_