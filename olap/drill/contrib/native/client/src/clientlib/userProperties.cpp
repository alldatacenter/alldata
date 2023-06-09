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
#include <boost/assign.hpp>
#include "drill/userProperties.hpp"

namespace Drill{

//Using boost assign to initialize maps. 
const std::map<std::string, uint32_t>  DrillUserProperties::USER_PROPERTIES=boost::assign::map_list_of
    ( USERPROP_USERNAME,    USERPROP_FLAGS_SERVERPROP|USERPROP_FLAGS_USERNAME|USERPROP_FLAGS_STRING )
    ( USERPROP_PASSWORD,    USERPROP_FLAGS_SERVERPROP|USERPROP_FLAGS_PASSWORD)
    ( USERPROP_SCHEMA,      USERPROP_FLAGS_SERVERPROP|USERPROP_FLAGS_STRING)
    ( USERPROP_IMPERSONATION_TARGET,   USERPROP_FLAGS_SERVERPROP|USERPROP_FLAGS_STRING)
    ( USERPROP_AUTH_MECHANISM,         USERPROP_FLAGS_STRING)
    ( USERPROP_SERVICE_NAME,           USERPROP_FLAGS_STRING)
    ( USERPROP_SERVICE_HOST,           USERPROP_FLAGS_STRING)
    ( USERPROP_USESSL,      USERPROP_FLAGS_BOOLEAN|USERPROP_FLAGS_SSLPROP)
    ( USERPROP_TLSPROTOCOL,      USERPROP_FLAGS_STRING|USERPROP_FLAGS_SSLPROP)
    ( USERPROP_CERTFILEPATH,    USERPROP_FLAGS_STRING|USERPROP_FLAGS_SSLPROP|USERPROP_FLAGS_FILEPATH)
    ( USERPROP_HOSTNAME_OVERRIDE,    USERPROP_FLAGS_STRING|USERPROP_FLAGS_SSLPROP)
    ( USERPROP_DISABLE_HOSTVERIFICATION,    USERPROP_FLAGS_BOOLEAN|USERPROP_FLAGS_SSLPROP)
    ( USERPROP_DISABLE_CERTVERIFICATION,    USERPROP_FLAGS_BOOLEAN|USERPROP_FLAGS_SSLPROP)
    ( USERPROP_USESYSTEMTRUSTSTORE,    USERPROP_FLAGS_BOOLEAN|USERPROP_FLAGS_SSLPROP)
    ( USERPROP_CUSTOM_SSLCTXOPTIONS,   USERPROP_FLAGS_STRING|USERPROP_FLAGS_SSLPROP)
    ( USERPROP_SASL_ENCRYPT,  USERPROP_FLAGS_STRING)
;

bool DrillUserProperties::validate(std::string& err){
    bool ret=true;
    //We can add additional validation for any params here
    return ret;
}

} // namespace Drill
