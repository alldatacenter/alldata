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

#include <memory>
#include <sstream>
#include <string>
#include <Access/SaslClient.h>
#include <Storages/Hive/TSaslClientTransport.h>
#include <thrift/transport/TTransport.h>
#include <thrift/transport/TVirtualTransport.h>
#include <Common/Exception.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int SASL_ERROR;
}
}

namespace apache::thrift::transport
{
// Wallpaper transport with sasl
TSaslClientTransport::TSaslClientTransport(std::shared_ptr<DB::SaslClient> sasl_client_, std::shared_ptr<TTransport> transport_)
    : sasl_client(sasl_client_), transport(transport_), should_wrap(false), mem_buf(new TMemoryBuffer())
{
}

TSaslClientTransport::~TSaslClientTransport()
{
    resetSaslNegotiationState();
}

bool TSaslClientTransport::isOpen()
{
    return transport->isOpen();
}

bool TSaslClientTransport::peek()
{
    return transport->peek();
}

void TSaslClientTransport::open()
{
    if (!transport->isOpen())
    {
        transport->open();
    }

    // Start the SASL negotiation protocol.
    doSaslNegotiation();
}

void TSaslClientTransport::close()
{
    transport->close();
}

uint32_t TSaslClientTransport::readLength()
{
    uint8_t len_buf[PAYLOAD_LENGTH_BYTES];

    transport->readAll(len_buf, PAYLOAD_LENGTH_BYTES);
    int32_t len = decodeInt(len_buf, 0);
    if (len < 0)
    {
        throw TTransportException("Frame size has negative value");
    }
    return static_cast<uint32_t>(len);
}

uint32_t TSaslClientTransport::read(uint8_t * buf, uint32_t len)
{
    uint32_t read_bytes = mem_buf->read(buf, len);

    if (read_bytes > 0)
    {
        shrinkBuffer();
        return read_bytes;
    }

    // if there's not enough data in cache, read from underlying transport
    uint32_t data_length = readLength();

    // Fast path
    if (len == data_length && !should_wrap)
    {
        transport->readAll(buf, len);
        return len;
    }

    uint8_t * tmp_buf = new uint8_t[data_length];
    transport->readAll(tmp_buf, data_length);
    if (should_wrap)
    {
        tmp_buf = sasl_client->unwrap(tmp_buf, 0, data_length, &data_length);
    }

    // We will consume all the data, no need to put it in the memory buffer.
    if (len == data_length)
    {
        memcpy(buf, tmp_buf, len);
        delete[] tmp_buf;
        return len;
    }

    mem_buf->write(tmp_buf, data_length);
    mem_buf->flush();
    delete[] tmp_buf;

    uint32_t ret = mem_buf->read(buf, len);
    shrinkBuffer();
    return ret;
}

void TSaslClientTransport::writeLength(uint32_t length)
{
    uint8_t len_buf[PAYLOAD_LENGTH_BYTES];

    encodeInt(length, len_buf, 0);
    transport->write(len_buf, PAYLOAD_LENGTH_BYTES);
}

void TSaslClientTransport::write(const uint8_t * buf, uint32_t len)
{
    const uint8_t * new_buf;

    if (should_wrap)
    {
        new_buf = sasl_client->wrap(const_cast<uint8_t *>(buf), 0, len, &len);
    }
    else
    {
        new_buf = buf;
    }
    writeLength(len);
    transport->write(new_buf, len);
}

void TSaslClientTransport::flush()
{
    transport->flush();
}

std::shared_ptr<TTransport> TSaslClientTransport::getUnderlyingTransport()
{
    return transport;
}

std::string TSaslClientTransport::getUsername()
{
    return sasl_client->getUsername();
}

void TSaslClientTransport::setupSaslNegotiationState()
{
    if (!sasl_client)
        throw DB::Exception("setupSaslNegotiationState() failed. SASL Client not created", DB::ErrorCodes::SASL_ERROR);

    sasl_client->setupSaslContext();
}

void TSaslClientTransport::resetSaslNegotiationState()
{
    if (!sasl_client)
        throw DB::Exception("setupSaslNegotiationState() failed. SASL Client not created", DB::ErrorCodes::SASL_ERROR);

    sasl_client->resetSaslContext();
}

void TSaslClientTransport::handleSaslStartMessage()
{
    uint32_t res_length = 0;
    uint8_t dummy = 0;
    uint8_t * initial_response = &dummy;

    // Get data to send to the server if the client goes first
    if (sasl_client->hasInitialResponse())
    {
        initial_response = sasl_client->evaluateChallengeOrResponse(nullptr, 0, &res_length);
    }

    //These two calls comprise a single message in the thrift-sasl protocol
    sendSaslMessage(
        TSASL_START,
        const_cast<uint8_t *>(reinterpret_cast<const uint8_t *>(sasl_client->getMechanismName().c_str())),
        sasl_client->getMechanismName().length(),
        false);
    sendSaslMessage(TSASL_OK, initial_response, res_length);

    transport->flush();
}

void TSaslClientTransport::doSaslNegotiation()
{
    NegotiationStatus status = TSASL_INVALID;
    uint32_t res_length;

    try
    {
        // Setup Sasl context.
        setupSaslNegotiationState();

        // Initiate SASL message.
        handleSaslStartMessage();

        // SASL connection handshake
        while (!sasl_client->isComplete())
        {
            uint8_t * message = receiveSaslMessage(&status, &res_length);
            if (status == TSASL_COMPLETE)
            {
                if (!sasl_client->isComplete())
                {
                    // Server sent COMPLETE out of order.
                    throw TTransportException("Received COMPLETE but no handshake occurred");
                }
                break; // handshake complete
            }
            else if (status != TSASL_OK)
            {
                std::stringstream ss;
                ss << "Expected COMPLETE or OK, got " << status;
                throw TTransportException(ss.str());
            }
            
            uint32_t challenge_length;
            uint8_t * challenge = sasl_client->evaluateChallengeOrResponse(message, res_length, &challenge_length);
            sendSaslMessage(sasl_client->isComplete() ? TSASL_COMPLETE : TSASL_OK, challenge, challenge_length);
        }

        // If the server isn't complete yet, we need to wait for its response.
        // This will occur with ANONYMOUS auth, for example, where we send an
        // initial response and are immediately complete.
        if (status == TSASL_INVALID || status == TSASL_OK)
        {
            receiveSaslMessage(&status, &res_length);
            if (status != TSASL_COMPLETE)
            {
                std::stringstream ss;
                ss << "Expected COMPLETE or OK, got " << status;
                throw TTransportException(ss.str());
            }
        }
    }
    catch (const TException & e)
    {
        // If we hit an exception, that means the Sasl negotiation failed. We explicitly
        // reset the negotiation state here since the caller may retry an open() which would
        // start a new connection negotiation.
        resetSaslNegotiationState();
        throw e;
    }
}

uint8_t * TSaslClientTransport::receiveSaslMessage(NegotiationStatus * status, uint32_t * length)
{
    uint8_t message_header[HEADER_LENGTH];

    // read header
    transport->readAll(message_header, HEADER_LENGTH);

    // get payload status
    *status = static_cast<NegotiationStatus>(message_header[0]);
    if ((*status < TSASL_START) || (*status > TSASL_COMPLETE))
    {
        throw TTransportException("invalid sasl status");
    }
    else if (*status == TSASL_BAD || *status == TSASL_ERROR)
    {
        throw TTransportException("sasl Peer indicated failure: ");
    }

    // get the length
    *length = decodeInt(message_header, STATUS_BYTES);

    // get payload
    proto_buf.reset(new uint8_t[*length]);
    transport->readAll(proto_buf.get(), *length);

    return proto_buf.get();
}

void TSaslClientTransport::sendSaslMessage(NegotiationStatus status, const uint8_t * payload, uint32_t length, bool flush)
{
    uint8_t message_header[STATUS_BYTES + PAYLOAD_LENGTH_BYTES];
    uint8_t dummy = 0;
    if (payload == nullptr)
    {
        payload = &dummy;
    }
    message_header[0] = static_cast<uint8_t>(status);
    encodeInt(length, message_header, STATUS_BYTES);
    transport->write(message_header, HEADER_LENGTH);
    transport->write(payload, length);
    if (flush)
        transport->flush();
}

void TSaslClientTransport::shrinkBuffer()
{
    // readEnd() returns the number of bytes already read, i.e. the number of 'junk' bytes
    // taking up space at the front of the memory buffer.
    uint32_t read_end = mem_buf->readEnd();

    // If the size of the junk space at the beginning of the buffer is too large, and
    // there's no data left in the buffer to read (number of bytes read == number of bytes
    // written), then shrink the buffer back to the default. We don't want to do this on
    // every read that exhausts the buffer, since the layer above often reads in small
    // chunks, which is why we only resize if there's too much junk. The write and read
    // pointers will eventually catch up after every RPC, so we will always take this path
    // eventually once the buffer becomes sufficiently full.
    //
    // readEnd() may reset the write / read pointers (but only once if there's no
    // intervening read or write between calls), so needs to be called a second time to
    // get their current position.
    if (read_end > mem_buf->defaultSize && mem_buf->writeEnd() == mem_buf->readEnd())
    {
        mem_buf->resetBuffer(mem_buf->defaultSize);
    }
}

std::shared_ptr<TTransport> TSaslClientTransport::wrapClientTransports(
    const std::string & hostname, const std::string & service_name, std::shared_ptr<TTransport> raw_transport)
{
    DB::SaslClient::setupSaslClientWithKerberos();
    std::shared_ptr<DB::SaslClient> sasl_client = std::make_shared<DB::SaslClient>(service_name, hostname);
    return std::make_unique<TSaslClientTransport>(sasl_client, raw_transport);
}

}
