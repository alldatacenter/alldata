/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.protocol.transformation;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;

import javax.annotation.Nonnull;

/**
 * TimeUnitConstantParam class is used for the definition and encapsulation of time unit constant param.
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("timeUnitConstant")
@Data
@NoArgsConstructor
public class TimeUnitConstantParam extends ConstantParam {

    private static final long serialVersionUID = 659127597540343115L;

    @JsonProperty("timeUnit")
    private TimeUnit timeUnit;

    /**
     * TimeUnitConstantParam constructor
     *
     * @param timeUnit It is used to store time unit constant value
     */
    @JsonCreator
    public TimeUnitConstantParam(@JsonProperty("timeUnit") @Nonnull TimeUnit timeUnit) {
        super(Preconditions.checkNotNull(timeUnit, "timeUnit is null").name());
        this.timeUnit = timeUnit;
    }

    @Override
    public String getName() {
        return "timeUnitConstant";
    }

    @Override
    public String format() {
        return getValue().toString();
    }

    /**
     * The TimeUnit class defines an enumeration of time units
     */
    public enum TimeUnit {
        /**
         * Time unit for second
         */
        SECOND,
        /**
         * Time unit for minute
         */
        MINUTE,
        /**
         * Time unit for hour
         */
        HOUR
    }

}
