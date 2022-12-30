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

#include "pack_queue.h"

#include <cstdlib>
#include <functional>

#include "proxylist_config.h"
#include "sdk_constant.h"
#include "sdk_core.h"
#include "logger.h"
#include "msg_protocol.h"
#include "send_buffer.h"
#include "socket_connection.h"
#include "tc_api.h"
#include "utils.h"
#include "msg_protocol.h"

namespace dataproxy_sdk
{
    PackQueue::PackQueue(const std::string &inlong_group_id, const std::string &inlong_stream_id)
        : cur_len_(0), inlong_group_id_(inlong_group_id), inlong_stream_id_(inlong_stream_id), groupId_num_(0), streamId_num_(0), msg_type_(g_config.msg_type_), data_capacity_(g_config.buf_size_)
    {
        data_ = new char[data_capacity_];
        memset(data_, 0x0, data_capacity_);
        topic_desc_ = "groupId=" + inlong_group_id_ + "&streamId=" + inlong_stream_id_;
        first_use_ = Utils::getCurrentMsTime();
        last_use_ = Utils::getCurrentMsTime();
        data_time_ = 0;
    }

    PackQueue::~PackQueue()
    {
        if (data_)
        {
            delete[] data_;
            data_ = nullptr;
        }
    }

    int32_t PackQueue::sendMsg(const std::string &msg,
                               const std::string &inlong_group_id,
                               const std::string &inlong_stream_id,
                               const std::string &client_ip,
                               uint64_t report_time,
                               UserCallBack call_back)
    {
        std::lock_guard<std::mutex> lck(mutex_);

        //pack previous
        if (isTriggerPack(report_time, msg.size()))
        {
            int32_t res = writeToBuf();
            if (res)
            {
                increasePackErr();
                return res;
            }
        }

        //write data to packqueue
        int32_t append_ret = appendMsg(msg, client_ip, report_time, call_back);
        if (append_ret)
        {
            LOG_ERROR("fail to write to pack queue, inlong_group_id: %s, inlong_stream_id: %s", inlong_group_id.c_str(), inlong_stream_id.c_str());
            return append_ret;
        }

        //if uneable_pack, single msg is written to packqueue and directly sent to buf
        if (!g_config.enable_pack_)
        {
            int32_t res = writeToBuf();
            if (res)
            {
                increasePackErr();
                return res;
            }
        }
        return 0;
    }

    //send pack data to buf
    int32_t PackQueue::writeToBuf()
    {
        if (inlong_group_id_.empty())
        {
            LOG_ERROR("something is wrong, check!!");
            return SDKInvalidResult::kFailGetConn;
        }
        if (msg_set_.empty())
        {
            LOG_ERROR("no msg in msg_set, check!");
            return SDKInvalidResult::kFailGetPackQueue;
        }
        auto conn = g_clusters->getSendConn(inlong_group_id_);
        if (!conn)
        {
            LOG_ERROR("no avaliable connection for inlong_group_id: %s, try later", inlong_group_id_.c_str());
            return SDKInvalidResult::kFailGetConn;
        }
        auto pool = g_pools->getPool(inlong_group_id_);
        if (!pool)
        {
            return SDKInvalidResult::kFailGetBufferPool;
        }

        SendBuffer *send_buf = nullptr;

        int32_t res = pool->getSendBuf(send_buf);
        if (res)
        {
            return res;
        }
        if (!send_buf)
        {
            LOG_ERROR("failed to get send_buf, something gets wrong, checkout!");
            return SDKInvalidResult::kFailGetSendBuf;
        }

        //lock sendbuf and write pack data to sendbuf
        {
            std::lock_guard<std::mutex> buf_lck(send_buf->mutex_);

            uint32_t len = 0;
            int32_t msg_cnt = msg_set_.size();
            // std::string msg_groupid = inlong_group_id_;
            uint32_t uniq_id = g_send_msgid.incrementAndGet();
            if (!packOperate(send_buf->content(), len, uniq_id) || len == 0)
            {
                LOG_ERROR("failed to write data to send buf from pack queue, pool id:%d, buf id:%d", pool->poolId(), pool->writeId());
                return SDKInvalidResult::kFailWriteToBuf;
            }
            send_buf->setLen(len);
            send_buf->setMsgCnt(msg_cnt);
            send_buf->setGroupid(inlong_group_id_);
            send_buf->setStreamid(inlong_stream_id_);
            send_buf->setUniqId(uniq_id);
            send_buf->setTarget(conn);
            send_buf->setIsPacked(true);

            for (auto it : msg_set_)
            {
                send_buf->addUserMsg(it);
            }
        }

        pack_num_.increment();
        g_pools->addUid2BufPool(send_buf->uniqId(),pool);//used for ack finding

        resetPackQueue();

        pool->sendBufToConn(send_buf);
        return 0;
    }

