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

#if defined(OS_LINUX)

#include <mutex>
#include <atomic>
#include <Common/Fiber.h>
#include <Common/FiberStack.h>
#include <Common/TimerDescriptor.h>
#include <Common/Epoll.h>
#include <Client/Connection.h>
#include <Client/IConnections.h>
#include <Poco/Timespan.h>

namespace Poco::Net
{
class Socket;
}

namespace DB
{

class RemoteQueryExecutorReadContext
{
public:
    std::atomic_bool is_read_in_progress = false;
    Packet packet;

    std::exception_ptr exception;
    FiberStack stack;
    boost::context::fiber fiber;
    /// This mutex for fiber is needed because fiber could be destroyed in cancel method from another thread.
    std::mutex fiber_lock;

    Poco::Timespan receive_timeout;
    IConnections & connections;
    Poco::Net::Socket * last_used_socket = nullptr;

    /// Here we have three descriptors we are going to wait:
    /// * connection_fd is a descriptor of connection. It may be changed in case of reading from several replicas.
    /// * timer is a timerfd descriptor to manually check socket timeout
    /// * pipe_fd is a pipe we use to cancel query and socket polling by executor.
    /// We put those descriptors into our own epoll which is used by external executor.
    TimerDescriptor timer{CLOCK_MONOTONIC, 0};
    bool is_timer_alarmed = false;
    int connection_fd = -1;
    int pipe_fd[2] = { -1, -1 };

    Epoll epoll;

    std::string connection_fd_description;

    explicit RemoteQueryExecutorReadContext(IConnections & connections_);
    ~RemoteQueryExecutorReadContext();

    bool checkTimeout(bool blocking = false);
    bool checkTimeoutImpl(bool blocking);

    void setConnectionFD(int fd, Poco::Timespan timeout = 0, const std::string & fd_description = "");
    void setTimer() const;

    bool resumeRoutine();
    void cancel();
};

}

#else

namespace DB
{
class RemoteQueryExecutorReadContext
{
public:
    void cancel() {}
};

}
#endif
