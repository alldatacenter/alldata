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

package com.qlangtech.tis.datax;

import com.alibaba.fastjson.JSON;
import com.qlangtech.tis.manage.common.TisUTF8;
import org.apache.curator.framework.recipes.queue.QueueSerializer;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-06 15:33
 **/
public class MessageSerializer implements QueueSerializer<CuratorDataXTaskMessage> {

    @Override
    public byte[] serialize(CuratorDataXTaskMessage item) {
        return JSON.toJSONString(item, false).getBytes(TisUTF8.get());
    }

    @Override
    public CuratorDataXTaskMessage deserialize(byte[] bytes) {
        return JSON.parseObject(new String(bytes), CuratorDataXTaskMessage.class);
    }
}
