/*
 * Copyright 2023 Bytedance Ltd. and/or its affiliates.
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

#pragma once

#include <memory>
#include <Access/SaslClient.h>
#include <boost/scoped_array.hpp>
#include <thrift/transport/TBufferTransports.h>
#include <thrift/transport/TTransport.h>
#include <thrift/transport/TVirtualTransport.h>

namespace apache::thrift::transport
{
enum NegotiationStatus
{
    TSASL_INVALID = -1,
    TSASL_START = 1,
    TSASL_OK = 2,
    TSASL_BAD = 3,
    TSASL_ERROR = 4,
    TSASL_COMPLETE = 5
};

static const int MECHANISM_NAME_BYTES = 1;
static const int STATUS_BYTES = 1;
static const int PAYLOAD_LENGTH_BYTES = 4;
static const int HEADER_LENGTH = STATUS_BYTES + PAYLOAD_LENGTH_BYTES;
static const int DEFAULT_MEM_BUF_SIZE = 1024 * 1024; // 1MB

// This transport implements the Simple Authentication and Security Layer (SASL) for client.
class TSaslClientTransport : public TVirtualTransport<TSaslClientTransport>
{
public:
    TSaslClientTransport(std::shared_ptr<DB::SaslClient> sasl_client_, std::shared_ptr<TTransport> transport_);

    ~TSaslClientTransport() override;

    bool isOpen() override;

    bool peek() override;

    void open() override;

    void close() override;

    uint32_t read(uint8_t * buf, uint32_t len);

    void write(const uint8_t * buf, uint32_t len);

    void flush() override;

    std::shared_ptr<TTransport> getUnderlyingTransport();

    // return username from sasl connection
    std::string getUsername();

    // only impl Mechanism GSSAPI (KRB5)
    static std::shared_ptr<TTransport>wrapClientTransports(const std::string& hostname, const std::string& service_name, std::shared_ptr<TTransport> raw_transport);
private:
    // Set up the Sasl server state for a connection
    void setupSaslNegotiationState();

    void resetSaslNegotiationState();

    void handleSaslStartMessage();

    //Performs the SASL negotiation
    void doSaslNegotiation();

    uint8_t * receiveSaslMessage(NegotiationStatus * status, uint32_t * length);

    void sendSaslMessage(NegotiationStatus status, const uint8_t * payload, uint32_t length, bool flush = true);

    // store the big endian format int to given buffer
    static void encodeInt(uint32_t x, uint8_t * buf, uint32_t offset) { *(reinterpret_cast<uint32_t *>(buf + offset)) = htonl(x); }

    // load the big endian format int to given buffer
    static uint32_t decodeInt(uint8_t * buf, uint32_t offset) { return ntohl(*(reinterpret_cast<uint32_t *>(buf + offset))); }

    uint32_t readLength();

    void writeLength(uint32_t length);

    /// If mem_buf is filled with bytes that are already read, and has crossed a size
    /// threshold (see implementation for exact value), resize the buffer to a default value.
    void shrinkBuffer();

    std::shared_ptr<DB::SaslClient> sasl_client;
    std::shared_ptr<TTransport> transport;

    // IF true we wrap data in encryption.
    bool should_wrap;
    // buffer for reading and writing
    std::unique_ptr<TMemoryBuffer> mem_buf;
    // Buffer to hold prtocol info
    boost::scoped_array<uint8_t> proto_buf;
};

}
