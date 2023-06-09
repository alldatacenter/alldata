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

#include <Access/SaslClient.h>
#include <Common/Exception.h>
#include <common/logger_useful.h>
#include <common/types.h>

#include <cstddef>
#include <memory>
#include <mutex>
#include <sstream>
#include <string>
#include <boost/algorithm/string.hpp>

#include <sasl/sasl.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int SASL_ERROR;
    extern const int NOT_IMPLEMENTED;
}

namespace SaslCommon
{
    // SASL_CB_LOG
    static int saslLogCallbacks(void * context, int level, const char * message)
    {
        String auth_context = reinterpret_cast<char *>(context);
        auto * log = &Poco::Logger::get(auth_context);
        const String auth_message(message);

        if (!message)
            return SASL_BADPARAM;

        switch (level)
        {
            case SASL_LOG_NONE: // "Don't log anything"
            case SASL_LOG_PASS: // "Traces... including passwords" - don't log!
                break;
            case SASL_LOG_ERR: // "Unusual errors"
            case SASL_LOG_FAIL: // "Authentication failures"
            case SASL_LOG_WARN: // "Non-fatal warnings"
                LOG_WARNING(log, "SASL Message" + auth_message);
                break;
            case SASL_LOG_NOTE: // "More verbose than WARN"

                break;
            case SASL_LOG_DEBUG: // "More verbose than NOTE"
                LOG_DEBUG(log, "SASL Message" + auth_message);
                break;
            case SASL_LOG_TRACE: // "Traces of internal protocols"
            default:
                LOG_TRACE(log, "SASL Message" + auth_message);
                break;
        }
        return SASL_OK;
    }

    static int saslUserCallbacks(void *, int, const char ** result, unsigned * len)
    {
        // Setting the username to the empty string causes the remote end to use the
        // clients Kerberos principal, which is correct.
        *result = "";
        if (len != nullptr)
            *len = 0;
        return SASL_OK;
    }

    static void setupGeneralCallbacks()
    {   
        String context("General");

        GENERAL_CALLBACKS.resize(2);

        GENERAL_CALLBACKS[0].id = SASL_CB_LOG;
        GENERAL_CALLBACKS[0].proc = reinterpret_cast<int (*)()>(&saslLogCallbacks);
        GENERAL_CALLBACKS[0].context = reinterpret_cast<void *>(context.data());

        GENERAL_CALLBACKS[1].id = SASL_CB_LIST_END;
        GENERAL_CALLBACKS[1].proc = nullptr;
        GENERAL_CALLBACKS[1].context = nullptr;

    }

    static void setupKerberosCallbacks()
    {       
        String context("KerBeros");

        KERBEROS_CALLBACKS.resize(3);

        KERBEROS_CALLBACKS[0].id = SASL_CB_LOG;
        KERBEROS_CALLBACKS[0].proc = reinterpret_cast<int (*)()>(&saslLogCallbacks);
        KERBEROS_CALLBACKS[0].context = reinterpret_cast<void *>(context.data());

        KERBEROS_CALLBACKS[1].id = SASL_CB_USER;
        KERBEROS_CALLBACKS[1].proc = reinterpret_cast<int (*)()>(&saslUserCallbacks);
        KERBEROS_CALLBACKS[1].context = nullptr;

        KERBEROS_CALLBACKS[2].id = SASL_CB_LIST_END;
        KERBEROS_CALLBACKS[2].proc = nullptr;
        KERBEROS_CALLBACKS[2].context = nullptr;
    }
}

SaslClient::SaslClient(
    const String & service_,
    const String & server_fqdn_,
    const String & authorization_id_,
    std::map<String, String> props_,
    const String & mechanisms_,
    sasl_callback_t * callbacks_)
    : service(service_)
    , server_fqdn(server_fqdn_)
    , authentication_id(authorization_id_)
    , mechanism_lists(mechanisms_)
    , sasl_callbacks(callbacks_)
    , client_started(false)
    , auth_completed(false)
    , sasl_connection(nullptr)
{
    if (!props_.empty())
        throw Exception("Properties not yet supported", ErrorCodes::NOT_IMPLEMENTED);
}

SaslClient::~SaslClient()
{
    resetSaslContext();
}

void SaslClient::setupSaslContext()
{
    if (sasl_connection != nullptr)
        throw Exception("sasl connection is exited", ErrorCodes::LOGICAL_ERROR);

    int result = sasl_client_new(service.c_str(), server_fqdn.c_str(), nullptr, nullptr, sasl_callbacks, 0, &sasl_connection);
    if (result != SASL_OK)
    {
        if (sasl_connection)
        {
            throw Exception(sasl_errdetail(sasl_connection), ErrorCodes::SASL_ERROR);
        }
        else
        {
            throw Exception("sasl connection start error", ErrorCodes::SASL_ERROR);
        }
    }
}

