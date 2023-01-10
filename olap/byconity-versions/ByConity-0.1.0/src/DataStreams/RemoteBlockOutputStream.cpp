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

#include <DataStreams/RemoteBlockOutputStream.h>

#include <Client/Connection.h>
#include <common/logger_useful.h>

#include <Common/NetException.h>
#include <Common/CurrentThread.h>
#include <Interpreters/InternalTextLogsQueue.h>
#include <IO/ConnectionTimeouts.h>
#include <Parsers/ASTInsertQuery.h>
#include <Parsers/ASTExpressionList.h>
#include <Parsers/ASTIdentifier.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int UNEXPECTED_PACKET_FROM_SERVER;
}


RemoteBlockOutputStream::RemoteBlockOutputStream(Connection & connection_,
                                                 const ConnectionTimeouts & timeouts,
                                                 const String & query_,
                                                 const Settings & settings_,
                                                 const ClientInfo & client_info_,
                                                 ContextPtr context_)
    : connection(connection_), query(query_), context(context_)
{
    ClientInfo modified_client_info = client_info_;
    modified_client_info.query_kind = ClientInfo::QueryKind::SECONDARY_QUERY;
    if (CurrentThread::isInitialized())
    {
        modified_client_info.client_trace_context
            = CurrentThread::get().thread_trace_context;
    }

    /** Send query and receive "header", that describes table structure.
      * Header is needed to know, what structure is required for blocks to be passed to 'write' method.
      */
    connection.sendQuery(timeouts, query, "", QueryProcessingStage::Complete, &settings_, &modified_client_info);

    while (true)
    {
        Packet packet = connection.receivePacket();

        if (Protocol::Server::Data == packet.type)
        {
            header = packet.block;
            break;
        }
        else if (Protocol::Server::Exception == packet.type)
        {
            packet.exception->rethrow();
            break;
        }
        else if (Protocol::Server::Log == packet.type)
        {
            /// Pass logs from remote server to client
            if (auto log_queue = CurrentThread::getInternalTextLogsQueue())
                log_queue->pushBlock(std::move(packet.block));
        }
        else if (Protocol::Server::TableColumns == packet.type)
        {
            /// Server could attach ColumnsDescription in front of stream for column defaults. There's no need to pass it through cause
            /// client's already got this information for remote table. Ignore.
        }
        else if (Protocol::Server::QueryMetrics == packet.type)
        {
            parseQueryWorkerMetrics(packet.query_worker_metric_elements);
        }
        else
            throw NetException("Unexpected packet from server (expected Data or Exception, got "
                + String(Protocol::Server::toString(packet.type)) + ")", ErrorCodes::UNEXPECTED_PACKET_FROM_SERVER);
    }
}


void RemoteBlockOutputStream::write(const Block & block)
{
    if (header)
        assertBlocksHaveEqualStructure(block, header, "RemoteBlockOutputStream");

    try
    {
        connection.sendData(block);
    }
    catch (const NetException &)
    {
        /// Try to get more detailed exception from server
        auto packet_type = connection.checkPacket();
        if (packet_type && *packet_type == Protocol::Server::Exception)
        {
            Packet packet = connection.receivePacket();
            packet.exception->rethrow();
        }

        throw;
    }
}


void RemoteBlockOutputStream::writePrepared(ReadBuffer & input, size_t size)
{
    /// We cannot use 'header'. Input must contain block with proper structure.
    connection.sendPreparedData(input, size);
}


void RemoteBlockOutputStream::writeSuffix()
{
    /// Empty block means end of data.
    connection.sendData(Block());

    /// Wait for EndOfStream or Exception packet, skip Log packets.
    while (true)
    {
        Packet packet = connection.receivePacket();

        if (Protocol::Server::EndOfStream == packet.type)
            break;
        else if (Protocol::Server::Exception == packet.type)
            packet.exception->rethrow();
        else if (Protocol::Server::Log == packet.type)
        {
            // Do nothing
        }
        else if (Protocol::Server::QueryMetrics == packet.type)
        {
            parseQueryWorkerMetrics(packet.query_worker_metric_elements);
        }
        else
            throw NetException("Unexpected packet from server (expected EndOfStream or Exception, got "
            + String(Protocol::Server::toString(packet.type)) + ")", ErrorCodes::UNEXPECTED_PACKET_FROM_SERVER);
    }

    finished = true;
}

RemoteBlockOutputStream::~RemoteBlockOutputStream()
{
    /// If interrupted in the middle of the loop of communication with the server, then interrupt the connection,
    ///  to not leave the connection in unsynchronized state.
    if (!finished)
    {
        try
        {
            connection.disconnect();
        }
        catch (...)
        {
            tryLogCurrentException(__PRETTY_FUNCTION__);
        }
    }
}

void RemoteBlockOutputStream::parseQueryWorkerMetrics(const QueryWorkerMetricElements & elements)
{
    for (const auto & element : elements)
    {
        if (context->getServerType() == ServerType::cnch_server)  /// For cnch server, directly push elements to the buffer
            context->getQueryContext()->insertQueryWorkerMetricsElement(*element);
        else if (context->getServerType() == ServerType::cnch_worker)  /// For cnch aggre worker, store the elements and forward them to cnch server
            context->getQueryContext()->addQueryWorkerMetricElements(std::move(element));
    }
}

}
