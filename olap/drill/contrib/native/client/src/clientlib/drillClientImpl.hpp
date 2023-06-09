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
#ifndef DRILL_CLIENT_IMPL_H
#define DRILL_CLIENT_IMPL_H

#include "drill/common.hpp"
// Define some BOOST defines
// WIN32_SHUTDOWN_ON_TIMEOUT is defined in "drill/common.hpp" for Windows 32 bit platform
#ifndef WIN32_SHUTDOWN_ON_TIMEOUT
#define BOOST_ASIO_ENABLE_CANCELIO
#endif //WIN32_SHUTDOWN_ON_TIMEOUT

#include <algorithm>
#include <queue>
#include <vector>

#include <boost/asio.hpp>
#if defined _WIN32  || defined _WIN64
//Windows header files redefine 'random'
#ifdef random
#undef random
#endif
#endif
#include <boost/asio/deadline_timer.hpp>
#include <boost/function.hpp>
#include <boost/thread.hpp>
#include "drill/drillClient.hpp"
#include "drill/drillConfig.hpp"
#include "drill/drillError.hpp"
#include "drill/preparedStatement.hpp"
#include "channel.hpp"
#include "collectionsImpl.hpp"
#include "metadata.hpp"
#include "rpcMessage.hpp"
#include "utils.hpp"
#include "User.pb.h"
#include "UserBitShared.pb.h"
#include "saslAuthenticatorImpl.hpp"

namespace Drill {

class DrillClientImpl;

class DrillClientQueryHandle;

class DrillClientPrepareHandle;
class RecordBatch;

/*
 * Defines the interface used by DrillClient and implemented by DrillClientImpl and PooledDrillClientImpl
 * */
class DrillClientImplBase{
    public:
        DrillClientImplBase(){
        }

        virtual ~DrillClientImplBase(){
        }

        //Connect via Zookeeper or directly.
        //Makes an initial connection to a drillbit. successful connect adds the first drillbit to the pool.
        virtual connectionStatus_t connect(const char* connStr, DrillUserProperties* props)=0;

        // Test whether the client is active. Returns true if any one of the underlying connections is active
        virtual bool Active()=0;

        // Closes all open connections. 
        virtual void Close()=0;

        // Returns the last error encountered by any of the underlying executing queries or connections
        virtual DrillClientError* getError()=0;

        // Submits a query to a drillbit. 
        virtual DrillClientQueryResult* SubmitQuery(::exec::shared::QueryType t, const std::string& plan, pfnQueryResultsListener listener, void* listenerCtx)=0;
        virtual DrillClientPrepareHandle* PrepareQuery(const std::string& plan, pfnPreparedStatementListener listener, void* listenerCtx)=0;
        virtual DrillClientQueryResult* ExecuteQuery(const PreparedStatement& pstmt, pfnQueryResultsListener listener, void* listenerCtx)=0;

        //Waits as a connection has results pending
        virtual void waitForResults()=0;

        //Validates handshake at connect time.
        virtual connectionStatus_t validateHandshake(DrillUserProperties* props)=0;

        virtual void freeQueryResources(DrillClientQueryHandle* pQryHandle)=0;

        virtual meta::DrillMetadata* getMetadata() = 0;

        virtual void freeMetadata(meta::DrillMetadata* metadata) = 0;
};

/**
 * Base type for query handles
 */
class DrillClientQueryHandle{
    friend class DrillClientImpl;
    public:
        DrillClientQueryHandle(DrillClientImpl& client, int32_t coordId, const std::string& query, void* context, int in_expectedRPCType) :
        m_client(client),
        m_coordinationId(coordId),
        m_query(query),
        m_status(QRY_SUCCESS),
        m_bCancel(false),
        m_bHasError(false),
        m_expectedRPCType(in_expectedRPCType),
        m_pError(NULL),
        m_pApplicationContext(context){
    };

    virtual ~DrillClientQueryHandle(){
        clearAndDestroy();
    };

    virtual void cancel();
    bool isCancelled() const {return m_bCancel;};
    int32_t getCoordinationId() const { return m_coordinationId;}
    const std::string&  getQuery() const { return m_query;}

    bool hasError() const { return m_bHasError;}
    void resetError() { m_bHasError = false; }

