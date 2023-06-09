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
#ifndef CHANNEL_HPP
#define CHANNEL_HPP

#include "drill/common.hpp"
#include "drill/drillClient.hpp"
#include "streamSocket.hpp"
#include "errmsgs.hpp"

#if defined(IS_SSL_ENABLED)
#include <openssl/ssl.h>
#include <openssl/err.h>
#endif

namespace Drill {

class UserProperties;

    class ConnectionEndpoint{
        public:
            ConnectionEndpoint(const char* connStr);
            ConnectionEndpoint(const char* host, const char* port);
            ~ConnectionEndpoint();

            //parse the connection string and set up the host and port to connect to
            connectionStatus_t getDrillbitEndpoint();
            
            const std::string& getProtocol() const {return m_protocol;}
            const std::string& getHost() const {return m_host;}
            const std::string& getPort() const {return m_port;}
            DrillClientError* getError(){ return m_pError;};

        private:
            void parseConnectString();
            bool isDirectConnection();
            bool isZookeeperConnection();
            connectionStatus_t getDrillbitEndpointFromZk();
            connectionStatus_t handleError(connectionStatus_t status, std::string msg);

            std::string m_connectString;
            std::string m_pathToDrill;
            std::string m_protocol; 
            std::string m_hostPortStr;
            std::string m_host;
            std::string m_port;

            DrillClientError* m_pError;

    };

    class ChannelContext{
        public:
            ChannelContext(DrillUserProperties* props):m_properties(props){};
            virtual ~ChannelContext(){};
            const DrillUserProperties* getUserProperties() const { return m_properties;}
        protected:
            DrillUserProperties* m_properties;
    };

    class SSLChannelContext: public ChannelContext{
        public:
            static boost::asio::ssl::context::method getTlsVersion(const std::string & version){
                if (version == "tlsv12") {
                    return boost::asio::ssl::context::tlsv12;
                } else if (version == "tlsv11") {
                    return boost::asio::ssl::context::tlsv11;
                } else if (version == "tlsv1") {
                    return boost::asio::ssl::context::tlsv1;
                } else if ((version == "tlsv1+") || (version == "tlsv11+") || (version == "tlsv12+")) {
                    // SSLv2 and SSLv3 are disabled, so this is the equivalent of 'tls' only mode.
                    // In boost version 1.64+, they've added support for context::tls; method.
                    return boost::asio::ssl::context::sslv23;
                } else {
                    return boost::asio::ssl::context::tlsv12;
                }
            }

            /// @brief Applies Minimum TLS protocol restrictions. 
            ///         tlsv11+ means restrict to TLS version 1.1 and higher.
            ///         tlsv12+ means restrict to TLS version 1.2 and higher.
            ///  Please note that SSL_OP_NO_TLSv tags are deprecated in openSSL 1.1.0.
            /// 
            /// @param in_ver               The protocol version.
            /// 
            /// @return The SSL context options.
            static long ApplyMinTLSRestriction(const std::string & in_ver){
#if defined(IS_SSL_ENABLED)
                if (in_ver == "tlsv11+") {
                    return SSL_OP_NO_TLSv1;
                } else if (in_ver == "tlsv12+") {
                    return (SSL_OP_NO_TLSv1 | SSL_OP_NO_TLSv1_1);
                } 
#endif
                return SSL_OP_NO_SSLv3;
            }

        SSLChannelContext(DrillUserProperties *props,
                          boost::asio::ssl::context::method tlsVersion,
                          boost::asio::ssl::verify_mode verifyMode,
                          const std::string& hostnameOverride,
                          const long customSSLCtxOptions = 0) :
                    ChannelContext(props),
                    m_SSLContext(tlsVersion),
                    m_hostnameOverride(hostnameOverride),
                    m_certHostnameVerificationStatus(true) 
            {
                m_SSLContext.set_default_verify_paths();
                m_SSLContext.set_options(
                        boost::asio::ssl::context::default_workarounds
                        | boost::asio::ssl::context::no_sslv2
                        | boost::asio::ssl::context::no_sslv3
                        | boost::asio::ssl::context::single_dh_use
                        | customSSLCtxOptions
                        );
                m_SSLContext.set_verify_mode(verifyMode);
            };

            ~SSLChannelContext(){};
            boost::asio::ssl::context& getSslContext(){ return m_SSLContext;}

            /// @brief Check the certificate host name verification status.
            /// 
            /// @return FALSE if the verification has failed, TRUE otherwise.
            const bool GetCertificateHostnameVerificationStatus() const { return m_certHostnameVerificationStatus; }