    int32_t PackQueue::appendMsg(const std::string &msg, std::string client_ip, int64_t report_time, UserCallBack call_back)
    {
        //too long msg
        if (msg.size() > g_config.ext_pack_size_)
        {
            LOG_ERROR("msg len (%d) more than ext_pack_size (%d)", msg.size(), g_config.ext_pack_size_);
            return SDKInvalidResult::kMsgTooLong;
        }

        //if datatime is illegal, fix it using current time
        if (Utils::isLegalTime(report_time))
            data_time_ = report_time;
        else
        {
            data_time_ = Utils::getCurrentMsTime();
            pack_redotime_cnt_.increment();
        }

        //used for callback
        if (call_back)
        {
            std::string user_client_ip = client_ip;
            int64_t user_report_time = report_time;
            if (client_ip.empty())
            {
                client_ip = "127.0.0.1";
            }
            std::string data_pack_format_attr = "__addcol1__reptime=" + Utils::getFormatTime(data_time_) + "&__addcol2__ip=" + client_ip;
            msg_set_.emplace_back(std::make_shared<UserMsg>(msg, client_ip, data_time_, call_back, data_pack_format_attr, user_client_ip, user_report_time));
        }
        
        cur_len_ += msg.size() + 1; // '\n' using one byte

        if (g_config.isNormalDataPackFormat())
        {
            cur_len_ += 4;
        }
        if (g_config.isAttrDataPackFormat())
        {
            cur_len_ += constants::kAttrLen + 8;
        }

        //update last using time
        last_use_ = Utils::getCurrentMsTime();

        return 0;
    }

    /**
     * @description: whether trigger pack data
     * @param {uint64_t} report_time
     * @param {int32_t} msg_len
     */
    bool PackQueue::isTriggerPack(uint64_t report_time, int32_t msg_len)
    {
        if (0 == cur_len_ || msg_set_.empty())
            return false;

        if (!Utils::isLegalTime(report_time))
        { 
            report_time = Utils::getCurrentMsTime();
        }

        int64_t max_pack_time_interval = 1800000; // FIXME: use user config？
        bool time_trigger = false;                //timeout trigger
        bool len_trigger = false;                 //content trigger
        if (llabs(report_time - data_time_) > max_pack_time_interval ||
            (report_time != data_time_ && report_time / 1000 / 3600 != data_time_ / 1000 / 3600))
        {
            time_trigger = true;
        }
        if (msg_len + cur_len_ > g_config.pack_size_)
        {
            len_trigger = true;
        }

        return (time_trigger || len_trigger);
    }

