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

package com.qlangtech.plugins.incr.flink.chunjun.doris.table;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.types.Row;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-12-18 12:27
 **/
public class PrintSinkFunction implements SinkFunction<Tuple2<Boolean, Row>> {


    public void invoke(Tuple2<Boolean, Row> value, Context context) throws Exception {

        StringBuffer print = new StringBuffer("flag:" + value.f0);

        //  print.append(",eventType:").append(value.getEventType());
        Row row = value.f1;
        print.append(",event:").append(row.getKind());
        Object val = null;
        for (int i = 0; i < row.getArity(); i++) {
            val = row.getField(i);
            print.append(" index:").append(i).append(",val:").append(val).append(",type:").append(val.getClass().getSimpleName()).append("\n");
        }

//        if (value.getBefore() != null) {
//            print.append("\n before:");
//            for (Map.Entry<String, Object> before : value.getBefore().entrySet()) {
//                print.append(before.getKey()).append(":").append(before.getValue()).append(",");
//            }
//        }
//
//        print.append("\n after:");
//        for (Map.Entry<String, Object> after : value.getAfter().entrySet()) {
//            print.append(after.getKey()).append(":").append(after.getValue()).append(",");
//        }

        System.out.println(print.toString());

    }
}
