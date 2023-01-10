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

#include <Interpreters/NamedSession.h>

#include <Common/setThreadName.h>
#include <Interpreters/Context.h>
#include <Transaction/TxnTimestamp.h>
#include <CloudServices/CnchWorkerResource.h>

#include <chrono>

namespace DB
{

namespace ErrorCodes
{
    extern const int SESSION_NOT_FOUND;
    extern const int SESSION_IS_LOCKED;
}

template<typename NamedSession>
NamedSessionsImpl<NamedSession>::~NamedSessionsImpl()
{
    try
    {
        {
            std::lock_guard lock{mutex};
            quit = true;
        }

        cond.notify_one();
        thread.join();
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }
}

template<typename NamedSession>
std::shared_ptr<NamedSession> NamedSessionsImpl<NamedSession>::acquireSession(
    const Key & session_id,
    ContextPtr context,
    std::chrono::steady_clock::duration timeout,
    bool throw_if_not_found)
{
    std::unique_lock lock(mutex);

    auto it = sessions.find(session_id);
    if (it == sessions.end())
    {
        if (throw_if_not_found)
            throw Exception("Session not found.", ErrorCodes::SESSION_NOT_FOUND);
        else
            LOG_DEBUG(&Poco::Logger::get("NamedCnchSession"), "Session not found, and create a new one");

        it = sessions.insert(std::make_pair(session_id, std::make_shared<NamedSession>(session_id, context, timeout, *this))).first;
    }

    /// Use existing session.
    const auto & session = it->second;

    /// For cnch, it's of for session to not be unique, e.g. in union query, the sub-query will have same transaction id,
    /// therefore they shared same session on worker.
    if constexpr (!std::is_same_v<NamedSession,NamedCnchSession>)
    {
        if (!session.unique())
            throw Exception("Session is locked by a concurrent client.", ErrorCodes::SESSION_IS_LOCKED);
    }

    return session;
}

template<typename NamedSession>
void NamedSessionsImpl<NamedSession>::scheduleCloseSession(NamedSession & session, std::unique_lock<std::mutex> &)
{
    /// Push it on a queue of sessions to close, on a position corresponding to the timeout.
    /// (timeout is measured from current moment of time)

    const UInt64 close_index = session.timeout / close_interval + 1;
    const auto new_close_cycle = close_cycle + close_index;

    if (session.close_cycle != new_close_cycle)
    {
        session.close_cycle = new_close_cycle;
        if (close_times.size() < close_index + 1)
            close_times.resize(close_index + 1);
        close_times[close_index].emplace_back(session.key);
    }
}

template<typename NamedSession>
void NamedSessionsImpl<NamedSession>::cleanThread()
{
    setThreadName("SessionCleaner");
    std::unique_lock lock{mutex};

    while (true)
    {
        auto interval = closeSessions(lock);

        if (cond.wait_for(lock, interval, [this]() -> bool { return quit; }))
            break;
    }
}

template<typename NamedSession>
std::chrono::steady_clock::duration NamedSessionsImpl<NamedSession>::closeSessions(std::unique_lock<std::mutex> & lock)
{
    const auto now = std::chrono::steady_clock::now();

    /// The time to close the next session did not come
    if (now < close_cycle_time)
        return close_cycle_time - now;  /// Will sleep until it comes.

    const auto current_cycle = close_cycle;

    ++close_cycle;
    close_cycle_time = now + close_interval;

    if (close_times.empty())
        return close_interval;

    auto & sessions_to_close = close_times.front();

    for (const auto & key : sessions_to_close)
    {
        const auto session = sessions.find(key);

        if (session != sessions.end() && session->second->close_cycle <= current_cycle)
        {
            if (!session->second.unique())
            {
                /// Skip but move it to close on the next cycle.
                session->second->timeout = std::chrono::steady_clock::duration{0};
                scheduleCloseSession(*session->second, lock);
            }
            else
                sessions.erase(session);
        }
    }

    close_times.pop_front();
    return close_interval;
}


NamedSession::NamedSession(NamedSessionKey key_, ContextPtr context_, std::chrono::steady_clock::duration timeout_, NamedSessions & parent_)
    : key(key_), context(Context::createCopy(context_)), timeout(timeout_), parent(parent_)
{
}

void NamedSession::release()
{
    parent.releaseSession(*this);
}

NamedCnchSession::NamedCnchSession(NamedSessionKey key_, ContextPtr context_, std::chrono::steady_clock::duration timeout_, NamedCnchSessions & parent_)
    : key(key_), context(Context::createCopy(context_)), timeout(timeout_), parent(parent_)
{
    context->worker_resource = std::make_shared<CnchWorkerResource>();
}

void NamedCnchSession::release()
{
    timeout = std::chrono::steady_clock::duration{0}; /// release immediately
    parent.releaseSession(*this);
    LOG_DEBUG(&Poco::Logger::get("NamedCnchSession"), "release CnchWorkerResource({})", key);
}

template class NamedSessionsImpl<NamedSession>;
template class NamedSessionsImpl<NamedCnchSession>;

}
