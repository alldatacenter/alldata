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

package com.qlangtech.tis.fullbuild.servlet;

import java.util.concurrent.Future;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-08-31 13:00
 **/
public class Test {
    public static void main(String[] args) throws Exception {
        Future<?> f = TisServlet.executeService.submit(() -> {


            while (true) {
                try {

                    System.out.println("i am here");
                    Thread.sleep(5000l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

        Thread.sleep(1000l);
        f.cancel(true);
        System.out.println("all over");

        Thread.sleep(90000l);
    }
}
