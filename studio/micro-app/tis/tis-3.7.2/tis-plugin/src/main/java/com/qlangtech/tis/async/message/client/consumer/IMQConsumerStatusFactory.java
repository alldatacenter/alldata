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
package com.qlangtech.tis.async.message.client.consumer;

/**
 * 监听mq group comsumer的偏移量
 *
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-08-29 16:27
 */
public interface IMQConsumerStatusFactory {

    default IMQConsumerStatus createConsumerStatus() {
        throw new UnsupportedOperationException();
    }

    interface IMQConsumerStatus {
        // 想了一想这个接口暂时没有什么用，先不用
        // long getTotalDiff();
        //
        // void close();
    }
}
