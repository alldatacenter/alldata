/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.oef.services.model;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DisPolicyBean {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "stream")
    private String stream;

    @JsonProperty(value = "project")
    private String project;

    @JsonProperty(value = "events")
    private ArrayList<String> events;

    @JsonProperty(value = "prefix")
    private String prefix;

    @JsonProperty(value = "suffix")
    private String suffix;

    @JsonProperty(value = "agency")
    private String agency;

    public DisPolicyBean() {

    }

    public DisPolicyBean(String id, String stream, String project, ArrayList<String> events, String agency) {
        this.id = id;
        this.stream = stream;
        this.project = project;
        this.agency = agency;
        this.events = events;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public ArrayList<String> getEvents() {
        return events;
    }

    public void setEvents(ArrayList<String> events) {
        this.events = events;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    @Override
    public String toString() {
        StringBuilder strEvents = new StringBuilder();
        if (events != null) {
            for (String event : events) {
                strEvents.append(event);
                strEvents.append(",");
            }
            if (strEvents.length() > 0) {
                strEvents = strEvents.deleteCharAt(strEvents.length() - 1);
            }
        }
        return "DisPolicyBean [id = " + id + ", stream = " + stream + ", project = " + project + ", events = "
                + strEvents + ", prefix = " + prefix + ", suffix = " + suffix + ", agency = " + agency + "]";
    }
}
