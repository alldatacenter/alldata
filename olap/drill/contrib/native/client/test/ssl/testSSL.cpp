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
#include <fstream>
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <boost/algorithm/string.hpp>
#include <boost/asio.hpp>
#include <boost/assign.hpp>
#include <boost/bind.hpp>
#include "drill/drillc.hpp"
#include "drill/drillError.hpp"
#include "clientlib/channel.hpp"
#include "clientlib/drillClientImpl.hpp"
#include "clientlib/errmsgs.hpp"
#include "clientlib/logger.hpp"
#include "clientlib/rpcMessage.hpp"
#include "clientlib/utils.hpp"
#include "protobuf/GeneralRPC.pb.h"
#include "protobuf/UserBitShared.pb.h"

namespace Drill {

class DrillTestClient {

    public:

    DrillTestClient(Channel* pChannel):
    m_handshakeStatus(exec::user::SUCCESS),
    m_wbuf(MAX_SOCK_RD_BUFSIZE),
    m_rbuf(0){
            m_pChannel=pChannel;
            m_pError=NULL;
            m_coordinationId=Utils::s_randomNumber()%1729+1;
        }

        connectionStatus_t recvHandshake(){
            if(m_rbuf==NULL){
                m_rbuf = Utils::allocateBuffer(MAX_SOCK_RD_BUFSIZE);
            }

            m_pChannel->getIOService().reset();

            m_pChannel->getSocketStream().asyncRead(
                    boost::asio::buffer(m_rbuf, LEN_PREFIX_BUFLEN),
                    boost::bind(
                        &DrillTestClient::handleHandshake,
                        this,
                        m_rbuf,
                        boost::asio::placeholders::error,
                        boost::asio::placeholders::bytes_transferred)
                    );
            DRILL_MT_LOG(DRILL_LOG(LOG_DEBUG) << "DrillClientImpl::recvHandshake: async read waiting for server handshake response.\n";)
            m_pChannel->getIOService().run();
            if(m_rbuf!=NULL){
                Utils::freeBuffer(m_rbuf, MAX_SOCK_RD_BUFSIZE); m_rbuf=NULL;
            }

            if (m_pError != NULL) {
                DRILL_MT_LOG(DRILL_LOG(LOG_ERROR) << "DrillClientImpl::recvHandshake: failed to complete handshake with server."
                        << m_pError->msg << "\n";)
                    return static_cast<connectionStatus_t>(m_pError->status);
            }

            return CONN_SUCCESS;
        }

        void doReadFromSocket(ByteBuf_t inBuf, size_t bytesToRead, boost::system::error_code& errorCode) {
            // Check if bytesToRead is zero
            if(0 == bytesToRead) {
                return;
            }

            // Read all the bytes. In case when all the bytes were not read the proper
            // errorCode will be set.
            while(1){
                size_t dataBytesRead = m_pChannel->getSocketStream().readSome(boost::asio::buffer(inBuf, bytesToRead), errorCode);
                // Update the state
                bytesToRead -= dataBytesRead;
                inBuf += dataBytesRead;

                // Check if errorCode is EINTR then just retry otherwise break from loop
                if(EINTR != errorCode.value()) break;

                // Check if all the data is read then break from loop
                if(0 == bytesToRead) break;
            }
        }

