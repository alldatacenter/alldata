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
package com.qlangtech.tis.coredefine.module.action;

import junit.framework.TestCase;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestLoopQueue extends TestCase {

    public void testQueue() {
        String[] buffer = new String[100];
        LoopQueue<String> queue = new LoopQueue<>(buffer);
        (new Thread() {

            @Override
            public void run() {
                while (true) {
                    String[] read = queue.readBuffer();
                    System.out.println("============================");
                    for (int i = 0; i < read.length; i++) {
                        System.out.print(read[i] + ",");
                    }
                    System.out.println();
                    try {
                        sleep(500);
                    } catch (Exception e) {
                    }
                }
            }
        }).start();
        int i = 0;
        while (true) {
            System.out.println("wite:" + i);
            queue.write(String.valueOf(i++));
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
        }
    }
}
