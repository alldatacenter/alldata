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

#include <chrono>
#include <memory>
#include <thread>
#include <variant>
#include <Columns/ColumnsNumber.h>
#include <Compression/CompressedReadBuffer.h>
#include <Core/Block.h>
#include <DataTypes/DataTypesNumber.h>
#include <Processors/Exchange/DataTrans/BroadcastSenderProxy.h>
#include <Processors/Exchange/DataTrans/BroadcastSenderProxyRegistry.h>
#include <Processors/Exchange/DataTrans/Brpc/BrpcExchangeReceiverRegistryService.h>
#include <Processors/Exchange/DataTrans/Brpc/BrpcRemoteBroadcastReceiver.h>
#include <Processors/Exchange/DataTrans/Brpc/BrpcRemoteBroadcastSender.h>
#include <Processors/Exchange/DataTrans/Brpc/ReadBufferFromBrpcBuf.h>
#include <Processors/Exchange/DataTrans/Brpc/WriteBufferFromBrpcBuf.h>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <Processors/Exchange/DataTrans/IBroadcastSender.h>
#include <Processors/Exchange/DataTrans/NativeChunkInputStream.h>
#include <Processors/Exchange/DataTrans/NativeChunkOutputStream.h>
#include <Processors/Exchange/ExchangeDataKey.h>
#include <Processors/Exchange/ExchangeOptions.h>
#include <Processors/Exchange/ExchangeSource.h>
#include <Processors/Executors/PullingAsyncPipelineExecutor.h>
#include <Processors/LimitTransform.h>
#include <Processors/QueryPipeline.h>
#include <Processors/Transforms/AggregatingTransform.h>
#include <Processors/tests/gtest_processers_utils.h>
#include <brpc/server.h>
#include <gtest/gtest.h>
#include <Poco/Util/MapConfiguration.h>
#include <Common/Brpc/BrpcApplication.h>
#include <Common/ClickHouseRevision.h>
#include <Common/tests/gtest_global_context.h>
#include <Common/tests/gtest_utils.h>

using namespace DB;
using namespace UnitTest;

using Clock = std::chrono::system_clock;

class ExchangeRemoteTest : public ::testing::Test
{
protected:
    static brpc::Server server;
    static BrpcExchangeReceiverRegistryService service_impl;
    static void startBrpcServer()
    {
        if (server.AddService(&service_impl, brpc::SERVER_DOESNT_OWN_SERVICE) != 0)
        {
            LOG(ERROR) << "Fail to add service";
            return;
        }
        LOG(INFO) << "add service success";

        // Start the server.
        brpc::ServerOptions options;
        options.idle_timeout_sec = -1;
        if (server.Start(8001, &options) != 0)
        {
            LOG(ERROR) << "Fail to start Server";
            return;
        }
        LOG(INFO) << "start Server";
    }

    static void SetUpTestCase()
    {
        UnitTest::initLogger();
        Poco::AutoPtr<Poco::Util::MapConfiguration> map_config = new Poco::Util::MapConfiguration;
        BrpcApplication::getInstance().initialize(*map_config);
        startBrpcServer();
    }

    static void TearDownTestCase() { server.Stop(1000); }
};

brpc::Server ExchangeRemoteTest::server;
BrpcExchangeReceiverRegistryService ExchangeRemoteTest::service_impl(73400320);

static Block getHeader(size_t column_num)
{
    ColumnsWithTypeAndName columns;
    for (size_t i = 0; i < column_num; i++)
    {
        columns.push_back(ColumnWithTypeAndName(ColumnUInt8::create(), std::make_shared<DataTypeUInt8>(), "column" + std::to_string(i)));
    }
    Block header = {columns};
    return header;
}

