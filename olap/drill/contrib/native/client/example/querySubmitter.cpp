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
#include <boost/thread.hpp>
#include <boost/algorithm/string/join.hpp>
#include "drill/drillc.hpp"

struct Option{
    char name[32];
    char desc[128];
    bool required;
} qsOptions[] = {
    {"plan", "Plan files separated by semicolons", false},
    {"query", "Query strings, separated by semicolons", false},
    {"type", "Query type [physical|logical|sql|server]", true},
    {"connectStr", "Connect string", true},
    {"schema", "Default schema", false},
    {"api", "API type [sync|async|meta]", true},
    {"logLevel", "Logging level [trace|debug|info|warn|error|fatal]", false},
    {"testCancel", "Cancel the query after the first record batch.", false},
    {"syncSend", "Send query only after previous result is received", false},
    {"hshakeTimeout", "Handshake timeout (second).", false},
    {"queryTimeout", "Query timeout (second).", false},
    {"heartbeatFrequency", "Heartbeat frequency (second). Disabled if set to 0.", false},
    {"user", "Username", false},
    {"password", "Password", false},
    {"saslPluginPath", "Path to where SASL plugins are installed", false},
    {"service_host", "Service host for Kerberos", false},
    {"service_name", "Service name for Kerberos", false},
    {"auth", "Authentication mechanism to use", false},
    {"sasl_encrypt", "Negotiate for encrypted connection", false},
    {"enableSSL", "Enable SSL", false},
    {"TLSProtocol", "TLS protocol version", false},
    {"certFilePath", "Path to SSL certificate file", false},
    {"disableHostnameVerification", "disable host name verification", false},
    {"disableCertVerification", "disable certificate verification", false},
    {"useSystemTrustStore", "[Windows only]. Use the system truststore.", false},
    {"CustomSSLCtxOptions", "The custom SSL CTX Options", false},
    {"supportComplexTypes", "Toggle for supporting complex types", false},
    {"hostnameOverride", "Override the SSL server hostname", false}
};

std::map<std::string, std::string> qsOptionValues;

bool bTestCancel=false;
bool bSyncSend=false;


Drill::status_t SchemaListener(void* ctx, Drill::FieldDefPtr fields, Drill::DrillClientError* err){
    if(!err){
        std::cout<< "SCHEMA CHANGE DETECTED:" << std::endl;
        for(size_t i=0; i<fields->size(); i++){
            std::string name= fields->at(i)->getName();
            std::cout << name << "\t";
        }
        std::cout << std::endl;
        return Drill::QRY_SUCCESS ;
    }else{
        std::cerr<< "ERROR: " << err->msg << std::endl;
        return Drill::QRY_FAILURE;
    }
}

boost::mutex listenerMutex;
Drill::status_t QueryResultsListener(void* ctx, Drill::RecordBatch* b, Drill::DrillClientError* err){
    // Invariant:
    // (received an record batch and err is NULL)
    // or
    // (received query state message passed by `err` and b is NULL)
    boost::lock_guard<boost::mutex> listenerLock(listenerMutex);
    if(!err){
        if(b!=NULL){
            b->print(std::cout, 0); // print all rows
            std::cout << "DATA RECEIVED ..." << std::endl;
            delete b; // we're done with this batch, we can delete it
            if(bTestCancel){
                return Drill::QRY_FAILURE;
            }else{
                return Drill::QRY_SUCCESS ;
            }
        }else{
            std::cout << "Query Complete." << std::endl;
            return Drill::QRY_SUCCESS;
		}
    }else{
        assert(b==NULL);
        switch(err->status) {
            case Drill::QRY_COMPLETED:
            case Drill::QRY_CANCELED:
                std::cerr<< "INFO: " << err->msg << std::endl;
                return Drill::QRY_SUCCESS;
            default:
                std::cerr<< "ERROR: " << err->msg << std::endl;
                return Drill::QRY_FAILURE;
        }
    }
}

