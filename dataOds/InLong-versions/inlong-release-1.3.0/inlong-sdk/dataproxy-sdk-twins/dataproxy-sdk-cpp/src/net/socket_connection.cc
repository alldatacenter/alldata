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

#include "socket_connection.h"

#include <chrono>
#include <mutex>
#include <stdint.h>

#include "buffer_pool.h"
#include "proxylist_config.h"
#include "sdk_constant.h"
#include "send_buffer.h"
#include "utils.h"

namespace dataproxy_sdk
{
    Connection::Connection(ExecutorThreadPtr &executor, ProxyInfoPtr &proxyinfo)
        : executor_(executor), thread_id_(executor_->threadId()), socket_(std::move(executor_->createTcpSocket())), status_(kConnecting), timer_(std::move(executor->createSteadyTimer())), proxyinfo_(proxyinfo), remote_info_(proxyinfo_->getString()), recv_buf_(std::make_shared<RecvBuffer>()), loads_(30), total_send_(0), total_read_(0), waiting_send_(0), send_err_nums_(0), next_load_idx_(0)
    {
        asio::ip::tcp::endpoint ep(asio::ip::address::from_string(proxyinfo_->ip()), static_cast<uint16_t>(proxyinfo_->portNum()));
        doConnect(ep);
    }

    Connection::~Connection() {}

    void Connection::doConnect(const asio::ip::tcp::endpoint &ep)
    {
        if (isStop())
        {
            LOG_WARN("fail to doConnect %s, connection status is disconnected", remote_info_.c_str());
            return;
        }
        status_ = kConnecting;
        timer_->expires_after(std::chrono::milliseconds(kConnectTimeout));
        timer_->async_wait(std::bind(&Connection::connectHandler, this, std::placeholders::_1));
        if (g_config.enable_TCP_nagle_ == false)
        {
            socket_->set_option(asio::ip::tcp::no_delay(true));
        } // close nagle
        socket_->async_connect(ep, [this](const std::error_code &ec)
                               {
        timer_->cancel();
        if (ec)
        {
            status_ = kDisconnected;
            LOG_ERROR("%s async connect error: %s,%s", remote_info_.c_str(), ec.message().c_str(), ec.category().name());
            doClose();
            return;
        }
        status_ = kConnected;
        socket_->set_option(asio::ip::tcp::no_delay(true));
        setLocalInfo();
        LOG_INFO("l:%s->r:%s is connected", local_info_.c_str(), remote_info_.c_str());

        doRead();
        doWrite(); });
    }

    void Connection::sendBuf(SendBuffer *buf)
    {
        auto self = shared_from_this();
        executor_->postTask([self, this, buf]()
                            {
        if (std::find(write_queue_.begin(), write_queue_.end(), buf) != write_queue_.end())
        {
            LOG_ERROR("send_buf (uid:%d) is repeat in connection write_queue", buf->uniqId());
            return;
        }

        bool queue_empty = write_queue_.empty();
        write_queue_.push_back(buf);
        waiting_send_.increment();
        executor_->waiting_send_.increment();
        LOG_TRACE("send_buf(uid:%d) is added to connection send queue, conn r:%s", buf->uniqId(), remote_info_.c_str());
        if (isConnected() && queue_empty) { doWrite(); } });
    }