            /// @brief Set the certificate host name verification status.
            ///
            /// @param in_result                The host name verification status.
            void SetCertHostnameVerificationStatus(bool in_result) { m_certHostnameVerificationStatus = in_result; }

            /// @brief Returns the overridden hostname used for certificate verification
            ///
            /// @return the hostname override, or empty if the hostname should not be overridden.
            const std::string& GetHostnameOverride() { return m_hostnameOverride; }

        private:
            boost::asio::ssl::context m_SSLContext;

            // The hostname to verify. Unused if empty.
            std::string m_hostnameOverride;

            // The flag to indicate the host name verification result.
            bool m_certHostnameVerificationStatus;
    };

    typedef ChannelContext ChannelContext_t; 
    typedef SSLChannelContext SSLChannelContext_t; 

    /***
     * The Channel class encapsulates a connection to a drillbit. Based on 
     * the connection string and the options, the connection will be either 
     * a simple socket or a socket using an ssl stream. The class also encapsulates
     * connecting to a drillbit directly or thru zookeeper.
     * The channel class owns the socket and the io_service that the applications
     * will use to communicate with the server.
     ***/
    class Channel{
        friend class ChannelFactory;
        public: 
            Channel(boost::asio::io_service& ioService, const char* connStr);
            Channel(boost::asio::io_service& ioService, const char* host, const char* port);
            virtual ~Channel();
            virtual connectionStatus_t init()=0;
            connectionStatus_t connect();
            bool isConnected(){ return m_state == CHANNEL_CONNECTED;}
            template <typename SettableSocketOption> void setOption(SettableSocketOption& option);
            DrillClientError* getError(){ return m_pError;}
            void close(){ 
                if(m_state==CHANNEL_INITIALIZED||m_state==CHANNEL_CONNECTED){
                    m_pSocket->protocolClose();
                    m_state=CHANNEL_CLOSED;
                }
            } // Not OK to use the channel after this call. 

            boost::asio::io_service& getIOService(){
                return m_ioService;
            }

            // returns a reference to the underlying socket 
            // This access should really be removed and encapsulated in calls that 
            // manage async_send and async_recv 
            // Until then we will let DrillClientImpl have direct access
            streamSocket_t& getInnerSocket(){
                return m_pSocket->getInnerSocket();
            }
            
            AsioStreamSocket& getSocketStream(){
                return *m_pSocket;
            }

            ConnectionEndpoint* getEndpoint(){return m_pEndpoint;}

            ChannelContext_t* getChannelContext(){ return m_pContext; }

        protected:
            connectionStatus_t handleError(connectionStatus_t status, std::string msg);

            /// @brief Handle protocol handshake exceptions.
            /// 
            /// @param in_err                   The error.
            /// 
            /// @return the connectionStatus.
            virtual connectionStatus_t HandleProtocolHandshakeException(const boost::system::system_error& in_err){
                return handleError(CONN_HANDSHAKE_FAILED, in_err.what());
            }

            virtual connectionStatus_t setSocketInformation() {
                return CONN_SUCCESS;
            }

            boost::asio::io_service& m_ioService;
            boost::asio::io_service m_ioServiceFallback; // used if m_ioService is not provided
            AsioStreamSocket* m_pSocket;
            ConnectionEndpoint *m_pEndpoint;
            ChannelContext_t *m_pContext;

        private:
            typedef enum channelState{ 
                CHANNEL_UNINITIALIZED=1, 
                CHANNEL_INITIALIZED, 
                CHANNEL_CONNECTED, 
                CHANNEL_CLOSED       
            } channelState_t;
            
            connectionStatus_t connectInternal();
            connectionStatus_t protocolHandshake(bool useSystemConfig){
                connectionStatus_t status = CONN_SUCCESS;
                try{
                    m_pSocket->protocolHandshake(useSystemConfig);
                } catch (boost::system::system_error e) {
                    status = HandleProtocolHandshakeException(e);
                }
                return status;
            }

            channelState_t m_state;
            DrillClientError* m_pError;
    };

    class SocketChannel: public Channel{
        public:
            SocketChannel(boost::asio::io_service& ioService, const char* connStr)
                :Channel(ioService, connStr){
            }
            SocketChannel(boost::asio::io_service& ioService, const char* host, const char* port)
                :Channel(ioService, host, port){
            }
            connectionStatus_t init();
    };

