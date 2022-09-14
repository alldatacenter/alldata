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

import com.google.gson.annotations.SerializedName;

import java.util.List;


public class AlertEntity {

    @SerializedName("alert_info")
    private String alertInfo;

    @SerializedName("alert_ip")
    private String alertIp;

    @SerializedName("alert_level")
    private int alertLevel;

    @SerializedName("alert_obj")
    private String alertObj;

    @SerializedName("alert_title")
    private String alertTitle = "Streamis application alert(Streamis流式应用系统告警)";

    @SerializedName("remark_info")
    private String remarkInfo;

    @SerializedName("sub_system_id")
    private String subSystemId;

    @SerializedName("use_umg_policy")
    private int useUmgPolicy = 1;

    @SerializedName("alert_reciver")
    private String alertReceiver;

    @SerializedName("alert_way")
    private String alertWay = "2,3";

    public static AlertEntity newInstance(String message, List<String> alertUsers, int alertLevel) {
        AlertEntity alertEntity = new AlertEntity();
        alertEntity.setAlertInfo(message);
        alertEntity.setAlertReceiver(String.join(",", alertUsers));
        alertEntity.setAlertLevel(alertLevel);
        alertEntity.setAlertIp(AlertConf.ALERT_IP.getValue());
        alertEntity.setSubSystemId(AlertConf.ALERT_SUB_SYS_ID.getValue());
        return alertEntity;
    }


    public String getAlertInfo() {
        return alertInfo;
    }

    public void setAlertInfo(String alertInfo) {
        this.alertInfo = alertInfo;
    }

    public String getAlertIp() {
        return alertIp;
    }

    public void setAlertIp(String alertIp) {
        this.alertIp = alertIp;
    }

    public int getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(int alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getAlertObj() {
        return alertObj;
    }

    public void setAlertObj(String alertObj) {
        this.alertObj = alertObj;
    }

    public String getAlertTitle() {
        return alertTitle;
    }

    public void setAlertTitle(String alertTitle) {
        this.alertTitle = alertTitle;
    }

    public String getRemarkInfo() {
        return remarkInfo;
    }

    public void setRemarkInfo(String remarkInfo) {
        this.remarkInfo = remarkInfo;
    }

    public String getSubSystemId() {
        return subSystemId;
    }

    public void setSubSystemId(String subSystemId) {
        this.subSystemId = subSystemId;
    }

    public int getUseUmgPolicy() {
        return useUmgPolicy;
    }

    public void setUseUmgPolicy(int useUmgPolicy) {
        this.useUmgPolicy = useUmgPolicy;
    }

    public String getAlertReceiver() {
        return alertReceiver;
    }

    public void setAlertReceiver(String alertReceiver) {
        this.alertReceiver = alertReceiver;
    }

    public String getAlertWay() {
        return alertWay;
    }

    public void setAlertWay(String alertWay) {
        this.alertWay = alertWay;
    }

    @Override
    public String toString() {
        return AlertConf.COMMON_GSON.toJson(this);
    }
}
