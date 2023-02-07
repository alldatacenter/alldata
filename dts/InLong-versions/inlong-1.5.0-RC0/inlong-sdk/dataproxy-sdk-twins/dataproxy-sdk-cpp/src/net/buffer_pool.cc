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

#include "buffer_pool.h"

#include <chrono>
#include <memory>
#include <vector>

#include "proxylist_config.h"
#include "sdk_core.h"
#include "logger.h"
#include "send_buffer.h"
#include "socket_connection.h"
#include "tc_api.h"

namespace dataproxy_sdk
{
    BufferPool::BufferPool(uint32_t pool_id, uint32_t buf_num, uint32_t buf_size)
        : pool_id_(pool_id), buf_num_(buf_num), read_(0), write_(0), current_use_(0), pool_mutex_(), executor_(std::make_shared<ExecutorThread>(pool_id))
    {
        buffers_.reserve(buf_num_);
        for (int i = 0; i < buf_num_; i++)
        {
            buffers_.push_back(new SendBuffer(buf_size));
        }
    }

    BufferPool::~BufferPool()
    { 
        for (auto it : buffers_)
        {
            delete it;
        }
        buffers_.clear();
        LOG_DEBUG("free send buffer memory, pool(id:%d)", pool_id_);
    }

    bool BufferPool::isAvaliable()
    {
        return current_use_ < buf_num_;
    }

    int32_t BufferPool::getSendBuf(SendBuffer *&send_buf)
    {
        std::lock_guard<std::mutex> lck(pool_mutex_);

        if (current_use_ >= buf_num_)
        {
            LOG_ERROR("buffer pool(id:%d) is full", pool_id_);
            return SDKInvalidResult::kBufferPoolFull;
        }

        while (buffers_[write_]->isUsed())
        {
            write_ = (write_ + 1) % buf_num_;
        }

        send_buf = buffers_[write_];
        // ++write_;
        if (!send_buf)
        {
            LOG_ERROR("failed to get send buf, pool(id:%d)", pool_id_);
            return SDKInvalidResult::kFailGetSendBuf;
        }

        send_buf->setIsUsed(true);
        ++current_use_;

        return 0;
    }

    int32_t BufferPool::sendBufToConn(SendBuffer *&send_buf)
    {
        std::lock_guard<std::mutex> pool_lck(pool_mutex_);

        std::lock_guard<std::mutex> buf_lck(send_buf->mutex_);

        auto self = shared_from_this();
        if (g_config.retry_num_ > 0 && g_config.retry_interval_ > 0) //resend if need
        {
            executor_->postTask([self, this, send_buf]
                                {
                send_buf->timeout_timer_ = executor_->createSteadyTimer();
                send_buf->timeout_timer_->expires_after(std::chrono::milliseconds(g_config.retry_interval_));
                send_buf->timeout_timer_->async_wait(std::bind(&BufferPool::RetryHandler, shared_from_this(), std::placeholders::_1, send_buf)); });
        }
        if (send_buf->target()->isStop())
        {
            auto new_conn = g_clusters->createActiveConn(send_buf->inlong_group_id(), pool_id_);
            if (!new_conn)
            {
                LOG_WARN("first send buf, fail to create new conn for sendbuf, inlong_group_id:%s, inlong_stream_id:%s", send_buf->inlong_group_id().c_str(), send_buf->inlong_stream_id().c_str());
                send_buf->fail_create_conn_.increment();
                // send_buf->increaseRetryNum();
                // send_err_.increment();
                already_send_buf_list_[send_buf->uniqId()] = send_buf; //preparing for next sending
                return -1;
            }
            send_buf->setTarget(new_conn);
        }
        send_buf->target()->sendBuf(send_buf); //do send operation
        send_buf->increaseRetryNum();          
        LOG_TRACE("request buf(id:%d) send to connection%s first time, inlong_group_id:%s, inlong_stream_id:%s", send_buf->uniqId(), send_buf->target()->getRemoteInfo().c_str(),
                  send_buf->inlong_group_id().c_str(), send_buf->inlong_stream_id().c_str());

        already_send_buf_list_[send_buf->uniqId()] = send_buf; //add buf uid into sent list

        send_total_.increment();

        //update metrics
        has_send_buf_.increment();
        waiting_ack_.increment();

        LOG_TRACE("success to write buf pool, pool(id:%d), buf(uid:%d), inlong_group_id:%s, inlong_stream_id:%s", pool_id_, send_buf->uniqId(), send_buf->inlong_group_id().c_str(),
                  send_buf->inlong_stream_id().c_str());
        return 0;
    }

