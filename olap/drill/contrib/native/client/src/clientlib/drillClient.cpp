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
#include <stdlib.h>
#include "drill/common.hpp"
#include "drill/drillClient.hpp"
#include "drill/fieldmeta.hpp"
#include "drill/recordBatch.hpp"
#include "drill/userProperties.hpp"
#include "drillClientImpl.hpp"
#include "env.h"
#include "errmsgs.hpp"
#include "logger.hpp"
#include "Types.pb.h"

namespace Drill{

DrillClientInitializer::DrillClientInitializer(){
    GOOGLE_PROTOBUF_VERIFY_VERSION;
    srand(time(NULL));
}

DrillClientInitializer::~DrillClientInitializer(){
    google::protobuf::ShutdownProtobufLibrary();
}

RecordIterator::~RecordIterator(){
    if(m_pColDefs!=NULL){
        for(std::vector<Drill::FieldMetadata*>::iterator it=m_pColDefs->begin();
                it!=m_pColDefs->end();
                ++it){
            delete *it;
        }
    }
    delete this->m_pQueryResult;
    this->m_pQueryResult=NULL;
    if(this->m_pCurrentRecordBatch!=NULL){
        DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "Deleted last Record batch " << (void*) m_pCurrentRecordBatch << std::endl;)
        delete this->m_pCurrentRecordBatch; this->m_pCurrentRecordBatch=NULL;
    }
}

FieldDefPtr RecordIterator::getColDefs(){
    if(m_pQueryResult->hasError()){
        return DrillClientQueryResult::s_emptyColDefs;
    }

    if (this->m_pColDefs != NULL && !this->hasSchemaChanged()) {
    	return this->m_pColDefs;
    }

    //NOTE: if query is cancelled, return whatever you have. Client applications job to deal with it.
    if(this->m_pCurrentRecordBatch==NULL){
    	this->m_pQueryResult->waitForData();
    	if(m_pQueryResult->hasError()){
    		return DrillClientQueryResult::s_emptyColDefs;
    	}
    }
    if(this->hasSchemaChanged()){
    	if(m_pColDefs!=NULL){
    		for(std::vector<Drill::FieldMetadata*>::iterator it=m_pColDefs->begin();
    				it!=m_pColDefs->end();
    				++it){
    			delete *it;
    		}
    		m_pColDefs->clear();
    		//delete m_pColDefs; m_pColDefs=NULL;
    	}
    }
    FieldDefPtr pColDefs(  new std::vector<Drill::FieldMetadata*>);
    {   //lock after we come out of the  wait.
    	boost::lock_guard<boost::mutex> bufferLock(this->m_recordBatchMutex);
    	boost::shared_ptr< std::vector<Drill::FieldMetadata*> >  currentColDefs=DrillClientQueryResult::s_emptyColDefs;
    	if(this->m_pCurrentRecordBatch!=NULL){
    		currentColDefs=this->m_pCurrentRecordBatch->getColumnDefs();
    	}else{
    		// This is reached only when the first results have been received but
    		// the getNext call has not been made to retrieve the record batch
    		RecordBatch* pR=this->m_pQueryResult->peekNext();
    		if(pR!=NULL){
    			currentColDefs=pR->getColumnDefs();
    		}
    	}
    	for(std::vector<Drill::FieldMetadata*>::const_iterator it=currentColDefs->begin(); it!=currentColDefs->end(); ++it){
    		Drill::FieldMetadata* fmd= new Drill::FieldMetadata;
    		fmd->copy(*(*it));//Yup, that's 2 stars
    		pColDefs->push_back(fmd);
    	}
    }
    this->m_pColDefs = pColDefs;
    return this->m_pColDefs;
}

