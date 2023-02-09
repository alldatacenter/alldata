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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.aliyun.oss.common.auth.ServiceSignatureTest;
import com.aliyun.oss.common.comm.HttpFactoryTest;
import com.aliyun.oss.common.comm.ServiceClientTest;
import com.aliyun.oss.common.utils.BinaryUtilTest;
import com.aliyun.oss.common.utils.DateUtilTest;
import com.aliyun.oss.common.utils.ExceptionFactoryTest;
import com.aliyun.oss.common.utils.IOUtilTest;
import com.aliyun.oss.common.utils.ResourceManagerTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    // package com.aliyun.oss
    OSSClientArgCheckTest.class,
    OSSClientRequestTest.class,
    OSSResponseParserTest.class,
    
    // package com.aliyun.oss.common.auth
    ServiceSignatureTest.class,
    
    // package com.aliyun.oss.common.comm
    HttpFactoryTest.class,
    ServiceClientTest.class,
    
    // package com.aliyun.oss.common.utils
    BinaryUtilTest.class,
    DateUtilTest.class,
    ExceptionFactoryTest.class,
    IOUtilTest.class,
    ResourceManagerTest.class
})

public class OSSJUnittestSuit {

}
