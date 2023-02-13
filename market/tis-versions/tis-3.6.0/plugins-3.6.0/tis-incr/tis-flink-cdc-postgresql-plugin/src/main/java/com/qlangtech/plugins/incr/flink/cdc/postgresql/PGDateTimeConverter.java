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

package com.qlangtech.plugins.incr.flink.cdc.postgresql;

import com.qlangtech.plugins.incr.flink.cdc.valconvert.DateTimeConverter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-19 15:53
 **/
public class PGDateTimeConverter extends DateTimeConverter {
    @Override
    protected String convertDate(Object input) {
        if (input != null) {
            // System.out.println("convertDate:" + input.getClass());
            java.time.LocalDate date = (java.time.LocalDate) input;
            return dateFormatter.format(date);
        }


        return null;
    }

    @Override
    protected String convertTime(Object input) {
        if (input != null) {
            System.out.println("convertTime:" + input.getClass());
        }
        return null;
    }

    @Override
    protected String convertDateTime(Object input) {
        if (input != null) {
            System.out.println("convertDateTime:" + input.getClass());
            throw new UnsupportedOperationException();
        }
        return null;
    }

    private static final ZoneId zof = ZoneId.of("Z");

    @Override
    protected String convertTimestamp(Object input) {
        if (input != null) {
//            if (input instanceof Instant) {
//
//            }

            // System.out.println("timestampZoneId:" + timestampZoneId);
            return timestampFormatter.format(LocalDateTime.ofInstant((Instant) input, zof));

//                System.out.println(">>>>convertTimestamp:" + input.getClass().getName());
//                System.out.println(">>>>convertTimestamp:" + input.getClass() + ",time:" + timestampFormatter.format((Instant) input));
////                return timestampFormatter.format((Instant) input);
//                return null;

        }
        return null;
    }
}