status_t RecordIterator::next(){
    status_t ret=QRY_SUCCESS;
    this->m_currentRecord++;
    if(this->m_pQueryResult->isCancelled()){
    	return QRY_CANCEL;
    }

    if(this->m_pCurrentRecordBatch==NULL || this->m_currentRecord==this->m_pCurrentRecordBatch->getNumRecords()){
    	boost::lock_guard<boost::mutex> bufferLock(this->m_recordBatchMutex);
    	if(this->m_pCurrentRecordBatch !=NULL){
    		DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "Deleted old Record batch " << (void*) m_pCurrentRecordBatch << std::endl;)
                		delete this->m_pCurrentRecordBatch; //free the previous record batch
    		this->m_pCurrentRecordBatch=NULL;
    	}
    	this->m_currentRecord=0;
    	this->m_pQueryResult->waitForData();
    	if(m_pQueryResult->hasError()){
    		return m_pQueryResult->getErrorStatus();
    	}
    	this->m_pCurrentRecordBatch=this->m_pQueryResult->getNext();
    	if(this->m_pCurrentRecordBatch != NULL){
    		DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "Fetched new Record batch " << std::endl;)
    	}else{
    		DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "No new Record batch found " << std::endl;)
    	}
    	if(this->m_pCurrentRecordBatch==NULL || this->m_pCurrentRecordBatch->getNumRecords()==0){
    		DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "No more data." << std::endl;)
                		ret = QRY_NO_MORE_DATA;
    	}else if(this->m_pCurrentRecordBatch->hasSchemaChanged()){
    		ret=QRY_SUCCESS_WITH_INFO;
    	}
    }

    return ret;
}

/* Gets the ith column in the current record. */
status_t RecordIterator::getCol(size_t i, void** b, size_t* sz){
    //TODO: check fields out of bounds without calling getColDefs
    //if(i>=getColDefs().size()) return QRY_OUT_OF_BOUNDS;
    //return raw byte buffer
    if(this->m_pQueryResult->isCancelled()){
    	return QRY_CANCEL;
    }
    const ValueVectorBase* pVector=this->m_pCurrentRecordBatch->getFields()[i]->getVector();
    if(!pVector->isNull(this->m_currentRecord)){
    	*b=pVector->getRaw(this->m_currentRecord);
    	*sz=pVector->getSize(this->m_currentRecord);
    }else{
    	*b=NULL;
    	*sz=0;
    }
    return QRY_SUCCESS;
}

/* true if ith column in the current record is NULL. */
bool RecordIterator::isNull(size_t i){
    if(this->m_pQueryResult->isCancelled()){
    	return false;
    }

    const ValueVectorBase* pVector=this->m_pCurrentRecordBatch->getFields()[i]->getVector();
    return pVector->isNull(this->m_currentRecord);
}

status_t RecordIterator::cancel(){
    this->m_pQueryResult->cancel();
    return QRY_CANCEL;
}

bool RecordIterator::hasSchemaChanged(){
    return m_currentRecord==0 && m_pCurrentRecordBatch!=NULL && m_pCurrentRecordBatch->hasSchemaChanged();
}

void RecordIterator::registerSchemaChangeListener(pfnSchemaListener l){
    assert(m_pQueryResult!=NULL);
    this->m_pQueryResult->registerSchemaChangeListener(l);
}

bool RecordIterator::hasError(){
    return m_pQueryResult->hasError();
}

const std::string& RecordIterator::getError(){
    return m_pQueryResult->getError()->msg;
}

DrillClientInitializer DrillClient::s_init;

DrillClientConfig DrillClient::s_config;

void DrillClient::initLogging(const char* path, logLevel_t l){
    if(path!=NULL) s_config.initLogging(path);
    s_config.setLogLevel(l);
}

DrillClient::DrillClient(){
    const char* enablePooledClient=std::getenv(ENABLE_CONNECTION_POOL_ENV);
    if(enablePooledClient!=NULL && atoi(enablePooledClient)!=0){
        this->m_pImpl=new PooledDrillClientImpl;
    }else{
        this->m_pImpl=new DrillClientImpl;
    }
}

DrillClient::~DrillClient(){
    delete this->m_pImpl;
}

connectionStatus_t DrillClient::connect(const char* connectStr, const char* defaultSchema){
    DrillUserProperties props;
    std::string schema(defaultSchema);
    props.setProperty(USERPROP_SCHEMA,  schema);
    if (defaultSchema != NULL) {
    	return connect(connectStr, static_cast<DrillUserProperties*>(NULL));
    }
    else {
    	return connect(connectStr, &props);
    }
}

connectionStatus_t DrillClient::connect(const char* connectStr, DrillUserProperties* properties){
    connectionStatus_t ret=CONN_SUCCESS;
    ret=this->m_pImpl->connect(connectStr, properties);
    if(ret==CONN_SUCCESS){
        ret=this->m_pImpl->validateHandshake(properties);
    }
    return ret;
}

bool DrillClient::isActive(){
    return this->m_pImpl->Active();
}