void receiver1()
{
    auto receiver_data = std::make_shared<ExchangeDataKey>("q1", 1, 1, 1, "localhost:6666");
    Block header = getHeader(1);
    BrpcRemoteBroadcastReceiverShardPtr receiver
        = std::make_shared<BrpcRemoteBroadcastReceiver>(receiver_data, "127.0.0.1:8001", getContext().context, header, true);
    receiver->registerToSenders(1000);
    auto packet = receiver->recv(1000);
    EXPECT_TRUE(std::holds_alternative<Chunk>(packet));
    Chunk & chunk = std::get<Chunk>(packet);
    EXPECT_EQ(chunk.getNumRows(), 1000);
    auto col = chunk.getColumns().at(0);
    EXPECT_EQ(col->getUInt(1), 7);
}

void receiver2()
{
    auto receiver_data = std::make_shared<ExchangeDataKey>("q1", 1, 1, 2, "localhost:6666");
    Block header = getHeader(1);
    BrpcRemoteBroadcastReceiverShardPtr receiver
        = std::make_shared<BrpcRemoteBroadcastReceiver>(receiver_data, "127.0.0.1:8001", getContext().context, header, true);
    receiver->registerToSenders(1000);
    auto packet = receiver->recv(1000);
    EXPECT_TRUE(std::holds_alternative<Chunk>(packet));
    Chunk & chunk = std::get<Chunk>(packet);
    EXPECT_EQ(chunk.getNumRows(), 1000);
    auto col = chunk.getColumns().at(0);
    EXPECT_EQ(col->getUInt(1), 7);
}

TEST_F(ExchangeRemoteTest, SendWithTwoReceivers)
{
    auto receiver_data1 = std::make_shared<ExchangeDataKey>("q1", 1, 1, 1, "localhost:6666");
    auto receiver_data2 = std::make_shared<ExchangeDataKey>("q1", 1, 1, 2, "localhost:6666");

    auto origin_chunk = createUInt8Chunk(1000, 1, 7);
    auto header = getHeader(1);

    std::thread thread_receiver1(receiver1);
    std::thread thread_receiver2(receiver2);

    auto sender_1 = BroadcastSenderProxyRegistry::instance().getOrCreate(receiver_data1);
    auto sender_2 = BroadcastSenderProxyRegistry::instance().getOrCreate(receiver_data2);
    sender_1->accept(getContext().context, header);
    sender_2->accept(getContext().context, header);
    sender_1->send(origin_chunk.clone());
    sender_2->send(origin_chunk.clone());

    thread_receiver1.join();
    thread_receiver2.join();
}

TEST_F(ExchangeRemoteTest, SerDserChunk)
{
    // ser
    auto origin_chunk = createUInt8Chunk(1000, 1, 7);
    auto header = getHeader(1);
    auto chunk_info = std::make_shared<AggregatedChunkInfo>();
    chunk_info->is_overflows = true;
    chunk_info->bucket_num = 99;
    origin_chunk.setChunkInfo(chunk_info);
    WriteBufferFromBrpcBuf out;
    NativeChunkOutputStream block_out(out, ClickHouseRevision::getVersionRevision(), header, false);
    block_out.write(origin_chunk);
    auto send_buf = out.getFinishedBuf();

    // dser
    ReadBufferFromBrpcBuf read_buffer(send_buf);
    NativeChunkInputStream chunk_in(read_buffer, header);
    Chunk chunk = chunk_in.readImpl();
    EXPECT_EQ(chunk.getNumRows(), 1000);
    const auto dser_chunk_info = std::dynamic_pointer_cast<const AggregatedChunkInfo>(chunk.getChunkInfo());
    EXPECT_TRUE(dser_chunk_info);
    EXPECT_EQ(dser_chunk_info->is_overflows, true);
    EXPECT_EQ(dser_chunk_info->bucket_num, 99);
    auto col = chunk.getColumns().at(0);
    EXPECT_EQ(col->getUInt(1), 7);
}

void sender_thread(BroadcastSenderProxyPtr sender, Chunk chunk)
{
    BroadcastStatus status = sender->send(std::move(chunk));
    ASSERT_TRUE(status.code == BroadcastStatusCode::RUNNING);
}