    void Connection::sendHB(bool isBinHB)
    {
        if (isStop())
        {
            return;
        }
        if (!write_queue_.empty())
        {
            LOG_INFO("conn l:%s->r:%s has cache msg to send, heartbeat will try next time", local_info_.c_str(), remote_info_.c_str());
            return;
        }
        binHB_.total_len = htonl(sizeof(BinaryHB) - 4);
        binHB_.msg_type = 8;
        binHB_.data_time = htonl(static_cast<uint32_t>(Utils::getCurrentMsTime() / 1000));
        binHB_.body_ver = 1;
        binHB_.body_len = 0;
        binHB_.attr_len = 0;
        binHB_.magic = htons(constants::kBinaryMagic);

        char *hb;
        uint32_t hb_len = 0;
        if (isBinHB)
        {
            hb_len = sizeof(binHB_);
            hb = (char *)&binHB_;
        }
        else
        {
            hb_len = sizeof(msgHB);
            hb = (char *)msgHB;
        }
        LOG_DEBUG("conn l:%s->r:%s send %s", local_info_.c_str(), remote_info_.c_str(), isBinHB ? "binaryHB" : "msgHB");
        auto self = shared_from_this();
        asio::async_write(*socket_, asio::buffer(hb, hb_len), [self, this, hb_len, isBinHB](const std::error_code &ec, std::size_t reslen)
                          {
        if (!ec && reslen == hb_len)  // send success
        {
            LOG_DEBUG("conn l:%s->r:%s send %s successfully", local_info_.c_str(), remote_info_.c_str(), isBinHB ? "binaryHB" : "msgHB");
            send_err_nums_.getAndSet(0);
            retry_hb_.increment();
        }
        else 
        {
            send_err_nums_.increment();
            LOG_ERROR("conn l:%s->r:%s send heartbeat error, this conn send_error_num:%d, error message:%s", local_info_.c_str(),
                      remote_info_.c_str(), send_err_nums_.get(), ec.message().c_str());
        }

        if (retry_hb_.get() > g_config.retry_num_)  //close and create new conn
        {
            LOG_ERROR("conn l:%s->r:%s send_error_num:%d, more than max_retry_num:%d, this conn will close", local_info_.c_str(),
                      remote_info_.c_str(), retry_hb_.get(), g_config.retry_num_);
            doClose(&ec);
        } });
    }

    void Connection::connClose()
    {
        auto self = shared_from_this();
        executor_->postTask([self, this]()
                            { doClose(); });
        LOG_DEBUG("post close request: conn l:%s->r:%s", local_info_.c_str(), remote_info_.c_str());
    }

    void Connection::doClose(const std::error_code *err)
    {
        if (isStop())
        {
            return;
        }

        status_ = kDisconnected;
        LOG_WARN("close conn, l:%s->r:%s", local_info_.c_str(), remote_info_.c_str());
        socket_->close();

        //clean
        write_queue_.clear();
        recv_buf_->Reset();
    }

    void Connection::doWrite()
    {
        if (isStop())
        {
            return;
        }
        if (write_queue_.empty())
        {
            return;
        }

        auto self = shared_from_this();
        auto curBuf = write_queue_.front();

        std::lock_guard<std::mutex> buf_lck(curBuf->mutex_);
        asio::async_write(*socket_, asio::buffer(curBuf->content(), curBuf->len()), [self, this, curBuf](const std::error_code &ec, std::size_t length)
                          {
        write_queue_.pop_front();

        if (!ec)  //send success
        {
            
            {  // lock sendbuf
                std::lock_guard<std::mutex> buf_lck(curBuf->mutex_);

                total_send_.increment();
                waiting_send_.decrement();
                executor_->waiting_send_.decrement();
                send_err_nums_.getAndSet(0);
                LOG_TRACE("l:%s->r:%s async write data success(len:%d), content_len:%d,  buf_id:%d", local_info_.c_str(), remote_info_.c_str(),
                          length, curBuf->len(), curBuf->uniqId());

                //ack buf
                if (g_config.msg_type_ == 2) { curBuf->timeout_timer_->cancel(); }

            }

            doWrite();
        }
        else 
        {
            std::lock_guard<std::mutex> buf_lck(curBuf->mutex_);
            send_err_nums_.increment();
            LOG_ERROR("l:%s->r:%s async write data error, buf_id:%d, error message:%s", local_info_.c_str(), remote_info_.c_str(), curBuf->uniqId(),
                      ec.message().c_str());
            doClose(&ec);

            auto new_conn = g_clusters->createActiveConn(curBuf->inlong_group_id(), thread_id_);
            if (!new_conn) { curBuf->fail_create_conn_.increment(); }
            curBuf->setTarget(new_conn);
            return;
        } });
    }

