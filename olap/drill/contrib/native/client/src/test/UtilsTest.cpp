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
#include <string>

#include <cppunit/TestFixture.h>
#include <cppunit/extensions/HelperMacros.h>

#include "utils.hpp"

class UtilsTest: public CppUnit::TestFixture {
public:
    UtilsTest() {}

    CPPUNIT_TEST_SUITE( UtilsTest );
    CPPUNIT_TEST(testParseConnectStr);
    CPPUNIT_TEST_SUITE_END();


    void testParseConnectStr() {
        std::string protocol;
        std::string hostAndPort;
        std::string path;

        Drill::Utils::parseConnectStr("local=localhost:12345/path/to/drill",
                path,
                protocol,
                hostAndPort);

        CPPUNIT_ASSERT(protocol == "local");
        CPPUNIT_ASSERT(hostAndPort == "localhost:12345");
        CPPUNIT_ASSERT(path == "/path/to/drill");
    }
};

CPPUNIT_TEST_SUITE_REGISTRATION( UtilsTest );
