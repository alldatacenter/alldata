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

package com.qlangtech.tis.mq;

import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.client.producer.DefaultMQProducer;

/**
 * @author: baisui 百岁
 * @create: 2020-11-02 10:11
 **/
public class TestEmployeeProducter extends BasicProducer {

    public void testProducter() throws Exception {

        DefaultMQProducer producter = this.createProducter();

//        {
//            "after": {
//               "account_num": "1.0"
//             },
//            "before": {
//               "account_num": "1.0",
//            },
//            "dbName": "order96",
//            "eventType": "UPDATE",
//            "orginTableName": "waitinginstanceinfo",
//            "targetTable": "otter_binlogorder"
//        }

        JSONObject msg = new JSONObject();
        JSONObject after = new JSONObject();
        after.put("emp_no", "9531");
        after.put("dept_no", "d999");
        after.put("from_date", "1991-04-28");
        after.put("to_date", "1991-05-09");
        msg.put("before", new JSONObject());
        msg.put("after", after);
        msg.put("dbName", "employee");
        msg.put("eventType", "INSERT");
        msg.put("orginTableName", "dept_emp");
        msg.put("targetTable", "test");


        producter.send(this.createMsg(msg.toJSONString(), "dept_emp"));

        msg = new JSONObject();
        after = new JSONObject();
        after.put("emp_no", "9531");
        after.put("dept_no", "d888");
        after.put("from_date", "1991-04-29");
        after.put("to_date", "1991-06-27");

        msg.put("before", new JSONObject());
        msg.put("after", after);
        msg.put("dbName", "employee");
        msg.put("eventType", "INSERT");
        msg.put("orginTableName", "dept_emp");
        msg.put("targetTable", "test");


        producter.send(this.createMsg(msg.toJSONString(), "dept_emp"));

    }
}