void print(const Drill::FieldMetadata* pFieldMetadata, void* buf, size_t sz){
    common::MinorType type = pFieldMetadata->getMinorType();
    common::DataMode mode = pFieldMetadata->getDataMode();
    unsigned char printBuffer[10240];
    memset(printBuffer, 0, sizeof(printBuffer));
    switch (type) {
        case common::BIGINT:
            switch (mode) {
                case common::DM_REQUIRED:
                    sprintf((char*)printBuffer, "%lld", *(uint64_t*)buf);
                    break;
                case common::DM_OPTIONAL:
                    break;
                case common::DM_REPEATED:
                    break;
            }
            break;
        case common::VARBINARY:
            switch (mode) {
                case common::DM_REQUIRED:
                    memcpy(printBuffer, buf, sz);
                    break;
                case common::DM_OPTIONAL:
                    break;
                case common::DM_REPEATED:
                    break;
            }
            break;
        case common::VARCHAR:
            switch (mode) {
                case common::DM_REQUIRED:
                    memcpy(printBuffer, buf, sz);
                    break;
                case common::DM_OPTIONAL:
                    break;
                case common::DM_REPEATED:
                    break;
            }
            break;
        default:
            //memcpy(printBuffer, buf, sz);
            sprintf((char*)printBuffer, "NIY");
            break;
    }
    printf("%s\t", (char*)printBuffer);
    return;
}

void printUsage(){
    std::cerr<<"Usage: querySubmitter ";
    for(int j=0; j<sizeof(qsOptions)/sizeof(qsOptions[0]) ;j++){
        std::cerr<< " "<< qsOptions[j].name <<"="  << "[" <<qsOptions[j].desc <<"]" ;
    }
    std::cerr<<std::endl;
}

int parseArgs(int argc, char* argv[]){
    bool error=false;
    for(int i=1; i<argc; i++){
        char*a =argv[i];
        char* o=strtok(a, "=");
        char*v=strtok(NULL, "");

        bool found=false;
        for(int j=0; j<sizeof(qsOptions)/sizeof(qsOptions[0]) ;j++){
            if(!strcmp(qsOptions[j].name, o)){
                found=true; break;
            }
        }
        if(!found){
            std::cerr<< "Unknown option:"<< o <<". Ignoring" << std::endl;
            continue;
        }

        if(v==NULL){
            std::cerr<< ""<< qsOptions[i].name << " [" <<qsOptions[i].desc <<"] " << "requires a parameter."  << std::endl;
            error=true;
        }
        qsOptionValues[o]=v;
    }

    for(int j=0; j<sizeof(qsOptions)/sizeof(qsOptions[0]) ;j++){
        if(qsOptions[j].required ){
            if(qsOptionValues.find(qsOptions[j].name) == qsOptionValues.end()){
                std::cerr<< ""<< qsOptions[j].name << " [" <<qsOptions[j].desc <<"] " << "is required." << std::endl;
                error=true;
            }
        }
    }
    if(error){
        printUsage();
        exit(1);
    }
    return 0;
}

void parseUrl(std::string& url, std::string& protocol, std::string& host, std::string& port){
    char u[1024];
    strcpy(u,url.c_str());
    char* z=strtok(u, "=");
    char* h=strtok(NULL, ":");
    char* p=strtok(NULL, ":");
    protocol=z; host=h; port=p;
}

std::vector<std::string> &splitString(const std::string& s, char delim, std::vector<std::string>& elems){
    std::stringstream ss(s);
    std::string item;
    while (std::getline(ss, item, delim)){
        elems.push_back(item);
    }
    return elems;
}

int readPlans(const std::string& planList, std::vector<std::string>& plans){
    std::vector<std::string> planFiles;
    std::vector<std::string>::iterator iter;
    splitString(planList, ';', planFiles);
    for(iter = planFiles.begin(); iter != planFiles.end(); iter++) {
        std::ifstream f((*iter).c_str());
        std::string plan((std::istreambuf_iterator<char>(f)), (std::istreambuf_iterator<char>()));
        std::cout << "plan:" << plan << std::endl;
        plans.push_back(plan);
    }
    return 0;
}

int readQueries(const std::string& queryList, std::vector<std::string>& queries){
    splitString(queryList, ';', queries);
    return 0;
}

bool validate(const std::string& type, const std::string& query, const std::string& plan){
	if (type != "sync" || type != "async") {
		return true;
	}
    if(query.empty() && plan.empty()){
        std::cerr<< "Either query or plan must be specified"<<std::endl;
        return false;    }
        if(type=="physical" || type == "logical" ){
            if(plan.empty()){
                std::cerr<< "A logical or physical  plan must be specified"<<std::endl;
                return false;
            }
        }else
            if(type=="sql"){
                if(query.empty()){
                    std::cerr<< "A drill SQL query must be specified"<<std::endl;
                    return false;
                }
            }else{
                std::cerr<< "Unknown query type: "<< type << std::endl;
                return false;
            }
        return true;
}

