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

#include <sys/time.h>
#include <time.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <string.h>
#include <pthread.h>
#include <stdarg.h>
#include <stddef.h>
#include <unistd.h>
#include <errno.h>

namespace dataproxy_sdk
{
    // log config
    debug_log_cfg gst_log_cfg;

    // log info
    debug_log_file gst_log_file;

    // log run info
    debug_log_run gst_log_run = {
        //.log_type  =
        _LOG_PTY,
        //.log_change_min =
        10,
    };

    pthread_mutex_t log_mutex = PTHREAD_MUTEX_INITIALIZER;

    
    void debug_show_logcfg()
    {
        debug_log_cfg *log_cfg = &gst_log_cfg;

        LOG_ERROR("debug log config info. level(%d).", log_cfg->level);

        return;
    }

    void debug_get_date(char *logTime)
    {
        time_t mytime = time(NULL);
        struct tm curr;
        localtime_r(&mytime, &curr);

        if (curr.tm_year > 50)
            sprintf(logTime, "%04d-%02d-%02d %02d:%02d:%02d",
                    curr.tm_year + 1900, curr.tm_mon + 1, curr.tm_mday,
                    curr.tm_hour, curr.tm_min, curr.tm_sec);
        else
            sprintf(logTime, "%04d-%02d-%02d %02d:%02d:%02d",
                    curr.tm_year + 2000, curr.tm_mon + 1, curr.tm_mday,
                    curr.tm_hour, curr.tm_min, curr.tm_sec);

        return;
    }

    // update log file
    int32_t debug_shift_file(int32_t loglevel)
    {
        int32_t i = 0;
        int32_t ret = 0;
        char file_name[NAME_LEN];
        char new_name[NAME_LEN];
        struct stat file_stat;
        debug_log_file *log_file = &gst_log_file;

        memset(&file_name[0], 0x0, NAME_LEN);
        snprintf(&file_name[0], NAME_LEN - 1, "%s", &log_file->file_list[loglevel][0]);

        // get log file stat
        ret = stat(file_name, &file_stat);
        if (ret < 0)
        {
            printf("get log file:%s stat err.", file_name);
            return 1;
        }

        switch (log_file->shift_type)
        {
        case _LOG_SHIFT_SIZE:
            if (file_stat.st_size < log_file->max_size)
            {
                return 0;
            }
            break;

        case _LOG_SHIFT_COUNT:
        default:
            if (log_file->log_count < log_file->max_count)
            {
                return 0;
            }
            log_file->log_count = 0;
            break;
        }

        // create new log file
        for (i = log_file->max_lognum; i >= 0; i--)
        {
            if (i == 0)
            {
                snprintf(&file_name[0], NAME_LEN - 1, "%s",
                        &log_file->file_list[loglevel][0]);
            }
            else
            {
                snprintf(&file_name[0], NAME_LEN - 1, "%s.%d",
                        &log_file->file_list[loglevel][0], i);
            }

            if (0 == access(file_name, F_OK))
            {
                snprintf(&new_name[0], NAME_LEN - 1, "%s.%d",
                        &log_file->file_list[loglevel][0], i + 1);
                if (rename(file_name, new_name) < 0)
                {
                    printf("log file rename err(%d).", errno);
                    return 1;
                }
            }
        }

        return 0;
    }

    // init log file
    int32_t debug_init_logfile(debug_log_file *log_file, debug_log_cfg *log_cfg)
    {
        const char *log_pre[_LOG_MAX_FILE] = {"error", "warn", "info", "debug", "trace", "stat"};

        memset(log_file, 0x0, sizeof(debug_log_file));

        for (int i = 0; i < _LOG_MAX_FILE; i++)
        {
            snprintf(&(log_file->file_list[i][0]), NAME_LEN - 1, "%ssdk_cpp_%s.log",
                    &log_cfg->path[0], log_pre[i]);
            //printf("log will write to file:%s.\n", &log_file->file_list[i][0]);
        }

        log_file->shift_type = log_cfg->shift_type;
        log_file->max_lognum = log_cfg->num;
        log_file->max_size = log_cfg->size * 1024 * 1024;
        log_file->max_count = log_cfg->size * 1024 * 1024;
        log_file->log_count = log_cfg->size * 1024 * 1024;

        return 0;
    }

    static time_t last_write_log_time[_LOG_MAX_FILE] = {0};
    static int log_limit_cnt[_LOG_MAX_FILE] = {0};

