/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/io/zero_copy_stream_impl_lite.h>
#include <google/protobuf/message_lite.h>
#include <google/protobuf/wire_format_lite.h>

#include "drill/common.hpp"
#include "rpcMessage.hpp"

namespace Drill{
namespace rpc {


namespace {
using google::protobuf::internal::WireFormatLite;
using google::protobuf::io::CodedOutputStream;
using exec::rpc::CompleteRpcMessage;

static const uint32_t HEADER_TAG = WireFormatLite::MakeTag(CompleteRpcMessage::kHeaderFieldNumber, WireFormatLite::WIRETYPE_LENGTH_DELIMITED);
static const uint32_t PROTOBUF_BODY_TAG = WireFormatLite::MakeTag(CompleteRpcMessage::kProtobufBodyFieldNumber, WireFormatLite::WIRETYPE_LENGTH_DELIMITED);
static const uint32_t RAW_BODY_TAG = WireFormatLite::MakeTag(CompleteRpcMessage::kRawBodyFieldNumber, WireFormatLite::WIRETYPE_LENGTH_DELIMITED);
static const uint32_t HEADER_TAG_LENGTH = CodedOutputStream::VarintSize32(HEADER_TAG);
static const uint32_t PROTOBUF_BODY_TAG_LENGTH = CodedOutputStream::VarintSize32(PROTOBUF_BODY_TAG);
}

std::size_t lengthDecode(const uint8_t* buf, uint32_t& length) {
    using google::protobuf::io::CodedInputStream;
    using google::protobuf::io::CodedOutputStream;

    // read the frame to get the length of the message and then

    CodedInputStream cis(buf, LEN_PREFIX_BUFLEN); // read LEN_PREFIX_BUFLEN bytes at most

    int startPos(cis.CurrentPosition()); // for debugging
    if (!cis.ReadVarint32(&length)) {
    	return -1;
    }

    #ifdef CODER_DEBUG
    std::cerr << "length = " << length << std::endl;
    #endif

    int endPos(cis.CurrentPosition());

    assert((endPos-startPos) == CodedOutputStream::VarintSize32(length));
    return (endPos-startPos);
}

// TODO: error handling
//
// - assume that the entire message is in the buffer and the buffer is constrained to this message
// - easy to handle with raw array in C++
bool decode(const uint8_t* buf, int length, InBoundRpcMessage& msg) {
    using google::protobuf::io::CodedInputStream;

    CodedInputStream cis(buf, length);

    int startPos(cis.CurrentPosition()); // for debugging

    CodedInputStream::Limit len_limit(cis.PushLimit(length));

    uint32_t header_length(0);

    if (!cis.ExpectTag(HEADER_TAG)) {
    	return false;
    }

    if (!cis.ReadVarint32(&header_length)) {
    	return false;
    }

    #ifdef CODER_DEBUG
    std::cerr << "Reading header length " << header_length << ", post read index " << cis.CurrentPosition() << std::endl;
    #endif

    exec::rpc::RpcHeader header;
    CodedInputStream::Limit header_limit(cis.PushLimit(header_length));

    if (!header.ParseFromCodedStream(&cis)) {
    	return false;
    }
    cis.PopLimit(header_limit);

    msg.m_has_mode = header.has_mode();
    msg.m_mode = header.mode();
    msg.m_coord_id = header.coordination_id();
    msg.m_has_rpc_type = header.has_rpc_type();
    msg.m_rpc_type = header.rpc_type();

    // read the protobuf body into a buffer.
    if (!cis.ExpectTag(PROTOBUF_BODY_TAG)) {
    	return false;
    }

    uint32_t pbody_length(0);
    if (!cis.ReadVarint32(&pbody_length)) {
    	return false;
    }

    #ifdef CODER_DEBUG
    std::cerr << "Reading protobuf body length " << pbody_length << ", post read index " << cis.CurrentPosition() << std::endl;
    #endif

    msg.m_pbody.resize(pbody_length);
    if (!cis.ReadRaw(msg.m_pbody.data(), pbody_length)) {
    	return false;
    }

    // read the data body.
    if (cis.BytesUntilLimit() > 0 ) {
		#ifdef CODER_DEBUG
			std::cerr << "Reading raw body, buffer has "<< std::cis->BytesUntilLimit() << " bytes available, current possion "<< cis.CurrentPosition()  << endl;
		#endif
        if (!cis.ExpectTag(RAW_BODY_TAG)) {
        	return false;
        }

        uint32_t dbody_length = 0;
        if (!cis.ReadVarint32(&dbody_length)) {
        	return false;
        }

        if(cis.BytesUntilLimit() != dbody_length) {
			#ifdef CODER_DEBUG
					cerr << "Expected to receive a raw body of " << dbody_length << " bytes but received a buffer with " <<cis->BytesUntilLimit() << " bytes." << endl;
			#endif
			return false;
        }

        int currPos(cis.CurrentPosition());
        int size;
        cis.GetDirectBufferPointer(const_cast<const void**>(reinterpret_cast<void**>(&msg.m_dbody)), &size);
        cis.Skip(size);

        assert(dbody_length == size);
        assert(msg.m_dbody==buf+currPos);

        // Enforce assertion due to unexpected crashes during network error.
        if (!((dbody_length == size) && (msg.m_dbody == (buf + currPos)))) {
            return false;
        }

		#ifdef CODER_DEBUG
			cerr << "Read raw body of " << dbody_length << " bytes" << endl;
		#endif
    } else {
		#ifdef CODER_DEBUG
			cerr << "No need to read raw body, no readable bytes left." << endl;
		#endif
    }
    cis.PopLimit(len_limit);


    // return the rpc message.
    // move the reader index forward so the next rpc call won't try to work with it.
    // buffer.skipBytes(dBodyLength);
    // messageCounter.incrementAndGet();
    #ifdef CODER_DEBUG
    std::cerr << "Inbound Rpc Message Decoded " << msg << std::endl;
    #endif

    int endPos = cis.CurrentPosition();
    assert((endPos-startPos) == length);

    // Enforce assertion due to unexpected crashes during network error.
    if ((endPos - startPos) != length) {
        return false;
        
    }
    return true;
}


bool encode(DataBuf& buf, const OutBoundRpcMessage& msg) {
    using exec::rpc::RpcHeader;
    using google::protobuf::io::CodedOutputStream;
    // Todo:
    //
    // - let a context manager to allocate a buffer `ByteBuf buf = ctx.alloc().buffer();`
    // - builder pattern
    //
    #ifdef CODER_DEBUG
    std::cerr << "Encoding outbound message " << msg << std::endl;
    #endif

    RpcHeader header;
    header.set_mode(msg.m_mode);
    header.set_coordination_id(msg.m_coord_id);
    header.set_rpc_type(msg.m_rpc_type);

    // calcute the length of the message
    long header_length = header.ByteSizeLong();
    long proto_body_length = msg.m_pbody->ByteSizeLong();
    long full_length = HEADER_TAG_LENGTH + CodedOutputStream::VarintSize32(header_length) + header_length + \
                      PROTOBUF_BODY_TAG_LENGTH + CodedOutputStream::VarintSize32(proto_body_length) + proto_body_length;

    /*
       if(raw_body_length > 0) {
       full_length += (RAW_BODY_TAG_LENGTH + getRawVarintSize(raw_body_length) + raw_body_length);
       }
       */

    buf.resize(full_length + CodedOutputStream::VarintSize32(full_length));

    uint8_t* data = buf.data();

    #ifdef CODER_DEBUG
    std::cerr << "Writing full length " << full_length << std::endl;
    #endif

    data = CodedOutputStream::WriteVarint32ToArray(full_length, data);

    #ifdef CODER_DEBUG
    std::cerr << "Writing header length " << header_length << std::endl;
    #endif

    data = CodedOutputStream::WriteVarint32ToArray(HEADER_TAG, data);
    data = CodedOutputStream::WriteVarint32ToArray(header_length, data);

    data = header.SerializeWithCachedSizesToArray(data);

    // write protobuf body length and body
    #ifdef CODER_DEBUG
    std::cerr << "Writing protobuf body length " << proto_body_length << std::endl;
    #endif

    data = CodedOutputStream::WriteVarint32ToArray(PROTOBUF_BODY_TAG, data);
    data = CodedOutputStream::WriteVarint32ToArray(proto_body_length, data);
    msg.m_pbody->SerializeWithCachedSizesToArray(data);

    // Done! no read to write data body for client
    return true;
}
} // namespace rpc
} // namespace Drill
