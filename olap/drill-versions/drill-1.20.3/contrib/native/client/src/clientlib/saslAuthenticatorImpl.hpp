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
#ifndef DRILLCLIENT_SASLAUTHENTICATORIMPL_HPP
#define DRILLCLIENT_SASLAUTHENTICATORIMPL_HPP

#include <string>
#include <map>
#include <vector>
#include "drill/drillClient.hpp"
#include "UserBitShared.pb.h"
#include "utils.hpp"

#ifdef WIN32 
#include "sasl.h"
#include "saslplug.h"
#else
#include "sasl/sasl.h"
#include "sasl/saslplug.h"
#endif

namespace Drill {

class SaslAuthenticatorImpl {

public:

    SaslAuthenticatorImpl(const DrillUserProperties *const properties);

    ~SaslAuthenticatorImpl();

    int init(const std::vector<std::string>& mechanisms, exec::shared::SaslMessage& response,
             EncryptionContext* const encryptCtxt);

    int step(const exec::shared::SaslMessage& challenge, exec::shared::SaslMessage& response) const;

    int verifyAndUpdateSaslProps();

    int wrap(const char* dataToWrap, const int& dataToWrapLen, const char** output, uint32_t& wrappedLen);

    int unwrap(const char* dataToUnWrap, const int& dataToUnWrapLen, const char** output, uint32_t& unWrappedLen);

    const std::string &getAuthMechanismName() const;

    const char *getErrorMessage(int errorCode);

    static const std::string KERBEROS_SIMPLE_NAME;

    static const std::string PLAIN_NAME;

private:

    static const std::map<std::string, std::string> MECHANISM_MAPPING;

    static boost::mutex s_mutex;
    static bool s_initialized;

    const DrillUserProperties *const m_pUserProperties;
    sasl_conn_t *m_pConnection;
    std::vector<sasl_callback_t> m_callbacks;
    std::string m_username;
    sasl_secret_t *m_ppwdSecret;
    EncryptionContext *m_pEncryptCtxt;
    std::string m_authMechanismName; // used for debugging/error messages

private:
    static int passwordCallback(sasl_conn_t *conn, void *context, int id, sasl_secret_t **psecret);

    static int userNameCallback(void *context, int id, const char **result, unsigned int *len);


    void setSecurityProps() const;
};

} /* namespace Drill */

#endif //DRILLCLIENT_SASLAUTHENTICATORIMPL_HPP
