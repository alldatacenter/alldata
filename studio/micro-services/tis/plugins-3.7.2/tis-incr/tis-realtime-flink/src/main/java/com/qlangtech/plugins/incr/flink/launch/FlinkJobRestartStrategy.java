/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.plugins.incr.flink.launch;

import com.qlangtech.tis.extension.Descriptor;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-25 16:01
 **/
public enum FlinkJobRestartStrategy {


    OFF(Descriptor.SWITCH_OFF), FIXED_DELAY("fixed-delay")
    , EXPONENTIAL_DELAY("exponential-delay"), FAILURE_RATE("failure-rate");

    public final String val;

    private FlinkJobRestartStrategy(String val) {
        this.val = val;
    }

    public static FlinkJobRestartStrategy parse(String val) {

        for (FlinkJobRestartStrategy s : FlinkJobRestartStrategy.values()) {
            if (s.val.equals(val)) {
                return s;
            }
        }
        throw new IllegalStateException("val:" + val + " is illegal");
    }
}
