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

#include <map>
#include <vector>
#include <memory>
#include <mutex>
#include <sasl/sasl.h>
#include <common/types.h>

namespace DB
{
// using for TSaslClientTransport with Thrift

namespace SaslCommon
{

static const std::string KERBEROS_MECHANISM = "GSSAPI";

static std::mutex sasl_mutex;
static bool sasl_inited = false; 

static std::vector<sasl_callback_t> GENERAL_CALLBACKS;   // Applies to all connections
static std::vector<sasl_callback_t> KERBEROS_CALLBACKS;     // kerberos connections

};

class SaslClient
{
public:
    SaslClient(
        const String & service,
        const String & server_fqdn,
        const String & authorization_id = "",
        std::map<String, String> props = {},
        const String & mechanisms = SaslCommon::KERBEROS_MECHANISM,
        sasl_callback_t* callbacks = SaslCommon::KERBEROS_CALLBACKS.data());

    ~SaslClient();

    void setupSaslContext();

    void resetSaslContext();

    void disposeSaslContext();

    static void saslInit(sasl_callback_t* callbacks);

    // Only use once at all severs finish
    static void saslDone() {sasl_done();}

    // Evaluates the challenge data and generates a response
    uint8_t * evaluateChallengeOrResponse(const uint8_t * challenge, const uint32_t & len, uint32_t * resLen);

    uint8_t * unwrap(const uint8_t * incoming, const int & offset, const uint32_t & len, uint32_t * outLen);

    uint8_t * wrap(const uint8_t * outgoing, int offset, const uint32_t & len, uint32_t * outLen);

    // Returns the IANA-registered mechanism name of this SASL client
    String getMechanismName();

    //Retrieves the negotiated property
    String getNegotiatedProperty(const String& prop_name);

    // Returns the username from the underlying sasl connection
    String getUsername();

    //Determines whether this mechanism has an optional initial response.
    bool hasInitialResponse();

    bool isComplete() const { return auth_completed;}

    static void setupSaslClientWithKerberos();
private:
    // Name of service
    String service;
    // FQDN of server in use or server connect to
    String server_fqdn;
    // TODO: setup security property
    String authentication_id;
    // List of possible mechanisms
    String mechanism_lists;
    // Callbacks to provide to the Cyrus-SASL
    sasl_callback_t * sasl_callbacks;

    bool client_started;
    
    bool auth_completed;

    sasl_conn_t * sasl_connection;

    String chosen_mechanism;
};

}
