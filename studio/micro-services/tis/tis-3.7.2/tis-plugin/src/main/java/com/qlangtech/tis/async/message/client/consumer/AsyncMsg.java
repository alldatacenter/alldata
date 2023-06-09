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

import com.qlangtech.tis.datax.TableAlias;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 异步Notify消息
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface AsyncMsg<SOURCE> extends Serializable {

    /**
     *com.qlangtech.tis.realtime.DTOStream <br/>
     * com.qlangtech.plugins.incr.flink.cdc.FlinkCol
     * @return
     */
   default  <DTOStream> Tab2OutputTag<DTOStream> getTab2OutputTag(){
       throw new UnsupportedOperationException();
   }

    /**
     * 关注的表,原始表表名
     *
     * @return
     */
    Set<String> getFocusTabs();

    /**
     * Topic
     *
     * @return
     */
    String getTopic();

    /**
     * Tag
     *
     * @return
     */
    String getTag();

    /**
     * 消息内容
     *
     * @return
     */
    SOURCE getSource() throws IOException;

    /**
     * MsgID
     *
     * @return
     */
    String getMsgID();

    /**
     * Key
     *
     * @return
     */
    // String getKey();

//    /**
//     * 重试次数
//     *
//     * @return
//     */
//    int getReconsumeTimes();

//    /**
//     * 开始投递的时间
//     *
//     * @return
//     */
//    long getStartDeliverTime();

//    /**
//     * 最初的MessageID。在消息重试时msgID会变
//     *
//     * @return
//     */
//    String getOriginMsgID();
}
