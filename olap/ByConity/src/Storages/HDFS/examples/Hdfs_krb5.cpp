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

#include <iostream>
#include <string>
#include <hdfs/hdfs.h>
#include <Access/KerberosInit.h>
#include <Poco/ConsoleChannel.h>
#include <Poco/Logger.h>
#include <Poco/AutoPtr.h>
#include <Poco/URI.h>
#include <Common/Exception.h>
#include <Access/KerberosInit.h>


/**
 * The example is used to check krb5 keytab and principal
 * can access the hdfs with krb5 for listing hdfs path
 * hdfs://user @ host : port/ path /
*/
using namespace DB;

int main(int argc, char ** argv)
{
    std::cout << "Kerberos Init" << "\n";

    if (argc != 5)
    {
        std::cout << "kerberos_init obtains and caches an initial ticket-granting ticket for principal." << "\n\n";
        std::cout << "Usage:" << "\n" << "hdfs_krb5 keytab principal cache_path hdfs_path" << "\n";
        std::cout << "hdfs_path : hdfs://host:port/path/" << "\n";
        return 0;
    }

    Poco::AutoPtr<Poco::ConsoleChannel> app_channel(new Poco::ConsoleChannel(std::cerr));
    Poco::Logger::root().setChannel(app_channel);
    Poco::Logger::root().setLevel("trace");

    try
    {
        kerberosInit(argv[1], argv[2], argv[3]);
    }
    catch (const Exception & e)
    {
        std::cout << "KerberosInit failure: " << getExceptionMessage(e, false) << "\n";
        return -1;
    }


    std::cout << "KerberosInit success" << "\n";

    const Poco::URI hdfs_uri(argv[4]);


    std::cout << "Try List HDFS Path" << "\n";

    auto builder = hdfsNewBuilder() ;

    hdfsBuilderSetNameNode(builder, hdfs_uri.getHost().c_str());
    std::cout << "NameNode: " << hdfs_uri.getHost()<< "\n";

    hdfsBuilderSetNameNodePort(builder, hdfs_uri.getPort());
    std::cout << "Port: " << hdfs_uri.getPort() << "\n";

    hdfsBuilderSetPrincipal(builder, argv[2]);
    std::cout << "Principal: " << argv[2] << "\n";

    std::string auth_method_key = "hadoop.security.authentication" ;
    std::string auth_method = "KERBEROS";
    hdfsBuilderConfSetStr(builder, auth_method_key.c_str(), auth_method.c_str());

    hdfsBuilderSetKerbTicketCachePath(builder, argv[3]);

    auto fs = hdfsBuilderConnect(builder);
    int file_num = 0 ;

    auto file_info = hdfsListDirectory(fs, hdfs_uri.getPath().c_str(), &file_num);

    for (int i = 0 ; i < file_num ; i++)
    {
        std::cout << i << ": "<<file_info[i].mName << "\n";
    }

    std::cout<< "error:" << std::string(hdfsGetLastError()) <<std::endl;

    return 0 ;
}
