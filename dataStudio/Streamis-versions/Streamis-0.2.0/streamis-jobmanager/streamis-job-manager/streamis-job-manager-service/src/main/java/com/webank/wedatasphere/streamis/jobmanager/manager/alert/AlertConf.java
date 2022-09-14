/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.manager.alert;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.linkis.common.conf.CommonVars;


public interface AlertConf {

    CommonVars<String> ALERT_IP = CommonVars.apply("wds.streamis.alert.streamis.ip", "127.0.0.1");

    CommonVars<String> ALERT_SUB_SYS_ID = CommonVars.apply("wds.streamis.alert.streamis.systemid", "7495");

    Gson COMMON_GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();


}