    int32_t log_print(int32_t loglevel, int32_t log_time, char *format, ...)
    {
        va_list ap;
        char date[50];
        FILE *file_id;
        struct timeval tm;
        debug_log_file *log_file = &gst_log_file;

        if ((loglevel > (int32_t)SDK_CFG_LOG_LEVEL) && (loglevel != _LOG_STAT))
        {
            return 0;
        }

    #if 1
        time_t now = time(NULL);
        if (loglevel != _LOG_STAT && now - last_write_log_time[loglevel] < 10)
        {
            if (log_limit_cnt[loglevel] > 200)
            {
                return 0;
            }

            log_limit_cnt[loglevel]++;
        }
        else
        {
            last_write_log_time[loglevel] = now;
            log_limit_cnt[loglevel] = 0;
        }
    #endif

        debug_get_date(date);
        pthread_mutex_lock(&log_mutex);
        // open log file
        file_id = fopen(&(log_file->file_list[loglevel][0]), "a+");
        if (NULL == file_id)
        {
            printf("open file:%s err, errno:%d.\n", &(log_file->file_list[loglevel][0]), errno);
            pthread_mutex_unlock(&log_mutex);
            return 1;
        }

        va_start(ap, format);
        if (1 == log_time)
        {
            fprintf(file_id, "[%s]", date);
        }
        else
        {
            gettimeofday(&tm, NULL);
            fprintf(file_id, "[%s.%.6d]", date,
                    (int32_t)tm.tv_usec);
        }

        vfprintf(file_id, format, ap);
        fprintf(file_id, "\n");
        va_end(ap);
        log_file->log_count++;
        fflush(file_id);
        fclose(file_id);
        debug_shift_file(loglevel);
        pthread_mutex_unlock(&log_mutex);

        return 0;
    }

    static int create_multi_dir(const char *path)
    {
        int i, len;

        len = strlen(path);
        char dir_path[len + 1];
        dir_path[len] = '\0';

        strncpy(dir_path, path, len);

        for (i = 0; i < len; i++)
        {
            if (dir_path[i] == '/' && i > 0)
            {
                dir_path[i] = '\0';
                if (access(dir_path, F_OK) < 0)
                {
                    if (mkdir(dir_path, 0755) < 0)
                    {
                        printf("mkdir=%s:msg=%s\n", dir_path, strerror(errno));
                        return -1;
                    }
                }
                dir_path[i] = '/';
            }
        }

        return 0;
    }

    int32_t check_path(const char *path)
    {
        struct stat st_stat = {0};
        int ret = stat(path, &st_stat);
        
        if (ret && errno != ENOENT)
        {
            fprintf(stderr, "Check directory error: %s\n", strerror(errno));
            return 1;
        }

        if ((ret && errno == ENOENT) || (!ret && !S_ISDIR(st_stat.st_mode)))
        {
            // create dir, rwxr-xr-x 
            if (mkdir(path, S_IRWXU | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH))
            {
                fprintf(stderr, "Crate directory error: %s\n", strerror(errno));

                return 1;
            }
        }

        return 0;
    }

    int32_t debug_init_log()
    {
        int32_t path_len = strlen(SDK_CFG_LOG_PATH);
        debug_log_cfg *log_cfg = &gst_log_cfg;
        debug_log_file *log_file = &gst_log_file;
        debug_log_run *log_run = &gst_log_run;

        log_cfg->level = SDK_CFG_LOG_LEVEL;
        log_cfg->num = SDK_CFG_LOG_NUM;
        log_cfg->size = SDK_CFG_LOG_SIZE;
        log_cfg->shift_type = _LOG_SHIFT_SIZE;
        log_cfg->file_type = 2;

        if (SDK_CFG_LOG_PATH[path_len] != '/')
        {
            snprintf(&log_cfg->path[0], NAME_LEN - 1,
                    "%s%s", SDK_CFG_LOG_PATH, "/");
        }
        else
        {
            snprintf(&log_cfg->path[0], NAME_LEN - 1,
                    "%s", SDK_CFG_LOG_PATH);
        }

        create_multi_dir(SDK_CFG_LOG_PATH);

        check_path(SDK_CFG_LOG_PATH);

        debug_init_logfile(log_file, log_cfg);

        log_run->log_pid = getpid();
        log_run->log_type = log_cfg->file_type;
        log_run->log_level = log_cfg->level;

        return 0;
    }


}  // namespace dataproxy_sdk
