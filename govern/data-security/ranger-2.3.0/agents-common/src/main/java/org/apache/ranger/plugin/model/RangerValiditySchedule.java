/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.model;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsonAutoDetect(fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)

public class RangerValiditySchedule implements Serializable {

    public static final String VALIDITY_SCHEDULE_DATE_STRING_SPECIFICATION = "yyyy/MM/dd HH:mm:ss";

    private String startTime;
    private String endTime;
    private String timeZone;

    private List<RangerValidityRecurrence> recurrences;

    public RangerValiditySchedule(String startTime, String endTime, String timeZone, List<RangerValidityRecurrence> recurrences) {
        setTimeZone(timeZone);
        setStartTime(startTime);
        setEndTime(endTime);
        setRecurrences(recurrences);
    }

    public RangerValiditySchedule() {
        this(null, null, null, null);
    }

    public String getTimeZone() { return timeZone; }
    public String getStartTime() { return startTime;}
    public String getEndTime() { return endTime;}
    public List<RangerValidityRecurrence> getRecurrences() { return recurrences;}

    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    public void setStartTime(String startTime) { this.startTime = startTime;}
    public void setEndTime(String endTime) { this.endTime = endTime;}
    public void setRecurrences(List<RangerValidityRecurrence> recurrences) { this.recurrences = recurrences == null ? new ArrayList<>() : recurrences;}

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RangerValiditySchedule={");

        sb.append(", startTime=").append(startTime);
        sb.append(", endTime=").append(endTime);
        sb.append(", timeZone=").append(timeZone);

        sb.append(", recurrences=").append(Arrays.toString(getRecurrences().toArray()));

        sb.append("}");

        return sb.toString();
    }
}