    status_t getErrorStatus() const { return m_pError!=NULL?(status_t)m_pError->status:QRY_SUCCESS;}
    const DrillClientError* getError() const { return m_pError;}
    void setQueryStatus(status_t s){ m_status = s;}
    status_t getQueryStatus() const { return m_status;}
    inline DrillClientImpl& client() const { return m_client; };
    int getExpectedRPCType() const { return m_expectedRPCType; };
    inline void* getApplicationContext() const { return m_pApplicationContext; }

    protected:

    virtual void signalError(DrillClientError* pErr);
    virtual void clearAndDestroy();

    private:
    DrillClientImpl& m_client;

    int32_t m_coordinationId;
    std::string m_query;
    status_t m_status;
    bool m_bCancel;
    bool m_bHasError;
    int m_expectedRPCType;
    const DrillClientError* m_pError;

    void* m_pApplicationContext;
};

template<typename Listener, typename ListenerValue>
class DrillClientBaseHandle: public DrillClientQueryHandle {
    friend class DrillClientImpl;
    public:
        DrillClientBaseHandle(DrillClientImpl& client, int32_t coordId, const std::string& query, Listener listener, void* context, int in_expectedRPCType) :
            DrillClientQueryHandle(client, coordId, query, context, in_expectedRPCType),
            m_pApplicationListener(listener){
    };

    virtual ~DrillClientBaseHandle(){
        clearAndDestroy();
    };

    inline Listener getApplicationListener() const { return m_pApplicationListener; }


    protected:
    virtual status_t notifyListener(ListenerValue v, DrillClientError* pErr);

    virtual void signalError(DrillClientError* pErr);

    private:
    Listener m_pApplicationListener;
};

class DrillClientQueryResult: public DrillClientBaseHandle<pfnQueryResultsListener, RecordBatch*>{
    friend class DrillClientImpl;
    public:
    DrillClientQueryResult(DrillClientImpl& client, int32_t coordId, const std::string& query, pfnQueryResultsListener listener, void* listenerCtx):
        DrillClientBaseHandle<pfnQueryResultsListener, RecordBatch*>(client, coordId, query, listener, listenerCtx, exec::user::QUERY_HANDLE),
        m_numBatches(0),
        m_columnDefs(new std::vector<Drill::FieldMetadata*>),
        m_bIsQueryPending(true),
        m_bIsLastChunk(false),
        m_bHasSchemaChanged(false),
        m_bHasData(false),
        m_queryState(exec::shared::QueryResult_QueryState_STARTING),
        m_pQueryId(NULL),
        m_pSchemaListener(NULL) {
    };

    virtual ~DrillClientQueryResult(){
        this->clearAndDestroy();
    };

    // get data asynchronously
    void registerSchemaChangeListener(pfnSchemaListener l){
        m_pSchemaListener=l;
    }

    void cancel();
    // Synchronous call to get data. Caller assumes ownership of the record batch
    // returned and it is assumed to have been consumed.
    RecordBatch*  getNext();
    // Synchronous call to get a look at the next Record Batch. This
    // call does not move the current pointer forward. Repeated calls
    // to peekNext return the same value until getNext is called.
    RecordBatch*  peekNext();
    // Blocks until data is available.
    void waitForData();

    // placeholder to return an empty col def vector when calls are made out of order.
    static FieldDefPtr s_emptyColDefs;

    FieldDefPtr getColumnDefs() {
        boost::lock_guard<boost::mutex> bufferLock(this->m_schemaMutex);
        return this->m_columnDefs;
    }

    bool hasSchemaChanged() const {return this->m_bHasSchemaChanged;};

    void setQueryId(exec::shared::QueryId* q){this->m_pQueryId=q;}
    exec::shared::QueryId& getQueryId() const { return *(this->m_pQueryId); }

    void setQueryState(exec::shared::QueryResult_QueryState s){ m_queryState = s;}
    exec::shared::QueryResult_QueryState getQueryState() const { return m_queryState;}
    void setIsQueryPending(bool isPending){
        boost::lock_guard<boost::mutex> cvLock(this->m_cvMutex);
        m_bIsQueryPending=isPending;
    }
    protected:
    virtual status_t notifyListener(RecordBatch* batch, DrillClientError* pErr);
    virtual void signalError(DrillClientError* pErr);
    virtual void clearAndDestroy();

