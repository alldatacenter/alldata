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
package org.apache.atlas.web.model;

public class DebugMetrics {
    private String name;
    private int    numops;
    private float  minTime;
    private float  maxTime;
    private float  stdDevTime;
    private float  avgTime;

    public DebugMetrics() {}

    public DebugMetrics(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumops() {
        return numops;
    }

    public void setNumops(int numops) {
        this.numops = numops;
    }

    public float getMinTime() {
        return minTime;
    }

    public void setMinTime(float minTime) {
        this.minTime = minTime;
    }

    public float getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(float maxTime) {
        this.maxTime = maxTime;
    }

    public float getStdDevTime() {
        return stdDevTime;
    }

    public void setStdDevTime(float stdDevTime) {
        this.stdDevTime = stdDevTime;
    }

    public float getAvgTime() {
        return avgTime;
    }

    public void setAvgTime(float avgTime) {
        this.avgTime = avgTime;
    }
}