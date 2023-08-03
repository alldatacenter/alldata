/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.authorization.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.AuditFilter;
import org.apache.ranger.plugin.model.RangerValidityRecurrence;
import org.apache.ranger.plugin.model.RangerValiditySchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class JsonUtils {
    private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);

    private static final ThreadLocal<Gson> gson = new ThreadLocal<Gson>() {
        @Override
        protected Gson initialValue() {
            return new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z").create();
        }
    };

    public static String mapToJson(Map<?, ?> map) {
        String ret = null;
        if (MapUtils.isNotEmpty(map)) {
            try {
                ret = gson.get().toJson(map);
            } catch (Exception e) {
                LOG.error("Invalid input data: ", e);
            }
        }
        return ret;
    }

    public static String listToJson(List<?> list) {
        String ret = null;
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                ret = gson.get().toJson(list);
            } catch (Exception e) {
                LOG.error("Invalid input data: ", e);
            }
        }
        return ret;
    }

    public static String objectToJson(Object object) {
        String ret = null;

        if(object != null) {
            try {
                ret = gson.get().toJson(object);
            } catch(Exception excp) {
                LOG.warn("objectToJson() failed to convert object to Json", excp);
            }
        }

        return ret;
    }

    public static <T> T jsonToObject(String jsonStr, Class<T> clz) {
        T ret = null;

        if(StringUtils.isNotEmpty(jsonStr)) {
            try {
                ret = gson.get().fromJson(jsonStr, clz);
            } catch(Exception excp) {
                LOG.warn("jsonToObject() failed to convert json to object: " + jsonStr, excp);
            }
        }

        return ret;
    }

    public static Map<String, String> jsonToMapStringString(String jsonStr) {
        Map<String, String> ret = null;

        if(StringUtils.isNotEmpty(jsonStr)) {
            try {
                Type mapType = new TypeToken<Map<String, String>>() {}.getType();
                ret = gson.get().fromJson(jsonStr, mapType);
            } catch(Exception excp) {
                LOG.warn("jsonToObject() failed to convert json to object: " + jsonStr, excp);
            }
        }

        return ret;
    }

    public static List<RangerValiditySchedule> jsonToRangerValiditySchedule(String jsonStr) {
        try {
            Type listType = new TypeToken<List<RangerValiditySchedule>>() {}.getType();
            return gson.get().fromJson(jsonStr, listType);
        } catch (Exception e) {
            LOG.error("Cannot get List<RangerValiditySchedule> from " + jsonStr, e);
            return null;
        }
    }

    public static List<AuditFilter> jsonToAuditFilterList(String jsonStr) {
        try {
            Type listType = new TypeToken<List<AuditFilter>>() {}.getType();
            return gson.get().fromJson(jsonStr, listType);
        } catch (Exception e) {
            LOG.error("failed to create audit filters from: " + jsonStr, e);
            return null;
        }
    }

    public static List<RangerValidityRecurrence> jsonToRangerValidityRecurringSchedule(String jsonStr) {
        try {
            Type listType = new TypeToken<List<RangerValidityRecurrence>>() {
            }.getType();
            return gson.get().fromJson(jsonStr, listType);
        } catch (Exception e) {
            LOG.error("Cannot get List<RangerValidityRecurrence> from " + jsonStr, e);
            return null;
        }
    }
}
