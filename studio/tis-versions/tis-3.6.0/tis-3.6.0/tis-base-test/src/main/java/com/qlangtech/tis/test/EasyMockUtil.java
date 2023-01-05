/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.test;

import com.google.common.collect.Lists;
import org.easymock.EasyMock;

import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-03-04 12:52
 */
public class EasyMockUtil {
    private static List<Object> mocks = Lists.newArrayList();

    static void clearMocks() {
        mocks = Lists.newArrayList();
    }

    static void verifyAll() {
        mocks.forEach((r) -> {
            EasyMock.verify(r);
        });
    }

    public static <T> T mock(String name, Class<?> toMock) {
        Object mock = EasyMock.createMock(name, toMock);
        mocks.add(mock);
        return (T) mock;
    }

    static void replay() {
        mocks.forEach((r) -> {
            EasyMock.replay(r);
        });
    }
}