    void Connection::doRead()
    {
        if (isStop())
        {
            return;
        }

        if (recv_buf_->length() == 0)
            recv_buf_->Reset();

        recv_buf_->EnsureWritableBytes(1024);
        auto self = shared_from_this();
        socket_->async_receive(
            asio::buffer(recv_buf_->WriteBegin(), recv_buf_->WritableBytes()), [self, this](const std::error_code &ec, std::size_t len)
            {
            if (ec)
            {
                LOG_ERROR("async read data error, l:%s->r:%s, error message:%s", local_info_.c_str(), remote_info_.c_str(), ec.message().c_str());
                doClose(&ec);
                return;
            }
            if (len == 0)
            {
                LOG_ERROR("async read 0 bytes, l:%s->r:%s", local_info_.c_str(), remote_info_.c_str());
                doClose(&ec);
                return;
            }
            recv_buf_->WriteBytes(len);

            //read the rest content
            std::error_code tmp_error;
            size_t left_size = socket_->available(tmp_error);
            if (left_size > 0 && !tmp_error)
            {
                recv_buf_->EnsureWritableBytes(left_size);
                size_t rlen = socket_->receive(asio::buffer(recv_buf_->WriteBegin(), left_size));
                if (rlen > 0) { recv_buf_->WriteBytes(rlen); }
            }
            LOG_TRACE("conn read %d bytes,  l:%s->r:%s", recv_buf_->length(), local_info_.c_str(), remote_info_.c_str());
            // parse ack package
            doParse();
            doRead(); });
    }

    void Connection::connectHandler(const std::error_code &ec)
    {
        if (ec)
            return;
        if (isStop())
            return;
        LOG_ERROR("connect timeout, %s", remote_info_.c_str());
        doClose();
    }

    int32_t Connection::doParse()
    {
        while (true)
        {
            if (recv_buf_->length() < 5)
            {
                return 0;
            }

            if (recv_buf_->length() < recv_buf_->PeekUint32() + 4)
            {
                return 0;
            }
            //read ack package
            uint32_t total_len = recv_buf_->ReadUint32();
            uint8_t msg_type = recv_buf_->ReadUint8();
            if (msg_type == 3 || msg_type == 5 || msg_type == 6 || (msg_type & 0x1F) == 7)
            {
                if ((msg_type & 0x1F) != 7)
                {
                    bool ret = parseProtocolAck(total_len);
                    LOG_TRACE("parseProtocolAck success? %d, %s", ret, remote_info_.c_str());
                }
                else
                {
                    bool ret = parseBinaryAck(total_len);
                    LOG_TRACE("parseBinaryAck success? %d, %s", ret, remote_info_.c_str());
                }
            }
            else if (msg_type == 1 && total_len == 0x1) 
            {
                retry_hb_.getAndSet(0);
                LOG_TRACE("success to parse a msghb_ack from %s", remote_info_.c_str());
            }
            else if (msg_type == 8) //binary hb
            {
                retry_hb_.getAndSet(0);
                bool ret = parseBinaryHB(total_len);
                LOG_TRACE("parseBinaryHB success? %d,%s", ret, remote_info_.c_str());
            }
            else
            {
                //wrong msg_type 
                LOG_ERROR("parse ack, and get wrong msgtype: %d, proxy info%s", msg_type, remote_info_.c_str());
                return 1;
                // FIXME: need add other handler, such as close conn?
            }
        }
        return 0;
    }

    bool Connection::parseProtocolAck(uint32_t total_len)
    {
        uint32_t body_len = recv_buf_->ReadUint32();
        if (body_len > recv_buf_->length())
        {
            LOG_ERROR("body_len is %d, more than recv_buf left len:%d ", body_len, recv_buf_->length());
            if (total_len < 4)
            {
                LOG_ERROR("total_len is less than 4, this should be check");
            }
            recv_buf_->Skip(total_len - 4);
            return false;
        }
        recv_buf_->Skip(body_len);
        LOG_TRACE("body_len is %d, and skip body");
        uint32_t attr_len = recv_buf_->ReadUint32();
        char attr[attr_len + 1];
        memset(attr, 0x0, attr_len + 1);
        strncpy(attr, recv_buf_->data(), attr_len);
        recv_buf_->Skip(attr_len);
        LOG_TRACE("attr_len is %d, attr info: %s", attr_len, attr);
        uint32_t buf_uniqId = parseAttr(attr, attr_len);

        auto bpr=g_pools->getUidBufPool(buf_uniqId);
        if(bpr!=nullptr){
            bpr->ackBuf(buf_uniqId);
            return true;
        }

        return false;
    }

