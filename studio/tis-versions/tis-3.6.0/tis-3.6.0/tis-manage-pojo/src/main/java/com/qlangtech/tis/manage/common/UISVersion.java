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
package com.qlangtech.tis.manage.common;

import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年6月16日
 */
public class UISVersion {

    public static final String SOLR_VERSION_6 = "solr6";

    public static final String SOLR_VERSION_5 = "solr5";

    public static boolean isDataCenterCollection(String collection) {
        // 数据中心测试用需要过滤掉
        return StringUtils.startsWith(collection, "search4_fat") || StringUtils.startsWith(collection, "search4_thin");
    // || StringUtils.equalsIgnoreCase(collection, "search4TimeStatistic")
    // || StringUtils.equalsIgnoreCase(collection, "search4OperationStatistic");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
    }
}
