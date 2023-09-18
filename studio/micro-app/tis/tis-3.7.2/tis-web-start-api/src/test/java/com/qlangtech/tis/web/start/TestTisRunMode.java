/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.web.start;

import org.junit.Test;

import java.util.function.Supplier;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-30 12:37
 **/
public class TestTisRunMode {
    @Test
    public void testStandalone() {

//        TisAppLaunch appLaunch = EasyMock.createMock("appLaunch", TisAppLaunch.class);
//
//        EasyMock.expect(appLaunch.isZeppelinActivate()).asStub();
//
//        EasyMock.expectLastCall().
//
//        TisAppLaunch.instance = appLaunch;

       // EasyMock.replay(appLaunch);

        Supplier<Boolean> zeppelinContextInitialized = TisRunMode.Standalone.zeppelinContextInitialized;
        org.junit.Assert.assertNotNull(zeppelinContextInitialized);

        zeppelinContextInitialized.get();

       // EasyMock.verify(appLaunch);
    }
}