    /**
     * @description: do pack operate
     * @param {int8*} pack_data: packed binary data
     * @param {uint32_t&} out_len
     * @return {*} true if pack successfully
     */
    bool PackQueue::packOperate(char *pack_data, uint32_t &out_len, uint32_t uniq_id)
    {
        if (!pack_data)
        {
            LOG_ERROR("nullptr, failed to allocate memory for buf");
            return false;
        }
        //add body into data_, then zip and copy to buffer
        uint32_t idx = 0;
        for (auto &it : msg_set_)
        {
            //msg>=5,body format: data_len|data

            if (msg_type_ >= 5) //add data_len
            {
                *(uint32_t *)(&data_[idx]) = htonl(it->msg.size());
                idx += sizeof(uint32_t);
            }
            //add data
            memcpy(&data_[idx], it->msg.data(), it->msg.size());
            idx += static_cast<uint32_t>(it->msg.size());

            //add attrlen|attr
            if (g_config.isAttrDataPackFormat())
            {
                *(uint32_t *)(&data_[idx]) = htonl(it->data_pack_format_attr.size());
                idx += sizeof(uint32_t);

                memcpy(&data_[idx], it->data_pack_format_attr.data(), it->data_pack_format_attr.size());
                idx += static_cast<uint32_t>(it->data_pack_format_attr.size());
            }

            // msgtype = 2/3 support '\n'
            if (msg_type_ == 2 || msg_type_ == 3)
            {
                data_[idx] = '\n';
                ++idx;
            }
        }

        //preprocess attr
        uint32_t cnt = 1;
        if (msg_set_.size())
        {
            cnt = msg_set_.size();
        }

        //pack
        if (msg_type_ >= constants::kBinPackMethod)
        {
            char *bodyBegin = pack_data + sizeof(BinaryMsgHead) + sizeof(uint32_t); //head+body_len
            uint32_t body_len = 0;

            std::string snappy_res;
            bool isSnappy = isZipAndOperate(snappy_res, idx);
            char real_msg_type;

            if (isSnappy) // need zip
            {
                body_len = static_cast<uint32_t>(snappy_res.size());
                memcpy(bodyBegin, snappy_res.data(), body_len); //copy data to buf
                // msg_type
                real_msg_type = (msg_type_ | constants::kBinSnappyFlag);
            }
            else
            {
                body_len = idx;
                memcpy(bodyBegin, data_, body_len);
                real_msg_type = msg_type_;
            }
            *(uint32_t *)(&(pack_data[sizeof(BinaryMsgHead)])) = htonl(body_len); //set bodylen

            bodyBegin += body_len;

            // groupid_num、streamid_num、ext_field、data_time、cnt、uniq
            uint32_t char_groupid_flag = 0;
            std::string groupid_streamid_char;
            uint16_t groupid_num = 0, streamid_num = 0;
            if (g_config.enableCharGroupid() || groupId_num_ == 0 || streamId_num_ == 0) //using string groupid and streamid
            {
                groupid_num = 0;
                streamid_num = 0;
                groupid_streamid_char = topic_desc_;
                char_groupid_flag = 0x4;
            }
            else
            {
                groupid_num = groupId_num_;
                streamid_num = streamId_num_;
            }
            uint16_t ext_field = (g_config.extend_field_ | char_groupid_flag);
            uint32_t data_time = data_time_ / 1000;

            // attr
            std::string attr;
            if (g_config.enableTraceIP())
            {
                if (groupid_streamid_char.empty())
                    attr = "node1ip=" + g_config.ser_ip_ + "&rtime1=" + std::to_string(Utils::getCurrentMsTime());
                else
                    attr = groupid_streamid_char + "&node1ip=" + g_config.ser_ip_ + "&rtime1=" + std::to_string(Utils::getCurrentMsTime());
            }
            else
            {
                attr = topic_desc_;
            }
            // attrlen
            *(uint16_t *)bodyBegin = htons(attr.size());
            bodyBegin += sizeof(uint16_t);
            // attr
            memcpy(bodyBegin, attr.data(), attr.size());
            bodyBegin += attr.size();

            // magic
            *(uint16_t *)bodyBegin = htons(constants::kBinaryMagic);

            uint32_t total_len = 25 + body_len + attr.size();

            // header
            char *p = pack_data;
            *(uint32_t *)p = htonl(total_len);
            p += 4;
            *p = real_msg_type;
            ++p;
            *(uint16_t *)p = htons(groupid_num);
            p += 2;
            *(uint16_t *)p = htons(streamid_num);
            p += 2;
            *(uint16_t *)p = htons(ext_field);
            p += 2;
            *(uint32_t *)p = htonl(data_time);
            p += 4;
            *(uint16_t *)p = htons(cnt);
            p += 2;
            *(uint32_t *)p = htonl(uniq_id);

            out_len = total_len + 4;
        }
        else
        {
            if (msg_type_ == 3 || msg_type_ == 2)
            {
                --idx;
            }

            // body whether needs zip
            char *bodyBegin = pack_data + sizeof(ProtocolMsgHead) + sizeof(uint32_t);
            uint32_t body_len = 0;
            std::string snappy_res;
            bool isSnappy = isZipAndOperate(snappy_res, idx);
            if (isSnappy)
            {
                body_len = static_cast<uint32_t>(snappy_res.size());
                memcpy(bodyBegin, snappy_res.data(), body_len); //copy
            }
            else
            {
                body_len = idx;
                memcpy(bodyBegin, data_, body_len);
            }
            *(uint32_t *)(&(pack_data[sizeof(ProtocolMsgHead)])) = htonl(body_len); //set bodylen
            bodyBegin += body_len;

            // attr
            std::string attr;
            attr = topic_desc_;
            attr += "&dt=" + std::to_string(data_time_);
            attr += "&mid=" + std::to_string(uniq_id);
            if (isSnappy)
                attr += "&cp=snappy";
            attr += "&cnt=" + std::to_string(cnt);
            attr += "&sid=" + std::string(Utils::getSnowflakeId());
            if (g_config.is_from_DC_)
            { //&__addcol1_reptime=yyyymmddHHMMSS&__addcol2__ip=BBB&f=dc
                attr += "&__addcol1_reptime=" + Utils::getFormatTime(Utils::getCurrentMsTime()) + "&__addcol2__ip=" + g_config.ser_ip_ + "&f=dc";
            }

            // attrlen
            *(uint32_t *)bodyBegin = htonl(attr.size());
            bodyBegin += sizeof(uint32_t);
            // attr
            memcpy(bodyBegin, attr.data(), attr.size());

            // total_len
            uint32_t total_len = 1 + 4 + body_len + 4 + attr.size();
            *(uint32_t *)pack_data = htonl(total_len);
            // msg_type
            *(&pack_data[4]) = msg_type_;

            LOG_TRACE("after packoperate: total_len:%d, body_len:%d, attr_len:%d", total_len, body_len, attr.size());

            out_len = total_len + 4;
        }
        return true;
    }