    void BufferPool::RetryHandler(const std::error_code &ec, SendBuffer *buf)
    {
        if (ec) // timer is cancelled, two cases: 1.ackbuf->sendbuf.reset;2.msg_type=2,conn.doWrite->cancel
        {
            if (g_config.msg_type_ == 2)
            {
                LOG_TRACE("msg_type is 2, no need ackmsg, clear buf(uid:%d) directly", buf->uniqId());
                ackBuf(buf->uniqId());
            }
            return;
        }

        if (!buf->isUsed())
        {
            return;
        } // buf is already acked before retry

        std::lock_guard<std::mutex> buf_lck(buf->mutex_);

        if (buf->getAlreadySend() == 2)
        {
            LOG_INFO("buf(id:%d, inlong_group_id:%s, inlong_stream_id:%s) ackmsg timeout, send %d times(max retry_num:%d)", buf->uniqId(), buf->inlong_group_id().c_str(),
                  buf->inlong_stream_id().c_str(), buf->getAlreadySend(), g_config.retry_num_);
        }
        else
        {
            LOG_DEBUG("buf(id:%d, inlong_group_id:%s, inlong_stream_id:%s) ackmsg timeout, send %d times(max retry_num:%d)", buf->uniqId(), buf->inlong_group_id().c_str(),
                  buf->inlong_stream_id().c_str(), buf->getAlreadySend(), g_config.retry_num_);
        }
        
        // max_retry_num, usercallback
        if (buf->getAlreadySend() >= g_config.retry_num_ || buf->fail_create_conn_.get() >= constants::kMaxRetryConnection)
        {
            LOG_WARN("fail to send buf(id:%d, inlong_group_id:%s, inlong_stream_id:%s), has send max_retry_num(%d) times, start usercallback", buf->uniqId(), buf->inlong_group_id().c_str(),
                     buf->inlong_stream_id().c_str(), g_config.retry_num_);
            buf->doUserCallBack();
            buf->reset();
        }
        else // ack timeout, resend
        {
            if (!buf->target() || buf->target()->isStop())
            {
                auto new_conn = g_clusters->createActiveConn(buf->inlong_group_id(), pool_id_); // TODO: should improve as choosing from active conn instread of creating?

                if (!new_conn) //create conn error, waiting for next creating
                {
                    LOG_INFO("fail to create new conn to send buf, inlong_group_id:%s, inlong_stream_id:%s", buf->inlong_group_id().c_str(), buf->inlong_stream_id().c_str());
                    buf->fail_create_conn_.increment();
                    buf->timeout_timer_->expires_after(std::chrono::milliseconds(g_config.retry_interval_));
                    buf->timeout_timer_->async_wait(std::bind(&BufferPool::RetryHandler, shared_from_this(), std::placeholders::_1, buf));
                    return;
                }
                buf->setTarget(new_conn);
            }

            buf->target()->sendBuf(buf);
            buf->increaseRetryNum();
            buf->timeout_timer_->expires_after(std::chrono::milliseconds(g_config.retry_interval_));
            buf->timeout_timer_->async_wait(std::bind(&BufferPool::RetryHandler, shared_from_this(), std::placeholders::_1, buf));
        }
    }

    void BufferPool::ackBufHelper(uint32_t uniq_id)
    {
        std::lock_guard<std::mutex> lck(pool_mutex_);
        if (already_send_buf_list_.find(uniq_id) == already_send_buf_list_.end())
        {
            LOG_ERROR("no buf(uid:%d) in already_send_buf_list", uniq_id);
            return;
        }
        auto &buf2ack = already_send_buf_list_[uniq_id];

        std::lock_guard<std::mutex> buf_lck(buf2ack->mutex_);

        int32_t msg_cnt = buf2ack->msgCnt();
        success_ack_.add(msg_cnt);

        //update packqueue metrics
        if(g_queues){
            auto packqueue = g_queues->getPackQueue(buf2ack->inlong_group_id(), buf2ack->inlong_stream_id());
            packqueue->success_num_.add(msg_cnt);
            packqueue->total_success_num_.add(msg_cnt);
        }

        already_send_buf_list_[uniq_id]->reset();
        already_send_buf_list_.erase(uniq_id);
        --current_use_;

        waiting_ack_.decrement();
        has_ack_.increment();
        LOG_TRACE("pool(id:%d) success ack msg cumulative num: %d", pool_id_, success_ack_.get());
    }

    void BufferPool::showState(const std::string &inlong_group_id)
    {
        if (inlong_group_id.empty())
        {
            LOG_STAT("STATE|pool_id:%d, current_use:%d(total:%d), invoke_send_buf:%d, has_ack:%d, waiting_ack:%d", pool_id_, currentUse(),
                      g_config.bufNum(), has_send_buf_.get(), has_ack_.get(), waiting_ack_.get());
        }
        else
        {
            LOG_STAT("STATE|inlong_group_id:%s, pool_id:%d, current_use:%d(total:%d), invoke_send_buf:%d, has_ack:%d, waiting_ack:%d", inlong_group_id.c_str(), pool_id_, currentUse(),
                      g_config.bufNum(), has_send_buf_.get(), has_ack_.get(), waiting_ack_.get());
        }
    }

    void BufferPool::close()
    {
        if (executor_ != nullptr)
        {
            executor_->close();
        }
        executor_.reset();
    }

