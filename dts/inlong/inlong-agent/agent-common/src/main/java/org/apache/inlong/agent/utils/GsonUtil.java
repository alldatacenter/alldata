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

package org.apache.inlong.agent.utils;

import com.google.gson.Gson;

/**
 * GsonUtil : Gson instances are Thread-safe, so you can reuse them freely across multiple threads.
 */
public class GsonUtil {

    private static final Gson gson = new Gson();

    /**
     * instantiation is not allowed
     */
    private GsonUtil() {
        throw new UnsupportedOperationException("This is a utility class, so instantiation is not allowed");
    }

    /**
     * This method deserializes the specified Json into an object of the specified class.
     *
     * @param json     json
     * @param classOfT class of T
     * @param <T>      T
     * @return T
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    /**
     * This method serializes the specified object into its equivalent Json representation.
     *
     * @param obj obj
     * @return json content
     */
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }
}
