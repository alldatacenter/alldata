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

#include <asio.hpp>
#include <string>
#include <system_error>

#include "socket_connection.h"

using namespace std;
using namespace dataproxy_sdk;

class BigSizeConn : public enable_shared_from_this<BigSizeConn>
{
  private:
    ExecutorThreadPtr executor_;
    string ip_;
    uint16_t port_;
    TcpSocketPtr socket_;
    bool isConnect_;
    char* data_;
    uint32_t len_;
    uint32_t runtimes_;
    uint32_t count_;
    RecvBufferPtr recv_buf_;

  public:
    BigSizeConn(ExecutorThreadPtr& executor, string ip, uint16_t port)
        : executor_(executor)
        , ip_(ip)
        , port_(port)
        , socket_(std::move(executor_->createTcpSocket()))
        , isConnect_(false)
        , data_(nullptr)
        , len_(0)
        , runtimes_(10)
        , count_(0)

    {
        recv_buf_ = std::make_shared<RecvBuffer>();
        asio::ip::tcp::endpoint ep(asio::ip::address::from_string(ip_), port_);
        doConnect(ep);
    }

    void doRead()
    {
        if (!isConnect_) return;
        recv_buf_->EnsureWritableBytes(1024);
        auto self = shared_from_this();
        socket_->async_receive(asio::buffer(recv_buf_->WriteBegin(), recv_buf_->WritableBytes()),
                               [self, this](const std::error_code& ec, std::size_t len) {
                                   if (ec)
                                   {
                                       LOG_ERROR("async read data error:%s", ec.message().c_str());
                                       doClose(&ec);
                                       return;
                                   }
                                   if (len == 0)
                                   {
                                       LOG_ERROR("async read 0 bytes");
                                       doClose(&ec);
                                       return;
                                   }
                                   recv_buf_->WriteBytes(len);

                                   //读取剩余未读完的数据，如果有的话
                                   std::error_code tmp_error;
                                   size_t left_size = socket_->available(tmp_error);
                                   if (left_size > 0 && !tmp_error)
                                   {
                                       recv_buf_->EnsureWritableBytes(left_size);
                                       size_t rlen = socket_->receive(asio::buffer(recv_buf_->WriteBegin(), left_size));
                                       if (rlen > 0) { recv_buf_->WriteBytes(rlen); }
                                   }
                                   LOG_DEBUG("conn read %d bytes", recv_buf_->length());
                                   doRead();
                               });
    }

    void doConnect(const asio::ip::tcp::endpoint& ep)
    {
        socket_->async_connect(ep, [this](const std::error_code& ec) {
            if (ec)
            {
                isConnect_ = false;
                LOG_ERROR("async connetc error");
                doClose(&ec);
                return;
            }
            isConnect_ = true;
            doRead();
        });
    }

    void doClose(const std::error_code* err)
    {
        if (!isConnect_) return;
        LOG_WARN("close connnection");
        socket_->close();
    }

    void doWrite()
    {
        if (!isConnect_ || runtimes_ == 0) return;
        auto self = shared_from_this();
        count_++;
        LOG_DEBUG("send count:%d", count_);
        asio::async_write(*socket_, asio::buffer(data_, len_), [self, this](const std::error_code& ec, std::size_t writelen) {
            LOG_WARN("doWrite callback, writelen:%d", writelen);
            if (writelen > 0 && writelen < len_)
            {                           
                while (writelen < len_)  
                {
                    LOG_WARN("cache data(len:%d) need to be resend", len_ - writelen);
                    writelen += asio::write(*socket_, asio::buffer(data_ + writelen, len_ - writelen));
                }
            }

            if (!ec)  
            {
                if (writelen != len_) { LOG_ERROR("async write error!!!!!!!!!!!!!!!!!!!!!"); }
                --runtimes_;

                doWrite();
            }
            else
            {
                LOG_ERROR("write error:%s", ec.message().c_str());
                doClose(&ec);
            }
        });
    }

    void writeData(char* data, uint32_t len, uint32_t runtimes)
    {
        auto self = shared_from_this();
        data_     = data;
        len_      = len;
        runtimes_ = runtimes;
        executor_->postTask([self, this]() {
            LOG_WARN("-----start send------");
            if (isConnect_) { doWrite(); }
        });
    }
};
using BigSizeConnPtr = shared_ptr<BigSizeConn>;

const uint32_t capacity = 1024 * 1204 * 11;
char data[capacity]     = {0};

int main(int argc, char* argv[])
{
    // g_config = new ClientConfig("config.json");

    int runtimes = 10;
    if (argc == 2) { runtimes = atoi(argv[1]); }

    ExecutorThreadPtr th1 = make_shared<ExecutorThread>(1);
    BigSizeConnPtr conn   = make_shared<BigSizeConn>(th1, "127.0.0.1", 46803);

    string msg(1024 * 1024 * 10, 'a');
    msg += "end";
    string attr =
        "inlong_group_id=test_group_id&inlong_stream_id=bigpacktest&dt=1638241486778&mid=10&cnt=1&sid=0x098714199eefe7f714569014e7000000";

    uint32_t body_len  = msg.size();
    uint32_t attr_len  = attr.size();
    uint32_t total_len = 9 + body_len + attr_len;
    char msg_type      = 3;

    char* p = data;

    *(uint32_t*)p = htonl(total_len);
    p += 4;
    *p = msg_type;
    ++p;
    *(uint32_t*)p = htonl(body_len);
    p += 4;
    memcpy(p, msg.data(), body_len);
    p += body_len;
    *(uint32_t*)p = htonl(attr_len);
    p += 4;
    memcpy(p, attr.data(), attr_len);

    uint32_t sendtotal = total_len + 4;

    this_thread::sleep_for(chrono::seconds(2));

    conn->writeData(data, sendtotal, runtimes);

    this_thread::sleep_for(chrono::minutes(2));

    return 0;
}