void DrillClient::close() {
    this->m_pImpl->Close();
}

status_t DrillClient::submitQuery(Drill::QueryType t, const std::string& plan, pfnQueryResultsListener listener, void* listenerCtx, QueryHandle_t* qHandle){
    ::exec::shared::QueryType castedType = static_cast< ::exec::shared::QueryType> (t);
    DrillClientQueryResult* pResult=this->m_pImpl->SubmitQuery(castedType, plan, listener, listenerCtx);
    *qHandle=static_cast<QueryHandle_t>(pResult);
    if(pResult==NULL){
        return (status_t)this->m_pImpl->getError()->status;
    }
    return QRY_SUCCESS;
}

RecordIterator* DrillClient::submitQuery(Drill::QueryType t, const std::string& plan, DrillClientError* err){
    RecordIterator* pIter=NULL;
    ::exec::shared::QueryType castedType = static_cast< ::exec::shared::QueryType> (t);
    DrillClientQueryResult* pResult=this->m_pImpl->SubmitQuery(castedType, plan, NULL, NULL);
    if(pResult){
        pIter=new RecordIterator(pResult);
    }
    return pIter;
}

status_t DrillClient::prepareQuery(const std::string& sql, pfnPreparedStatementListener listener, void* listenerCtx, QueryHandle_t* qHandle) {
	DrillClientPrepareHandle* pResult=this->m_pImpl->PrepareQuery(sql, listener, listenerCtx);
	*qHandle=static_cast<QueryHandle_t>(pResult);
	if(pResult==NULL){
		return static_cast<status_t>(this->m_pImpl->getError()->status);
	}
	return QRY_SUCCESS;
}

status_t DrillClient::executeQuery(const PreparedStatement& pstmt, pfnQueryResultsListener listener, void* listenerCtx, QueryHandle_t* qHandle) {
	DrillClientQueryResult* pResult=this->m_pImpl->ExecuteQuery(pstmt, listener, listenerCtx);
	*qHandle=static_cast<QueryHandle_t>(pResult);
	if(pResult==NULL){
		return static_cast<status_t>(this->m_pImpl->getError()->status);
	}
	return QRY_SUCCESS;
}

void DrillClient::cancelQuery(QueryHandle_t handle) {
	if (!handle) {
		return;
	}
	DrillClientQueryHandle* pHandle = static_cast<DrillClientQueryHandle*>(handle);
	pHandle->cancel();
}

void* DrillClient::getApplicationContext(QueryHandle_t handle){
    assert(handle!=NULL);
    return (static_cast<DrillClientQueryHandle*>(handle))->getApplicationContext();
}

status_t DrillClient::getQueryStatus(QueryHandle_t handle){
    assert(handle!=NULL);
    return static_cast<DrillClientQueryHandle*>(handle)->getQueryStatus();
}

std::string& DrillClient::getError(){
    return m_pImpl->getError()->msg;
}

const std::string& DrillClient::getError(QueryHandle_t handle){
    return static_cast<DrillClientQueryHandle*>(handle)->getError()->msg;
}

void DrillClient::waitForResults(){
    this->m_pImpl->waitForResults();
}

void DrillClient::registerSchemaChangeListener(QueryHandle_t* handle, pfnSchemaListener l){
	if (!handle) {
		return;
	}

	// Let's ensure that handle is really an instance of DrillClientQueryResult
	// by using dynamic_cast to verify. Since void is not a class, we first have
	// to static_cast to a DrillClientQueryHandle
	DrillClientQueryHandle* pHandle = static_cast<DrillClientQueryHandle*>(*handle);
	DrillClientQueryResult* result = dynamic_cast<DrillClientQueryResult*>(pHandle);

	if (result) {
        result->registerSchemaChangeListener(l);
    }
}

void DrillClient::freeQueryResources(QueryHandle_t* handle){
	this->m_pImpl->freeQueryResources(static_cast<DrillClientQueryHandle*>(*handle));
    *handle=NULL;
}

void DrillClient::freeRecordBatch(RecordBatch* pRecordBatch){
    delete pRecordBatch;
}

Metadata* DrillClient::getMetadata() {
    return this->m_pImpl->getMetadata();
}

void DrillClient::freeMetadata(Metadata** metadata) {
    this->m_pImpl->freeMetadata(static_cast<meta::DrillMetadata*>(*metadata));
    *metadata = NULL;
}
} // namespace Drill