TEST_F(ExchangeRemoteTest, RemoteNormalTest)
{
    ExchangeOptions exchange_options{.exhcange_timeout_ms = 1000};
    auto header = getHeader(1);
    auto data_key = std::make_shared<ExchangeDataKey>("q1", 1, 1, 1, "localhost:6666");

    Chunk chunk = createUInt8Chunk(10, 1, 7);
    auto total_bytes = chunk.bytes();

    auto sender = BroadcastSenderProxyRegistry::instance().getOrCreate(data_key);
    sender->accept(getContext().context, header);
    std::thread thread_sender(sender_thread, sender, std::move(chunk));

    BrpcRemoteBroadcastReceiverShardPtr receiver
        = std::make_shared<BrpcRemoteBroadcastReceiver>(data_key, "127.0.0.1:8001", getContext().context, header, true);
    receiver->registerToSenders(1000);
    auto exchange_source = std::make_shared<ExchangeSource>(std::move(header), receiver, exchange_options);

    QueryPipeline pipeline;
    pipeline.init(Pipe(exchange_source));

    PullingAsyncPipelineExecutor executor(pipeline);
    Chunk pull_chunk;
    ASSERT_TRUE(executor.pull(pull_chunk));
    ASSERT_TRUE(pull_chunk.getNumRows() == 10);
    ASSERT_TRUE(pull_chunk.bytes() == total_bytes);
    try
    {
        /// trigger timeout
        executor.pull(pull_chunk);
        /// rethrow exception
        executor.pull(pull_chunk);
        ASSERT_TRUE(false) << "Should have thrown.";
    }
    catch (DB::Exception & e)
    {
        ASSERT_TRUE(e.displayText().find("timeout") != std::string::npos) << "Expected 'timeout after ms', got: " << e.displayText();
    }

    thread_sender.join();
    executor.cancel();
}

TEST_F(ExchangeRemoteTest, RemoteSenderLimitTest)
{
    ExchangeOptions exchange_options{.exhcange_timeout_ms = 200};
    auto header = getHeader(1);
    auto data_key = std::make_shared<ExchangeDataKey>("q1", 1, 1, 1, "localhost:6666");
    Chunk chunk = createUInt8Chunk(10, 1, 8);
    auto sender = BroadcastSenderProxyRegistry::instance().getOrCreate(data_key);
    sender->accept(getContext().context, header);
    std::vector<std::thread> thread_senders;
    std::vector<Chunk> chunks;
    for (int i = 0; i < 5; i++)
    {
        Chunk clone = chunk.clone();
        chunks.emplace_back(std::move(clone));
        std::thread thread_sender(sender_thread, sender, std::move(chunks[i]));
        thread_senders.push_back(std::move(thread_sender));
    }

    std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    BrpcRemoteBroadcastReceiverShardPtr receiver
        = std::make_shared<BrpcRemoteBroadcastReceiver>(data_key, "127.0.0.1:8001", getContext().context, header, true);
    receiver->registerToSenders(1000);

    auto exchange_source = std::make_shared<ExchangeSource>(std::move(header), receiver, exchange_options);
    QueryPipeline pipeline;

    Pipe pipe;
    pipe.addSource(exchange_source);
    pipe.addTransform(std::make_shared<LimitTransform>(exchange_source->getPort().getHeader(), 1, 0));
    pipeline.init(std::move(pipe));

    PullingAsyncPipelineExecutor executor(pipeline);
    Chunk pull_chunk;
    ASSERT_TRUE(executor.pull(pull_chunk));
    ASSERT_TRUE(pull_chunk.getNumRows() == 1);
    ASSERT_FALSE(executor.pull(pull_chunk) && pull_chunk);
    executor.cancel();
    for (auto & th : thread_senders)
    {
        th.join();
    }
}
