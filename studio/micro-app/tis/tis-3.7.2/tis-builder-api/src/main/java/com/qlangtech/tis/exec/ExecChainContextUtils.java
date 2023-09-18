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
package com.qlangtech.tis.exec;

import com.qlangtech.tis.fullbuild.indexbuild.ITabPartition;
import com.qlangtech.tis.order.center.IJoinTaskContext;
import com.qlangtech.tis.sql.parser.TabPartitions;

import java.util.Optional;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月24日
 */
public class ExecChainContextUtils {

    public static final String PARTITION_DATA_PARAMS = "dateParams";

    private ExecChainContextUtils() {
    }

    /**
     * 取得全量構建依賴的全部表的Partition信息
     *
     * @param context
     * @return
     */
    public static TabPartitions getDependencyTablesPartitions(IJoinTaskContext context) {
        TabPartitions dateParams = context.getAttribute(PARTITION_DATA_PARAMS);
        if (dateParams == null) {
            throw new IllegalStateException("dateParams is not in context");
        }
        return dateParams;
    }

    public static void setDependencyTablesPartitions(IJoinTaskContext context, TabPartitions dateParams) {
        context.setAttribute(ExecChainContextUtils.PARTITION_DATA_PARAMS, dateParams);
    }


//    public static ITabPartition getDependencyTablesMINPartition(IJoinTaskContext context) {
//        TabPartitions dateParams = getDependencyTablesPartitions(context);
//        Optional<ITabPartition> min = dateParams.getMinTablePartition();// dateParams.values().stream().min(Comparator.comparing((r) -> Long.parseLong(r.getPt())));
//        if (!min.isPresent()) {
//            return () ->  context.getPartitionTimestampWithMillis();
//        }
//        return min.get();
//    }
}