    private:
    status_t setupColumnDefs(exec::shared::QueryData* pQueryData);
    status_t defaultQueryResultsListener(void* ctx, RecordBatch* b, DrillClientError* err);
    // Construct a DrillClientError object, set the appropriate state and signal any listeners, condition variables.
    // Also used when a query is cancelled or when a query completed response is received.
    // Error object is now owned by the DrillClientQueryResult object.
    void signalComplete();

    size_t m_numBatches; // number of record batches received so far

    // Vector of Buffers holding data returned by the server
    // Each data buffer is decoded into a RecordBatch
    std::vector<ByteBuf_t> m_dataBuffers;
    std::queue<RecordBatch*> m_recordBatches;
    FieldDefPtr m_columnDefs;

    // Mutex to protect schema definitions
    boost::mutex m_schemaMutex;
    // Mutex for Cond variable for read write to batch vector
    boost::mutex m_cvMutex;
    // Condition variable to signal arrival of more data. Condition variable is signaled
    // if the recordBatches queue is not empty
    boost::condition_variable m_cv;

    // state
    // if m_bIsQueryPending is true, we continue to wait for results
    bool m_bIsQueryPending;
    bool m_bIsLastChunk;
    bool m_bHasSchemaChanged;
    bool m_bHasData;

    // state in the last query result received from the server.
    exec::shared::QueryResult_QueryState m_queryState;

    exec::shared::QueryId* m_pQueryId;

    // Schema change listener
    pfnSchemaListener m_pSchemaListener;
};

class DrillClientPrepareHandle: public DrillClientBaseHandle<pfnPreparedStatementListener, PreparedStatement*>, public PreparedStatement {
    public:
    DrillClientPrepareHandle(DrillClientImpl& client, int32_t coordId, const std::string& query, pfnPreparedStatementListener listener, void* listenerCtx):
        DrillClientBaseHandle<pfnPreparedStatementListener, PreparedStatement*>(client, coordId, query, listener, listenerCtx, exec::user::PREPARED_STATEMENT),
        PreparedStatement(),
        m_columnDefs(new std::vector<Drill::FieldMetadata*>) {
    };

    // PreparedStatement overrides
	virtual std::size_t getNumFields() const { return m_columnDefs->size(); }
	virtual const Drill::FieldMetadata& getFieldMetadata(std::size_t index) const { return *m_columnDefs->at(index);}

    protected:
    virtual void clearAndDestroy();

    private:
    friend class DrillClientImpl;
    status_t setupPreparedStatement(const exec::user::PreparedStatement& pstmt);

    FieldDefPtr m_columnDefs;
    ::exec::user::PreparedStatementHandle m_preparedStatementHandle;
};

typedef status_t (*pfnServerMetaListener)(void* ctx, const exec::user::ServerMeta* serverMeta, DrillClientError* err);
class DrillClientServerMetaHandle: public DrillClientBaseHandle<pfnServerMetaListener, const exec::user::ServerMeta*> {
    public:
    DrillClientServerMetaHandle(DrillClientImpl& client, int32_t coordId, pfnServerMetaListener listener, void* listenerCtx):
        DrillClientBaseHandle<pfnServerMetaListener, const exec::user::ServerMeta*>(client, coordId, "server meta", listener, listenerCtx, exec::user::SERVER_META) {
    };

    private:
    friend class DrillClientImpl;

};

template<typename Listener, typename MetaType, typename MetaImpl, typename MetadataResult>
class DrillClientMetadataResult: public DrillClientBaseHandle<Listener, const DrillCollection<MetaType>*> {
public:
    DrillClientMetadataResult(DrillClientImpl& client, int32_t coordId, const std::string& query, Listener listener, void* listenerCtx, int in_expectedRPCType) :
        DrillClientBaseHandle<Listener, const DrillCollection<MetaType>*>(client, coordId, query, listener, listenerCtx, in_expectedRPCType) {}

    void attachMetadataResult(MetadataResult* result) { this->m_pMetadata.reset(result); }

private:
    friend class DrillClientImpl;

    // Meta informations returned to the user, linked to the handle
    DrillVector<MetaType, MetaImpl> m_meta;

