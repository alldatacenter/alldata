/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server;

import com.netease.arctic.ams.server.utils.AmsUtilsTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    TestHighAvailabilityServices.class,
    AmsUtilsTest.class
})
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "org.apache.http.conn.ssl.*",
                  "com.amazonaws.http.conn.ssl.*",
                  "javax.net.ssl.*", "org.apache.hadoop.*", "javax.*", "com.sun.org.apache.*", "org.apache.xerces.*",
                  "javax.xml.parsers.*"})
public class OutAmsTestBase {

}