void SaslClient::resetSaslContext()
{
    client_started = false;
    auth_completed = false;
    disposeSaslContext();
}

void SaslClient::disposeSaslContext()
{
    if (sasl_connection != nullptr)
        sasl_dispose(&sasl_connection);
    sasl_connection = nullptr;
}

void SaslClient::saslInit(sasl_callback_t * callbacks)
{
    if (SaslCommon::sasl_inited)
    {
        LOG_WARNING(&Poco::Logger::get("SaslClient"), "Sasl Client is already Init");
        return;
    }
    int result = sasl_client_init(callbacks);
    if (result != SASL_OK)
        throw Exception("sasl client init error", ErrorCodes::SASL_ERROR);
    SaslCommon::sasl_inited = true;
}

uint8_t * SaslClient::evaluateChallengeOrResponse(const uint8_t * challenge, const uint32_t & len, uint32_t * resLen)
{
    sasl_interact_t * client_interact = nullptr;
    uint8_t * out = nullptr;
    uint32_t outlen = 0;
    uint32_t result;
    char * mech_using;

    if (!client_started)
    {
        result = sasl_client_start(
            sasl_connection,
            mechanism_lists.c_str(),
            &client_interact, /* filled in if an interaction is needed */
            const_cast<const char **>(reinterpret_cast<char **>(&out)), /* filled in on success */
            &outlen, /* filled in on success */
            const_cast<const char **>(&mech_using));
        client_started = true;
        if (result == SASL_OK || result == SASL_CONTINUE)
        {
            chosen_mechanism = mech_using;
        }
    }
    else
    {
        if (len > 0)
        {
            result = sasl_client_step(
                sasl_connection, /* our context */
                reinterpret_cast<const char *>(challenge), /* the data from the server */
                len, /* its length */
                &client_interact, /* this should be unallocated and NULL */
                const_cast<const char **>(reinterpret_cast<char **>(&out)), /* filled in on success */
                &outlen); /* filled in on success */
        }
        else
        {
            result = SASL_CONTINUE;
        }
    }

    if (result == SASL_OK)
    {
        auth_completed = true;
    }
    else if (result != SASL_CONTINUE)
    {
        throw Exception(sasl_errdetail(sasl_connection), ErrorCodes::SASL_ERROR);
    }
    *resLen = outlen;
    return reinterpret_cast<uint8_t *>(out);
}

uint8_t * SaslClient::unwrap(const uint8_t * incoming, const int &, const uint32_t & len, uint32_t * outLen)
{
    uint32_t outputlen;
    uint8_t * output;
    int result;

    result = sasl_decode(
        sasl_connection,
        reinterpret_cast<const char *>(incoming),
        len,
        const_cast<const char **>(reinterpret_cast<char **>(&output)),
        &outputlen);
    if (result != SASL_OK)
    {
        throw Exception(sasl_errdetail(sasl_connection), ErrorCodes::SASL_ERROR);
    }
    *outLen = outputlen;
    return output;
}

uint8_t * SaslClient::wrap(const uint8_t * outgoing, int offset, const uint32_t & len, uint32_t * outLen)
{
    uint32_t outputlen;
    uint8_t * output;
    int result;

    result = sasl_encode(
        sasl_connection,
        reinterpret_cast<const char *>(outgoing) + offset,
        len,
        const_cast<const char **>(reinterpret_cast<char **>(&output)),
        &outputlen);

    if (result != SASL_OK)
    {
        throw Exception(sasl_errdetail(sasl_connection), ErrorCodes::SASL_ERROR);
    }
    *outLen = outputlen;
    return output;
}

String SaslClient::getMechanismName()
{
    return chosen_mechanism;
}

String SaslClient::getNegotiatedProperty(const String &)
{
    throw Exception("get Negotiated Property is not impl", ErrorCodes::NOT_IMPLEMENTED);
}

String SaslClient::getUsername()
{
    const char * username;
    int result = sasl_getprop(sasl_connection, SASL_USERNAME, reinterpret_cast<const void **>(&username));
    if (result != SASL_OK)
    {
        std::stringstream ss;
        ss << "Error getting SASL_USERNAME property: " << sasl_errstring(result, nullptr, nullptr);
        throw Exception(ss.str(), ErrorCodes::SASL_ERROR);
    }
    // Copy the username and return it to the caller. There is no cleanup/delete call for
    // calls to sasl_getprops, the sasl layer handles the cleanup internally.
    String ret(username);
    return ret;
}

bool SaslClient::hasInitialResponse()
{
    // TODO: Need to return a value based on the mechanism.
    return true;
}

void SaslClient::setupSaslClientWithKerberos()
{
    std::lock_guard lck(SaslCommon::sasl_mutex);
    SaslCommon::setupGeneralCallbacks();
    SaslCommon::setupKerberosCallbacks();

    SaslClient::saslInit(SaslCommon::GENERAL_CALLBACKS.data());
}

}
