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
#include <cppunit/TestFixture.h>
#include <cppunit/extensions/HelperMacros.h>

#include "drill/userProperties.hpp"
#include "drillClientImpl.hpp"


class DrillClientTest : public CppUnit::TestFixture {
public:
    DrillClientTest() {}

    CPPUNIT_TEST_SUITE(DrillClientTest);
    CPPUNIT_TEST(testHandleComplexTypes);
    CPPUNIT_TEST_SUITE_END();

    void testHandleComplexTypes() {
        Drill::DrillClientImpl DCI;
        Drill::DrillUserProperties properties1, properties2, properties3;
        bool supportComplexTypes;

        // Check when property is set to true
        properties1.setProperty(USERPROP_SUPPORT_COMPLEX_TYPES, "true");
        supportComplexTypes = DCI.handleComplexTypes(&properties1);
        CPPUNIT_ASSERT(supportComplexTypes == true);

        // Check when property is set to false
        properties2.setProperty(USERPROP_SUPPORT_COMPLEX_TYPES, "false");
        supportComplexTypes = DCI.handleComplexTypes(&properties2);
        CPPUNIT_ASSERT(supportComplexTypes == false);

        // Check the default value
        supportComplexTypes = DCI.handleComplexTypes(&properties3);
        CPPUNIT_ASSERT(supportComplexTypes == false);
    }
};

CPPUNIT_TEST_SUITE_REGISTRATION(DrillClientTest);