        void handleHandshake(ByteBuf_t inBuf,
                const boost::system::error_code& err,
                size_t bytes_transferred) {
            boost::system::error_code error=err;
            DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "Deadline timer cancelled." << std::endl;)
                if(!error){
                    rpc::InBoundRpcMessage msg;
                    uint32_t length = 0;
                    std::size_t bytes_read = rpc::lengthDecode(m_rbuf, length);
                    if(length>0){
                        const size_t leftover = LEN_PREFIX_BUFLEN - bytes_read;
                        const ByteBuf_t b = m_rbuf + LEN_PREFIX_BUFLEN;
                        const size_t bytesToRead=length - leftover;
                        doReadFromSocket(b, bytesToRead, error);

                        // Check if any error happen while reading the message bytes. If yes then return before decoding the Msg
                        if(error) {
                            DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "DrillClientImpl::handleHandshake: ERR_CONN_RDFAIL. "
                                    << " Failed to read entire handshake message. with error: "
                                    << error.message().c_str() << "\n";)
                                handleConnError(CONN_FAILURE, getMessage(ERR_CONN_RDFAIL, "Failed to read entire handshake message"));
                            return;
                        }

                        // Decode the bytes into a valid RPC Message
                        if (!decode(m_rbuf+bytes_read, length, msg)) {
                            DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "DrillClientImpl::handleHandshake: ERR_CONN_RDFAIL. Cannot decode handshake.\n";)
                                handleConnError(CONN_FAILURE, getMessage(ERR_CONN_RDFAIL, "Cannot decode handshake"));
                            return;
                        }
                    }else{
                        DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "DrillClientImpl::handleHandshake: ERR_CONN_RDFAIL. No handshake.\n";)
                            handleConnError(CONN_FAILURE, getMessage(ERR_CONN_RDFAIL, "No handshake"));
                        return;
                    }
                    exec::user::BitToUserHandshake b2u;
                    b2u.ParseFromArray(msg.m_pbody.data(), msg.m_pbody.size());
                    this->m_handshakeErrorId=b2u.errorid();
                    this->m_handshakeErrorMsg=b2u.errormessage();
                }else{
                    // boost error
                    if(error==boost::asio::error::eof){ // Server broke off the connection
                        handleConnError(CONN_HANDSHAKE_FAILED, getMessage(ERR_CONN_NOHSHAKE, DRILL_RPC_VERSION));
                    }else{
                        handleConnError(CONN_FAILURE, getMessage(ERR_CONN_RDFAIL, error.message().c_str()));
                    }
                    return;
                }
            return;
        }

        connectionStatus_t handleConnError(connectionStatus_t status, const std::string& msg){
            DrillClientError* pErr = new DrillClientError(status, DrillClientError::CONN_ERROR_START+status, msg);
            if(m_pError!=NULL){ delete m_pError; m_pError=NULL;}
            m_pError=pErr;
            return status;
        }

        connectionStatus_t sendSyncCommon(rpc::OutBoundRpcMessage& msg) {
            encode(m_wbuf, msg);
            boost::system::error_code ec;
            doWriteToSocket(reinterpret_cast<char*>(m_wbuf.data()), m_wbuf.size(), ec);

            if(!ec) {
                return CONN_SUCCESS;
            } else {
                return handleConnError(CONN_FAILURE, getMessage(ERR_CONN_WFAIL, ec.message().c_str()));
            }
        }

        void doWriteToSocket(const char* dataPtr, size_t bytesToWrite,
                boost::system::error_code& errorCode) {
            if(0 == bytesToWrite) {
                return;
            }

            // Write all the bytes to socket. In case of error when all bytes are not successfully written
            // proper errorCode will be set.
            while(1) {
                size_t bytesWritten = m_pChannel->getSocketStream().writeSome(boost::asio::buffer(dataPtr, bytesToWrite), errorCode);
                // Update the state
                bytesToWrite -= bytesWritten;
                dataPtr += bytesWritten;

                if(EINTR != errorCode.value()) break;

                // Check if all the data is written then break from loop
                if(0 == bytesToWrite) break;
            }
        }

        connectionStatus_t validateHandshake(DrillUserProperties* properties){

            DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "validateHandShake\n";)

                exec::user::UserToBitHandshake u2b;
            u2b.set_channel(exec::shared::USER);
            u2b.set_rpc_version(DRILL_RPC_VERSION);
            u2b.set_support_listening(true);
            u2b.set_support_timeout(DrillClientConfig::getHeartbeatFrequency() > 0);
            u2b.set_sasl_support(exec::user::SASL_PRIVACY);

            // Adding version info
            exec::user::RpcEndpointInfos* infos = u2b.mutable_client_infos();
            infos->set_name(DrillClientConfig::getClientName());
            infos->set_application(DrillClientConfig::getApplicationName());
            infos->set_version(DRILL_VERSION_STRING);
            infos->set_majorversion(DRILL_VERSION_MAJOR);
            infos->set_minorversion(DRILL_VERSION_MINOR);
            infos->set_patchversion(DRILL_VERSION_PATCH);

            if(properties != NULL && properties->size()>0){
                std::string username;
                std::string err;
                if(!properties->validate(err)){
                    DRILL_MT_LOG(DRILL_LOG(LOG_INFO) << "Invalid user input:" << err << std::endl;)
                }
                exec::user::UserProperties* userProperties = u2b.mutable_properties();

                std::map<char,int>::iterator it;
                for (std::map<std::string,std::string>::const_iterator propIter=properties->begin(); propIter!=properties->end(); ++propIter){
                    std::string currKey=propIter->first;
                    std::string currVal=propIter->second;
                    std::map<std::string,uint32_t>::const_iterator it=DrillUserProperties::USER_PROPERTIES.find(currKey);
                    if(it==DrillUserProperties::USER_PROPERTIES.end()){
                        DRILL_MT_LOG(DRILL_LOG(LOG_INFO) << "Connection property ("<< currKey
                                << ") is unknown" << std::endl;)
                            exec::user::Property* connProp = userProperties->add_properties();
                        connProp->set_key(currKey);
                        connProp->set_value(currVal);
                        continue;
                    }
                    if(IS_BITSET((*it).second,USERPROP_FLAGS_SERVERPROP)){
                        exec::user::Property* connProp = userProperties->add_properties();
                        connProp->set_key(currKey);
                        connProp->set_value(currVal);
                        //Username(but not the password) also needs to be set in UserCredentials
                        if(IS_BITSET((*it).second,USERPROP_FLAGS_USERNAME)){
                            exec::shared::UserCredentials* creds = u2b.mutable_credentials();
                            username=currVal;
                            creds->set_user_name(username);
                            //u2b.set_credentials(&creds);
                        }
                        if(IS_BITSET((*it).second,USERPROP_FLAGS_PASSWORD)){
                            DRILL_MT_LOG(DRILL_LOG(LOG_INFO) <<  currKey << ": ********** " << std::endl;)
                        }else{
                            DRILL_MT_LOG(DRILL_LOG(LOG_INFO) << currKey << ":" << currVal << std::endl;)
                        }
                    }// Server properties
                }
            }

            {
                boost::lock_guard<boost::mutex> lock(this->m_dcMutex);
                uint64_t coordId = ++m_coordinationId;

                rpc::OutBoundRpcMessage out_msg(exec::rpc::REQUEST, exec::user::HANDSHAKE, coordId, &u2b);
                sendSyncCommon(out_msg);
                DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "Sent handshake request message. Coordination id: " << coordId << "\n";)
            }

            connectionStatus_t ret = recvHandshake();
            if(ret!=CONN_SUCCESS){
                return ret;
            }

            switch(this->m_handshakeStatus) {
                case exec::user::SUCCESS:
                    // reset io_service after handshake is validated before running queries
                    m_pChannel->getIOService().reset();
                    return CONN_SUCCESS;
                case exec::user::RPC_VERSION_MISMATCH:
                    DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "Invalid rpc version.  Expected "
                            << DRILL_RPC_VERSION << ", actual "<< 0 << "." << std::endl;)
                        return handleConnError(CONN_BAD_RPC_VER, getMessage(ERR_CONN_BAD_RPC_VER, DRILL_RPC_VERSION,
                                    0,
                                    this->m_handshakeErrorId.c_str(),
                                    this->m_handshakeErrorMsg.c_str()));
                case exec::user::AUTH_FAILED:
                    DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "Authentication failed." << std::endl;)
                        return handleConnError(CONN_AUTH_FAILED, getMessage(ERR_CONN_AUTHFAIL,
                                    this->m_handshakeErrorId.c_str(),
                                    this->m_handshakeErrorMsg.c_str()));
                case exec::user::UNKNOWN_FAILURE:
                    DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "Unknown error during handshake." << std::endl;)
                        return handleConnError(CONN_HANDSHAKE_FAILED, getMessage(ERR_CONN_UNKNOWN_ERR,
                                    this->m_handshakeErrorId.c_str(),
                                    this->m_handshakeErrorMsg.c_str()));
                case exec::user::AUTH_REQUIRED:
                    DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "Server requires SASL authentication." << std::endl;)
                    return handleConnError(CONN_HANDSHAKE_FAILED, getMessage(ERR_CONN_UNKNOWN_ERR,
                                                                             this->m_handshakeErrorId.c_str(),
                                                                             this->m_handshakeErrorMsg.c_str()));
                default:
                    DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "Unknown return status." << std::endl;)
                        return handleConnError(CONN_HANDSHAKE_FAILED, getMessage(ERR_CONN_UNKNOWN_ERR,
                                    this->m_handshakeErrorId.c_str(),
                                    this->m_handshakeErrorMsg.c_str()));
            }
        }

        DrillClientError* m_pError;
    private:
        Channel* m_pChannel;
        int32_t m_coordinationId;
        std::string m_handshakeErrorId;
        std::string m_handshakeErrorMsg;
        exec::user::HandshakeStatus m_handshakeStatus;
        DataBuf m_wbuf;
        ByteBuf_t m_rbuf;
        boost::mutex m_dcMutex;
    



};

} // namespace Drill