    class SSLStreamChannel: public Channel{
        public:
            SSLStreamChannel(boost::asio::io_service& ioService, const char* connStr)
                :Channel(ioService, connStr){
            }
            SSLStreamChannel(boost::asio::io_service& ioService, const char* host, const char* port)
                :Channel(ioService, host, port){
            }
            connectionStatus_t init();
        protected:
#if defined(IS_SSL_ENABLED)
            /// @brief Handle protocol handshake exceptions for SSL specific failures.
            /// 
            /// @param in_err               The error.
            /// 
            /// @return the connectionStatus.
            connectionStatus_t HandleProtocolHandshakeException(const boost::system::system_error& in_err) {
                const boost::system::error_code& errcode = in_err.code();
                if (!(((SSLChannelContext_t *)m_pContext)->GetCertificateHostnameVerificationStatus())){
                    return handleError(
                        CONN_HANDSHAKE_FAILED,
                        getMessage(ERR_CONN_SSL_CN, in_err.what()));
                }
                else if (boost::asio::error::get_ssl_category() == errcode.category() && 
                    SSL_R_CERTIFICATE_VERIFY_FAILED == ERR_GET_REASON(errcode.value())){
                    return handleError(
                        CONN_HANDSHAKE_FAILED,
                        getMessage(ERR_CONN_SSL_CERTVERIFY, in_err.what()));
                }
                else if (boost::asio::error::get_ssl_category() == errcode.category() &&
                    SSL_R_UNSUPPORTED_PROTOCOL == ERR_GET_REASON(errcode.value())){
                    return handleError(
                        CONN_HANDSHAKE_FAILED,
                        getMessage(ERR_CONN_SSL_PROTOVER, in_err.what()));
                }
                else{
                    return handleError(
                        CONN_HANDSHAKE_FAILED,
                        getMessage(ERR_CONN_SSL_GENERAL, in_err.what()));
                }
            }

            connectionStatus_t setSocketInformation() {
                const char* sniProperty;
                SSLChannelContext_t& context = *((SSLChannelContext_t *)m_pContext);
                if (!context.GetHostnameOverride().empty()){
                    sniProperty = context.GetHostnameOverride().c_str();
                }
                else{
                    sniProperty = m_pEndpoint->getHost().c_str();
                }
                if (!SSL_set_tlsext_host_name(((SslSocket *)m_pSocket)->getSocketStream().native_handle(), sniProperty)) {
                    return handleError(CONN_SSLERROR, getMessage(ERR_CONN_SSL_SNI, sniProperty, ERR_func_error_string(ERR_get_error())));
                }
                return CONN_SUCCESS;
            }
#endif
    };

    class ChannelFactory{
        public:
            static Channel* getChannel(channelType_t t,
                                       boost::asio::io_service& ioService,
                                       const char* connStr, DrillUserProperties* props);
            static Channel* getChannel(channelType_t t,
                                       boost::asio::io_service& ioService,
                                       const char* host,
                                       const char* port,
                                       DrillUserProperties* props);
        private:
            static ChannelContext_t* getChannelContext(channelType_t t, DrillUserProperties* props);
    };

    /// @brief Hostname verification callback wrapper.
    class DrillSSLHostnameVerifier{
        public:
            /// @brief The constructor.
            /// 
            /// @param in_channel                  The Channel.
            DrillSSLHostnameVerifier(Channel* in_channel) : m_channel(in_channel){
                DRILL_LOG(LOG_INFO)
                    << "DrillSSLHostnameVerifier::DrillSSLHostnameVerifier: +++++ Enter +++++" 
                    << std::endl;
            }

            /// @brief Perform certificate verification.
            /// 
            /// @param in_preverified           Pre-verified indicator.
            /// @param in_ctx                   Verify context.
            bool operator()(
                bool in_preverified,
                boost::asio::ssl::verify_context& in_ctx){
                DRILL_LOG(LOG_INFO) << "DrillSSLHostnameVerifier::operator(): +++++ Enter +++++" << std::endl;

                // Gets the channel context.
                SSLChannelContext_t* context = (SSLChannelContext_t*)(m_channel->getChannelContext());

                const char* hostname;
                if (context->GetHostnameOverride().empty()) {
                    // Retrieve the host before we perform Host name verification.
                    // This is because host with ZK mode is selected after the connect() function is called.
                    hostname = m_channel->getEndpoint()->getHost().c_str();
                } else {
                    hostname = context->GetHostnameOverride().c_str();
                }

                boost::asio::ssl::rfc2818_verification verifier(hostname);

                // Perform verification.
                bool verified = verifier(in_preverified, in_ctx);

                DRILL_LOG(LOG_DEBUG) 
                    << "DrillSSLHostnameVerifier::operator(): Verification Result: " 
                    << verified 
                    << std::endl;

                // Sets the result back to the context.
                context->SetCertHostnameVerificationStatus(verified);
                return verified;
            }

        private:

            // The SSL channel.
            Channel* m_channel;
    };

} // namespace Drill

#endif // CHANNEL_HPP

