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

package com.qlangtech.tis.realtime.s4totalpay;

import com.qlangtech.tis.manage.common.HttpUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-08-21 15:19
 **/
public class S4Totalpay {

    public static void stubSchemaXStream() {
        HttpUtils.addMockApply(-1, "search4totalpay/0/daily/schema.xml", "schema-xstream.xml", S4Totalpay.class);
    }
}
