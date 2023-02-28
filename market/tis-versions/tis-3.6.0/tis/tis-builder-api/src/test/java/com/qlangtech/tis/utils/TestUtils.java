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

package com.qlangtech.tis.utils;

import junit.framework.TestCase;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-18 17:04
 **/
public class TestUtils extends TestCase {
    public void testReadLastUtf8Lines() {
        File monitorFile = new File("./src/test/resources/com/qlangtech/tis/utils/monitorFile.txt");
        assertTrue(monitorFile.exists());
        Set<String> chars = new HashSet<>();
        chars.add("吃");
        chars.add("喝");
        chars.add("玩");
        chars.add("乐");
        Utils.readLastNLine(monitorFile, 10, (line) -> {
            chars.remove(line);
        });
        assertTrue(chars.isEmpty());
    }
}
