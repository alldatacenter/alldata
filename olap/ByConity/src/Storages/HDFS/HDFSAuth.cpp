/*
 * Copyright 2023 Bytedance Ltd. and/or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <Common/config.h>

#if USE_KRB5

#include <Storages/HDFS/HDFSAuth.h>
#include <string>
#include <Access/KerberosInit.h>
#include <Storages/HDFS/HDFSCommon.h>
#include <hdfs/hdfs.h>
#include <Poco/Util/AbstractConfiguration.h>
#include <common/logger_useful.h>
#include <common/types.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int EXCESSIVE_ELEMENT_IN_CONFIG;
}

HDFSKrb5Params
HDFSKrb5Params::parseKrb5FromConfig(const Poco::Util::AbstractConfiguration & config, const String & config_prefix, bool isUser)
{
    auto config_key = [&config_prefix](const String & key) { return config_prefix.empty() ? key : config_prefix + "." + key; };

    const String kerberos_keytab = config_key("hadoop_kerberos_keytab");
    const String kerberos_principal = config_key("hadoop_kerberos_principal");
    const String kerberos_ticket_cache_path = config_key("hadoop_security_kerberos_ticket_cache_path");

    HDFSKrb5Params krb5_params;

    if (config.has(kerberos_keytab))
    {
        krb5_params.need_kinit = true;
        krb5_params.hadoop_kerberos_keytab = config.getString(kerberos_keytab);
    }

    if (config.has(kerberos_principal))
    {
        krb5_params.need_kinit = true;
        krb5_params.hadoop_kerberos_principal = config.getString(kerberos_principal);
    }

    if (config.has(kerberos_ticket_cache_path))
    {
        if (isUser)
        {
            throw Exception("hadoop.security.kerberos.ticket.cache.path cannot be set per user", ErrorCodes::EXCESSIVE_ELEMENT_IN_CONFIG);
        }
        krb5_params.hadoop_security_kerberos_ticket_cache_path = config.getString(kerberos_ticket_cache_path);
    }

    return krb5_params;
}

void HDFSKrb5Params::setHDFSKrb5Config(hdfsBuilder * builder) const
{
    hdfsBuilderSetPrincipal(builder, hadoop_kerberos_principal.c_str());
    if (!hadoop_security_kerberos_ticket_cache_path.empty())
    {
        hdfsBuilderSetKerbTicketCachePath(builder, hadoop_security_kerberos_ticket_cache_path.c_str());
    }
}

void HDFSKrb5Params::runKinit() const
{
    if (need_kinit)
    {
        LOG_DEBUG(&Poco::Logger::get("HDFSClient"), "Running KerberosInit");
        kerberosInit(hadoop_kerberos_keytab, hadoop_kerberos_principal, hadoop_security_kerberos_ticket_cache_path);
        LOG_DEBUG(&Poco::Logger::get("HDFSClient"), "Finished KerberosInit");
    }
}

}
#endif // USE_KRB5
