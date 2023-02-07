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

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.inlong.sort.protocol.transformation.function.HopEndFunction;
import org.apache.inlong.sort.protocol.transformation.function.HopFunction;
import org.apache.inlong.sort.protocol.transformation.function.HopStartFunction;
import org.apache.inlong.sort.protocol.transformation.function.SessionEndFunction;
import org.apache.inlong.sort.protocol.transformation.function.SessionFunction;
import org.apache.inlong.sort.protocol.transformation.function.SessionStartFunction;
import org.apache.inlong.sort.protocol.transformation.function.TumbleEndFunction;
import org.apache.inlong.sort.protocol.transformation.function.TumbleFunction;
import org.apache.inlong.sort.protocol.transformation.function.TumbleStartFunction;

/**
 * interface for all the window functions appeared in flink
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = WatermarkField.class, name = "watermark"),
        @JsonSubTypes.Type(value = HopStartFunction.class, name = "hopStart"),
        @JsonSubTypes.Type(value = HopEndFunction.class, name = "hopEnd"),
        @JsonSubTypes.Type(value = TumbleStartFunction.class, name = "tumbleStart"),
        @JsonSubTypes.Type(value = TumbleEndFunction.class, name = "tumbleEnd"),
        @JsonSubTypes.Type(value = SessionStartFunction.class, name = "sessionStart"),
        @JsonSubTypes.Type(value = SessionEndFunction.class, name = "sessionEnd"),
        @JsonSubTypes.Type(value = SessionFunction.class, name = "session"),
        @JsonSubTypes.Type(value = TumbleFunction.class, name = "tumble"),
        @JsonSubTypes.Type(value = HopFunction.class, name = "hop")
})
public interface TimeWindowFunction extends Function {

    TimeUnitConstantParam getTimeUnit();

    ConstantParam getInterval();

}
