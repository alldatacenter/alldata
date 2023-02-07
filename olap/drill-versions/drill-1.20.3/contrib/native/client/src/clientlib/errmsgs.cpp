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
#include <assert.h>
#include <stdarg.h>
#include <stdio.h>
#include <boost/lexical_cast.hpp>
#include "errmsgs.hpp"

namespace Drill{

static Drill::ErrorMessages errorMessages[]={
    {ERR_NONE, 0, 0, "No Error."},
    {ERR_CONN_FAILURE, ERR_CATEGORY_CONN, 0, "Connection failure. Host:%s port:%s. Error: %s."},
    {ERR_CONN_EXCEPT, ERR_CATEGORY_CONN, 0, "Socket connection failure with the following exception: %s."},
    {ERR_CONN_UNKPROTO, ERR_CATEGORY_CONN, 0, "Unknown protocol: %s."},
    {ERR_CONN_RDFAIL, ERR_CATEGORY_CONN, 0, "Connection failed with error: %s."},
    {ERR_CONN_WFAIL, ERR_CATEGORY_CONN, 0, "Synchronous socket write failed with error: %s."},
    {ERR_CONN_ZOOKEEPER, ERR_CATEGORY_CONN, 0, "Zookeeper error. %s"},
    {ERR_CONN_NOHSHAKE, ERR_CATEGORY_CONN, 0, "Handshake failed because the server killed the connection. "
        "Expected RPC version %d."},
    {ERR_CONN_ZKFAIL, ERR_CATEGORY_CONN, 0, "Failed to connect to Zookeeper."},
    {ERR_CONN_ZKTIMOUT, ERR_CATEGORY_CONN, 0, "Timed out while waiting to connect."},
    {ERR_CONN_ZKERR, ERR_CATEGORY_CONN, 0, "Error in reading from Zookeeper (error code: %d)."},
    {ERR_CONN_ZKDBITERR, ERR_CATEGORY_CONN, 0, "Error in reading drillbit endpoint from Zookeeper (error code: %d)."},
    {ERR_CONN_ZKNODBIT, ERR_CATEGORY_CONN, 0, "No drillbit found with this Zookeeper."},
    {ERR_CONN_ZKNOAUTH, ERR_CATEGORY_CONN, 0, "Authentication failed."},
    {ERR_CONN_ZKEXP, ERR_CATEGORY_CONN, 0, "Session expired."},
    {ERR_CONN_HSHAKETIMOUT, ERR_CATEGORY_CONN, 0, "Handshake Timeout."},
    {ERR_CONN_BAD_RPC_VER, ERR_CATEGORY_CONN, 0, "Handshake failed because of a RPC version mismatch. "
        "Expected RPC version %d, got %d. [Server message was: (%s) %s]"},
    {ERR_CONN_AUTHFAIL, ERR_CATEGORY_CONN, 0, "User authentication failed (please check the username and password)."
        "[Server message was: (%s) %s]"},
    {ERR_CONN_UNKNOWN_ERR, ERR_CATEGORY_CONN, 0, "Handshake Failed due to an error on the server. [Server message was: (%s) %s]"},
    {ERR_CONN_NOCONN, ERR_CATEGORY_CONN, 0, "There is no connection to the server."},
    {ERR_CONN_ALREADYCONN, ERR_CATEGORY_CONN, 0, "This client is already connected to a server."},
    {ERR_CONN_NOCONNSTR, ERR_CATEGORY_CONN, 0, "Cannot connect if either host name or port number are empty."},
    {ERR_CONN_SSLCERTFAIL, ERR_CATEGORY_CONN, 0, "SSL certificate file %s could not be loaded (exception message: %s)."},
    {ERR_CONN_NOSOCKET, ERR_CATEGORY_CONN, 0, "Failed to open socket connection."},
    {ERR_CONN_NOSERVERAUTH, ERR_CATEGORY_CONN, 0, "Client needs a secure connection but server does not"
        " support any security mechanisms. Please contact an administrator. [Warn: This"
        " could be due to a bad configuration or a security attack is in progress.]"},
    {ERR_CONN_NOSERVERENC, ERR_CATEGORY_CONN, 0, "Client needs encryption but encryption is disabled on the server."
        " Please check connection parameters or contact administrator. [Warn: This"
        " could be due to a bad configuration or a security attack is in progress.]"},
    {ERR_CONN_SSL_GENERAL, ERR_CATEGORY_CONN, 0, "Encountered an exception during SSL handshake. [Details: %s]"},
    {ERR_CONN_SSL_CN, ERR_CATEGORY_CONN, 0, "SSL certificate host name verification failure. [Details: %s]" },
    {ERR_CONN_SSL_CERTVERIFY, ERR_CATEGORY_CONN, 0, "SSL certificate verification failed. [Details: %s]"},
    {ERR_CONN_SSL_PROTOVER, ERR_CATEGORY_CONN, 0, "Unsupported TLS protocol version. [Details: %s]" },
    {ERR_CONN_SSL_SNI, ERR_CATEGORY_CONN, 0, "Failed to set TLS SNI. Host: %s [Details: %s]"},
    {ERR_QRY_OUTOFMEM, ERR_CATEGORY_QRY, 0, "Out of memory."},
    {ERR_QRY_COMMERR, ERR_CATEGORY_QRY, 0, "Communication error. %s"},
    {ERR_QRY_INVREADLEN, ERR_CATEGORY_QRY, 0, "Internal Error: Received a message with an invalid read length."},
    {ERR_QRY_INVQUERYID, ERR_CATEGORY_QRY, 0, "Internal Error: Cannot find query Id in internal structure."},
    {ERR_QRY_INVRPCTYPE, ERR_CATEGORY_QRY, 0, "Unknown rpc type received from server:%d."},
    {ERR_QRY_OUTOFORDER, ERR_CATEGORY_QRY, 0, "Internal Error: Query result received before query id. Aborting ..."},
    {ERR_QRY_INVRPC, ERR_CATEGORY_QRY, 0, "Rpc Error: %s."},
    {ERR_QRY_TIMOUT, ERR_CATEGORY_QRY, 0, "Timed out waiting for server to respond."},
    {ERR_QRY_FAILURE, ERR_CATEGORY_QRY, 0, "Query execution error. Details:[ \n%s\n]"},
    {ERR_QRY_SELVEC2, ERR_CATEGORY_QRY, 0, "Receiving a selection_vector_2 from the server came as a complete surprise at this point"},
    {ERR_QRY_RESPFAIL, ERR_CATEGORY_QRY, 0, "Received a RESPONSE_FAILURE from the server."},
    {ERR_QRY_UNKQRYSTATE, ERR_CATEGORY_QRY, 0, "Got an unknown query state message from the server."},
    {ERR_QRY_UNKQRY, ERR_CATEGORY_QRY, 0, "Query not found on server. It might have been terminated already."},
    {ERR_QRY_CANCELED, ERR_CATEGORY_QRY, 0, "Query has been cancelled"},
    {ERR_QRY_COMPLETED, ERR_CATEGORY_QRY, 0, "Query completed."},
    {ERR_QRY_16, ERR_CATEGORY_QRY, 0, "Query Failed."},
    {ERR_QRY_17, ERR_CATEGORY_QRY, 0, "Query Failed."},
    {ERR_QRY_18, ERR_CATEGORY_QRY, 0, "Query Failed."},
    {ERR_QRY_19, ERR_CATEGORY_QRY, 0, "Query Failed."},
    {ERR_QRY_20, ERR_CATEGORY_QRY, 0, "Query Failed."},
};

std::string getMessage(uint32_t msgId, ...){
    char str[10240];
    std::string s;
    assert((ERR_NONE <= msgId) && (msgId < ERR_QRY_MAX));
    va_list args;
    va_start (args, msgId);
    vsnprintf (str, sizeof(str), errorMessages[msgId-DRILL_ERR_START].msgFormatStr, args);
    va_end (args);
    s=std::string("[")+boost::lexical_cast<std::string>(msgId)+std::string("]")+str;
    return s;
}

}// namespace Drill