    // to keep a reference on the underlying metadata object, and
    // make sure it's clean when this handle is destroyed
    boost::shared_ptr<MetadataResult> m_pMetadata;

};

class DrillClientCatalogResult: public DrillClientMetadataResult<Metadata::pfnCatalogMetadataListener, meta::CatalogMetadata, meta::DrillCatalogMetadata, exec::user::GetCatalogsResp> {
    friend class DrillClientImpl;
public:
    DrillClientCatalogResult(DrillClientImpl& client, int32_t coordId, Metadata::pfnCatalogMetadataListener listener, void* listenerCtx):
        DrillClientMetadataResult<Metadata::pfnCatalogMetadataListener, meta::CatalogMetadata, meta::DrillCatalogMetadata, exec::user::GetCatalogsResp>(client, coordId, "getCatalog", listener, listenerCtx, exec::user::CATALOGS) {}
};

class DrillClientSchemaResult: public DrillClientMetadataResult<Metadata::pfnSchemaMetadataListener, meta::SchemaMetadata, meta::DrillSchemaMetadata, exec::user::GetSchemasResp> {
    friend class DrillClientImpl;
public:
    DrillClientSchemaResult(DrillClientImpl& client, int32_t coordId, Metadata::pfnSchemaMetadataListener listener, void* listenerCtx):
        DrillClientMetadataResult<Metadata::pfnSchemaMetadataListener, meta::SchemaMetadata, meta::DrillSchemaMetadata, exec::user::GetSchemasResp>(client, coordId, "getSchemas", listener, listenerCtx, exec::user::SCHEMAS) {}
};

class DrillClientTableResult: public DrillClientMetadataResult<Metadata::pfnTableMetadataListener, meta::TableMetadata, meta::DrillTableMetadata, exec::user::GetTablesResp> {
    friend class DrillClientImpl;
public:
    DrillClientTableResult(DrillClientImpl& client, int32_t coordId, Metadata::pfnTableMetadataListener listener, void* listenerCtx):
        DrillClientMetadataResult<Metadata::pfnTableMetadataListener, meta::TableMetadata, meta::DrillTableMetadata, exec::user::GetTablesResp>(client, coordId, "getTables", listener, listenerCtx, exec::user::TABLES) {}
};

class DrillClientColumnResult: public DrillClientMetadataResult<Metadata::pfnColumnMetadataListener, meta::ColumnMetadata, meta::DrillColumnMetadata, exec::user::GetColumnsResp> {
    friend class DrillClientImpl;
    public:
    DrillClientColumnResult(DrillClientImpl& client, int32_t coordId, Metadata::pfnColumnMetadataListener listener, void* listenerCtx):
        DrillClientMetadataResult<Metadata::pfnColumnMetadataListener, meta::ColumnMetadata, meta::DrillColumnMetadata, exec::user::GetColumnsResp>(client, coordId, "getColumns", listener, listenerCtx, exec::user::COLUMNS) {}
};

// Length Decoder Function Pointer definition
typedef size_t (DrillClientImpl::*lengthDecoder)(const ByteBuf_t, uint32_t&);

class DrillClientImpl : public DrillClientImplBase{
    public:
        DrillClientImpl():
            m_handshakeVersion(0),
            m_handshakeStatus(exec::user::SUCCESS),
            m_bIsConnected(false),
            m_saslAuthenticator(NULL),
            m_saslResultCode(SASL_OK),
            m_saslDone(false),
            m_pendingRequests(0),
            m_pError(NULL),
            m_pListenerThread(NULL),
            m_pWork(NULL),
            m_pChannel(NULL),
            m_pChannelContext(NULL),
            m_deadlineTimer(m_io_service),
            m_heartbeatTimer(m_io_service),
            m_rbuf(NULL),
            m_wbuf(MAX_SOCK_RD_BUFSIZE),
            m_bIsDirectConnection(false)
    {
        m_coordinationId=rand()%1729+1;
        m_fpCurrentReadMsgHandler = &DrillClientImpl::readMsg;
        m_fpCurrentSendHandler = &DrillClientImpl::sendSyncPlain;
    };