Drill::logLevel_t getLogLevel(const char *s){
    if(s!=NULL){
        if(!strcmp(s, "trace")) return Drill::LOG_TRACE;
        if(!strcmp(s, "debug")) return Drill::LOG_DEBUG;
        if(!strcmp(s, "info")) return Drill::LOG_INFO;
        if(!strcmp(s, "warn")) return Drill::LOG_WARNING;
        if(!strcmp(s, "error")) return Drill::LOG_ERROR;
        if(!strcmp(s, "fatal")) return Drill::LOG_FATAL;
    }
    return Drill::LOG_ERROR;
}

int main(int argc, char* argv[]) {
    try {

        parseArgs(argc, argv);

        std::vector<std::string*> queries;

        std::string connectStr=qsOptionValues["connectStr"];
        std::string schema=qsOptionValues["schema"];
        std::string queryList=qsOptionValues["query"];
        std::string planList=qsOptionValues["plan"];
        std::string api=qsOptionValues["api"];
        std::string type_str=qsOptionValues["type"];
        std::string logLevel=qsOptionValues["logLevel"];
        std::string testCancel=qsOptionValues["testCancel"];
        std::string syncSend=qsOptionValues["syncSend"];
        std::string hshakeTimeout=qsOptionValues["hshakeTimeout"];
        std::string queryTimeout=qsOptionValues["queryTimeout"];
        std::string heartbeatFrequency=qsOptionValues["heartbeatFrequency"];
        std::string user=qsOptionValues["user"];
        std::string password=qsOptionValues["password"];
        std::string saslPluginPath=qsOptionValues["saslPluginPath"];
        std::string sasl_encrypt=qsOptionValues["sasl_encrypt"];
        std::string serviceHost=qsOptionValues["service_host"];
        std::string serviceName=qsOptionValues["service_name"];
        std::string auth=qsOptionValues["auth"];
        std::string enableSSL=qsOptionValues["enableSSL"];
        std::string tlsProtocol=qsOptionValues["TLSProtocol"];
        std::string certFilePath=qsOptionValues["certFilePath"];
        std::string disableHostnameVerification=qsOptionValues["disableHostnameVerification"];
        std::string disableCertVerification=qsOptionValues["disableCertVerification"];
        std::string useSystemTrustStore = qsOptionValues["useSystemTrustStore"];
        std::string customSSLOptions = qsOptionValues["CustomSSLCtxOptions"];
        std::string supportComplexTypes = qsOptionValues["supportComplexTypes"];
        std::string hostnameOverride = qsOptionValues["hostnameOverride"];

        Drill::QueryType type;

        if(!validate(type_str, queryList, planList)){
            exit(1);
        }

        Drill::logLevel_t l=getLogLevel(logLevel.c_str());

        std::vector<std::string> queryInputs;
        if(type_str=="sql" ){
            readQueries(queryList, queryInputs);
            type=Drill::SQL;
        }else if(type_str=="physical" ){
            readPlans(planList, queryInputs);
            type=Drill::PHYSICAL;
        }else if(type_str == "logical"){
            readPlans(planList, queryInputs);
            type=Drill::LOGICAL;
        }else{
            readQueries(queryList, queryInputs);
            type=Drill::SQL;
        }

        bTestCancel = !strcmp(testCancel.c_str(), "true")?true:false;
        bSyncSend = !strcmp(syncSend.c_str(), "true")?true:false;

        std::vector<std::string>::iterator queryInpIter;

        std::vector<Drill::RecordIterator*> recordIterators;
        std::vector<Drill::RecordIterator*>::iterator recordIterIter;

        std::vector<Drill::QueryHandle_t> queryHandles;
        std::vector<Drill::QueryHandle_t>::iterator queryHandleIter;

        Drill::DrillClient client;
#if defined _WIN32 || defined _WIN64
        TCHAR tempPath[MAX_PATH];
        GetTempPath(MAX_PATH, tempPath);
		char logpathPrefix[MAX_PATH + 128];
		strcpy(logpathPrefix,tempPath);
		strcat(logpathPrefix, "\\drillclient");
#else
		const char* logpathPrefix = "/var/log/drill/drillclient";
#endif
		// To log to file
        Drill::DrillClient::initLogging(logpathPrefix, l);
        // To log to stderr
        //Drill::DrillClient::initLogging(NULL, l);

        int nQueries=queryInputs.size();
        Drill::DrillClientConfig::setBufferLimit(nQueries*2*1024*1024); // 2MB per query. The size of a record batch may vary, but is unlikely to exceed the 256 MB which is the default. 

        if(!hshakeTimeout.empty()){
            Drill::DrillClientConfig::setHandshakeTimeout(atoi(hshakeTimeout.c_str()));
        }
        if (!queryTimeout.empty()){
            Drill::DrillClientConfig::setQueryTimeout(atoi(queryTimeout.c_str()));
        }
        if(!heartbeatFrequency.empty()) {
            Drill::DrillClientConfig::setHeartbeatFrequency(atoi(heartbeatFrequency.c_str()));
        }
        if (!saslPluginPath.empty()){
            Drill::DrillClientConfig::setSaslPluginPath(saslPluginPath.c_str());
        }

        Drill::DrillUserProperties props;
        if(schema.length()>0){
            props.setProperty(USERPROP_SCHEMA, schema);
        }
        if(user.length()>0){
            props.setProperty(USERPROP_USERNAME, user);
        }
        if(password.length()>0){
            props.setProperty(USERPROP_PASSWORD, password);
        }
        if(sasl_encrypt.length()>0){
            props.setProperty(USERPROP_SASL_ENCRYPT, sasl_encrypt);
        }
        if(serviceHost.length()>0){
            props.setProperty(USERPROP_SERVICE_HOST, serviceHost);
        }
        if(serviceName.length()>0){
            props.setProperty(USERPROP_SERVICE_NAME, serviceName);
        }
        if(auth.length()>0){
            props.setProperty(USERPROP_AUTH_MECHANISM, auth);
        }
        if(enableSSL.length()>0){
            props.setProperty(USERPROP_USESSL, enableSSL);
			if (enableSSL == "true" && certFilePath.length() <= 0 && useSystemTrustStore.length() <= 0){
                std::cerr<< "SSL is enabled but no certificate or truststore provided. " << std::endl;
                return -1;
            }
            props.setProperty(USERPROP_TLSPROTOCOL, tlsProtocol);
            props.setProperty(USERPROP_CERTFILEPATH, certFilePath);
            props.setProperty(USERPROP_DISABLE_HOSTVERIFICATION, disableHostnameVerification);
            props.setProperty(USERPROP_DISABLE_CERTVERIFICATION, disableCertVerification);
			if (useSystemTrustStore.length() > 0){
				props.setProperty(USERPROP_USESYSTEMTRUSTSTORE, useSystemTrustStore);
			}
            if (customSSLOptions.length() > 0){
                props.setProperty(USERPROP_CUSTOM_SSLCTXOPTIONS, customSSLOptions);
            }
        }
        if (supportComplexTypes.length() > 0){
            props.setProperty(USERPROP_SUPPORT_COMPLEX_TYPES, supportComplexTypes);
        }
        if (hostnameOverride.length() > 0) {
            props.setProperty(USERPROP_HOSTNAME_OVERRIDE, hostnameOverride);
        }

        if(client.connect(connectStr.c_str(), &props)!=Drill::CONN_SUCCESS){
            std::cerr<< "Failed to connect with error: "<< client.getError() << " (Using:"<<connectStr<<")"<<std::endl;
            return -1;
        }
        std::cout<< "Connected!\n" << std::endl;
        if(api=="meta") {
        	Drill::Metadata* metadata = client.getMetadata();
        	if (metadata) {
        		std::cout << "Connector:" << std::endl;
        		std::cout << "\tname:" << metadata->getConnectorName() << std::endl;
        		std::cout << "\tversion:" << metadata->getConnectorVersion() << std::endl;
        		std::cout << std::endl;
        		std::cout << "Server:" << std::endl;
        		std::cout << "\tname:" << metadata->getServerName() << std::endl;
        		std::cout << "\tversion:" << metadata->getServerVersion() << std::endl;
        		std::cout << std::endl;
        		std::cout << "Metadata:" << std::endl;
        		std::cout << "\tall tables are selectable: " << metadata->areAllTableSelectable() << std::endl;
        		std::cout << "\tcatalog separator: " << metadata->getCatalogSeparator() << std::endl;
        		std::cout << "\tcatalog term: " << metadata->getCatalogTerm() << std::endl;
        		std::cout << "\tCOLLATE support: " << metadata->getCollateSupport() << std::endl;
        		std::cout << "\tcorrelation names: " << metadata->getCorrelationNames() << std::endl;
        		std::cout << "\tdate time functions: " << boost::algorithm::join(metadata->getDateTimeFunctions(), ", ") << std::endl;
        		std::cout << "\tdate time literals support: " << metadata->getDateTimeLiteralsSupport() << std::endl;
        		std::cout << "\tGROUP BY support: " << metadata->getGroupBySupport() << std::endl;
        		std::cout << "\tidentifier case: " << metadata->getIdentifierCase() << std::endl;
        		std::cout << "\tidentifier quote string: " << metadata->getIdentifierQuoteString() << std::endl;
        		std::cout << "\tmax binary literal length: " << metadata->getMaxBinaryLiteralLength() << std::endl;
        		std::cout << "\tmax catalog name length: " << metadata->getMaxCatalogNameLength() << std::endl;
        		std::cout << "\tmax char literal length: " << metadata->getMaxCharLiteralLength() << std::endl;
        		std::cout << "\tmax column name length: " << metadata->getMaxColumnNameLength() << std::endl;
        		std::cout << "\tmax columns in GROUP BY: " << metadata->getMaxColumnsInGroupBy() << std::endl;
        		std::cout << "\tmax columns in ORDER BY: " << metadata->getMaxColumnsInOrderBy() << std::endl;
        		std::cout << "\tmax columns in SELECT: " << metadata->getMaxColumnsInSelect() << std::endl;
        		std::cout << "\tmax cursor name length: " << metadata->getMaxCursorNameLength() << std::endl;
        		std::cout << "\tmax logical lob size: " << metadata->getMaxLogicalLobSize() << std::endl;
        		std::cout << "\tmax row size: " << metadata->getMaxRowSize() << std::endl;
        		std::cout << "\tmax schema name length: " << metadata->getMaxSchemaNameLength() << std::endl;
        		std::cout << "\tmax statement length: " << metadata->getMaxStatementLength() << std::endl;
        		std::cout << "\tmax statements: " << metadata->getMaxStatements() << std::endl;
        		std::cout << "\tmax table name length: " << metadata->getMaxTableNameLength() << std::endl;
        		std::cout << "\tmax tables in SELECT: " << metadata->getMaxTablesInSelect() << std::endl;
        		std::cout << "\tmax user name length: " << metadata->getMaxUserNameLength() << std::endl;
        		std::cout << "\tNULL collation: " << metadata->getNullCollation() << std::endl;
        		std::cout << "\tnumeric functions: " << boost::algorithm::join(metadata->getNumericFunctions(), ", ") << std::endl;
        		std::cout << "\tOUTER JOIN support: " << metadata->getOuterJoinSupport() << std::endl;
        		std::cout << "\tquoted identifier case: " << metadata->getQuotedIdentifierCase() << std::endl;
        		std::cout << "\tSQL keywords: " << boost::algorithm::join(metadata->getSQLKeywords(), ",") << std::endl;
        		std::cout << "\tschema term: " << metadata->getSchemaTerm() << std::endl;
        		std::cout << "\tsearch escape string: " << metadata->getSearchEscapeString() << std::endl;
        		std::cout << "\tspecial characters: " << metadata->getSpecialCharacters() << std::endl;
        		std::cout << "\tstring functions: " << boost::algorithm::join(metadata->getStringFunctions(), ",") << std::endl;
        		std::cout << "\tsub query support: " << metadata->getSubQuerySupport() << std::endl;
        		std::cout << "\tsystem functions: " << boost::algorithm::join(metadata->getSystemFunctions(), ",") << std::endl;
        		std::cout << "\ttable term: " << metadata->getTableTerm() << std::endl;
        		std::cout << "\tUNION support: " << metadata->getUnionSupport() << std::endl;
        		std::cout << "\tBLOB included in max row size: " << metadata->isBlobIncludedInMaxRowSize() << std::endl;
        		std::cout << "\tcatalog at start: " << metadata->isCatalogAtStart() << std::endl;
        		std::cout << "\tcolumn aliasing supported: " << metadata->isColumnAliasingSupported() << std::endl;
        		std::cout << "\tLIKE escape clause supported: " << metadata->isLikeEscapeClauseSupported() << std::endl;
        		std::cout << "\tNULL plus non NULL equals to NULL: " << metadata->isNullPlusNonNullNull() << std::endl;
        		std::cout << "\tread-only: " << metadata->isReadOnly() << std::endl;
        		std::cout << "\tSELECT FOR UPDATE supported: " << metadata->isSelectForUpdateSupported() << std::endl;
        		std::cout << "\ttransaction supported: " << metadata->isTransactionSupported() << std::endl;
        		std::cout << "\tunrelated columns in ORDER BY supported: " << metadata->isUnrelatedColumnsInOrderBySupported() << std::endl;

        		client.freeMetadata(&metadata);
        	} else {
        		std::cerr << "Cannot get metadata:" << client.getError() << std::endl;
        	}
        } else if(api=="sync"){
            Drill::DrillClientError* err=NULL;
            Drill::status_t ret;
            int nQueries=0;
            for(queryInpIter = queryInputs.begin(); queryInpIter != queryInputs.end(); queryInpIter++) {
                Drill::RecordIterator* pRecIter = client.submitQuery(type, *queryInpIter, err);
                if(pRecIter!=NULL){
                    recordIterators.push_back(pRecIter);
                    nQueries++;
                }
            }
            Drill::DrillClientConfig::setBufferLimit(nQueries*2*1024*1024); // 2MB per query. Allows us to hold at least two record batches.
            size_t row=0;
            for(recordIterIter = recordIterators.begin(); recordIterIter != recordIterators.end(); recordIterIter++) {
                // get fields.
                row=0;
                Drill::RecordIterator* pRecIter=*recordIterIter;
                Drill::FieldDefPtr fields= pRecIter->getColDefs();
                while((ret=pRecIter->next()), (ret==Drill::QRY_SUCCESS || ret==Drill::QRY_SUCCESS_WITH_INFO) && !pRecIter->hasError()){
                    fields = pRecIter->getColDefs();
                    row++;
                    if( (ret==Drill::QRY_SUCCESS_WITH_INFO  && pRecIter->hasSchemaChanged() )|| ( row%100==1)){
                        for(size_t i=0; i<fields->size(); i++){
                            std::string name= fields->at(i)->getName();
                            printf("%s\t", name.c_str());
                        }
                        printf("\n");
                    }
                    printf("ROW: %ld\t", row);
                    for(size_t i=0; i<fields->size(); i++){
                        void* pBuf; size_t sz;
                        pRecIter->getCol(i, &pBuf, &sz);
                        print(fields->at(i), pBuf, sz);
                    }
                    printf("\n");
                    if(bTestCancel && row%100==1){
                        pRecIter->cancel();
                        printf("Application cancelled the query.\n");
                    }
                }
                if(ret!=Drill::QRY_NO_MORE_DATA && ret!=Drill::QRY_CANCEL){
                    std::cerr<< pRecIter->getError() << std::endl;
                }
                client.freeQueryIterator(&pRecIter);
            }
            client.waitForResults();
        }else{
            if(bSyncSend){
                for(queryInpIter = queryInputs.begin(); queryInpIter != queryInputs.end(); queryInpIter++) {
                    Drill::QueryHandle_t qryHandle;
                    client.submitQuery(type, *queryInpIter, QueryResultsListener, NULL, &qryHandle);
                    client.registerSchemaChangeListener(&qryHandle, SchemaListener);
                    
                     if(bTestCancel) {
                        // Send cancellation request after 5seconds
                        boost::this_thread::sleep(boost::posix_time::milliseconds(1000));
                        std::cout<< "\n Cancelling query: " << *queryInpIter << "\n" << std::endl;
                        client.cancelQuery(qryHandle);
                    } else {
                        client.waitForResults();
                    }

                    client.freeQueryResources(&qryHandle);
                }

            }else{
                for(queryInpIter = queryInputs.begin(); queryInpIter != queryInputs.end(); queryInpIter++) {
                    Drill::QueryHandle_t qryHandle;
                    client.submitQuery(type, *queryInpIter, QueryResultsListener, NULL, &qryHandle);
                    client.registerSchemaChangeListener(&qryHandle, SchemaListener);
                    queryHandles.push_back(qryHandle);
                }
                client.waitForResults();
                for(queryHandleIter = queryHandles.begin(); queryHandleIter != queryHandles.end(); queryHandleIter++) {
                    client.freeQueryResources(&*queryHandleIter);
                }
            }
        }
        client.close();
    } catch (std::exception& e) {
        std::cerr << e.what() << std::endl;
    }

    return 0;
}

