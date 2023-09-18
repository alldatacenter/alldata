///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//package com.qlangtech.tis.fullbuild.taskflow;
//
//import com.qlangtech.tis.exec.ExecChainContextUtils;
//import com.qlangtech.tis.exec.IExecChainContext;
//import com.qlangtech.tis.sql.parser.TabPartitions;
//
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2014年7月21日下午3:39:06
// */
//
//public class TemplateContext implements ITemplateContext {
//
//    private static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
//
//        @Override
//        protected SimpleDateFormat initialValue() {
//            return new SimpleDateFormat("yyyyMMdd");
//        }
//    };
//
//    public <T> T getAttribute(String key) {
//        return params.getAttribute(key);
//    }
//
//    public String datediff(int offset) {
//        Calendar calendar = Calendar.getInstance();
//        calendar.add(Calendar.DAY_OF_YEAR, offset);
//        return dateFormat.get().format(calendar.getTime());
//    }
//
//    public static void main(String[] args) {
////        TemplateContext context = new TemplateContext(null);
////        System.out.println(context.datediff(-365));
//    }
//
//    private Map<String, Object> contextValues = new HashMap<>();
//
//    // 用户提交的参数
//    private final IExecChainContext params;
//
//    public TemplateContext(IExecChainContext paramContext) {
//        this.params = paramContext;
//    }
//
//    public TabPartitions getTablePartition() {
//        return ExecChainContextUtils.getDependencyTablesPartitions(params);
//    }
//
//    @Override
//    public IExecChainContext getExecContext() {
//        return this.params;
//    }
//
//    // public IExecChainContext getParams() {
//    //    return params;
//    // }
//
//    public void putContextValue(String key, Object v) {
//        this.contextValues.put(key, v);
//    }
//
//    /**
//     * @param key
//     * @return
//     */
//    @SuppressWarnings("all")
//    public <T> T getContextValue(String key) {
//        return (T) this.contextValues.get(key);
//    }
//
////    public String getDate() {
////        if (params == null) {
////            return null;
////        }
////        return params.getPartitionTimestampWithMillis();
////    }
//}