        ~DrillClientImpl(){
            //Cancel any pending requests
            m_heartbeatTimer.cancel();
            m_deadlineTimer.cancel();
            m_io_service.stop();
            //Free any record batches or buffers remaining
            //Clear and destroy DrillClientQueryResults vector?
            if(this->m_pWork!=NULL){
                delete this->m_pWork;
                this->m_pWork = NULL;
            }
            if(this->m_saslAuthenticator!=NULL){
                delete this->m_saslAuthenticator;
                this->m_saslAuthenticator = NULL;
            }
            {
                boost::lock_guard<boost::mutex> lock(m_channelMutex);
                if (this->m_pChannel != NULL) {
                    m_pChannel->close();
                    delete this->m_pChannel;
                    this->m_pChannel = NULL;
                }
                if (this->m_pChannelContext != NULL) {
                    delete this->m_pChannelContext;
                    this->m_pChannelContext = NULL;
                }
            }

            if(m_rbuf!=NULL){
                Utils::freeBuffer(m_rbuf, MAX_SOCK_RD_BUFSIZE); m_rbuf=NULL;
            }
            if(m_pError!=NULL){
                delete m_pError; m_pError=NULL;
            }
            //Terminate and free the heartbeat thread
            //if(this->m_pHeartbeatThread!=NULL){
            //    this->m_pHeartbeatThread->interrupt();
            //    this->m_pHeartbeatThread->join();
            //    delete this->m_pHeartbeatThread;
            //    this->m_pHeartbeatThread = NULL;
            //}
            //Terminate and free the listener thread
            if(this->m_pListenerThread!=NULL){
                this->m_pListenerThread->interrupt();
                this->m_pListenerThread->join();
                delete this->m_pListenerThread;
                this->m_pListenerThread = NULL;
            }
        };

        //Connect via Zookeeper or directly
        connectionStatus_t connect(const char* connStr, DrillUserProperties* props);
        connectionStatus_t connect(const char* host, const char* port, DrillUserProperties* props);

        // test whether the client is active
        bool Active();
        void Close() ;
        DrillClientError* getError(){ return m_pError;}
        DrillClientQueryResult* SubmitQuery(::exec::shared::QueryType t, const std::string& plan, pfnQueryResultsListener listener, void* listenerCtx);
        DrillClientPrepareHandle* PrepareQuery(const std::string& plan, pfnPreparedStatementListener listener, void* listenerCtx);
        DrillClientQueryResult* ExecuteQuery(const PreparedStatement& pstmt, pfnQueryResultsListener listener, void* listenerCtx);

        void waitForResults();
        connectionStatus_t validateHandshake(DrillUserProperties* props);
        void freeQueryResources(DrillClientQueryHandle* pQryHandle){
            delete pQryHandle;
        };
        
        const exec::user::RpcEndpointInfos& getServerInfos() const { return m_serverInfos; }

        meta::DrillMetadata* getMetadata();

        void freeMetadata(meta::DrillMetadata* metadata);

        static bool clientNeedsAuthentication(const DrillUserProperties* userProperties);

        bool handleComplexTypes(const DrillUserProperties* userProperties);

    private:
        friend class meta::DrillMetadata;
        friend class DrillClientQueryHandle;
        friend class DrillClientQueryResult;
        friend class PooledDrillClientImpl;

        struct compareQueryId{
            bool operator()(const exec::shared::QueryId* q1, const exec::shared::QueryId* q2) const {
                return q1->part1()<q2->part1() || (q1->part1()==q2->part1() && q1->part2() < q2->part2());
            }
        };

        // Direct connection to a drillbit
        // host can be name or ip address, port can be port number or name of service in /etc/services
        connectionStatus_t connect(const char* host, const char* port);
        void startHeartbeatTimer();// start or restart the heartbeat timer
        connectionStatus_t sendHeartbeat(); // send a heartbeat to the server
        void handleHeartbeatTimeout(const boost::system::error_code & err); // send a heartbeat. If send fails, broadcast error, close connection and bail out.