    TotalPools::TotalPools() : next_(0), mutex_()
    {
        if (g_config.enable_groupId_isolation_) // different groupid data use different bufpool
        {
            for (int32_t i = 0; i < g_config.inlong_group_ids_.size(); i++) // create a bufpool for ervery groupidS
            {
                std::vector<BufferPoolPtr> groupid_pool;
                groupid_pool.reserve(g_config.buffer_num_per_groupId_);
                for (int32_t j = 0; j < g_config.buffer_num_per_groupId_; j++)
                {
                    groupid_pool.push_back(std::make_shared<BufferPool>(j, g_config.bufNum(), g_config.buf_size_));
                }

                groupid2pool_map_[g_config.inlong_group_ids_[i]] = groupid_pool;
                groupid2next_[g_config.inlong_group_ids_[i]] = 0;
            }
        }
        else // round-robin
        {
            pools_.reserve(g_config.shared_buf_nums_);
            for (int i = 0; i < g_config.shared_buf_nums_; i++)
            {
                pools_.push_back(std::make_shared<BufferPool>(i, g_config.bufNum(), g_config.buf_size_));
            }
        }
    }

    BufferPoolPtr TotalPools::getPool(const std::string &inlong_group_id)
    {
        if (g_config.enable_groupId_isolation_) // groupid isolate
        {
            auto groupid_pool = groupid2pool_map_.find(inlong_group_id);
            if (groupid_pool == groupid2pool_map_.end() || groupid_pool->second.empty())
            {
                LOG_ERROR("fail to get bufferpool, inlong_group_id:%s", inlong_group_id.c_str());
                return nullptr;
            }
            if (groupid2next_.find(inlong_group_id)==groupid2next_.end())
            {
                groupid2next_[inlong_group_id]=0;
            }
            
            auto& pool_set=groupid_pool->second;
            int32_t idx=0;
            for (int32_t i = 0; i < pool_set.size(); i++)
            {
                idx = (groupid2next_[inlong_group_id]++) % pool_set.size();
                if (pool_set[idx]->isAvaliable())
                {
                    return pool_set[idx];
                }
                
            }

            return nullptr;
        }
        else
        {
            if (pools_.empty())
            {
                LOG_ERROR("fail to get bufferpool, allocate error in tc_init");
                return nullptr;
            }

            int32_t idx=0;
            for (int32_t i = 0; i < pools_.size(); i++)
            {
                idx = (next_++) % pools_.size();
                if (pools_[idx]->isAvaliable())
                {
                    return pools_[idx];
                }
                
            }

            return nullptr;
            
        }
    }

    bool TotalPools::isPoolAvailable(const std::string &inlong_group_id)
    {

        if (g_config.enable_groupId_isolation_) // groupid_isolation
        {
            auto groupid_pool = groupid2pool_map_.find(inlong_group_id);
            if (groupid_pool == groupid2pool_map_.end())
            {
                LOG_ERROR("no buffer allocated to inlong_group_id:%s, check config", inlong_group_id.c_str());
                return false;
            }
            for (int i = 0; i < groupid_pool->second.size(); i++)
            {
                if (groupid_pool->second[i]->isAvaliable())
                {
                    return true;
                }
            }
            return false;
        }
        else // rr
        {
            for (int i = 0; i < pools_.size(); i++)
            {
                if (pools_[i]->isAvaliable())
                {
                    return true;
                }
            }
            return false;
        }
    }

    void TotalPools::addUid2BufPool(uint32_t uniqId, BufferPoolPtr& bpr){
        std::lock_guard<std::mutex> lck(mutex_);
        uid2buf_pool_[uniqId]=bpr;
    }

    BufferPoolPtr TotalPools::getUidBufPool(uint32_t uniqId){
        std::lock_guard<std::mutex> lck(mutex_);
        auto iter=uid2buf_pool_.find(uniqId);
        if(iter==uid2buf_pool_.end()){
            return nullptr;
        }
        uid2buf_pool_.erase(iter); //TODO: need add it into bufpool after ack operation
        
        return iter->second;
        
    }

    void TotalPools::showState()
    {
        if (g_config.enable_groupId_isolation_)
        {
            for (auto &groupid_pool : groupid2pool_map_)
            {
                showStateHelper(groupid_pool.first, groupid_pool.second);
            }
        }
        else
        {
            showStateHelper("", pools_);
        }
    }

    void TotalPools::showStateHelper(const std::string &inlong_group_id, std::vector<BufferPoolPtr> &pools)
    {
        for (auto &pool : pools)
        {
            pool->showState(inlong_group_id);
        }
    }

    void TotalPools::close()
    {
        // waiting for sending the rest data
        if (uid2buf_pool_.size())
        {
            LOG_INFO("waiting for 10s to ack remaining msg");
            std::this_thread::sleep_for(std::chrono::seconds(10));
        }
        
        for (auto &pool : pools_)
        {
            pool->close();
        }

        for (auto &groupid_pool : groupid2pool_map_)
        {
            for (auto &pool : groupid_pool.second)
            {
                pool->close();
            }
        }
    }

} // namespace dataproxy_sdk
