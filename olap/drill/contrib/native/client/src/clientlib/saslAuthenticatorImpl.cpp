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
#include <vector>
#include <algorithm>
#include <boost/algorithm/string.hpp>
#include <boost/assign.hpp>
#include "drill/userProperties.hpp"
#include "saslAuthenticatorImpl.hpp"

#include "drillClientImpl.hpp"
#include "logger.hpp"

namespace Drill {

const std::string DEFAULT_SERVICE_NAME = "drill";
const int PREFERRED_MIN_SSF = 56;
const std::string SaslAuthenticatorImpl::KERBEROS_SIMPLE_NAME = "kerberos";
const std::string SaslAuthenticatorImpl::PLAIN_NAME = "plain";

const std::string KERBEROS_SASL_NAME = "gssapi";

const std::map<std::string, std::string> SaslAuthenticatorImpl::MECHANISM_MAPPING = boost::assign::map_list_of
    (SaslAuthenticatorImpl::KERBEROS_SIMPLE_NAME, KERBEROS_SASL_NAME)
    (SaslAuthenticatorImpl::PLAIN_NAME, SaslAuthenticatorImpl::PLAIN_NAME)
;

boost::mutex SaslAuthenticatorImpl::s_mutex;
bool SaslAuthenticatorImpl::s_initialized = false;

SaslAuthenticatorImpl::SaslAuthenticatorImpl(const DrillUserProperties* const properties) :
    m_pUserProperties(properties), m_pConnection(NULL), m_ppwdSecret(NULL), m_pEncryptCtxt(NULL) {
    if (!s_initialized) {
        boost::lock_guard<boost::mutex> lock(SaslAuthenticatorImpl::s_mutex);
        if (!s_initialized) {
            // set plugin path if provided
            if (DrillClientConfig::getSaslPluginPath()) {
                char *saslPluginPath = const_cast<char *>(DrillClientConfig::getSaslPluginPath());
                sasl_set_path(0, saslPluginPath);
            }

            // loads all the available mechanism and factories in the sasl_lib referenced by the path
            const int err = sasl_client_init(NULL);
            if (0 != err) {
                std::stringstream errMsg;
                errMsg << "Failed to load authentication libraries. code: " << err;
                DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << errMsg.str() << std::endl;)
                throw std::runtime_error(errMsg.str().c_str());
            }
            { // for debugging purposes
                const char **mechanisms = sasl_global_listmech();
                int i = 0;
                DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "SASL mechanisms available on client: " << std::endl;)
                while (mechanisms[i] != NULL) {
                    DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << i << " : " << mechanisms[i] << std::endl;)
                    i++;
                }
            }
            s_initialized = true;
        }
    }
}

SaslAuthenticatorImpl::~SaslAuthenticatorImpl() {
    if (m_ppwdSecret) {
        free(m_ppwdSecret);
    }
    m_ppwdSecret = NULL;
    // may be used to negotiated security layers before disposing in the future
    if (m_pConnection) {
        sasl_dispose(&m_pConnection);
    }
    m_pConnection = NULL;

    // Memory is owned by DrillClientImpl object
    m_pEncryptCtxt = NULL;
}

typedef int (*sasl_callback_proc_t)(void); // see sasl_callback_ft

int SaslAuthenticatorImpl::userNameCallback(void *context, int id, const char **result, unsigned *len) {
    const std::string* const username = static_cast<const std::string* const>(context);

    if ((SASL_CB_USER == id || SASL_CB_AUTHNAME == id)
        && username != NULL) {
        *result = username->c_str();
        // *len = (unsigned int) username->length();
    }
    return SASL_OK;
}

int SaslAuthenticatorImpl::passwordCallback(sasl_conn_t *conn, void *context, int id, sasl_secret_t **psecret) {
    const SaslAuthenticatorImpl* const authenticator = static_cast<const SaslAuthenticatorImpl* const>(context);

    if (SASL_CB_PASS == id) {
        *psecret = authenticator->m_ppwdSecret;
    }
    return SASL_OK;
}