        int32_t getNextCoordinationId(){ return ++m_coordinationId; };
        // synchronous message send handlers
        connectionStatus_t sendSyncCommon(rpc::OutBoundRpcMessage& msg);
        connectionStatus_t sendSyncPlain();
        connectionStatus_t sendSyncEncrypted();
        // handshake
        connectionStatus_t recvHandshake();
        void handleHandshake(ByteBuf_t b, const boost::system::error_code& err, std::size_t bytes_transferred );
        void handleHShakeReadTimeout(const boost::system::error_code & err);
        // starts the listener thread that receives responses/messages from the server
        void startMessageListener(); 
        // Query results
        void getNextResult();
        // Read Message Handlers
        status_t readMsg(const ByteBuf_t inBuf, AllocatedBufferPtr* allocatedBuffer, rpc::InBoundRpcMessage& msg);
        status_t readAndDecryptMsg(const ByteBuf_t inBuf, AllocatedBufferPtr* allocatedBuffer, rpc::InBoundRpcMessage& msg);
        status_t readLenBytesFromSocket(const ByteBuf_t bufWithLenField, AllocatedBufferPtr* bufferWithDataAndLenBytes,
                                        uint32_t& lengthFieldLength, lengthDecoder lengthDecodeHandler);
        void doReadFromSocket(ByteBuf_t inBuf, size_t bytesToRead, boost::system::error_code& errorCode);
        void doWriteToSocket(const char* dataPtr, size_t bytesToWrite, boost::system::error_code& errorCode);
        // Length decode handlers
        size_t lengthDecode(const ByteBuf_t inBuf, uint32_t& rmsgLen);
        size_t rpcLengthDecode(const ByteBuf_t inBuf, uint32_t& rmsgLen);
        status_t processQueryResult(AllocatedBufferPtr allocatedBuffer, const rpc::InBoundRpcMessage& msg);
        status_t processQueryData(AllocatedBufferPtr allocatedBuffer, const rpc::InBoundRpcMessage& msg);
        status_t processCancelledQueryResult( exec::shared::QueryId& qid, exec::shared::QueryResult* qr);
        status_t processQueryId(AllocatedBufferPtr allocatedBuffer, const rpc::InBoundRpcMessage& msg );
        status_t processPreparedStatement(AllocatedBufferPtr allocatedBuffer, const rpc::InBoundRpcMessage& msg );
        status_t processCatalogsResult(AllocatedBufferPtr allocatedBuffer, const rpc::InBoundRpcMessage& msg );
        status_t processSchemasResult(AllocatedBufferPtr allocatedBuffer, const rpc::InBoundRpcMessage& msg );
        status_t processTablesResult(AllocatedBufferPtr allocatedBuffer, const rpc::InBoundRpcMessage& msg );
        status_t processColumnsResult(AllocatedBufferPtr allocatedBuffer, const rpc::InBoundRpcMessage& msg );
        status_t processServerMetaResult(AllocatedBufferPtr allocatedBuffer, const rpc::InBoundRpcMessage& msg );
        DrillClientQueryResult* findQueryResult(const exec::shared::QueryId& qid);
        status_t processQueryStatusResult( exec::shared::QueryResult* qr,
                DrillClientQueryResult* pDrillClientQueryResult);
        void handleReadTimeout(const boost::system::error_code & err);
        void handleRead(ByteBuf_t inBuf, const boost::system::error_code & err, size_t bytes_transferred) ;
        status_t validateDataMessage(const rpc::InBoundRpcMessage& msg, const exec::shared::QueryData& qd, std::string& valError);
        status_t validateResultMessage(const rpc::InBoundRpcMessage& msg, const exec::shared::QueryResult& qr, std::string& valError);
        bool validateResultRPCType(DrillClientQueryHandle* pQueryHandle, const rpc::InBoundRpcMessage& msg);
        connectionStatus_t handleConnError(connectionStatus_t status, const std::string& msg);
        connectionStatus_t handleConnError(DrillClientError* err);
        status_t handleQryCancellation(status_t status, DrillClientQueryResult* pQueryResult);
        status_t handleQryError(status_t status, const std::string& msg, DrillClientQueryHandle* pQueryHandle);
        status_t handleQryError(status_t status, const exec::shared::DrillPBError& e, DrillClientQueryHandle* pQueryHandle);
        // handle query state indicating query is COMPLETED or CANCELED
        // (i.e., COMPLETED or CANCELED)
        status_t handleTerminatedQryState(status_t status,
                const std::string& msg,
                DrillClientQueryResult* pQueryResult);
        void broadcastError(DrillClientError* pErr);
        void removeQueryHandle(DrillClientQueryHandle* pQueryHandle);
        void sendAck(const rpc::InBoundRpcMessage& msg, bool isOk);
        void sendCancel(const exec::shared::QueryId* pQueryId);

        template<typename Handle>
        Handle* sendMsg(boost::function<Handle*(int32_t)> handleFactory, ::exec::user::RpcType type, const ::google::protobuf::Message& msg);

