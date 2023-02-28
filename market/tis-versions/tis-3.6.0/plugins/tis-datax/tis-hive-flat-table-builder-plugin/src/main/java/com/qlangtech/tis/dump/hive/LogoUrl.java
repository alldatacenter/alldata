///* * Copyright 2020 QingLang, Inc.
// *
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.qlangtech.tis.dump.hive;
//
//import org.apache.hadoop.hive.ql.exec.UDF;
//
///* *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2016年9月21日
// */
//public class LogoUrl extends UDF {
//
//    public String evaluate(String entity_id, final String attachment_id, final String typee, final String server, String path) {
//        int type = 0;
//        try {
//            type = Integer.parseInt(typee);
//        } catch (Throwable e) {
//        }
//        if ("99928321".equals(entity_id)) {
//            System.out.println("====================================================================================");
//            return ("attid:" + attachment_id + ",type:" + type + ",server:" + server + ",path:" + path);
//        // System.out.println("====================================================================================");
//        }
//        if (isBlank(attachment_id) && type == 3 && "zmfile.2dfire-daily.com".equals(server)) {
//            return ("http://" + server + "/upload_files/" + path);
//        } else if (isBlank(attachment_id) && type == 3 && "ifiletest.2dfire.com".equals(server)) {
//            return ("http://" + server + "/" + path);
//        } else if (isBlank(attachment_id) && type == 0 && "zmfile.2dfire-daily.com".equals(server)) {
//            return ("http://" + server + "/upload_files/" + path);
//        } else if (isBlank(attachment_id) && (type == 0 && "ifiletest.2dfire.com".equals(server))) {
//            return ("http://" + server + '/' + path);
//        } else {
//            return ("http://" + server + "/upload_files/" + path);
//        }
//    }
//
//    private static boolean isBlank(String str) {
//        int strLen;
//        if (str == null || (strLen = str.length()) == 0) {
//            return true;
//        }
//        for (int i = 0; i < strLen; i++) {
//            if ((Character.isWhitespace(str.charAt(i)) == false)) {
//                return false;
//            }
//        }
//        return true;
//    }
//}