int SaslAuthenticatorImpl::init(const std::vector<std::string>& mechanisms, exec::shared::SaslMessage& response,
                                EncryptionContext* const encryptCtxt) {

    // EncryptionContext should not be NULL here.
    assert(encryptCtxt != NULL);
    m_pEncryptCtxt = encryptCtxt;

	// find and set parameters
    std::string authMechanismToUse;
    std::string serviceName;
    std::string serviceHost;
    for (std::map<std::string, std::string>::const_iterator it=m_pUserProperties->begin(); 
            it!=m_pUserProperties->end(); 
            ++it){
        const std::string key = it->first;
        const std::string value = it->second;
        if (USERPROP_SERVICE_HOST == key) {
            serviceHost = value;
        } else if (USERPROP_SERVICE_NAME == key) {
            serviceName = value;
        } else if (USERPROP_PASSWORD == key) {
            const size_t length = value.length();
            m_ppwdSecret = (sasl_secret_t *) malloc(sizeof(sasl_secret_t) + length);
            std::memcpy(m_ppwdSecret->data, value.c_str(), length);
            m_ppwdSecret->len = length;
            authMechanismToUse = SaslAuthenticatorImpl::PLAIN_NAME;
        } else if (USERPROP_USERNAME == key) {
            m_username = value;
        } else if (USERPROP_AUTH_MECHANISM == key) {
            authMechanismToUse = value;
        }
    }
    // clientNeedsAuthentication() cannot be false if the code above picks an authMechanism other than PLAIN
    assert (authMechanismToUse.empty() || authMechanismToUse == SaslAuthenticatorImpl::PLAIN_NAME ||
            DrillClientImpl::clientNeedsAuthentication(m_pUserProperties));

    m_authMechanismName = authMechanismToUse;
    if (authMechanismToUse.empty()) return SASL_NOMECH;

    // check if requested mechanism is supported by server
    boost::algorithm::to_lower(authMechanismToUse);
    if (std::find(mechanisms.begin(), mechanisms.end(), authMechanismToUse) == mechanisms.end()) return SASL_NOMECH;

    // find the SASL name
    const std::map<std::string, std::string>::const_iterator it =
            SaslAuthenticatorImpl::MECHANISM_MAPPING.find(authMechanismToUse);
    if (it == SaslAuthenticatorImpl::MECHANISM_MAPPING.end()) return SASL_NOMECH;

    const std::string saslMechanismToUse = it->second;

    // setup callbacks and parameter
    sasl_callback_t user = { SASL_CB_USER, (sasl_callback_proc_t) &userNameCallback, static_cast<void *>(&m_username) };
    sasl_callback_t authname = { SASL_CB_AUTHNAME, (sasl_callback_proc_t)&userNameCallback, static_cast<void *>(&m_username) };
    sasl_callback_t pass = { SASL_CB_PASS, (sasl_callback_proc_t)&passwordCallback, static_cast<void *>(this) };
    sasl_callback_t list_end = { SASL_CB_LIST_END, NULL, NULL };
    m_callbacks.push_back(user);
    m_callbacks.push_back(authname);
    m_callbacks.push_back(pass);
    m_callbacks.push_back(list_end);

    if (serviceName.empty()) serviceName = DEFAULT_SERVICE_NAME;

    // create SASL client
    int saslResult = sasl_client_new(serviceName.c_str(), serviceHost.c_str(), NULL /** iplocalport */,
                                     NULL /** ipremoteport */, &m_callbacks[0], 0 /** sec flags */, &m_pConnection);
    DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "SaslAuthenticatorImpl::init: sasl_client_new code: "
                                      << saslResult << std::endl;)
    if (saslResult != SASL_OK) return saslResult;

    // set the security properties
    setSecurityProps();

    // initiate; for now, pass in only one mechanism
    const char *out;
    unsigned outlen;
    const char *mech;
    saslResult = sasl_client_start(m_pConnection, saslMechanismToUse.c_str(), NULL /** no prompt */, &out, &outlen,
                                   &mech);
    DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "SaslAuthenticatorImpl::init: sasl_client_start code: "
                                      << saslResult << std::endl;)
    if (saslResult != SASL_OK && saslResult != SASL_CONTINUE) return saslResult;

    // prepare response
    DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "SaslAuthenticatorImpl::init: chosen: " << authMechanismToUse << std::endl;)
    response.set_mechanism(authMechanismToUse);
    response.set_data(NULL == out ? "" : out, outlen);
    response.set_status(exec::shared::SASL_START);
    return saslResult;
}

int SaslAuthenticatorImpl::step(const exec::shared::SaslMessage& challenge, exec::shared::SaslMessage& response) const {
    const char *in = challenge.data().c_str();
    const unsigned inlen = challenge.data().length();
    const char *out;
    unsigned outlen;
    const int saslResult = sasl_client_step(m_pConnection, in, inlen, NULL /** no prompt */, &out, &outlen);
    switch (saslResult) {
        case SASL_CONTINUE:
            response.set_data(out, outlen);
            response.set_status(exec::shared::SASL_IN_PROGRESS);
            break;
        case SASL_OK:
            response.set_data(out, outlen);
            response.set_status(exec::shared::SASL_SUCCESS);
            break;
        default:
            response.set_status(exec::shared::SASL_FAILED);
            break;
    }
    DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "SaslAuthenticatorImpl::step: result: " << saslResult << std::endl;)
    return saslResult;
}

/*
 * Verify that the negotiated value is correct as per system configurations. Also retrieves and set the rawWrapSendSize
 */