        // metadata requests
        DrillClientCatalogResult* getCatalogs(const std::string& catalogPattern, const std::string& searchEscapeString, Metadata::pfnCatalogMetadataListener listener, void* listenerCtx);
        DrillClientSchemaResult* getSchemas(const std::string& catalogPattern, const std::string& schemaPattern, const std::string& searchEscapeString, Metadata::pfnSchemaMetadataListener listener, void* listenerCtx);
        DrillClientTableResult* getTables(const std::string& catalogPattern, const std::string& schemaPattern, const std::string& tablePattern, const std::vector<std::string>* tableTypes, const std::string& searchEscapeString, Metadata::pfnTableMetadataListener listener, void* listenerCtx);
        DrillClientColumnResult* getColumns(const std::string& catalogPattern, const std::string& schemaPattern, const std::string& tablePattern, const std::string& columnPattern, const std::string& searchEscapeString, Metadata::pfnColumnMetadataListener listener, void* listenerCtx);

        // SASL exchange
        connectionStatus_t handleAuthentication(const DrillUserProperties *userProperties);
        void initiateAuthentication();
        void sendSaslResponse(const exec::shared::SaslMessage& response);
        void processSaslChallenge(AllocatedBufferPtr allocatedBuffer, const rpc::InBoundRpcMessage& msg);
        void finishAuthentication();

        void shutdownSocket();
        bool clientNeedsEncryption(const DrillUserProperties* userProperties);

        int32_t m_coordinationId;
        int32_t m_handshakeVersion;
        exec::user::HandshakeStatus m_handshakeStatus;
        std::string m_handshakeErrorId;
        std::string m_handshakeErrorMsg;
        exec::user::RpcEndpointInfos m_serverInfos;
        std::vector<exec::user::RpcType> m_supportedMethods;
        bool m_bIsConnected;

        std::vector<std::string> m_serverAuthMechanisms;
        SaslAuthenticatorImpl* m_saslAuthenticator;
        int m_saslResultCode;
        bool m_saslDone;
        boost::mutex m_saslMutex; // mutex to protect m_saslDone
        // mutex to protect deallocation of sasl connection
        boost::mutex m_sasl_dispose_mutex;
        boost::condition_variable m_saslCv; // to signal completion of SASL exchange

        // Used for encryption and is set when server notifies in first handshake response.
        EncryptionContext m_encryptionCtxt;

        // Function pointer for read and send handler. By default these are referred to handler for plain message read/send. When encryption is enabled
        // then after successful handshake these pointers refer to handler for encrypted message read/send over wire.
        status_t (DrillClientImpl::*m_fpCurrentReadMsgHandler)(ByteBuf_t inBuf, AllocatedBufferPtr* allocatedBuffer, rpc::InBoundRpcMessage& msg);
        connectionStatus_t (DrillClientImpl::*m_fpCurrentSendHandler)();

        std::string m_connectStr; 

        // 
        // number of outstanding read requests.
        // handleRead will keep asking for more results as long as this number is not zero.
        size_t m_pendingRequests;
        //mutex to protect m_pendingRequests
        boost::mutex m_prMutex;

        // Error Object. NULL if no error. Set if the error is valid for ALL running queries.
        // All the query result objects will
        // also have the error object set.
        // If the error is query specific, only the query results object will have the error set.
        DrillClientError* m_pError;

        //Started after the connection is established and sends heartbeat messages after {heartbeat frequency} seconds
        //The thread is killed on disconnect.
        //boost::thread * m_pHeartbeatThread;

        // for boost asio
        boost::thread * m_pListenerThread;
        boost::asio::io_service m_io_service;
        // the work object prevent io_service running out of work
        boost::asio::io_service::work * m_pWork;

        // Mutex to protect channel
        boost::mutex m_channelMutex;
        Channel* m_pChannel;
        ChannelContext_t* m_pChannelContext;

        boost::asio::deadline_timer m_deadlineTimer; // to timeout async queries that never return
        boost::asio::deadline_timer m_heartbeatTimer; // to send heartbeat messages

        std::string m_connectedHost; // The hostname and port the socket is connected to.

        //for synchronous messages, like validate handshake
        ByteBuf_t m_rbuf; // buffer for receiving synchronous messages
        DataBuf m_wbuf; // buffer for sending synchronous message

        // Mutex to protect drill client operations
        boost::mutex m_dcMutex;

        // Map of coordination id to Query handles.
        std::map<int, DrillClientQueryHandle*> m_queryHandles;