    /**
     * @description: whether body data should be zipped; if needs, zip data and save as res
     * @param {string&} res
     * @param {uint32_t} real_cur_len: data len before zip
     * @return {*} true if needing zip
     */
    bool PackQueue::isZipAndOperate(std::string &res, uint32_t real_cur_len)
    {
        if (g_config.enable_zip_ && real_cur_len > g_config.min_zip_len_)
        {
            LOG_TRACE("start snappy.");
            Utils::zipData(data_, real_cur_len, res);
            return true;
        }
        else
            return false;
    }

    void PackQueue::checkQueue(bool isLastPack)
    {
        if (cur_len_ == 0 || msg_set_.empty())
            return;
        //no timeout, and it isn't last packing
        if (Utils::getCurrentMsTime() - first_use_ < g_config.pack_timeout_ && !isLastPack)// FIXME:should use first_use instead of last_use?
            return;
        LOG_TRACE("start auto pack, inlong_group_id:%s, inlong_stream_id:%s", inlong_group_id_.c_str(), inlong_stream_id_.c_str());

        std::lock_guard<std::mutex> lck(mutex_);
        last_use_ = Utils::getCurrentMsTime();
        //send fail, callback and reset
        if (writeToBuf()) 
        {
            for (auto& it : msg_set_)
            {
               if (it->cb) { it->cb(inlong_group_id_.data(), inlong_stream_id_.data(), it->msg.data(), it->msg.size(), it->user_report_time, it->user_client_ip.data()); }
         
            }
            resetPackQueue();
            
        }
    }

    GlobalQueues::~GlobalQueues()
    {
        closeCheckSubroutine();
        if (worker_.joinable())
        {
            worker_.join();
        }
        for (auto it : queues_) 
        {
            it.second->checkQueue(true);
        }
        queues_.clear();
    }

    void GlobalQueues::startCheckSubroutine() { worker_ = std::thread(&GlobalQueues::checkPackQueueSubroutine, this); }

    void GlobalQueues::checkPackQueueSubroutine()
    {
        LOG_INFO("start checkPackQueue subroutine");
        while (!exit_flag_)
        {
            Utils::taskWaitTime(2); // FIXME:improve pack interval
            for (auto it : queues_)
            {
                it.second->checkQueue(exit_flag_);
            }
        }
        LOG_WARN("exit checkPackQueue subroutine");
    }

    //get pack queue, create if not exists
    PackQueuePtr GlobalQueues::getPackQueue(const std::string &inlong_group_id, const std::string &inlong_stream_id)
    {
        std::lock_guard<std::mutex> lck(mutex_);

        auto it = queues_.find(inlong_group_id + inlong_stream_id);
        if (it != queues_.end())
            return it->second;
        else
        {
            PackQueuePtr p = std::make_shared<PackQueue>(inlong_group_id, inlong_stream_id);
            queues_.emplace(inlong_group_id + inlong_stream_id, p);
            return p;
        }
    }
    void GlobalQueues::printAck()
    {
        if (queues_.empty())
            return;
        for (auto &it : queues_)
        {
            LOG_STAT("dataproxy_sdk_cpp #local:%s#%s#success send msg:%d", g_config.ser_ip_.c_str(), it.second->topicDesc().c_str(),
                     it.second->success_num_.getAndSet(0));
        }
    }

    void GlobalQueues::printTotalAck()
    {
        if (queues_.empty())
            return;
        for (auto &it : queues_)
        {
            LOG_STAT("dataproxy_sdk_cpp #local:%s#%s#total success msg:%d", g_config.ser_ip_.c_str(), it.second->topicDesc().c_str(),
                     it.second->total_success_num_.get());
        }
    }

    void GlobalQueues::showState()
    {
        if (1 == user_exit_flag.get())
            return;
        uint32_t total_pack = 0;
        for (auto &it : queues_)
        {
            uint32_t pack = it.second->pack_num_.getAndSet(0);
            total_pack += pack;
            LOG_DEBUG("toipc:%s, pack_num:%d", it.second->topicDesc().c_str(), pack);
        }
        LOG_DEBUG("total_pack:%d", total_pack);

        g_pools->showState();
        g_executors->showState();
    }

} // namespace dataproxy_sdk