using namespace Drill;

int main(int argc, char* argv[]){
    Channel *pChannel = NULL;
    ChannelContext *pChannelContext = NULL;
    std::string connectStr = "zk=localhost:2181/drill/drillbits1";
    //std::string connectStr = "drillbit=localhost:31090";
    channelType_t type;
    boost::asio::io_service ioService;

    bool isSSL = argc==2 && !(strcmp(argv[1], "ssl"));
    type = CHANNEL_TYPE_SOCKET;
    if(isSSL){
        type = CHANNEL_TYPE_SSLSTREAM;
    }
    Drill::DrillUserProperties props;
    props.setProperty(USERPROP_USERNAME, "admin");
    props.setProperty(USERPROP_PASSWORD, "admin");
    props.setProperty(USERPROP_CERTFILEPATH, "../../../test/ssl/drillTestCert.pem");

    pChannel = ChannelFactory::getChannel(type, ioService, connectStr.c_str(), &props);
    if(pChannel != NULL){
        connectionStatus_t connStat;
        connStat = pChannel->init();
        if(connStat != CONN_SUCCESS){
            std::cout << "Init Failed." << std::endl;
            return -1;
        }
        connStat = pChannel->connect();
        if(connStat != CONN_SUCCESS){
            std::cout << "Connect Failed." << std::endl;
            std::cout << pChannel->getError()->msg << std::endl;
            return -1;
        }
    } else{
        std::cout << "Channel creation failed." << std::endl;
        return -1;
    }
    std::cout << "Connected." << std::endl;
    std::cout << "Starting Drill handshake" << std::endl;


    DrillTestClient client(pChannel);

    connectionStatus_t stat = client.validateHandshake(&props);
    if(stat == CONN_SUCCESS){
        std::cout << "Handshake validated." << std::endl;
    } else{ 
        if(client.m_pError != NULL){
            std::cout << "Handshake failed: " << client.m_pError->msg << ". " << std::endl;
        } else{
            std::cout << "Handshake failed  with unknown error" << ". " << std::endl;
        }
    }

    return 0;

}

