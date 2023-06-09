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
package com.qlangtech.tis.coredefine.module.action;

import org.apache.commons.lang.StringUtils;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规格
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class Specification {

    private static final Pattern p = Pattern.compile("(\\d+)(\\w*)");

    public static Specification parse(String val) {
        Matcher m = p.matcher(val);
        if (!m.matches()) {
            throw new IllegalArgumentException("val:" + val + " is not match the pattern:" + p);
        }
        Specification s = new Specification();
        s.setVal(Integer.parseInt(m.group(1)));
        s.setUnit(m.group(2));
        return s;
    }

    private int val;

    private String unit;

    public int getVal() {
        return val;
    }

    public boolean isUnitEmpty() {
        return StringUtils.isEmpty(this.unit);
    }

    public void setVal(int val) {
        this.val = val;
    }

    /**
     * 归一化内存规格，单位：兆
     * @return
     */
    public int normalizeMemory() {
        int result = 0;
        if ("M".equals(this.getUnit())) {
            result = this.getVal();
        } else if ("G".equals(this.getUnit())) {
            result = this.getVal() * 1024;
        } else {
            throw new IllegalStateException("invalid memory unit:" + this.getUnit());
        }
        return result;
    }

    public String literalVal() {
        return this.getVal() + this.getUnit();
    }

    public int normalizeCPU() {
        // d.setCpuRequest(Specification.parse("300m"));
        // d.setCpuLimit(Specification.parse("2"));
        int result = 0;
        if ("m".equals(this.getUnit())) {
            result = this.getVal();
        } else if (this.isUnitEmpty()) {
            result = this.getVal() * 1024;
        } else {
            throw new IllegalStateException("invalid cpu unit:" + this.getUnit());
        }
        return result;
    }

    public boolean memoryBigThan(Specification spec){
        Objects.requireNonNull(spec,"param spec can not be null");
        return this.normalizeMemory() > spec.normalizeMemory();
    }

    public boolean cpuBigThan(Specification spec){
        Objects.requireNonNull(spec,"param spec can not be null");
        return this.normalizeCPU() > spec.normalizeCPU();
    }


    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String toString() {
        return this.val + this.unit;
    }
}
