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

package org.apache.inlong.agent.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * metric for field
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Metric {

    /**
     * Type of metric
     *
     * @return metric type
     */
    Type type() default Type.DEFAULT;

    /**
     * Doc of metric
     *
     * @return metric doc
     */
    String desc() default "";

    enum Type {
        DEFAULT("java.lang.String"),
        COUNTER_INT("java.lang.Integer"),
        COUNTER_LONG("java.lang.Long"),
        GAUGE_INT("java.lang.Integer"),
        GAUGE_LONG("java.lang.Long"),
        TAG("java.lang.String");
        private final String value;

        private Type(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
