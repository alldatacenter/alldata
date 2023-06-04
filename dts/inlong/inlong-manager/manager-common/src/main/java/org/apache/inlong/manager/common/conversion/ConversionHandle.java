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

package org.apache.inlong.manager.common.conversion;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Unit conversion handle.
 */
@Slf4j
@Component
public class ConversionHandle {

    private static Map<String, ConversionStrategy> unitMap;

    private void loadStrategy() {
        unitMap = new HashMap<>(16);
        unitMap.put("hours_minutes", new HoursToMinute());
        unitMap.put("hours_seconds", new HoursToSeconds());
        unitMap.put("gb_mb", new GBToMB());
        unitMap.put("tb_mb", new TBToMB());
        unitMap.put("days_seconds", new DaysToSeconds());
        unitMap.put("days_minutes", new DaysToMinute());
        unitMap.put("seconds_seconds", new SecondsToSeconds());
        unitMap.put("mb_mb", new MBToMB());
    }

    /**
     * Unit conversion handle.
     */
    public Integer handleConversion(Integer value, String type) {
        if (unitMap == null) {
            this.loadStrategy();
        }
        ConversionStrategy conversionStrategy = unitMap.get(type);
        ConversionStatusContext conversionStatusContext = new ConversionStatusContext(conversionStrategy);
        return conversionStatusContext.executeConversion(value);
    }

}
