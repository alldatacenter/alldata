/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * Captures time-boundary details
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class TimeBoundary implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TIME_FORMAT = "yyyy/MM/dd HH:mm:ss";

    private String startTime;
    private String endTime;
    private String timeZone; // null for local-time; or a valid ID for TimeZone.getTimeZone(id)

    public TimeBoundary() {
        this(null, null, null);
    }

    public TimeBoundary(String startTime) {
        this(startTime, null, null);
    }

    public TimeBoundary(String startTime, String endTime) {
        this(startTime, endTime, null);
    }

    public TimeBoundary(String startTime, String endTime, String timeZone) {
        this.startTime = startTime;
        this.endTime   = endTime;
        this.timeZone  = timeZone;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        TimeBoundary that = (TimeBoundary) o;
        return Objects.equals(startTime, that.startTime) &&
               Objects.equals(endTime, that.endTime) &&
               Objects.equals(timeZone, that.timeZone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime, timeZone);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TimeBoundary{");

        sb.append("startTime='").append(startTime)
          .append("; endTime='").append(endTime)
          .append("; timeZone='").append(timeZone)
          .append('}');

        return sb.toString();
    }
}
