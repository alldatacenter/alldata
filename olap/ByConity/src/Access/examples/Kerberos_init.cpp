/*
 * Copyright 2016-2023 ClickHouse, Inc.
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

#include <iostream>
#include <Poco/ConsoleChannel.h>
#include <Poco/Logger.h>
#include <Poco/AutoPtr.h>
#include <Common/Exception.h>
#include <Access/KerberosInit.h>

/** The example demonstrates using of kerberosInit function to obtain and cache Kerberos ticket-granting ticket.
  * The first argument specifies keytab file. The second argument specifies principal name.
  * The third argument is optional. It specifies credentials cache location.
  * After successful run of kerberos_init it is useful to call klist command to list cached Kerberos tickets.
  * It is also useful to run kdestroy to destroy Kerberos tickets if needed.
  */

using namespace DB;

int main(int argc, char ** argv)
{
    std::cout << "Kerberos Init" << "\n";

    if (argc < 3)
    {
        std::cout << "kerberos_init obtains and caches an initial ticket-granting ticket for principal." << "\n\n";
        std::cout << "Usage:" << "\n" << "    kerberos_init keytab principal [cache]" << "\n";
        return 0;
    }

    const char * cache_name = "";
    if (argc == 4)
        cache_name = argv[3];

    Poco::AutoPtr<Poco::ConsoleChannel> app_channel(new Poco::ConsoleChannel(std::cerr));
    Poco::Logger::root().setChannel(app_channel);
    Poco::Logger::root().setLevel("trace");

    try
    {
        kerberosInit(argv[1], argv[2], cache_name);
    }
    catch (const Exception & e)
    {
        std::cout << "KerberosInit failure: " << getExceptionMessage(e, false) << "\n";
        return -1;
    }
    std::cout << "Done" << "\n";
    return 0;
}
