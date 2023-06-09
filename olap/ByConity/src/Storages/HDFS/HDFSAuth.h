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

#pragma once

#include <Common/config.h>

#if USE_KRB5
#include <string>
#include <hdfs/hdfs.h>
#include <Poco/Util/AbstractConfiguration.h>
#include <common/types.h>

class HDFSBuilderPtr;

namespace DB
{

class HDFSKrb5Params
{
public:
    String hadoop_kerberos_keytab;
    String hadoop_kerberos_principal;
    String hadoop_security_kerberos_ticket_cache_path;

    bool need_kinit{false};

    static HDFSKrb5Params
    parseKrb5FromConfig(const Poco::Util::AbstractConfiguration & config, const String & config_prefix = "", bool isUser = false);

    void setHDFSKrb5Config(hdfsBuilder * builder) const;
    void runKinit() const;
};

}
#endif //USE_KRB5