int SaslAuthenticatorImpl::verifyAndUpdateSaslProps() {
    const int* negotiatedValue;
    int result = SASL_OK;

    if(SASL_OK != (result = sasl_getprop(m_pConnection, SASL_SSF, reinterpret_cast<const void **>(&negotiatedValue)))) {
        return result;
    }

    // If the negotiated SSF value is less than required one that means we have negotiated for weaker security level.
    if(*negotiatedValue < PREFERRED_MIN_SSF) {
        DRILL_MT_LOG(DRILL_LOG(LOG_DEBUG) << "SaslAuthenticatorImpl::verifyAndUpdateSaslProps: "
                                          << "Negotiated SSF parameter:" << *negotiatedValue
                                          << " is less than Preferred one: " << PREFERRED_MIN_SSF << std::endl;)
        result = SASL_BADPARAM;
        return result;
    }

    if(SASL_OK != (result = sasl_getprop(m_pConnection, SASL_MAXOUTBUF,
                                         reinterpret_cast<const void **>(&negotiatedValue)))) {
        return result;
    }

    DRILL_MT_LOG(DRILL_LOG(LOG_DEBUG) << "SaslAuthenticatorImpl::verifyAndUpdateSaslProps: "
                                      << "Negotiated Raw Wrap Buffer size: " << *negotiatedValue << std::endl;)

    m_pEncryptCtxt->setWrapSizeLimit(*negotiatedValue);
    return result;
}

/*
 *  Set the security properties structure with all the needed parameters for encryption so that
 *  a proper mechanism with and cipher is chosen after handshake.
 *
 *  PREFERRED_MIN_SSF is chosen to be 56 since that is the max_ssf supported by gssapi. We want 
 *  stronger cipher algorithm to be used all the time (preferably AES-256), so leaving MAX_SSF as UINT_MAX
 */
void SaslAuthenticatorImpl::setSecurityProps() const{

    if(m_pEncryptCtxt->isEncryptionReqd()) {
        // set the security properties.
        sasl_security_properties_t secprops;
        secprops.min_ssf = PREFERRED_MIN_SSF;
        secprops.max_ssf = UINT_MAX;
        secprops.maxbufsize = m_pEncryptCtxt->getMaxWrappedSize();
        secprops.property_names = NULL;
        secprops.property_values = NULL;
        // Only specify NOPLAINTEXT for encryption since the mechanism is selected based on name not
        // the security properties configured here.
        secprops.security_flags = SASL_SEC_NOPLAINTEXT;

        // Set the security properties in the connection context.
        sasl_setprop(m_pConnection, SASL_SEC_PROPS, &secprops);
    }
}

/*
 * Encodes the input data by calling the sasl_encode provided by Cyrus-SASL library which internally calls
 * the wrap function of the chosen mechanism. The output buffer will have first 4 octets as the length of
 * encrypted data in network byte order.
 *
 * Parameters:
 *      dataToWrap      -   in param    -   pointer to data buffer to encrypt.
 *      dataToWrapLen   -   in param    -   length of data buffer to encrypt.
 *      output          -   out param   -   pointer to data buffer with encrypted data. Allocated by Cyrus-SASL
 *      wrappedLen      -   out param   -   length of data after encryption
 * Returns:
 *      SASL_OK         - success (returns input if no layer negotiated)
 *      SASL_NOTDONE    - security layer negotiation not finished
 *      SASL_BADPARAM   - inputlen is greater than the SASL_MAXOUTBUF
 */
int SaslAuthenticatorImpl::wrap(const char* dataToWrap, const int& dataToWrapLen, const char** output,
                                uint32_t& wrappedLen) {
    return sasl_encode(m_pConnection, dataToWrap, dataToWrapLen, output, &wrappedLen);
}

/*
 * Decodes the input data by calling the sasl_decode provided by Cyrus-SASL library which internally calls
 * the wrap function of the chosen mechanism. The input buffer will have first 4 octets as the length of
 * encrypted data in network byte order.
 *
 * Parameters:
 *      dataToUnWrap      -   in param    -   pointer to data buffer to decrypt.
 *      dataToUnWrapLen   -   in param    -   length of data buffer to decrypt.
 *      output            -   out param   -   pointer to data buffer with decrypted data. Allocated by Cyrus-SASL
 *      unWrappedLen      -   out param   -   length of data after decryption
 * Returns:
 *      SASL_OK         - success (returns input if no layer negotiated)
 *      SASL_NOTDONE    - security layer negotiation not finished
 *      SASL_BADPARAM   - inputlen is greater than the SASL_MAXOUTBUF
 */
int SaslAuthenticatorImpl::unwrap(const char* dataToUnWrap, const int& dataToUnWrapLen, const char** output,
                                  uint32_t& unWrappedLen) {
    return sasl_decode(m_pConnection, dataToUnWrap, dataToUnWrapLen, output, &unWrappedLen);
}

const char* SaslAuthenticatorImpl::getErrorMessage(int errorCode) {
    switch (errorCode) {
        case SASL_NOMECH:
            return "No mechanism found that meets requested properties ";
        default:
            return sasl_errdetail(m_pConnection);
    }
}

    const std::string &SaslAuthenticatorImpl::getAuthMechanismName() const {
        return m_authMechanismName;
    }

} /* namespace Drill */