    bool Connection::parseBinaryAck(uint32_t total_len)
    {
        uint32_t uniq = recv_buf_->ReadUint32();
        uint16_t attr_len = recv_buf_->ReadUint16();
        recv_buf_->Skip(attr_len);
        uint16_t magic = recv_buf_->ReadUint16();

        if (total_len + 4 != 13 + attr_len)
        {
            LOG_ERROR("failed to parse binary ack, total_len(%d) + 4 != attr_len(%d) + 13", total_len, attr_len);
            return false;
        }
        if (magic != constants::kBinaryMagic)
        {
            LOG_ERROR("failed to parse binary ack, get error magic: %d", magic);
            return false;
        }

        auto bpr=g_pools->getUidBufPool(uniq);
        if(bpr!=nullptr){
            bpr->ackBuf(uniq);
        }

        return true;
    }

    bool Connection::parseBinaryHB(uint32_t total_len)
    {
        uint32_t data_time = recv_buf_->ReadUint32();
        uint8_t body_ver = recv_buf_->ReadUint8();
        uint32_t body_len = recv_buf_->ReadUint32();
        uint16_t load = recv_buf_->PeekUint16(); // proxy load
        recv_buf_->Skip(body_len);
        uint16_t attr_len = recv_buf_->ReadUint16();
        recv_buf_->Skip(attr_len);
        uint16_t magic = recv_buf_->ReadUint16();

        if (total_len + 4 != 18 + attr_len + body_len)
        {
            LOG_ERROR("failed to parse binary heartbeat ack, total_len(%d) + 4 != 18 + attr_len(%d) + body_len(%d)", total_len, attr_len, body_len);
            return false;
        }

        if (magic != constants::kBinaryMagic)
        {
            LOG_ERROR("failed to parse binary heartbeat ack, get error magic: %d", magic);
            return false;
        }

        std::lock_guard<std::mutex> lck(load_mutex_);
        if (body_ver == 1 && body_len == 2)
        {
            loads_[next_load_idx_ % 30] = load;
            LOG_TRACE("update proxy%s load, cur_load:%d, cur_idx:%d", remote_info_.c_str(), load, next_load_idx_);
        }
        else
        {
            loads_[next_load_idx_ % 30] = 0;
            LOG_TRACE("update proxy%s loads, cur_load:%d, cur_idx:%d", remote_info_.c_str(), load, next_load_idx_);
        }
        ++next_load_idx_;

        return true;
    }

    uint32_t Connection::parseAttr(char *attr, int32_t attr_len)
    {
        char *mid = nullptr;
        LOG_TRACE("ack attr:%s", attr);

        mid = strstr(attr, "mid=");
        if (!mid)
        {
            if (attr[attr_len - 1] != '\0')
            {
                attr[attr_len - 1] = '\0';
                LOG_ERROR("force show len(%d) attr:%s.", attr_len, attr);
            }
            else
            {
                LOG_ERROR("show len(%d) attr:%s.", attr_len, attr);
            }

            return -1;
        }

        uint32_t buf_uniqId = atoi(&mid[4]);
        LOG_TRACE("parse ack and get buf uid:%d", buf_uniqId);

        return buf_uniqId;
    }

    int32_t Connection::getAvgLoad()
    {
        if (isStop())
            return -1;

        std::lock_guard<std::mutex> lck(load_mutex_);
        int32_t numerator = 0;   
        int32_t denominator = 0;
        for (int i = 0; i < loads_.size(); i++)
        {
            if (loads_[i] > 0)
            {
                numerator += loads_[i] * constants::kWeight[i];
                denominator += constants::kWeight[i];
            }
        }
        if (0 == denominator)
            return 0;
        return numerator / denominator;
    }

    void Connection::setLocalInfo()
    {
        local_info_ = "[ip:" + socket_->local_endpoint().address().to_string() + ", port:" + std::to_string(socket_->local_endpoint().port()) + "]";
    }

} // namespace dataproxy_sdk