        // Map of query id to query result for currently executing queries
        std::map<exec::shared::QueryId*, DrillClientQueryResult*, compareQueryId> m_queryResults;

        // Condition variable to signal completion of all queries. 
        boost::condition_variable m_cv;

        bool m_bIsDirectConnection;
};

inline bool DrillClientImpl::Active() {
    return this->m_bIsConnected;
}


/* *
 *  Provides the same public interface as a DrillClientImpl but holds a pool of DrillClientImpls.
 *  Every submitQuery uses a different DrillClientImpl to distribute the load.
 *  DrillClient can use this class instead of DrillClientImpl to get better load balancing.
 * */
class PooledDrillClientImpl : public DrillClientImplBase{
    public:
        PooledDrillClientImpl():
        	m_lastConnection(-1),
			m_queriesExecuted(0),
			m_maxConcurrentConnections(DEFAULT_MAX_CONCURRENT_CONNECTIONS),
			m_bIsDirectConnection(false),
			m_pError(NULL),
			m_pUserProperties() {
            char* maxConn=std::getenv(MAX_CONCURRENT_CONNECTIONS_ENV);
            if(maxConn!=NULL){
                m_maxConcurrentConnections=atoi(maxConn);
            }
        }

        ~PooledDrillClientImpl(){
            for(std::vector<DrillClientImpl*>::iterator it = m_clientConnections.begin(); it != m_clientConnections.end(); ++it){
                delete *it;
            }
            m_clientConnections.clear();
            if(m_pError!=NULL){ delete m_pError; m_pError=NULL;}
        }

        //Connect via Zookeeper or directly.
        //Makes an initial connection to a drillbit. successful connect adds the first drillbit to the pool.
        connectionStatus_t connect(const char* connStr, DrillUserProperties* props);

        // Test whether the client is active. Returns true if any one of the underlying connections is active
        bool Active();

        // Closes all open connections. 
        void Close() ;

        // Returns the last error encountered by any of the underlying executing queries or connections
        DrillClientError* getError();

        // Submits a query to a drillbit. If more than one query is to be sent, we may choose a
        // a different drillbit in the pool. No more than m_maxConcurrentConnections will be allowed.
        // Connections once added to the pool will be removed only when the DrillClient is closed.
        DrillClientQueryResult* SubmitQuery(::exec::shared::QueryType t, const std::string& plan, pfnQueryResultsListener listener, void* listenerCtx);

        DrillClientPrepareHandle* PrepareQuery(const std::string& plan, pfnPreparedStatementListener listener, void* listenerCtx);
        DrillClientQueryResult* ExecuteQuery(const PreparedStatement& pstmt, pfnQueryResultsListener listener, void* listenerCtx);

        //Waits as long as any one drillbit connection has results pending
        void waitForResults();

        //Validates handshake only against the first drillbit connected to.
        connectionStatus_t validateHandshake(DrillUserProperties* props);

        void freeQueryResources(DrillClientQueryHandle* pQueryHandle);

        int getDrillbitCount() const { return m_drillbits.size();};
        
        meta::DrillMetadata* getMetadata();

        void freeMetadata(meta::DrillMetadata* metadata);

    private:
        
        std::string m_connectStr; 
        std::string m_lastQuery;
        
        // A list of all the current client connections. We choose a new one for every query. 
        // When picking a drillClientImpl to use, we see how many queries each drillClientImpl
        // is currently executing. If none,  
        std::vector<DrillClientImpl*> m_clientConnections; 
		boost::mutex m_poolMutex; // protect access to the vector
        // Use this to decide which drillbit to select next from the list of drillbits.
        size_t m_lastConnection;
		boost::mutex m_cMutex;

        // Number of queries executed so far. Can be used to select a new Drillbit from the pool.
        size_t m_queriesExecuted;

        size_t m_maxConcurrentConnections;

        bool m_bIsDirectConnection;

        DrillClientError* m_pError;

        connectionStatus_t handleConnError(connectionStatus_t status, std::string msg);
        // get a connection from the pool or create a new one. Return NULL if none is found
        DrillClientImpl* getOneConnection();

        std::vector<std::string> m_drillbits;

        boost::shared_ptr<DrillUserProperties> m_pUserProperties;//Keep a copy of user properties
};

} // namespace Drill

#endif
